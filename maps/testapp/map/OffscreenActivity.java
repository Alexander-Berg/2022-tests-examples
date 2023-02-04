package com.yandex.maps.testapp.map;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.MapLoadStatistics;
import com.yandex.mapkit.map.MapType;
import com.yandex.mapkit.map.OffscreenMapWindow;
import com.yandex.mapkit.map.MapLoadedListener;
import com.yandex.mapkit.map.Map;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.TestAppActivity;

public class OffscreenActivity extends TestAppActivity
{
    private static final Point CAMERA_TARGET = new Point(59.945933, 30.320045);
    private static final CameraPosition CAMERA_POSITION = new CameraPosition(CAMERA_TARGET, 12.f, 0.f, 0.f);

    private ImageView imageView;
    private OffscreenMapWindow offscreenMapWindow;
    private MapLoadedListener mapLoadedListener;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.offscreen);
        imageView = findViewById(R.id.image);

        offscreenMapWindow = MapKitFactory.getInstance().createOffscreenMapWindow(1024, 1024);

        mapLoadedListener = statistics -> imageView.setImageBitmap(offscreenMapWindow.captureScreenshot());

        Map map = offscreenMapWindow.getMapWindow().getMap();
        map.move(CAMERA_POSITION);
        map.setMapType(MapType.VECTOR_MAP);
        map.setMapLoadedListener(mapLoadedListener);
    }
    @Override
    protected void onStartImpl() {
    }

    @Override
    protected void onStopImpl() {
    }
}
