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

package utility.vision.scancard;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import com.google.android.gms.common.api.CommonStatusCodes;

import java.util.ArrayList;
import java.util.List;

/**
 * Main activity demonstrating how to pass extra parameters to an activity that
 * recognizes text.
 */
public class MainActivity extends Activity implements View.OnClickListener {

    private static final int RC_HANDLE_CALL_PERM = 3;
    // Use a compound button so either checkbox or switch widgets work.
    //private TextView statusMessage;
    private EditText textNumber;
    private EditText textPrefix;
    private EditText textSuffix;
    private ImageView imageView;

    RecyclerView mRecyclerView;
    MyAdapter mRcvAdapter;
    List<String> datalist;

    private static final int RC_OCR_CAPTURE = 9003;
    private static final String TAG = "MainActivity";

    private SharedPreferences settings;

    private AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get from the SharedPreferences
        settings = getApplicationContext().getSharedPreferences("scancard_profile", 0);
        String strPrefix = settings.getString("prefix","*100*");
        String strSuffix = settings.getString("suffix","#");

        //Initialize ads
        MobileAds.initialize(this, "ca-app-pub-7136084704647401~1928016697");
        mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        //statusMessage = (TextView)findViewById(R.id.status_message);
        textNumber = (EditText) findViewById(R.id.text_number);
        textPrefix = (EditText) findViewById(R.id.text_prefix);
        textSuffix = (EditText) findViewById(R.id.text_suffix);
        imageView = (ImageView)findViewById(R.id.imageView);
        mRecyclerView = (RecyclerView) findViewById(R.id.listCode);

        textPrefix.setText(strPrefix);
        textSuffix.setText(strSuffix);

        findViewById(R.id.imageButtonCancel).setOnClickListener(this);
        findViewById(R.id.imageButtonCall).setOnClickListener(this);

        Intent intent = new Intent(this, OcrCaptureActivity.class);

