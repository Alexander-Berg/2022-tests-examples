package com.yandex.mail.testopithecus;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import java.util.Collection;

import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static androidx.test.runner.lifecycle.Stage.RESUMED;

public class ControlledActivityTestRule<T extends Activity> extends ActivityTestRule<T> {

    private static final String TAG = "ControlledActivityTestRule";

    public ControlledActivityTestRule(Class<T> activityClass) {
        super(activityClass, false);
    }

    public ControlledActivityTestRule(Class<T> activityClass, boolean initialTouchMode) {
        super(activityClass, initialTouchMode, true);
    }

    public ControlledActivityTestRule(Class<T> activityClass, boolean initialTouchMode, boolean launchActivity) {
        super(activityClass, initialTouchMode, launchActivity);
    }

    public void finish() {
        finishActivity();
    }

    public void finishAll() {
        if (getFaceActivity() != null) {
            getFaceActivity().finish();
            Log.i(TAG, "Killed activity");
        }
        finishActivity();
        Log.i(TAG, "Finished traced activity");
    }

    public void relaunchActivity() {
        finishAll();
        sleep(5);
        launchActivity();
        Log.i(TAG, "Launched activity");
        sleep(5);
    }

    public void launchActivity() {
        launchActivity(getActivityIntent());
    }

    public void sleep(int seconds) {
        if (seconds > 0) {
            try {
                Thread.sleep(seconds * 1000);
            } catch (Exception ex) {
                Log.i(TAG, "Can't sleep");
            }
        }
    }

    public static Activity getFaceActivity() {
        final Activity[] currentActivity = {null};
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                Collection resumedActivities = ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(RESUMED);
                if (resumedActivities.iterator().hasNext()) {
                    currentActivity[0] = (Activity) resumedActivities.iterator().next();
                }
            }
        });
        return currentActivity[0];
    }

    @Override
    protected Intent getActivityIntent() {
        return new Intent(Intent.ACTION_MAIN).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
    }
}
