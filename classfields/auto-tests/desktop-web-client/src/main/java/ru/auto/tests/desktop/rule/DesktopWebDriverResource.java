package ru.auto.tests.desktop.rule;

import org.junit.rules.ExternalResource;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.CapabilityType;
import ru.auto.tests.commons.webdriver.WebDriverManager;
import ru.auto.tests.desktop.utils.AntiAdBlockUtil;

import javax.inject.Inject;
import java.util.logging.Level;

import static java.lang.String.format;

public class DesktopWebDriverResource extends ExternalResource {

    @Inject
    private WebDriverManager driverManager;

    protected void before() throws Throwable {
        driverManager.updateChromeOptions(chromeOptions -> {
            chromeOptions.addArguments(format("user-agent=%s", AntiAdBlockUtil.USER_AGENT));
            chromeOptions.addArguments("--no-sandbox");
            chromeOptions.addArguments("--disable-infobars");
            chromeOptions.addArguments("--disable-dev-shm-usage");
            chromeOptions.addArguments("--disable-browser-side-navigation");
            chromeOptions.addArguments("--disable-gpu");
            chromeOptions.addArguments("--disable-features=VizDisplayCompositor");
            chromeOptions.addArguments("--hide-scrollbars");
            chromeOptions.addArguments("--disable-web-security");
            chromeOptions.addArguments("--disable-site-isolation-trials");
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
    }

    protected void after() {
        driverManager.stopDriver();
    }
}
