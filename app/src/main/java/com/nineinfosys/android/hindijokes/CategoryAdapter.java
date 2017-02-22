package com.nineinfosys.android.hindijokes;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

/**
 * Created by Dev on 22-02-2017.
 */

public class CategoryAdapter extends ArrayAdapter<HindiJokesCategory> {
    Context mContext;

    int mLayoutResourceId;

    public CategoryAdapter(Context context, int resource) {
        super(context, resource);
        mContext = context;
        mLayoutResourceId = resource;

    }

    @Override
    public View getView(int position, final View convertView, ViewGroup parent) {
        View row = convertView;
        final HindiJokesCategory currentItem = getItem(position);
        if (row == null) {
            LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
            row = inflater.inflate(R.layout.row_list_category, parent, false);

        }
        row.setTag(currentItem);

        final TextView textViewCategory = (TextView) row.findViewById(R.id.textViewCategory);

        textViewCategory.setText(currentItem.getCategory());
        //imageViewCategories.set

        final ImageView imageViewCategory = (ImageView) row.findViewById(R.id.imageViewCategory);

      //  imageViewCategory.setImageURI(Uri.parse(currentItem.getImage()));
        Picasso.with(mContext).load(currentItem.getImage()).into(imageViewCategory);

        return row;

    }
}
