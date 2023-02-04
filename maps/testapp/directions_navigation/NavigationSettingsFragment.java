package com.yandex.maps.testapp.directions_navigation;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.yandex.mapkit.annotations.AnnotationLanguage;
import com.yandex.mapkit.directions.navigation.SpeedLimits;
import com.yandex.mapkit.directions.navigation.SpeedLimitsPolicy;
import com.yandex.maps.testapp.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.text.DecimalFormat;

public class NavigationSettingsFragment extends Fragment {

    public interface NavigationSettingsContext {
        void onAnnotationLanguageUpdated();
        void onSpeedLimitsRatioUpdated();
        void onAlternativesEnabledUpdated();
        void onMuteUpdated();

        void onAvoidTollsUpdated();
        void onAvoidUnpavedUpdated();
        void onAvoidPoorConditionsUpdated();

        void onSetVehicleOptions();
        void onSetAnnotatedEventsClicked();

        void onSimulationEnabledUpdated();
        void onFillSimulationSpeedUpdated();

        NavigationSettings getNavigationSettings();
        SpeedLimitsPolicy getSpeedLimitsPolicy();
    }

    private NavigationSettingsContext navigationSettingsContext;
    private NavigationSettings navigationSettings;

    private TextView urbanSpeedTextView;
    private TextView ruralSpeedTextView;
    private TextView expresswayTextView;

    private TextView toleranceTextView;

