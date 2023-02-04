package com.yandex.maps.testapp.map;

import android.app.Activity;

import com.yandex.mapkit.geometry.BoundingBox;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.MapWindow;
import com.yandex.mapkit.road_events.EventTag;
import com.yandex.mapkit.road_events_layer.RoadEvent;
import com.yandex.mapkit.road_events_layer.RoadEventSignificance;
import com.yandex.mapkit.road_events_layer.RoadEventsLayer;
import com.yandex.mapkit.traffic.TrafficLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

class RoadEventsPerformanceTestScenario extends PerformanceTestScenario {
    RoadEventsPerformanceTestScenario(
            TrafficLayer layer,
            MapWindow mapWindow,
            Callback callback,
            PerformanceTestStep[] steps,
            String name) {
        super(mapWindow, callback, steps, name);
        trafficLayer = layer;
    }

    @Override
    protected void preExecute(Runnable onComplete) {
        visibleTags.clear();
        for (EventTag tag : EventTag.values()) {
            if (trafficLayer.isRoadEventVisible(tag))
                visibleTags.add(tag);
            trafficLayer.setRoadEventVisible(tag, true);
        }

        super.preExecute(onComplete);
    }

    @Override
    protected void postExecute() {
        for (EventTag tag : EventTag.values()) {
            trafficLayer.setRoadEventVisible(tag, false);
        }
        for (EventTag tag : visibleTags) {
            trafficLayer.setRoadEventVisible(tag, true);
        }

        super.postExecute();
    }

    private TrafficLayer trafficLayer;
    private List<EventTag> visibleTags = new ArrayList();
}

class RoadEvents2PerformanceTestScenario extends PerformanceTestScenario {
    final Point MOSCOW_CENTER = new Point(55.75, 37.62);

    class GenerateRouteEvents extends TimerTask {
        @Override
        public void run() {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (generateRouteEventsNatively) {
                        roadEventsLayer.generateRoadEventsOnRoute(
                                routeEventsCount,
                                new BoundingBox(
                                    new Point(
                                        MOSCOW_CENTER.getLatitude() - 0.1,
                                        MOSCOW_CENTER.getLongitude() - 0.1),
                                    new Point(
                                        MOSCOW_CENTER.getLatitude() + 0.1,
                                        MOSCOW_CENTER.getLongitude() + 0.1)));
                    } else {
                        roadEventsLayer.setRoadEventsOnRoute(
                            generateRoadEventsOnRoute());
                    }
                }
            });
        }
    }

    RoadEvents2PerformanceTestScenario(
            RoadEventsLayer layer,
            Activity activity,
            MapWindow mapWindow,
            Callback callback,
            PerformanceTestStep[] steps,
            String name,
            int routeEventsCount,
            boolean generateRouteEventsNatively) {
        super(mapWindow, callback, steps, name);
        this.activity = activity;
        roadEventsLayer = layer;
        this.routeEventsCount = routeEventsCount;
        this.generateRouteEventsNatively = generateRouteEventsNatively;
    }

    private List<EventTag> randomEventTags() {
        EventTag[] allTags = EventTag.values();
        List<EventTag> tags = new ArrayList();
        tags.add(allTags[rand.nextInt(allTags.length)]);
        return tags;
    }

    private List<RoadEvent> generateRoadEventsOnRoute() {
        List<RoadEvent> events = new ArrayList();

        for (int i = 0; i < routeEventsCount; ++i) {
            Double latOffset = rand.nextDouble() * 0.2 - 0.1;
            Double lonOffset = rand.nextDouble() * 0.2 - 0.1;
            RoadEvent event = new RoadEvent(
                    "route_event_id_" + i,
                    new Point(
                            MOSCOW_CENTER.getLatitude() + latOffset,
                            MOSCOW_CENTER.getLongitude() + lonOffset),
                    randomEventTags(),
                    "",
                    false);

            events.add(event);
        }

        return events;
    }

    @Override
    protected void preExecute(Runnable onComplete) {
        // Store layer state before test to restore it after.
        generatedRoadEventsInterval =
                roadEventsLayer.getGeneratedRoadEventsInterval();
        roadEventsLayer.setGeneratedRoadEventsInterval(null);

        visibleTags.clear();
        for (EventTag tag : EventTag.values()) {
            if (roadEventsLayer.isRoadEventVisible(tag))
                visibleTags.add(tag);
            roadEventsLayer.setRoadEventVisible(tag, true);
        }

        if (routeEventsCount > 0) {
            generateRouteEventsTimer = new Timer();
            generateRouteEventsTimer.schedule(new GenerateRouteEvents(), 200, 200);
        }

        super.preExecute(onComplete);
    }

    @Override
    protected void postExecute() {
        if (generateRouteEventsTimer != null) {
            generateRouteEventsTimer.cancel();
            generateRouteEventsTimer = null;
        }

        roadEventsLayer.setGeneratedRoadEventsInterval(
                generatedRoadEventsInterval);

        roadEventsLayer.setRoadEventsOnRoute(new ArrayList());

        for (EventTag tag : EventTag.values()) {
            roadEventsLayer.setRoadEventVisible(tag, false);
        }
        for (EventTag tag : visibleTags) {
            roadEventsLayer.setRoadEventVisible(tag, true);
        }

        super.postExecute();
    }

    private Activity activity;
    private List<EventTag> visibleTags = new ArrayList();
    private Double generatedRoadEventsInterval;
    private RoadEventsLayer roadEventsLayer;
    private Random rand = new Random();
    private int routeEventsCount;
    private boolean generateRouteEventsNatively;
    private Timer generateRouteEventsTimer;
}
