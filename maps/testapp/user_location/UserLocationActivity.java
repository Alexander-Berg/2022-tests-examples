package com.yandex.maps.testapp.user_location;

import android.graphics.PointF;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.yandex.mapkit.Animation;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.directions.DirectionsFactory;
import com.yandex.mapkit.ScreenPoint;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.directions.guidance.Guide;
import com.yandex.mapkit.indoor.IndoorLevel;
import com.yandex.mapkit.indoor.IndoorPlan;
import com.yandex.mapkit.indoor.IndoorStateListener;
import com.yandex.mapkit.layers.ObjectEvent;
import com.yandex.mapkit.map.CameraListener;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.CameraUpdateReason;
import com.yandex.mapkit.map.IconStyle;
import com.yandex.mapkit.map.CompositeIcon;
import com.yandex.mapkit.map.InputListener;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.map.RotationType;
import com.yandex.mapkit.user_location.UserLocationAnchorType;
import com.yandex.mapkit.user_location.UserLocationIconType;
import com.yandex.mapkit.user_location.UserLocationLayer;
import com.yandex.mapkit.user_location.UserLocationAnchorChanged;
import com.yandex.mapkit.user_location.UserLocationIconChanged;
import com.yandex.mapkit.user_location.UserLocationTapListener;
import com.yandex.mapkit.user_location.UserLocationObjectListener;
import com.yandex.mapkit.user_location.UserLocationView;
import com.yandex.runtime.image.ImageProvider;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.Utils;
import com.yandex.maps.testapp.map.MapBaseActivity;

import java.util.List;

public class UserLocationActivity extends MapBaseActivity implements IndoorStateListener
{
    private UserLocationLayer userLocationLayer = null;
    private CameraListener cameraListener = null;
    private UserLocationTapListener tapListener = null;
    private InputListener inputListener = null;
    private UserLocationObjectListener objectListener = null;