    private Spinner languageSelectSpinner;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            navigationSettingsContext = (NavigationSettingsContext)context;
            navigationSettings = navigationSettingsContext.getNavigationSettings();
        } catch (ClassCastException ex) {
            throw new ClassCastException(context.toString() + " must implement NavigationSettingsContext");
        }
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        View panel = inflater.inflate(
                R.layout.directions_navigation_settings_fragment,
                container,
                false);

        CheckBox backgroundWorkCheckbox = panel.findViewById(R.id.navigation_background_work_checkbox);
        backgroundWorkCheckbox.setChecked(navigationSettings.backgroundWorkEnabled);
        backgroundWorkCheckbox.setOnClickListener(view -> {
            navigationSettings.backgroundWorkEnabled = ((CheckBox) view).isChecked();
        });

        languageSelectSpinner = panel.findViewById(R.id.phrase_language);
        setupLanguageSelectSpinner();

        urbanSpeedTextView = panel.findViewById(R.id.urban_speed);
        ruralSpeedTextView = panel.findViewById(R.id.rural_speed);
        expresswayTextView = panel.findViewById(R.id.expressway_speed);

        toleranceTextView = panel.findViewById(R.id.tolerance_value);

        initSeekBar(panel);
        updateSpeedLimits();

        Button setVehicleOptionsButton = panel.findViewById(R.id.set_vehicle_options_button);
        setVehicleOptionsButton.setOnClickListener(view -> {
            navigationSettingsContext.onSetVehicleOptions();
        });

        CheckBox avoidTollsCheckbox = panel.findViewById(R.id.navigation_avoid_tolls);
        avoidTollsCheckbox.setChecked(navigationSettings.avoidTolls);
        avoidTollsCheckbox.setOnClickListener(view -> {
            navigationSettings.avoidTolls = ((CheckBox) view).isChecked();
            navigationSettingsContext.onAvoidTollsUpdated();
        });

        CheckBox avoidUnpavedCheckbox = panel.findViewById(R.id.navigation_avoid_unpaved);
        avoidUnpavedCheckbox.setChecked(navigationSettings.avoidUnpaved);
        avoidUnpavedCheckbox.setOnClickListener(view -> {
            navigationSettings.avoidUnpaved = ((CheckBox) view).isChecked();
            navigationSettingsContext.onAvoidUnpavedUpdated();
        });


        CheckBox avoidPoorConditionsCheckbox = panel.findViewById(R.id.navigation_avoid_poor_conditions);
        avoidPoorConditionsCheckbox.setChecked(navigationSettings.avoidPoorConditions);
        avoidPoorConditionsCheckbox.setOnClickListener(view -> {
            navigationSettings.avoidPoorConditions = ((CheckBox) view).isChecked();
            navigationSettingsContext.onAvoidPoorConditionsUpdated();
        });

        CheckBox enableAlternativesCheckbox = panel.findViewById(R.id.directions_navigation_enable_alternatives);
        enableAlternativesCheckbox.setChecked(navigationSettings.alternativesEnabled);
        enableAlternativesCheckbox.setOnClickListener(view -> {
            navigationSettings.alternativesEnabled = ((CheckBox) view).isChecked();
            navigationSettingsContext.onAlternativesEnabledUpdated();
        });

        CheckBox muteCheckbox = panel.findViewById(R.id.annotator_mute_status_checkbox);
        muteCheckbox.setChecked(navigationSettings.muted);
        muteCheckbox.setOnClickListener(view -> {
            navigationSettings.muted = ((CheckBox) view).isChecked();
            navigationSettingsContext.onMuteUpdated();
        });

        CheckBox simulationCheckbox = panel.findViewById(R.id.simulation_checkbox);
        simulationCheckbox.setChecked(navigationSettings.simulationEnabled);
        simulationCheckbox.setOnClickListener(view -> {
            navigationSettings.simulationEnabled = ((CheckBox) view).isChecked();
            navigationSettingsContext.onSimulationEnabledUpdated();
        });

        CheckBox fillSimulationSpeedCheckbox = panel.findViewById(R.id.fill_simulation_speed_checkbox);
        fillSimulationSpeedCheckbox.setChecked(navigationSettings.fillSimulationSpeedEnabled);
        fillSimulationSpeedCheckbox.setOnClickListener(view -> {
            navigationSettings.fillSimulationSpeedEnabled = ((CheckBox) view).isChecked();
            navigationSettingsContext.onFillSimulationSpeedUpdated();
        });

        initVisibilityCheckBox(
            panel.findViewById(R.id.show_arrival_points),
            navigationSettings.arrivalPoints);
        initVisibilityCheckBox(
            panel.findViewById(R.id.show_standing_segments_checkbox),
            navigationSettings.standingSegments);
        initVisibilityCheckBox(
            panel.findViewById(R.id.show_road_objects_checkbox),
            navigationSettings.roadObjects);

        Button setAnnotatedEventsButton = panel.findViewById(R.id.annotated_evens_button);
        setAnnotatedEventsButton.setOnClickListener(view -> {
            navigationSettingsContext.onSetAnnotatedEventsClicked();
        });

        if (getArguments().getBoolean("recorded_simulation_is_active")) {
            avoidTollsCheckbox.setEnabled(false);
            enableAlternativesCheckbox.setEnabled(false);
            setVehicleOptionsButton.setEnabled(false);
            languageSelectSpinner.setEnabled(false);
        }

        return panel;
    }

    private void initVisibilityCheckBox(CheckBox checkBox, VisibilitySetting visibilitySetting) {
        checkBox.setChecked(visibilitySetting.getVisible());
        checkBox.setOnCheckedChangeListener((view, isChecked) -> visibilitySetting.setVisible(isChecked));
    }

    private void initSeekBar(View panel) {
        SeekBar customSpeedLimitSeekBar = panel.findViewById(R.id.custom_speed_limit_seek_bar);
        customSpeedLimitSeekBar.setMax(200);
        customSpeedLimitSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float ratio = (float) progress / 100.0f;
                if (navigationSettings.speedLimitsRatio == ratio) {
                    return;
                }
                navigationSettings.speedLimitsRatio = ratio;
                updateSpeedLimits();
                navigationSettingsContext.onSpeedLimitsRatioUpdated();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        customSpeedLimitSeekBar.setProgress((int) (navigationSettings.speedLimitsRatio * 100 + 0.5));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    private static List<AnnotationLanguage> getAvailableLanguages() {
        return new ArrayList<>(Arrays.asList(AnnotationLanguage.values()));
    }

    private void setupLanguageSelectSpinner() {
        List<AnnotationLanguage> languages = getAvailableLanguages();
        int selectedIndex = languages.indexOf(navigationSettings.annotationLanguage);
        List<String> languageStrings = new ArrayList<>();
        for (AnnotationLanguage language : languages) {
            languageStrings.add(language.toString());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                languageStrings
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSelectSpinner.setAdapter(adapter);
        languageSelectSpinner.setSelection(selectedIndex);
        languageSelectSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String item = adapterView.getItemAtPosition(i).toString();
                if (navigationSettings.annotationLanguage == AnnotationLanguage.valueOf(item)) {
                    return;
                }
                navigationSettings.annotationLanguage = AnnotationLanguage.valueOf(item);
                navigationSettingsContext.onAnnotationLanguageUpdated();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) { }
        });
    }

    private void updateSpeedLimits() {
        SpeedLimitsPolicy speedLimitsPolicy = navigationSettingsContext.getSpeedLimitsPolicy();

        SpeedLimits legal = speedLimitsPolicy.getLegalSpeedLimits();
        SpeedLimits custom = speedLimitsPolicy.customSpeedLimits(navigationSettings.speedLimitsRatio);

        String legalText = legal.getUrban().getText() + " - " + custom.getUrban().getText();
        String ruralText = legal.getRural().getText() + " - " + custom.getRural().getText();
        String expresswayText = legal.getExpressway().getText() + " - " + custom.getExpressway().getText();

        DecimalFormat decimalFormat = new DecimalFormat("#.##");
        toleranceTextView.setText(decimalFormat.format(navigationSettings.speedLimitsRatio));

        if (navigationSettings.speedLimitsRatio > 1.0) {
            legalText += " (!)";
            ruralText += " (!)";
            expresswayText += " (!)";
        }
        urbanSpeedTextView.setText(legalText);
        ruralSpeedTextView.setText(ruralText);
        expresswayTextView.setText(expresswayText);
    }

    public static NavigationSettingsFragment newInstance(boolean recordedSimulationIsActive) {
        Bundle args = new Bundle();
        args.putBoolean("recorded_simulation_is_active", recordedSimulationIsActive);

        NavigationSettingsFragment fragment = new NavigationSettingsFragment();
        fragment.setArguments(args);
        return fragment;
    }

}
