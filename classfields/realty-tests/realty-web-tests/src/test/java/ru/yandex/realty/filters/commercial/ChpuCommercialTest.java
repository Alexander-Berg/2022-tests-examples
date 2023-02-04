package ru.yandex.realty.filters.commercial;

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
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.CoreMatchers.not;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.yandex.realty.consts.Filters.COMMERCIAL;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.OFIS;
import static ru.yandex.realty.consts.Filters.SKLADSKOE_POMESHCHENIE;
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.element.saleads.FiltersBlock.TYPE_BUTTON;
import static ru.yandex.realty.matchers.AttributeMatcher.isDisabled;
import static ru.yandex.realty.rules.MockRuleConfigurable.PATH_TO_OFFER_WITH_SITE_SEARCH_COUNT_TEMPLATE;

@DisplayName("Фильтры поиска по коммерческой недвижимости")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ChpuCommercialTest {

    public static final String WAREHOUSE = "Складское помещение";
    public static final String OFFICE = "Офисное помещение";
    private static final String FURNITURE = "Мебель";

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
    public void openSaleAdsPage() {
        mockRuleConfigurable.offerWithSiteSearchCountStub(
                getResourceAsString(PATH_TO_OFFER_WITH_SITE_SEARCH_COUNT_TEMPLATE))
                .createWithDefaults();
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(COMMERCIAL).open();
        basePageSteps.onOffersSearchPage().openExtFilter();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("2 параметра: Купить квартиру + вторичка + от собственника:")
    public void shouldSee2ParamsCommercialInUrl() {
        basePageSteps.onCommercialPage().extendFilters().select(TYPE_BUTTON, OFFICE);
        basePageSteps.onCommercialPage().extendFilters().button(OFFICE).click();
        basePageSteps.scrollToElement(basePageSteps.onCommercialPage().extendFilters().byName(FURNITURE));
        basePageSteps.onCommercialPage().extendFilters().byName(FURNITURE).checkButton("Есть");
        basePageSteps.onCommercialPage().extendFilters().checkButton("Бизнес-центр");
        basePageSteps.onCommercialPage().extendFilters().applyFiltersButton().clickIf(not(isDisabled()));
        urlSteps.path(OFIS).path("/s-mebeliu-i-biznes-center/").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметры которые не формируют ЧПУ Купить коммерческую + складское помещение + склад + кондиционер")
    public void shouldSee3ParamsCommercialInUrl() {
        basePageSteps.onCommercialPage().extendFilters().select(TYPE_BUTTON, WAREHOUSE);
        basePageSteps.onCommercialPage().extendFilters().button(WAREHOUSE).click();
        basePageSteps.onCommercialPage().extendFilters().checkButton("Складской комплекс");
        basePageSteps.onCommercialPage().extendFilters().checkButton("Кондиционер");
        basePageSteps.onCommercialPage().extendFilters().applyFiltersButton().click();
        urlSteps.path(SKLADSKOE_POMESHCHENIE).queryParam("commercialBuildingType", "WAREHOUSE").queryParam("hasAircondition", "YES")
                .shouldNotDiffWithWebDriverUrl();
    }
}
