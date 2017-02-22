package com.nineinfosys.android.hindijokes;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * Created by AndriodDev8 on 20-02-2017.
 */

public class ContentAdapter extends ArrayAdapter<HindiJokesContent> {

    Context mContext;

    int mLayoutResourceId;

    public ContentAdapter(Context context, int resource){
        super(context, resource);
        mContext = context;
        mLayoutResourceId = resource;

    }

    @Override
    public View getView(int position, final View convertView, ViewGroup parent) {
        View row = convertView;
        final HindiJokesContent currentItem = getItem(position);
        if (row == null) {
            LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
            row = inflater.inflate(R.layout.row_list_content, parent, false);

        }
        row.setTag(currentItem);

        final TextView textViewContent = (TextView)row.findViewById(R.id.textViewContent);
        textViewContent.setText(currentItem.getContent());

        return row;







    }






}
