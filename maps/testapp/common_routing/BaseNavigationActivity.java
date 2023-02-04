package com.yandex.maps.testapp.common_routing;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RelativeLayout;

import com.yandex.mapkit.ScreenPoint;
import com.yandex.mapkit.ScreenRect;
import com.yandex.mapkit.guidance_camera.Camera;
import com.yandex.mapkit.guidance_camera.CameraListener;
import com.yandex.mapkit.guidance_camera.CameraMode;
import com.yandex.mapkit.map.MapWindow;
import com.yandex.mapkit.map.SizeChangedListener;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.map.MapBaseActivity;

import androidx.annotation.NonNull;

public class BaseNavigationActivity extends MapBaseActivity
        implements CameraListener, SizeChangedListener {
    protected Camera camera;
    protected RadioButton freeModeButton;
    protected RadioButton overviewModeButton;
    protected RadioButton followingModeButton;

    protected int routesOverviewIndentDp = 0;
    protected int topButtonsSizeDp = 0;
    protected int bottomButtonsSizeDp = 0;
    protected int rightButtonsSizeDp = 0;
    protected float userYPos = 0.5f;

    protected FocusRectController focusRectController;
    protected FocusRectController overviewRectController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MapWindow mapWindow = mapview.getMapWindow();

        mapWindow.addSizeChangedListener(this);
        onMapWindowSizeChanged(mapWindow, mapWindow.width(), mapWindow.height());
    }

    @Override
    public void onCameraModeChanged() {
        CameraMode newMode = camera.cameraMode();

        switch (newMode) {
            case FOLLOWING:
                followingModeButton.setChecked(true);
                break;
            case FREE:
                freeModeButton.setChecked(true);
                break;
            case OVERVIEW:
                overviewModeButton.setChecked(true);
                break;
        }
    }


    private void resetRectControllers(int width, int height) {
        ViewGroup rootLayout = findViewById(R.id.content_frame);

        float dp = getResources().getDisplayMetrics().density;
        float topIndent = topButtonsSizeDp * dp;
        float bottomIndent = bottomButtonsSizeDp * dp;
        float rightIndent = rightButtonsSizeDp * dp;
        float routesOverviewIndent = routesOverviewIndentDp * dp;
        focusRectController = new FocusRectController(
                this, rootLayout, this::updateFocusRect,
                new ScreenRect(
                        new ScreenPoint(0, topIndent),
                        new ScreenPoint(width - rightIndent, height - bottomIndent)
                ),
                new ScreenPoint(width / 2, height * userYPos),
                width, height
        );

        overviewRectController = new FocusRectController(
                this, rootLayout, this::updateOverviewRect,
                new ScreenRect(
                        new ScreenPoint(routesOverviewIndent, topIndent + routesOverviewIndent),
                        new ScreenPoint(width - rightIndent - routesOverviewIndent,
                                height - bottomIndent - routesOverviewIndent)
                ),
                null,
                width, height
        );
    }

    @Override
    public void onMapWindowSizeChanged(@NonNull MapWindow mapWindow, int width, int height) {
        resetRectControllers(width, height);
        updateFocusRect();
        updateOverviewRect();
    }

    public void updateFocusRect() {
        mapview.getMapWindow().setFocusRect(focusRectController.getRect());
        mapview.getMapWindow().setFocusPoint(focusRectController.getFocusPoint());
    }

    public void updateOverviewRect() {
        if (camera == null)
            return;
        camera.setOverviewRect(overviewRectController.getRect(), null);
    }

    public void enableFreeMode(View view) {
        if (camera == null)
            return;
        camera.setCameraMode(CameraMode.FREE, null);
    }

    public void enableFollowingMode(View view) {
        if (camera == null)
            return;
        camera.setCameraMode(CameraMode.FOLLOWING, null);
    }

    public void enableOverviewMode(View view) {
        if (camera == null)
            return;
        camera.setCameraMode(CameraMode.OVERVIEW, null);
    }
}