        startActivityForResult(intent, RC_OCR_CAPTURE);
    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.imageButtonCancel) {
            // launch Ocr capture activity.
            Intent intent = new Intent(this, OcrCaptureActivity.class);

            startActivityForResult(intent, RC_OCR_CAPTURE);

            //startActivity(intent);
        }else if(v.getId() == R.id.imageButtonCall){
            //Store data
            SharedPreferences.Editor editor = settings.edit();
            if(textPrefix.getText().toString().length() < 5){
                //Do not update, and set default value
                textPrefix.setText("*100*");
            }else{
                if(textPrefix.getText().toString().startsWith("*") && textPrefix.getText().toString().endsWith("*")){
                    editor.putString("prefix", textPrefix.getText().toString());
                }else{
                    textPrefix.setText("*100*");
                }
            }

            if(textSuffix.getText().toString().length() == 0){
                //Do not update, and set default value
                textSuffix.setText("#");
            }else{
                editor.putString("suffix", textSuffix.getText().toString());
            }

            // Apply the edits!
            editor.apply();

            Log.d(TAG, "Call: " + textPrefix.getText().toString() + textNumber.getText().toString() + textSuffix.getText().toString());

            Intent intent = new Intent(Intent.ACTION_CALL, Uri.fromParts("tel", textPrefix.getText().toString() + textNumber.getText().toString() + textSuffix.getText().toString(), null));
            int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE);
            if (rc != PackageManager.PERMISSION_GRANTED) {
                requestCallPermission();
            }else {
                startActivity(intent);
            }
        }
    }

    @Override
    public void onBackPressed() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(R.string.title_activity_main);
        //builder.setIcon(R.drawable.icon);
        builder.setMessage("Do you want to exit?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();

    }
    /**
     * Handles the requesting of the call permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private void requestCallPermission() {
        Log.w(TAG, "Call permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CALL_PHONE};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CALL_PHONE)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CALL_PERM);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_CALL_PERM);
            }
        };

        Snackbar.make(findViewById(R.layout.activity_main), R.string.permission_call_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != RC_HANDLE_CALL_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (requestCode == RC_HANDLE_CALL_PERM) {
            if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Call permission granted");
                return;
            } else {
                Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                        " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

                DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //finish();
                        moveTaskToBack(true);
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("ScanCard")
                        .setMessage(R.string.no_call_permission)
                        .setPositiveButton(R.string.ok, listener)
                        .show();
            }
        }
    }

    /**
     * Called when an activity you launched exits, giving you the requestCode
     * you started it with, the resultCode it returned, and any additional
     * data from it.  The <var>resultCode</var> will be
     * {@link #RESULT_CANCELED} if the activity explicitly returned that,
     * didn't return any result, or crashed during its operation.
     * <p/>
     * <p>You will receive this call immediately before onResume() when your
     * activity is re-starting.
     * <p/>
     *
     * @param requestCode The integer request code originally supplied to
     *                    startActivityForResult(), allowing you to identify who this
     *                    result came from.
     * @param resultCode  The integer result code returned by the child activity
     *                    through its setResult().
     * @param data        An Intent, which can return result data to the caller
     *                    (various data can be attached to Intent "extras").
     * @see #startActivityForResult
     * @see #createPendingResult
     * @see #setResult(int)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == RC_OCR_CAPTURE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    //String text = data.getStringExtra(OcrCaptureActivity.TextBlockObject);
                    ArrayList<String> arrText = data.getStringArrayListExtra(OcrCaptureActivity.TextBlockObject);
                    byte[] image = data.getByteArrayExtra(OcrCaptureActivity.ImageObject);
                    Log.d(TAG, "Image read: " + image.length);
                    //statusMessage.setText(R.string.ocr_success);
                    String stext = "";
                    datalist = new ArrayList<>();
                    for(String text : arrText) {
                        //stext += text + ",";
                        datalist.add(getCodeNumber(text));
                    }

                    mRcvAdapter = new MyAdapter(this, datalist);

                    String [] temp = codeNumberIndex(datalist);
                    textNumber.setText(temp[0]);

                    mRcvAdapter.lastSelectedPosition = Integer.parseInt(temp[1]);

                    LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
                    layoutManager.setOrientation(LinearLayoutManager.VERTICAL);

                    mRecyclerView.setLayoutManager(layoutManager);
                    mRecyclerView.setAdapter(mRcvAdapter);

                    mRcvAdapter.setOnItemClickedListener(new MyAdapter.OnItemClickedListener() {
                        @Override
                        public void onItemClick(String codeNumber) {
                            textNumber.setText(codeNumber);
                            Log.d(TAG, "Copy: " + codeNumber);
                            setClipboard(MainActivity.this, codeNumber);
                            Toast.makeText(MainActivity.this, "Copy : " + codeNumber, Toast.LENGTH_SHORT).show();
                        }
                    });

                    Bitmap b = BitmapFactory.decodeByteArray(image,0,image.length);
                    imageView.setImageBitmap(b);
                } else {
                    //statusMessage.setText(R.string.ocr_failure);
                    Log.d(TAG, "No Text captured, intent data is null");
                }
            } else {
                //statusMessage.setText(String.format(getString(R.string.ocr_error),
                //        CommonStatusCodes.getStatusCodeString(resultCode)));
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    protected String getCodeNumber(String text){
        String rc = "";
        String temp = "";

        //Remove CR LF
        temp = text.replaceAll("\\n", " ");
        temp = temp.replaceAll("\\r", " ");

        //Trim string
        temp = temp.trim();

        String [] arrTemp = temp.split(" ");
        int i, len = arrTemp.length, idMaxLen = 0, maxLen = 0;

        if(arrTemp.length > 1){
            for(i = 0; i < len; i++){
                if(TextUtils.isDigitsOnly(arrTemp[i])){     //Check is digit
                    if(arrTemp[i].indexOf("1800") < 0){
                        rc += arrTemp[i];
                    }
                }else if(countAlphabet(arrTemp[i]) == 1){   //May be have an character at first
                    rc += arrTemp[i];
                }

                if(maxLen < arrTemp[i].length()){
                    maxLen = arrTemp[i].length();
                    idMaxLen = i;
                }                
            }

            //Re-check
            if(rc.length() == 0){
                //Find item has max length
                rc = arrTemp[idMaxLen];
            }
        }else{
            rc = arrTemp[0];
        }

        if(rc.charAt(0) == 't'){
            rc = rc.replaceFirst("t", "1");
        }else{
            rc = rc.replaceAll("[^0-9]", "");
        }

        return rc;
    }

    protected String[] codeNumberIndex(List<String> data){
        int id = 0;
        String [] result = {"",""};

        if(data.size() == 1){
            id = 0;
        }else{
            //Viettel
            if(data.get(0).length() == 15 && data.get(1).length() == 14){
                id = 0;
            }else if(data.get(0).length() == 14 && data.get(1).length() == 15){
                id = 1;
            }else if(data.get(0).length() == 13 && data.get(1).length() == 11){
                id = 0;
            }else if(data.get(0).length() == 11 && data.get(1).length() == 13){
                id = 1;
            //Mobile
            }else if(data.get(0).length() == 12 && data.get(1).length() == 15){
                id = 0;
            }else if(data.get(0).length() == 15 && data.get(1).length() == 12){
                id = 1;
            //Vinaphone
            }else if(data.get(0).length() == 12 && data.get(1).length() == 14){
                id = 0;
            }else if(data.get(0).length() == 14 && data.get(1).length() == 12){
                id = 1;
            }else{
                id = 0;
            }
        }
        result[0] = data.get(id);
        result[1] = "" + id;
        return result;
    }

    protected int countAlphabet(String input){
        int count = 0, len, i;
        char ch;

        len = input.length();

        for(i = 0; i < len; i++){
            ch = input.charAt(i);
            if(ch < 48 && ch > 57){//not number
                count++;
            }
        }

        return count;
    }

    private void setClipboard(Context context, String text) {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", text);
        clipboard.setPrimaryClip(clip);
    }
}
