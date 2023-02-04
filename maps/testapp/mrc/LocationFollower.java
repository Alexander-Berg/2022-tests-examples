package com.yandex.maps.testapp.mrc;


import android.content.Context;
import android.graphics.Bitmap;
import androidx.annotation.NonNull;

import com.yandex.mapkit.Animation;
import com.yandex.mapkit.geometry.BoundingBox;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.directions.guidance.ClassifiedLocation;
import com.yandex.mapkit.directions.guidance.Guide;
import com.yandex.mapkit.directions.guidance.ViewArea;
import com.yandex.mapkit.location.Location;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.IconStyle;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.map.RotationType;
import com.yandex.maps.testapp.R;
import com.yandex.runtime.image.ImageProvider;

public class LocationFollower implements com.yandex.mapkit.directions.guidance.GuidanceListener {
    static private final double WGS84RADIUS = 6378137.0;
    static private final double LAT_CONVERSION = WGS84RADIUS * Math.PI / 180.;
    static private final float ZOOM_VIEW_OUT = 0.2f;

    private final Guide guide;
    private final Map map;
    private final MapObjectCollection mapObjects;
    private final Bitmap arrowIcon;
    private PlacemarkMapObject locationPoint = null;

    public LocationFollower(Context context, Guide guide, Map map)
    {
        this.guide = guide;
        this.map = map;
        mapObjects = map.getMapObjects().addCollection();
        arrowIcon = ImageProvider.fromResource(context, R.drawable.navigation_icon).getImage();
    }

    @Override
    public void onLocationUpdated() {
        ClassifiedLocation classifiedLocation = guide.getLocation();
        if (classifiedLocation == null) {
            return;
        }
        Location location = classifiedLocation.getLocation();
        if (location == null) {
            return;
        }

        moveToCurrentPosition(location);

        updateArrow(location);
    }

    private void moveToCurrentPosition(@NonNull Location currentLocation) {
        ViewArea viewArea = guide.getViewArea();
        if(viewArea == null) {
            return;
        }
        CameraPosition position = map.getCameraPosition();
        float azimuth = currentLocation.getHeading() != null ?
                currentLocation.getHeading().floatValue() : position.getAzimuth();
        Point center = calculateViewCenter(viewArea, azimuth, currentLocation.getPosition());
        float zoom = map.cameraPosition(calculateViewBoundingBox(viewArea, center)).getZoom();

        position = new CameraPosition(center,zoom - ZOOM_VIEW_OUT, azimuth,0f);
        Animation animation = new Animation(Animation.Type.SMOOTH, 0.0f);
        map.move(position, animation, null);
    }

    private void updateArrow(@NonNull Location location) {
        if (locationPoint != null) {
            locationPoint.setGeometry(location.getPosition());
        } else {
            locationPoint = mapObjects.addPlacemark(location.getPosition());
            locationPoint.setZIndex(50.0f);
            locationPoint.setIcon(
                    ImageProvider.fromBitmap(arrowIcon),
                    new IconStyle()
                            .setFlat(true)
                            .setRotationType(RotationType.ROTATE)
            );
        }

        if (location.getHeading() != null) {
            locationPoint.setDirection(location.getHeading().floatValue());
        }
    }

    @Override
    public void onRoutePositionUpdated() {

    }

    @Override
    public void onAnnotationsUpdated() {

    }

    @Override
    public void onRoadNameUpdated() {

    }

    @Override
    public void onFinishedRoute() {

    }

    @Override
    public void onLostRoute() {

    }

    @Override
    public void onReturnedToRoute() {

    }

    @Override
    public void onRouteUpdated() {

    }

    @Override
    public void onAlternativesUpdated() {

    }

    @Override
    public void onAlternativesTimeDifferenceUpdated() {

    }

    @Override
    public void onSpeedLimitUpdated() {

    }

    @Override
    public void onSpeedLimitExceededUpdated() {

    }

    @Override
    public void onSpeedLimitExceeded() {

    }

    @Override
    public void onLaneSignUpdated() {

    }

    @Override
    public void onUpcomingEventsUpdated() {

    }

    @Override
    public void onFasterAlternativeUpdated() {

    }

    @Override
    public void onParkingRoutesUpdated() {

    }

    @Override
    public void onManeuverAnnotated() {

    }

    @Override
    public void onLastViaPositionChanged() {

    }

    @Override
    public void onFasterAlternativeAnnotated() {

    }

    @Override
    public void onStandingStatusUpdated() {

    }

    @Override
    public void onFreeDriveRouteUpdated() {

    }

    @Override
    public void onReachedWayPoint() {

    }

    @Override
    public void onDirectionSignUpdated() {

    }

    private static Point calculateViewCenter(ViewArea viewArea, float azimuth, Point position) {
        double lengthwise = viewArea.getLengthwise() / 3; // to central position
        double aziRad = toRadians(azimuth);
        double lenLatCenter = lengthwise * Math.cos(aziRad);
        double lenLonCenter = lengthwise * Math.sin(aziRad);
        return shiftPoint(position, lenLatCenter, lenLonCenter);
    }

    private static BoundingBox calculateViewBoundingBox(ViewArea viewArea, Point center) {
        double lengthwise = viewArea.getLengthwise() * 2 / 3; // to central position
        double transverse = viewArea.getTransverse();
        Point southWest = shiftPoint(center, -lengthwise, -transverse);
        Point northEast = shiftPoint(center, lengthwise, transverse);
        return new BoundingBox(southWest, northEast);
    }

    private static Point shiftPoint(Point point, double lenLat, double lenLon) {
        return new Point(point.getLatitude() + metersToDlat(lenLat),
                point.getLongitude() + metersToDlon(lenLon, point));
    }

    private static double metersToDlat(double len) {
        return len / LAT_CONVERSION;
    }

    private static double metersToDlon(double len, Point point) {
        return metersToDlat(len) / Math.cos(toRadians(point.getLatitude()));
    }

    private static double toRadians(double angle) {
        return angle * Math.PI / 180.;
    }
}
