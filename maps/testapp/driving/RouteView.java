package com.yandex.maps.testapp.driving;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

import com.yandex.mapkit.directions.driving.ArrowManeuverStyle;
import com.yandex.mapkit.directions.driving.ConditionsListener;
import com.yandex.mapkit.directions.driving.DirectionSign;
import com.yandex.mapkit.directions.driving.DrivingRoute;
import com.yandex.mapkit.directions.driving.Event;
import com.yandex.mapkit.directions.driving.FordCrossing;
import com.yandex.mapkit.directions.driving.JamType;
import com.yandex.mapkit.directions.driving.PedestrianCrossing;
import com.yandex.mapkit.directions.driving.RailwayCrossing;
import com.yandex.mapkit.directions.driving.SpeedBump;
import com.yandex.mapkit.directions.driving.internal.LaneBitmapFactory;
import com.yandex.mapkit.directions.driving.LaneSign;
import com.yandex.mapkit.directions.driving.RestrictedTurn;
import com.yandex.mapkit.directions.driving.RuggedRoad;
import com.yandex.mapkit.directions.driving.RestrictedEntry;
import com.yandex.mapkit.directions.driving.TrafficLight;
import com.yandex.mapkit.directions.driving.TollRoad;
import com.yandex.mapkit.directions.driving.RoutePoint;
import com.yandex.mapkit.directions.driving.ManoeuvreVehicleRestriction;
import com.yandex.mapkit.directions.driving.RoadVehicleRestriction;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.geometry.Polyline;
import com.yandex.mapkit.geometry.PolylinePosition;
import com.yandex.mapkit.geometry.Subpolyline;
import com.yandex.mapkit.geometry.SubpolylineHelper;
import com.yandex.mapkit.geometry.geo.PolylineUtils;
import com.yandex.mapkit.map.CameraListener;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.CameraUpdateReason;
import com.yandex.mapkit.map.PolylineMapObject;
import com.yandex.mapkit.directions.driving.JamStyle;
import com.yandex.mapkit.directions.driving.JamTypeColor;
import com.yandex.mapkit.directions.driving.ManeuverStyle;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.MapObjectTapListener;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.directions.driving.RouteHelper;
import com.yandex.mapkit.road_events.EventTag;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.Utils;
import com.yandex.runtime.image.ImageProvider;

import java.util.List;
import java.util.ArrayList;

class RoadEventsUtils {
    static int resourceId(EventTag tag) {
        switch (tag) {
            case ACCIDENT:
                return R.drawable.accident;
            case CHAT:
            case LOCAL_CHAT:
                return R.drawable.chat;
            case CLOSED:
                return R.drawable.closed;
            case DRAWBRIDGE:
                return R.drawable.drawbridge;
            case RECONSTRUCTION:
                return R.drawable.reconstruction;
            case SCHOOL:
                return R.drawable.school;
            case TRAFFIC_ALERT:
                return R.drawable.traffic_alert;
            case DANGER:
                return R.drawable.other; // TODO(dbeliakov) icon for danger

            case POLICE:
                return R.drawable.police;
            case SPEED_CONTROL:
                return R.drawable.speed_camera;
            case LANE_CONTROL:
                return R.drawable.lane_camera;
            case MOBILE_CONTROL:
                return R.drawable.mobile_control;
            case ROAD_MARKING_CONTROL:
                return R.drawable.road_marking_control;
            case CROSS_ROAD_CONTROL:
                return R.drawable.cross_road_control;
            case NO_STOPPING_CONTROL:
                return R.drawable.no_stopping_control;

            default:
                return R.drawable.other;
        }
    }
}

class EventInfo {
    EventInfo(
            String eventId,
            List<EventTag> tags,
            Point location,
            String speedLimit,
            String descriptionText) {
        this.eventId = eventId;
        this.tags = tags;
        this.location = location;
        this.speedLimit = speedLimit;
        this.descriptionText = descriptionText;
    }

    public String eventId;
    public List<EventTag> tags;
    public Point location;
    public String speedLimit;
    public String descriptionText;
}

