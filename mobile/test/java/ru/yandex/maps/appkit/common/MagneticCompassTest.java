package ru.yandex.maps.appkit.common;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.view.Display;
import android.view.WindowManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.TimeUnit;

import io.reactivex.schedulers.TestScheduler;
import ru.yandex.yandexmaps.app.lifecycle.AppLifecycleDelegation;
import ru.yandex.yandexmaps.compass.MagneticCompass;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class MagneticCompassTest {
    private final TestScheduler testScheduler = new TestScheduler();
    private MagneticCompass compass;
    @Mock
    private WindowManager windowManager;
    @Mock
    private Display display;
    @Mock
    private SensorManager sensorManager;
    @Mock
    private Sensor rotation;
    @Mock
    private Sensor magnetometer;

    private AppLifecycleDelegation.Suspendable appLifecycleDelegationSuspendable;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        Mockito.when(windowManager.getDefaultDisplay()).thenReturn(display);
        Mockito.when(sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)).thenReturn(rotation);
        Mockito.when(sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)).thenReturn(magnetometer);
        Mockito.when(sensorManager.registerListener(any(), eq(rotation), anyInt())).thenReturn(true);
        Mockito.when(sensorManager.registerListener(any(), eq(magnetometer), anyInt())).thenReturn(true);

        final AppLifecycleDelegation appLifecycleDelegation = new AppLifecycleDelegation() {
            @Override
            public void suspendAlongLifecycle(Suspendable suspendable, boolean notifyAboutCurrentState) {
                appLifecycleDelegationSuspendable = suspendable;
            }

            @Override
            public void remove(Suspendable suspendable) {

            }
        };
        compass = new MagneticCompass(windowManager, sensorManager, appLifecycleDelegation, testScheduler, testScheduler);
    }

    @Test
    public void severalEnablesDoNothing() {
        appLifecycleDelegationSuspendable.resume();

        compass.setEnabled(false);
        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        compass.setEnabled(true);
        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        compass.setEnabled(true);
        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        Mockito.verify(sensorManager, times(1)).getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        Mockito.verify(sensorManager, times(1)).getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        Mockito.verify(sensorManager, times(1)).registerListener(any(), eq(rotation), anyInt());
        Mockito.verify(sensorManager, times(1)).registerListener(any(), eq(magnetometer), anyInt());
    }

    @Test
    public void notResumed_notEnabled_doesNotSubscribe() {
        verify(sensorManager, never()).registerListener(any(), eq(magnetometer), anyInt());
    }

    @Test
    public void notResumed_enabled_doesNotSubscribe() {
        compass.setEnabled(true);

        verify(sensorManager, never()).registerListener(any(), eq(magnetometer), anyInt());
    }

    @Test
    public void notResumed_enabled_disabled_enabled_doesNotSubscribe() {
        compass.setEnabled(true);
        compass.setEnabled(false);
        compass.setEnabled(true);

        verify(sensorManager, never()).registerListener(any(), eq(magnetometer), anyInt());
    }

    @Test
    public void resumed_notEnabled_doesNotSubscribe() {
        appLifecycleDelegationSuspendable.resume();

        verify(sensorManager, never()).registerListener(any(), eq(magnetometer), anyInt());
    }

    @Test
    public void resumed_enabled_subscribes() {
        appLifecycleDelegationSuspendable.resume();
        compass.setEnabled(true);

        verify(sensorManager, times(1)).registerListener(any(), eq(magnetometer), anyInt());
    }

    @Test
    public void resumed_enabled_disabled_subscribesAndThenUnsubscribes() {
        appLifecycleDelegationSuspendable.resume();
        compass.setEnabled(true);
        compass.setEnabled(false);

        InOrder inOrder = Mockito.inOrder(sensorManager);

        inOrder.verify(sensorManager).registerListener(any(), eq(magnetometer), anyInt());
        inOrder.verify(sensorManager).unregisterListener(any(), eq(magnetometer));
    }

    @Test
    public void resumed_enabled_suspended_subscribesAndThenUnsubscribes() {
        appLifecycleDelegationSuspendable.resume();
        compass.setEnabled(true);
        appLifecycleDelegationSuspendable.suspend();

        InOrder inOrder = Mockito.inOrder(sensorManager);

        inOrder.verify(sensorManager).registerListener(any(), eq(magnetometer), anyInt());
        inOrder.verify(sensorManager).unregisterListener(any(), eq(magnetometer));
    }
}
