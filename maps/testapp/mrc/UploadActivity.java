package com.yandex.maps.testapp.mrc;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;

import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.auth.AuthUtil;
import com.yandex.mrc.ride.MRCFactory;
import com.yandex.mrc.StorageManager;
import com.yandex.mrc.StorageManager.DataMoveListener;
import com.yandex.mrc.StorageManager.ErrorListener;
import com.yandex.mrc.StorageManager.PathListener;
import com.yandex.mrc.UnavailableForLegalReasonsError;
import com.yandex.mrc.UploadManager;
import com.yandex.mrc.UploadManagerListener;
import com.yandex.runtime.Error;
import com.yandex.runtime.logging.Logger;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class UploadActivity extends BaseMrcActivity implements UploadManagerListener, DataMoveListener, PathListener, ErrorListener {
    private final String PREF_CELLULAR_UPLOAD = "com.yandex.maps.testapp.mrc.CELLULAR_UPLOAD_PREF";
    private static final String RESULTS_SUBDIR = "/mrc";

    private TextView uploadStatusTextView;
    private TextView ridePhotosCountTextView;
    private TextView placemarksCountTextView;
    private TextView placemarkPhotosCountTextView;
    private TextView resultsDirPathTextView;
    private Button uploadStartStopButton;
    private Button moveDataButton;
    private CheckBox allowCellularCheckbox;

    private UploadManager uploadManager;
    private PhotosCountWatcher photosCountWatcher;
    private SharedPreferences sharedPreferences;
    private StorageInfoProvider storageInfoProvider;
    private StorageManager storageManager;

    enum ResultsStorage {
        InternalMemory,
        SdCard
    }

    ResultsStorage currentResultsStorage = null;
    private File currentResultsDir = null;
    private AlertDialog alertDialog = null;

    private PhotosCountWatcher.PhotosCountChangedListener photosCountChangedListener
            = new PhotosCountWatcher.PhotosCountChangedListener() {
        @Override
        public void onPhotosCountChanged() {
            ridePhotosCountTextView.setText(getString(R.string.mrc_photos_count,
                    photosCountWatcher.getRidePhotosCount()));
            placemarksCountTextView.setText(getString(R.string.mrc_placemarks_count,
                    photosCountWatcher.getPlacemarksCount()));
            placemarkPhotosCountTextView.setText(getString(R.string.mrc_placemark_photos_count,
                    photosCountWatcher.getPlacemarkPhotosCount()));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mrc_upload);

        uploadStartStopButton = findViewById(R.id.start_stop_upload_button);
        moveDataButton = findViewById(R.id.start_data_move_button);
        ridePhotosCountTextView = findViewById(R.id.ride_photos_count);
        placemarksCountTextView = findViewById(R.id.placemarks_count);
        placemarkPhotosCountTextView = findViewById(R.id.placemark_photos_count);
        uploadStatusTextView = findViewById(R.id.upload_status);
        resultsDirPathTextView = findViewById(R.id.results_storage_path);
        allowCellularCheckbox = findViewById(R.id.allow_cellular_checkbox);

        storageInfoProvider = new StorageInfoProvider(this);

        uploadManager = MRCFactory.getInstance().getUploadManager();
        Logger.info("onCreate, UploadManager state: " + uploadManager.getUploadingState());
        photosCountWatcher = new PhotosCountWatcher();
        storageManager = MRCFactory.getInstance().getStorageManager();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        allowCellularCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (!compoundButton.isPressed()) {
                    return;
                }
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean(PREF_CELLULAR_UPLOAD, isChecked);
                editor.apply();
                uploadManager.setCellularNetworksAccess(isChecked
                        ? UploadManager.CellularNetworksAccess.ALLOW
                        : UploadManager.CellularNetworksAccess.DISALLOW);
            }
        });

        storageManager.requestPath(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (AuthUtil.getCurrentAccount() == null) {
            showUserMessage(R.string.sign_into_account);
            finish();
        }
    }

    @Override
    protected void onResume() {
        Logger.info("UploadingActivity.onResume");

        super.onResume();
        MRCFactory.getInstance().onResume();
        uploadManager.subscribe(this);
        storageManager.addDataMoveListener(this);
        storageManager.addErrorListener(this);
        photosCountWatcher.subscribe(photosCountChangedListener);
        boolean allowCellular = sharedPreferences.getBoolean(PREF_CELLULAR_UPLOAD, false);

        uploadManager.setCellularNetworksAccess(allowCellular
                ? UploadManager.CellularNetworksAccess.ALLOW
                : UploadManager.CellularNetworksAccess.DISALLOW);
        allowCellularCheckbox.setChecked(allowCellular);

        uploadManager.stop();
        onUploadingStateChanged();
    }

    @Override
    protected void onPause() {
        Logger.info("UploadingActivity.onPause");

        uploadManager.stop();
        photosCountWatcher.unsubscribe();
        uploadManager.unsubscribe(this);
        storageManager.removeDataMoveListener(this);
        storageManager.removeErrorListener(this);
        MRCFactory.getInstance().onPause();
        super.onPause();
    }

    public void onUploadClick(View view) {
        Logger.info("Uploading control clicked");

        switch (uploadManager.getUploadingState()) {
            case STOPPED:
                uploadManager.start();
                break;
            case ACTIVE:
            case DELAYED:
                uploadManager.stop();
                break;
        }
    }

    public void onDataMoveClick(View view) {
        StorageInfo info = null;
        if (currentResultsStorage == ResultsStorage.InternalMemory) {
            info = storageInfoProvider.getRemovableStorage();
        } else if (currentResultsStorage == ResultsStorage.SdCard) {
            info = storageInfoProvider.getBuiltInStorage();
        }
        if (info != null) {
            File newPath = new File(info.path, RESULTS_SUBDIR);
            moveData(newPath);
        }
    }

    private void moveData(File newResultsDir) {
        try {
            if (currentResultsDir == null ||
                    !currentResultsDir.getCanonicalFile().equals(newResultsDir.getCanonicalFile())) {
                storageManager.moveData(newResultsDir.getPath());
                showUserMessage(R.string.mrc_data_move_started);
            }
        } catch (IOException e) {
            showUserMessage(R.string.mrc_data_move_error);
        }
    }

    private void showStopped() {
        uploadStatusTextView.setText(R.string.mrc_upload_status_stopped);
        uploadStartStopButton.setText(R.string.mrc_start_upload);
    }

    private void showUploading() {
        uploadStatusTextView.setText(R.string.mrc_upload_status_active);
        uploadStartStopButton.setText(R.string.mrc_stop_upload);
    }

    private void showWaitForNetworkConnection() {
        uploadStatusTextView.setText(R.string.mrc_upload_status_delayed);
        uploadStartStopButton.setText(R.string.mrc_stop_upload);
    }

    @Override
    public void onCurrentUploadingItemChanged() {

    }

    @Override
    public void onUploadingQueueChanged() {

    }

    @Override
    public void onUploadingStateChanged() {
        Logger.info("Uploading state changed: " + uploadManager.getUploadingState());
        switch (uploadManager.getUploadingState()) {
            case STOPPED:
                showStopped();
                break;
            case ACTIVE:
                showUploading();
                break;
            case DELAYED:
                showWaitForNetworkConnection();
                break;
        }
    }

    @Override
    public void onSizeCalculated(@NotNull List<String> uploadingIds, long sizeBytes, long photosCount) {

    }

    @Override
    public void onClearCompleted(@NotNull List<String> uploadingIds) {

    }

    @Override
    public void onPathReceived(@NotNull String path) {
        if (path.isEmpty())
            return;

        File file = new File(path);
        currentResultsDir = file;
        try {
            Boolean isRemoveable = StorageInfoProvider.isRemovalbe(file);

            if (isRemoveable) {
                currentResultsStorage = ResultsStorage.SdCard;
                resultsDirPathTextView.setText(getString(R.string.mrc_results_storage_path, "SD card"));
            } else {
                currentResultsStorage = ResultsStorage.InternalMemory;
                resultsDirPathTextView.setText(getString(R.string.mrc_results_storage_path, "internal memory"));
            }
        } catch (IllegalArgumentException e) {
            Logger.warn("Bad results path: " + path);
            return;
        }

        // If current path is available and there is only one storage type
        // (e.g. no SD card exists), disable "Move data" button
        if ((storageInfoProvider.getBuiltInStorage() == null ||
                storageInfoProvider.getRemovableStorage() == null)) {
            moveDataButton.setEnabled(false);
        }
    }

    @Override
    public void onDataMoveProgress(int percent) {
        if (resultsDirPathTextView != null) {
            resultsDirPathTextView.setText(getString(R.string.mrc_data_move_progress, percent) + "%%");
        }
    }

    @Override
    public void onDataMoveCompleted() {
        showUserMessage(R.string.mrc_data_move_completed);
        // Replace the progress with the new path in UI.
        storageManager.requestPath(this);
    }

    @Override
    public void onDataMoveError(Error error) {
        Logger.error("Data move error: " + error.toString());
        showUserMessage(ErrorHandlerHelper.getErrorMessageResId(error));
        storageManager.requestPath(this);
    }

    @Override
    public void onStorageError(Error error) {
        Logger.error(error.toString());
        showUserMessage(ErrorHandlerHelper.getErrorMessageResId(error));
    }

    @Override
    public void onUploadingError(@NonNull Error error) {
        Logger.error(error.toString());

        if (error instanceof UnavailableForLegalReasonsError) {
            UnavailableForLegalReasonsError legalError = (UnavailableForLegalReasonsError) error;
            if (legalError.getCode() == UnavailableForLegalReasonsError.Code.LEGAL_REASON_PHONE_REQUIRED) {
                showLegalErrorDialog(legalError.getDescription());
                return;
            }
        }

        showUserMessage(ErrorHandlerHelper.getErrorMessageResId(error));
    }

    @Override
    public void onDataOperationError(@NonNull Error error) {
        Logger.error(error.toString());
        showUserMessage(ErrorHandlerHelper.getErrorMessageResId(error));
    }

    public void showLegalErrorDialog(String text) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.mrc_upload_unavailable)
                .setMessage(text)
                .setPositiveButton(R.string.mrc_bind_phone, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();

                        String url = getString(R.string.mrc_bind_phone_url);
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(url));
                        startActivity(intent);

                        alertDialog = null;
                        finish();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        alertDialog = null;
                        finish();
                    }
                });
        alertDialog = builder.create();
        alertDialog.show();
    }

    protected void showUserMessage(int resId) {
        Toast.makeText(this, getString(resId), Toast.LENGTH_LONG).show();
    }

    protected void showUserMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
