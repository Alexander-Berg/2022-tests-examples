package com.yandex.maps.testapp.location;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.core.app.ActivityCompat;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.yandex.mapkit.Animation;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Circle;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.indoor.IndoorLevel;
import com.yandex.mapkit.indoor.IndoorPlan;
import com.yandex.mapkit.indoor.IndoorStateListener;
import com.yandex.mapkit.location.DummyLocationManager;
import com.yandex.mapkit.location.Location;
import com.yandex.mapkit.location.LocationListener;
import com.yandex.mapkit.location.LocationManager;
import com.yandex.mapkit.location.LocationManagerUtils;
import com.yandex.mapkit.location.LocationStatus;
import com.yandex.mapkit.location.FilteringMode;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.CircleMapObject;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.Utils;
import com.yandex.maps.testapp.map.MapBaseActivity;
import com.yandex.runtime.image.ImageProvider;

import java.util.List;

public class LocationActivity extends MapBaseActivity implements IndoorStateListener
{
    private LocationManager locationManager;
    private LocationManager ylbsManager;
    private PlacemarkMapObject lastKnownPositionPlacemark;
    private CircleMapObject lastKnownPositionCircle;
    private MapObjectCollection mapObjects;

    private boolean followUserDirection = false;

    private static final float USER_LOCATION_MOVE_TIME = 0.5f;
    private static final float DEFAULT_LOCATION_ACCURACY = 40;

    private RadioGroup indoorRadioGroup_;
    private TextView indoorLevelIdView_;
    private IndoorPlan activeIndoorPlan_;

    private PlacemarkMapObject addPlacemark(MapObjectCollection mapObjects, int color) {
        PlacemarkMapObject placemark = mapObjects.addPlacemark(new Point(0, 0));
        placemark.setVisible(false);
        placemark.setIcon(ImageProvider.fromBitmap(
                Utils.createPlacemarkBitmap(color & 0xA0FFFFFF, 20, this)));
        return placemark;
    }

    private CircleMapObject addCircle(MapObjectCollection mapObjects) {
        CircleMapObject circle = mapObjects.addCircle(
                new Circle(new Point(0, 0), 0), Color.TRANSPARENT, 5.f, Color.TRANSPARENT);
        circle.setVisible(false);
        return circle;
    }

    private void updatePlacemark(
        PlacemarkMapObject placemark,
        CircleMapObject circle,
        int circleColor,
        int circleFillColor,
        Point position,
        float accuracy
    ) {
        hideLastKnownLocation();

        placemark.setVisible(true);
        placemark.setGeometry(position);

        circle.setVisible(true);
        int fillColor = 0;

        circle.setGeometry(new Circle(position, accuracy));

        circle.setStrokeColor(circleColor);
        circle.setFillColor(circleFillColor);
    }

    private void updatePlacemark(
        PlacemarkMapObject placemark,
        CircleMapObject circle,
        int circleColor,
        int circleInsideColor,
        Location location
    ) {
        int insideColor = location.getAccuracy() != null ? circleColor : circleInsideColor;
        float accuracy = location.getAccuracy() != null ?
                location.getAccuracy().floatValue() :
                DEFAULT_LOCATION_ACCURACY;
        updatePlacemark(
            placemark,
            circle,
            circleColor,
            insideColor,
            location.getPosition(),
            accuracy
        );
    }

    private void updatePlacemark(
            PlacemarkMapObject placemark,
            CircleMapObject circle,
            int circleColor,
            int circleInsideColor,
            android.location.Location location
    ) {
        int insideColor = location.getAccuracy() == 0 ? circleColor : circleInsideColor;
        float accuracy = location.getAccuracy() == 0 ?
                location.getAccuracy() :
                DEFAULT_LOCATION_ACCURACY;
        updatePlacemark(
            placemark,
            circle,
            circleColor,
            insideColor,
            new Point(
                location.getLatitude(),
                location.getLongitude()),
            accuracy
        );
    }


    private LocationListener createLocationListener(
        PlacemarkMapObject placemark,
        CircleMapObject circle,
        int circleColor,
        int circleInsideColor,
        boolean moveMap
    ) {
        return new LocationListener() {
            @Override
            public void onLocationUpdated(Location location) {
                updatePlacemark(placemark, circle, circleColor, circleInsideColor, location);

                if (location.getIndoorLevelId() != null) {
                    indoorLevelIdView_.setText(
                        "LEVEL: " + location.getIndoorLevelId() + ", " +
                        "ACC: " + Math.round(location.getAccuracy()));
                } else {
                    indoorLevelIdView_.setText("LEVEL: ---");
                }

                if (moveMap) {
                    float direction = mapview.getMap().getCameraPosition().getAzimuth();
                    if (followUserDirection && location.getHeading() != null) {
                        direction = (float) location.getHeading().doubleValue();
                    }
                    centerMap(location.getPosition(), direction);
                }
            }

            @Override
            public void onLocationStatusUpdated(LocationStatus status) {
                if (status == LocationStatus.RESET) {
                    return;
                }

                placemark.setVisible(status == LocationStatus.AVAILABLE);
                circle.setVisible(status == LocationStatus.AVAILABLE);
            }
        };
    }

