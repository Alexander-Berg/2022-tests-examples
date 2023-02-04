package com.yandex.maps.testapp.panorama;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.EditText;
import android.view.inputmethod.EditorInfo;
import android.view.KeyEvent;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.layers.GeoObjectTapEvent;
import com.yandex.mapkit.layers.GeoObjectTapListener;
import com.yandex.mapkit.map.InputListener;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.mapkit.places.PlacesFactory;
import com.yandex.mapkit.places.panorama.AirshipTapInfo;
import com.yandex.mapkit.places.panorama.PanoramaLayer;
import com.yandex.mapkit.places.panorama.PanoramaService;
import com.yandex.mapkit.places.panorama.PanoramaService.SearchListener;
import com.yandex.mapkit.places.panorama.PanoramaService.SearchSession;
import com.yandex.mapkit.places.panorama.NotFoundError;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.map.MapBaseActivity;
import com.yandex.runtime.Error;

import java.util.logging.Logger;

public class PanoMapActivity extends MapBaseActivity
                             implements SearchListener, InputListener, GeoObjectTapListener {

    private EditText panoramaId;
    private CheckBox showStreetLayer;
    private CheckBox showAirLayer;

    private PanoramaService panoramaService;
    private PanoramaLayer panoramaLayer;
    private SearchSession searchSession;

    private static Logger LOGGER =
            Logger.getLogger("yandex.maps");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.pano_map);
        super.onCreate(savedInstanceState);

        panoramaId = (EditText)findViewById(R.id.panorama_oid);
        showStreetLayer = (CheckBox)findViewById(R.id.panomap_showstreetlayer);
        showAirLayer = (CheckBox)findViewById(R.id.panomap_showairlayer);

        showStreetLayer.setChecked(true);
        showAirLayer.setChecked(false);
        panoramaId.setOnEditorActionListener(
            new OnEditorActionListener() {
                @Override
                public boolean onEditorAction(
                    TextView view, int actionId, KeyEvent event)
                {
                    if (actionId == EditorInfo.IME_ACTION_GO) {
                        openPanorama(view.getText().toString());
                    }
                    return false;
                }
            });

        panoramaService = PlacesFactory.getInstance().createPanoramaService();
        mapview.getMap().addInputListener(this);
        panoramaLayer = PlacesFactory.getInstance().createPanoramaLayer(mapview.getMapWindow());
        showStreetLayer.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                panoramaLayer.setStreetPanoramaVisible(b);
            }
        });
        showAirLayer.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                panoramaLayer.setAirshipPanoramaVisible(b);
            }
        });
        mapview.getMap().addTapListener(this);

    }

    @Override
    public boolean onObjectTap(GeoObjectTapEvent event) {
        LOGGER.info("Object tapped");
        AirshipTapInfo info = event.getGeoObject().getMetadataContainer().getItem(AirshipTapInfo.class);
        if (info == null) {
            return false;
        }

        event.setSelected(true);
        openPanorama(info.getPanoramaId());
        return true;
    }

    @Override
    public void onMapTap(Map map, Point position) {
        LOGGER.info("Map tapped");
    }

    @Override
    public void onMapLongTap(Map map, Point position) {
        searchSession = panoramaService.findNearest(position, PanoMapActivity.this);
    }

    @Override
    public void onPanoramaSearchError(Error error) {
        LOGGER.info("Error occured");
        if (error instanceof NotFoundError) {
            Toast.makeText(getApplicationContext(),
                "No panorama found in the neighborhood of given location", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(),
                "An error has occured while searching for nearest panorama", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onPanoramaSearchResult(String panoid) {
        if (panoid.length() == 0) {
            LOGGER.info("Panorama not found");
        } else {
            LOGGER.info("Openning panorama id: " + panoid);
            openPanorama(panoid);
        }
    }

    private void openPanorama(String panoid) {
        Intent intent = new Intent(this, PanoActivity.class);
        Bundle bundle = new Bundle();

        bundle.putString("panoid", panoid);
        intent.putExtras(bundle);
        startActivity(intent);
    }
}
