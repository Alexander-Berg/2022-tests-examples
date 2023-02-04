package com.yandex.maps.testapp.toponym_photo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.yandex.maps.testapp.R;

import java.util.List;

public class UploadListAdapter extends ArrayAdapter<UploadItem> {

    private static class ViewHolder {
        ImageView image;
        TextView text;
    }

    public UploadListAdapter(Context context, List<UploadItem> photos) {
        super(context, 0, photos);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final UploadItem photo = getItem(position);

        final ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.toponym_photo_upload_item, parent, false);

            viewHolder.image = convertView.findViewById(R.id.toponym_photo_item_image);
            viewHolder.text = convertView.findViewById(R.id.toponym_photo_item_text);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder)convertView.getTag();
        }

        viewHolder.image.setImageBitmap(photo.thumbnail());
        viewHolder.text.setText(photo.description());

        return convertView;
    }

}
