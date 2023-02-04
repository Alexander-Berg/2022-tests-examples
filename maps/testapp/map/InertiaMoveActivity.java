package com.yandex.maps.testapp.map;

import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.NonNull;

import android.widget.TextView;

import com.yandex.mapkit.map.CameraListener;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.CameraUpdateReason;
import com.yandex.mapkit.map.InertiaMoveListener;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.maps.testapp.R;
import com.yandex.runtime.image.ImageProvider;

import java.util.logging.Logger;

import static com.yandex.maps.testapp.map.BitmapHelpers.createFilledCircle;
import static com.yandex.maps.testapp.map.BitmapHelpers.getDensity;

public class InertiaMoveActivity extends MapBaseActivity {
    private static final Logger LOGGER = Logger.getLogger("yandex.maps");
    private TextView finishView;
    private TextView cancelView;
    private PlacemarkMapObject targetPlacemark;
    private PlacemarkMapObject inertiaFinishPlacemark;

    private final CameraListener cameraListener = new CameraListener() {
        @Override
        public void onCameraPositionChanged(
                @NonNull Map map, @NonNull CameraPosition cameraPosition,
                @NonNull CameraUpdateReason cameraUpdateReason, boolean finished) {
            targetPlacemark.setGeometry(cameraPosition.getTarget());
        }
    };

    private final InertiaMoveListener inertiaMoveListener = new InertiaMoveListener() {
        @Override
        public void onStart(@NonNull Map map, @NonNull CameraPosition finishCameraPosition) {
            LOGGER.info("onStart");
            inertiaFinishPlacemark.setGeometry(finishCameraPosition.getTarget());
            inertiaFinishPlacemark.setVisible(true);
            finishView.setAlpha(0.0f);
        }

        @Override
        public void onCancel(@NonNull Map map, @NonNull CameraPosition cameraPosition) {
            LOGGER.info("onCancel");
            cancelView.setAlpha(1.0f);
            finishView.setAlpha(0.0f);
        }

        @Override
        public void onFinish(@NonNull Map map, @NonNull CameraPosition cameraPosition) {
            LOGGER.info("onFinish");
            cancelView.setAlpha(0.0f);
            finishView.setAlpha(1.0f);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.inertia_move);
        super.onCreate(savedInstanceState);


        Map map = mapview.getMap();
        map.addInertiaMoveListener(inertiaMoveListener);
        map.addCameraListener(cameraListener);

        cancelView = findViewById(R.id.cancel_view);
        finishView = findViewById(R.id.finish_view);
        cancelView.setAlpha(0.0f);
        finishView.setAlpha(0.0f);

        inertiaFinishPlacemark = map.getMapObjects().addPlacemark(
                map.getCameraPosition().getTarget(), ImageProvider.fromBitmap(
                        createFilledCircle(Color.RED, 40, getDensity(this))));
        inertiaFinishPlacemark.setVisible(false);

        targetPlacemark = map.getMapObjects().addPlacemark(map.getCameraPosition().getTarget());
        targetPlacemark.setZIndex(100);
    }
}
