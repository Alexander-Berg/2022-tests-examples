package com.yandex.maps.testapp.storage;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.TextView;

import com.yandex.mapkit.Animation;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.CameraListener;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.CameraUpdateReason;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.map.MapType;
import com.yandex.mapkit.storage.StorageManager;
import com.yandex.mapkit.storage.StorageManager.ClearListener;
import com.yandex.mapkit.storage.StorageManager.SizeListener;
import com.yandex.maps.testapp.R;

import com.yandex.maps.testapp.map.MapBaseActivity;

public class StorageActivity extends MapBaseActivity {
    private StorageManager storageManager;
    private CameraListener cameraListener;
    private SizeListener sizeListener;
    private Button clearCache;
    private EditText limitTextField;
    private Button updateCacheLimit;
    private TextView sizeText;
    private Long currCacheSize;
    private Long maxCacheSize;

    private final Point topLeft = new Point(55.861944, 37.415528);
    private final double rightBorder = 37.766839;
    private final float zoomStep = 0.5f;

    private final int BYTES_IN_MEGABYTE = 1024 * 1024;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.storage);
        super.onCreate(savedInstanceState);

        storageManager = MapKitFactory.getInstance().getStorageManager();

        clearCache = findViewById(R.id.clear_cache_button);
        limitTextField = findViewById(R.id.current_limit);
        updateCacheLimit = findViewById(R.id.update_cache_limit);
        sizeText = findViewById(R.id.size_text_view);

        setUiListeners();

            storageManager.maxTileStorageSize(new SizeListener() {
            @Override
            public void onSuccess(Long sizeInBytes) {
                maxCacheSize = (sizeInBytes != null) ? sizeInBytes / BYTES_IN_MEGABYTE : 0;
                limitTextField.setText(Long.toString(maxCacheSize));
            }

            @Override
            public void onError(com.yandex.runtime.Error error) {
                Toast.makeText(
                    StorageActivity.this,
                    "Error: " + error.toString(),
                    Toast.LENGTH_LONG).show();
            }
        });

        sizeListener = new SizeListener() {
            @Override
            public void onSuccess(Long sizeInBytes) {
                currCacheSize = sizeInBytes / BYTES_IN_MEGABYTE;
                sizeText.setText("Cache size, Mb: " + currCacheSize.toString());
            }

            @Override
            public void onError(com.yandex.runtime.Error error) {
                Toast.makeText(
                    StorageActivity.this,
                    "Error: " + error.toString(),
                    Toast.LENGTH_LONG).show();
            }
        };

        storageManager.computeSize(sizeListener);

        cameraListener = (map, cameraPosition, cameraUpdateReason, finished) -> {
            if (finished) {
                storageManager.computeSize(sizeListener);
            }
        };

        mapview.getMap().addCameraListener(cameraListener);
    }

    private void setUiListeners() {
        clearCache.setOnClickListener(new OnClearCacheClickListener());
        updateCacheLimit.setOnClickListener(new OnUpdateCacheLimitClickListener());
    }

    private final class OnClearCacheClickListener
        implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            final AlertDialog clearCacheDialog = new AlertDialog.Builder(StorageActivity.this)
                .setTitle("Clearing cache...")
                .setCancelable(false)
                .create();

            clearCacheDialog.setCanceledOnTouchOutside(false);
            clearCacheDialog.show();

            storageManager.clear(() -> {
                clearCacheDialog.dismiss();
                new AlertDialog.Builder(StorageActivity.this)
                    .setTitle("Cache cleared")
                    .setPositiveButton("Ok", null)
                    .show();
                storageManager.computeSize(sizeListener);
            });
        }
    }

    private final class OnUpdateCacheLimitClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            String limitString = limitTextField.getText().toString();
            try {
                Long limit = Long.parseLong(limitString);
                if (limit < 0) {
                    new AlertDialog.Builder(StorageActivity.this)
                        .setTitle("Error: Storage limitation < 0")
                        .setPositiveButton("Ok", null)
                        .show();
                }

                storageManager.setMaxTileStorageSize(limit * BYTES_IN_MEGABYTE, new SizeListener() {
                    @Override
                    public void onSuccess(Long sizeInBytes) {
                        maxCacheSize = limit;
                    }

                    @Override
                    public void onError(com.yandex.runtime.Error error) {
                        Toast.makeText(
                            StorageActivity.this,
                            "Error: Failed to reduce cache size limit",
                            Toast.LENGTH_LONG).show();
                    }
                });

            } catch (NumberFormatException e) {
                new AlertDialog.Builder(StorageActivity.this)
                    .setTitle("Error: Unable to parse number from " + limitString)
                    .setPositiveButton("Ok", null)
                    .show();
            }
        }
    }

    public void fillCache(View sender) {
        float time = 0.3f;
        double diffDegree = 0.001;
        if (mapview.getMap().getMapType() == MapType.VECTOR_MAP) {
            time = 0.1f;
            diffDegree = 0.005;
        }
        move(topLeft, mapview.getMap().getMaxZoom(), new Animation(Animation.Type.LINEAR, time), diffDegree);
    }

    private void move(Point point, float zoom, Animation animation, double diffDegree) {
        if (currCacheSize >= maxCacheSize || point.getLatitude() <= 0.0) {
            return;
        }

        mapview.getMap().move(new CameraPosition(point, zoom, 0.0f, 60.0f), animation, completed -> {
            if (completed) {
                double latitude = point.getLatitude();
                double longitude = point.getLongitude() + diffDegree;

                if (longitude > rightBorder) {
                    longitude = topLeft.getLongitude();
                    latitude -= diffDegree;
                }

                final Point p = new Point(latitude, longitude);
                float z = zoom;
                final double minZoom = mapview.getMap().getMinZoom() + 5.0f;
                if (zoom > minZoom) {
                    z -= zoomStep;
                } else {
                    z = mapview.getMap().getMaxZoom();
                }

                move(p, z, animation, diffDegree);
            }
        });
    }
}
