package ru.yandex.general.webdriver;

import lombok.Getter;
import org.junit.rules.ExternalResource;
import ru.auto.tests.commons.webdriver.WebDriverManager;

import javax.inject.Inject;

public class WebDriverResource extends ExternalResource {

    @Inject
    @Getter
    private WebDriverManager driverManager;

    protected void before() throws Throwable {
        getDriverManager().startDriver();
    }

    protected void after() {
        getDriverManager().stopDriver();
    }

}
