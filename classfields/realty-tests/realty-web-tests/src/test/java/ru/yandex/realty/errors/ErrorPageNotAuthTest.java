package ru.yandex.realty.errors;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Smoke;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.module.RealtyWebModuleWithoutServiceUnavailable;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAIN;
import static ru.yandex.realty.page.BasePage.PAGE_NOT_FOUND;
import static ru.yandex.realty.page.BasePage.SERVICE_UNAVAILABLE;

@DisplayName("Отображение ошибок")
@Feature(MAIN)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModuleWithoutServiceUnavailable.class)
public class ErrorPageNotAuthTest {

    private static final String WRONG_PATH = "erfefddd";
    private static final String WRONG_QUERY_NAME = "disable-api";
    private static final String WRONG_QUERY_VALUE = "region_info";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Smoke.class, Testing.class})
    @DisplayName("Переходим по ссылке, видим 404")
    public void shouldSee404() {
        urlSteps.testing().path(WRONG_PATH).open();
        basePageSteps.onBasePage().errorPage(PAGE_NOT_FOUND).should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class, Smoke.class, Testing.class})
    @DisplayName("Переходим по ссылке, видим 500")
    public void shouldSee500() {
        urlSteps.testing().path(MOSKVA).queryParam(WRONG_QUERY_NAME, WRONG_QUERY_VALUE).open();
        basePageSteps.onBasePage().errorPage(SERVICE_UNAVAILABLE).should(isDisplayed());
    }
}
