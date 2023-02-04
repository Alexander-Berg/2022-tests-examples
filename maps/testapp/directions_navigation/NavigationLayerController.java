package com.yandex.maps.testapp.directions_navigation;

import android.content.Context;

import com.yandex.mapkit.directions.DirectionsFactory;
import com.yandex.mapkit.directions.driving.DrivingRoute;
import com.yandex.mapkit.directions.navigation_layer.NavigationLayer;
import com.yandex.mapkit.directions.navigation_layer.RouteView;
import com.yandex.mapkit.directions.navigation_layer.RouteViewListener;
import com.yandex.mapkit.directions.navigation_layer.NavigationLayerListener;
import com.yandex.mapkit.directions.navigation_layer.RoutesSource;
import com.yandex.mapkit.map.MapWindow;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.road_events.EventTag;
import com.yandex.mapkit.road_events_layer.RoadEventsLayer;
import com.yandex.mapkit.styling.roadevents.RoadEventsLayerDefaultStyleProvider;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class NavigationLayerController implements RouteViewListener, NavigationLayerListener {
    private NavigationController navigationController;
    private NavigationLayer navigationLayer;
    private RoadEventsLayer roadEventsLayer;
    private ArrayList<EventTag> visibleRoadEventInLayerTags = new ArrayList();
    private ArrayList<EventTag> visibleRoadEventOnRouteTags = new ArrayList();
    private NavigationStyleProviderImpl navigationStyleProvider;

    public enum JamsMode {
        DISABLED,
        ENABLED_FOR_CURRENT_ROUTE,
        ENABLED
    }

    public enum UserPlacemarkType {
        GENERAL,
        STANDING
    }

    public NavigationLayerController(Context context, NavigationController navigationController, MapWindow mapWindow,
                                     @Nullable RoadEventsLayer existsRoadEventsLayer,
                                     boolean showTrafficLights,
                                     boolean showRoadEventsOnRoute, boolean showRoadEventsOutsideRoute,
                                     boolean showBalloons, JamsMode currentJamsMode, boolean showPredicted,
                                     ArrayList<EventTag> visibleRoadEventTags) {
        this.navigationController = navigationController;
        roadEventsLayer = existsRoadEventsLayer != null ? existsRoadEventsLayer : MapKitFactory.getInstance().createRoadEventsLayer(
                mapWindow,
                new RoadEventsLayerDefaultStyleProvider(context));
        navigationStyleProvider = new NavigationStyleProviderImpl(context);
        navigationLayer = DirectionsFactory.getInstance().createNavigationLayer(
                mapWindow, roadEventsLayer, navigationStyleProvider, navigationController.getNavigation());
        navigationStyleProvider.setShowTrafficLights(showTrafficLights);
        navigationStyleProvider.setShowRoadEventsOnRoute(showRoadEventsOnRoute);
        if (visibleRoadEventTags != null) {
            this.visibleRoadEventInLayerTags = (ArrayList<EventTag>) visibleRoadEventTags.clone();
            this.visibleRoadEventOnRouteTags = (ArrayList<EventTag>) visibleRoadEventTags.clone();
        }
        navigationStyleProvider.setShowBalloons(showBalloons);
        navigationStyleProvider.setCurrentJamsMode(currentJamsMode);
        navigationStyleProvider.setShowPredicted(showPredicted);
        navigationLayer.addRouteViewListener(this);
        navigationLayer.addListener(this);

        roadEventsLayer.setShowRoadEventsOutsideRoute(showRoadEventsOutsideRoute);
        updateVisibleRoadEventTags();
    }

    public void setJamsMode(JamsMode mode) {
        if (navigationStyleProvider.getCurrentJamsMode() == mode)
            return;

        navigationStyleProvider.setCurrentJamsMode(mode);
        navigationLayer.refreshStyle();
    }

    private void updateVisibleRoadEventTags() {
        for (EventTag tag : EventTag.values()) {
            boolean visibleInLayer = visibleRoadEventInLayerTags.contains(tag);
            roadEventsLayer.setRoadEventVisible(tag, visibleInLayer);
            boolean visibleOnRoute = visibleRoadEventOnRouteTags.contains(tag);
            roadEventsLayer.setRoadEventVisibleOnRoute(tag, visibleOnRoute);
        }
    }

    public void setShowTrafficLights(boolean show) {
        if (navigationStyleProvider.getShowTrafficLights() == show)
            return;
        navigationStyleProvider.setShowTrafficLights(show);
        navigationLayer.refreshStyle();
    }

    public void setShowRoadEventsOnRoute(boolean show) {
        if (navigationStyleProvider.getShowRoadEventsOnRoute() == show)
            return;
        navigationStyleProvider.setShowRoadEventsOnRoute(show);
        navigationLayer.refreshStyle();
    }

    public void setShowRoadEventsOutsideRoute(boolean show) {
        roadEventsLayer.setShowRoadEventsOutsideRoute(show);
    }

    public void setVisibleRoadEventInLayerTags(ArrayList<EventTag> tags) {
        visibleRoadEventInLayerTags =
                tags == null ? new ArrayList() : (ArrayList<EventTag>) tags.clone();
        updateVisibleRoadEventTags();
    }

    public void setVisibleRoadEventOnRouteTags(ArrayList<EventTag> tags) {
        visibleRoadEventOnRouteTags =
                tags == null ? new ArrayList() : (ArrayList<EventTag>) tags.clone();
        updateVisibleRoadEventTags();
    }

    public void setShowBalloons(boolean show) {
        if (navigationStyleProvider.getShowBalloons() == show)
            return;
        navigationStyleProvider.setShowBalloons(show);
        navigationLayer.refreshStyle();
    }

    public void setShowPredicted(boolean show) {
        if (navigationStyleProvider.getShowPredicted() == show)
            return;
        navigationStyleProvider.setShowPredicted(show);
        navigationLayer.refreshStyle();
    }

    public @Nullable RouteView getCurrentRoute() {
        return navigationLayer.selectedRoute();
    }

    public @Nullable RouteView getCurrentOrPredictedRoute() {
        RouteView route = navigationLayer.selectedRoute();
        if (route != null) {
            return route;
        }
        if (navigationLayer.getRoutes().size() == 1) {
            route = navigationLayer.getRoutes().get(0);
            if (route.getRoute().getMetadata().getFlags().getPredicted()) {
                return route;
            }
        }
        return null;
    }

    public void removeFromMap() {
        navigationLayer.removeFromMap();
    }

    public NavigationLayer getNavigationLayer() {
        return navigationLayer;
    }

    public RoadEventsLayer getRoadEventsLayer() {
        return roadEventsLayer;
    }

    @Override
    public void onRouteViewsChanged() {
        if (navigationLayer.selectedRoute() == null) {
            if (navigationLayer.getRoutes().size() > 0)
                // routes ordered by relevance. Select the most relevant route if no one selected
                onRouteViewTap(navigationLayer.getRoutes().get(0));
        }
    }

    @Override
    public void onRouteViewTap(@NonNull RouteView routeView) {
        switch (navigationLayer.getRoutesSource()) {
            case NAVIGATION:
                navigationLayer.selectRoute(routeView);
                break;
            case GUIDANCE:
                navigationController.switchToRoute(routeView.getRoute());
                break;
        }
    }

    @Override
    public void onSelectedRouteChanged() {}

    @Override
    public void onRoutesSourceChanged() {}

    public RoutesSource routesSource() {
        return navigationLayer.getRoutesSource();
    }

    public void setUserPlacemark(UserPlacemarkType type) {
        if (navigationStyleProvider.getUserPlacemarkType() == type)
            return;
        navigationStyleProvider.setUserPlacemarkType(type);
        navigationLayer.refreshStyle();
    }
}
