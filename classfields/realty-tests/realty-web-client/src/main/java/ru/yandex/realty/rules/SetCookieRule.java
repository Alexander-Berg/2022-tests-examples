package ru.yandex.realty.rules;

import com.google.inject.Inject;
import io.qameta.allure.Step;
import lombok.Getter;
import org.junit.rules.ExternalResource;
import org.openqa.selenium.WebDriverException;
import ru.auto.tests.commons.webdriver.WebDriverManager;
import ru.auto.tests.commons.webdriver.WebDriverSteps;
import ru.yandex.realty.config.RealtyWebConfig;

import java.util.Arrays;

import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_ADD;

/**
 * Created by vicdev on 23.08.17.
 * VERTISTEST-543
 */
public class SetCookieRule extends ExternalResource {

    private static final String COOKIE_DOMAIN = ".yandex.ru";

    @Inject
    @Getter
    private WebDriverManager driverManager;

    @Inject
    private WebDriverSteps webDriverSteps;

    @Inject
    private RealtyWebConfig conf;

    @Override
    protected void before() {
        try {
            setCookie();
        } catch (WebDriverException e) {
            webDriverSteps.refresh();
            setCookie();
        }
    }

    @Step("Выставляем куки")
    private void setCookie() {
        getDriverManager().getDriver().get(conf.getTestingURI().toString());
        webDriverSteps.setCookie("exp_flags", conf.getExperimentFlags(), COOKIE_DOMAIN);
        if (!conf.getCookies().isEmpty()) {
            Arrays.stream(conf.getCookies().split(";"))
                    .forEach(s -> webDriverSteps.setCookie(s.split("=")[0], s.split("=")[1], COOKIE_DOMAIN));
        }
        if (conf.isPrestable()) {
            webDriverSteps.setCookie("prestable", "1", COOKIE_DOMAIN);
        }
        if (conf.isDocker()) {
            webDriverSteps.setCookie("docker", "1", COOKIE_DOMAIN);
        }
        getDriverManager().getDriver().navigate().refresh();
    }
}
