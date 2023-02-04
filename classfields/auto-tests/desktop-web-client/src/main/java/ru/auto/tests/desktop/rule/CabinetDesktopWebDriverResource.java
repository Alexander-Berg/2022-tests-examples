package ru.auto.tests.desktop.rule;

import io.qameta.allure.Step;
import lombok.Getter;
import org.junit.rules.ExternalResource;
import org.openqa.selenium.Dimension;
import ru.auto.tests.commons.webdriver.WebDriverManager;
import ru.auto.tests.desktop.utils.AntiAdBlockUtil;

import javax.inject.Inject;

import static java.lang.String.format;

public class CabinetDesktopWebDriverResource extends ExternalResource {

    @Inject
    @Getter
    private WebDriverManager driverManager;

    protected void before() throws Throwable {
        driverManager.updateChromeOptions(chromeOptions -> {
            chromeOptions.addArguments(format("user-agent=%s", AntiAdBlockUtil.USER_AGENT));
        });
        getDriverManager().startDriver();
        setWindowSize(1280, 3000);
    }

    protected void after() {
        getDriverManager().stopDriver();
    }

    @Step("Устанавливаем размеры окна на {width}x{height}")
    private void setWindowSize(int width, int height) {
        driverManager.getDriver().manage().window().setSize(new Dimension(width, height));
    }
}