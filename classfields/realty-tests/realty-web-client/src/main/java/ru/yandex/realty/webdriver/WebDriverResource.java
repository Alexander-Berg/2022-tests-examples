package ru.yandex.realty.webdriver;

import lombok.Getter;
import org.junit.rules.ExternalResource;
import ru.auto.tests.commons.webdriver.WebDriverManager;

import javax.inject.Inject;

public class WebDriverResource extends ExternalResource {

    @Inject
    @Getter
    private WebDriverManager driverManager;

    protected void before() throws Throwable {
        getDriverManager().updateChromeOptions(chromeOptions -> {
            chromeOptions.addArguments("--no-sandbox");
            chromeOptions.addArguments("--disable-infobars");
            chromeOptions.addArguments("--disable-dev-shm-usage");
            chromeOptions.addArguments("--disable-browser-side-navigation");
            chromeOptions.addArguments("--disable-gpu");
            chromeOptions.addArguments("--disable-features=VizDisplayCompositor");
        });
        getDriverManager().startDriver();
    }

    protected void after() {
        getDriverManager().stopDriver();
    }

}
