package com.yandex.maps.testapp.fitness_navigation;

import android.content.Context;
import android.os.Bundle;
import android.text.Layout;
import android.widget.LinearLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.yandex.mapkit.transport.masstransit.FilterVehicleTypes;
import com.yandex.mapkit.transport.navigation.Type;
import com.yandex.maps.testapp.R;

import org.jetbrains.annotations.NotNull;

public class FitnessSettingsFragment extends Fragment {

    private FitnessNavigationActivity navigationActivity;
    private CheckBox backgroundModeBtn;
    private CheckBox suspendBtn;
    private ToggleButton layerBtn;
    private CheckBox showRequestPointsBtn;
    private CheckBox rotationBtn;
    private CheckBox autoZoomBtn;
    private CheckBox autoSwitchModesBtn;
    private SeekBar speedBar;
    private TextView speedTextView;
    private SeekBar maxAlternativeCountBar;
    private TextView maxAlternativeCountTextView;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            navigationActivity = (FitnessNavigationActivity)context;
        } catch (ClassCastException ex) {
            throw new ClassCastException(context.toString() + " must be FitnessNavigationActivity");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View panel = inflater.inflate(
                R.layout.fitness_settings_fragment,
                container,
                false);

        backgroundModeBtn = panel.findViewById(R.id.backgroundModeBtn);
        suspendBtn = panel.findViewById(R.id.suspendBtn);

        initSourceRadioBtn(panel, R.id.gpsRadioBtn, FitnessNavigationActivity.LocationSource.GPS);
        initSourceRadioBtn(panel, R.id.simulatorRadioBtn, FitnessNavigationActivity.LocationSource.Simulator);

        initTypeRadioBtn(panel, R.id.pedestrianBtn, Type.PEDESTRIAN);
        initTypeRadioBtn(panel, R.id.bicycleBtn, Type.BICYCLE);
        initTypeRadioBtn(panel, R.id.scooterBtn, Type.SCOOTER);
        initTypeRadioBtn(panel, R.id.masstransitBtn, Type.MASSTRANSIT);

        initTransportAvoidBtn(panel, R.id.checkbox_bus, FilterVehicleTypes.BUS);
        initTransportAvoidBtn(panel, R.id.checkbox_minibus, FilterVehicleTypes.MINIBUS);
        initTransportAvoidBtn(panel, R.id.checkbox_trolleybus, FilterVehicleTypes.TROLLEYBUS);
        initTransportAvoidBtn(panel, R.id.checkbox_tramway, FilterVehicleTypes.TRAMWAY);
        initTransportAvoidBtn(panel, R.id.checkbox_underground, FilterVehicleTypes.UNDERGROUND);
        initTransportAvoidBtn(panel, R.id.checkbox_railway, FilterVehicleTypes.RAILWAY);
        initTransportAvoidBtn(panel, R.id.checkbox_suburban, FilterVehicleTypes.SUBURBAN);

        layerBtn = panel.findViewById(R.id.layerBtn);
        showRequestPointsBtn = panel.findViewById(R.id.showRequestPointsBtn);
        rotationBtn = panel.findViewById(R.id.rotationBtn);
        autoZoomBtn = panel.findViewById(R.id.autoZoomBtn);
        autoSwitchModesBtn = panel.findViewById(R.id.autoSwitchModesBtn);
        speedBar = panel.findViewById(R.id.speedBar);
        speedTextView = panel.findViewById(R.id.speedTextView);

        maxAlternativeCountBar = panel.findViewById(R.id.maxAlternativeCountBar);
        maxAlternativeCountTextView = panel.findViewById(R.id.maxAlternativeCountTextView);

        init();

        return panel;
    }

    private void initSourceRadioBtn(@NotNull View panel, @IdRes int id, FitnessNavigationActivity.LocationSource source) {
        RadioButton btn = panel.findViewById(id);
        btn.setChecked(navigationActivity.getLocationSource() == source);
        btn.setOnClickListener(view -> {
            navigationActivity.initNavigation(source, null);
            init();
        });
    }

    private void initTypeRadioBtn(@NotNull View panel, @IdRes int id, Type type) {
        RadioButton btn = panel.findViewById(id);
        btn.setChecked(navigationActivity.getNavigationType() == type);

        btn.setOnClickListener(view -> {
            navigationActivity.setNavigationType(type);
            navigationActivity.initNavigation(navigationActivity.getLocationSource(), null);
        });
    }

    private void initTransportAvoidBtn(@NotNull View panel, @IdRes int id, FilterVehicleTypes type) {
        CheckBox checkBox = panel.findViewById(id);

        checkBox.setChecked((navigationActivity.getTransportAvoidTypes() & type.value) > 0);

        checkBox.setOnCheckedChangeListener((view, isChecked) -> {
            int avoidTypes = navigationActivity.getTransportAvoidTypes();
            avoidTypes = isChecked ? (avoidTypes | type.value) : (avoidTypes & (~type.value));
            navigationActivity.setTransportAvoidTypes(avoidTypes);
        });
    }

    private void init() {
        backgroundModeBtn.setChecked(navigationActivity.isBackgroundMode());
        backgroundModeBtn.setOnCheckedChangeListener((compoundButton, isChecked) -> navigationActivity.setBackgroundMode(isChecked));

        suspendBtn.setChecked(navigationActivity.isSuspend());
        suspendBtn.setOnCheckedChangeListener((compoundButton, isChecked) -> navigationActivity.setSuspend(isChecked));

        layerBtn.setChecked(navigationActivity.getLayer() != null);
        layerBtn.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (isChecked)
                navigationActivity.createLayer();
            else
                navigationActivity.removeLayer();

            showRequestPointsBtn.setEnabled(isChecked);
            rotationBtn.setEnabled(isChecked);
            autoZoomBtn.setEnabled(isChecked);
            autoSwitchModesBtn.setEnabled(isChecked);
        });

        showRequestPointsBtn.setEnabled(layerBtn.isChecked());
        showRequestPointsBtn.setChecked(navigationActivity.getLayer() != null && navigationActivity.getLayer().isShowRequestPoints());
        showRequestPointsBtn.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            navigationActivity.getLayer().setShowRequestPoints(isChecked);
        });

        rotationBtn.setChecked(navigationActivity.isRotate());
        rotationBtn.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            navigationActivity.setRotate(isChecked);
        });

        autoZoomBtn.setChecked(navigationActivity.isAutoZoom());
        autoZoomBtn.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            navigationActivity.setAutoZoom(isChecked);
        });

        autoSwitchModesBtn.setChecked(navigationActivity.getLayer() != null && navigationActivity.getLayer().getCamera().isSwitchModesAutomatically());
        autoSwitchModesBtn.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (navigationActivity.getLayer() != null)
                navigationActivity.getLayer().getCamera().setSwitchModesAutomatically(isChecked);
        });

        boolean enabledSpeedControls = navigationActivity.getLocationSource() == FitnessNavigationActivity.LocationSource.Simulator;
        speedBar.setEnabled(enabledSpeedControls);
        speedTextView.setEnabled(enabledSpeedControls);

        speedBar.setProgress(navigationActivity.getSimulatorSpeed());
        setSpeedTextValue(navigationActivity.getSimulatorSpeed());
        speedBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                navigationActivity.setSimulatorSpeed(progress);
                setSpeedTextValue(navigationActivity.getSimulatorSpeed());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });


        maxAlternativeCountBar.setProgress(navigationActivity.getMaxAlternativeCount());
        setMaxAlternativeCountTextValue(navigationActivity.getMaxAlternativeCount());
        maxAlternativeCountBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                navigationActivity.setMaxAlternativeCount(progress);
                setMaxAlternativeCountTextValue(navigationActivity.getMaxAlternativeCount());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    void setSpeedTextValue(int value) {
        speedTextView.setText(String.format("Simulation Speed: %d km/h", value));
    }

    void setMaxAlternativeCountTextValue(int value) {
        maxAlternativeCountTextView.setText(String.format("Max alternative count: %d", value));
    }
}
