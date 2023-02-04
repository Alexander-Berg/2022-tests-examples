package com.yandex.maps.testapp.driving;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.text.InputType;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.yandex.mapkit.Animation;
import com.yandex.mapkit.RequestPoint;
import com.yandex.mapkit.directions.DirectionsFactory;
import com.yandex.mapkit.directions.driving.ConditionsListener;
import com.yandex.mapkit.directions.driving.DrivingOptions;
import com.yandex.mapkit.directions.driving.DrivingRoute;
import com.yandex.mapkit.directions.driving.DrivingRouteMetadata;
import com.yandex.mapkit.directions.driving.DrivingRouter;
import com.yandex.mapkit.directions.driving.DrivingSection;
import com.yandex.mapkit.directions.driving.DrivingSession.DrivingRouteListener;
import com.yandex.mapkit.directions.driving.DrivingSummarySession;
import com.yandex.mapkit.directions.driving.VehicleOptions;
import com.yandex.mapkit.directions.driving.VehicleRestriction;
import com.yandex.mapkit.road_events.EventTag;
import com.yandex.mapkit.directions.driving.Flags;
import com.yandex.mapkit.directions.driving.Summary;
import com.yandex.mapkit.geometry.BoundingBox;
import com.yandex.mapkit.geometry.BoundingBoxHelper;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.geometry.PolylinePosition;
import com.yandex.mapkit.geometry.SubpolylineHelper;
import com.yandex.mapkit.map.InputListener;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.map.MapObject;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.MapObjectDragListener;
import com.yandex.mapkit.map.MapObjectTapListener;
import com.yandex.mapkit.map.RotationType;
import com.yandex.mapkit.map.IconStyle;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.map.PolylineMapObject;

import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.map.MapBaseActivity;
import com.yandex.maps.testapp.Utils;
import com.yandex.runtime.Error;
import com.yandex.runtime.image.ImageProvider;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import java.text.SimpleDateFormat;

import static androidx.core.content.PermissionChecker.PERMISSION_GRANTED;

class RouteDisplay extends RouteView implements ConditionsListener {

    private TextView sectionAnnotation;
    private TextView routeInfo;
    private TextView conditionsTime;

    private Button copyUriButton;

    private MapObjectCollection mapUserPoints;

    private PolylineMapObject highlightedSectionMapObject;
    private int highlightedSectionIndex = 0;

    private DrivingSection getHighlightedSection() {
        return getRoute().getSections().get(highlightedSectionIndex);
    }

    private Context context;
    private Map map;

    public RouteDisplay(
            final Context context,
            Map map,
            TextView sectionAnnotation,
            TextView routeInfo,
            TextView conditionsTime,
            Button copyUriButton,
            MapObjectTapListener tapListener) {
        super(map, tapListener, new RouteView.ImageProviderFactory() {
            @Override
            public ImageProvider fromResource(int resourceId) {
                return ImageProvider.fromResource(context, resourceId);
            }
        });
        this.setSelectedArrivalPointsEnabled(true);
        this.context = context;
        this.map = map;
        this.sectionAnnotation = sectionAnnotation;
        this.routeInfo = routeInfo;
        this.conditionsTime = conditionsTime;
        this.copyUriButton = copyUriButton;

        mapUserPoints = map.getMapObjects().addCollection();
    }

    public int getHighlightedSectionIndex() {
        return highlightedSectionIndex;
    }

    public PlacemarkMapObject addWaypoint(
            Point point, char letter, MapObjectDragListener listener, int color) {
        PlacemarkMapObject placemark = mapUserPoints.addPlacemark(point);
        placemark.setIcon(IconWithLetter.iconWithLetter(letter, color));
        placemark.setDraggable(true);
        placemark.setDragListener(listener);
        placemark.setZIndex(100);
        return placemark;
    }

    public void resetWaypoints() {
        mapUserPoints.clear();
    }

