package com.yandex.maps.testapp.directions_navigation;

import android.content.Context;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;

import com.yandex.maps.testapp.R;

public class NavigationActionsFragment extends Fragment {

    public interface NavigationActionsDockContext {
        void onSelectMapkitsim();
        void onSelectTestCase();

        void onParkingClicked();
        void onAlternativesClicked();

        void onSerializeClicked();
        void onDeserializeClicked();
    }

    private View panel;
    private NavigationActionsDockContext navigationActionsDockContext;

    private Button selectMapkitsimButton;
    private Button selectTestCaseButton;

    private Button parkingButton;
    private Button alternativesButton;

    private Button serializeButton;
    private Button deserializeButton;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            navigationActionsDockContext = (NavigationActionsDockContext)context;
        } catch (ClassCastException ex) {
            throw new ClassCastException(context.toString() + " must implement NavigationActionsDockContext");
        }
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        panel = inflater.inflate(
                R.layout.directions_navigation_actions_fragment,
                container,
                false);

        selectMapkitsimButton = panel.findViewById(R.id.select_mapkitsim_button);
        selectTestCaseButton = panel.findViewById(R.id.select_test_case_button);

        parkingButton = panel.findViewById(R.id.parking_button);
        alternativesButton = panel.findViewById(R.id.alternatives_button);

        serializeButton = panel.findViewById(R.id.serialize_button);
        deserializeButton = panel.findViewById(R.id.deserialize_button);

        selectMapkitsimButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeFragment();
                navigationActionsDockContext.onSelectMapkitsim();
            }
        });

        selectTestCaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeFragment();
                navigationActionsDockContext.onSelectTestCase();
            }
        });

        parkingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigationActionsDockContext.onParkingClicked();
            }
        });

        alternativesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigationActionsDockContext.onAlternativesClicked();
            }
        });

        serializeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigationActionsDockContext.onSerializeClicked();
            }
        });

        deserializeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigationActionsDockContext.onDeserializeClicked();
            }
        });

        if (getArguments().getBoolean("recorded_simulation_is_active")) {
            alternativesButton.setEnabled(false);
            parkingButton.setEnabled(false);
            serializeButton.setEnabled(false);
        }

        return panel;
    }

    private void closeFragment() {
        getFragmentManager().popBackStack();
    }

    public static NavigationActionsFragment newInstance(boolean recordedSimulationIsActive) {
        Bundle args = new Bundle();
        args.putBoolean("recorded_simulation_is_active", recordedSimulationIsActive);

        NavigationActionsFragment fragment = new NavigationActionsFragment();
        fragment.setArguments(args);
        return fragment;
    }
}
