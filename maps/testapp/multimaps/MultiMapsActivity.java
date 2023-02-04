package com.yandex.maps.testapp.multimaps;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;

import com.yandex.mapkit.Animation;
import com.yandex.mapkit.geometry.BoundingBox;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.TestAppActivity;

import java.util.ArrayList;

public class MultiMapsActivity extends TestAppActivity {
    private void moveMap(int viewId, BoundingBox bbox) {
        MapView mapView = (MapView)findViewById(viewId);
        Map map = mapView.getMap();
        map.move(map.cameraPosition(bbox),
                new Animation(Animation.Type.SMOOTH, ANIMATION_TIME),
                null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.multi_maps);
        moveMap(
            R.id.moscow_mapview,
            new BoundingBox(
                    new Point(55.490631, 37.298509),
                    new Point(55.957565, 37.967682)));

        moveMap(
                R.id.nizhny_mapview,
                new BoundingBox(
                        new Point(56.151532, 43.560817),
                        new Point(56.423296, 44.154442)));

        moveMap(
                R.id.london_mapview,
                new BoundingBox(
                        new Point(51.280427, -0.563154),
                        new Point(51.683977, 0.278963)));
    }

    @Override
    protected void onStopImpl() {
        ((MapView)findViewById(R.id.moscow_mapview)).onStop();
        ((MapView)findViewById(R.id.nizhny_mapview)).onStop();
        ((MapView)findViewById(R.id.london_mapview)).onStop();
    }

    @Override
    protected void onStartImpl() {
        ((MapView)findViewById(R.id.moscow_mapview)).onStart();
        ((MapView)findViewById(R.id.nizhny_mapview)).onStart();
        ((MapView)findViewById(R.id.london_mapview)).onStart();
    }

    private static final float ANIMATION_TIME = .0f;
}
