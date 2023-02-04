package com.yandex.maps.testapp.guidance.performance;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.yandex.mapkit.directions.guidance.PerformanceMonitor;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.map.MapBaseActivity;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class GuidancePerformanceTestActivity
        extends MapBaseActivity
        implements TestScenario.TestScenarioListener {

    private static final Logger LOGGER = Logger.getLogger("yandex.maps.guidance");

    private enum ExecutionMode {
        Idle,
        PlayingOnline,
        ReplayingOffline
    }

    private TestScenario testScenario = null;
    private TestScenario.TestScenarioListener testScenarioListener = null;
    private ToggleButton recordingToggle = null;
    private ToggleButton replayingToggle = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.guidance_performance_test);
        super.onCreate(savedInstanceState);

        recordingToggle = findViewById(R.id.recording_toggle);
        replayingToggle = findViewById(R.id.replaying_toggle);
    }

    @Override
    public void onScenarioFinished(List<PerformanceMonitor> performanceMonitors) {
        setMode(ExecutionMode.Idle);
        showMessage("Scenario finished", Level.INFO);

        if (testScenarioListener != null) {
            testScenarioListener.onScenarioFinished(performanceMonitors);
        }
    }

    @Override
    public void onError(String error, Level level) {
        showMessage(error, level);

        if (level == Level.SEVERE) {
            setMode(ExecutionMode.Idle);
        }

        if (testScenarioListener != null) {
            testScenarioListener.onError(error, level);
        }
    }

    public void onRecordingToggle(View view) {
        if (testScenario == null) {
            setMode(ExecutionMode.PlayingOnline);
        } else {
            setMode(ExecutionMode.Idle);
        }
    }

    public void onReplayingToggle(View view) {
        if (testScenario == null) {
            setMode(ExecutionMode.ReplayingOffline);
        } else {
            setMode(ExecutionMode.Idle);
        }
    }

    public void setTestScenarioListener(TestScenario.TestScenarioListener testScenarioListener) {
        this.testScenarioListener = testScenarioListener;
    }

    public void executeScenario() {
        if (testScenario != null) {
            testScenario.stop();
        }

        setMode(ExecutionMode.ReplayingOffline);
    }

    private void showMessage(String message, Level level) {
        if (level != Level.OFF)
            LOGGER.log(level, message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void setMode(ExecutionMode mode) {
        if (mode == ExecutionMode.Idle) {
            if (testScenario != null) {
                testScenario.stop();
                testScenario = null;
            }

            recordingToggle.setChecked(false);
            recordingToggle.setEnabled(true);

            replayingToggle.setChecked(false);
            replayingToggle.setEnabled(true);
        } else if (mode == ExecutionMode.PlayingOnline) {
            testScenario = new TestScenarioImpl(this, mapview, this);
            testScenario.execute();
            showMessage("Playing online started", Level.INFO);

            recordingToggle.setChecked(true);
            recordingToggle.setEnabled(true);

            replayingToggle.setChecked(false);
            replayingToggle.setEnabled(false);
        } else if (mode == ExecutionMode.ReplayingOffline) {
            testScenario = new ReplayingTestScenario(this, mapview, this);
            testScenario.execute();
            showMessage("Replaying offline started", Level.INFO);

            recordingToggle.setChecked(false);
            recordingToggle.setEnabled(false);

            replayingToggle.setChecked(true);
            replayingToggle.setEnabled(true);
        }
    }
}
