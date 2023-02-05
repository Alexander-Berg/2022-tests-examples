package com.yandex.launcher.theme;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import com.yandex.launcher.common.app.AndroidHandler;
import com.yandex.launcher.BaseRobolectricTest;
import com.yandex.launcher.TestPackageManager;
import com.yandex.launcher.TestResources;
import com.yandex.launcher.app.TestApplication;
import com.yandex.launcher.di.ApplicationModule;
import com.yandex.launcher.di.Injector;
import com.yandex.launcher.themes.ThemeInfo;
import com.yandex.launcher.themes.ThemesProvider;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowPackageManager;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.robolectric.Shadows.shadowOf;

@Config(sdk = 21, application = TestApplication.class, packageName = "com.yandex.launcher",
        shadows = { TestPackageManager.class })
public class ThemesProviderTest extends BaseRobolectricTest {

    private ThemesProvider themesProvider;
    private ShadowPackageManager packageManager;
    private Set<TestResources> themeResources = new HashSet<>();

    public ThemesProviderTest() {
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Injector.setModule(new TestModule());

        packageManager =
                shadowOf(getAppContext().getPackageManager());
        initExternalThemes(getAppContext(), packageManager);

        //themesProvider = new ThemesProvider(getAppContext());
    }

    @After
    public void tearDown() {
        for (TestResources resources : themeResources) {
            resources.close();
        }
    }

    @Test
    public void stub() {}

        /*
    @Test
    public void getValidThemeInfoSeparately() {
        final ExternalThemeInfo redThemeInfo = (ExternalThemeInfo)
                themesProvider.getThemeInfoById("com.yandex.launcher.externaltheme.red");
        Assert.assertNotNull(redThemeInfo);
        Assert.assertNull(redThemeInfo.getParentId());
        Assert.assertEquals(redThemeInfo.getTitle(), "Red Theme");
        Assert.assertEquals(redThemeInfo.getDescription(), "Red Description");
        Assert.assertEquals(redThemeInfo.getSortOrderId(), 2);
        Assert.assertEquals(redThemeInfo.getVersion(), "1.0.0");

        final ThemeColors redThemeAccent = redThemeInfo.getThemeColors();
        Assert.assertNotNull(redThemeAccent);
        Assert.assertEquals(redThemeAccent.baseColor, Color.parseColor("#ff9d0b0e"));
        Assert.assertEquals(redThemeAccent.accentColor, Color.parseColor("#ffffde01"));
        Assert.assertEquals(redThemeAccent.accentBgColor, Color.parseColor("#ffef8e00"));

        final ExternalThemeInfo builtinThemeInfo = (ExternalThemeInfo)
                themesProvider.getThemeInfoById("com.yandex.launcher.externaltheme.builtin");
        Assert.assertNotNull(builtinThemeInfo);
        Assert.assertNull(builtinThemeInfo.getThemeColors());
        Assert.assertEquals(builtinThemeInfo.getParentId(), "light");

        final ExternalThemeInfo greenThemeInfo = (ExternalThemeInfo)
                themesProvider.getThemeInfoById("com.yandex.launcher.externaltheme.green");
        Assert.assertNotNull(greenThemeInfo);
        Assert.assertEquals(greenThemeInfo.getParentId(), "com.yandex.launcher.externaltheme.red");
    }

    @Test
    public void overrideThemeColor() {
        final ExternalTheme theme = (ExternalTheme) themesProvider.getTheme("com.yandex.launcher.externaltheme.green");
        Assert.assertNotNull(theme);
        Assert.assertEquals(theme.getThemeColor(ThemeColor.home_search_input_bg), Color.parseColor("#105c08"));
    }

    @Test
    public void getThemeInfoList() throws InterruptedException {

        themesProvider.loadData(new Callback<List<ThemeInfo>>() {
            @Override
            public void onDataLoaded(List<ThemeInfo> infoList) {
                Assert.assertEquals(infoList.size(), 6);

                // Builtin
                checkThemeInfoExist(infoList, "light");
                checkThemeInfoExist(infoList, "dark");
                checkThemeInfoExist(infoList, "colors");

                // External
                checkThemeInfoExist(infoList, "com.yandex.launcher.externaltheme.builtin");
                checkThemeInfoExist(infoList, "com.yandex.launcher.externaltheme.red");
                checkThemeInfoExist(infoList, "com.yandex.launcher.externaltheme.green");
            }

            @Override
            protected void release() {
            }
        });
    }
    */


    private static void checkThemeInfoExist(List<ThemeInfo> infoList, String themeId) {
        for (ThemeInfo info : infoList) {
            if (info.getId().equals(themeId)) {
                return;
            }
        }
        Assert.fail("Theme with id " + themeId + " not found");
    }


    private void initExternalThemes(Context context, ShadowPackageManager packageManager)
            throws IOException, XmlPullParserException {
        initExternalTheme("com.yandex.launcher.externaltheme.builtin", context, packageManager);
        initExternalTheme("com.yandex.launcher.externaltheme.red", context, packageManager);
        initExternalTheme("com.yandex.launcher.externaltheme.green", context, packageManager);

        initExternalTheme("com.yandex.launcher.externaltheme.invalid", context, packageManager);
        initExternalTheme("com.yandex.launcher.externaltheme.cyclic_1", context, packageManager);
        initExternalTheme("com.yandex.launcher.externaltheme.cyclic_2", context, packageManager);
        initExternalTheme("com.yandex.launcher.externaltheme.missed", context, packageManager);
    }

    private void initExternalTheme(String packageName, Context context, ShadowPackageManager packageManager)
            throws IOException, XmlPullParserException {

        final InputStream is = getResourceInputStream("themes/" + packageName + ".xml");
        final TestResources themeResources = new TestResources(context, is);

        final ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.descriptionRes = themeResources.getIdentifier("description", "string", null);

        final PackageInfo packageInfo = new PackageInfo();
        packageInfo.applicationInfo = applicationInfo;
        packageInfo.packageName = packageName;
        packageInfo.versionName = "1.0.0";

        final ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.packageName = packageName;

        final ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = activityInfo;
        resolveInfo.resolvePackageName = packageName;

        final Intent themeIntent = new Intent("com.yandex.launcher.THEME");

        packageManager.addPackage(packageInfo);
        packageManager.addResolveInfoForIntent(themeIntent, resolveInfo);
    }

    private class TestModule extends ApplicationModule {

        public TestModule() {
            super(getAppContext());
        }

        @Override
        public AndroidHandler provideAuxHandler() {
            return AndroidHandler.createOnMainThread();
        }
    }
}