public class RouteView implements CameraListener {
    private DrivingRoute route;

    private MapObjectCollection mapObjects;
    private PolylineMapObject routePolyline;
    private MapObjectCollection mapSelectedArrivalPoints;

    private boolean eventsEnabled = false;
    private boolean maneuversEnabled = true;
    private boolean trafficLightsAndBarriersEnabled = true;
    private boolean selectedArrivalPointsEnabled = false;
    private boolean laneSignsEnabled = false;
    private boolean directionSignsEnabled = false;

    private float zIndex = 0.0f;
    private float lastZoom;
    private float widthScale = 1.0f;

    private JamStyle jamStyle = RouteHelper.createDefaultJamStyle();
    private ManeuverStyle maneuverStyle = RouteHelper.createDefaultManeuverStyle();

    private MapObjectTapListener roadEventsTapListener;

    private MapObjectCollection mapRoadEvents;
    private MapObjectCollection mapBarriers;
    private MapObjectCollection restrictedTurnMarks;
    private MapObjectCollection laneSigns;
    private MapObjectCollection directionSigns;

    private final MapObjectCollection railwayCrossings;
    private final MapObjectCollection pedestrianCrossings;
    private final MapObjectCollection speedBumps;

    private MapObjectCollection mapRoads;
    private List<PolylineMapObject> roads = new ArrayList<PolylineMapObject>();

    private boolean vehicleRestrictionsEnabled = false;
    private MapObjectCollection vehicleRestrictions;
    private List<PolylineMapObject> vehicleRestrictionRoads = new ArrayList<PolylineMapObject>();
    private MapObjectTapListener vehicleRestrictionsTapListener = null;

    private PlacemarkMapObject textPlacemark;
    private static final float TEXT_PLACEMARK_Z_INDEX = 100.0f;
    private static final float TRAFFIC_LIGHT_Z_INDEX = 25.0f;
    private static final float LANE_SIGN_POS_Z_INDEX = 30.0f;
    private static final float DIRECTION_SIGN_POS_Z_INDEX = 40.0f;
    private static final float LANE_SIGN_Z_INDEX = 50.0f;
    private static final float VEHICLE_RESTRICTION_Z_INDEX = 55.0f;

    private LaneSign laneSign;
    private PlacemarkMapObject laneSignPlacemark;
    private static final int LANE_SIGN_BACKGROUND_COLOR = Color.rgb(0, 0, 204);
    private static final int LANE_SIGN_MARGIN = 10;

    private interface CurrentZoomProvider {
        float getZoom();
    }
    private CurrentZoomProvider currentZoomProvider;

    public interface ImageProviderFactory {
        ImageProvider fromResource(int resourceId);
    }
    private ImageProviderFactory imageProviderFactory;

    private MapObjectTapListener tapListener = null;

    public RouteView(
            final Map map,
            MapObjectTapListener roadEventsTapListener,
            ImageProviderFactory imageProviderFactory) {
        mapObjects = map.getMapObjects();
        currentZoomProvider = new CurrentZoomProvider() {
            private final Map map_;
            { this.map_ = map; }

            @Override
            public float getZoom() {
                return map_.getCameraPosition().getZoom();
            }
        };
        lastZoom = currentZoomProvider.getZoom();

        this.imageProviderFactory = imageProviderFactory;

        mapRoadEvents = map.getMapObjects().addCollection();
        this.roadEventsTapListener = roadEventsTapListener;

        mapBarriers = map.getMapObjects().addCollection();

        restrictedTurnMarks = map.getMapObjects().addCollection();

        mapRoads = map.getMapObjects().addCollection();

        vehicleRestrictions = map.getMapObjects().addCollection();

        laneSigns = map.getMapObjects().addCollection();
        directionSigns = map.getMapObjects().addCollection();

        mapSelectedArrivalPoints = map.getMapObjects().addCollection();

        railwayCrossings = map.getMapObjects().addCollection();
        pedestrianCrossings = map.getMapObjects().addCollection();
        speedBumps = map.getMapObjects().addCollection();

        map.addCameraListener(this);
    }

