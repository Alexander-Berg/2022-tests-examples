package com.yandex.maps.testapp.offlinecaches;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.offline_cache.OfflineCacheManager;
import com.yandex.mapkit.offline_cache.OfflineCacheManager.ClearListener;
import com.yandex.mapkit.offline_cache.OfflineCacheManager.SizeListener;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.TestAppActivity;

public class OfflineCachesSettingsActivity extends TestAppActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.offline_caches_settings);

        offlineCacheManager_ = MapKitFactory.getInstance().getOfflineCacheManager();

        allowUseCellularNetwork_ =
                (CheckedTextView) findViewById(R.id.allow_use_cellular_network);
        enableAutoUpdate_ =
                (CheckedTextView) findViewById(R.id.enable_auto_update);
        regions_ = (Button) findViewById(R.id.regions_button);
        clearCache_ = (Button) findViewById(R.id.clear_cache_button);
        calculateCacheSize_ = (Button) findViewById(R.id.calculate_cache_size_button);
        managePath_ = (Button) findViewById(R.id.manage_storage_path);
        simulateCachesUpdate_ = (Button) findViewById(R.id.simulate_caches_update);

        loadPreferences();
        setListeners();

    }

    private void loadPreferences() {
        preferences_ = getSharedPreferences(PREFERENCE_FILE_KEY, Context.MODE_PRIVATE);

        boolean allowUseCellularNetwork = preferences_.getBoolean(ALLOW_USE_CELLULAR_NETWORK_KEY, false);
        boolean enableAutoUpdate = preferences_.getBoolean(ENABLE_AUTO_UPDATE_KEY, false);

        allowUseCellularNetwork_.setChecked(allowUseCellularNetwork);
        enableAutoUpdate_.setChecked(enableAutoUpdate);

        offlineCacheManager_.allowUseCellularNetwork(allowUseCellularNetwork);
        offlineCacheManager_.enableAutoUpdate(enableAutoUpdate);

        offlineCacheManager_.addNecessaryLayersAvailableListener(
                new OfflineCacheManager.NecessaryLayersAvailableListener() {
                    @Override
                    public void onNecessaryLayersAvailable() {
                        findViewById(R.id.necessary_layers_ready_label).setVisibility(View.VISIBLE);
                    }
                }
        );
    }

    private void setListeners() {
        allowUseCellularNetwork_.setOnClickListener(new OnUseCellularNetworkClickListener());
        enableAutoUpdate_.setOnClickListener(new OnAutoUpdateClickListener());
        regions_.setOnClickListener(new OnRegionsButtonClickListener());
        clearCache_.setOnClickListener(new OnClearCacheClickListener());
        calculateCacheSize_.setOnClickListener(new OnCalculateCacheSizeClickListener());
        managePath_.setOnClickListener(new OnManagePathClickListener());
        simulateCachesUpdate_.setOnClickListener(new OnSimulateCachesUpdateClickListener());
    }

    @Override
    protected void onStopImpl(){}
    @Override
    protected void onStartImpl(){}

    private final class OnManagePathClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            offlineCacheManager_.requestPath(
                    new OfflineCacheManager.PathGetterListener() {
                        @Override
                        public void onPathReceived(String currentPath) {
                            startActivity(
                                    OfflineCachesPathActivity.createActivityIntent(
                                            OfflineCachesSettingsActivity.this, currentPath));
                        }
                    }
            );
        }
    }

    private final class OnUseCellularNetworkClickListener
            implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            allowUseCellularNetwork_.toggle();
            offlineCacheManager_.allowUseCellularNetwork(allowUseCellularNetwork_.isChecked());

            SharedPreferences.Editor editor = preferences_.edit();
            editor.putBoolean(ALLOW_USE_CELLULAR_NETWORK_KEY, allowUseCellularNetwork_.isChecked());
            editor.commit();
        }
    }

    private final class OnAutoUpdateClickListener
            implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            enableAutoUpdate_.toggle();
            offlineCacheManager_.enableAutoUpdate(enableAutoUpdate_.isChecked());

            SharedPreferences.Editor editor = preferences_.edit();
            editor.putBoolean(ENABLE_AUTO_UPDATE_KEY, enableAutoUpdate_.isChecked());
            editor.commit();
        }
    }

    private final class OnRegionsButtonClickListener
            implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(OfflineCachesSettingsActivity.this,
                    OfflineCachesRegionsActivity.class);
            startActivity(intent);
        }
    }

    private final class OnClearCacheClickListener
            implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            final AlertDialog clearCacheDialog = new AlertDialog.Builder(OfflineCachesSettingsActivity.this)
                    .setTitle("Clearing cache...")
                    .setCancelable(false)
                    .create();

            clearCacheDialog.setCanceledOnTouchOutside(false);
            clearCacheDialog.show();

            offlineCacheManager_.clear(new ClearListener() {

                @Override
                public void onClearCompleted() {
                    clearCacheDialog.dismiss();
                    new AlertDialog.Builder(OfflineCachesSettingsActivity.this)
                        .setTitle("Cache cleared")
                        .setPositiveButton("Ok", null)
                        .show();
                }
            });
        }
    }

    private final class OnCalculateCacheSizeClickListener
            implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            final AlertDialog calculatingSizeDialog = new AlertDialog.Builder(OfflineCachesSettingsActivity.this)
                        .setTitle("Calculating size...")
                        .setCancelable(false)
                        .create();

            calculatingSizeDialog.setCanceledOnTouchOutside(false);
            calculatingSizeDialog.show();

            offlineCacheManager_.computeCacheSize(new SizeListener() {
                @Override
                public void onSizeComputed(Long sizeInBytes) {
                    calculatingSizeDialog.dismiss();
                    long sizeValue = (sizeInBytes != null) ? sizeInBytes : 0;
                    new AlertDialog.Builder(OfflineCachesSettingsActivity.this)
                        .setTitle("Total cache size: " + Long.toString(sizeValue) + " bytes.")
                        .setPositiveButton("Ok", null)
                        .show();
                }
            });
        }
    }

    private final class OnSimulateCachesUpdateClickListener
            implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            offlineCacheManager_.simulateUpdate();
        }
    }

    public void removeUnnecessaryLayers(View view) {
        offlineCacheManager_.removeUnnecessaryLayers();
    }

    public void dropRegionsWithoutNecessaryLayers(View view) {
        offlineCacheManager_.dropRegionsWithoutNecessaryLayers();
    }

    private OfflineCacheManager offlineCacheManager_;
    private CheckedTextView allowUseCellularNetwork_;
    private CheckedTextView enableAutoUpdate_;
    private Button regions_;
    private Button clearCache_;
    private Button calculateCacheSize_;
    private Button managePath_;
    private Button simulateCachesUpdate_;
    private SharedPreferences preferences_;

    private final String PREFERENCE_FILE_KEY =
            "com.yandex.maps.testapp.PREFERENCE_FILE_KEY";
    private final String ALLOW_USE_CELLULAR_NETWORK_KEY =
            "ALLOW_USE_CELLULAR_NETWORK_KEY";
    private final String ENABLE_AUTO_UPDATE_KEY =
            "ENABLE_AUTO_UPDATE_KEY";
    private final int BYTES_IN_MEGABYTE = 1048576;
}
