package ru.auto.tests.desktop.rule;

import org.junit.rules.ExternalResource;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.CapabilityType;
import ru.auto.tests.commons.webdriver.WebDriverManager;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class MobileEmulationWebDriverResource extends ExternalResource {

    @Inject
    private WebDriverManager driverManager;

    protected void before() throws Throwable {
        driverManager.updateChromeOptions(chromeOptions -> {
            chromeOptions.setPageLoadStrategy(PageLoadStrategy.EAGER);

            Map<String, Object> mobileEmulation = new HashMap<>();
            mobileEmulation.put("deviceName", "Nexus 5");

            chromeOptions.setExperimentalOption("mobileEmulation", mobileEmulation);
        });

        driverManager.updateCapabilities(capabilities -> {
            LoggingPreferences logs = new LoggingPreferences();
            logs.enable(LogType.BROWSER, Level.ALL);

            capabilities.setCapability(CapabilityType.LOGGING_PREFS, logs);
            capabilities.setCapability("goog:loggingPrefs", logs);
        });

        driverManager.startDriver();
    }

    protected void after() {
        driverManager.stopDriver();
    }

}
