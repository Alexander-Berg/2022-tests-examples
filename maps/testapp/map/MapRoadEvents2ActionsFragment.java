package com.yandex.maps.testapp.map;

import android.content.Context;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ToggleButton;

import com.yandex.maps.testapp.R;

public class MapRoadEvents2ActionsFragment extends Fragment {

    MapRoadEvents2ActionsFragment(
            boolean userLocationEnabled,
            boolean zoomAnimation) {
        this.userLocationEnabled = userLocationEnabled;
        this.zoomAnimation = zoomAnimation;
    }

    public interface MapRoadEvents2ActionsDockContext {
        void onUserLocationClicked(boolean enable);
        void onZoomAnimationClicked(boolean enable);

        void onChangeZoomClicked();
        void onRunDrivingTestClicked();
        void onRunNoLocationTestClicked();
        void onStopTestClicked();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mapRoadEvents2ActionsDockContext = (MapRoadEvents2ActionsDockContext)context;
        } catch (ClassCastException ex) {
            throw new ClassCastException(
                context.toString() + " must implement MapRoadEvents2ActionsDockContext");
        }
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        panel = inflater.inflate(
                R.layout.map_road_events_2_actions_fragment,
                container,
                false);

        userLocationButton = panel.findViewById(R.id.switch_user_location);
        userLocationButton.setChecked(userLocationEnabled);

        zoomAnimationButton = panel.findViewById(R.id.switch_zoom_animation);
        zoomAnimationButton.setChecked(zoomAnimation);

        runDrivingTestButton = panel.findViewById(R.id.run_driving_test);
        runNoLocationTestButton = panel.findViewById(R.id.run_no_location_test);
        stopTestButton = panel.findViewById(R.id.stop_test);
        changeZoomButton = panel.findViewById(R.id.change_zoom);

        userLocationButton.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mapRoadEvents2ActionsDockContext.onUserLocationClicked(isChecked);
            }
        });

        zoomAnimationButton.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mapRoadEvents2ActionsDockContext.onZoomAnimationClicked(isChecked);
            }
        });

        changeZoomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapRoadEvents2ActionsDockContext.onChangeZoomClicked();
            }
        });

        runDrivingTestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeFragment();
                mapRoadEvents2ActionsDockContext.onRunDrivingTestClicked();
            }
        });

        runNoLocationTestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeFragment();
                mapRoadEvents2ActionsDockContext.onRunNoLocationTestClicked();
            }
        });

        stopTestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeFragment();
                mapRoadEvents2ActionsDockContext.onStopTestClicked();
            }
        });

        return panel;
    }

    private void closeFragment() {
        getFragmentManager().popBackStack();
    }

    private View panel;
    private MapRoadEvents2ActionsDockContext mapRoadEvents2ActionsDockContext;

    private ToggleButton userLocationButton;
    private ToggleButton zoomAnimationButton;

    private Button runDrivingTestButton;
    private Button runNoLocationTestButton;
    private Button stopTestButton;
    private Button changeZoomButton;
    private boolean userLocationEnabled;
    private boolean zoomAnimation;
}
