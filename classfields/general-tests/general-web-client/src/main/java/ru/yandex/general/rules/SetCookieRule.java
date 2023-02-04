package ru.yandex.general.rules;

import com.google.inject.Inject;
import io.qameta.allure.Step;
import lombok.Getter;
import org.junit.rules.ExternalResource;
import org.openqa.selenium.WebDriverException;
import ru.auto.tests.commons.webdriver.WebDriverManager;
import ru.auto.tests.commons.webdriver.WebDriverSteps;
import ru.yandex.general.config.GeneralWebConfig;

public class SetCookieRule extends ExternalResource {

    private static final String NO_EXP_COOKIE = "%7B%22stringMap%22%3A%7B%7D%2C%22stringListMap%22%3A%7B%7D%2C%22" +
            "intMap%22%3A%7B%7D%2C%22floatMap%22%3A%7B%7D%2C%22booleanMap%22%3A%7B%22without_exp%22%3Atrue%7D%7D";

    @Inject
    @Getter
    private WebDriverManager driverManager;

    @Inject
    private WebDriverSteps webDriverSteps;

    @Inject
    private GeneralWebConfig config;

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
        getDriverManager().getDriver().get(config.getTestingURI().toString());
        webDriverSteps.setCookie("autotest", "1", config.getBaseDomain());
        webDriverSteps.setCookie("exp_flags", NO_EXP_COOKIE, config.getBaseDomain());
        webDriverSteps.setCookie("classified_add_offer_safe_message", "true", config.getBaseDomain());
        getDriverManager().getDriver().navigate().refresh();
    }

}
