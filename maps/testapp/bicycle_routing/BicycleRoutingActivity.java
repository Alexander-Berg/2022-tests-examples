package com.yandex.maps.testapp.bicycle_routing;

import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;

import com.yandex.mapkit.geometry.BoundingBox;
import com.yandex.mapkit.geometry.BoundingBoxHelper;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.transport.TransportFactory;
import com.yandex.mapkit.transport.bicycle.BicycleRouter;
import com.yandex.mapkit.transport.bicycle.Route;
import com.yandex.mapkit.transport.bicycle.Leg;
import com.yandex.mapkit.transport.bicycle.Session;
import com.yandex.mapkit.transport.bicycle.Summary;
import com.yandex.mapkit.transport.bicycle.SummarySession;
import com.yandex.mapkit.transport.bicycle.VehicleType;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.Utils;
import com.yandex.maps.testapp.common_routing.BaseRoutingActivity;
import com.yandex.maps.testapp.common_routing.PlacemarkManager;
import com.yandex.maps.testapp.common_routing.Selector;
import com.yandex.runtime.Error;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class BicycleRoutingActivity extends BaseRoutingActivity {

    private BicycleRouter router;
    private Session routesRequestSession;
    private Session.RouteListener routeListener;
    private SummarySession summarySession;
    private SummarySession.SummaryListener summaryListener;
    private final RouteSelector routeSelector = new RouteSelector();
    private final LegSelector legSelector = new LegSelector();
    private PlacemarkManager placemarkManager;
    RadioButton bicycleVehicleModeButton;
    RadioButton scooterVehicleModeButton;

    private TextView annotationBox;
    private TextView routingBox;
    private TextView summaryBox;

    private MapObjectManager mapObjectManager;

    public class RouteSelector extends Selector<Route> {
        @Override
        protected void onSelected(@NotNull Route route, int index) {
            mapObjectManager.setRoute(route);
            assert route.getLegs().size() != 0;
            legSelector.reset(route.getLegs());
            legSelector.selectFirst();

            String alternativeCountText = String.format(
                getString(R.string.alternative_x_of_y),
                index + 1,
                routeSelector.getItems().size());
            setTextViewText(
                routingBox,
                alternativeCountText + "\n" +
                String.format(getString(R.string.bicycle_route_annotation),
                    route.getWeight().getDistance().getText(),
                    route.getWeight().getTime().getText(),
                    route.getFlags().getHasAutoRoad(),
                    route.getFlags().getRequiresAccessPass()));
        }
        @Override
        protected void onDeselected(@NotNull Route route, int index) {
            mapObjectManager.setRoute(null);
        }
    }

    public class LegSelector extends Selector<Leg> {
        @Override
        protected void onSelected(@NotNull Leg leg, int index) {
            mapObjectManager.setLeg(leg);
            setTextViewText(
                annotationBox,
                String.format(
                    getText(R.string.bicycle_route_leg_annotation).toString(),
                    getText(R.string.bicycle_ride),
                    leg.getWeight().getDistance().getText(),
                    leg.getWeight().getTime().getText()
                )
            );
        }
        @Override
        protected void onDeselected(@NotNull Leg leg, int index) {
            setTextViewText(annotationBox, getString(R.string.no_route_yet));
            mapObjectManager.setLeg(null);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bicycle_routing);
        mapview.getMap().addInputListener(this);

        router = TransportFactory.getInstance().createBicycleRouter();
        mapObjectManager = new MapObjectManager(mapview.getMap().getMapObjects());

        routeListener = new Session.RouteListener() {
            @Override
            public void onBicycleRoutes(List<Route> routes) {
                routesRequestSession = null;
                routeSelector.reset(routes);

                if (routes == null || routes.size() == 0) {
                    final String noPath = getString(R.string.routing_no_path);
                    setTextViewText(routingBox, noPath);
                    setTextViewText(annotationBox, noPath);
                } else {
                    routeSelector.selectFirst();
                    focusOnRoute();
                }
            }

            public void onBicycleRoutesError(Error error) {
                routesRequestSession = null;
                routeSelector.reset(Collections.emptyList());
                legSelector.reset(Collections.emptyList());
                cancelRequests();
                setTextViewText(
                    routingBox,
                    String.format(
                        getString(R.string.routing_error),
                        error.toString()));
            }
        };

        bicycleVehicleModeButton = findViewById(R.id.bicycle_vehicle_type);
        scooterVehicleModeButton = findViewById(R.id.scooter_vehicle_type);

        annotationBox = findViewById(R.id.annotation_box);
        routingBox = findViewById(R.id.routing_box);
        summaryBox = findViewById(R.id.summary_box);

        summaryListener = new SummarySession.SummaryListener() {
            public void onBicycleSummaries(List<Summary> summaries) {
                summarySession = null;
                if (summaries == null || summaries.size() == 0) {
                    setTextViewText(summaryBox, "No summaries for given points");
                    return;
                }
                setTextViewText(
                    summaryBox,
                    String.format(getString(R.string.bicycle_route_summary),
                        summaries.get(0).getWeight().getDistance().getText(),
                        summaries.get(0).getWeight().getTime().getText(),
                        summaries.get(0).getFlags().getHasAutoRoad(),
                        summaries.get(0).getFlags().getRequiresAccessPass())
                );

            }
            public void onBicycleSummariesError(Error error) {
                summarySession = null;
                setTextViewText(
                    summaryBox,
                    String.format(
                        getString(R.string.summary_error),
                        error.toString()
                    )
                );
            }
        };

        placemarkManager = new PlacemarkManager(
                mapview.getMap().getMapObjects().addCollection(),
                new com.yandex.maps.testapp.common_routing.WayPoint.MoveListener() {
                    @Override
                    public void onPlacemarkMoved() {
                        trySubmitRoutingRequest();
                    }
                });
    }


    private VehicleType currentVehicleType()
    {
        if (bicycleVehicleModeButton.isChecked())
            return VehicleType.BICYCLE;

        return VehicleType.SCOOTER;
    }

    private void trySubmitRoutingRequest() {
        if (!placemarkManager.isReady()) {
            return;
        }
        cancelRequests();
        setTextViewText(routingBox, getString(R.string.routing_pending));
        routesRequestSession = router.requestRoutes(
            placemarkManager.getRequestPoints(),
            currentVehicleType(),
            routeListener);
        summarySession = router.requestRoutesSummary(
            placemarkManager.getRequestPoints(),
            currentVehicleType(),
            summaryListener);
    }

    private void cancelRequests() {
        setTextViewText(routingBox, getString(R.string.routing_cancel));
        setTextViewText(summaryBox, "");
        if (routesRequestSession != null) {
            routesRequestSession.cancel();
            routesRequestSession = null;
        }
        if (summarySession != null) {
            summarySession.cancel();
            summarySession = null;
        }
    }

    @Override
    protected BoundingBox getRouteBoundingBox() {
        Route route = routeSelector.getCurrent();
        if (route == null) {
            return null;
        }
        return BoundingBoxHelper.getBounds(route.getGeometry());
    }

    public void onSelectPrevAlternative(View view) {
        routeSelector.selectPrev();
        focusOnRoute();
    }

    public void onSelectNextAlternative(View view) {
        routeSelector.selectNext();
        focusOnRoute();
    }

    public void onSelectPrevLeg(View view) {
        legSelector.selectPrev();
    }

    public void onSelectNextLeg(View view) {
        legSelector.selectNext();
    }

    @Override
    public void onMapLongTap(Map map, Point position) {
        placemarkManager.append(position);
        trySubmitRoutingRequest();
    }

    public void onClearRouteTap(View view) {
        Utils.hideKeyboard(this);
        placemarkManager.reset();
        legSelector.reset(Collections.emptyList());
        routeSelector.reset(Collections.emptyList());
        setTextViewText(annotationBox, getString(R.string.no_route_yet));
        cancelRequests();
        setTextViewText(routingBox, getString(R.string.routing_select_from));
    }

    private static void setTextViewText(TextView view, String text) {
        view.setText(text, TextView.BufferType.NORMAL);
    }

}
