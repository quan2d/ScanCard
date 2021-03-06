/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package utility.vision.scancard.ui.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.google.android.gms.vision.CameraSource;

import java.util.HashSet;
import java.util.Set;

/**
 * A view which renders a series of custom graphics to be overlaid on top of an associated preview
 * (i.e., the camera preview).  The creator can add graphics objects, update the objects, and remove
 * them, triggering the appropriate drawing and invalidation within the view.<p>
 *
 * Supports scaling and mirroring of the graphics relative the camera's preview properties.  The
 * idea is that detection items are expressed in terms of a preview size, but need to be scaled up
 * to the full view size, and also mirrored in the case of the front-facing camera.<p>
 *
 * Associated {@link Graphic} items should use the following methods to convert to view coordinates
 * for the graphics that are drawn:
 * <ol>
 * <li>{@link Graphic#scaleX(float)} and {@link Graphic#scaleY(float)} adjust the size of the
 * supplied value from the preview scale to the view scale.</li>
 * <li>{@link Graphic#translateX(float)} and {@link Graphic#translateY(float)} adjust the coordinate
 * from the preview's coordinate system to the view coordinate system.</li>
 * </ol>
 */
public class GraphicOverlay<T extends GraphicOverlay.Graphic> extends View {
    private final Object mLock = new Object();
    private int mPreviewWidth;
    private float mWidthScaleFactor = 1.0f;
    private int mPreviewHeight;
    private float mHeightScaleFactor = 1.0f;
    private int mFacing = CameraSource.CAMERA_FACING_BACK;
    private Set<T> mGraphics = new HashSet<>();
    private Paint paint = new Paint();
    private Point p1, p2, p3, p4;

    /**
     * Base class for a custom graphics object to be rendered within the graphic overlay.  Subclass
     * this and implement the {@link Graphic#draw(Canvas)} method to define the
     * graphics element.  Add instances to the overlay using {@link GraphicOverlay#add(Graphic)}.
     */
    public static abstract class Graphic {
        private GraphicOverlay mOverlay;

        public Graphic(GraphicOverlay overlay) {
            mOverlay = overlay;
        }

        /**
         * Draw the graphic on the supplied canvas.  Drawing should use the following methods to
         * convert to view coordinates for the graphics that are drawn:
         * <ol>
         * <li>{@link Graphic#scaleX(float)} and {@link Graphic#scaleY(float)} adjust the size of
         * the supplied value from the preview scale to the view scale.</li>
         * <li>{@link Graphic#translateX(float)} and {@link Graphic#translateY(float)} adjust the
         * coordinate from the preview's coordinate system to the view coordinate system.</li>
         * </ol>
         *
         * @param canvas drawing canvas
         */
        public abstract void draw(Canvas canvas);

        /**
         * Returns true if the supplied coordinates are within this graphic.
         */
        public abstract boolean contains(float x, float y);

        /**
         * Adjusts a horizontal value of the supplied value from the preview scale to the view
         * scale.
         */
        public float scaleX(float horizontal) {
            return horizontal * mOverlay.mWidthScaleFactor;
        }

        /**
         * Adjusts a vertical value of the supplied value from the preview scale to the view scale.
         */
        public float scaleY(float vertical) {
            return vertical * mOverlay.mHeightScaleFactor;
        }

        /**
         * Adjusts the x coordinate from the preview's coordinate system to the view coordinate
         * system.
         */
        public float translateX(float x) {
            if (mOverlay.mFacing == CameraSource.CAMERA_FACING_FRONT) {
                return mOverlay.getWidth() - scaleX(x);
            } else {
                return scaleX(x);
            }
        }

        /**
         * Adjusts the y coordinate from the preview's coordinate system to the view coordinate
         * system.
         */
        public float translateY(float y) {
            return scaleY(y);
        }

        public void postInvalidate() {
            mOverlay.postInvalidate();
        }
    }

    public GraphicOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Removes all graphics from the overlay.
     */
    public void clear() {
        synchronized (mLock) {
            mGraphics.clear();
        }
        postInvalidate();
    }

    /**
     * Adds a graphic to the overlay.
     */
    public void add(T graphic) {
        synchronized (mLock) {
            mGraphics.add(graphic);
        }
        postInvalidate();
    }

