package com.yandex.maps.testapp.guidance.performance;

import android.content.Context;
import androidx.annotation.NonNull;

import com.yandex.mapkit.mapview.MapView;
import com.yandex.mapkit.directions.guidance.PerformanceMonitor;
import com.yandex.runtime.Error;
import com.yandex.runtime.network.internal.FileOperationsListener;
import com.yandex.runtime.network.internal.NetworkPlayer;
import com.yandex.runtime.network.internal.NetworkRecording;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.logging.Level;

public class ReplayingTestScenario implements
        TestScenario,
        TestScenario.TestScenarioListener {

    private final Context ctx;
    private final TestScenarioListener listener;
    private final MapView mapView;

    private TestScenario impl;

    private NetworkPlayer player = null;

    ReplayingTestScenario(final Context ctx, MapView mapView, TestScenarioListener listener) {
        this.ctx = ctx;
        this.listener = listener;
        this.mapView = mapView;
    }

    @Override
    public void execute() {
        if (player != null) {
            listener.onError("Replaying was restarted!", Level.WARNING);
            player.stop();
        }

        player = NetworkRecording.createPlayer();

        if (player == null || !player.isValid()) {
            listener.onError("Error while attempting to replay traffic!", Level.SEVERE);
            return;
        }

        try {
            final String filePath = Utilities.getNetworkRecordFilePath(ctx);
            player.play(filePath, new FileOperationsListener() {
                @Override
                public void onSuccess() {
                    startReplaying();
                }

                @Override
                public void onError(@NonNull Error error) {
                    listener.onError("Error reading from the " + filePath, Level.WARNING);
                    player = null;
                }
            });
        } catch (FileNotFoundException ex) {
            listener.onError(ex.getMessage(), Level.WARNING);
            player = null;
        }
    }

    @Override
    public void stop() {
        if (impl != null) {
            impl.stop();
            stopReplaying();
            impl = null;
        }
    }

    @Override
    public void onScenarioFinished(List<PerformanceMonitor> performanceMonitors) {
        stopReplaying();
        listener.onScenarioFinished(performanceMonitors);
    }

    @Override
    public void onError(String error, Level level) {
        listener.onError(error, level);
    }

    private void startReplaying() {
        impl = new TestScenarioImpl(ctx, mapView, this);
        impl.execute();
    }

    private void stopReplaying() {
        if (player != null) {
            player.stop();
            player = null;
        }
    }
}
