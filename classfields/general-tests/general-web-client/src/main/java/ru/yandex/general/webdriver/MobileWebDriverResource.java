package ru.yandex.general.webdriver;

import io.qameta.allure.Step;
import lombok.Getter;
import org.junit.rules.ExternalResource;
import org.openqa.selenium.Dimension;
import ru.auto.tests.commons.webdriver.WebDriverManager;

import javax.inject.Inject;

public class MobileWebDriverResource extends ExternalResource {

    @Inject
    @Getter
    private WebDriverManager driverManager;

    protected void before() throws Throwable {
        getDriverManager().updateChromeOptions(chromeOptions -> {
            chromeOptions.addArguments("user-agent=Mozilla/5.0 " +
                    "(iPhone; CPU iPhone OS 13_2_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) " +
                    "Version/13.0.3 Mobile/15E148 Safari/604.1");
        });
        getDriverManager().startDriver();
        setWindowSize(375, 812);
    }

    @Step("Устанавливаем размеры окна на {width}x{height}")
    private void setWindowSize(int width, int height) {
        driverManager.getDriver().manage().window().setSize(new Dimension(width, height));
    }

    protected void after() {
        getDriverManager().stopDriver();
    }

}
