package ru.yandex.navi.tf;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.MobileElement;
import io.appium.java_client.Setting;
import io.appium.java_client.android.Activity;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.GsmCallActions;
import io.appium.java_client.android.connection.ConnectionState;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import io.qameta.allure.Step;
import org.junit.Assert;
import org.openqa.selenium.html5.Location;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AndroidUser extends MobileUser {
    private static final String YANDEXNAVI_PREFIX = "ru.yandex.yandexnavi";
    private static final String NAVI_MAIN_ACTIVITY = YANDEXNAVI_PREFIX + ".core.NavigatorActivity";
    private static final Duration APP_RESUME_DELAY = Duration.ofSeconds(3);
    private static final Location YANDEX = new Location(55.733969, 37.587093, 0.0);
    private String appPackage;
    private AndroidDriver<MobileElement> driver;

    @Override
    protected void doCreateDriver(boolean reset) {
        DesiredCapabilities caps = createCaps(userCaps);
        Location location = userCaps.initLocation;

        final String surfwaxQuota = getOptEnv("SURFWAX_QUOTA");
        final String udid = getOptEnv("UDID");
        final String avdName = getOptEnv("AVD_NAME");
        if (surfwaxQuota != null) {
            if (getPlatformVersion() == null)
                setPlatformVersion("8.0");
            driver = createSeleniumDriver(caps, getPlatformVersion(), surfwaxQuota);
            if (location == null)
                location = YANDEX;
        } else if (udid != null) {
            driver = createRemoteDriver(caps, udid);
        } else if (avdName != null) {
            detectPlatformVersionByAvdName(avdName);
            driver = createLocalDriver(caps, avdName);
        } else {
            throw new RuntimeException("Can't create AndroidDriver: invalid parameters");
        }

        driver.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);

        if (location != null)
            driver.setLocation(location);

        driver.setSetting(Setting.IMAGE_MATCH_THRESHOLD, 0.7);

        appPackage = driver.getCurrentPackage();
    }

    private static DesiredCapabilities createCaps(UserCapabilities userCaps) {
        // http://appium.io/docs/en/writing-running-appium/caps/
        // https://github.com/appium/java-client/blob/master/docs/The-starting-of-an-Android-app.md
        // https://github.com/appium/appium/blob/1.5/docs/en/writing-running-appium/caps.md#android-only
        //
        DesiredCapabilities caps = createDefaultCaps(userCaps);

        String app = getOptEnv("APP_PATH");
        final String appPackage = getOptEnv("APP_PACKAGE");
        if (app != null) {
            if (app.startsWith("https://teamcity"))  // TODO: remove
                app = app.replace("https://", "https://" + getEnv("TC_CREDENTIALS") + "@");
            caps.setCapability("app", app);
            caps.setCapability("fullReset", true);
        } else if (appPackage != null) {
            caps.setCapability("appPackage", appPackage);
            caps.setCapability("noReset", true);
        } else {
            throw new RuntimeException(
                "Nothing to run. Please provide env.APP_PATH or env.APP_PACKAGE.");
        }

        caps.setCapability("appActivity", NAVI_MAIN_ACTIVITY);
        caps.setCapability("autoGrantPermissions", userCaps.grantPermissions);
        caps.setCapability("automationName", getAutomationName(userCaps.platformVersion));
        caps.setCapability("clearDeviceLogsOnStart", true);
        caps.setCapability("deviceName", "Android Emulator");
        caps.setCapability("gpsEnabled", true);
        caps.setCapability("optionalIntentArguments", "--ez autotest true");
        caps.setCapability("platformName", "Android");
        caps.setCapability("adbExecTimeout", "60000");
        caps.setCapability("allowTestPackages", true);

        return caps;
    }

    private static String getAutomationName(String platformVersion) {
        if (platformVersion != null && platformVersion.startsWith("4."))
            return "UiAutomator1";
        return "UiAutomator2";
    }

    private static AndroidDriver<MobileElement> createLocalDriver(
            DesiredCapabilities caps, String avdName) {

        caps.setCapability("avd", avdName);
        caps.setCapability("avdArgs", getOptEnv("AVD_ARGS"));

        // To prevent [INSTALL_FAILED_INSUFFICIENT_STORAGE] Android bug
        caps.setCapability("androidInstallPath", "/sdcard/");

        // Setting "--allow-insecure=adb_shell" allows to run adb shell commands with Appium:
        return new AndroidDriver<>(
                new AppiumServiceBuilder().withArgument(() -> "--allow-insecure", "adb_shell"),
                caps);
    }

    private static AndroidDriver<MobileElement> createRemoteDriver(
            DesiredCapabilities caps, String udid) {
        caps.setCapability("udid", udid);

        return new AndroidDriver<>(caps);
    }

    private static AndroidDriver<MobileElement> createSeleniumDriver(
            DesiredCapabilities caps, String version, String surfwaxQuota) {
        // https://github.com/SeleniumHQ/selenium/wiki/DesiredCapabilities
        //
        caps.setCapability("browserName", "android-phone");
        caps.setCapability("enableVNC", true);
        caps.setCapability("locationContextEnabled", true);
        caps.setCapability("version", version + "-" + surfwaxQuota);
        caps.setCapability("selenoid:options", new HashMap<String, String>() {{
            put("sessionTimeout", "5m");  // Cold start of session in SurfWax can be slow
        }});

        try {
            URL url = new URL("http://" + surfwaxQuota + "@sw.yandex-team.ru:80/v0");
            return new AndroidDriver<>(url, caps);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private void detectPlatformVersionByAvdName(String avdName) {
        if (getPlatformVersion() != null)
            return;

        // https://source.android.com/setup/start/build-numbers
        //
        final String[] platformVersionByApiLevel = {
                null, "1.0", "1.1", "1.5", "1.6", "2.0", "2.0", "2.1", "2.2", "2.3",
                "2.3", "3.0", "3.1", "3.2", "4.0", "4.0", "4.1", "4.2", "4.3", "4.4",
                null, "5.0", "5.1", "6.0", "7.0", "7.1", "8.0", "8.1", "9.0", "10.0",
        };

        final int pos = avdName.indexOf("_API_");
        if (pos < 0)
            return;

        try {
            final int apiLevel = Integer.parseInt(avdName.substring(pos + 5));
            if (apiLevel > 0 && apiLevel < platformVersionByApiLevel.length)
                setPlatformVersion(platformVersionByApiLevel[apiLevel]);
        }
        catch (NumberFormatException ignored) {
            System.err.println("Can't detect platformVersion for AVD: " + avdName);
        }
    }

    @Override
    protected String getBundleId() {
        return appPackage;
    }

    @Override
    protected String getClipboard() {
        return driver.getClipboardText();
    }

    @Step("Переключить режим 'В самолете' -> {value}")
    @Override
    public void setAirplaneMode(boolean value) {
        final long connectionState;
        if (value)
            connectionState = ConnectionState.AIRPLANE_MODE_MASK;
        else
            connectionState = ConnectionState.DATA_MASK + ConnectionState.WIFI_MASK;
        driver.setConnection(new ConnectionState(connectionState));
    }

    @Step("Switch the state of the location service")
    @Override
    public void toggleLocationServices() {
        driver.toggleLocationServices();
    }

    @Override
    public void makePhoneCall() {
        final String PHONE_YANDEX = "84957397000";
        driver.makeGsmCall(PHONE_YANDEX, GsmCallActions.CALL);
        waitFor(Duration.ofSeconds(1));
        driver.makeGsmCall(PHONE_YANDEX, GsmCallActions.ACCEPT);
        waitFor(Duration.ofSeconds(1));
        driver.makeGsmCall(PHONE_YANDEX, GsmCallActions.CANCEL);
    }

    @Override
    public final void doRestartApp() {
        startsActivity(true, null);
        waitFor(APP_RESUME_DELAY);
    }

    @Step("Restarts activity")
    @Override
    public final void restartActivity() {
        final String FLAG_ACTIVITY_CLEAR_TASK = "0x00008000";
        startsActivity(false, FLAG_ACTIVITY_CLEAR_TASK);
    }

    @Override
    public List<MobileElement> findElementsBy(ByText byText, String value) {
        // https://developer.android.com/reference/androidx/test/uiautomator/UiSelector
        final String selector;
        if (byText == ByText.Regex && value.startsWith("^"))
            selector = String.format(".textStartsWith(\"%s\")", value.substring(1));
        else if (byText == ByText.Regex && value.endsWith("$"))
            selector = String.format(".textMatches(\"%s\")", value);
        else
            selector = String.format(".text(\"%s\")", value);
        return driver.findElementsByAndroidUIAutomator(selector);
    }

    @Override
    @Step("Presses HOME button")
    public void pressesHomeButton() {
        pressKey(AndroidKey.HOME);
    }

    private void startsActivity(boolean stopApp, String intentFlags) {
        Activity activity = new Activity(appPackage, NAVI_MAIN_ACTIVITY);
        activity.setStopApp(stopApp);
        activity.setIntentFlags(intentFlags);
        driver.startActivity(activity);
    }

    @Step("Should not see background guidance")
    public void shouldNotSeeBackgroundGuidance() {
        openNotifications();
        final boolean isDisplayedNavi = !findElementsByText("Яндекс.Навигатор").isEmpty();
        Assert.assertFalse("Background guidance state is wrong", isDisplayedNavi);
        pressesBackButton();
        closeNotifications();
    }

    @Step("Нажать кнопку BACK")
    @Override
    public void pressesBackButton() {
        pressKey(AndroidKey.BACK);
    }

    @Override
    public void navigateBack() {
        pressesBackButton();
    }

    @Override
    public AppiumDriver<MobileElement> getDriver() {
        return driver;
    }

    private void pressKey(AndroidKey key) {
        KeyEvent keyEvent = new KeyEvent().withKey(key);
        driver.pressKey(keyEvent);
    }

    @Step("Opens notification tray")
    private void openNotifications() {
        driver.openNotifications();
    }

    @Step("Closes notification tray")
    private void closeNotifications() {
        driver.pressKey(new KeyEvent(AndroidKey.BACK));
    }
}
