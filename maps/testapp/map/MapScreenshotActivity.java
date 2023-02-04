package com.yandex.maps.testapp.map;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import com.yandex.mapkit.Animation;
import com.yandex.mapkit.geometry.BoundingBox;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.map.MapLoadStatistics;
import com.yandex.mapkit.map.MapLoadedListener;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.mapkit.transport.masstransit.MasstransitLayer;
import com.yandex.mapkit.transport.TransportFactory;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.TestAppActivity;


public class MapScreenshotActivity extends TestAppActivity {
    private MapLoadedListener mapLoadedListener;
    private MapView mapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_screenshot);
        mapView = (MapView)findViewById(R.id.movable_map);
        final ImageView imageView = (ImageView)findViewById(R.id.static_map);
        Map map = mapView.getMap();
        map.move(map.cameraPosition(
                new BoundingBox(
                    new Point(55.769875, 37.595119),
                    new Point(55.732698, 37.657604))),
                new Animation(Animation.Type.SMOOTH, ANIMATION_TIME),
                null);

        final MasstransitLayer masstransitLayer = TransportFactory.getInstance().createMasstransitLayer(mapView.getMapWindow());
        mapLoadedListener = new MapLoadedListener() {
            @Override
            public void onMapLoaded(MapLoadStatistics statistics) {
                Log.d("yandex.maps", "onMapLoaded");
                imageView.setImageBitmap(mapView.getScreenshot());

                masstransitLayer.setVehiclesVisible(true);
            }
        };

        map.setMapLoadedListener(mapLoadedListener);
    }

    @Override
    protected void onStopImpl() {
        mapView.onStop();
    }

    @Override
    protected void onStartImpl() {
        mapView.onStart();
    }

    private static final float ANIMATION_TIME = .0f;
}
