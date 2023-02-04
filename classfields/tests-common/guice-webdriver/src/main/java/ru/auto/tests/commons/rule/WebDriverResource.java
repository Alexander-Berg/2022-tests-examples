package ru.auto.tests.commons.rule;

import lombok.Getter;
import org.junit.rules.ExternalResource;
import ru.auto.tests.commons.webdriver.WebDriverManager;

import javax.inject.Inject;

/**
 * @author Artem Eroshenko <erosenkoam@me.com>
 */
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
