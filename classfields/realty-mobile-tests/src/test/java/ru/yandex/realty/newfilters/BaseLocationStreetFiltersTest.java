package ru.yandex.realty.newfilters;

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
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.mobile.page.MainPage.DOM_OPTION;
import static ru.yandex.realty.mobile.page.MainPage.KVARTIRU_OPTION;

@DisplayName("Базовые фильтры. Улицы")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class BaseLocationStreetFiltersTest {

    private final static String STREET = "улица Новый Арбат";
    private final static String STREET_PATH = "/st-ulica-novyj-arbat-30296/";

    private final static String NOT_CHPU_STREET = "Даниловский переулок";
    private final static String UNIFIED_ADDRESS = "Россия, Москва, Даниловский переулок";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onMobileMainPage().searchFilters().waitUntil(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим ЧПУ улицу")
    public void shouldSeeStreetChpuInUrl() {
        basePageSteps.onMobileMainPage().searchFilters().metroAndStreet().click();
        basePageSteps.onMobileMainPage().searchFilters().filterPopup().input().sendKeys(STREET);
        basePageSteps.onMobileMainPage().searchFilters().filterPopup().item(STREET).click();
        basePageSteps.onMobileMainPage().searchFilters().filterPopup().showButton().click();

        urlSteps.path(KUPIT).path(KVARTIRA).path(STREET_PATH).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("не видим ЧПУ улицу")
    public void shouldNotSeeStreetChpuInUrl() {
        basePageSteps.onMobileMainPage().searchFilters().metroAndStreet().click();
        basePageSteps.onMobileMainPage().searchFilters().filterPopup().input().sendKeys(NOT_CHPU_STREET);
        basePageSteps.onMobileMainPage().searchFilters().filterPopup().item(NOT_CHPU_STREET).click();
        basePageSteps.onMobileMainPage().searchFilters().filterPopup().backArrow().click();
        basePageSteps.onMobileMainPage().searchFilters().metroAndStreet().should(hasText(NOT_CHPU_STREET));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим ЧПУ улицу в новостройках")
    public void shouldSeeNewBuildingStreetChpuInUrl() {
        basePageSteps.onMobileMainPage().searchFilters().newFlat().button("Новостройки").click();
        basePageSteps.onMobileMainPage().searchFilters().metroAndStreet().click();
        basePageSteps.onMobileMainPage().searchFilters().filterPopup().input().sendKeys(STREET);
        basePageSteps.onMobileMainPage().searchFilters().filterPopup().item(STREET).click();
        basePageSteps.onMobileMainPage().searchFilters().filterPopup().showButton().click();

        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(NOVOSTROJKA).path(STREET_PATH)
                .queryParam(UrlSteps.NEW_FLAT_URL_PARAM, UrlSteps.YES_VALUE ).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим ЧПУ улицу в коттеджных поселках")
    public void shouldNotSeeVillagesStreetChpuInUrl() {
        basePageSteps.onMobileMainPage().searchFilters().waitUntil(isDisplayed());
        basePageSteps.selectOption(basePageSteps.onMobileMainPage().searchFilters().selector(KVARTIRU_OPTION),
                DOM_OPTION);
        basePageSteps.onMobileMainPage().searchFilters().button("Коттеджный пос.").click();
        basePageSteps.onMobileMainPage().searchFilters().metroAndStreet().click();
        basePageSteps.onMobileMainPage().searchFilters().filterPopup().input().sendKeys(STREET);
        basePageSteps.onMobileMainPage().searchFilters().filterPopup().item(STREET).click();
        basePageSteps.onMobileMainPage().searchFilters().filterPopup().backArrow().click();
        basePageSteps.onMobileMainPage().searchFilters().metroAndStreet().should(hasText(STREET));
    }
}