    private String formatMetadata(DrivingRouteMetadata metadata) {
        StringBuilder text = new StringBuilder();

        // Format route weight
        String distance = metadata.getWeight().getDistance().getText();
        String timeWithTraffic = metadata.getWeight().getTimeWithTraffic().getText();
        String timeWithoutTraffic = metadata.getWeight().getTime().getText();
        text.append(String.format(
            context.getString(R.string.driving_success),
            distance, timeWithTraffic, timeWithoutTraffic));
        text.append("\n");

        // Format condition flags
        Flags f = metadata.getFlags();
        ArrayList<String> flags = new ArrayList<>();
        if (f.getBlocked()) { flags.add(context.getString(R.string.driving_blocked)); }
        if (f.getFutureBlocked()) { flags.add(context.getString(R.string.driving_future_blocked)); }
        if (f.getHasTolls()) { flags.add(context.getString(R.string.driving_has_tolls)); }
        if (f.getHasFerries()) { flags.add(context.getString(R.string.driving_has_ferries)); }
        if (f.getHasFordCrossing()) { flags.add(context.getString(R.string.driving_has_ford_crossing)); }
        if (f.getHasRuggedRoads()) { flags.add(context.getString(R.string.driving_has_rugged_roads)); }
        if (f.getHasUnpavedRoads()) { flags.add(context.getString(R.string.driving_has_unpaved_roads)); }
        if (f.getHasInPoorConditionRoads()) { flags.add(context.getString(R.string.driving_has_roads_in_poor_condition)); }
        if (f.getHasVehicleRestrictions()) { flags.add(context.getString(R.string.driving_has_vehicle_restrictions)); }
        if (f.getCrossesBorders()) {
            flags.add(context.getString(R.string.driving_crosses_borders));
        }
        if (f.getRequiresAccessPass()) {
            flags.add(context.getString(R.string.driving_requires_access_pass));
        }
        if (f.getBuiltOffline()) {
            flags.add(context.getString(R.string.driving_route_was_built_offline));
        }
        text.append("flags: ");
        String delimiter = "";
        for (String flag: flags) {
            text.append(delimiter);
            text.append(flag);
            delimiter = ", ";
        }

        // Format tags
        text.append(", tags: ");
        delimiter = "";
        for (String tag : metadata.getTags()) {
            text.append(delimiter);
            text.append(tag);
            delimiter = ", ";
        }

        // Format via streets
        String viaStreets = metadata.getDescription().getVia();
        if (viaStreets == null) {
            viaStreets = "<>";
        }
        text.append(", via: " + viaStreets);

        // Format URI
        String uri = metadata.getUri();
        if (uri != null) {
            text.append(", uri: " + uri);
        }

        return text.toString();
    }

    public void resetRoute() {
        super.setRoute(null);

        if (highlightedSectionMapObject != null) {
            map.getMapObjects().remove(highlightedSectionMapObject);
            highlightedSectionMapObject = null;
        }

        sectionAnnotation.setText(context.getString(R.string.driving_no_route_yet));
        routeInfo.setText(context.getString(R.string.driving_no_route_yet));
        copyUriButton.setVisibility(View.GONE);
    }

    public void displayRoute(DrivingRoute route, int alternativeIndex, int routesCount, boolean hasOriginalRoute) {
        if (getRoute() != null) {
            getRoute().removeConditionsListener(this);
        }
        setRoute(route);
        getRoute().addConditionsListener(this);
        updateJamsTime();

        String formattedMetadata = formatMetadata(route.getMetadata());
        String routeTitle;
        if (!hasOriginalRoute) {
            routeTitle = String.format(context.getString(R.string.driving_alternative_x_of_y), alternativeIndex + 1, routesCount);
        } else if (alternativeIndex > 0) {
            routeTitle = String.format(context.getString(R.string.driving_alternative_x_of_plus_y), alternativeIndex, routesCount - 1);
        } else {
            routeTitle = String.format(context.getString(R.string.driving_alternative_orig), routesCount - 1);
        }
        routeInfo.setText(routeTitle + "\n" + formattedMetadata);
        copyUriButton.setVisibility(View.VISIBLE);

        highlightSection(0);
    }

