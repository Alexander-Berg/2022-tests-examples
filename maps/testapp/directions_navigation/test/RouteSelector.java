package com.yandex.maps.testapp.directions_navigation.test;

import com.yandex.mapkit.directions.driving.DrivingRoute;

import java.util.List;

public class RouteSelector {
    private final Selector selector;

    public enum SelectorType {
        FIRST_ROUTE_SELECTOR,
        TOLL_FREE_ROUTE_SELECTOR
    }

    public RouteSelector(SelectorType selectorType) {
        this.selector = createSelector(selectorType);
    }

    public DrivingRoute selectRoute(List<DrivingRoute> routes) {
        return selector.selectRoute(routes);
    }

    private interface Selector {
        DrivingRoute selectRoute(List<DrivingRoute> routes);
    }

    private Selector createSelector(SelectorType selectorType) {
        Selector selector = null;
        switch (selectorType) {
            case FIRST_ROUTE_SELECTOR:
                selector = new FirstRouteSelector();
                break;
            case TOLL_FREE_ROUTE_SELECTOR:
                selector = new TollFreeRouteSelector();
                break;
        }
        return selector;
    }

    private static class FirstRouteSelector implements Selector {
        @Override
        public DrivingRoute selectRoute(List<DrivingRoute> routes) {
            if (routes.isEmpty())
                return null;
            return routes.get(0);
        }
    }

    private static class TollFreeRouteSelector implements Selector {
        @Override
        public DrivingRoute selectRoute(List<DrivingRoute> routes) {
            for (DrivingRoute route : routes) {
                if (!route.getMetadata().getFlags().getHasTolls()) {
                    return route;
                }
            }
            return null;
        }
    }
}
