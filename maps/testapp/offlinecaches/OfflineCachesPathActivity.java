package com.yandex.maps.testapp.offlinecaches;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.offline_cache.DataMoveListener;
import com.yandex.mapkit.offline_cache.OfflineCacheManager;
import com.yandex.runtime.DiskWriteAccessError;
import com.yandex.maps.testapp.R;

public class OfflineCachesPathActivity extends Activity {

    private static final String dataPathExtra = "dataPathExtra";

    public static Intent createActivityIntent(Activity parent, String currentPath) {
        Intent pathManageIntent = new Intent(
                parent,
                OfflineCachesPathActivity.class);
        pathManageIntent.putExtra(OfflineCachesPathActivity.dataPathExtra, currentPath);
        return pathManageIntent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.offline_caches_path_activity);

        offlineCacheManager = MapKitFactory.getInstance().getOfflineCacheManager();

        currentPathText = (EditText)findViewById(R.id.current_path);
        moveStorageButton = (Button)findViewById(R.id.move_storage);
        switchStorageButton = (Button)findViewById(R.id.switch_storage_path);
        progressLayout = (RelativeLayout)findViewById(R.id.progress_layout);
        progressBar = (ProgressBar)findViewById(R.id.progress_bar);

        currentPathText.setText(getIntent().getStringExtra(dataPathExtra));

        setListeners();
    }

    private void setListeners()
    {
        moveStorageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressLayout.setVisibility(View.VISIBLE);
                String newPath = currentPathText.getText().toString();
                moveStorageButton.setEnabled(false);
                switchStorageButton.setEnabled(false);
                offlineCacheManager.moveData(newPath, new DataMoveListener() {
                    @Override
                    public void onDataMoveProgress(int i) {
                        progressBar.setProgress(i);
                    }

                    @Override
                    public void onDataMoveCompleted() {
                        moveStorageButton.setEnabled(true);
                        switchStorageButton.setEnabled(true);
                        progressLayout.setVisibility(View.GONE);
                        finish();
                    }

                    @Override
                    public void onDataMoveError(com.yandex.runtime.Error error) {
                        String message;
                        if (error instanceof DiskWriteAccessError) {
                            message = getString(R.string.disk_write_access_error);
                        } else {
                            message = "Error: " + error.toString();
                        }

                        Toast.makeText(
                                OfflineCachesPathActivity.this,
                                message,
                                Toast.LENGTH_LONG).show();

                        moveStorageButton.setEnabled(true);
                        switchStorageButton.setEnabled(true);
                        progressLayout.setVisibility(View.GONE);
                        finish();
                    }
                });
            }
        });
        switchStorageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newPath = currentPathText.getText().toString();
                offlineCacheManager.setCachePath(newPath, new OfflineCacheManager.PathSetterListener() {
                    @Override
                    public void onPathSet() {
                        Toast.makeText(
                                OfflineCachesPathActivity.this,
                                R.string.on_path_set_success,
                                Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onPathSetError(com.yandex.runtime.Error error) {
                        Toast.makeText(
                                OfflineCachesPathActivity.this,
                                getResources().getString(R.string.on_path_set_fail) + ": " + error.toString(),
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

    }

    private OfflineCacheManager offlineCacheManager;
    private EditText currentPathText;
    private Button moveStorageButton;
    private Button switchStorageButton;
    private RelativeLayout progressLayout;
    private ProgressBar progressBar;
}