    public void highlightSection(int index) {
        highlightedSectionIndex = index;
        if (highlightedSectionMapObject != null) {
            map.getMapObjects().remove(highlightedSectionMapObject);
            highlightedSectionMapObject = null;
        }
        if (getRoute() == null || index >= getRoute().getSections().size()) {
            return;
        }
        highlightedSectionMapObject = map.getMapObjects().addPolyline(
                SubpolylineHelper.subpolyline(getRoute().getGeometry(), getHighlightedSection().getGeometry()));
        highlightedSectionMapObject.setStrokeColor(Color.BLUE);
        highlightedSectionMapObject.setZIndex(100);
        sectionAnnotation.setText(getHighlightedSection().getMetadata().getAnnotation().getDescriptionText());
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

    private void focusOnBoundingBox(BoundingBox bbox) {
        BoundingBox bbox2 = inflatedBoundingBox(bbox, 1.8);
        map.move(map.cameraPosition(bbox2), new Animation(Animation.Type.SMOOTH, 0.2f), null);
    }

    public void focusOnRoute() {
        if (getRoutePolyline() != null) {
            focusOnBoundingBox(BoundingBoxHelper.getBounds(getRoutePolyline().getGeometry()));
        }
    }

    public void focusOnSection() {
        if (highlightedSectionMapObject != null) {
            focusOnBoundingBox(BoundingBoxHelper.getBounds(
                highlightedSectionMapObject.getGeometry()));
        }
    }

    private void updateJamsTime() {
        conditionsTime.setText("Jams time: " + (new SimpleDateFormat("HH:mm:ss").format(new Date())));
    }

    @Override
    public void onConditionsUpdated() {
        updateJamsTime();
    }

    @Override
    public void onConditionsOutdated() {}
}

public class DrivingActivity
        extends MapBaseActivity
        implements MapObjectDragListener,
        InputListener,
        MapObjectTapListener
{
    private static Logger LOGGER = Logger.getLogger("yandex.maps");

    private static final int TEST_CASES_REQUEST_CODE = 0;

    private TextView sectionAnnotation;
    private TextView routeInfo;
    private TextView conditionsTime;

    private CheckBox summaryCheckBox;

    private Button copyUriButton;

    private Router router = new Router();
    private VehicleOptions vehicleOptions = new VehicleOptions();

    private List<DrivingRoute> routes = new ArrayList<>();
    private int alternativeIndex = 0;

    private PolylinePosition inroutePosition;

    private DrivingRoute getRoute() {
        return routes.get(alternativeIndex);
    }

    private RouteDisplay routeDisplay;

    private boolean hasOriginalRoute = false;

    private boolean useSummaryReuqest = false;

    private void selectAlternative(int alternativeIndex) {
        this.alternativeIndex = alternativeIndex;

        routeDisplay.displayRoute(getRoute(), alternativeIndex, routes.size(), hasOriginalRoute);
    }

    private void addUserPoint(Point point) {
        int waypointIndex = router.addWaypoint(point);
        addPointToRouteDisplay(point, waypointIndex, Color.RED, waypointIndex);
    }

    private void addUserAlternativePoint(Point point) {
        int waypointIndex = router.addAlternativeWaypoint(point);
        addPointToRouteDisplay(point, waypointIndex, Color.BLUE, null);
    }

    private void addPointToRouteDisplay(Point point, int index, int color, Object userData) {
        char letter = (char) ('A' + index);
        routeDisplay.addWaypoint(point, letter, this, color).setUserData(userData);
    }

    private void resetRouteAndPoints() {
        routes = new ArrayList<>();
        sectionAnnotation.setText(getString(R.string.driving_no_route_yet));
        routeInfo.setText(getString(R.string.driving_no_route_yet));
        conditionsTime.setText("");
        routeDisplay.resetRoute();
        routeDisplay.resetWaypoints();
        router.clear();
    }

    private DrivingOptions makeDrivingOptions() {
        DrivingOptions options = new DrivingOptions();
        options.setAvoidTolls(
                ((CheckBox)findViewById(R.id.drivingAvoidTolls)).isChecked());
        options.setAvoidUnpaved(
                ((CheckBox)findViewById(R.id.drivingAvoidUnpaved)).isChecked());
        options.setAvoidPoorConditions(
                ((CheckBox)findViewById(R.id.drivingAvoidPoorConditions)).isChecked());
        return options;
    }

    private void submitRoutingRequest(boolean focusOnResult) {
        router.cancel();
        hasOriginalRoute = false;
        routeDisplay.resetRoute();

        routeInfo.setText(getString(R.string.driving_building_route));

        final DrivingRouteListener listener = new DrivingRouteListener() {
            @Override
            public void onDrivingRoutes(@NonNull List<DrivingRoute> routes) {
                receiveResponse(routes);
                if (focusOnResult) {
                    routeDisplay.focusOnRoute();
                }
            }

            @Override
            public void onDrivingRoutesError(@NonNull Error error) {
                receiveResponseError(error);
            }
        };

        router.requestRoute(makeDrivingOptions(), this.vehicleOptions, listener);
    }

    private void submitSummaryRequest() {
        router.cancel();
        hasOriginalRoute = false;
        routeDisplay.resetRoute();

        routeInfo.setText(getString(R.string.driving_building_summary));

        final DrivingSummarySession.DrivingSummaryListener listener = new DrivingSummarySession.DrivingSummaryListener() {

            @Override
            public void onDrivingSummaries(List<Summary> summaries) {
                receiveSummaryResponse(summaries);
            }

            @Override
            public void onDrivingSummariesError(Error error) {
                receiveResponseError(error);
            }
        };

        router.requestRouteSummary(makeDrivingOptions(), this.vehicleOptions, listener);
    }

    private void submitReroutingRequest() {
        router.cancel();
        routeDisplay.resetRoute();

        routeInfo.setText(getString(R.string.driving_building_reroute));

        inroutePosition = getRoute().getSections().get(routeDisplay.getHighlightedSectionIndex()).getGeometry().getBegin();

        router.requestAlternativesForRoute(
            getRoute(),
            inroutePosition,
            makeDrivingOptions(),
            this.vehicleOptions,
            new DrivingRouteListener() {
                @Override
                public void onDrivingRoutes(List<DrivingRoute> routes) {
                    hasOriginalRoute = true;
                    int savedSectionIndex = routeDisplay.getHighlightedSectionIndex();
                    List<DrivingRoute> routesFixed = new ArrayList<>();
                    routesFixed.add(getRoute());
                    routesFixed.addAll(routes);
                    receiveResponse(routesFixed);

                    routeDisplay.highlightSection(savedSectionIndex);
                }

                @Override
                public void onDrivingRoutesError(Error error) {
                    receiveResponseError(error);
                }
            });
    }

    private void submitResolveUriRequest(String uri) {
        router.cancel();
        hasOriginalRoute = false;
        routeDisplay.resetRoute();

        routeInfo.setText(getString(R.string.driving_building_route));

        router.resolveUri(
                uri,
                makeDrivingOptions(),
                vehicleOptions,
                new DrivingRouteListener() {
                    @Override
                    public void onDrivingRoutes(@NonNull List<DrivingRoute> routes) {
                        receiveResponse(routes);
                    }

                    @Override
                    public void onDrivingRoutesError(@NonNull Error error) {
                        receiveResponseError(error);
                    }
                }
        );
    }

    private void receiveSummaryResponse(List<Summary> summaries) {
        if (summaries.size() > 0) {
            routeInfo.setText("Time: " + summaries.get(0).getWeight().getTimeWithTraffic().getText());
        } else {
            routeInfo.setText(getString(R.string.driving_no_path));
        }
    }

    private void receiveResponse(List<DrivingRoute> routes) {
        this.routes = routes;
        if (routes.isEmpty()) {
            routeInfo.setText(getString(R.string.driving_no_path));
            routeDisplay.onConditionsOutdated();
        } else {
            this.alternativeIndex = 0;

            routeDisplay.displayRoute(getRoute(), alternativeIndex, routes.size(), hasOriginalRoute);
        }
    }

    private void receiveResponseError(Error error) {
        LOGGER.info("Got driving error: " + error.toString());
        routeDisplay.resetRoute();
        routeInfo.setText(String.format(getString(R.string.driving_error), error.toString()));
    }


    private void addPoint(Point point) {
        if (useSummaryReuqest) {
            addUserPoint(point);
            if (router.waypointsCount() > 1) {
                submitSummaryRequest();
            }
        } else {
            addUserPoint(point);
            if (router.waypointsCount() > 1) {
                submitRoutingRequest(/*focusOnResult*/ false);
            }
        }
    }

    private void addAlternativePoint(Point point) {
        if (router.waypointsCount() > 0) {
            addUserAlternativePoint(point);
            if (router.waypointsCount() > 1) {
                submitRoutingRequest(/*focusOnResult*/ false);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.driving);
        super.onCreate(savedInstanceState);

        routeInfo = findViewById(R.id.routing_box);
        conditionsTime = findViewById(R.id.conditions_time);
        sectionAnnotation = findViewById(R.id.route_list_box);
        copyUriButton = findViewById(R.id.copy_uri_button);
        summaryCheckBox = findViewById(R.id.summary);
        CheckBox jamsEnabled = findViewById(R.id.jamsCheckbox);
        jamsEnabled.setChecked(true);
        jamsEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> routeDisplay.setJamsEnabled(isChecked));
        CheckBox eventsEnabled = findViewById(R.id.eventsCheckbox);
        eventsEnabled.setChecked(false);
        eventsEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> routeDisplay.setEventsEnabled(isChecked));

        routeInfo.setMovementMethod(new ScrollingMovementMethod());

        router = new Router();
        routeDisplay = new RouteDisplay(
                this, mapview.getMap(), sectionAnnotation,
                routeInfo, conditionsTime, copyUriButton, this);

        Bitmap arrowIcon = ImageProvider.fromResource(
                getApplicationContext(), R.drawable.navigation_icon).getImage();
        PlacemarkMapObject locationPoint = mapview.getMap().getMapObjects().addPlacemark(new Point(0, 0));
        locationPoint.setZIndex(50.0f);
        locationPoint.setIcon(
            ImageProvider.fromBitmap(arrowIcon),
            new IconStyle().setFlat(true).setRotationType(RotationType.ROTATE));
        locationPoint.setVisible(false);

        mapview.getMap().addInputListener(this);

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{ android.Manifest.permission.WRITE_EXTERNAL_STORAGE }, 0);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void onClearRouteTap(View view) {
        resetRouteAndPoints();
        router.cancel();
    }

