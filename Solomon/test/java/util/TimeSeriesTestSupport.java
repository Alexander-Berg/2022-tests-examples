package ru.yandex.solomon.alert.util;

import java.time.Instant;

import ru.yandex.solomon.model.point.AggrPoint;

/**
 * @author Vladimir Gordiychuk
 */
public final class TimeSeriesTestSupport {
    private TimeSeriesTestSupport() {
    }

    public static AggrPoint point(String time, double value) {
        return AggrPoint.builder()
                .time(time)
                .doubleValue(value)
                .build();
    }

    public static AggrPoint point(Instant time, double value) {
        return AggrPoint.builder()
                .time(time.toEpochMilli())
                .doubleValue(value)
                .build();
    }
}
