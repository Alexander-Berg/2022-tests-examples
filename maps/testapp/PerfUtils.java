package com.yandex.maps.testapp;

public class PerfUtils {
    public static long measureTime(Runnable runnable) {
        long start = System.currentTimeMillis();
        runnable.run();
        long stop = System.currentTimeMillis();
        return stop - start;
    }
}
