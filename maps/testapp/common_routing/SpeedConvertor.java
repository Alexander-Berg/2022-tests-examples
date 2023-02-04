package com.yandex.maps.testapp.common_routing;

public class SpeedConvertor {

    // from km/h to m/s
    public static double toMs(double speed) {
        return speed / 3.6;
    }

    // from m/s to km/h
    public static double toKmh(double speed) {
        return speed * 3.6;
    }
}
