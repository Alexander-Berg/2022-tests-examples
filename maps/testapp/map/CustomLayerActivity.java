package com.yandex.maps.testapp.map;

import android.app.Activity;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.ToggleButton;
import java.util.HashMap;

import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.TileId;
import com.yandex.mapkit.Version;
import com.yandex.mapkit.layers.Layer;
import com.yandex.mapkit.layers.LayerOptions;
import com.yandex.mapkit.layers.OverzoomMode;
import com.yandex.mapkit.map.MapType;
import com.yandex.mapkit.tiles.UrlProvider;
import com.yandex.mapkit.images.DefaultImageUrlProvider;
import com.yandex.mapkit.geometry.geo.Projections;
import com.yandex.mapkit.geometry.geo.XYPoint;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.mapkit.traffic.TrafficLayer;
import com.yandex.maps.testapp.R;

public class CustomLayerActivity extends Activity {
    private UrlProvider tileUrlProvider;
    private DefaultImageUrlProvider imageUrlProvider;
    private Layer layer;
    private MapView mapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.map_custom_layer);
        super.onCreate(savedInstanceState);

        mapView = (MapView)findViewById(R.id.mapview);
        mapView.getMap().setMapType(MapType.NONE);

        tileUrlProvider = new UrlProvider() {
            @Override
            public String formatUrl(TileId tileId, Version version) {
                return "https://maps-ios-pods-public.s3.yandex.net/mapkit_logo.png";
            }
        };
        imageUrlProvider = new DefaultImageUrlProvider();

        layer = mapView.getMap().addLayer(
            "static",
            "image/png",
            new LayerOptions().setOverzoomMode(OverzoomMode.ENABLED),
            tileUrlProvider,
            imageUrlProvider,
            Projections.getSphericalMercator());
        layer.invalidate("0.0.0");
    }

    @Override
    protected void onStop() {
        mapView.onStop();
        MapKitFactory.getInstance().onStop();
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        MapKitFactory.getInstance().onStart();
        mapView.onStart();
    }
}
