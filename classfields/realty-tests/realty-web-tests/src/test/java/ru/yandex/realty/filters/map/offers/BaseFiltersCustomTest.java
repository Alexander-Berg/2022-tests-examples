package ru.yandex.realty.filters.map.offers;

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
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static java.lang.String.valueOf;
import static ru.auto.tests.commons.util.Utils.getRandomShortInt;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Filters.SNYAT;
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAPFILTERS;
import static ru.yandex.realty.element.saleads.FiltersBlock.TO;
import static ru.yandex.realty.step.UrlSteps.PRICE_MAX_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.ROOMS_TOTAL_URL_PARAM;

@DisplayName("Карта. Базовые фильтры поиска по объявлениям")
@Feature(MAPFILTERS)
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

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Все параметры «количество комнат»")
    public void shouldSeeAllRoomTotalParamsInUrl() {
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(KVARTIRA).path(KARTA).open();
        basePageSteps.onMapPage().filters().selectAll("Студия", "1", "2", "3", "4+");
        basePageSteps.loaderWait();
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(KVARTIRA).path(KARTA)
                .queryParam(ROOMS_TOTAL_URL_PARAM, "1","2","3", "PLUS_4", "STUDIO" ).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметры цена «до» и «количество комнат», комнатность рядом")
    public void shouldSeePriceMaxAndRoomTotalParamsInUrl() {
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(KVARTIRA).path(KARTA).open();
        String priceMax = valueOf(getRandomShortInt());
        basePageSteps.onMapPage().filters().price().input(TO).sendKeys(priceMax);

        basePageSteps.onMapPage().filters().selectAll("2", "3");
        basePageSteps.loaderWait();
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(KVARTIRA).path("2,3-komnatnie/").path(KARTA)
                .queryParam(PRICE_MAX_URL_PARAM, priceMax).shouldNotDiffWithWebDriverUrl();

        basePageSteps.onMapPage().filters().selectButton("1");
        basePageSteps.onMapPage().filters().deSelectButton("3");
        basePageSteps.loaderWait();
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(KVARTIRA).path("1,2-komnatnie/").path(KARTA)
                .queryParam(PRICE_MAX_URL_PARAM, priceMax).shouldNotDiffWithWebDriverUrl();
    }


    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Несколько параметров «ремонта»")
    public void shouldSeeTypesOfRenovationUrl() {
        urlSteps.testing().path(SPB_I_LO).path(SNYAT).path(KVARTIRA).path(KARTA).open();
        basePageSteps.onMapPage().openExtFilter();
        basePageSteps.onMapPage().extendFilters().selectButton("Евро");
        basePageSteps.onMapPage().extendFilters().selectButton("Требует ремонта");
        basePageSteps.loaderWait();
        urlSteps.queryParam(RENOVATION, "EURO").queryParam(RENOVATION, "NEEDS_RENOVATION")
                .shouldNotDiffWithWebDriverUrl();
    }
}
