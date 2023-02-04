package com.yandex.maps.testapp.guidance;

import android.content.Context;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.yandex.mapkit.directions.guidance.Guide;
import com.yandex.maps.testapp.R;

public class SimulationDockFragment extends Fragment {

    public interface SimulationDockContext {
        void onSimulation(boolean turnOn);
        void onSelectTestCase();
        void onSelectReport();
        void onResetRoute();
        void onRecordPause();
        void onSetVehicleOptions();
        void updateStandingSegments();

        Guide getGuide();

        boolean isRecordedSimulationOn();
        void setSimulationClockRate(int clockRate);
        int getSimulationClockRate();

        boolean isSimulationOn();
        void setSimulationSpeed(int speed);
        double getSimulationSpeed();

        SimulationParameters getSimulationParameters();
    }

    static private final String speedTextTemplate = "Simulation Speed: %.0f m/s";
    static private final String clockRateTextTemplate = "Simulation Speed: %dx";

    private Guide guide;

    private View panel;
    private SimulationDockContext simulationDockContext;
    private SimulationParameters simulationParameters;

    private CheckBox muteCheckBox;
    private CheckBox standingSegmentsCheckBox;
    private CheckBox parkingRoutesBox;
    private CheckBox backgroundWorkCheckBox;

    private ToggleButton simulateToggle;
    private Button resetRouteButton;
    private Button selectTestCaseButton;
    private Button selectReportButton;
    private Button vehicleOptionsButton;
    private ToggleButton pauseReportToggle;

    private SeekBar simulationSpeedSeek;
    private TextView simulationSpeedText;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            simulationDockContext = (SimulationDockContext)context;
            simulationParameters = simulationDockContext.getSimulationParameters();
        } catch (ClassCastException ex) {
            throw new ClassCastException(context.toString() + " must implement SimulationDockContext");
        }
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        guide = simulationDockContext.getGuide();

        panel = inflater.inflate(
                R.layout.guidance_simulation_dock_fragment,
                container,
                false);

        muteCheckBox = panel.findViewById(R.id.mute_checkbox);
        standingSegmentsCheckBox = panel.findViewById(R.id.show_standing_segments_checkbox);
        parkingRoutesBox = panel.findViewById(R.id.use_parking_routes_checkbox);
        backgroundWorkCheckBox = panel.findViewById(R.id.background_work_checkbox);

        simulateToggle = panel.findViewById(R.id.simulate_toggle);
        resetRouteButton = panel.findViewById(R.id.reset_route_button);
        selectTestCaseButton = panel.findViewById(R.id.select_test_case_button);
        selectReportButton = panel.findViewById(R.id.select_report_button);
        pauseReportToggle = panel.findViewById(R.id.pause_report_simulation_toggle);
        vehicleOptionsButton = panel.findViewById(R.id.vehicle_options);

        simulationSpeedSeek = panel.findViewById(R.id.speed_seek_bar);
        simulationSpeedText = panel.findViewById(R.id.speed_label);

        muteCheckBox.setChecked(simulationParameters.muted);
        standingSegmentsCheckBox.setChecked(simulationParameters.showStandingSegments);
        parkingRoutesBox.setChecked(simulationParameters.useParkingRoutes);
        backgroundWorkCheckBox.setChecked(simulationParameters.backgroundWork);

        setControlsMode(simulationDockContext.isRecordedSimulationOn());

        muteCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                simulationParameters.muted = muteCheckBox.isChecked();
                if (simulationParameters.muted) {
                    guide.mute();
                } else {
                    guide.unmute();
                }
            }
        });

        standingSegmentsCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                simulationParameters.showStandingSegments = standingSegmentsCheckBox.isChecked();
                simulationDockContext.updateStandingSegments();
            }
        });

        parkingRoutesBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                simulationParameters.useParkingRoutes = parkingRoutesBox.isChecked();
                guide.setParkingRoutesEnabled(parkingRoutesBox.isChecked());
            }
        });

        backgroundWorkCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                simulationParameters.backgroundWork = backgroundWorkCheckBox.isChecked();
            }
        });

        simulateToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                simulationDockContext.onSimulation(simulateToggle.isChecked());
                setControlsMode(simulationDockContext.isRecordedSimulationOn());
            }
        });

        selectTestCaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeFragment();
                simulationDockContext.onSelectTestCase();
            }
        });

        resetRouteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                simulationDockContext.onResetRoute();
            }
        });

        selectReportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeFragment();
                simulationDockContext.onSelectReport();
            }
        });

        pauseReportToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                simulationParameters.recordedSimulationPaused = pauseReportToggle.isChecked();
                simulationDockContext.onRecordPause();
            }
        });

        vehicleOptionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                simulationDockContext.onSetVehicleOptions();
            }
        });

        return panel;
    }

    private abstract class OnSpeedSeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    }

    private void closeFragment() {
        getFragmentManager().popBackStack();
    }

    private boolean isAnySimulationOn() {
        return simulationDockContext.isSimulationOn() ||
               simulationDockContext.isRecordedSimulationOn();
    }

    private void setControlsMode(boolean isRecordedSimulationOn) {
        if (isRecordedSimulationOn) {
            resetToRecordedSimulationMode();
        } else {
            resetToDefaultMode();
        }
    }

    private void resetToRecordedSimulationMode() {
        selectReportButton.setVisibility(View.GONE);
        pauseReportToggle.setVisibility(View.VISIBLE);
        pauseReportToggle.setChecked(simulationParameters.recordedSimulationPaused);
        simulateToggle.setChecked(isAnySimulationOn());

        simulationSpeedSeek.setMax(SimulationParameters.MAX_CLOCK_RATE - 1);
        simulationSpeedSeek.setProgress(simulationDockContext.getSimulationClockRate() - 1);
        simulationSpeedText.setText(String.format(clockRateTextTemplate, simulationDockContext.getSimulationClockRate()));
        simulationSpeedSeek.setOnSeekBarChangeListener(new OnSpeedSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    simulationSpeedText.setText(String.format(clockRateTextTemplate, progress + 1));
                    simulationDockContext.setSimulationClockRate(progress + 1);
                }
            }
        });
    }

    private void resetToDefaultMode() {
        pauseReportToggle.setChecked(false);
        pauseReportToggle.setVisibility(View.GONE);
        selectReportButton.setVisibility(View.VISIBLE);
        simulateToggle.setChecked(isAnySimulationOn());

        simulationSpeedSeek.setMax(SimulationParameters.MAX_SIMULATION_SPEED);
        simulationSpeedSeek.setProgress((int) simulationDockContext.getSimulationSpeed());
        simulationSpeedText.setText(String.format(speedTextTemplate, (double)simulationSpeedSeek.getProgress()));
        simulationSpeedSeek.setOnSeekBarChangeListener(new OnSpeedSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    simulationSpeedText.setText(String.format(speedTextTemplate, (double)progress));
                    simulationDockContext.setSimulationSpeed(progress);
                }
            }
        });
    }
}