    private RadioGroup indoorRadioGroup_;
    private IndoorPlan activeIndoorPlan_;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.user_location);
        super.onCreate(savedInstanceState);

        MapKitFactory.getInstance().resetLocationManagerToDefault();
        userLocationLayer = MapKitFactory.getInstance().createUserLocationLayer(mapview.getMapWindow());

        final ToggleButton locationToggle = (ToggleButton)findViewById(R.id.location);
        final ToggleButton headingToggle = (ToggleButton)findViewById(R.id.heading);
        final ToggleButton autoZoomToggle = (ToggleButton)findViewById(R.id.auto_zoom);
        final Button moveToLocationButton = (Button)findViewById(R.id.move_to_location);
        final Button setGuideButton = (Button)findViewById(R.id.set_guide);
        final Button setLocMgrButton = (Button)findViewById(R.id.set_lm);

        final ImageProvider pinRed = ImageProvider.fromResource(this, R.drawable.custom_ya_point_red);
        final ImageProvider pinBlue = ImageProvider.fromResource(this, R.drawable.custom_ya_point_blue);
        final ImageProvider pinBlack = ImageProvider.fromResource(this, R.drawable.custom_ya_point_shadow);
        final ImageProvider arrowRed = ImageProvider.fromResource(this, R.drawable.custom_navigation_icon_red);
        final ImageProvider arrowBlue = ImageProvider.fromResource(this, R.drawable.custom_navigation_icon_blue);
        final ImageProvider arrowBlack = ImageProvider.fromResource(this, R.drawable.custom_navigation_icon_shadow);
        final IconStyle pinIconStyle = new IconStyle();
        final IconStyle arrowIconStyle = new IconStyle();
        arrowIconStyle.setFlat(true).setRotationType(RotationType.ROTATE);
        final IconStyle pinShadowIconStyle = new IconStyle();
        final PointF shadowAnchor = new PointF(0.4f, 0.4f);
        pinShadowIconStyle.setAnchor(shadowAnchor);
        final IconStyle arrowShadowIconStyle = new IconStyle();
        arrowShadowIconStyle.setFlat(true).setRotationType(RotationType.ROTATE).setAnchor(shadowAnchor);

        locationToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                userLocationLayer.setVisible(isChecked);
            }
        });

        headingToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    userLocationLayer.setAnchor(getAnchorCenter(), getAnchorCourse());
                    userLocationLayer.setHeadingEnabled(true);
                    Utils.showMessage(UserLocationActivity.this, "Heading ON");
                } else {
                    userLocationLayer.resetAnchor();
                    userLocationLayer.setHeadingEnabled(false);
                    Utils.showMessage(UserLocationActivity.this, "Heading OFF");
                }
            }
        });

        autoZoomToggle.setChecked(userLocationLayer.isAutoZoomEnabled());
        autoZoomToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                userLocationLayer.setAutoZoomEnabled(isChecked);
            }
        });

        moveToLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CameraPosition position = userLocationLayer.cameraPosition();
                if (position != null)
                    mapview.getMap().move(position);
                else
                    Utils.showMessage(UserLocationActivity.this, "No current location");
            }
        });

        setGuideButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Guide guide = DirectionsFactory.getInstance().createGuide();
                guide.resume();
                userLocationLayer.setSource(
                    com.yandex.mapkit.directions.guidance.LocationViewSourceFactory.createLocationViewSource(guide));
            }
        });

        setLocMgrButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userLocationLayer.setSource(
                    com.yandex.mapkit.location.LocationViewSourceFactory.createLocationViewSource(
                        MapKitFactory.getInstance().createLocationManager()));
            }
        });

        tapListener = new UserLocationTapListener() {
            @Override
            public void onUserLocationObjectTap(Point point) {
                Utils.showMessage(UserLocationActivity.this, "Location coordinates " + point.getLatitude() + ";" + point.getLongitude());
            }
        };
        userLocationLayer.setTapListener(tapListener);

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

        cameraListener = new CameraListener() {
            @Override
            public void onCameraPositionChanged(Map map, CameraPosition cameraPosition,
                                                CameraUpdateReason cameraUpdateReason,
                                                boolean finished) {
                // If user changes the camera by gestures, then turn off heading or anchoring.
                if (cameraUpdateReason == CameraUpdateReason.GESTURES &&
                        (userLocationLayer.isHeadingEnabled() || userLocationLayer.isAnchorEnabled()))
                {
                    if (userLocationLayer.isHeadingEnabled()) {
                        Utils.showMessage(UserLocationActivity.this, "Heading OFF");
                    }
                    else if (userLocationLayer.isAnchorEnabled()) {
                        Utils.showMessage(UserLocationActivity.this, "Anchoring OFF");
                    }
                    headingToggle.setChecked(false);
                    userLocationLayer.resetAnchor();
                    userLocationLayer.setHeadingEnabled(false);
                }
            }
        };
        mapview.getMap().addCameraListener(cameraListener);

        inputListener = new InputListener() {
            @Override
            public void onMapTap(Map map, Point point) {
            }
            @Override
            public void onMapLongTap(Map map, Point point) {
                ScreenPoint screenPt = mapview.worldToScreen(point);
                PointF anchor = new PointF(screenPt.getX(), screenPt.getY());
                headingToggle.setChecked(false);
                userLocationLayer.setAnchor(anchor, anchor);
                userLocationLayer.setHeadingEnabled(false);
                Utils.showMessage(UserLocationActivity.this, "Anchoring ON");
            }
        };
        mapview.getMap().addInputListener(inputListener);

        objectListener = new UserLocationObjectListener() {
            @Override
            public void onObjectAdded(UserLocationView userLocationView) {
                // initialization
                userLocationView.getAccuracyCircle().setFillColor(0x9966ff99);
                CompositeIcon compositeArrow = userLocationView.getArrow().useCompositeIcon();
                compositeArrow.setIcon("arrow", arrowRed, arrowIconStyle);
                compositeArrow.setIcon("shadow", arrowBlack, arrowShadowIconStyle);
                CompositeIcon compositePin = userLocationView.getPin().useCompositeIcon();
                compositePin.setIcon("pin", pinRed, pinIconStyle);
                compositePin.setIcon("shadow", pinBlack, pinShadowIconStyle);
            }
            @Override
            public void onObjectRemoved(UserLocationView userLocationView) {
            }
            @Override
            public void onObjectUpdated(UserLocationView userLocationView, ObjectEvent userLocationEvent) {
                if (userLocationEvent instanceof UserLocationIconChanged) {
                    UserLocationIconChanged iconChangedEvent = (UserLocationIconChanged)userLocationEvent;
                    if (iconChangedEvent.getIconType() == UserLocationIconType.ARROW) {
                        // arrow icon
                        userLocationView.getAccuracyCircle().setFillColor(Color.RED & 0x99ffffff);
                    } else if (iconChangedEvent.getIconType() == UserLocationIconType.PIN) {
                        // pin icon
                        userLocationView.getAccuracyCircle().setFillColor(Color.GREEN & 0x99ffffff);
                    }
                } else if (userLocationEvent instanceof UserLocationAnchorChanged) {
                    UserLocationAnchorChanged anchorChangedEvent = (UserLocationAnchorChanged)userLocationEvent;
                    if (anchorChangedEvent.getAnchorType() == UserLocationAnchorType.NORMAL) {
                        // normal anchor
                        userLocationView.getAccuracyCircle().setFillColor(Color.BLUE & 0x99ffffff);
                        userLocationView.getArrow().useCompositeIcon()
                            .setIcon("arrow", arrowRed, arrowIconStyle);
                        userLocationView.getPin().useCompositeIcon()
                            .setIcon("pin", pinRed, pinIconStyle);
                    } else if (anchorChangedEvent.getAnchorType() == UserLocationAnchorType.COURSE) {
                        // course anchor
                        userLocationView.getAccuracyCircle().setFillColor(Color.MAGENTA & 0x99ffffff);
                        userLocationView.getArrow().useCompositeIcon()
                            .setIcon("arrow", arrowBlue, arrowIconStyle);
                        userLocationView.getPin().useCompositeIcon()
                            .setIcon("pin", pinBlue, pinIconStyle);
                    }
                }
            }
        };
        userLocationLayer.setObjectListener(objectListener);

        indoorRadioGroup_ = findViewById(R.id.indoor_levels_radio_group);
        indoorRadioGroup_.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId > 0)
                    loadLevel(checkedId - 1);
            }
        });

        mapview.getMap().setIndoorEnabled(true);
        mapview.getMap().addIndoorStateListener(this);
    }

    private PointF getAnchorCenter()
    {
        return new PointF((float)(mapview.getWidth() * 0.5), (float)(mapview.getHeight() * 0.5));
    }

    private PointF getAnchorCourse()
    {
        return new PointF((float)(mapview.getWidth() * 0.5), (float)(mapview.getHeight() * 0.83));
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
}