    private android.location.LocationProvider getProvider(final String providerName) {
        String permission;
        if (providerName.equals("gps")) {
            permission = Manifest.permission.ACCESS_FINE_LOCATION;
        } else if (providerName.equals("network")) {
            permission = Manifest.permission.ACCESS_COARSE_LOCATION;
        } else {
            return null;
        }

        final android.location.LocationManager androidLocationManager =
                (android.location.LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(LocationActivity.this, permission)
                == PackageManager.PERMISSION_GRANTED) {
            return androidLocationManager.getProvider(providerName);
        }

        return null;
    }

    private CompoundButton.OnCheckedChangeListener createOnCheckedListener(
            final PlacemarkMapObject placemark,
            final CircleMapObject circle,
            final int circleColor,
            final int circleInsideColor,
            final String providerName
    ) {
        final android.location.LocationProvider provider = getProvider(providerName);
        if (provider == null) {
            return new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        Utils.showMessage(LocationActivity.this, "Provider " + providerName
                                + " doesn't exist in the system or app does not have permission to use it.");
                        buttonView.toggle();
                    }
                }
            };
        }

        final android.location.LocationListener locationListener = new android.location.LocationListener() {
            @Override
            public void onLocationChanged(android.location.Location location) {
                updatePlacemark(placemark, circle, circleColor, circleInsideColor, location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        final android.location.LocationManager androidLocationManager =
                (android.location.LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    androidLocationManager.requestLocationUpdates(
                            providerName,
                            0, 0, locationListener);
                    circle.setVisible(true);
                    placemark.setVisible(true);
                } else {
                    androidLocationManager.removeUpdates(locationListener);
                    circle.setVisible(false);
                    placemark.setVisible(false);
                }
            }
        };
    }

    private LocationManager getLocationManager() {
        if (locationManager == null) {
            locationManager = MapKitFactory.getInstance().createLocationManager();

            locationManager.requestSingleUpdate(new LocationListener() {
                @Override
                public void onLocationUpdated(@NonNull Location location) {
                }

                @Override
                public void onLocationStatusUpdated(@NonNull LocationStatus locationStatus) {
                }
            });
        }

        return locationManager;
    }

    private LocationManager getYlbsManager() {
        if (ylbsManager == null) {
            ylbsManager = MapKitFactory.getInstance().createLbsLocationManager();
        }
        return ylbsManager;
    }

    private void showLastKnownLocation() {
        lastKnownPositionPlacemark = addPlacemark(mapObjects, Color.GRAY);
        lastKnownPositionCircle = addCircle(mapObjects);

        Location location = LocationManagerUtils.getLastKnownLocation();
        if (location != null) {
            updatePlacemark(
                lastKnownPositionPlacemark,
                lastKnownPositionCircle,
                0xA0AAAAAA,
                0xA0CCCCCC,
                location
            );

            float direction = mapview.getMap().getCameraPosition().getAzimuth();
            if (followUserDirection && location.getHeading() != null) {
                direction = (float) location.getHeading().doubleValue();
            }
            centerMap(location.getPosition(), direction);
        }
    }

    private void hideLastKnownLocation() {
        lastKnownPositionPlacemark.setVisible(false);
        lastKnownPositionCircle.setVisible(false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.location);
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Map map = mapview.getMap();
        mapObjects = map.getMapObjects();

        showLastKnownLocation();

        ToggleButton directionsToggle =
                (ToggleButton) findViewById(R.id.location_direction_toggle);
        directionsToggle.setOnCheckedChangeListener(new ToggleButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton button, boolean isChecked) {
                followUserDirection = isChecked;
                if (!isChecked) {
                    centerMap(mapview.getMap().getCameraPosition().getTarget(), 0);
                }
            }
        });

        final PlacemarkMapObject placemark = addPlacemark(mapObjects, Color.BLUE);
        final CircleMapObject circle = addCircle(mapObjects);
        final LocationListener listener = createLocationListener(placemark,
                circle,
                0xA00000FF,
                0xA09999FF,
                true);

        ToggleButton locationToggle = (ToggleButton) findViewById(R.id.location_switch);
        locationToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked) {
                    getLocationManager().unsubscribe(listener);
                    placemark.setVisible(false);
                    circle.setVisible(false);
                } else {
                    getLocationManager().subscribeForLocationUpdates(1, 0, 0, false, FilteringMode.ON, listener);
                }
            }
        });

        ToggleButton ylbsToggle = (ToggleButton) findViewById(R.id.ylbs_switch);
        final PlacemarkMapObject yPlacemark = addPlacemark(mapObjects, Color.BLACK);
        final CircleMapObject yCircle = addCircle(mapObjects);
        final LocationListener yListener = createLocationListener(
                yPlacemark,
                yCircle,
                0xA0000000,
                0xA0444444,
                false);
        ylbsToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    yCircle.setVisible(true);
                    yPlacemark.setVisible(true);
                    getYlbsManager().subscribeForLocationUpdates(1, 0, 0, false, FilteringMode.ON, yListener);
                } else {
                    getYlbsManager().unsubscribe(yListener);
                    yCircle.setVisible(false);
                    yPlacemark.setVisible(false);
                }
            }
        });

        ToggleButton gpsToggle = (ToggleButton) findViewById(R.id.gps_switch);
        gpsToggle.setOnCheckedChangeListener(
                createOnCheckedListener(
                        addPlacemark(mapObjects, Color.GREEN),
                        addCircle(mapObjects),
                        0xA000FF00,
                        0xA099FF99,
                        android.location.LocationManager.GPS_PROVIDER
                        ));

        ToggleButton lbsToggle = (ToggleButton) findViewById(R.id.lbs_switch);
        lbsToggle.setOnCheckedChangeListener(
                createOnCheckedListener(
                        addPlacemark(mapObjects, Color.RED),
                        addCircle(mapObjects),
                        0xA0ff0000,
                        0xA0ff9999,
                        android.location.LocationManager.NETWORK_PROVIDER
                ));

        indoorRadioGroup_ = findViewById(R.id.indoor_levels_radio_group);
        indoorRadioGroup_.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId > 0)
                    loadLevel(checkedId - 1);
            }
        });

        indoorLevelIdView_ = findViewById(R.id.indoor_level_id);
        indoorLevelIdView_.setText("LEVEL: ---");

        mapview.getMap().setIndoorEnabled(true);
        mapview.getMap().addIndoorStateListener(this);
    }

    // IndoorStateListener
    @Override
    public void onActivePlanFocused(IndoorPlan indoorPlan)
    {
        indoorRadioGroup_.setVisibility(View.GONE);
        indoorRadioGroup_.removeAllViews();
        indoorRadioGroup_.clearCheck();

        activeIndoorPlan_ = indoorPlan;
        indoorRadioGroup_.setVisibility(View.VISIBLE);

        final List<IndoorLevel> levels = indoorPlan.getLevels();
        final String activeLevelId = indoorPlan.getActiveLevelId();

        int checkedIndex = 0;

        for(int i = 0; i < levels.size(); ++i)
        {
            final IndoorLevel level = levels.get(i);
            RadioButton button = new RadioButton(this);
            button.setBackgroundResource(R.drawable.radio_button_background);
            button.setButtonDrawable(android.R.color.transparent);
            button.setGravity(Gravity.CENTER);
            button.setTextColor(0xFF000000);
            button.setText(level.getName());
            button.setId(i + 1);
            indoorRadioGroup_.addView(button, i,
                new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    Gravity.TOP));
            if (activeLevelId.equals(level.getId()))
                checkedIndex = i;
        }
        ((RadioButton)indoorRadioGroup_.getChildAt(checkedIndex)).setChecked(true);
    }

    // IndoorStateListener
    @Override
    public void onActivePlanLeft()
    {
        indoorRadioGroup_.setVisibility(View.GONE);
        indoorRadioGroup_.removeAllViews();
        indoorRadioGroup_.clearCheck();
        activeIndoorPlan_ = null;
    }

    // IndoorStateListener
    @Override
    public void onActiveLevelChanged(String activeLevelId)
    {
        if (activeIndoorPlan_ != null)
        {
            final List<IndoorLevel> levels = activeIndoorPlan_.getLevels();
            for(int i = 0; i < levels.size(); ++i)
            {
                final IndoorLevel level = levels.get(i);
                if (activeLevelId.equals(level.getId()))
                {
                    ((RadioButton)indoorRadioGroup_.getChildAt(i)).setChecked(true);
                    break;
                }
            }
        }
    }

    private void loadLevel(int index)
    {
        if (activeIndoorPlan_ != null)
        {
            final List<IndoorLevel> levels = activeIndoorPlan_.getLevels();
            final String id = levels.get(index).getId();
            activeIndoorPlan_.setActiveLevelId(id);
        }
    }

    @Override
    protected void onPause() {
        getLocationManager().suspend();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getLocationManager().resume();
    }

    private void centerMap(Point position, float direction) {
        Map map = mapview.getMap();
        CameraPosition cameraPosition = map.getCameraPosition();
        map.move(
                new CameraPosition(
                        position,
                        cameraPosition.getZoom(),
                        direction,
                        cameraPosition.getTilt()),
                new Animation(Animation.Type.SMOOTH, USER_LOCATION_MOVE_TIME),
                null);
    }
}
