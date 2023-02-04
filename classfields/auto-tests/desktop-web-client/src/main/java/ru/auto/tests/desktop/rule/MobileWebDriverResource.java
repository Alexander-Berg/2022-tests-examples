package ru.auto.tests.desktop.rule;

import org.junit.rules.ExternalResource;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.CapabilityType;
import ru.auto.tests.commons.webdriver.WebDriverManager;
import ru.auto.tests.desktop.step.ScreenshotSteps;

import javax.inject.Inject;

import java.util.logging.Level;

public class MobileWebDriverResource extends ExternalResource {

    @Inject
    private WebDriverManager driverManager;

    @Inject
    private ScreenshotSteps screenshotSteps;

    protected void before() throws Throwable {
        driverManager.updateChromeOptions(chromeOptions -> {
            chromeOptions.addArguments("user-agent=Mozilla/5.0 (iPhone; CPU iPhone OS 15_5 like Mac OS X) " +
                    "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.5 Mobile/15E148 Safari/604.1");
            chromeOptions.addArguments("--use-mobile-user-agent");
            chromeOptions.addArguments("--no-sandbox");
            chromeOptions.addArguments("--disable-infobars");
            chromeOptions.addArguments("--disable-dev-shm-usage");
            chromeOptions.addArguments("--disable-browser-side-navigation");
            chromeOptions.addArguments("--disable-gpu");
            chromeOptions.addArguments("--disable-features=VizDisplayCompositor");
            chromeOptions.addArguments("--hide-scrollbars");
            chromeOptions.setPageLoadStrategy(PageLoadStrategy.EAGER);
        });

        driverManager.updateCapabilities(capabilities -> {
            LoggingPreferences logs = new LoggingPreferences();
            logs.enable(LogType.BROWSER, Level.ALL);
            logs.enable(LogType.PERFORMANCE, Level.ALL);

            capabilities.setCapability(CapabilityType.LOGGING_PREFS, logs);
            capabilities.setCapability("goog:loggingPrefs", logs);
        });

        driverManager.startDriver();
        screenshotSteps.setWindowSize(375, 812);
    }

    protected void after() {
        driverManager.stopDriver();
    }
}
