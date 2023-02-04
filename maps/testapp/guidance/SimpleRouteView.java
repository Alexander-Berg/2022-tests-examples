package com.yandex.maps.testapp.guidance;

import com.yandex.mapkit.directions.driving.DrivingRoute;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.PolylineMapObject;

public class SimpleRouteView {
    static public class PolylineStyle {
        public Integer strokeColor;
        public Float dashLength;
        public Float gapLength;
        public Float strokeWidth;

        public PolylineStyle(int color, float width) {
            strokeColor = color;
            strokeWidth = width;
        }
    }

    private PolylineStyle style;
    private PolylineMapObject routePolyline;
    private MapObjectCollection objectCollection;
    private float zIndex = 0.0f;
    private DrivingRoute route;

    public SimpleRouteView(MapObjectCollection objectCollection, PolylineStyle style) {
        this.objectCollection = objectCollection;
        this.style = style;
        this.routePolyline = null;
    }

    public void setZIndex(float zIndex) {
        this.zIndex  = zIndex;
        if (routePolyline != null) {
            routePolyline.setZIndex(this.zIndex);
        }
    }

    public void setRoute(DrivingRoute route) {
        if(route != null) {
            if (routePolyline != null) {
                routePolyline.setGeometry(route.getGeometry());
            } else {
                routePolyline = objectCollection.addPolyline(route.getGeometry());
                routePolyline.setZIndex(zIndex);
                assert style.strokeWidth != null;
                assert style.strokeColor != null;

                routePolyline.setStrokeWidth(style.strokeWidth);
                routePolyline.setStrokeColor(style.strokeColor);
                if (style.dashLength != null) {
                    routePolyline.setDashLength(style.dashLength);
                }
                if (style.gapLength != null) {
                    routePolyline.setGapLength(style.gapLength);
                }
            }
        }else{
            if(routePolyline != null) {
                objectCollection.remove(routePolyline);
                routePolyline = null;
            }
        }
        this.route = route;
    }

    public DrivingRoute getRoute() {
        return route;
    }

}
