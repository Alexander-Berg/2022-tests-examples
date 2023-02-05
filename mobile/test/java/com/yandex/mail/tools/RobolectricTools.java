package com.yandex.mail.tools;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.view.View;

import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowNetworkInfo;

import java.lang.Thread.UncaughtExceptionHandler;

import androidx.annotation.NonNull;
import io.reactivex.android.plugins.RxAndroidPlugins;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.robolectric.Robolectric.flushBackgroundThreadScheduler;
import static org.robolectric.Shadows.shadowOf;

public class RobolectricTools {

    private static final NetworkInfo connected = ShadowNetworkInfo.newInstance(
            NetworkInfo.DetailedState.CONNECTED,
            ConnectivityManager.TYPE_MOBILE,
            TelephonyManager.NETWORK_TYPE_EDGE,
            true,
            true
    );

    private static final NetworkInfo disconnected = ShadowNetworkInfo.newInstance(
            NetworkInfo.DetailedState.DISCONNECTED,
            ConnectivityManager.TYPE_MOBILE,
            TelephonyManager.NETWORK_TYPE_EDGE,
            true,
            false
    );

    private RobolectricTools() { }

    public static ConnectivityManager getConnectivityManager() {
        return (ConnectivityManager) RuntimeEnvironment.application.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public static void turnNetworkOn() {
        shadowOf(getConnectivityManager()).setActiveNetworkInfo(connected);
    }

    public static void turnNetworkOff() {
        shadowOf(getConnectivityManager()).setActiveNetworkInfo(disconnected);
    }

    public static void incrementTime() {
        SystemClock.setCurrentTimeMillis(SystemClock.currentThreadTimeMillis() + 1);
    }

    public static void clearAllStartedActivities() {
        while (TestContext.shapp.getNextStartedActivity() != null) {
            //noinspection StatementWithEmptyBody
        }
    }

    public static void callOnPreDraw(@NonNull View view) {
        view.getViewTreeObserver().dispatchOnPreDraw();
    }

    public static void executeWithPausedBackgroundScheduler(@NonNull Runnable runnable) {
        Robolectric.getBackgroundThreadScheduler().pause();
        runnable.run();
        Robolectric.getBackgroundThreadScheduler().unPause();
        flushBackgroundThreadScheduler();
    }

    public static void executeWithPausedMainLooper(@NonNull Runnable runnable) {
        ShadowLooper.pauseMainLooper();
        runnable.run();
        ShadowLooper.unPauseMainLooper();
    }

    public static void enableFakeRxScheduler() {
        RxJavaPlugins.setIoSchedulerHandler(scheduler -> Schedulers.trampoline());
    }

    public static void setRxUncaughtExceptionHandler() {
        RxJavaPlugins.setErrorHandler(e -> {
            UncaughtExceptionHandler saved = Thread.currentThread().getUncaughtExceptionHandler();
            Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
                saved.uncaughtException(thread, throwable);
                throw (InternalError) throwable;
            });
            throw new InternalError(e);
        });
    }

    public static void resetFakeRxScheduler() {
        RxJavaPlugins.reset();
        RxAndroidPlugins.reset();
    }

    public static void assertSharedPrefNotEmpty(@NonNull String name) {
        SharedPreferences preferences = RuntimeEnvironment.application.getSharedPreferences(name, Context.MODE_PRIVATE);
        assertThat(preferences.getAll()).isNotEmpty();
    }
}