    public DrivingRoute getRoute() {
        return route;
    }

    public PolylineMapObject getRoutePolyline() {
        return routePolyline;
    }

    public void setJamsEnabled(boolean jamsEnabled) {
        jamStyle = jamsEnabled ?
            RouteHelper.createDefaultJamStyle() : RouteHelper.createDisabledJamStyle();
        applyRouteStyle(routePolyline, route);
    }

    public void setJamsColors(List<JamTypeColor> colors) {
        if (colors == null)
            colors = RouteHelper.createDefaultJamStyle().getColors();
        jamStyle = new JamStyle(colors);
        applyRouteStyle(routePolyline, route);
    }

    public void updateManeuverStyle(
            int fillColor,
            int outlineColor,
            float outlineWidth) {
        maneuverStyle = new ManeuverStyle(
                new ArrowManeuverStyle(
                        fillColor,
                        outlineColor,
                        outlineWidth,
                        maneuverStyle.getArrow().getLength(),
                        maneuverStyle.getArrow().getTriangleHeight(),
                        showManeuvers()),
                maneuverStyle.getPolygon());
        applyRouteStyle(routePolyline, route);
    }

    public void setEventsEnabled(boolean eventsEnabled) {
        if (this.eventsEnabled != eventsEnabled) {
            this.eventsEnabled = eventsEnabled;
            updateRoadEvents();
        }
    }

    public void setLaneSignsEnabled(boolean laneSignsEnabled) {
        if (this.laneSignsEnabled != laneSignsEnabled) {
            this.laneSignsEnabled = laneSignsEnabled;
            updateLaneSigns();
        }
    }

    public void setDirectionSignsEnabled(boolean directionSignsEnabled) {
        if (this.directionSignsEnabled != directionSignsEnabled) {
            this.directionSignsEnabled = directionSignsEnabled;
            updateDirectionSigns();
        }
    }

    public void setSelectedArrivalPointsEnabled(boolean enabled) {
        if (this.selectedArrivalPointsEnabled != enabled) {
            this.selectedArrivalPointsEnabled = enabled;
            updateSelectedArrivalPoints();
        }
    }

    public void setManeuversEnabled(boolean maneuversEnabled){
        if (this.maneuversEnabled != maneuversEnabled){
            this.maneuversEnabled = maneuversEnabled;
            applyRouteStyle(routePolyline, route);
        }
    }

    public void setTrafficLightsAndBarriersEnabled(boolean enabled) {
        if (trafficLightsAndBarriersEnabled != enabled) {
            this.trafficLightsAndBarriersEnabled = enabled;
            updateTrafficLightsAndBarriers();
        }
    }

    private boolean showManeuvers() {
        return maneuversEnabled && currentZoomProvider.getZoom() >= 14.5;
    }

    static private List<JamTypeColor> offlineAndOutdatedColors() {
        List<JamTypeColor> list = new ArrayList<JamTypeColor>();
        int color = 0xff177ee6;
        list.add(new JamTypeColor(JamType.UNKNOWN, color));
        list.add(new JamTypeColor(JamType.BLOCKED, color));
        list.add(new JamTypeColor(JamType.FREE, color));
        list.add(new JamTypeColor(JamType.LIGHT, color));
        list.add(new JamTypeColor(JamType.HARD, color));
        list.add(new JamTypeColor(JamType.VERY_HARD, color));
        return list;
    }

    private void updateLaneSigns() {
        laneSigns.clear();

        if (route == null || imageProviderFactory == null || !laneSignsEnabled) {
            return;
        }

        final ImageProvider icon = imageProviderFactory.fromResource(R.drawable.lane_sign);
        for (LaneSign laneSign : route.getLaneSigns()) {
            Point point = PolylineUtils.pointByPolylinePosition(
                    getRoute().getGeometry(),
                    laneSign.getPosition());
            PlacemarkMapObject placemark = laneSigns.addPlacemark(point);
            placemark.setZIndex(LANE_SIGN_POS_Z_INDEX);
            placemark.setIcon(icon);
        }
    }

