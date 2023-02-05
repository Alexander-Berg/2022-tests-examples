package utils;

import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.location.Location;

public final class LocationStubs {
    private LocationStubs(){
        throw new RuntimeException();
    }

    static final double GOOD_ACCURACY = 10.1415926535;
    static final double GOOD_ALTITUDE_ACCURACY = 13.1415926535;

    static final double BAD_ACCURACY = 1012.1415926535;
    static final double BAD_ALTITUDE_ACCURACY = 511.1415926535;

    public static Location accurate() {
        return new Location(
                new Point(0,0),
                GOOD_ACCURACY,
                0d,
                GOOD_ALTITUDE_ACCURACY,
                0d,
                0d,
                null,
                System.currentTimeMillis(),
                System.currentTimeMillis()
                );
    }


    public static Location speed(double value) {
        return new Location(
                new Point(0,0),
                GOOD_ACCURACY,
                0d,
                GOOD_ALTITUDE_ACCURACY,
                0d,
                value,
                null,
                System.currentTimeMillis(),
                System.currentTimeMillis()
                );
    }

    public static Location inaccurateSpeed(double value) {
        return new Location(
                new Point(0,0),
                BAD_ACCURACY,
                0d,
                BAD_ALTITUDE_ACCURACY,
                0d,
                value,
                null,
                System.currentTimeMillis(),
                System.currentTimeMillis()
                );
    }

}
