package com.yandex.maps.testapp.common_routing;

import android.graphics.Color;
import android.util.Log;

import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.geometry.Polyline;
import com.yandex.mapkit.geometry.PolylinePosition;
import com.yandex.mapkit.geometry.Subpolyline;
import com.yandex.mapkit.geometry.SubpolylineHelper;
import com.yandex.mapkit.geometry.geo.PolylineUtils;
import com.yandex.mapkit.map.Arrow;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.map.PolylineMapObject;
import com.yandex.mapkit.transport.masstransit.Annotation;
import com.yandex.mapkit.transport.masstransit.WayPoint;
import com.yandex.mapkit.transport.masstransit.JamsListener;
import com.yandex.mapkit.transport.masstransit.JamUtils;
import com.yandex.mapkit.transport.masstransit.ConstructionID;
import com.yandex.mapkit.transport.masstransit.Route;
import com.yandex.mapkit.transport.masstransit.RoutePainter;
import com.yandex.mapkit.transport.masstransit.RestrictedEntry;
import com.yandex.maps.testapp.common_routing.IconHelper;
import com.yandex.maps.testapp.driving.IconWithLetter;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapObjectManager {
    private PolylineMapObject routeObject;
    private final MapObjectCollection restrictedEntryObjects;
    private final MapObjectCollection wayPointsObjects;
    private final MapObjectCollection objects;
    private Route route;
    private Destroyable sectionManager;

    private interface Destroyable {
        void destroy();
    }

    public MapObjectManager(@NotNull MapObjectCollection objects) {
        this.objects = objects;
        this.restrictedEntryObjects = objects.addCollection();
        this.wayPointsObjects = objects.addCollection();
    }

    public void makeRestrictedEntriesFrom(List<AnnotatedSection> sections) {
        restrictedEntryObjects.clear();
        if (sections == null) {
            return;
        }
        for (AnnotatedSection section : sections) {
            List<Point> points = section.getPolyline().getPoints();
            if (points.isEmpty()) {
                continue;
            }
            for (RestrictedEntry entry : section.getRestrictedEntries()) {
                IconHelper.addPlacemark(
                    restrictedEntryObjects,
                    points.get(entry.getPosition()),
                    IconHelper.createIconWithLetter('G', Color.LTGRAY),
                    100
                );
            }
        }
    }

    public void setRoute(Route route) {
        if (routeObject != null) {
            objects.remove(routeObject);
        }
        setSection(null);
        routeObject = null;
        this.route = null;
        wayPointsObjects.clear();
        if (route != null) {
            routeObject = objects.addPolyline(route.getGeometry());
            this.route = route;
            setWayPoints();
        }
    }

    public void setSection(AnnotatedSection section) {
        if (sectionManager != null) {
            sectionManager.destroy();
            sectionManager = null;
        }
        if (section != null) {
            Point point = section.getPoint();
            if (point != null ) {
                sectionManager = new PlacemarkManager(point);
            } else {
                sectionManager = new ColoredPolylineManager(
                    section.getSubpolyline(),
                    section.getConstructions(),
                    section.getAnnotations());
            }
        }
    }

    private void setWayPoints() {
        List<WayPoint> wayPoints = route.getWayPoints();
        for (int i = 0; i < wayPoints.size(); ++i) {
            WayPoint wayPoint = wayPoints.get(i);
            final char letter = (char)('A' + i);

            IconHelper.addPlacemark(
                wayPointsObjects,
                wayPoint.getPosition(),
                IconHelper.createIconWithLetter(letter, Color.CYAN),
                200
            );
            IconHelper.addPlacemark(
                wayPointsObjects,
                wayPoint.getSelectedArrivalPoint(),
                IconHelper.createIconWithLetter(letter, Color.BLUE),
                200
            );
            IconHelper.addPlacemark(
                wayPointsObjects,
                wayPoint.getSelectedDeparturePoint(),
                IconHelper.createIconWithLetter(letter, Color.GRAY),
                200
            );
        }
    }

    private class PlacemarkManager implements Destroyable {
        private PlacemarkMapObject mapObject;
        PlacemarkManager(@NotNull Point point) {
            mapObject = objects.addPlacemark(point);
            mapObject.setIcon(IconHelper.createIconWithLetter(' ', Color.GREEN));
        }
        @Override
        public void destroy() {
            assert mapObject != null;
            objects.remove(mapObject);
        }
    }

    private class ColoredPolylineManager
        implements Destroyable, JamsListener
    {
        private final
        RoutePainter routePainter = JamUtils.createRoutePainter();
        private PolylineMapObject mapObject;
        private final Subpolyline subpolyline;

        ColoredPolylineManager(
            @NotNull Subpolyline subpolyline,
            @NotNull List<Integer> constructions,
            @NotNull List<Annotation> annotations)
        {
            assert route != null;
            routePainter.reset(route);
            mapObject = objects.addPolyline();
            mapObject.setInnerOutlineEnabled(true);
            mapObject.setOutlineWidth(2);
            mapObject.setOutlineColor(Color.WHITE);

            if (constructions.isEmpty()) {
                routePainter.updatePolyline(mapObject, subpolyline);
                mapObject.setStrokeWidth(10.0F);
                mapObject.setGradientLength(10.0F);
                this.subpolyline = subpolyline;
            } else {
                mapObject.setGeometry(
                    SubpolylineHelper.subpolyline(
                        route.getGeometry(),
                        subpolyline));
                mapObject.setStrokeColors(constructions);
                for (ConstructionID key : colors.keySet()) {
                    mapObject.setPaletteColor(key.ordinal(), colors.get(key));
                }
                this.subpolyline = null;
            }
            mapObject.setZIndex(1.0F);

            if (route.getJams() != null) { // can be null for pedestrian routes
                route.getJams().addListener(this);
            }

            for (Annotation annotation : annotations) {
                drawArrow(annotation.getPosition());
            }
        }

        private void drawArrow(PolylinePosition position) {
            Arrow arrow = mapObject.addArrow(position, 20, Color.WHITE);
            arrow.setTriangleHeight(5);
            arrow.setOutlineColor(Color.BLACK);
            arrow.setOutlineWidth(1.0f);
        }

        @Override
        public void destroy() {
            assert route != null;
            if (route.getJams() != null) {
                route.getJams().removeListener(this);
            }
            assert mapObject != null;
            objects.remove(mapObject);
            mapObject = null;
        }

        @Override public void onJamsOutdated() { onJamsChanged(); }
        @Override public void onJamsUpdated() { onJamsChanged(); }

        private void onJamsChanged() {
            if (mapObject == null) {
                // Sometimes onJamsUpdated() may be called after removing listener and
                // destroying ColoredPolylineManager.
                return;
            }

            assert route != null;
            routePainter.reset(route);

            if (subpolyline != null) {
                routePainter.updatePolyline(mapObject, subpolyline);
            }
        }
    }

    private static final
    Map<ConstructionID, Integer> colors = new
        HashMap<ConstructionID, Integer>()
    {{
        put(ConstructionID.BINDING, 0xffcccc);
        put(ConstructionID.CROSSWALK, 0xff9999cc);
        put(ConstructionID.UNDERPASS, 0xff555555);
        put(ConstructionID.OVERPASS, 0xffcccccc);
        put(ConstructionID.TRANSITION, 0xffcc11cc);
        put(ConstructionID.STAIRS_DOWN, 0xff116611);
        put(ConstructionID.STAIRS_UP, 0xff44cc44);
        put(ConstructionID.STAIRS_UNKNOWN, 0xff339933);
        put(ConstructionID.UNKNOWN, Color.GREEN);
    }};
}