    /**
     * Removes a graphic from the overlay.
     */
    public void remove(T graphic) {
        synchronized (mLock) {
            mGraphics.remove(graphic);
        }
        postInvalidate();
    }

    /**
     * Returns the first graphic, if any, that exists at the provided absolute screen coordinates.
     * These coordinates will be offset by the relative screen position of this view.
     * @return First graphic containing the point, or null if no text is detected.
     */
    public T getGraphicAtLocation(float rawX, float rawY) {
        synchronized (mLock) {
            // Get the position of this View so the raw location can be offset relative to the view.
            int[] location = new int[2];
            this.getLocationOnScreen(location);
            for (T graphic : mGraphics) {
                if (graphic.contains(rawX - location[0], rawY - location[1])) {
                    return graphic;
                }
            }
            return null;
        }
    }

    /**
     * Sets the camera attributes for size and facing direction, which informs how to transform
     * image coordinates later.
     */
    public void setCameraInfo(int previewWidth, int previewHeight, int facing) {
        synchronized (mLock) {
            mPreviewWidth = previewWidth;
            mPreviewHeight = previewHeight;
            mFacing = facing;

            p1 = new Point();
            p2 = new Point();
            p3 = new Point();
            p4 = new Point();

            p1.x = mPreviewWidth/6;
            p1.y = mPreviewHeight*2/5;

            p2.x = mPreviewWidth*5/6;
            p2.y = mPreviewHeight*2/5;

            p3.x = mPreviewWidth/6;
            p3.y = mPreviewHeight*3/5;

            p4.x = mPreviewWidth*5/6;
            p4.y = mPreviewHeight*3/5;
        }
        postInvalidate();
    }

    /**
     * Draws the overlay with its associated graphic objects.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        synchronized (mLock) {
            if ((mPreviewWidth != 0) && (mPreviewHeight != 0)) {
                mWidthScaleFactor = (float) canvas.getWidth() / (float) mPreviewWidth;
                mHeightScaleFactor = (float) canvas.getHeight() / (float) mPreviewHeight;
            }

            paint.setColor(Color.WHITE);
            paint.setStrokeWidth(6f);

            canvas.drawLine(p1.x * mWidthScaleFactor, p1.y * mHeightScaleFactor, p1.x * mWidthScaleFactor, p1.y * mHeightScaleFactor + 30, paint);
            canvas.drawLine(p1.x * mWidthScaleFactor - 3, p1.y * mHeightScaleFactor, p1.x * mWidthScaleFactor + 30, p1.y * mHeightScaleFactor, paint);

            canvas.drawLine(p2.x * mWidthScaleFactor, p2.y * mHeightScaleFactor, p2.x * mWidthScaleFactor, p2.y * mHeightScaleFactor + 30, paint);
            canvas.drawLine(p2.x * mWidthScaleFactor + 3, p2.y * mHeightScaleFactor, p2.x * mWidthScaleFactor - 30, p2.y * mHeightScaleFactor, paint);

            canvas.drawLine(p3.x * mWidthScaleFactor, p3.y * mHeightScaleFactor, p3.x * mWidthScaleFactor, p3.y * mHeightScaleFactor - 30, paint);
            canvas.drawLine(p3.x * mWidthScaleFactor - 3, p3.y * mHeightScaleFactor, p3.x * mWidthScaleFactor + 30, p3.y * mHeightScaleFactor, paint);

            canvas.drawLine(p4.x * mWidthScaleFactor, p4.y * mHeightScaleFactor, p4.x * mWidthScaleFactor, p4.y * mHeightScaleFactor - 30, paint);
            canvas.drawLine(p4.x * mWidthScaleFactor + 3, p4.y * mHeightScaleFactor, p4.x * mWidthScaleFactor - 30, p4.y * mHeightScaleFactor, paint);

            paint.setColor(Color.RED);
            paint.setStrokeWidth(2f);

            canvas.drawLine(p1.x * mWidthScaleFactor, mPreviewHeight/2 * mHeightScaleFactor, p2.x * mWidthScaleFactor, mPreviewHeight/2 * mHeightScaleFactor, paint);

            for (Graphic graphic : mGraphics) {
                graphic.draw(canvas);
            }
        }
    }
}
