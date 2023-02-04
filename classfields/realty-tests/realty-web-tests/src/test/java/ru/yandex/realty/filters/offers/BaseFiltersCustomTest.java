package ru.yandex.realty.filters.offers;

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
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static java.lang.String.valueOf;
import static ru.auto.tests.commons.util.Utils.getRandomShortInt;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.SNYAT;
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.Owners.JENKL;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Owners.KOPITSA;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.element.saleads.FiltersBlock.TO;
import static ru.yandex.realty.step.UrlSteps.PRICE_MAX_URL_PARAM;

@DisplayName("Базовые фильтры поиска по объявлениям")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class BaseFiltersCustomTest {

    private static final String RENOVATION = "renovation";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void openSaleAdsPage() {
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(KVARTIRA).open();
    }

    @Test
    @Owner(KOPITSA)
    @DisplayName("Все параметры «количество комнат»")
    public void shouldSeeAllRoomTotalParamsInUrl() {
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onOffersSearchPage().filters().selectAll("Студия", "1", "2", "3", "4+");
        basePageSteps.loaderWait();
        urlSteps.queryParam("roomsTotal", "1", "2", "3", "PLUS_4", "STUDIO").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(JENKL)
    @DisplayName("Параметры цена «до» и «количество комнат», комнатность рядом")
    public void shouldSeePriceMaxAndRoomTotalParamsInUrl() {
        basePageSteps.onOffersSearchPage().filters().selectAll("2", "3");
        basePageSteps.loaderWait();
        String priceMax = valueOf(getRandomShortInt());
        basePageSteps.onOffersSearchPage().filters().price().input(TO).sendKeys(priceMax);
        urlSteps.path("2,3-komnatnie/").queryParam(PRICE_MAX_URL_PARAM, priceMax).shouldNotDiffWithWebDriverUrl();

        basePageSteps.onOffersSearchPage().filters().deSelectButton("3");
        basePageSteps.onOffersSearchPage().filters().selectButton("1");
        basePageSteps.loaderWait();
        urlSteps.replacePath("sankt-peterburg_i_leningradskaya_oblast/kupit/kvartira/1,2-komnatnie/")
                .shouldNotDiffWithWebDriverUrl();
    }


    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Несколько параметров «ремонта»")
    public void shouldSeeTypesOfRenovationUrl() {
        urlSteps.testing().path(SPB_I_LO).path(SNYAT).path(KVARTIRA).open();
        basePageSteps.onOffersSearchPage().openExtFilter();
        basePageSteps.onOffersSearchPage().extendFilters().selectButton("Евро");
        basePageSteps.onOffersSearchPage().extendFilters().selectButton("Требует ремонта");
        basePageSteps.onOffersSearchPage().extendFilters().applyFiltersButton().click();
        basePageSteps.loaderWait();
        urlSteps.queryParam(RENOVATION, "EURO").queryParam(RENOVATION, "NEEDS_RENOVATION")
                .shouldNotDiffWithWebDriverUrl();
    }
}
