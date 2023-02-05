package ru.yandex.navi.tf;

import com.google.common.collect.ImmutableMap;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.MobileElement;
import io.appium.java_client.ios.IOSDriver;
import io.qameta.allure.Step;
import org.openqa.selenium.json.Json;
import org.openqa.selenium.remote.DesiredCapabilities;
import ru.yandex.navi.GeoPoint;
import ru.yandex.navi.ui.Dialog;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class IosUser extends MobileUser {
    private IOSDriver<MobileElement> driver;
    private String bundleId;

    @Override
    public void doRestartApp() {
        driver.quit();
        //noinspection AssignmentToNull
        driver = null;  // to prevent calls of driver from createDriver
        createDriver(false);
    }

    @Step("Restarts activity")
    @Override
    public void restartActivity() {
        final Duration timeout = Duration.ofSeconds(1);
        driver.runAppInBackground(timeout);
        waitFor(timeout);
    }

    @Override
    public List<MobileElement> findElementsBy(ByText byText, String value) {
        if (byText == ByText.Regex && value.startsWith("^"))
            return driver.findElementsByIosClassChain(String.format(
                    "**/XCUIElementTypeStaticText[`name BEGINSWITH \"%s\"`]", value.substring(1)));

        return driver.findElementsByAccessibilityId(value);
    }

    @Override
    protected void doCreateDriver(boolean reset) {
        // Capabilities: https://github.com/appium/appium-xcuitest-driver
        DesiredCapabilities caps = createDefaultCaps(userCaps);
        caps.setCapability("app", getEnv("APP_PATH"));
        caps.setCapability("automationName", "XCUITest");
        caps.setCapability("deviceName", "iPhone 8 Plus");
        caps.setCapability("iosInstallPause", 10000);
        caps.setCapability("noReset", !reset);
        caps.setCapability("platformName", "iOS");
        caps.setCapability("platformVersion", getPlatformVersion());
        caps.setCapability("processArguments",
            new Json().toJson(ImmutableMap.of("args", new String[]{"-autotest"})));
        caps.setCapability("screenshotQuality", 2);
        caps.setCapability("wdaLaunchTimeout", 120000);
        caps.setCapability("wdaStartupRetries", 4);
        caps.setCapability("wdaStartupRetryInterval", 20000);

        if (userCaps.grantPermissions)
            grantPermissions(caps);

        driver = new IOSDriver<>(caps);
        driver.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);

        driver.setLocation(Optional.ofNullable(userCaps.initLocation).orElse(GeoPoint.YANDEX));
    }

    private void grantPermissions(DesiredCapabilities caps) {
        final Map<String, Object> permissions = ImmutableMap.of(
                getBundleId(), ImmutableMap.of(
                        "location", "always",
                        "microphone", "YES",
                        "notifications", "YES"));

        caps.setCapability("permissions", new Json().toJson(permissions));
    }

    @Override
    protected String getBundleId() {
        if (bundleId == null) {
            if (driver != null) {
                bundleId = (String) driver.getSessionDetail("CFBundleIdentifier");
            } else {
                bundleId = getEnv("BUNDLE_ID", "ru.yandex.mobile.navigator.inhouse");
            }
            assert bundleId != null;
        }
        return bundleId;
    }

    @Override
    protected String getClipboard() {
        throw notImplemented();
    }

    @Override
    protected void doOpenUrl(String url) {
        super.doOpenUrl(url);

        Dialog dialog = new Dialog("Открыть в программе «Навигатор»?");
        if (dialog.isDisplayed())
            dialog.clickAt("Открыть");
    }

    private NoRetryException notImplemented() {
        return new NoRetryException("Not implemented");
    }

    @Override
    public void quit() {
        driver.executeScript("mobile:clearKeychains");
        super.quit();
    }

    @Override
    @Step("Presses HOME button")
    public void pressesHomeButton() {
        driver.executeScript("mobile:pressButton", ImmutableMap.of("name", "home"));
    }

    @Override
    public void pressesBackButton() {
        throw notImplemented();
    }

    @Step("Presses '<' on NavigationBar")
    @Override
    public void navigateBack() {
        MobileElement element = driver.findElementByAccessibilityId("CommonBackArrow");
        element.click();
    }

    @Override
    public void shouldNotSeeBackgroundGuidance() {
        throw notImplemented();
    }

    @Override
    public void makePhoneCall() {
        throw notImplemented();
    }

    @Override
    public AppiumDriver<MobileElement> getDriver() {
        return driver;
    }

    @Override
    public void setAirplaneMode(boolean value) {
        throw notImplemented();
    }

    @Override
    public void toggleLocationServices() {
        throw notImplemented();
    }
}
