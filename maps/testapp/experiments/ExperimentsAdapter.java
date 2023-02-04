package com.yandex.maps.testapp.experiments;

import android.content.Context;
import androidx.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.yandex.maps.testapp.R;

import java.util.Collection;
import java.util.List;

class ExperimentsAdapter extends ArrayAdapter<Experiment> {
    ExperimentsAdapter(Context context, int resource, List<Experiment> experiments) {
        super(context, resource, experiments);
    }

    @NonNull
    @Override
    public View getView(final int position, View view, @NonNull ViewGroup parent) {
        final Experiment experiment = getItem(position);
        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.experiment_row, parent, false);
        }
        TextView serviceId = (TextView)view.findViewById(R.id.row_service_id);
        TextView name = (TextView)view.findViewById(R.id.row_parameter_name);
        TextView value = (TextView)view.findViewById(R.id.row_parameter_value);
        Button deleteButton = (Button)view.findViewById(R.id.delete_experiment_button);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ((ExperimentsActivity)getContext()).switchExperimentButtonState(false, experiment);
            }
        });
        assert experiment != null;
        serviceId.setText(experiment.serviceId);
        name.setText(experiment.parameterName);
        value.setText(experiment.parameterValue);
        return view;
    }

    void updateList(Collection<Experiment> experiments) {
        clear();
        addAll(experiments);
        notifyDataSetChanged();
    }
}
