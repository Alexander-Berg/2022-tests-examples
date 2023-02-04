package com.yandex.maps.testapp.map;

import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.yandex.mapkit.Animation;
import com.yandex.mapkit.geometry.BoundingBox;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.indoor.IndoorLevel;
import com.yandex.mapkit.indoor.IndoorPlan;
import com.yandex.mapkit.indoor.IndoorStateListener;
import com.yandex.mapkit.layers.GeoObjectTapEvent;
import com.yandex.mapkit.layers.GeoObjectTapListener;
import com.yandex.mapkit.logo.Alignment;
import com.yandex.mapkit.logo.HorizontalAlignment;
import com.yandex.mapkit.logo.VerticalAlignment;
import com.yandex.mapkit.map.CameraListener;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.CameraUpdateReason;
import com.yandex.mapkit.map.GeoObjectSelectionMetadata;
import com.yandex.mapkit.map.InputListener;
import com.yandex.mapkit.map.Map;
import com.yandex.maps.testapp.R;

import java.lang.ref.WeakReference;
import java.util.List;

public class MapIndoorActivity extends MapBaseActivity implements
    CameraListener,
    GeoObjectTapListener,
    IndoorStateListener,
    InputListener
{
    private Context context_;
    private RadioGroup radioGroup_;
    private IndoorPlan activeIndoorPlan_;
    private String pendingIndoorLevelId_;

    @Override protected void onCreate(Bundle savedInstanceState)
    {
        setContentView(R.layout.map_indoor);
        super.onCreate(savedInstanceState);
        context_ = this;

        mapview.getMap().getLogo().setAlignment(
                new Alignment(HorizontalAlignment.CENTER, VerticalAlignment.BOTTOM));

        mapview.getMap().addIndoorStateListener(this);

        ToggleButton indoorToggle = findViewById(R.id.indoor_switch);
        indoorToggle.setChecked(false);
        indoorToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mapview.getMap().setIndoorEnabled(isChecked);
            }
        });

        radioGroup_ = findViewById(R.id.indoor_levels_radio_group);
        radioGroup_.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
          @Override public void onCheckedChanged(RadioGroup group, int checkedId) {
            if (checkedId > 0)
                loadLevel(checkedId - 1);
          }
        });

        mapview.getMap().addTapListener(this);
        mapview.getMap().addInputListener(this);

        mapview.getMap().move(new CameraPosition(new Point(55.755, 37.54), 15.5f, 0.0f, 0.0f));
    }

    @Override protected void onDestroy()
    {
        mapview.getMap().removeIndoorStateListener(this);
        super.onDestroy();
    }

    // IndoorStateListener
    @Override public void onActivePlanFocused(IndoorPlan indoorPlan)
    {
        radioGroup_.setVisibility(View.GONE);
        radioGroup_.removeAllViews();
        radioGroup_.clearCheck();

        activeIndoorPlan_ = indoorPlan;
        radioGroup_.setVisibility(View.VISIBLE);

        final List<IndoorLevel> levels = indoorPlan.getLevels();
        final String activeLevelId = pendingIndoorLevelId_ == null ?
            indoorPlan.getActiveLevelId() :
            pendingIndoorLevelId_;

        int checkedIndex = 0;

        for(int i = 0; i < levels.size(); ++i)
        {
            final IndoorLevel level = levels.get(i);
            RadioButton button = new RadioButton(context_);
            button.setBackgroundResource(R.drawable.radio_button_background);
            button.setButtonDrawable(android.R.color.transparent);
            button.setGravity(Gravity.CENTER);
            button.setTextColor(0xFF000000);
            button.setText(level.getName());
            button.setId(i + 1);
            radioGroup_.addView(button, i, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    Gravity.TOP));

            if (activeLevelId.equals(level.getId()))
                checkedIndex = i;
        }
        ((RadioButton)radioGroup_.getChildAt(checkedIndex)).setChecked(true);
    }

    // IndoorStateListener
    @Override public void onActivePlanLeft()
    {
        radioGroup_.setVisibility(View.GONE);
        radioGroup_.removeAllViews();
        radioGroup_.clearCheck();
        activeIndoorPlan_ = null;
    }

    // IndoorStateListener
    @Override public void onActiveLevelChanged(String activeLevelId)
    {
        if (activeIndoorPlan_ != null)
            selectActiveLevel(activeLevelId);
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

    // GeoObjectTapListener
    @Override
    public boolean onObjectTap(GeoObjectTapEvent event)
    {
        final GeoObjectSelectionMetadata selectionMetadata = event.getGeoObject()
                .getMetadataContainer()
                .getItem(GeoObjectSelectionMetadata.class);

        if (selectionMetadata == null)
            return false;

        if (event.getGeoObject().getName() != null) {
            Toast toast = Toast.makeText(getApplicationContext(),
                "name: " + event.getGeoObject().getName(),
                Toast.LENGTH_SHORT);
            toast.show();
        }

        mapview.getMap().selectGeoObject(selectionMetadata.getId(), selectionMetadata.getLayerId());
        return true;
    }

    // InputListener
    @Override
    public void onMapTap(Map map, Point point)
    {
        mapview.getMap().deselectGeoObject();
    }

    // InputListener
    @Override public void onMapLongTap(Map map, Point point)
    {}

    // CameraListener
    @Override public void onCameraPositionChanged(Map map, CameraPosition camPos, CameraUpdateReason reason, boolean finished) {
        pendingIndoorLevelId_ = null;
    }

    private void selectActiveLevel(String id)
    {
        int index = -1;
        final List<IndoorLevel> levels = activeIndoorPlan_.getLevels();
        for(int i = 0; i < levels.size(); ++i) {
            final IndoorLevel level = levels.get(i);
            if (id.equals(level.getId())) {
                index = i;
                break;
            }
        }
        if (index >= 0)
            ((RadioButton)radioGroup_.getChildAt(index)).setChecked(true);
    }

    public void onShowGUMClick(View view)
    {
        moveCameraAndSwitchIndoorLevel(
            new Point(55.754923, 37.622677),
            new Point(55.754212, 37.620953),
            "2");
    }

    public void onShowAfimallClick(View view)
    {
        moveCameraAndSwitchIndoorLevel(
            new Point(55.7493229, 37.5402281),
            new Point(55.748090, 37.538470),
            "3");
    }

    static class CameraCallbackImpl implements Map.CameraCallback {
        private WeakReference<MapIndoorActivity> weakSelf;
        private String indoorLevelId;

        public CameraCallbackImpl(WeakReference<MapIndoorActivity> weakSelf, String indoorLevelId) {
            this.weakSelf = weakSelf;
            this.indoorLevelId = indoorLevelId;
        }

        @Override
        public void onMoveFinished(boolean completed) {
            if (completed) {
                MapIndoorActivity self = weakSelf.get();
                if(self != null) {
                    if (self.activeIndoorPlan_ != null)
                        self.selectActiveLevel(indoorLevelId);
                    else
                        self.pendingIndoorLevelId_ = indoorLevelId;
                }
            }
        }
    }

    private void moveCameraAndSwitchIndoorLevel(Point southWest, Point northEast, String indoorLevelId)
    {
        pendingIndoorLevelId_ = null;
        CameraPosition camPos = mapview.getMap().cameraPosition(new BoundingBox(southWest, northEast));
        mapview.getMap().move(camPos, new Animation(Animation.Type.LINEAR, 1),
                new CameraCallbackImpl(new WeakReference<>(this), indoorLevelId));
    }
}
