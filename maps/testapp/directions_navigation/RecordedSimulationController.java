package com.yandex.maps.testapp.directions_navigation;

import android.content.Context;

import com.yandex.mapkit.directions.DirectionsFactory;
import com.yandex.mapkit.directions.simulation.RecordedSimulator;
import com.yandex.mapkit.directions.simulation.RecordedSimulatorListener;
import com.yandex.mapkit.directions.navigation.Navigation;
import com.yandex.mapkit.directions.navigation.Guidance;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.runtime.i18n.I18nManager;
import com.yandex.runtime.i18n.I18nManagerFactory;
import com.yandex.runtime.recording.ReportData;
import com.yandex.maps.testapp.Utils;

import java.util.logging.Level;
import java.util.logging.Logger;

public class RecordedSimulationController implements RecordedSimulatorListener {
    private static final Logger LOGGER = Logger.getLogger("yandex.maps");
    private static final I18nManager i18nManager = I18nManagerFactory.getI18nManagerInstance();

    private final int DEFAULT_RECORDED_SIMULATION_CLOCK_RATE = 1;

    private final Context context;
    private final Navigation navigation;
    private final Guidance guidance;

    private RecordedSimulator recordedSimulator;
    private Long endTime;

    private final Listener listener;

    public interface Listener {
        void onRecordedSimulationStarted();
        void onRecordedSimulationStopped();
        void onRecordedSimulationFinished();
        void onRecordedSimulationLocationUpdated();
    }

    public RecordedSimulationController(Navigation navigation, Guidance guidance, Listener listener, Context context) {
	    this.navigation = navigation;
	    this.guidance = guidance;
	    this.context = context;
	    this.listener = listener;
    }

    public void startRecordedSimulation(ReportData report) {
	    if (recordedSimulator != null) {
            return;
        }

	    endTime = report.getEndTime();

        Utils.showMessage(
                context,
                "Starting recorded simulation",
                Level.INFO,
                LOGGER);


        guidance.setEnableAlternatives(false);
        
        navigation.suspend();

        recordedSimulator = DirectionsFactory.getInstance().createRecordedSimulator(report);
        recordedSimulator.subscribeForSimulatorEvents(this);
        MapKitFactory.getInstance().setLocationManager(recordedSimulator);
        recordedSimulator.resume();

        navigation.resume();

        listener.onRecordedSimulationStarted();
    }

    public String timeLeft() {
        if (!isRecordedSimulationActive())
            return null;
        int duration = (int) ((endTime - recordedSimulator.getTimestamp()) / 1000);
        return i18nManager.localizeDuration(duration);
    }

    @Override
    public void onLocationUpdated() {
        listener.onRecordedSimulationLocationUpdated();
    }

    @Override
    public void onRouteUpdated() {
    }

    @Override
    public void onRouteUriUpdated() {
        RecordedSimulator.RouteChangeReason reason = recordedSimulator.getRouteChangeReason();

        if (reason == null) {
            LOGGER.warning("Null RouteChangeReason in onRouteUriUpdated()");
            return;
        }

        switch (reason) {
            case USER:
                String routeUri = recordedSimulator.getRouteUri();
                if (routeUri != null) {
                    navigation.resolveUri(routeUri);
                } else {
                    LOGGER.warning(String.format("Null RouteUri while RouteChangeReason is %s", reason.toString()));
                }
                break;
            case REROUTING:
                LOGGER.info("RouteUri updated with RouteChangeReason REROUTING");
                break;
            case FREEDRIVE:
                navigation.startGuidance(null);
                break;
            case STOP:
                navigation.stopGuidance();
                break;
            case FINISH:
                break;
            default:
                throw new AssertionError("Unknown RouteChangeReason");
        }
    }

    @Override
    public void onProblemMark() {
        Utils.showMessage(
                context,
                "Problem marked",
                Level.INFO,
                LOGGER);
    }

    @Override
    public void onFinish() {
        Utils.showMessage(
                context,
                "Recorded simulation finished",
                Level.INFO,
                LOGGER);
        resetRecordedSimulation();
        listener.onRecordedSimulationFinished();
    }

    public void stopRecordedSimulation() {
        if (recordedSimulator == null) {
            return;
        }

        resetRecordedSimulation();
        listener.onRecordedSimulationStopped();
    }

    public boolean isRecordedSimulationActive() {
        return recordedSimulator != null;
    }

    public int getRecordedSimulationClockRate() {
        if (recordedSimulator == null) {
            return DEFAULT_RECORDED_SIMULATION_CLOCK_RATE;
        }
        return recordedSimulator.getClockRate();
    }

    public void setRecordedSimulationClockRate(int clockRate) {
        if (recordedSimulator == null) {
            return;
        }
        recordedSimulator.setClockRate(clockRate);
    }

    public void suspend() {
        if (recordedSimulator == null) {
            return;
        }
        recordedSimulator.suspend();
    }

    public void resume() {
        if (recordedSimulator == null) {
            return;
        }
        recordedSimulator.resume();
    }

    private void resetRecordedSimulation() {
    	navigation.suspend();

        recordedSimulator.unsubscribeFromSimulatorEvents(this);
        recordedSimulator = null;
        MapKitFactory.getInstance().resetLocationManagerToDefault();
        
        navigation.resume();
    }
}

