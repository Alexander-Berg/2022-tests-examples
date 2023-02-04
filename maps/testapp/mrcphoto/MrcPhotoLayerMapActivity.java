package com.yandex.maps.testapp.mrcphoto;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.InputListener;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.places.PlacesFactory;
import com.yandex.mapkit.places.mrc.MrcPhotoLayer;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.map.MapBaseActivity;

import java.util.logging.Logger;

public class MrcPhotoLayerMapActivity extends MapBaseActivity implements InputListener {
    private static Logger LOGGER = Logger.getLogger("yandex.maps.MrcPhotoLayerMapActivity");

    private Spinner mrcLayerSpinner;
    private MrcPhotoLayer mrcPhotoLayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.mrc_photo_map);
        super.onCreate(savedInstanceState);

        mrcPhotoLayer = PlacesFactory.getInstance().createMrcPhotoLayer(mapview.getMapWindow());
        mrcLayerSpinner = (Spinner)findViewById(R.id.mrc_photo_layer_spinner);


        mrcLayerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                final String[] choices = getResources().getStringArray(R.array.mrc_photo_layer_choice);
                switch(choices[position]) {
                    case "off":
                        mrcPhotoLayer.disable();
                        break;
                    case "automotive":
                        mrcPhotoLayer.enable(MrcPhotoLayer.VisibleLayer.AUTOMOTIVE);
                        break;
                    case "automotive-age":
                        mrcPhotoLayer.enable(MrcPhotoLayer.VisibleLayer.AUTOMOTIVE_AGE);
                        break;
                    case "pedestrian":
                        mrcPhotoLayer.enable(MrcPhotoLayer.VisibleLayer.PEDESTRIAN);
                        break;
                    case "pedestrian-age":
                        mrcPhotoLayer.enable(MrcPhotoLayer.VisibleLayer.PEDESTRIAN_AGE);
                        break;
                }
                onMrcPhotoLayerChange();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        mapview.getMap().addInputListener(this);
    }

    @Override
    public void onMapTap(@NonNull Map map, @NonNull Point point) {
        LOGGER.info("onMapTap");
    }

    @Override
    public void onMapLongTap(@NonNull Map map, @NonNull Point point) {
        LOGGER.info("onMapLongTap");
        MrcPhotoLayer.VisibleLayer layer = mrcPhotoLayer.getVisibleLayer();
        if (layer != null) {
            onMrcLayerTap(map, point);
        } else {
            Toast.makeText(this, "Visible layer must be set", Toast.LENGTH_LONG).show();
        }
    }

    @Nullable
    protected MrcPhotoLayer.VisibleLayer getVisibleLayer() {
        return mrcPhotoLayer.getVisibleLayer();
    }

    protected void onMrcPhotoLayerChange() {
        // override in derived classes if needed
    }

    protected void onMrcLayerTap(@NonNull Map map, @NonNull Point point) {
        // override in derived classes if needed
    }
}
