package ru.auto.tests.desktop.rule;

import org.junit.rules.ExternalResource;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.CapabilityType;
import ru.auto.tests.commons.webdriver.WebDriverManager;
import ru.auto.tests.desktop.managers.DevToolsManager;
import ru.auto.tests.desktop.utils.AntiAdBlockUtil;

import javax.inject.Inject;
import java.util.logging.Level;

import static java.lang.String.format;

public class DevToolsResource extends ExternalResource {

    @Inject
    private DevToolsManager devToolsManager;

    @Override
    protected void before() throws Throwable {
        devToolsManager.startDevTools();
        devToolsManager.startRecordRequests();
    }

    @Override
    protected void after() {
        devToolsManager.stopRecordRequests();
    }
}