    private void updateDirectionSigns() {
        directionSigns.clear();

        if (route == null || imageProviderFactory == null || !directionSignsEnabled) {
            return;
        }

        final ImageProvider icon = imageProviderFactory.fromResource(R.drawable.direction_sign);
        for (DirectionSign directionSign : route.getDirectionSigns()) {
            Point point = PolylineUtils.pointByPolylinePosition(
                    getRoute().getGeometry(),
                    directionSign.getPosition());
            PlacemarkMapObject placemark = directionSigns.addPlacemark(point);
            placemark.setZIndex(DIRECTION_SIGN_POS_Z_INDEX);
            placemark.setIcon(icon);
        }
    }

    private ImageProvider getLaneSignImageProvider(LaneSign laneSign) {
        Bitmap opaqueLaneSignBitmap = LaneBitmapFactory.createLaneBitmap(laneSign.getLanes());
        Bitmap laneSignBitmap = Bitmap.createBitmap(
                opaqueLaneSignBitmap.getWidth() + 2 * LANE_SIGN_MARGIN,
                opaqueLaneSignBitmap.getHeight() + 2 * LANE_SIGN_MARGIN,
                opaqueLaneSignBitmap.getConfig());
        Canvas canvas = new Canvas(laneSignBitmap);
        canvas.drawColor(LANE_SIGN_BACKGROUND_COLOR);
        canvas.drawBitmap(opaqueLaneSignBitmap, LANE_SIGN_MARGIN, LANE_SIGN_MARGIN, null);
        return ImageProvider.fromBitmap(laneSignBitmap);
    }

    private void removeLaneSignPlacemark() {
        if (laneSignPlacemark != null) {
            mapObjects.remove(laneSignPlacemark);
            laneSignPlacemark = null;
        }
        laneSign = null;
    }

    private void showLaneSignPlacemark(LaneSign newLaneSign) {
        Point point = PolylineUtils.pointByPolylinePosition(
                routePolyline.getGeometry(),
                newLaneSign.getPosition());
        laneSignPlacemark = mapObjects.addPlacemark(point);
        laneSignPlacemark.setIcon(getLaneSignImageProvider(newLaneSign));
        laneSignPlacemark.setZIndex(LANE_SIGN_Z_INDEX);

        laneSign = newLaneSign;
    }

    public void setLaneSign(LaneSign newLaneSign) {
        if (laneSign == newLaneSign) {
            return;
        }

        removeLaneSignPlacemark();

        if (laneSignsEnabled && routePolyline != null && newLaneSign != null) {
            showLaneSignPlacemark(newLaneSign);
        }
    }

    private ConditionsListener conditionsListener = new ConditionsListener() {
        @Override
        public void onConditionsUpdated() {
            updateConditions();
        }

        @Override
        public void onConditionsOutdated() {
            updateConditions();
        }
    };

    private boolean conditionsOutdated() {
        return route == null || route.isAreConditionsOutdated();
    }

    private void updateConditions() {
        applyRouteStyle(routePolyline, route);
        updateRoadEvents();
        setPosition(route.getPosition());
    }

    private void updateTrafficLightsAndBarriers() {
        mapBarriers.clear();

        if (route == null || !trafficLightsAndBarriersEnabled || imageProviderFactory == null) {
            return;
        }

        final ImageProvider restrictedEntryIcon = imageProviderFactory.fromResource(R.drawable.barrier);
        for (RestrictedEntry entry: route.getRestrictedEntries()) {
            Point point = polylinePoint(route.getGeometry(), entry.getPosition());
            PlacemarkMapObject placemark = mapBarriers.addPlacemark(point);
            placemark.setIcon(restrictedEntryIcon);
            placemark.setZIndex(100);
        }

        final ImageProvider trafficLightIcon = imageProviderFactory.fromResource(R.drawable.traffic_light);
        for (TrafficLight trafficLight: route.getTrafficLights()) {
            Point point = polylinePoint(route.getGeometry(), trafficLight.getPosition());
            PlacemarkMapObject placemark = mapBarriers.addPlacemark(point);
            placemark.setIcon(trafficLightIcon);
            placemark.setZIndex(TRAFFIC_LIGHT_Z_INDEX);
        }
    }

