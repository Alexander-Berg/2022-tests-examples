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

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Filters.ODNOKOMNATNAYA;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.mobile.element.main.FilterPopup.METRO;
import static ru.yandex.realty.step.UrlSteps.NEW_FLAT_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.YES_VALUE;

@DisplayName("Фильтры поиска по объявлениям. ЧПУ тесты")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class ChpuBuyTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("2 параметра: Купить квартиру + вторичка + от собственника:")
    public void shouldSee2ParamsInUrl() {
        urlSteps.testing().path(SPB_I_LO).open();
        basePageSteps.onMobileMainPage().searchFilters().button("Вторичка").click();
        basePageSteps.onMobileMainPage().openExtFilter();
        basePageSteps.selectExtFilterElement("От собственников");
        basePageSteps.onMobileMainPage().extendFilters().applyFiltersButton().click();
        urlSteps.path(KUPIT).path(KVARTIRA).path("/vtorichniy-rynok-i-bez-posrednikov/")
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Комнатность + 2 параметра - Купить квартиру + однокомнатная + новостройки + рядом парк")
    public void shouldSee2ParamsWithRoomsInUrl() {
        urlSteps.testing().path(SPB_I_LO).open();
        basePageSteps.onMobileMainPage().searchFilters().button("1").click();
        basePageSteps.onMobileMainPage().searchFilters().button("Новостройки").click();
        basePageSteps.onMobileMainPage().openExtFilter();
        basePageSteps.selectExtFilterElement("Парк");
        basePageSteps.onMobileMainPage().extendFilters().applyFiltersButton().click();
        urlSteps.path(KUPIT).path(NOVOSTROJKA).path(ODNOKOMNATNAYA).queryParam("hasPark", YES_VALUE)
                .queryParam(NEW_FLAT_URL_PARAM, YES_VALUE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Комнатность + метро + 2 параметра: квартиру + однокомнатная + метро арбат + кухня 10м2 + с балконом")
    public void shouldSee2ParamsWithRoomsAndMetroInUrl() {
        urlSteps.testing().path(SANKT_PETERBURG).open();
        basePageSteps.onMobileMainPage().searchFilters().button("1").click();
        basePageSteps.onMobileMainPage().openExtFilter();
        basePageSteps.selectExtFilterElement("Балкон");
        basePageSteps.selectExtFilterElement("от 10 м²");
        basePageSteps.onMobileMainPage().hideExtFilters();
        basePageSteps.onMobileMainPage().searchFilters().waitUntil(isDisplayed());
        basePageSteps.onMobileMainPage().searchFilters().metroAndStreet().click();
        basePageSteps.onMobileMainPage().searchFilters().filterPopup().button(METRO).click();
        basePageSteps.onMobileMainPage().searchFilters().filterPopup().metro("Петроградская").click();
        basePageSteps.onMobileMainPage().searchFilters().filterPopup().showButton().click();
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(KVARTIRA).path(ODNOKOMNATNAYA).path("/metro-petrogradskaya/")
                .path("/s-balkonom-i-s-bolshoy-kuhney/").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("3 параметра: Купить квартиру + с парком + с водоёмом + кухня от 10м2")
    public void shouldSee3ParamsInUrl() {
        urlSteps.testing().path(SPB_I_LO).open();
        basePageSteps.onMobileMainPage().openExtFilter();
        basePageSteps.selectExtFilterElement("от 10 м²");
        basePageSteps.selectExtFilterElement("Парк");
        basePageSteps.selectExtFilterElement("Водоём");
        basePageSteps.onMobileMainPage().extendFilters().applyFiltersButton().click();
        urlSteps.path(KUPIT).path(KVARTIRA).queryParam("hasPark", "YES").queryParam("hasPond", "YES")
                .queryParam("kitchenSpaceMin", "10").shouldNotDiffWithWebDriverUrl();
    }
}
