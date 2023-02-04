package com.yandex.maps.testapp.experiments;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;

import com.yandex.maps.testapp.R;

import java.util.Collection;
import java.util.List;

class ExperimentButtonsAdapter extends ArrayAdapter<ExperimentButton> {
    ExperimentButtonsAdapter(Context context, int resource, List<ExperimentButton> experiments) {
        super(context, resource, experiments);
    }

    @NonNull
    @Override
    public View getView(final int position, View view, @NonNull ViewGroup parent) {
        final ExperimentButton expBtn = getItem(position);
        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.toggle_button, parent, false);
        }

        ToggleButton viewAsButton = (ToggleButton) view.findViewById(R.id.toggle_button_body);

        viewAsButton.setOnCheckedChangeListener(null);
        viewAsButton.setText(expBtn.name);
        viewAsButton.setTextOn(expBtn.name);
        viewAsButton.setTextOff(expBtn.name);
        viewAsButton.setChecked(expBtn.on);
        viewAsButton.setOnCheckedChangeListener(expBtn.listener);

        return viewAsButton;
    }

    void updateList(Collection<ExperimentButton> experiments) {
        clear();
        addAll(experiments);
        notifyDataSetChanged();
    }

}
