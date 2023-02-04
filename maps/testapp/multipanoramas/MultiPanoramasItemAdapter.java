package com.yandex.maps.testapp.multipanoramas;

import com.yandex.mapkit.places.panorama.PanoramaView;
import com.yandex.mapkit.places.panorama.Player;
import com.yandex.maps.testapp.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

public class MultiPanoramasItemAdapter extends ArrayAdapter<String> {

    public MultiPanoramasItemAdapter(Context context, String[] panoIds) {
        super(context, R.layout.single_panoview, panoIds);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        String panoId = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.single_panoview, parent, false);
        }

        PanoramaView panoramaView = (PanoramaView) convertView.findViewById(R.id.single_panoview);
        Player player = panoramaView.getPlayer();

        player.reset();
        player.openPanorama(panoId);

        return convertView;
    }
}
