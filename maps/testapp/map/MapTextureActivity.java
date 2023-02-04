package com.yandex.maps.testapp.map;

import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.mapview.MapTexture;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.map.MapType;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.TestAppActivity;

import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnTouchListener;

import androidx.annotation.NonNull;

public class MapTextureActivity extends TestAppActivity implements TextureView.SurfaceTextureListener, OnTouchListener
{
    private static final Point CAMERA_TARGET = new Point(59.945933, 30.320045);
    private static final CameraPosition CAMERA_POSITION = new CameraPosition(CAMERA_TARGET, 12.f, 0.f, 0.f);

    private TextureView textureView;
    private MapTexture mapTexture;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_texture);
        textureView = findViewById(R.id.textureView);
        textureView.setSurfaceTextureListener(this);
        textureView.setOnTouchListener(this);
        mapTexture = new MapTexture(getApplicationContext());
    }

    @Override
    protected void onStartImpl() {
        mapTexture.onStart();
    }

    @Override
    protected void onStopImpl() {
        mapTexture.onStop();
    }

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
        mapTexture.setTexture(surfaceTexture, width, height);

        Map map = mapTexture.getMap();
        map.move(CAMERA_POSITION);
        map.setMapType(MapType.VECTOR_MAP);
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
        mapTexture.onTextureSizeChanged(width, height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
        mapTexture.removeTexture();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {

    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        return mapTexture.onTouchEvent(motionEvent);
    }
}
