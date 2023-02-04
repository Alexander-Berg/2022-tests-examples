package com.yandex.maps.testapp.guidance.performance;

import com.yandex.mapkit.directions.guidance.GuidanceListener;

final class GuidanceListenerImpl implements GuidanceListener {

    private LocationUpdateListener listener;

    GuidanceListenerImpl(LocationUpdateListener listener) {
        this.listener = listener;
    }

    public void onLocationUpdated() {
        listener.onLocationUpdated();
    }
    public void onAlternativesUpdated() {
        listener.onAlternativesUpdated();
    }
    public void onRoutePositionUpdated() {
        listener.onRoutePositionUpdated();
    }
    public void onRouteUpdated() {
        listener.onRouteUpdated();
    }
    public void onReturnedToRoute() {
        listener.onReturnedToRoute();
    }
    public void onFinishedRoute() {
        listener.onFinishedRoute();
    }

    public void onLostRoute() {
        listener.onLostRoute();
    }

    public void onAnnotationsUpdated() {}
    public void onRoadNameUpdated() {}
    public void onFasterAlternativeUpdated() {}
    public void onAlternativesTimeDifferenceUpdated() {}
    public void onSpeedLimitUpdated() {}
    public void onSpeedLimitExceededUpdated() {}
    public void onSpeedLimitExceeded() {}
    public void onLaneSignUpdated() {}
    public void onUpcomingEventsUpdated() {}
    public void onParkingRoutesUpdated() {}
    public void onLastViaPositionChanged() {}
    public void onManeuverAnnotated() {}
    public void onFasterAlternativeAnnotated() {}
    public void onStandingStatusUpdated() {}
    public void onFreeDriveRouteUpdated() {}
    public void onReachedWayPoint() {}
    public void onDirectionSignUpdated() {}

    interface LocationUpdateListener {
        void onLocationUpdated();
        void onAlternativesUpdated();
        void onRoutePositionUpdated();
        void onRouteUpdated();
        void onReturnedToRoute();
        void onFinishedRoute();
        void onLostRoute();
    }
}
