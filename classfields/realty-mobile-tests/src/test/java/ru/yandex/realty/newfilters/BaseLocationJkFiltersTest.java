package ru.yandex.realty.newfilters;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.UrlSteps;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.matchers.AttributeMatcher.hasHref;
import static ru.yandex.realty.step.CommonSteps.FIRST;


@DisplayName("Базовые фильтры. ЖК")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class BaseLocationJkFiltersTest {

    private static final String TEST_JK = "Апарт-отель VALO";
    private static final String SITE_ID = "siteId";
    private static final String TEST_JK_VALUE = "395435";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(KURAU)
    @DisplayName("Выбираем ЖК")
    public void shouldChooseOneJk() {
        urlSteps.testing().path(SPB_I_LO).open();
        basePageSteps.onMobileMainPage().searchFilters().waitUntil(isDisplayed());
        basePageSteps.onMobileMainPage().searchFilters().metroAndStreet().click();
        basePageSteps.onMobileMainPage().searchFilters().filterPopup().input().sendKeys(TEST_JK);
        basePageSteps.onMobileMainPage().searchFilters().filterPopup().item(TEST_JK).click();
        basePageSteps.onMobileMainPage().searchFilters().filterPopup().showButton().click();

        urlSteps.path(KUPIT).path(KVARTIRA).path(format("zhk-valo-%s/", TEST_JK_VALUE))
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Переходим по ссылке поиска по ЖК")
    public void shouldSeeJk() {
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(NOVOSTROJKA).queryParam(SITE_ID, TEST_JK_VALUE)
                .open();
        basePageSteps.onNewBuildingPage().site(FIRST).title().should(hasText(containsString("апарт-отель VALO")));
        basePageSteps.onNewBuildingPage().site(FIRST).link().should(hasHref(equalTo(
                urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(NOVOSTROJKA)
                        .path(format("valo-%s/", TEST_JK_VALUE)).toString())));
    }
}
