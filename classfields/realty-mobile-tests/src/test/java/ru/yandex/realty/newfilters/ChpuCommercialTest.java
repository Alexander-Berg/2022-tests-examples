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
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.UrlSteps;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.yandex.realty.consts.Filters.COMMERCIAL;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.OFIS;
import static ru.yandex.realty.consts.Filters.SKLADSKOE_POMESHCHENIE;
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.mobile.element.main.FiltersBlock.FIELD_COMMERCIAL_TYPE;
import static ru.yandex.realty.rules.MockRuleConfigurable.PATH_TO_OFFER_WITH_SITE_SEARCH_COUNT_TEMPLATE;
import static ru.yandex.realty.step.UrlSteps.CATEGORY_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.COMMERCIAL_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.RGID;
import static ru.yandex.realty.step.UrlSteps.SELL_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.SPB_I_LO_RGID;
import static ru.yandex.realty.step.UrlSteps.TYPE_URL_PARAM;

@DisplayName("Фильтры поиска по коммерческой недвижимости. ЧПУ тесты")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class ChpuCommercialTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRuleConfigurable.offerWithSiteSearchCountStub(
                getResourceAsString(PATH_TO_OFFER_WITH_SITE_SEARCH_COUNT_TEMPLATE))
                .createWithDefaults();
        urlSteps.testing().path(Pages.FILTERS).queryParam(RGID, SPB_I_LO_RGID)
                .queryParam(TYPE_URL_PARAM, SELL_URL_PARAM).queryParam(CATEGORY_URL_PARAM, COMMERCIAL_URL_PARAM).open();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("2 параметра: Бизнес-центр + мебель + от собственника:")
    public void shouldSee2ParamsCommercialInUrl() {
        basePageSteps.onMobileMainPage().searchFilters().byName(FIELD_COMMERCIAL_TYPE).click();
        basePageSteps.onMobileMainPage().searchFilters().filterPopup().tumbler("Офисное помещение").click();
        basePageSteps.onMobileMainPage().searchFilters().filterPopup().showButton().click();
        basePageSteps.onMobileMainPage().searchFilters().click();
        basePageSteps.scrollToElement(basePageSteps.onMobileMainPage().searchFilters().byName("Мебель"));
        basePageSteps.onMobileMainPage().searchFilters().byName("Мебель").button("Есть").click();
        basePageSteps.onMobileMainPage().searchFilters().byName("Тип здания").button("Бизнес-центр").click();
        basePageSteps.onMobileMainPage().searchFilters().applyFiltersButton().click();
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(COMMERCIAL).path(OFIS)
                .path("/s-mebeliu-i-biznes-center/").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметры которые не формируют ЧПУ Купить коммерческую + складское помещение + склад + кондиционер")
    public void shouldSee3ParamsCommercialInUrl() {
        basePageSteps.onMobileMainPage().searchFilters().byName(FIELD_COMMERCIAL_TYPE).click();
        basePageSteps.onMobileMainPage().searchFilters().filterPopup().tumbler("Складское помещение").click();
        basePageSteps.onMobileMainPage().searchFilters().filterPopup().showButton().click();
        basePageSteps.scrollToElement(basePageSteps.onMobileMainPage().searchFilters().byName("Тип здания"));
        basePageSteps.onMobileMainPage().searchFilters().byName("Тип здания").button("Складской комплекс").click();
        basePageSteps.scrollToElement(basePageSteps.onMobileMainPage().searchFilters().byName("Удобства"));
        basePageSteps.onMobileMainPage().searchFilters().byName("Удобства").button("Кондиционер").click();
        basePageSteps.onMobileMainPage().searchFilters().applyFiltersButton().click();
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(COMMERCIAL).path(SKLADSKOE_POMESHCHENIE)
                .queryParam("commercialBuildingType", "WAREHOUSE").queryParam("hasAircondition", "YES")
                .shouldNotDiffWithWebDriverUrl();
    }
}
