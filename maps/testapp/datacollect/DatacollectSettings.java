package com.yandex.maps.testapp.datacollect;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.experiments.Experiment;
import com.yandex.maps.testapp.experiments.ExperimentsUtils;


public class DatacollectSettings extends Fragment {
    private final Experiment datacollectExperiment = new Experiment("MAPKIT", "collect_tracks_via_passive_provider", "on");

    private DockContext dockContext;

    public interface DockContext {
        boolean isUsingCustomLM();

        void setUsingCustomLM(boolean usingCustomLM);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            dockContext = (DockContext) context;
        } catch (ClassCastException ex) {
            throw new ClassCastException(context.toString() + " must implement DatacollectSettings.DockContext");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View panel = inflater.inflate(R.layout.datcollect_settings_fragment, container, false);

        CheckBox experimentCheckbox = panel.findViewById(R.id.datacollect_experiment_checkbox);
        experimentCheckbox.setChecked(isExperimentEnabled());
        experimentCheckbox.setOnCheckedChangeListener(this::onExperimentCheckboxClicked);

        CheckBox useCustomLmCheckbox = panel.findViewById(R.id.datacollect_use_custom_lm);
        useCustomLmCheckbox.setChecked(dockContext.isUsingCustomLM());
        useCustomLmCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> dockContext.setUsingCustomLM(isChecked));

        return panel;
    }

    public void onExperimentCheckboxClicked(View checkbox, boolean checked) {
        if (checked) {
            ExperimentsUtils.refreshCustomExperiment(datacollectExperiment);
            ExperimentsUtils.addExperimentToDump(datacollectExperiment, getContext());
        } else {
            ExperimentsUtils.resetCustomExperiment(datacollectExperiment);
            ExperimentsUtils.removeExperimentFromDump(datacollectExperiment, getContext());
        }
    }

    private boolean isExperimentEnabled() {
        return ExperimentsUtils.loadExperimentsList(getContext()).contains(datacollectExperiment);
    }
}
