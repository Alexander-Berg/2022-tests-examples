package com.yandex.maps.testapp.masstransit_routing;

import androidx.fragment.app.Fragment;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.yandex.mapkit.geometry.BoundingBox;
import com.yandex.mapkit.geometry.BoundingBoxHelper;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.transport.TransportFactory;
import com.yandex.mapkit.transport.masstransit.TransitOptions;
import com.yandex.mapkit.transport.masstransit.FilterVehicleTypes;
import com.yandex.mapkit.transport.masstransit.MasstransitRouter;
import com.yandex.mapkit.transport.masstransit.Route;
import com.yandex.mapkit.transport.masstransit.Section;
import com.yandex.mapkit.transport.masstransit.Session;
import com.yandex.mapkit.transport.masstransit.Summary;
import com.yandex.mapkit.transport.masstransit.SummarySession;
import com.yandex.mapkit.transport.masstransit.TimeOptions;
import com.yandex.mapkit.transport.masstransit.TravelEstimation;
import com.yandex.mapkit.transport.masstransit.Weight;
import com.yandex.mapkit.uri.UriObjectMetadata;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.Utils;
import com.yandex.maps.testapp.common_routing.*;
import com.yandex.runtime.Error;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;


public class MasstransitRoutingActivity
    extends BaseRoutingActivity
    implements
        VehicleTypesProvider.Listener,
        TimeSettingsFragment.Listener,
        Session.RouteListener,
        SummarySession.SummaryListener
{
    private TextView routeBox;
    private TextView summaryBox;
    private TextView annotationBox;
    private ImageButton showAlertsButton;

    private VehicleTypesProvider vehicleTypesProvider = new VehicleTypesProvider(this, this);
    private List<String> avoidTypes = Collections.emptyList();

    private final MasstransitRouter masstransitRouter =
        TransportFactory.getInstance().createMasstransitRouter();
    private Session routeSession;
    private SummarySession summarySession;

    private EditText uriInput;
    private TextView uriOut;

    private TimeOptions timeOptions = new TimeOptions();

    private PlacemarkManager placemarkManager;
    private RouteSelector routeSelector = new RouteSelector();
    private SectionSelector sectionSelector = new SectionSelector();
    private MapObjectManager mapObjectManager;

    private static Logger LOGGER = Logger.getLogger("yandex.maps");

    private void onRequestSent() {
        LOGGER.info("Submitted routing requests");
        sectionSelector.reset(Collections.emptyList());
        routeSelector.reset(Collections.emptyList());
    }

    @Override
    public void onMasstransitRoutes(@NotNull List<Route> routes) {
        LOGGER.info("Got masstransit routing response");

        if (routes.size() == 0) {
            setTextViewText(routeBox, getString(R.string.routing_no_path));
        } else {
            routeSelector.reset(routes);
            routeSelector.selectFirst();

            if (placemarkManager.getRequestPoints().size() != 0) {
                return;
            }

            ArrayList<Point> points = new ArrayList<>();
            for (com.yandex.mapkit.transport.masstransit.WayPoint wp :
                    routeSelector.getCurrent().getWayPoints()) {
                points.add(wp.getPosition());
            }
            placemarkManager.reset(points);
        }
    }
    @Override
    public void onMasstransitRoutesError(@NotNull Error error) {
        LOGGER.info("Got masstransit routing error: " + error.toString());
        setTextViewText(
            routeBox,
            String.format(getString(R.string.routing_error), error.toString()));
    }
    @Override
    public void onMasstransitSummaries(@NotNull List<Summary> summaries) {
        LOGGER.info("Got masstransit summaries response");
        if (!summaries.isEmpty()) {
            Weight weight = summaries.get(0).getWeight();
            TravelEstimation estimation = summaries.get(0).getEstimation();
            String message = String.format(
                "Summary: Time: %s, Walking distance: %s, Transfers: %d",
                weight.getTime().getText(),
                weight.getWalkingDistance().getText(),
                weight.getTransfersCount());

            if (estimation != null) {
                message += String.format(
                    ", Departure: %s, Arrival: %s",
                    estimation.getDepartureTime().getText(),
                    estimation.getArrivalTime().getText());
            }
            setTextViewText(summaryBox, message);
        } else {
            setTextViewText(summaryBox, "No summary found");
        }
    }
    @Override
    public void onMasstransitSummariesError(@NotNull Error error) {
        LOGGER.info("Got masstransit summary error: " + error.toString());
        setTextViewText(
            summaryBox,
            String.format(getString(R.string.summary_error), error.toString()));
    }

    @Override
    public void onVehicleTypesSelected(@NotNull List<String> types) {
        avoidTypes = types;
        trySubmitRoutingRequest();
    }

    private class RouteSelector extends Selector<Route> {
        @Override
        protected void onSelected(@NotNull Route route, int index) {
            mapObjectManager.setRoute(route);
            focusOnBoundingBox(BoundingBoxHelper.getBounds(route.getGeometry()));

            selectMasstransitRoute(route);

            String alternativeCounts = String.format(
                getString(R.string.alternative_x_of_y), index + 1, getItems().size());
            String routeMetadata = Helpers.formatMetadata(
                route.getMetadata(), MasstransitRoutingActivity.this);
            setTextViewText(routeBox, String.format("%s\n%s", alternativeCounts, routeMetadata));

            UriObjectMetadata uriMetadata = route.getUriMetadata();
            if (uriMetadata.getUris().isEmpty()) {
                uriOut.setText("There are no uris in route object");
                return;
            }
            LOGGER.info("Setting uri " + uriMetadata.getUris().get(0).getValue());
            uriOut.setText(uriMetadata.getUris().get(0).getValue());
        }
        @Override
        protected void onDeselected(@NotNull Route route, int index) {
            mapObjectManager.setRoute(null);
            mapObjectManager.makeRestrictedEntriesFrom(null);
            uriOut.setText("");
        }
        private void selectMasstransitRoute(@NotNull Route route) {
            List<AnnotatedSection> sections = new ArrayList<>();
            for (Section section : route.getSections()) {
                sections.add(
                    new AnnotatedSection(
                        section,
                        route,
                        MasstransitRoutingActivity.this));
            }

            sectionSelector.reset(sections);
            assert !sections.isEmpty();
            sectionSelector.selectFirst();

            LOGGER.info("segment count: " + sectionSelector.getItems().size());
            mapObjectManager.makeRestrictedEntriesFrom(sections);
        }
    }

    private class SectionSelector extends Selector<AnnotatedSection> {
        @Override
        protected void onSelected(@NotNull AnnotatedSection section, int index) {
            mapObjectManager.setSection(section);
            setTextViewText(annotationBox, section.getAnnotation());

            if (section.getAlertsText() != null) {
                showAlertsButton.setVisibility(View.VISIBLE);
            } else {
                showAlertsButton.setVisibility(View.GONE);
            }
        }

        @Override
        protected void onDeselected(@NotNull AnnotatedSection section, int index) {
            mapObjectManager.setSection(null);
            setTextViewText(annotationBox, getString(R.string.no_route_yet));
            showAlertsButton.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_masstransit_routing);
        mapview.getMap().addInputListener(this);
        routeBox = (TextView)findViewById(R.id.routing_box);
        summaryBox = (TextView)findViewById(R.id.summary_box);
        annotationBox = (TextView)findViewById(R.id.route_list_box);
        showAlertsButton = (ImageButton)findViewById(R.id.mtroute_show_alerts_btn);

        uriInput = (EditText)findViewById(R.id.input_uri);
        uriOut = (TextView)findViewById(R.id.uri_box);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        placemarkManager = new PlacemarkManager(
            mapview.getMap().getMapObjects().addCollection(),
            new WayPoint.MoveListener() {
                @Override
                public void onPlacemarkMoved() {
                    trySubmitRoutingRequest();
                }
            });

        mapObjectManager = new MapObjectManager(mapview.getMap().getMapObjects().addCollection());
    }

    @Override
    public void onMapLongTap(Map map, Point position) {
        placemarkManager.append(position);
        trySubmitRoutingRequest();
    }

    public void onClearRouteTap(View view) {
        Utils.hideKeyboard(this);
        resetRouteAndPoints();
        cancelRequests();
        setTextViewText(routeBox, getString(R.string.routing_select_from));
        setTextViewText(summaryBox, "");
        uriInput.setText("");
    }

    public void onSetFiltersTap(View view) {
        Utils.hideKeyboard(this);
        vehicleTypesProvider.poll();
    }

    public void onSetTimeTap(View view) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.time_settings_fragment);
        ((TimeSettingsFragment)fragment).show();
    }

    @Override
    public void onTimeSettingsChanged(@NotNull TimeOptions options) {
        timeOptions = options;
        trySubmitRoutingRequest();
    }


    public void onSelectAlternative(View view) {
        int id = view.getId();

        if (id == R.id.next_alternative) {
            routeSelector.selectNext();
        } else {
            routeSelector.selectPrev();
        }
    }

    public void onSelectSection(View view) {
        int id = view.getId();

        if (id == R.id.next_annotation) {
            sectionSelector.selectNext();
        } else {
            sectionSelector.selectPrev();
        }
    }

    public void onAnnotationBoxTap(View view) {
        AnnotatedSection section = sectionSelector.getCurrent();
        if (section != null) {
            focusOnBoundingBox(section.getBoundingBox());
        }
    }

    public void onResolveUri(View view) {
        Utils.hideKeyboard(this);
        String uri = uriInput.getText().toString();
        if (uri.isEmpty()) {
            Utils.showMessage(this, "Input uri to resolve");
            return;
        }
        placemarkManager.reset();
        routeSession = masstransitRouter.resolveUri(uri, timeOptions, this);

        onRequestSent();
    }

    public void onUriBoxTap(View view) {
        String uri = uriOut.getText().toString();
        uriInput.setText(uri);
    }

    private void setTextViewText(@NotNull TextView view, String text) {
        view.setText(text, TextView.BufferType.NORMAL);
    }

    private void resetRouteAndPoints() {
        placemarkManager.reset();
        sectionSelector.reset(Collections.emptyList());
        routeSelector.reset(Collections.emptyList());
    }

    @Override
    protected BoundingBox getRouteBoundingBox() {
        Route route = routeSelector.getCurrent();
        if (route == null) {
            return null;
        }
        return BoundingBoxHelper.getBounds(route.getGeometry());
    }

    private void cancelRequests() {
        if (routeSession != null) {
            routeSession.cancel();
            routeSession = null;
        }
        if (summarySession != null) {
            summarySession.cancel();
            summarySession = null;
        }
    }

    private void trySubmitRoutingRequest() {
        if (!placemarkManager.isReady()) {
            return;
        }

        cancelRequests();

        setTextViewText(routeBox, getString(R.string.routing_pending));
        setTextViewText(summaryBox, "");
        LOGGER.info("Submitting routing requests");

        Integer filterMask = FilterVehicleTypes.NONE.value;
        for (String type : avoidTypes) {
            Integer typeMask = FilterVehicleTypes.valueOf(type.toUpperCase()).value;
            filterMask |= typeMask;
        }
        TransitOptions options = new TransitOptions(
            filterMask,
            timeOptions);
        summarySession = masstransitRouter.requestRoutesSummary(
            placemarkManager.getRequestPoints(), options, this);
        routeSession = masstransitRouter.requestRoutes(
            placemarkManager.getRequestPoints(), options, this);

        onRequestSent();
    }

    public void onShowAlertsTap(View view) {
        findViewById(R.id.mtroute_alerts_panel).setVisibility(View.VISIBLE);
        TextView textView = (TextView)findViewById(R.id.mtroute_alerts_panel_text);
        AnnotatedSection section = sectionSelector.getCurrent();
        if (section != null) {
            textView.setText(section.getAlertsText());
        } else {
            textView.setText("");
        }
    }

    public void onCloseAlertsPanelTap(View view) {
        findViewById(R.id.mtroute_alerts_panel).setVisibility(View.GONE);
    }
}
