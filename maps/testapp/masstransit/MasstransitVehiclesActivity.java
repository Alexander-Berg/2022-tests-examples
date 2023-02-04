package com.yandex.maps.testapp.masstransit;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Handler;
import android.util.LruCache;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.yandex.mapkit.Animation;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.geometry.Polyline;
import com.yandex.mapkit.map.CameraListener;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.CameraUpdateReason;
import com.yandex.mapkit.map.CircleMapObject;
import com.yandex.mapkit.map.ClusterizedPlacemarkCollection;
import com.yandex.mapkit.map.CompositeIcon;
import com.yandex.mapkit.map.IconStyle;
import com.yandex.mapkit.map.MapObject;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.MapObjectCollectionListener;
import com.yandex.mapkit.map.MapObjectTapListener;
import com.yandex.mapkit.map.MapObjectVisitor;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.map.PolygonMapObject;
import com.yandex.mapkit.map.PolylineMapObject;
import com.yandex.mapkit.map.RotationType;
import com.yandex.mapkit.transport.TransportFactory;
import com.yandex.mapkit.transport.masstransit.Line;
import com.yandex.mapkit.transport.masstransit.LineInfo;
import com.yandex.mapkit.transport.masstransit.LineSession;
import com.yandex.mapkit.transport.masstransit.MasstransitInfoService;
import com.yandex.mapkit.transport.masstransit.MasstransitLayer;
import com.yandex.mapkit.transport.masstransit.MasstransitVehicleTapListener;
import com.yandex.mapkit.transport.masstransit.ThreadInfo;
import com.yandex.mapkit.transport.masstransit.ThreadSession;
import com.yandex.mapkit.transport.masstransit.Vehicle;
import com.yandex.mapkit.transport.masstransit.VehicleData;
import com.yandex.mapkit.transport.masstransit.VehicleSession;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.Utils;
import com.yandex.maps.testapp.map.MapBaseActivity;
import com.yandex.runtime.Error;
import com.yandex.runtime.image.ImageProvider;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class MasstransitVehiclesActivity extends MapBaseActivity {

    private class CompositeIconLayers {
        @NotNull final ImageProvider arrowIcon;
        @NotNull final ImageProvider innerIcon;

        CompositeIconLayers(@NotNull ImageProvider arrowIcon, @NotNull ImageProvider innerIcon) {
            this.arrowIcon = arrowIcon;
            this.innerIcon = innerIcon;
        }
    }

    private MasstransitInfoService mtInfoService;
    private MapObjectCollection threadGeometry;
    private List<ThreadInfo> threads;
    private int selectedThreadIndex;
    private Line selectedLine;
    private MasstransitLayer masstransitLayer;

    private static Logger LOGGER = Logger.getLogger("yandex.maps");

    // Indicates whether the text icon is placed to the left or to the right of the vehicle icon
    // for the specific vehicle. We need this to decide whether we should update the text icon
    // when the vehicle's azimuth changes
    private Map<String, Boolean> vehicleIconTextOnLeftEdge = new HashMap<>();
    private Map<String, CompositeIconLayers> vehicleIcons = new HashMap<>();
    private VehicleIconDrawer iconDrawer;

    private HashSet<String> vehicleTypeFilter = new HashSet<>(4);

    private LruCache<String, Line> lineIdsInView = new LruCache<>(100);
    private HashSet<String> lineIdFilter = new HashSet<>(100);

    private AlertDialog filterDialog;

    private PlacemarkMapObject selectedVehicle;

    private CheckBox iconsModeCheckbox;
    private boolean useClientIconsForVehicles;

    private float mapAzimuth;
    private Handler mapRotatedEventHandler = new Handler();

    private TextView tappedVehicleInfoTextview;
    private Handler tappedVehicleInfoHideTaskHandler = new Handler();
    private Runnable tappedVehicleInfoHideTask = new Runnable() {
        @Override
        public void run() {
            tappedVehicleInfoTextview.setVisibility(View.GONE);
        }
    };

    // Must be a class member to keep the listener alive
    private final MapObjectCollectionListener vehiclesCollectionListener = new MapObjectCollectionListener() {
        @Override
        public void onMapObjectAdded(MapObject mapObject) {
            PlacemarkMapObject placemark = (PlacemarkMapObject)mapObject;
            VehicleData vehicle = (VehicleData)mapObject.getUserData();
            if (useClientIconsForVehicles) {
                setVehicleIcon(vehicle, placemark.useCompositeIcon());
                setVehicleNameIcon(vehicle, placemark.useCompositeIcon(), false);
            }
            lineIdsInView.put(vehicle.getLine().getId(), vehicle.getLine());
        }

        @Override
        public void onMapObjectRemoved(MapObject mapObject) {
            final String id = ((VehicleData)mapObject.getUserData()).getId();
            vehicleIconTextOnLeftEdge.remove(id);
        }

        @Override
        public void onMapObjectUpdated(MapObject mapObject) {
            updateVehicleNameIcon((PlacemarkMapObject)mapObject);
        }
    };

    // This listener is used to select and deselect vehicles
    private final MapObjectTapListener vehicleCollectionTapListener = new MapObjectTapListener() {
        @Override
        public boolean onMapObjectTap(MapObject mapObject, Point point) {
            onTapped((PlacemarkMapObject)mapObject);
            return true;
        }
    };

    // This listener shows text notification when a vehicle is tapped
    // It demonstrates that layer's tap listener works
    private final MasstransitVehicleTapListener vehicleTapListener = new MasstransitVehicleTapListener() {
        @Override
        public boolean onVehicleTap(VehicleData vehicle) {
            final Context context = getApplicationContext();
            String vehicleName = vehicle.getLine().getName();
            List<String> vehicleTypes = vehicle.getLine().getVehicleTypes();
            tappedVehicleInfoTextview.setText(
                String.format(context.getString(R.string.masstransit_vehicle_tap_message),
                    vehicle.getLine().getIsNight() ? context.getString(R.string.masstransit_night) : "",
                    vehicleTypes.get(0), vehicleName));
            tappedVehicleInfoTextview.setVisibility(View.VISIBLE);

            mtInfoService.vehicle(vehicle.getId(), new VehicleSession.VehicleListener() {
                @Override
                public void onVehicleResponse(Vehicle vehicle) {
                    Vehicle.Properties properties = vehicle.getProperties();
                    Toast.makeText(getApplicationContext(), propertiesToString(properties), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onVehicleError(Error error) {
                    Toast.makeText(getApplicationContext(), R.string.masstransit_could_not_fetch_vehicle_info, Toast.LENGTH_SHORT).show();
                }
            });

            // Hide the text view after 3 seconds
            // In user taps another vehicle, and the text message changes, restart the timer
            tappedVehicleInfoHideTaskHandler.removeCallbacks(tappedVehicleInfoHideTask);
            tappedVehicleInfoHideTaskHandler.postDelayed(tappedVehicleInfoHideTask, 3000);

            return false;
        }

        private String propertiesToString(Vehicle.Properties properties) {
            final Context context = getApplicationContext();
            final StringBuilder propertiesBuilder = new StringBuilder();

            propertiesBuilder.append(propertyToString("wheelchair accessible", properties.getWheelchairAccessible()))
                    .append(propertyToString("low floor", properties.getLowFloor()))
                    .append(propertyToString("to depot", properties.getToDepot()))
                    .append(propertyToString("bikes allowed", properties.getBikesAllowed()))
                    .append(propertyToString("air conditioning", properties.getAirConditioning()));

            if (propertiesBuilder.toString().isEmpty())
                propertiesBuilder.append(context.getString(R.string.masstransit_no_vehicle_properties));

            return propertiesBuilder.toString();
        }

        private String propertyToString(String propertyName, Object propertyValue) {
            final Context context = getApplicationContext();
            // If property is null then it's not specified
            if (propertyValue != null)
                return String.format(context.getString(R.string.masstransit_property),
                        propertyName, propertyValue.toString());
            else
                return "";
        }
    };

    private final CameraListener cameraListener = new CameraListener() {
        @Override
        public void onCameraPositionChanged(com.yandex.mapkit.map.Map map,
            CameraPosition cameraPosition, CameraUpdateReason cameraUpdateReason, boolean finished) {

            final float AZIMUTH_EPS = 1.0f; // as in Y-Transport
            final float curMapAzimuth = cameraPosition.getAzimuth();
            if (Math.abs(curMapAzimuth - mapAzimuth) > AZIMUTH_EPS &&
                    Math.abs(curMapAzimuth - mapAzimuth) < 360.0 - AZIMUTH_EPS) {
                mapAzimuth = curMapAzimuth;
                // Fire the event only if camera was not moving for at least 200ms
                // Y-Transport do this to prevent the high number of icon updates when user
                // rotates the map
                mapRotatedEventHandler.removeCallbacks(updateAllVehicleNameIcons);
                mapRotatedEventHandler.postDelayed(updateAllVehicleNameIcons, 200);
            }
        }
    };

    private final Runnable updateAllVehicleNameIcons = new Runnable() {
        @Override
        public void run() {
            masstransitLayer.getVehicleObjects().traverse(new MapObjectVisitor() {
                @Override
                public void onPlacemarkVisited(PlacemarkMapObject placemark) {
                    updateVehicleNameIcon(placemark);
                }
                @Override
                public void onPolylineVisited(PolylineMapObject polyline) { }
                @Override
                public void onPolygonVisited(PolygonMapObject polygon) { }
                @Override
                public void onCircleVisited(CircleMapObject circle) { }
                @Override
                public boolean onCollectionVisitStart(MapObjectCollection collection) {
                    return true;
                }
                @Override
                public void onCollectionVisitEnd(MapObjectCollection collection) { }
                @Override
                public boolean onClusterizedCollectionVisitStart(ClusterizedPlacemarkCollection collection) {
                    return true;
                }
                @Override
                public void onClusterizedCollectionVisitEnd(ClusterizedPlacemarkCollection collection) {
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.hideKeyboard(this);
        setContentView(R.layout.map_masstransit_vehicles);
        super.onCreate(savedInstanceState);

        iconsModeCheckbox = (CheckBox)findViewById(R.id.change_vehicle_icons_mode);
        setUseClientIconsForVehicles(true);
        initIcons(getApplicationContext());

        tappedVehicleInfoTextview = (TextView)findViewById(R.id.tapped_vehicle_info_text);

        mapAzimuth = mapview.getMap().getCameraPosition().getAzimuth();

        masstransitLayer = TransportFactory.getInstance().createMasstransitLayer(mapview.getMapWindow());

        mapview.getMap().addCameraListener(cameraListener);
        masstransitLayer.setVehiclesVisible(true);
        masstransitLayer.getVehicleObjects().addListener(vehiclesCollectionListener);
        masstransitLayer.getVehicleObjects().addTapListener(vehicleCollectionTapListener);
        masstransitLayer.setVehicleTapListener(vehicleTapListener);

        mtInfoService = TransportFactory.getInstance().createMasstransitInfoService();
        threadGeometry = mapview.getMap().getMapObjects().addCollection();
    }

    public void onLineFilterButtonClick(View view) {
        if (filterDialog != null && filterDialog.isShowing())
            return;

        Map<String, String> lineOptions = new HashMap<>();
        for (Map.Entry<String, Line> x : lineIdsInView.snapshot().entrySet()) {
            Line line = x.getValue();
            String label = "";
            if (!line.getVehicleTypes().isEmpty()) {
                label += line.getVehicleTypes().get(0) +  " ";
            }
            lineOptions.put(line.getId(), label + line.getName());
        }

        showFilterDialog(
            lineOptions,
            lineIdFilter,
            R.string.masstransit_vehicle_line_filter_btn,
            new Runnable() {
                @Override
                public void run() {
                    masstransitLayer.clearLineFilter();
                    if (lineIdFilter.size() < lineOptions.size()) {
                        for (String lineId : lineIdFilter) {
                            masstransitLayer.addLineFilter(lineId);
                        }
                    }
                }
            });
    }

    public void onLineRequestButtonClick(View view) {
        findViewById(R.id.thread_choice_layout).setVisibility(View.VISIBLE);
        findViewById(R.id.request_line_button).setVisibility(View.GONE);

        mtInfoService.line(selectedLine.getId(), new LineSession.LineListener() {
            @Override
            public void onLineResponse(LineInfo lineInfo) {
                threads = lineInfo.getThreads();
                selectedThreadIndex = 0;
                unselectThread();
                selectThread(threads.get(0));
            }

            @Override
            public void onLineError(Error error) {
                LOGGER.info("Got " + error.toString() + " while requesting line " + selectedLine.getId());
            }
        });
    }

    public void onNextThreadButtonClick(View view) {
        if (threads != null && selectedThreadIndex + 1 < threads.size()) {
            ++selectedThreadIndex;
            unselectThread();
            selectThread(threads.get(selectedThreadIndex));
        }
    }

    public void onPreviousThreadButtonClick(View view) {
        if (threads != null && selectedThreadIndex > 0) {
            --selectedThreadIndex;
            unselectThread();
            selectThread(threads.get(selectedThreadIndex));
        }
    }

    public void updateSummary() {
        Context context = getApplicationContext();
        ((TextView)findViewById(R.id.thread_name)).setText(
                String.format(
                        context.getString(R.string.masstransit_thread_summary),
                        selectedLine.getName(),
                        selectedLine.getId(),
                        selectedThreadIndex + 1,
                        threads.size(),
                        threads.get(selectedThreadIndex).getThread().getId()));
    }

    public void unselectThread() {
        threadGeometry.clear();
    }

    public void selectThread(ThreadInfo threadInfo) {
        for (Polyline p : threadInfo.getStages()) {
            threadGeometry.addPolyline(p);
        }
        if (threads != null) {
            updateSummary();
        }
    }

    public void onTypeFilterButtonClick(View view) {
        if (filterDialog != null && filterDialog.isShowing())
            return;

        // Let us limit the number of vehicle types shown in the filter dialog to four
        // for the sake of UI simplicity
        List<String> availableTypes = Arrays.asList(
            "bus",
            "minibus",
            "tramway",
            "trolleybus"
        );

        showFilterDialog(
            availableTypes,
            vehicleTypeFilter,
            R.string.masstransit_vehicle_type_filter_btn,
            new Runnable() {
                @Override
                public void run() {
                    masstransitLayer.clearTypeFilter();
                    if (vehicleTypeFilter.size() < availableTypes.size()) {
                        for (String type : vehicleTypeFilter) {
                            masstransitLayer.addTypeFilter(type);
                        }
                    }
                }
            });
    }

    public void onVehicleIconsModeChanged(View view)
    {
        setUseClientIconsForVehicles(!useClientIconsForVehicles);
        // This flushes all existing vehicle objects and guarantees that we will get the
        // onMapObjectAdded event for each vehicle on the screen
        com.yandex.mapkit.map.Map map = mapview.getMap();
        masstransitLayer.setVehiclesVisible(false);
        masstransitLayer.setVehiclesVisible(true);
        // Masstransit layer is currently waiting until the visible region gets updated
        // Update the visible region manually in order to wake up the masstransit layer
        map.move(map.getCameraPosition(), new Animation(Animation.Type.SMOOTH, 0.0f), null);
    }

    public void onTapped(@NotNull PlacemarkMapObject placemark) {
        unselectThread();
        findViewById(R.id.thread_choice_layout).setVisibility(View.GONE);
        findViewById(R.id.request_line_button).setVisibility(View.GONE);
        threads = null;

        if (selectedVehicle != null && selectedVehicle.isValid()) {
            if (useClientIconsForVehicles) {
                // Restore normal vehicle name icon for the previously selected vehicle
                setVehicleNameIcon((VehicleData) selectedVehicle.getUserData(),
                    selectedVehicle.useCompositeIcon(), false);
            }
            // If user tapped the same placemark twice - deselect the placemark
            if (selectedVehicle == placemark) {
                selectedVehicle = null;
                return;
            }
        }

        findViewById(R.id.request_line_button).setVisibility(View.VISIBLE);
        selectedVehicle = placemark;
        VehicleData vehicleData = (VehicleData)selectedVehicle.getUserData();
        final String selectedThreadId = vehicleData.getThreadId();
        selectedLine = vehicleData.getLine();
        if (useClientIconsForVehicles)
            setVehicleNameIcon(vehicleData,
                selectedVehicle.useCompositeIcon(), true);

        mtInfoService.thread(selectedThreadId, new ThreadSession.ThreadListener() {
            @Override
            public void onThreadResponse(ThreadInfo threadInfo) {
                selectThread(threadInfo);
            }

            @Override
            public void onThreadError(Error error) {
                LOGGER.info("Got " + error.toString() + " while requesting thread " + selectedThreadId);
            }

        });
    }

    private void showFilterDialog(
        @NotNull final List<String> availableOptions,
        @NotNull final HashSet<String> selectedOptions,
        int titleId,
        @NotNull Runnable onSelected)
    {
        Map<String, String> map = new HashMap<>();
        for (String option : availableOptions) {
            map.put(option, option);
        }
        showFilterDialog(map, selectedOptions, titleId, onSelected);
    }

    private <T> void showFilterDialog(
        @NotNull final Map<T, String> availableOptions,
        @NotNull final HashSet<T> selectedOptions,
        int titleId,
        @NotNull Runnable onSelected)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(titleId);

        // Put selected options to the top of the list
        Set<T> deselectedOptions = new HashSet<T>(availableOptions.keySet());
        deselectedOptions.removeAll(selectedOptions);

        List<T> all = new ArrayList<>(selectedOptions);
        all.addAll(deselectedOptions);

        final boolean[] selected = new boolean[availableOptions.size()];

        if (selectedOptions.isEmpty())
            // No filter - everything is being shown
            Arrays.fill(selected, true);
        else
            // Use filter - select options which are in the filter now
            Arrays.fill(selected, 0, selectedOptions.size(), true);

        String[] labels = new String[all.size()];
        for (int i = 0; i < all.size(); ++i) {
            labels[i] = availableOptions.get(all.get(i));
        }

        builder.setMultiChoiceItems(
            labels,
            selected,
            new DialogInterface.OnMultiChoiceClickListener() {
                @Override
                public void onClick(
                    DialogInterface dialog, int which, boolean isChecked)
                {}
            });

        builder.setNegativeButton(R.string.masstransit_vehicle_filter_uncheck_all, null);
        builder.setPositiveButton(R.string.masstransit_vehicle_filter_apply, null);
        builder.setNeutralButton(R.string.masstransit_vehicle_filter_clear, null);

        filterDialog = builder.show();

        filterDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedOptions.clear();
                    ListView view = filterDialog.getListView();
                    SparseBooleanArray checked = view.getCheckedItemPositions();
                    for (int i = 0; i < view.getCount(); ++i) {
                        if (checked.get(i)) {
                            selectedOptions.add(all.get(i));
                        }
                    }
                    filterDialog.dismiss();
                    onSelected.run();
                }
        });

        // 'Clear filter' button
        filterDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedOptions.clear();
                    filterDialog.dismiss();
                    onSelected.run();
                }
            });

        // 'Uncheck all' button
        filterDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Arrays.fill(selected, false);
                    filterDialog.getListView().clearChoices();
                    filterDialog.getListView().invalidateViews();
                }
            }
        );
    }

    private static @NotNull ArrayList<String> getSelectedItems(@NotNull AlertDialog dialog)
    {
        ArrayList<String> result = new ArrayList<>();

        ListView list = dialog.getListView();
        int count = list.getCount();

        for (int i = 0; i < count; ++i)
            if (list.isItemChecked(i))
                result.add((String)list.getItemAtPosition(i));

        return result;
    }

    private void setVehicleIcon(@NotNull VehicleData vehicle, @NotNull CompositeIcon icon) {
        CompositeIconLayers layers = getCompositeIconLayers(vehicle);
        icon.setIcon("arrow", layers.arrowIcon, new IconStyle()
            .setRotationType(RotationType.ROTATE).setZIndex(1.f).setAnchor(new PointF(0.5f, 0.585f)));
        icon.setIcon("inner", layers.innerIcon, (new IconStyle()).setZIndex(2.f));
    }

    private void setVehicleNameIcon(@NotNull VehicleData vehicle, @NotNull CompositeIcon icon,
        boolean checked) {
        CompositeIconLayers layers = getCompositeIconLayers(vehicle);
        final int vehicleIconWidth = layers.arrowIcon.getImage().getWidth();
        final boolean textOnLeftEdge = isTextOnLeftEdge(vehicle);

        ImageProvider nameIcon = iconDrawer.getDrawableWithBackground(vehicle, vehicleIconWidth,
                !textOnLeftEdge, checked);

        float nameImageAnchorX = 0.f;
        if (textOnLeftEdge)
            nameImageAnchorX = 1.f;

        icon.setIcon("name", nameIcon, (new IconStyle()).setZIndex(0.f)
             .setAnchor(new PointF(nameImageAnchorX, 0.5f)));

        vehicleIconTextOnLeftEdge.put(vehicle.getId(), textOnLeftEdge);
    }

    private void updateVehicleNameIcon(@NotNull PlacemarkMapObject placemark)
    {
        VehicleData vehicle = (VehicleData)placemark.getUserData();
        final String id = vehicle.getId();
        if (!vehicleIconTextOnLeftEdge.containsKey(id)
                || vehicleIconTextOnLeftEdge.get(id) != isTextOnLeftEdge(vehicle)) {
            // Should change the text position
            final boolean isChecked = selectedVehicle != null && selectedVehicle.isValid()
                    && selectedVehicle == placemark;
            setVehicleNameIcon(vehicle, placemark.useCompositeIcon(), isChecked);
        }
    }

    private boolean isTextOnLeftEdge(@NotNull VehicleData vehicle)
    {
        float screenSpaceAzimuth = vehicle.getCurrentAzimuth() - mapAzimuth;
        if (screenSpaceAzimuth < 0.0f)
            screenSpaceAzimuth += 360.0f;
        return screenSpaceAzimuth < 180.f;
    }

    private void initIcons(@NotNull Context context) {
        iconDrawer = new VehicleIconDrawer(context);

        vehicleIcons.put("bus", new CompositeIconLayers(
            ImageProvider.fromResource(context, R.drawable.bus_icon_16_18),
            ImageProvider.fromResource(context, R.drawable.bus_inner_icon)));
        vehicleIcons.put("minibus", new CompositeIconLayers(
            ImageProvider.fromResource(context, R.drawable.minibus_icon_16_18),
            ImageProvider.fromResource(context, R.drawable.minibus_inner_icon)));
        vehicleIcons.put("tramway", new CompositeIconLayers(
            ImageProvider.fromResource(context, R.drawable.tramway_icon_16_18),
            ImageProvider.fromResource(context, R.drawable.tramway_inner_icon)));
        vehicleIcons.put("trolleybus", new CompositeIconLayers(
            ImageProvider.fromResource(context, R.drawable.trolleybus_icon_16_18),
            ImageProvider.fromResource(context, R.drawable.trolleybus_inner_icon)));
    }

    private @NotNull CompositeIconLayers getCompositeIconLayers(@NotNull VehicleData vehicle) {
        String type = "bus";
        for (String t : vehicle.getLine().getVehicleTypes())
            if (vehicleIcons.containsKey(t)) {
                type = t;
                break;
            }
        return vehicleIcons.get(type);
    }

    private void setUseClientIconsForVehicles(boolean value) {
        if (value != useClientIconsForVehicles) {
            useClientIconsForVehicles = value;
            iconsModeCheckbox.setChecked(value);
        }
    }
}