    public void onShowRouteTap(View view) {
        if (!routes.isEmpty()) {
            routeDisplay.focusOnRoute();
        }
    }

    public void onRerouteTap(View view) {
        if (!routes.isEmpty())
            submitReroutingRequest();
    }

    public void onSelectAlternative(View view) {
        int id = view.getId();

        if (id == R.id.next_alternative) {
            if (alternativeIndex + 1 < routes.size()) {
                selectAlternative(alternativeIndex + 1);
            }
        } else {
            if (alternativeIndex > 0) {
                selectAlternative(alternativeIndex - 1);
            }
        }
    }

    public void onSelectSection(View view) {
        if (routes.isEmpty()) {
            return;
        }

        int id = view.getId();

        if (id == R.id.next_annotation) {
            if (routeDisplay.getHighlightedSectionIndex() + 1 < getRoute().getSections().size()) {
                routeDisplay.highlightSection(routeDisplay.getHighlightedSectionIndex() + 1);
            }
        } else {
            if (routeDisplay.getHighlightedSectionIndex() > 0) {
                routeDisplay.highlightSection(routeDisplay.getHighlightedSectionIndex() - 1);
            }
        }
    }

    public void onSectionAnnotationBoxTap(View view) {
        if (!routes.isEmpty()) {
            routeDisplay.focusOnSection();
        }
    }

