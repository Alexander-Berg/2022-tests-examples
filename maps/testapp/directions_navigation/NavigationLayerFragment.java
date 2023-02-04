package com.yandex.maps.testapp.directions_navigation;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.IdRes;

import com.yandex.maps.testapp.R;

public class NavigationLayerFragment extends Fragment {

    public interface NavigationLayerDockContext {
        void onAutoCameraUpdated();

        void onAutoRotationUpdated();

        void onAutoZoomUpdated();

        void onBalloonsEnabledUpdated();

        void onTrafficLightsEnabledUpdated();

        void onShowBalloonsGeometryUpdated();

        void onZoomOffsetUpdated();

        void onRecreateLayerClicked();

        void onEventSettingsClicked();

        void onJamsModeUpdated();

        void onShowPredictedUpdated();

        NavigationLayerSettings getNavigationLayerSettings();
    }

    private View panel;
    private NavigationLayerDockContext navigationLayerDockContext;
    private NavigationLayerSettings navigationLayerSettings;

    private CheckBox autoCameraCheckBox;
    private CheckBox autoRotationCheckBox;
    private CheckBox autoZoomCheckBox;
    private CheckBox balloonsCheckBox;
    private CheckBox trafficLightsCheckBox;
    private CheckBox showPredictedCheckBox;
    private CheckBox showBalloonsGeometryCheckBox;
    private SeekBar zoomOffsetSeekBar;
    private TextView zoomOffsetTextView;

    private Button recreateLayerButton;
    private Button roadEventSettingsButton;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            navigationLayerDockContext = (NavigationLayerDockContext) context;
            navigationLayerSettings = navigationLayerDockContext.getNavigationLayerSettings();
        } catch (ClassCastException ex) {
            throw new ClassCastException(context.toString() + " must implement NavigationLayerDockContext");
        }
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        panel = inflater.inflate(
                R.layout.directions_navigation_layer_fragment,
                container,
                false);

        autoCameraCheckBox = panel.findViewById(R.id.autocamera_checkbox);
        autoRotationCheckBox = panel.findViewById(R.id.autorotation_checkbox);
        autoZoomCheckBox = panel.findViewById(R.id.autozoom_checkbox);
        balloonsCheckBox = panel.findViewById(R.id.balloons_enabled_checkbox);
        trafficLightsCheckBox = panel.findViewById(R.id.traffic_lights_enabled_checkbox);
        showPredictedCheckBox = panel.findViewById(R.id.show_predicted_checkbox);
        showBalloonsGeometryCheckBox = panel.findViewById(R.id.show_balloons_geometry_checkbox);
        zoomOffsetSeekBar = panel.findViewById(R.id.zoom_offset_seek_bar);
        zoomOffsetTextView = panel.findViewById(R.id.zoom_offset_text_view);

        recreateLayerButton = panel.findViewById(R.id.recreate_layer_button);
        roadEventSettingsButton = panel.findViewById(R.id.road_event_settings_button);

        autoCameraCheckBox.setChecked(navigationLayerSettings.autoCamera);
        autoRotationCheckBox.setChecked(navigationLayerSettings.autoRotation);
        autoZoomCheckBox.setChecked(navigationLayerSettings.autoZoom);
        balloonsCheckBox.setChecked(navigationLayerSettings.balloonsEnabled);
        trafficLightsCheckBox.setChecked(navigationLayerSettings.trafficLightsEnabled);
        showPredictedCheckBox.setChecked(navigationLayerSettings.showPredicted);
        showBalloonsGeometryCheckBox.setChecked(navigationLayerSettings.showBalloonsGeometry);
        zoomOffsetSeekBar.setProgress((int) (navigationLayerSettings.zoomOffset * 10));
        setZoomOffsetText(navigationLayerSettings.zoomOffset);

        autoCameraCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigationLayerSettings.autoCamera = autoCameraCheckBox.isChecked();
                navigationLayerDockContext.onAutoCameraUpdated();
            }
        });

        autoRotationCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigationLayerSettings.autoRotation = autoRotationCheckBox.isChecked();
                navigationLayerDockContext.onAutoRotationUpdated();
            }
        });

        autoZoomCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigationLayerSettings.autoZoom = autoZoomCheckBox.isChecked();
                navigationLayerDockContext.onAutoZoomUpdated();
            }
        });

        balloonsCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigationLayerSettings.balloonsEnabled = balloonsCheckBox.isChecked();
                navigationLayerDockContext.onBalloonsEnabledUpdated();
            }
        });

        trafficLightsCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigationLayerSettings.trafficLightsEnabled = trafficLightsCheckBox.isChecked();
                navigationLayerDockContext.onTrafficLightsEnabledUpdated();
            }
        });

        showPredictedCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigationLayerSettings.showPredicted = showPredictedCheckBox.isChecked();
                navigationLayerDockContext.onShowPredictedUpdated();
            }
        });

        showBalloonsGeometryCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigationLayerSettings.showBalloonsGeometry = showBalloonsGeometryCheckBox.isChecked();
                navigationLayerDockContext.onShowBalloonsGeometryUpdated();
            }
        });

        zoomOffsetSeekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int value, boolean byUser) {
                        float offset = value / 10f;
                        navigationLayerSettings.zoomOffset = offset;
                        setZoomOffsetText(offset);
                        navigationLayerDockContext.onZoomOffsetUpdated();
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {}
                }
        );

        recreateLayerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigationLayerDockContext.onRecreateLayerClicked();
            }
        });

        roadEventSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeFragment();
                navigationLayerDockContext.onEventSettingsClicked();
            }
        });

        initJamsModeButton(R.id.jams_disabled_mode, navigationLayerSettings.jamsMode.DISABLED);
        initJamsModeButton(R.id.jams_current_route_mode, navigationLayerSettings.jamsMode.ENABLED_FOR_CURRENT_ROUTE);
        initJamsModeButton(R.id.jams_all_routes_mode, navigationLayerSettings.jamsMode.ENABLED);

        return panel;
    }

    private void initJamsModeButton(@IdRes int buttonId, NavigationLayerController.JamsMode mode) {
        RadioButton button = panel.findViewById(buttonId);
        button.setChecked(navigationLayerSettings.jamsMode == mode);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigationLayerSettings.jamsMode = mode;
                navigationLayerDockContext.onJamsModeUpdated();
            }
        });
    }

    private void setZoomOffsetText(float offset) {
        zoomOffsetTextView.setText(String.format("%+.2f", offset));
    }

    private void closeFragment() {
        getFragmentManager().popBackStack();
    }

}
