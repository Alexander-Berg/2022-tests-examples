package com.yandex.maps.testapp.map;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.coverage.Coverage;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.CameraListener;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.CameraUpdateReason;
import com.yandex.mapkit.map.InputListener;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.map.MapLoadedListener;
import com.yandex.maps.testapp.R;
import com.yandex.runtime.bindings.Serialization;
import com.yandex.mapkit.logo.Alignment;
import com.yandex.mapkit.logo.HorizontalAlignment;
import com.yandex.mapkit.logo.VerticalAlignment;

import java.util.logging.Logger;

public class MapActivity extends MapBaseActivity {
    public static String CAMERA_TARGET_EXTRA = "cameraTarget";
    public static int PICK_CAMERA_TARGET = 0;
    private static final Logger LOGGER = Logger.getLogger("yandex.maps");

    private TextView gestureStatusView_ = null;
    private TextView loadedTimeView_ = null;

    private RegionIdWidget regionIdWidget;
    private InputListener inputListener = new InputListener() {
        @Override
        public void onMapTap(Map map, Point point) {}

        @Override
        public void onMapLongTap(Map map, Point point) {
            moveMap(point);
        }
    };

    private final CameraListener cameraListener = new CameraListener() {
        @Override
        public void onCameraPositionChanged(Map map, CameraPosition position,
                CameraUpdateReason updateReason, boolean finished) {
            if (finished) {
                active = false;
                gestureStatusView_.setTextColor(Color.RED);
                gestureStatusView_.setText("stopped");
            } else {
                // receive 60 times in a second
                if (!active) {
                    gestureStatusView_.setTextColor(Color.BLACK);
                    gestureStatusView_.setText("move");
                    active = true;
                }
            }
        }

        boolean active = false;
    };

    private MapLoadedListener mapLoadStopwatch;

    public void onMoveClick(View view) {
        Intent intent = new Intent(this, MapCoordinatesActivity.class);
        startActivityForResult(intent, PICK_CAMERA_TARGET);
    }

    private void moveMap(Point point) {
        moveMap(point, 1.0f);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_CAMERA_TARGET && resultCode == RESULT_OK) {
            byte[] serializedTarget = data.getByteArrayExtra(CAMERA_TARGET_EXTRA);
            Point cameraTarget = Serialization.deserializeFromBytes(
                serializedTarget,
                Point.class);
            moveMap(cameraTarget);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.map);
        super.onCreate(savedInstanceState);

        byte[] serializedTarget = getIntent().getByteArrayExtra(CAMERA_TARGET_EXTRA);
        if (serializedTarget != null) {
            Point cameraTarget = Serialization.deserializeFromBytes(
                serializedTarget,
                Point.class);
            moveMap(cameraTarget);
        }

        mapview.getMap().getLogo().setAlignment(
                new Alignment(HorizontalAlignment.CENTER, VerticalAlignment.BOTTOM));

        mapview.getMap().addInputListener(inputListener);

        gestureStatusView_ = (TextView)findViewById(R.id.gesture_status);
        loadedTimeView_ = (TextView)findViewById(R.id.map_loaded_time);

        TextView regionIdView = (TextView)findViewById(R.id.region_id);
        Coverage coverage = MapKitFactory.getInstance().createTrafficLevelCoverage();
        regionIdWidget = new RegionIdWidget(regionIdView,
                coverage,
                mapview.getMap(),
                getString(R.string.region_empty),
                getString(R.string.region_error));

        ToggleButton zoomEnabledModeToggle = (ToggleButton)findViewById(R.id.zoom_enabled);
        zoomEnabledModeToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mapview.getMap().setZoomGesturesEnabled(isChecked);
            }
        });

        ToggleButton scrollEnabledToggle = (ToggleButton)findViewById(R.id.scroll_enabled);
        scrollEnabledToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mapview.getMap().setScrollGesturesEnabled(isChecked);
            }
        });

        ToggleButton tiltEnabledToggle = (ToggleButton)findViewById(R.id.tilt_enabled);
        tiltEnabledToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mapview.getMap().setTiltGesturesEnabled(isChecked);
            }
        });

        ToggleButton rotateEnabledToggle = (ToggleButton)findViewById(R.id.rotate_enabled);
        rotateEnabledToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mapview.getMap().setRotateGesturesEnabled(isChecked);
            }
        });

        mapview.getMap().addCameraListener(cameraListener);
        mapLoadStopwatch = new MapLoadStopwatch(this, loadedTimeView_);
        mapview.getMap().setMapLoadedListener(mapLoadStopwatch);
    }
}
