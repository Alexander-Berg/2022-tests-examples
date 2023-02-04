package com.yandex.maps.testapp.map;

import android.graphics.PointF;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.yandex.mapkit.directions.driving.DrivingOptions;
import com.yandex.mapkit.directions.driving.DrivingRoute;
import com.yandex.mapkit.directions.driving.DrivingSession;
import com.yandex.mapkit.directions.driving.JamType;
import com.yandex.mapkit.directions.driving.JamTypeColor;
import com.yandex.mapkit.directions.driving.VehicleOptions;
import com.yandex.mapkit.directions.guidance.LocationViewSourceFactory;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.layers.ObjectEvent;
import com.yandex.mapkit.map.CameraListener;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.CameraUpdateReason;
import com.yandex.mapkit.map.InputListener;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.ModelStyle;
import com.yandex.mapkit.map.PolylineMapObject;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.annotations.AnnotationLanguage;
import com.yandex.mapkit.user_location.UserLocationLayer;

import com.yandex.mapkit.user_location.UserLocationObjectListener;
import com.yandex.mapkit.user_location.UserLocationView;
import com.yandex.maps.testapp.R;

import com.yandex.maps.testapp.Utils;
import com.yandex.maps.testapp.driving.RouteView;
import com.yandex.maps.testapp.driving.Router;
import com.yandex.maps.testapp.guidance.MockSpeaker;
import com.yandex.runtime.Error;
import com.yandex.runtime.image.ImageProvider;
import com.yandex.runtime.model.ModelProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NaviActivity extends MapBaseActivity {
    private static final int JAMS_TYPE_UNKNOWN_COLOR = 0xffa0a0a0;
    private static final int JAMS_TYPE_BLOCKED_COLOR = 0xff932100;
    private static final int JAMS_TYPE_FREE_COLOR = 0xff82ea0e;
    private static final int JAMS_TYPE_LIGHT_COLOR = 0xffffff41;
    private static final int JAMS_TYPE_HARD_COLOR = 0xffff5413;
    private static final int JAMS_TYPE_VERY_HARD_COLOR = 0xff932100;

    private static final int MANEUVER_FILL_COLOR = 0xffffffff;
    private static final int MANEUVER_OUTLINE_COLOR = 0xff000000;
    private static final float MANEUVER_OUTLINE_WIDTH = 1.f;

    private static final float ARROW_MODEL_SIZE = 60.f;

    private static Logger LOGGER = Logger.getLogger("yandex.maps.navi");
    private UserLocationLayer userLocationLayer;
    private UserLocationObjectListener userLocationObjectListener;

    private MapObjectCollection waypoints;
    private RouteView routeView;
    private Router router;
    private SimulationController simulation;

    static void applyJamsStyle(RouteView routeView) {
        routeView.setJamsColors(new ArrayList<JamTypeColor>(){{
            add(new JamTypeColor(JamType.UNKNOWN, JAMS_TYPE_UNKNOWN_COLOR));
            add(new JamTypeColor(JamType.BLOCKED, JAMS_TYPE_BLOCKED_COLOR));
            add(new JamTypeColor(JamType.FREE, JAMS_TYPE_FREE_COLOR));
            add(new JamTypeColor(JamType.LIGHT, JAMS_TYPE_LIGHT_COLOR));
            add(new JamTypeColor(JamType.HARD, JAMS_TYPE_HARD_COLOR));
            add(new JamTypeColor(JamType.VERY_HARD, JAMS_TYPE_VERY_HARD_COLOR));
        }});
    }

    static void applyManeuverStyle(RouteView routeView) {
        routeView.updateManeuverStyle(
                MANEUVER_FILL_COLOR, MANEUVER_OUTLINE_COLOR, MANEUVER_OUTLINE_WIDTH);
    }

    static void applyPolylineStyle(PolylineMapObject polylineMapObject) {
        polylineMapObject.setOutlineColor(Color.BLACK);
        polylineMapObject.setOutlineWidth(1.f);
        polylineMapObject.setTurnRadius(4.f);
        polylineMapObject.setArcApproximationStep(26.0f);
        polylineMapObject.setInnerOutlineEnabled(false);
    }

    private InputListener inputListener = new InputListener() {
        @Override
        public void onMapTap(Map map, Point point) {}

        @Override
        public void onMapLongTap(Map map, Point point) {
            if (router.waypointsCount() == 0) {
                CameraPosition position = userLocationLayer.cameraPosition();
                if (position != null) {
                    addWaypoint(position.getTarget());
                }
            }

            addWaypoint(point);

            if (router.waypointsCount() > 1) {
                router.requestRoute(
                    new DrivingOptions(),
                    new VehicleOptions(),
                    new DrivingSession.DrivingRouteListener() {
                        @Override
                        public void onDrivingRoutes(List<DrivingRoute> routes) {
                            if (!routes.isEmpty()) {
                                routeView.setRoute(routes.get(0));
                                applyPolylineStyle(routeView.getRoutePolyline());
                            }
                        }

                        @Override
                        public void onDrivingRoutesError(Error error) {}
                    }
                );
            }
        }

        private void addWaypoint(Point point) {
            router.addWaypoint(point);
            waypoints.addPlacemark(point);
        }
    };

    private final CameraListener cameraListener = new CameraListener() {
        @Override
        public void onCameraPositionChanged(Map map, CameraPosition position,
                CameraUpdateReason reason, boolean finished) {
            if (reason == CameraUpdateReason.GESTURES) {
                userLocationLayer.resetAnchor();
                userLocationLayer.setHeadingEnabled(false);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.navi);
        super.onCreate(savedInstanceState);

        router = new Router();

        routeView = new RouteView(mapview.getMap(), null, new RouteView.ImageProviderFactory() {
            @Override
            public ImageProvider fromResource(int resourceId) {
                return ImageProvider.fromResource(NaviActivity.this, resourceId);
            }
        });
        applyJamsStyle(routeView);
        applyManeuverStyle(routeView);
        waypoints = mapview.getMap().getMapObjects().addCollection();

        MockSpeaker speaker = new MockSpeaker(
            this,
            AnnotationLanguage.RUSSIAN,
            new MockSpeaker.SayCallback() {
                @Override
                public void onSay(final String string) {
                    Utils.showMessage(NaviActivity.this, string, Level.INFO, LOGGER);
                }
            }
        );

        simulation = new SimulationController(null);
        simulation.getGuide().setLocalizedSpeaker(speaker, AnnotationLanguage.RUSSIAN);

        mapview.setFieldOfViewY(60.0);

        List<PointF> tiltFunction = new ArrayList<>();
        tiltFunction.add(new PointF(13.0f, 0.0f));
        tiltFunction.add(new PointF(15.0f, 35.0f));
        tiltFunction.add(new PointF(16.0f, 47.5f));
        mapview.getMap().setTiltFunction(tiltFunction);

        userLocationLayer = MapKitFactory.getInstance().createUserLocationLayer(mapview.getMapWindow());

        final ImageProvider arrowModelTex = ImageProvider.fromResource(this, R.drawable.camaro);
        final ModelProvider modelProvider = ModelProvider.fromResource(this, R.raw.camaro, arrowModelTex);
        userLocationObjectListener = new UserLocationObjectListener() {
            @Override
            public void onObjectAdded(UserLocationView userLocationView) {
                userLocationView.getArrow().setModel(modelProvider, new ModelStyle(ARROW_MODEL_SIZE, ModelStyle.UnitType.NORMALIZED, ModelStyle.RenderMode.USER_MODEL));
            }

            @Override
            public void onObjectRemoved(UserLocationView userLocationView) {}

            @Override
            public void onObjectUpdated(UserLocationView userLocationView, ObjectEvent userLocationEvent) {}
        };
        userLocationLayer.setObjectListener(userLocationObjectListener);
        userLocationLayer.setVisible(true);

        mapview.getMap().addCameraListener(cameraListener);
        mapview.getMap().addInputListener(inputListener);

        mapview.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                // Update heading anchors when layout is changes
                if (userLocationLayer.isHeadingEnabled()) {
                    userLocationLayer.setAnchor(getAnchorCenter(), getAnchorCourse());
                }
            }
        });

        final ToggleButton simulateToggle = findViewById(R.id.simulate_mode);
        simulateToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (routeView.getRoute() != null) {
                        simulation.start(routeView.getRoute(), routeView.getRoutePolyline());
                        userLocationLayer.setSource(
                            LocationViewSourceFactory.createLocationViewSource(simulation.getGuide()));
                    } else {
                        simulateToggle.setChecked(false);
                    }
                } else {
                    simulation.stop();
                }
            }
        });

        ToggleButton muteToggle = findViewById(R.id.mute_mode);
        muteToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    simulation.getGuide().mute();
                } else {
                    simulation.getGuide().unmute();
                }
            }
        });

        final EditText poiLimitEdit = (EditText) findViewById(R.id.poiLimit);
        poiLimitEdit.setEnabled(false);
        final SeekBar survivalRateSeekBar = (SeekBar) findViewById(R.id.survivalRateBar);
        final TextView survivalCountTextView = (TextView) findViewById(R.id.survived_count);
        final String labelPrefix = getResources().getString(R.string.poi_limit);
        survivalCountTextView.setText(String.format("%s: NO", labelPrefix));
        survivalRateSeekBar.setProgress(survivalRateSeekBar.getMax());
        setPoiLimit(null);
        survivalRateSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress == seekBar.getMax()) {
                    survivalCountTextView.setText(String.format("%s: NO", labelPrefix));
                    setPoiLimit(null);
                } else {
                    survivalCountTextView.setText(String.format(Locale.US, "%s: %d", labelPrefix, progress));
                    setPoiLimit(progress);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        onFindMeClick(null);
    }

    @Override
    protected void onDestroy() {
        simulation.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        simulation.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        simulation.onResume();
    }

    public void onFindMeClick(View view) {
        CameraPosition position = userLocationLayer.cameraPosition();
        if (position != null)
            mapview.getMap().move(position);

        userLocationLayer.setAnchor(getAnchorCenter(), getAnchorCourse());
        userLocationLayer.setHeadingEnabled(true);
    }

    private PointF getAnchorCenter() {
        return new PointF((float)(mapview.getWidth() * 0.5), (float)(mapview.getHeight() * 0.5));
    }

    private PointF getAnchorCourse() {
        return new PointF((float)(mapview.getWidth() * 0.5), (float)(mapview.getHeight() * 0.83));
    }

    public void onClearRouteClick(View view) {
        simulation.stop();
        routeView.setRoute(null);
        router.clear();
        waypoints.clear();
    }

}