    public void setRoute(DrivingRoute newRoute) {
        if (route != null) {
            route.removeConditionsListener(conditionsListener);
        }

        if (routePolyline != null) {
            if (tapListener != null) {
                routePolyline.removeTapListener(tapListener);
            }
            mapObjects.remove(routePolyline);
            routePolyline = null;
        }

        clearMapObjects();

        if (textPlacemark != null) {
            mapObjects.remove(textPlacemark);
            textPlacemark = null;
        }

        route = newRoute;
        if (route != null) {
            route.addConditionsListener(conditionsListener);

            routePolyline = mapObjects.addPolyline();
            if (tapListener != null) {
                routePolyline.addTapListener(tapListener);
            }
            routePolyline.setZIndex(zIndex);

            updateMapObjects();

            applyRoutePolylineWidth(currentZoomProvider.getZoom());
            applyRoadPolylinesWidth(currentZoomProvider.getZoom());
        }
        updateSelectedArrivalPoints();
    }

    private void clearMapObjects() {
        mapRoadEvents.clear();
        mapBarriers.clear();
        restrictedTurnMarks.clear();
        mapRoads.clear();
        roads.clear();
        laneSigns.clear();
        directionSigns.clear();
        vehicleRestrictions.clear();
        vehicleRestrictionRoads.clear();
        railwayCrossings.clear();
        pedestrianCrossings.clear();
        speedBumps.clear();
    }

    private void updateMapObjects() {
        updateTollRoads();
        updateRuggedRoads();
        updateFordCrossings();
        updateLaneSigns();
        updateDirectionSigns();
        updateTrafficLightsAndBarriers();
        updateVehicleRestrictions();
        updateConditions();
        updateRestrictedTurns();
        updateRailwayCrossings();
        updatePedestrianCrossings();
        updateSpeedBumps();
    }

    public void setWidthScale(float scale) {
        widthScale = scale;
        applyRoutePolylineWidth(currentZoomProvider.getZoom());
        applyRoadPolylinesWidth(currentZoomProvider.getZoom());
    }

    public float getWidthScale() {
        return  widthScale;
    }

    private void applyRoutePolylineWidth(float zoom) {
        if (routePolyline == null)
            return;

        routePolyline.setStrokeWidth(calcRouteWidth(zoom) * widthScale);
        routePolyline.setGradientLength(calcGradientLength(zoom));
    }

    private void applyRoadPolylinesWidth(float zoom) {
        float width = calcRouteWidth(zoom) * widthScale;
        for (PolylineMapObject p: roads) {
            p.setStrokeWidth(width + 10);
        }
        for (PolylineMapObject p: vehicleRestrictionRoads) {
            p.setStrokeWidth(width + 10);
        }
    }

    private void applyRouteStyle(PolylineMapObject routePolyline, DrivingRoute route) {
        applyJamsStyle(routePolyline, route);
        applyManeuverStyle(routePolyline, route);
    }

    private void applyJamsStyle(PolylineMapObject routePolyline, DrivingRoute route) {
        if (route == null || routePolyline == null)
            return;

        JamStyle style;
        if (conditionsOutdated()) {
            if (route.getMetadata().getFlags().getBuiltOffline()) {
                style = new JamStyle(offlineAndOutdatedColors());
            } else {
                style = RouteHelper.createDisabledJamStyle();
            }
        }
        else {
            style = new JamStyle(jamStyle.getColors());
        }

        RouteHelper.updatePolyline(routePolyline, route, style, true);
    }

    private void applyManeuverStyle(PolylineMapObject routePolyline, DrivingRoute route) {
        if (route == null || routePolyline == null)
            return;

        ArrowManeuverStyle arrowManeuverStyle = maneuverStyle.getArrow();
        maneuverStyle = new ManeuverStyle(
                new ArrowManeuverStyle(
                        arrowManeuverStyle.getFillColor(),
                        arrowManeuverStyle.getOutlineColor(),
                        arrowManeuverStyle.getOutlineWidth(),
                        arrowManeuverStyle.getLength(),
                        arrowManeuverStyle.getTriangleHeight(),
                        showManeuvers()),
                maneuverStyle.getPolygon());
        RouteHelper.applyManeuverStyle(routePolyline, maneuverStyle);
    }

