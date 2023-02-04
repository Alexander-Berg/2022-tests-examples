package com.yandex.maps.testapp.map;

import com.yandex.mapkit.map.CameraPosition;

public class PerformanceTestStep {
    final CameraPosition position;
    final float time;

    PerformanceTestStep(CameraPosition position, float time) {
        this.position = position;
        this.time = time;
    }
}
