package com.yandex.maps.testapp.bicycle_routing;

import android.graphics.Color;
import androidx.annotation.NonNull;

import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.geometry.Polyline;
import com.yandex.mapkit.geometry.Subpolyline;
import com.yandex.mapkit.geometry.SubpolylineHelper;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.map.PolylineMapObject;
import com.yandex.mapkit.transport.bicycle.ConstructionID;
import com.yandex.mapkit.transport.bicycle.Leg;
import com.yandex.mapkit.transport.bicycle.Route;
import com.yandex.mapkit.transport.bicycle.RestrictedEntry;
import com.yandex.mapkit.transport.bicycle.TrafficTypeID;
import com.yandex.mapkit.transport.bicycle.WayPoint;
import com.yandex.maps.testapp.common_routing.ConstructionResolver;
import com.yandex.maps.testapp.common_routing.Helpers;
import com.yandex.maps.testapp.common_routing.IconHelper;

import java.util.ArrayList;
import java.util.HashMap;

class MapObjectManager {
    private final MapObjectCollection routeObjects;
    private final MapObjectCollection restrictedEntryObjects;
    private final MapObjectCollection legObjects;
    private final MapObjectCollection wayPointObjects;
    private Route route;

    private final HashMap<ConstructionID, Integer> constructionsColors =
        new HashMap<ConstructionID, Integer>() {{
            put(ConstructionID.BINDING, 0xffcccc);
            put(ConstructionID.CROSSING, 0xff9999cc);
            put(ConstructionID.UNDERPASS, 0xff555555);
            put(ConstructionID.OVERPASS, 0xffcccccc);
            put(ConstructionID.TUNNEL, 0xff000000);
            put(ConstructionID.STAIRS_DOWN, 0xff116611);
            put(ConstructionID.STAIRS_UP, 0xff44cc44);
            put(ConstructionID.STAIRS_UNKNOWN, 0xff339933);
            put(ConstructionID.UNKNOWN, Color.GREEN);
        }};

    private final HashMap<TrafficTypeID, Integer> trafficTypeColors =
        new HashMap<TrafficTypeID, Integer>() {{
            put(TrafficTypeID.AUTO, 0xffa52a2a);
            put(TrafficTypeID.PEDESTRIAN, 0xffffff00);
            put(TrafficTypeID.BICYCLE, 0xffffa500);
            put(TrafficTypeID.OTHER, 0xffff0000);
        }};

    MapObjectManager(@NonNull MapObjectCollection objects) {
        routeObjects = objects.addCollection();
        restrictedEntryObjects = objects.addCollection();
        legObjects = objects.addCollection();
        wayPointObjects = objects.addCollection();
    }

    void setRoute(Route route) {
        routeObjects.clear();
        restrictedEntryObjects.clear();
        legObjects.clear();
        wayPointObjects.clear();

        if (route == null) {
            return;
        }

        this.route = route;
        setWayPoints(route);
        routeObjects.addPolyline(route.getGeometry());

        for (RestrictedEntry entry : route.getRestrictedEntries()) {
            int segmentIndex = entry.getPosition().getSegmentIndex();
            Point point = route.getGeometry().getPoints().get(segmentIndex);
            PlacemarkMapObject placemark = restrictedEntryObjects.addPlacemark(point);
            placemark.setIcon(IconHelper.createIconWithLetter('G', Color.LTGRAY));
        }
    }

    void setLeg(Leg leg) {
        legObjects.clear();
        if (leg == null || route == null) {
            return;
        }
        Polyline polyline = SubpolylineHelper.subpolyline(route.getGeometry(), leg.getGeometry());
        PolylineMapObject constructionsPolyline = legObjects.addPolyline(polyline);
        setPolylineColors(constructionsColors, constructionsPolyline);
        PolylineMapObject trafficsPolyline = legObjects.addPolyline(polyline);
        setPolylineColors(trafficTypeColors, trafficsPolyline);

        ArrayList<Integer> constructionsColorsArray = Helpers.getFlatArray(
            route.getConstructions().size(), leg.getGeometry(),
            new ConstructionResolver() {
                @Override
                public Subpolyline subpolyline(int index) {
                    return route.getConstructions().get(index).getSubpolyline();
                }

                @Override
                public Integer id(int index) {
                    return route.getConstructions().get(index).getConstruction().ordinal();
                }
            }
        );
        ArrayList<Integer> trafficColorsArray = Helpers.getFlatArray(
            route.getTrafficTypes().size(), leg.getGeometry(),
            new ConstructionResolver() {
                @Override
                public Subpolyline subpolyline(int index) {
                    return route.getTrafficTypes().get(index).getSubpolyline();
                }

                @Override
                public Integer id(int index) {
                    return route.getTrafficTypes().get(index).getTrafficType().ordinal();
                }
            }
        );


        constructionsPolyline.setStrokeColors(constructionsColorsArray);
        constructionsPolyline.setOutlineWidth(0);
        constructionsPolyline.setStrokeWidth(2);
        constructionsPolyline.setZIndex(2);
        constructionsPolyline.setInnerOutlineEnabled(true);
        trafficsPolyline.setStrokeColors(trafficColorsArray);
        trafficsPolyline.setStrokeWidth(6);
        trafficsPolyline.setZIndex(1);
        trafficsPolyline.setInnerOutlineEnabled(true);
        trafficsPolyline.setOutlineColor(Color.WHITE);
        trafficsPolyline.setOutlineWidth(2);
    }

    private void setWayPoints(Route route) {
        for (int i = 0; i < route.getWayPoints().size(); ++i) {
            WayPoint wayPoint = route.getWayPoints().get(i);
            final char letter = (char)('A' + i);
            IconHelper.addPlacemark(
                wayPointObjects,
                wayPoint.getPosition(),
                IconHelper.createIconWithLetter(letter, Color.CYAN),
                200
            );
            IconHelper.addPlacemark(
                wayPointObjects,
                wayPoint.getSelectedArrivalPoint(),
                IconHelper.createIconWithLetter(letter, Color.BLUE),
                200
            );
        }
    }

    private <E extends Enum<E>> void setPolylineColors(
        HashMap<E, Integer> colors,
        PolylineMapObject polyline)
    {
        for (E key : colors.keySet()) {
            polyline.setPaletteColor(key.ordinal(), colors.get(key));
        }
    }
}