    private void updateRoadEvents() {
        mapRoadEvents.clear();
        if (!eventsEnabled || route == null || imageProviderFactory == null || conditionsOutdated()) {
            return;
        }
        List<Event> events = route.getEvents();
        for (int i = 0; i < events.size(); ++i) {
            Event event = events.get(i);
            Point point = event.getLocation();
            List<EventTag> tags = event.getTags();

            PlacemarkMapObject placemark = mapRoadEvents.addPlacemark(point);
            placemark.setIcon(imageProviderFactory.fromResource(RoadEventsUtils.resourceId(tags.get(0))));

            if( this.roadEventsTapListener != null )
                placemark.addTapListener(this.roadEventsTapListener);

            Float speedLimit = event.getSpeedLimit();
            String description = event.getDescriptionText();
            placemark.setUserData(
                new EventInfo(
                    event.getEventId(),
                    tags,
                    event.getLocation(),
                    speedLimit != null ? (Float.valueOf(3.6f * speedLimit)).toString() : "Unknown",
                    description != null ? description : "Empty description"
                )
            );
        }
    }

    private void updateSelectedArrivalPoints() {
        mapSelectedArrivalPoints.clear();
        if (!selectedArrivalPointsEnabled || route == null) {
            return;
        }
        for (RoutePoint point: route.getMetadata().getRoutePoints()) {
            if (point.getSelectedArrivalPoint() != null) {
                PlacemarkMapObject placemark = mapSelectedArrivalPoints.addPlacemark(
                        point.getSelectedArrivalPoint());
                placemark.setIcon(IconWithLetter.iconWithLetter(' ', Color.GREEN, 50));
                placemark.setZIndex(101);
            }
            if (point.getDrivingArrivalPointId() != null) {
                List<Point> polyline = routePolyline.getGeometry().getPoints();
                PlacemarkMapObject placemark = mapSelectedArrivalPoints.addPlacemark(
                        polyline.get(polyline.size() - 1));
                placemark.setIcon(IconWithLetter.iconWithLetter(
                        point.getDrivingArrivalPointId().charAt(0),
                        Color.YELLOW, 50));
                placemark.setZIndex(101);
            }
        }
    }

    private void updateRestrictedTurns() {
        if (route == null) {
            return;
        }
        for (RestrictedTurn turn: route.getRestrictedTurns()) {
            PolylinePosition from = PolylineUtils.advancePolylinePosition(route.getGeometry(), turn.getPosition(), -10.0);
            PolylinePosition to = PolylineUtils.advancePolylinePosition(route.getGeometry(), turn.getPosition(), 10.0);
            PolylineMapObject restrictedTurnMark = restrictedTurnMarks.addPolyline(
                SubpolylineHelper.subpolyline(
                    getRoute().getGeometry(),
                    new Subpolyline(from, to)
                )
            );
            restrictedTurnMark.setStrokeColor(0xFFFF0000);
            restrictedTurnMark.setStrokeWidth(routePolyline.getStrokeWidth() * 2.0f);
            restrictedTurnMark.setDashLength(10.0f);
            restrictedTurnMark.setGapLength(10.0f);
            restrictedTurnMark.setZIndex(this.zIndex - 1);
        }
    }

    private void updateTollRoads() {
        if (route == null) {
            return;
        }
        for (TollRoad road: route.getTollRoads()) {
            PolylineMapObject p = mapRoads.addPolyline(
                SubpolylineHelper.subpolyline(route.getGeometry(),
                road.getPosition()));
            p.setStrokeColor(0xFFFFD700);
            p.setZIndex(zIndex - 1);
            roads.add(p);
        }
    }

