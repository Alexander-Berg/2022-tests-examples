package com.yandex.launcher.statistics;

import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.yandex.launcher.BaseRobolectricTest;
import com.yandex.launcher.metrica.AppVersionManager;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowPackageManager;


public class AppVersionManagerTest extends BaseRobolectricTest {

    private static final String LAUNCHER_APP_VERSION_CODE_KEY = "LAUNCHER_APP_VERSION_CODE_KEY";

    public AppVersionManagerTest() throws NoSuchFieldException, IllegalAccessException {
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void sameCodes() {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.versionCode = 0;
        packageInfo.versionName = "testVersion";
        packageInfo.packageName = getAppContext().getPackageName();

        ShadowPackageManager packageManager = shadowOf(getAppContext().getPackageManager());

        packageManager.addPackage(packageInfo);

        SharedPreferences prefs = getAppContext().getSharedPreferences(AppVersionManager.PREFERENCES, Context.MODE_PRIVATE);

        prefs.edit().putInt(LAUNCHER_APP_VERSION_CODE_KEY, 0).commit();

        Assert.assertFalse(AppVersionManager.updateVersionInPreferences(getAppContext()));
    }

    @Test
    public void savedCodeLess() {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.versionCode = 1;
        packageInfo.versionName = "testVersion";
        packageInfo.packageName = getAppContext().getPackageName();

        ShadowPackageManager packageManager = shadowOf(getAppContext().getPackageManager());

        packageManager.addPackage(packageInfo);

        SharedPreferences prefs = getAppContext().getSharedPreferences(AppVersionManager.PREFERENCES, Context.MODE_PRIVATE);

        prefs.edit().putInt(LAUNCHER_APP_VERSION_CODE_KEY, 0).commit();

        Assert.assertTrue(AppVersionManager.updateVersionInPreferences(getAppContext()));
    }

    @Test
    public void savedCodeMore() {
        PackageInfo packageInfo = new PackageInfo();

        packageInfo.versionCode = -1;
        packageInfo.versionName = "testVersion";
        packageInfo.packageName = getAppContext().getPackageName();

        ShadowPackageManager packageManager = shadowOf(getAppContext().getPackageManager());

        packageManager.addPackage(packageInfo);

        SharedPreferences prefs = getAppContext().getSharedPreferences(AppVersionManager.PREFERENCES, Context.MODE_PRIVATE);

        prefs.edit().putInt(LAUNCHER_APP_VERSION_CODE_KEY, 0).commit();

        Assert.assertFalse(AppVersionManager.updateVersionInPreferences(getAppContext()));
    }

}