    @Override
    public void onMapTap(Map map, Point position) {
        router.cancel();
        addAlternativePoint(position);
    }

    @Override
    public void onMapLongTap(Map map, Point position) {
        router.cancel();
        addPoint(position);
    }

    @Override
    public void onMapObjectDragStart(MapObject mapObject) {}

    @Override
    public void onMapObjectDrag(MapObject mapObject, Point point) {
        Integer index = (Integer)mapObject.getUserData();
        if (index == null) {
            return;  // can not drag arrival points
        }
        router.setWaypoint(index, point);
    }

    @Override
    public void onMapObjectDragEnd(MapObject mapObject) {
        if (router.waypointsCount() > 1) {
            submitRoutingRequest(/*focusOnResult*/ false);
        }
    }

    @Override
    public boolean onMapObjectTap(MapObject mapObject, Point point) {
        EventInfo ei = (EventInfo)mapObject.getUserData();
        StringBuilder builder = new StringBuilder();
        for (EventTag tag : ei.tags) {
            builder.append(tag.name());
            builder.append(",");
        }
        String title = builder.toString();
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder
                .setTitle(title)
                .setMessage("speed limit: " + ei.speedLimit + "\nDescription: " + ei.descriptionText)
                .setIcon(RoadEventsUtils.resourceId(ei.tags.get(0)))
                .setNeutralButton("OK", (dialog, id) -> dialog.cancel());
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
        return true;
    }