    private void updateRuggedRoads() {
        if (route == null) {
            return;
        }
        for (RuggedRoad road: route.getRuggedRoads()) {
            Polyline roadPolyline = SubpolylineHelper.subpolyline(
                route.getGeometry(),
                road.getPosition());
            PolylineMapObject p = mapRoads.addPolyline(roadPolyline);
            p.setStrokeColor(0xFF593001);
            p.setZIndex(zIndex - 1);
            roads.add(p);
            if (roadPolyline.getPoints().isEmpty() || imageProviderFactory == null) {
                continue;
            }
            final ImageProvider ruggedBeginsIcon = imageProviderFactory.fromResource(R.drawable.rugged_road_begins);
            final ImageProvider ruggedEndsIcon = imageProviderFactory.fromResource(R.drawable.rugged_road_ends);
            Point ruggedBeginsPoint = roadPolyline.getPoints().get(0);
            PlacemarkMapObject ruggedBeginsPlacemark = mapRoads.addPlacemark(ruggedBeginsPoint);
            ruggedBeginsPlacemark.setIcon(ruggedBeginsIcon);
            ruggedBeginsPlacemark.setZIndex(100);
            Point ruggedEndsPoint = roadPolyline.getPoints().get(roadPolyline.getPoints().size() - 1);
            PlacemarkMapObject ruggedEndsPlacemark = mapRoads.addPlacemark(ruggedEndsPoint);
            ruggedEndsPlacemark.setIcon(ruggedEndsIcon);
            ruggedEndsPlacemark.setZIndex(100);
        }
    }

    private void updateRailwayCrossings() {
        railwayCrossings.clear();
        if (route == null || imageProviderFactory == null) {
            return;
        }
        final ImageProvider icon = imageProviderFactory.fromResource(R.drawable.railway_crossing);
        for (RailwayCrossing railwayCrossing: route.getRailwayCrossings()) {
            Point point = polylinePoint(routePolyline.getGeometry(), railwayCrossing.getPosition());
            PlacemarkMapObject placemark = railwayCrossings.addPlacemark(point);
            placemark.setIcon(icon);
            placemark.setZIndex(100);
        }
    }

    private void updatePedestrianCrossings() {
        pedestrianCrossings.clear();
        if (route == null || imageProviderFactory == null) {
            return;
        }
        final ImageProvider icon = imageProviderFactory.fromResource(R.drawable.pedestrian_crossing);
        for (PedestrianCrossing pedestrianCrossing: route.getPedestrianCrossings()) {
            Point point = polylinePoint(routePolyline.getGeometry(), pedestrianCrossing.getPosition());
            PlacemarkMapObject placemark = pedestrianCrossings.addPlacemark(point);
            placemark.setIcon(icon);
            placemark.setZIndex(100);
        }
    }

    private void updateSpeedBumps() {
        speedBumps.clear();
        if (route == null || imageProviderFactory == null) {
            return;
        }
        final ImageProvider icon = imageProviderFactory.fromResource(R.drawable.speed_bump);
        for (SpeedBump speedBump: route.getSpeedBumps()) {
            Point point = polylinePoint(routePolyline.getGeometry(), speedBump.getPosition());
            PlacemarkMapObject placemark = speedBumps.addPlacemark(point);
            placemark.setIcon(icon);
            placemark.setZIndex(100);
        }
    }

    private void updateFordCrossings() {
        if (route == null) {
            return;
        }
        for (FordCrossing road: route.getFordCrossings()) {
            Polyline roadPolyline = SubpolylineHelper.subpolyline(
                route.getGeometry(),
                road.getPosition());
            PolylineMapObject p = mapRoads.addPolyline(roadPolyline);
            p.setStrokeColor(0xFF03ADFC);
            p.setZIndex(zIndex - 1);
            roads.add(p);
        }
    }

