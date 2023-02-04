package com.yandex.maps.testapp.guidance;

import android.content.Context;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import com.yandex.mapkit.directions.guidance.SpeedingPolicy;
import com.yandex.mapkit.directions.guidance.SpeedLimits;
import com.yandex.mapkit.annotations.AnnotationLanguage;
import com.yandex.maps.testapp.R;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by dbeliakov on 21.06.16.
 */
public class GuidanceSettingsFragment extends Fragment {

    public interface AnnotationSettingsContext {
        void onSettingsChanged();
        GuidanceSettings getGuidanceSettings();
        SpeedingPolicy getSpeedingPolicy();
    }

    private AnnotationSettingsContext annotationSettingsContext;
    private GuidanceSettings guidanceSettings;

    private TextView regionTextView;
    private TextView urbanSpeedTextView;
    private TextView ruralSpeedTextView;
    private TextView expresswayTextView;

    private Spinner languageSelectSpinner;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            annotationSettingsContext = (AnnotationSettingsContext)context;
            guidanceSettings = annotationSettingsContext.getGuidanceSettings();
        } catch (ClassCastException ex) {
            throw new ClassCastException(context.toString() + " must implement SimulationDockContext");
        }
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        View panel = inflater.inflate(
                R.layout.guidance_annotation_settings_fragment,
                container,
                false);

        regionTextView = panel.findViewById(R.id.current_region);
        urbanSpeedTextView = panel.findViewById(R.id.urban_speed);
        ruralSpeedTextView = panel.findViewById(R.id.rural_speed);
        expresswayTextView = panel.findViewById(R.id.expressway_speed);

        SeekBar customSpeedLimitSeekBar = panel.findViewById(R.id.custom_speed_limit_seek_bar);
        customSpeedLimitSeekBar.setMax(300);
        customSpeedLimitSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                final float tolerance = (float) progress / 100;
                guidanceSettings.speedLimitsRatio = tolerance;
                updateSpeedLimits();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        customSpeedLimitSeekBar.setProgress((int) (guidanceSettings.speedLimitsRatio * 100 + 0.5));

        setupCheckBox(panel, R.id.maneuvers);
        setupCheckBox(panel, R.id.speed_excess);
        setupCheckBox(panel, R.id.road_events);
        setupCheckBox(panel, R.id.freedrive);

        // AnnotatedEventTags
        setupCheckBox(panel, R.id.speed_control);
        setupCheckBox(panel, R.id.mobile_control);
        setupCheckBox(panel, R.id.cross_road_control);
        setupCheckBox(panel, R.id.road_marking_control);
        setupCheckBox(panel, R.id.lane_control);
        setupCheckBox(panel, R.id.school);
        setupCheckBox(panel, R.id.accident);
        setupCheckBox(panel, R.id.reconstruction);
        setupCheckBox(panel, R.id.danger);
        setupCheckBox(panel, R.id.overtaking_danger);
        setupCheckBox(panel, R.id.pedestrian_danger);
        setupCheckBox(panel, R.id.cross_road_danger);

        // Display settings
        setupCheckBox(panel, R.id.show_events);
        setupCheckBox(panel, R.id.show_lanes);
        setupCheckBox(panel, R.id.show_maneuvers);
        setupCheckBox(panel, R.id.show_traffic_lights);
        setupCheckBox(panel, R.id.show_direction_signs);

        // Driving options
        setupCheckBox(panel, R.id.guidance_avoid_tolls);
        setupCheckBox(panel, R.id.guidance_avoid_unpaved);
        setupCheckBox(panel, R.id.guidance_avoid_poor_conditions);

        languageSelectSpinner = panel.findViewById(R.id.phrase_language);
        setupLanguageSelectSpinner();

        return panel;
    }

    @Override
    public void onDestroyView() {
        annotationSettingsContext.onSettingsChanged();
        super.onDestroyView();
    }

    void updateSpeedLimits() {
        SpeedingPolicy policy = annotationSettingsContext.getSpeedingPolicy();

        Integer region = policy.getRegion();
        regionTextView.setText(region != null ? regionToName(region) : "");

        SpeedLimits legal = policy.getLegalSpeedLimits();
        SpeedLimits custom = policy.customSpeedLimits(guidanceSettings.speedLimitsRatio);

        String legalText = legal.getUrban().getText() + " - " + custom.getUrban().getText();
        String ruralText = legal.getRural().getText() + " - " + custom.getRural().getText();
        String expresswayText = legal.getExpressway().getText() + " - " + custom.getExpressway().getText();

        if (guidanceSettings.speedLimitsRatio > 1.0) {
            legalText += " (!)";
            ruralText += " (!)";
            expresswayText += " (!)";
        }
        urbanSpeedTextView.setText(legalText);
        ruralSpeedTextView.setText(ruralText);
        expresswayTextView.setText(expresswayText);
    }

    private static List<AnnotationLanguage> getAvailableLanguages() {
        List<AnnotationLanguage> result = new ArrayList<>();
        for (AnnotationLanguage language : AnnotationLanguage.values()) {
            result.add(language);
        }
        return result;
    }

    private void setupLanguageSelectSpinner() {
        List<AnnotationLanguage> languages = getAvailableLanguages();
        int selectedIndex = languages.indexOf(guidanceSettings.speakerLanguage);
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
                guidanceSettings.speakerLanguage = AnnotationLanguage.valueOf(item);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) { }
        });
    }

    private String regionToName(Integer regionId) {
        if (regionId == null) {
            return "Unknown";
        }
        String regionKey = "region_" + regionId.toString();
        int regionResourceId = getResources().getIdentifier(regionKey, "string", getContext().getPackageName());
        if (regionResourceId == 0) {
            return regionId.toString();
        }
        return getResources().getString(regionResourceId);
    }

    private void setupCheckBox(View panel, final Integer checkBoxId) {
        CheckBox checkBox = panel.findViewById(checkBoxId);
        checkBox.setChecked(guidanceSettings.switches.get(checkBoxId));

        checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                guidanceSettings.switches.put(checkBoxId, ((CheckBox) view).isChecked());
            }
        });
    }
}