    public void onCopyUriTap(View view) {
        assert routeDisplay.getRoute() != null;
        String routeUri = routeDisplay.getRoute().getMetadata().getUri();

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("URI", routeUri);
        clipboard.setPrimaryClip(clip);
    }

    public void onResolveUriTap(View view) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = clipboard.getPrimaryClip();
        if (clipData == null || clipData.getItemCount() == 0) {
            return;
        }
        String routeUri = clipData.getItemAt(0).coerceToText(getApplicationContext()).toString();
        submitResolveUriRequest(routeUri);
    }

    public void onSelectTestCase(View view) {
        Intent intent = new Intent(this, TestCasesActivity.class);
        startActivityForResult(intent, TEST_CASES_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TEST_CASES_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            launchTestcase(data.getParcelableExtra("testCase"));
        }
    }

    private void launchTestcase(TestCase testCase) {
        router.clear();
        for (RequestPoint routePoint: testCase.getRoutePoints()) {
            int index = router.addRequestPoint(routePoint);
            addPointToRouteDisplay(routePoint.getPoint(), index, Color.RED, index);
        }
        submitRoutingRequest(/*focusOnResult*/ true);
    }

    public void onSummaryValueChanged(View view) {
        useSummaryReuqest = summaryCheckBox.isChecked();
    }

    public void onVehicleRestrictionsValueChanged(View view) {
        if (((CheckBox)view).isChecked()) {
            enableVehicleRestrictions();
        } else {
            disableVehicleRestrictions();
        }
    }

    public void saveRoute(View view) {
        if (routeDisplay.getRoute() == null) {
            return;
        }
        showSaveDialog();
    }

    public void onSetVehicleOptionsTap(View view) {
        Utils.hideKeyboard(this);
        VehicleOptionsProvider.poll(
            this,
            vehicleOptions,
            (vehicleOptions) -> {
                this.vehicleOptions = vehicleOptions;
                if (router.waypointsCount() > 1) {
                    submitRoutingRequest(/*focusOnResult*/ false);
                }
            });
    }

    private void showSaveDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Route file name...");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText("route");
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> save(routeDisplay.getRoute(), input.getText().toString()));
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void save(DrivingRoute route, String fileName) {
        DrivingRouter drivingRouter = DirectionsFactory.getInstance().createDrivingRouter();
        byte[] data = drivingRouter.routeSerializer().save(route);

        File root = Environment.getExternalStorageDirectory();
        File file = new File(root, fileName + ".bin");
        BufferedOutputStream stream = null;
        try {
            stream = new BufferedOutputStream(new FileOutputStream(file));
            stream.write(data);
            stream.flush();
            LOGGER.info("Saved route into " + file.getAbsolutePath());
        } catch (FileNotFoundException e) {
            LOGGER.severe(e.getMessage());
        } catch (IOException e) {
            LOGGER.severe(e.getMessage());
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    LOGGER.severe(e.getMessage());
                }
            }
        }
    }

    private void enableVehicleRestrictions() {
        Context context = this;
        routeDisplay.enableVehicleRestrictions(new MapObjectTapListener() {
            @Override
            public boolean onMapObjectTap(MapObject mapObject, Point point) {
                VehicleRestriction vehicleRestriction = (VehicleRestriction)mapObject.getUserData();
                StringBuilder builder = new StringBuilder();
                Float weightLimit = vehicleRestriction.getWeightLimit();
                if (weightLimit != null) {
                    builder.append("\n WeightLimit: ");
                    builder.append(weightLimit.toString());
                }
                Float maxWeightLimit = vehicleRestriction.getMaxWeightLimit();
                if (maxWeightLimit != null) {
                    builder.append("\n MaxWeightLimit: ");
                    builder.append(maxWeightLimit.toString());
                }
                Float payloadLimit = vehicleRestriction.getPayloadLimit();
                if (payloadLimit != null) {
                    builder.append("\n PayloadLimit: ");
                    builder.append(payloadLimit.toString());
                }
                Float heightLimit = vehicleRestriction.getHeightLimit();
                if (heightLimit != null) {
                    builder.append("\n HeightLimit: ");
                    builder.append(heightLimit.toString());
                }
                Float widthLimit = vehicleRestriction.getWidthLimit();
                if (widthLimit != null) {
                    builder.append("\n WidthLimit: ");
                    builder.append(widthLimit.toString());
                }
                Float lengthLimit = vehicleRestriction.getLengthLimit();
                if (lengthLimit != null) {
                    builder.append("\n LengthLimit: ");
                    builder.append(lengthLimit.toString());
                }
                Integer minEcoClass = vehicleRestriction.getMinEcoClass();
                if (minEcoClass != null) {
                    builder.append("\n MinEcoClass: ");
                    builder.append(minEcoClass.toString());
                }
                Boolean trailerNotAllowed = vehicleRestriction.getTrailerNotAllowed();
                if (trailerNotAllowed != null) {
                    builder.append("\n TrailerNotAllowed: ");
                    builder.append(trailerNotAllowed.toString());
                }
                Boolean legal = vehicleRestriction.getLegal();
                if (legal != null) {
                    builder.append("\n Legal: ");
                    builder.append(legal.toString());
                }
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
                alertDialogBuilder
                        .setTitle("Vehicle Restrictions")
                        .setMessage(builder.toString())
                        .setNeutralButton("OK", (dialog, id) -> dialog.cancel());
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
                return true;
            }
        });
    }

    public void onUpdateConditionsTap(View view) {
        for (DrivingRoute route : routes) {
            route.requestConditionsUpdate();
        }
    }

    private void disableVehicleRestrictions() {
        routeDisplay.disableVehicleRestrictions();
    }
}
