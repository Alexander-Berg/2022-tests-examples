package ru.yandex.realty.webdriver;

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
            chromeOptions.addArguments("user-agent=Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.5005.99 Mobile Safari/537.36");
            chromeOptions.addArguments("--no-sandbox");
            chromeOptions.addArguments("--disable-infobars");
            chromeOptions.addArguments("--disable-dev-shm-usage");
            chromeOptions.addArguments("--disable-browser-side-navigation");
            chromeOptions.addArguments("--disable-gpu");
            chromeOptions.addArguments("--disable-features=VizDisplayCompositor");
        });
        getDriverManager().startDriver();
        setWindowSize(375, 812);
    }

    @Step("Устанавливаем размеры окна на «{width}x{height}»")
    private void setWindowSize(int width, int height) {
        driverManager.getDriver().manage().window().setSize(new Dimension(width, height));
    }

    protected void after() {
        getDriverManager().stopDriver();
    }

}
