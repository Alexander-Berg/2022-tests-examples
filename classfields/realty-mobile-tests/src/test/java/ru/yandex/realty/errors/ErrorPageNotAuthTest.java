package ru.yandex.realty.errors;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAIN;
import static ru.yandex.realty.page.BasePage.PAGE_NOT_FOUND;
import static ru.yandex.realty.page.BasePage.SERVICE_UNAVAILABLE;

@DisplayName("Отображение ошибок")
@Feature(MAIN)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class ErrorPageNotAuthTest {

    private static final String WRONG_QUERY_NAME = "disable-api";
    private static final String WRONG_QUERY_VALUE = "region_info";
    private static final String WRONG_PATH = "KVARTIRkA";
    private static final String OFFER_URL_404 = "https://m.realty.yandex.ru/offer/1";
    private static final String VILLAGE_URL_404 = "https://m.realty.test.vertis.yandex.ru/moskva/kupit/kottedzhnye-poselki/elizavetinskoe/?id=1852000&rgid=292840";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CompareSteps compareSteps;

    @Before
    public void before() {
        basePageSteps.clearCookies();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Переходим по ссылке, видим 404")
    public void shouldSee404() {
        urlSteps.open("https://m.realty.yandex.ru/moskva/kupit/kvartira/cardIndex=0");
        basePageSteps.refresh();
        basePageSteps.onBasePage().errorPage(PAGE_NOT_FOUND).should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Переходим на несуществующую карточку оффера, видим 404")
    public void shouldSee404Offer() {
        urlSteps.open(OFFER_URL_404);
        basePageSteps.refresh();
        basePageSteps.onBasePage().errorPage(PAGE_NOT_FOUND).should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Переходим на несуществующую карточку коттеджного поселка, видим 404")
    public void shouldSee404Village() {
        urlSteps.open(VILLAGE_URL_404);
        basePageSteps.refresh();
        basePageSteps.onBasePage().errorPage(PAGE_NOT_FOUND).should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Переходим по ссылке, видим 500")
    public void shouldSee500() {
        urlSteps.testing().path(MOSKVA).queryParam(WRONG_QUERY_NAME, WRONG_QUERY_VALUE).open();
        basePageSteps.onBasePage().errorPage(SERVICE_UNAVAILABLE).should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим скриншот 500")
    public void shouldSee500Screenshot() {
        urlSteps.testing().path(MOSKVA).queryParam(WRONG_QUERY_NAME, WRONG_QUERY_VALUE).open();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onBasePage().errorPage(SERVICE_UNAVAILABLE));
        urlSteps.setMobileProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onBasePage().errorPage(SERVICE_UNAVAILABLE));
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим скриншот 404")
    public void shouldSee404Screenshot() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(WRONG_PATH).open();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onBasePage().errorPage(PAGE_NOT_FOUND));
        urlSteps.setMobileProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onBasePage().errorPage(PAGE_NOT_FOUND));
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
