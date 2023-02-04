package com.yandex.maps.testapp.guidance;

import com.yandex.mapkit.directions.guidance.ViewArea;
import com.yandex.mapkit.geometry.BoundingBox;
import com.yandex.mapkit.geometry.Point;

final public class GuidanceUtils {

    public static Point calculateViewCenter(ViewArea viewArea, float azimuth, Point position) {
        double lengthwise = viewArea.getLengthwise() / 3; // to central position
        double aziRad = azimuth * Math.PI / 180.;
        double lenLatCenter = lengthwise * Math.cos(aziRad);
        double lenLonCenter = lengthwise * Math.sin(aziRad);
        return com.yandex.maps.testapp.Utils.shiftPoint(position, lenLatCenter, lenLonCenter);
    }

    public static BoundingBox calculateViewBoundingBox(ViewArea viewArea, Point center) {
        double lengthwise = viewArea.getLengthwise() * 2 / 3; // to central position
        double transverse = viewArea.getTransverse();
        Point southWest = com.yandex.maps.testapp.Utils.shiftPoint(center, -lengthwise, -transverse);
        Point northEast = com.yandex.maps.testapp.Utils.shiftPoint(center, lengthwise, transverse);
        return new BoundingBox(southWest, northEast);
    }

}
