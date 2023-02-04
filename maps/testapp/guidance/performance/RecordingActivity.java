package com.yandex.maps.testapp.guidance.performance;

import android.app.Activity;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.yandex.maps.testapp.R;
import com.yandex.runtime.Error;
import com.yandex.runtime.network.internal.FileOperationsListener;
import com.yandex.runtime.network.internal.NetworkRecorder;
import com.yandex.runtime.network.internal.NetworkRecording;

import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;

class RecorderHolder {

    private static final RecorderHolder instance = new RecorderHolder();

    private final NetworkRecorder recorder = NetworkRecording.createRecorder();
    private boolean isRecording = false;

    static RecorderHolder instance() {
        return instance;
    }

    boolean isRecording() {
        return isRecording;
    }

    void startNetworkRecording() {
        if (checkValid()) {
            recorder.start();
            isRecording = true;
        }
    }

    void stopNetworkRecording() {
        if (checkValid()) {
            recorder.stop();
            isRecording = false;
        }
    }

    void dump(String filePath, FileOperationsListener listener) {
        if (checkValid()) {
            recorder.dump(filePath, listener);
        }
    }

    private boolean checkValid() {
        boolean isValid = recorder != null && recorder.isValid();
        if (!isValid) {
            isRecording = false;
        }
        return isValid;
    }
}

public class RecordingActivity
        extends Activity {

    private static final Logger LOGGER = Logger.getLogger("yandex.maps.guidance");

    private ToggleButton recordingToggle = null;
    private Button saveButton = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.recording_activity);
        super.onCreate(savedInstanceState);

        recordingToggle = findViewById(R.id.recording_toggle);
        saveButton = findViewById(R.id.recording_save_button);

        recordingToggle.setChecked(RecorderHolder.instance().isRecording());
        saveButton.setEnabled(!RecorderHolder.instance().isRecording());
    }

    public void onRecordingToggle(View view) {
        if (recordingToggle.isChecked()) {
            RecorderHolder.instance().startNetworkRecording();
        } else {
            RecorderHolder.instance().stopNetworkRecording();
        }

        recordingToggle.setChecked(RecorderHolder.instance().isRecording());
        saveButton.setEnabled(!RecorderHolder.instance().isRecording());
    }

    public void onSaveButton(View view) {
        final String filePath;
        try {
            filePath = Utilities.getNetworkRecordFilePath(this);
            RecorderHolder.instance().dump(filePath, new FileOperationsListener()
            {
                @Override
                public void onSuccess() {
                    showMessage("File saved to " + filePath, Level.WARNING);
                }

                @Override
                public void onError(@NonNull Error error) {
                    showMessage("Failed to save file", Level.SEVERE);
                }
            });
        } catch (FileNotFoundException ex) {
            showMessage(ex.getMessage(), Level.SEVERE);
        }
    }

    private void showMessage(String message, Level level) {
        if (level != Level.OFF)
            LOGGER.log(level, message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
