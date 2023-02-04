package com.yandex.maps.testapp.map;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.fragment.app.Fragment;

import com.yandex.mapkit.Animation;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.CameraListener;
import com.yandex.mapkit.map.CameraUpdateReason;
import com.yandex.mapkit.map.InputListener;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.RequestPoint;
import com.yandex.mapkit.RequestPointType;
import com.yandex.mapkit.road_events.EventTag;
import com.yandex.mapkit.road_events_layer.HighlightedRoadEvent;
import com.yandex.mapkit.road_events_layer.RoadEvent;
import com.yandex.mapkit.road_events_layer.StyleProvider;
import com.yandex.mapkit.road_events_layer.RoadEventsLayer;
import com.yandex.mapkit.road_events_layer.RoadEventsLayerListener;
import com.yandex.mapkit.road_events_layer.HighlightMode;
import com.yandex.maps.testapp.map.RoadEventsLayerStyleProvider;
import com.yandex.mapkit.user_location.UserLocationLayer;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.map.MapRoadEvents2TestCaseController;
import com.yandex.runtime.bindings.Serialization;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class MapRoadEvents2Activity extends MapBaseActivity
        implements InputListener,
            RoadEventsLayerListener,
            CameraListener,
            MapRoadEvents2ActionsFragment.MapRoadEvents2ActionsDockContext
{
    public void onActionsClick(View view) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag(roadEventsActionsFragmentTag);

        if (fragment == null) {
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            ViewGroup container = findViewById(R.id.submenu_container);
            container.bringToFront();
            container.invalidate();

            fragment = new MapRoadEvents2ActionsFragment(
                    userLocationLayer.isVisible(),
                    zoomAnimation);
            fragmentTransaction.add(R.id.submenu_container, fragment, roadEventsActionsFragmentTag);
            fragmentTransaction.addToBackStack(roadEventsActionsBackStackName);
            fragmentTransaction.commit();
        } else {
            fragmentManager.popBackStack(roadEventsActionsBackStackName, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    public void onTagsClick(View view) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.findFragmentByTag(roadEventsActionsFragmentTag) != null) {
            fragmentManager.popBackStack(roadEventsActionsBackStackName, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }

        Intent intent = new Intent(this, MapRoadEventsTagsActivity.class);
        intent.putExtra("RoadEventsTagsState", getTagsState());
        intent.putExtra("DisabledRoadEventTags", new RoadEventTagsList(Arrays.asList(EventTag.CHAT)));
        startActivityForResult(intent, REQUEST_CODE_TAGS);
    }

    public void onClickBack(View view) {
        finish();
    }

    @Override
    public void onUserLocationClicked(boolean enable) {
        userLocationLayer.setVisible(enable);
    }

    @Override
    public void onZoomAnimationClicked(boolean enable) {
        zoomAnimation = enable;
    }

    @Override
    public void onChangeZoomClicked() {
        CameraPosition position = userLocationLayer.cameraPosition();
        if (position == null) {
            return;
        }
        CameraPosition newPosition = new CameraPosition(
                position.getTarget(),
                position.getZoom() > 8 ? mapview.getMap().getMinZoom() : 17.f,
                position.getAzimuth(),
                position.getTilt());
        if (zoomAnimation) {
            mapview.getMap().move(
                newPosition, 
                new Animation(Animation.Type.SMOOTH, 0.1f), 
                null);
        } else {
            mapview.getMap().move(newPosition);
        }
    }

    @Override
    public void onRunNoLocationTestClicked() {
        List<RequestPoint> routePoints1 = new ArrayList<>();
        routePoints1.add(new RequestPoint(
                new Point(55.921745, 42.908597), RequestPointType.WAYPOINT, null));
        routePoints1.add(new RequestPoint(
                new Point(55.922622, 42.919450), RequestPointType.WAYPOINT, null));

        List<RequestPoint> routePoints2 = new ArrayList<>();
        routePoints2.add(new RequestPoint(
                new Point(55.922463, 42.917470), RequestPointType.WAYPOINT, null));
        routePoints2.add(new RequestPoint(
                new Point(55.924504, 42.924683), RequestPointType.WAYPOINT, null));

        List<RequestPoint> routePoints3 = new ArrayList<>();
        routePoints3.add(new RequestPoint(
                new Point(55.925658, 42.925863), RequestPointType.WAYPOINT, null));
        routePoints3.add(new RequestPoint(
                new Point(55.927002, 42.927379), RequestPointType.WAYPOINT, null));

        final long DELAY_LESS_THAN_LOCATION_UNAVAILABILITY_DELAY = 20000;
        final long DELAY_MORE_THAN_LOCATION_UNAVAILABILITY_DELAY = 80000;

        List<MapRoadEventsTestRoute> routes = new ArrayList<>();
        routes.add(new MapRoadEventsTestRoute(
                routePoints1, DELAY_LESS_THAN_LOCATION_UNAVAILABILITY_DELAY));
        routes.add(new MapRoadEventsTestRoute(
                routePoints2, DELAY_MORE_THAN_LOCATION_UNAVAILABILITY_DELAY));
        routes.add(new MapRoadEventsTestRoute(routePoints3, 0));

        MapRoadEventsTestCase testCaseData = new MapRoadEventsTestCase(routes, 16.f);

        testCaseController.runTestCase(testCaseData);
    }

    @Override
    public void onRunDrivingTestClicked() {
        List<RequestPoint> routePoints = new ArrayList<>();
        routePoints.add(new RequestPoint(
                new Point(55.921745, 42.908597), RequestPointType.WAYPOINT, null));
        routePoints.add(new RequestPoint(
                new Point(55.937926, 42.953977), RequestPointType.WAYPOINT, null));

        List<MapRoadEventsTestRoute> routes = new ArrayList<>();
        routes.add(new MapRoadEventsTestRoute(routePoints, 0));

        MapRoadEventsTestCase testCaseData = new MapRoadEventsTestCase(routes, 16.f);

        testCaseController.runTestCase(testCaseData);
    }

    @Override
    public void onStopTestClicked() {
        testCaseController.stopTestCase();
    }

    @Override
    public void onRoadEventPlacemarkTap(@NonNull RoadEvent roadEvent) {
        roadEventsLayer.selectRoadEvent(roadEvent.getId());
        selectedRoadEvent = roadEvent.getId();
        updateHighlightModeSelector();
        briefCard.show(roadEvent);
    }

    @Override
    public void onMapTap(Map map, Point position) {
        roadEventsLayer.deselectRoadEvent();
        selectedRoadEvent = null;
        updateHighlightModeSelector();
        briefCard.hide();
    }

    @Override
    public void onCameraPositionChanged(
            Map map,
            CameraPosition camPos,
            CameraUpdateReason reason,
            boolean finished) {
        if (reason == CameraUpdateReason.GESTURES) {
            testCaseController.setFollowPoint(false);
        }
    }

    @Override
    public void onMapLongTap(Map map, final Point position) {
        Intent intent = new Intent(MapRoadEvents2Activity.this, MapAddRoadEventActivity.class);
        intent.putExtra(pointExtra, Serialization.serializeToBytes(position));
        intent.putExtra("Tags", new RoadEventTagsList(Arrays.asList(
                EventTag.SPEED_CONTROL,
                EventTag.ACCIDENT,
                EventTag.RECONSTRUCTION,
                EventTag.LOCAL_CHAT,
                EventTag.OTHER)));
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent dataIntent) {
        super.onActivityResult(requestCode, resultCode, dataIntent);

        switch (requestCode) {
            case REQUEST_CODE_TAGS:
                if(resultCode == RESULT_OK) {
                    RoadEventsTagsState tagsState = dataIntent.getParcelableExtra("RoadEventsTagsState");
                    for (EventTag tag : EventTag.values()) {
                        roadEventsLayer.setRoadEventVisible(tag, tagsState.isTagEnabled(tag));
                    }
                }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.map_road_events_2);
        super.onCreate(savedInstanceState);

        MapKitFactory.getInstance().resetLocationManagerToDefault();

        styleProvider = new RoadEventsLayerStyleProvider(getApplicationContext());
        roadEventsLayer = MapKitFactory.getInstance().createRoadEventsLayer(
                mapview.getMapWindow(),
                styleProvider);
        roadEventsLayer.addListener(this);

        userLocationLayer =
                MapKitFactory.getInstance()
                        .createUserLocationLayer(mapview.getMapWindow());
        userLocationLayer.setVisible(true);

        testCaseController = new MapRoadEvents2TestCaseController(
                mapview,
                this);

        highlightModeSelector = findViewById(R.id.highlight_mode_selector);
        highlightModeSelector.setEnabled(false);
        highlightModeSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                if (highlightModeSelectorIndex == position)
                    return;
                highlightModeSelectorIndex = position;
                if (selectedRoadEvent == null)
                    return;
                highlightMode = indexToHighlightMode(position);
                if (highlightMode == null) {
                    roadEventsLayer.setHighlightedRoadEvent(null);
                    highlightedRoadEvent = null;
                } else {
                    roadEventsLayer.setHighlightedRoadEvent(new HighlightedRoadEvent(selectedRoadEvent, highlightMode));
                    highlightedRoadEvent = selectedRoadEvent;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) { }
        });

        briefCard = new BriefCard();
        mapview.getMap().addInputListener(this);
        mapview.getMap().addCameraListener(this);

        final Button moveToLocationButton = (Button)findViewById(R.id.findMeButton);
        moveToLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (testCaseController.isTestCaseRunning()) {
                    testCaseController.setFollowPoint(true);
                } else {
                    CameraPosition position = userLocationLayer.cameraPosition();
                    if (position != null)
                        mapview.getMap().move(position);
                }
            }
        });

        setAllRoadEventsVisible();
    }

    @Override
    protected void onDestroy() {
        testCaseController.stopTestCase();
        super.onDestroy();
    }

    private void showFullCard(String eventId) {
        Intent intent = new Intent(this, MapRoadEventFullCardActivity.class);
        intent.putExtra(eventIdExtra, eventId);
        startActivity(intent);
    }

    private void setAllRoadEventsVisible() {
        for (EventTag tag : EventTag.values()) {
            roadEventsLayer.setRoadEventVisible(tag, true);
        }
    }

    private RoadEventsTagsState getTagsState() {
        boolean[] tagEnabled = new boolean[EventTag.values().length];
        for (int i = 0; i < EventTag.values().length; i++) {
            tagEnabled[i] = roadEventsLayer.isRoadEventVisible(EventTag.values()[i]);
        }
        return new RoadEventsTagsState(tagEnabled);
    }

    private class BriefCard {
        public BriefCard () {
            briefCardLayout = findViewById(R.id.road_event_brief_card);
            id = findViewById(R.id.road_event_brief_card_id);
            tags = findViewById(R.id.road_event_brief_card_tags);
        }

        public void hide() {
            briefCardLayout.setVisibility(View.GONE);
        }

        public void show(final RoadEvent info) {
            id.setText("Id: " + info.getId());

            StringBuilder builder = new StringBuilder();
            for (EventTag tag : info.getTags()) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }
                builder.append(tag.name());
            }
            String tagsString = builder.toString();

            tags.setText("Tags: " + tagsString);

            briefCardLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showFullCard(info.getId());
                }
            });
            briefCardLayout.setVisibility(View.VISIBLE);
        }

        private LinearLayout briefCardLayout;
        private TextView id;
        private TextView tags;
    }

    private void updateHighlightModeSelector() {
        if (selectedRoadEvent != null && selectedRoadEvent.equals(highlightedRoadEvent)) {
            highlightModeSelectorIndex = highlightModeToIndex(highlightMode);
        } else {
            highlightModeSelectorIndex = highlightModeToIndex(null);
        }

        highlightModeSelector.setSelection(highlightModeSelectorIndex);
        highlightModeSelector.setEnabled(selectedRoadEvent != null);
    }

    public void resetHighlight(View view) {
        roadEventsLayer.setHighlightedRoadEvent(null);
        highlightedRoadEvent = null;
        highlightMode = null;
        updateHighlightModeSelector();
    }

    private static int highlightModeToIndex(@Nullable HighlightMode mode) {
        if (mode == null)
            return 0;
        switch (mode) {
            case SOFT_PULSATION:
                return 1;
            case HARD_PULSATION:
                return 2;
        }
        return 0;
    }

    private static @Nullable HighlightMode indexToHighlightMode(int index) {
        switch (index) {
            case 0:
                return null;
            case 1:
                return HighlightMode.SOFT_PULSATION;
            case 2:
                return HighlightMode.HARD_PULSATION;
        }
        return null;
    }

    private static final String roadEventsActionsBackStackName = "ROAD_EVENTS_ACTIONS_NAME";
    private static final String roadEventsActionsFragmentTag = "ROAD_EVENTS_ACTIONS_TAG";

    private static final int REQUEST_CODE_TAGS = 1;
    private static final String pointExtra = "point";
    private static final String eventIdExtra = "event";

    private String selectedRoadEvent;
    private String highlightedRoadEvent;
    private HighlightMode highlightMode;
    private Spinner highlightModeSelector;
    private int highlightModeSelectorIndex = 0;
    private boolean zoomAnimation = true;

    private BriefCard briefCard;
    private StyleProvider styleProvider;
    private RoadEventsLayer roadEventsLayer;
    private UserLocationLayer userLocationLayer;
    private MapRoadEvents2TestCaseController testCaseController;
}
