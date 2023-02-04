package com.yandex.maps.testapp.guidance.performance;

import com.yandex.mapkit.directions.guidance.PerformanceMonitor;

import java.util.List;
import java.util.logging.Level;

public interface TestScenario {
    void execute();
    void stop();

    interface TestScenarioListener {
        void onScenarioFinished(List<PerformanceMonitor> performanceMonitors);
        void onError(String error, Level level);
    }
}
