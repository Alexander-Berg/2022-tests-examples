package com.yandex.launcher.settings;

import android.content.Context;

import com.yandex.launcher.BaseRobolectricTest;
import com.yandex.launcher.preferences.Preference;
import com.yandex.launcher.preferences.PreferencesManager;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

public class LauncherPreferencesTest extends BaseRobolectricTest {

    public LauncherPreferencesTest() throws NoSuchFieldException, IllegalAccessException {
    }

    @Before
    public void init() {
        PreferencesManager.init(getContext());
    }

    @Test
    public void readWrite() {
        final long screedId = 3;
        PreferencesManager.put(Preference.DEFAULT_HOME_SCREEN_ID, screedId - 1);
        Assert.assertNotEquals(screedId, (long) PreferencesManager.getLong(Preference.DEFAULT_HOME_SCREEN_ID));
        PreferencesManager.put(Preference.DEFAULT_HOME_SCREEN_ID, screedId);
        Assert.assertEquals(screedId, (long) PreferencesManager.getLong(Preference.DEFAULT_HOME_SCREEN_ID));
    }

    private Context getContext() {
        return getAppContext().getApplicationContext();
    }
}
