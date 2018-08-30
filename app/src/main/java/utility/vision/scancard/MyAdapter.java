package utility.vision.scancard;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {
    private Context mContext;
    private List<String> data = new ArrayList<>();
    public int lastSelectedPosition = -1;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class MyViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        TextView txtCodeNumber;
        LinearLayout line;
        RadioButton selectionState;
        public MyViewHolder(View itemView) {
            super(itemView);
            txtCodeNumber = (TextView) itemView.findViewById(R.id.code_number);
            line = (LinearLayout) itemView.findViewById(R.id.line);
            selectionState = (RadioButton) itemView.findViewById(R.id.radio);

            View.OnClickListener clickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    lastSelectedPosition = getAdapterPosition();
                    //notifyDataSetChanged();
                    notifyItemRangeChanged(0, data.size());

                    if (onItemClickedListener != null) {
                        onItemClickedListener.onItemClick(txtCodeNumber.getText().toString());
                    }
                    /*
                    Toast.makeText(MyAdapter.this.mContext,
                            "selected item is " + txtCodeNumber.getText(),
                            Toast.LENGTH_LONG).show();
                    */
                }
            };
            selectionState.setOnClickListener(clickListener);
            line.setOnClickListener(clickListener);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public MyAdapter(Context mContext, List<String> data) {
        this.mContext = mContext;
        this.data = data;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public MyAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent,
                                                     int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.my_textview, parent, false);
        return new MyViewHolder(view);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {
        holder.txtCodeNumber.setText(data.get(position));
        holder.txtCodeNumber.setTextSize(22);
        holder.selectionState.setChecked(lastSelectedPosition == position);
        /*
        holder.line.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //holder.line.setBackgroundColor(ContextCompat.getColor(mContext, R.color.common_google_signin_btn_text_light_pressed));
                if (onItemClickedListener != null) {
                    onItemClickedListener.onItemClick(holder.txtCodeNumber.getText().toString());
                    holder.selectionState.setChecked(true);
                }
            }
        });
        */
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return data.size();
    }

    public interface OnItemClickedListener {
        void onItemClick(String codeNumber);
    }

    private OnItemClickedListener onItemClickedListener;

    public void setOnItemClickedListener(OnItemClickedListener onItemClickedListener) {
        this.onItemClickedListener = onItemClickedListener;
    }
}