    private void updateVehicleRestrictions() {
        vehicleRestrictions.clear();
        vehicleRestrictionRoads.clear();
        if (route == null || !vehicleRestrictionsEnabled) {
            return;
        }
        for (RoadVehicleRestriction road: route.getRoadVehicleRestrictions()) {
            PolylineMapObject polyline = vehicleRestrictions.addPolyline(
                SubpolylineHelper.subpolyline(route.getGeometry(),
                road.getPosition()));
            polyline.setStrokeColor(0xFFFF2A00);
            polyline.setZIndex(zIndex + 1);
            polyline.setUserData(road.getVehicleRestriction());
            vehicleRestrictionRoads.add(polyline);
            if (vehicleRestrictionsTapListener != null) {
                polyline.addTapListener(vehicleRestrictionsTapListener);
            }
        }
        if (imageProviderFactory == null) {
            return;
        }
        final ImageProvider iconVehicleRestriction = imageProviderFactory.fromResource(R.drawable.other);
        for (ManoeuvreVehicleRestriction manoeuvre: route.getManoeuvreVehicleRestrictions()) {
            Point point = polylinePoint(route.getGeometry(), manoeuvre.getPosition());
            PlacemarkMapObject placemark = vehicleRestrictions.addPlacemark(point);
            placemark.setIcon(iconVehicleRestriction);
            placemark.setZIndex(VEHICLE_RESTRICTION_Z_INDEX);
            placemark.setUserData(manoeuvre.getVehicleRestriction());
            if (vehicleRestrictionsTapListener != null) {
                placemark.addTapListener(vehicleRestrictionsTapListener);
            }
        }
    }

    @Override
    public void onCameraPositionChanged(
            Map map, CameraPosition position, CameraUpdateReason updateReason, boolean finished) {
        if (lastZoom == position.getZoom()) {
            return;
        }
        lastZoom = position.getZoom();
        applyRoutePolylineWidth(position.getZoom());
        applyRoadPolylinesWidth(position.getZoom());
        applyManeuverStyle(routePolyline, route);
    }

    private static float linearStep(float x, float x0, float x1, float y0, float y1) {
        return Utils.lerp((Utils.clamp(x, x0, x1) - x0) / (x1 - x0), y0, y1);
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

    private static Point polylinePoint(Polyline polyline, PolylinePosition position) {
        return SubpolylineHelper.subpolyline(
                polyline, new Subpolyline(position, position)).getPoints().get(0);
    }

    public void setPosition(PolylinePosition position) {
        if (position != null && routePolyline != null)
            routePolyline.hide(new Subpolyline(new PolylinePosition(0, 0.0), position));
    }

    public void setZIndex(float zIndex) {
        this.zIndex  = zIndex;
        if (routePolyline != null) {
            routePolyline.setZIndex(this.zIndex);
        }
        for (PolylineMapObject p: roads) {
            p.setZIndex(this.zIndex - 1);
        }
        for (PolylineMapObject p: vehicleRestrictionRoads) {
            p.setZIndex(this.zIndex - 1);
        }
    }

    public void setText(PolylinePosition position, String text, int color, int textSize) {
        if (routePolyline == null)
            return;

        if (textPlacemark != null) {
            mapObjects.remove(textPlacemark);
            textPlacemark = null;
        }

        Point point = PolylineUtils.pointByPolylinePosition(routePolyline.getGeometry(), position);
        textPlacemark = mapObjects.addPlacemark(point);
        textPlacemark.setIcon(IconWithText.iconWithText(text, color, textSize));
        textPlacemark.setZIndex(TEXT_PLACEMARK_Z_INDEX);
    }

    public void setTapListener(MapObjectTapListener listener) {
        if (tapListener != null && routePolyline != null) {
            routePolyline.removeTapListener(tapListener);
        }
        tapListener = listener;
        if (tapListener != null && routePolyline != null) {
            routePolyline.addTapListener(listener);
        }
    }

    public void enableVehicleRestrictions(MapObjectTapListener listener) {
        vehicleRestrictionsEnabled = true;
        vehicleRestrictionsTapListener = listener;
        updateVehicleRestrictions();
        applyRoadPolylinesWidth(currentZoomProvider.getZoom());
    }

    public void disableVehicleRestrictions() {
        vehicleRestrictionsEnabled = false;
        vehicleRestrictionsTapListener = null;
        vehicleRestrictions.clear();
        vehicleRestrictionRoads.clear();
    }
}
