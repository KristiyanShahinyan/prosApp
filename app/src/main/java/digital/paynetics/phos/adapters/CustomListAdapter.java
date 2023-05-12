package digital.paynetics.phos.adapters;


import android.content.Context;
import androidx.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import digital.paynetics.phos.R;
import digital.paynetics.phos.classes.MenuRow;
import digital.paynetics.phos.classes.enums.RowType;


public class CustomListAdapter extends ArrayAdapter {


    private final int selectedRow;
    private final ArrayList<MenuRow> records;

    private HashMap<RowType, Integer> layouts = new HashMap<>();

    @Override
    public int getViewTypeCount() {
        return RowType.values().length;
    }

    @Override
    public int getItemViewType(int position) {

        return records.get(position).getType().getCode();
    }


    public CustomListAdapter(@NonNull Context context, int resource, @NonNull ArrayList<MenuRow> objects, int selectedRow) {
        super(context, resource, objects);

        this.records = objects;
        this.selectedRow = selectedRow;


        layouts.put(RowType.TITLE, R.layout.row_popup_title);
        layouts.put(RowType.REGULAR, R.layout.row_popup_regular);

    }

    public void setCustomLayoutFor(RowType rowType, Integer layoutId) {
        layouts.put(rowType, layoutId);
    }

    public class ViewHolder {
        private  TextView text;
        private ImageView icon;
        private View parent;

        public ViewHolder(View convertView) {
            this.text = convertView.findViewById(R.id.title);
            this.icon = convertView.findViewById(R.id.icon);
            this.parent = convertView;
        }

        public TextView getText() {
            return text;
        }

        public void setText(TextView text) {
            this.text = text;
        }


        public ImageView getIcon() {
            return icon;
        }


        public void setIcon(ImageView icon) {
            this.icon = icon;
        }


        public View getParent() {
            return parent;
        }


        public void setParent(View parent) {
            this.parent = parent;
        }


    }

    @Override
    @NonNull
    public View  getView(int position, View convertView, @NonNull  ViewGroup parent) {

        ViewHolder viewHolder;
        MenuRow row = records.get(position);
        int itemType = getItemViewType(position);

        if (convertView == null) {

            convertView = LayoutInflater.from(getContext()).inflate(layouts.get(RowType.byCode(itemType)), null);

            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);

        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.getText().setText(row.getTitle());

        if (viewHolder.getIcon() != null)
            viewHolder.getIcon().setImageDrawable(row.getIcon());

        if (selectedRow == position) {
            viewHolder.getParent().setBackgroundColor(getContext().getResources().getColor(R.color.lightblue));
        } else if (row.getOnClick() != null) {
            viewHolder.getParent().setBackground(getContext().getResources().getDrawable(R.drawable.button_flat));
        } else {
            viewHolder.getParent().setBackgroundColor(getContext().getResources().getColor(R.color.white));
        }

        viewHolder.parent.setOnClickListener(view -> {
            if (row.getOnClick() != null) {
                row.getOnClick().itemClicked();
            }
        });

        return viewHolder.getParent();
    }

    public ArrayList<MenuRow> getRecords() {
        return records;
    }
}
