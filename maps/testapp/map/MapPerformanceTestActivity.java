package com.yandex.maps.testapp.map;

import android.widget.Spinner;

import com.yandex.mapkit.map.MapLoadStatistics;
import com.yandex.mapkit.map.MapLoadedListener;
import com.yandex.maps.testapp.R;

public class MapPerformanceTestActivity extends MapPerformanceActivity  {
    private MapLoadedListener mapLoadedListener;
    private PerformanceTestScenario.Callback performanceListener;

    public static final String DEBUG_INFO_ENABLED_TAG = "debugInfoEnabled";

    public void setMapLoadedListener(MapLoadedListener mapLoadedListener) {
        this.mapLoadedListener = mapLoadedListener;
    }

    public void setPerformanceListener(PerformanceTestScenario.Callback performanceListener) {
        this.performanceListener = performanceListener;
    }

    @Override
    public void onMapLoaded(MapLoadStatistics statistics) {
        super.onMapLoaded(statistics);
        if (mapLoadedListener != null) {
            mapLoadedListener.onMapLoaded(statistics);
        }
    }

    @Override
    public void onScenarioFinished(final String scenarioName, final String renderResult, final String decoderResult) {
        super.onScenarioFinished(scenarioName, renderResult, decoderResult);
        if (performanceListener != null) {
            performanceListener.onScenarioFinished(scenarioName, renderResult, decoderResult);
        }
    }

    public void executeTest(String name) {
        Spinner scenarioSpinner = findViewById(R.id.scenario_spinner);
        int i = 0;
        for (PerformanceTestScenario scenario : scenarios) {
            if (scenario.name().toLowerCase().equals(name.toLowerCase())) {
                scenarioSpinner.setSelection(i);
                scenario.execute();
                return;
            }
            i++;
        }

        throw new RuntimeException(String.format("Unknown test: %s", name));
    }

    @Override
    protected void onInitMap() {
        super.onInitMap();
        Boolean debugInfoEnabled = (Boolean) getIntent().getSerializableExtra(DEBUG_INFO_ENABLED_TAG);
        if (debugInfoEnabled != null) {
            mapview.getMap().setDebugInfoEnabled(debugInfoEnabled);
        }
    }
}
