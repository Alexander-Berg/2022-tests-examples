package com.yandex.maps.testapp.map;

import androidx.annotation.NonNull;

import com.yandex.mapkit.Animation;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.map.MapLoadStatistics;
import com.yandex.mapkit.map.MapLoadedListener;
import com.yandex.mapkit.map.MapWindow;
import com.yandex.mapkit.mapview.MapSurface;
import com.yandex.mapkit.mapview.MapView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PerformanceTestScenario {
    public interface Callback {
        void onScenarioFinished(final String scenarioName, final String renderResult, final String decoderResult);
    }

    PerformanceTestScenario(
            MapWindow mapWindow, Callback callback, PerformanceTestStep[] steps, String name) {
        this(mapWindow, callback, Arrays.asList(steps), name);
    }

    PerformanceTestScenario(
            MapWindow mapWindow, Callback callback, String name) {
        this(mapWindow, callback, new ArrayList<PerformanceTestStep>(), name);
    }

    PerformanceTestScenario(
            MapWindow mapWindow, Callback callback, List<PerformanceTestStep> steps, String name) {
        this.mapWindow = mapWindow;
        this.callback = callback;
        this.name = name;
        this.steps = steps;
    }

    protected void preExecute(Runnable onComplete) {
        onComplete.run();
    }

    protected void postExecute() {
        // override to make some actions after scenario is executed
    }

    protected void onNextStep() {
        // override to make some action before each step is executed
    }

    String name() {
        return name;
    }

    public void execute() {
        preExecute(new Runnable() {
            @Override
            public void run() {
                stepIndex = 0;
                loadedListener = new MapLoadedListener() {
                    @Override
                    public void onMapLoaded(@NonNull MapLoadStatistics statistics) {
                        mapWindow.startPerformanceMetricsCapture();
                        mapWindow.getMap().startTileLoadMetricsCapture();
                        nextStep();
                    }
                };
                mapWindow.getMap().setMapLoadedListener(loadedListener);
                mapWindow.getMap().move(steps.get(stepIndex).position);
            }
        });
    }

    static class CameraCallbackImpl implements Map.CameraCallback {
        private WeakReference<PerformanceTestScenario> weakSelf;
        public CameraCallbackImpl(WeakReference<PerformanceTestScenario> weakSelf) {
            this.weakSelf = weakSelf;
        }

        @Override
        public void onMoveFinished(boolean completed) {
            if (completed) {
                PerformanceTestScenario self = weakSelf.get();
                if(self != null) {
                    self.nextStep();
                }
            }
        }
    }

    private void nextStep() {
        onNextStep();
        stepIndex++;
        if (stepIndex >= steps.size()) {
            finish();
        } else {
            PerformanceTestStep step = steps.get(stepIndex);
            mapWindow.getMap().move(step.position, new Animation(Animation.Type.LINEAR, step.time),
                new CameraCallbackImpl(new WeakReference<>(this)));
        }
    }

    private void finish() {
        String renderResult = mapWindow.stopPerformanceMetricsCapture();
        String decoderResult = mapWindow.getMap().stopTileLoadMetricsCapture();
        postExecute();
        callback.onScenarioFinished(name, renderResult, decoderResult);
    }

    @Override
    public String toString() {
        return name;
    }

    final MapWindow mapWindow;
    private final Callback callback;
    List<PerformanceTestStep> steps;
    private final String name;
    private int stepIndex = 0;
    private MapLoadedListener loadedListener;
}
