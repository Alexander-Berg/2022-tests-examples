package com.yandex.maps.testapp.mrcphoto;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yandex.mapkit.Animation;
import com.yandex.mapkit.geometry.BoundingBox;
import com.yandex.mapkit.geometry.BoundingBoxHelper;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.geometry.Polyline;
import com.yandex.mapkit.map.CameraListener;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.CameraUpdateReason;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.map.MapObjectTapListener;
import com.yandex.mapkit.map.PolylineMapObject;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.maps.testapp.Utils;

class RoutePolyline implements CameraListener {
    private static final float ZINDEX = 30;
    private MapView mapview = null;
    private float cameraZoom = 0.0f;
    protected PolylineMapObject mapPolylineObject = null;

    RoutePolyline(@NonNull MapView mapview) {
        this.mapview = mapview;
        this.mapview.getMap().addCameraListener(this);
    }

    public void setGeometry(@Nullable Polyline polyline) {
        clear();

        mapPolylineObject = mapview.getMap().getMapObjects().addPolyline(polyline);
        mapPolylineObject.setZIndex(ZINDEX);
        applyPolylineWidth(mapPolylineObject, cameraZoom);
    }

    public Polyline getGeometry() {
        if (mapPolylineObject == null) {
            return null;
        }
        return mapPolylineObject.getGeometry();
    }

    public PolylineMapObject getRouteMapObject() {
        return  mapPolylineObject;
    }

    public void clear() {
        if (mapPolylineObject != null) {
            mapview.getMap().getMapObjects().remove(mapPolylineObject);
            mapPolylineObject = null;
        }
    }

    public void onDestroy() {
        clear();
        this.mapview.getMap().removeCameraListener(this);
    }

    public void subscribeForTaps(@NonNull MapObjectTapListener lsnr) {
        if (mapPolylineObject != null) {
            mapPolylineObject.addTapListener(lsnr);
        }
    }

    public void unsubsribeFromTaps(@NonNull MapObjectTapListener lsnr) {
        if (mapPolylineObject != null) {
            mapPolylineObject.removeTapListener(lsnr);
        }
    }

    public void setVisible(boolean visibility) {
        if (mapPolylineObject != null) {
            mapPolylineObject.setVisible(visibility);
        }
    }

    public void focusOnRoute() {
        if (mapPolylineObject != null) {
            focusOnBoundingBox(BoundingBoxHelper.getBounds(getGeometry()));
        }
    }

    @Override
    public void onCameraPositionChanged(
            Map map, CameraPosition position, CameraUpdateReason updateReason, boolean finished) {
        if (updateCameraZoom()) {
            applyPolylineWidth(mapPolylineObject, cameraZoom);
        }
    }

    private boolean updateCameraZoom() {
        final float CAMERA_ZOOM_EPS = 1e-3f;

        if (Math.abs(cameraZoom - mapview.getMap().getCameraPosition().getZoom()) > CAMERA_ZOOM_EPS) {
            cameraZoom = mapview.getMap().getCameraPosition().getZoom();
            return true;
        }
        return false;
    }

    private void focusOnBoundingBox(BoundingBox bbox) {
        final BoundingBox bbox2 = inflatedBoundingBox(bbox, 1.8);
        final CameraPosition cameraPosition = mapview.getMap().cameraPosition(bbox2);
        mapview.getMap().move(cameraPosition,
                new Animation(Animation.Type.SMOOTH, 0.2f), null);
    }

    private static BoundingBox inflatedBoundingBox(BoundingBox bbox, double inflationFactor) {
        double minX = bbox.getSouthWest().getLatitude();
        double maxX = bbox.getNorthEast().getLatitude();
        double minY = bbox.getSouthWest().getLongitude();
        double maxY = bbox.getNorthEast().getLongitude();
        double dx = (maxX - minX) * (inflationFactor - 1) / 2;
        double dy = (maxY - minY) * (inflationFactor - 1) / 2;

        return new BoundingBox(new Point(minX - dx, minY - dy), new Point(maxX + dx, maxY + dy));
    }

    private static void applyPolylineWidth(PolylineMapObject routePolyline, float zoom) {
        if (routePolyline == null)
            return;

        routePolyline.setStrokeWidth(calcRouteWidth(zoom));
        routePolyline.setGradientLength(calcGradientLength(zoom));
    }

    private static float calcRouteWidth(float zoom) {
        final float MIN_WIDTH_ZOOM = 12.0f;
        final float MAX_WIDTH_ZOOM = 15.0f;
        final float MIN_WIDTH = 6.0f;
        final float MAX_WIDTH = 12.0f;

        return linearStep(zoom, MIN_WIDTH_ZOOM, MAX_WIDTH_ZOOM, MIN_WIDTH, MAX_WIDTH);
    }

    private static float calcGradientLength(float zoom) {
        final float MIN_GRAD_LEN_ZOOM = 13.5f;
        final float MAX_GRAD_LEN_ZOOM = 15.0f;
        final float MIN_GRAD_LEN = 0.0f;
        final float MAX_GRAD_LEN = 40.0f;

        return linearStep(zoom, MIN_GRAD_LEN_ZOOM, MAX_GRAD_LEN_ZOOM, MIN_GRAD_LEN, MAX_GRAD_LEN);
    }

    private static float linearStep(float x, float x0, float x1, float y0, float y1) {
        return Utils.lerp((Utils.clamp(x, x0, x1) - x0) / (x1 - x0), y0, y1);
    }
}
