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
import static ru.yandex.realty.consts.Filters.COMMERCIAL;
import static ru.yandex.realty.consts.Filters.DOM;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Filters.ODNOKOMNATNAYA;
import static ru.yandex.realty.consts.Filters.OFIS;
import static ru.yandex.realty.consts.Filters.SNYAT;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.element.saleads.FiltersBlock.PRICE_FROM;
import static ru.yandex.realty.step.UrlSteps.PINNED_OFFER_ID_PARAM;
import static ru.yandex.realty.step.UrlSteps.PRICE_MIN_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.SORT_URL_PARAM;
import static ru.yandex.realty.utils.UtilsWeb.getNormalPrice;

@DisplayName("Фильтры поиска по объявлениям.")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class CancelParametersTest {

    private static final String TEST_ID_VALUE = "9999999999999999";
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {

    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Добавляем фильтр по цене, параметр скидывается")
    public void shouldSeeCancelParamByAddPriceFilter() {
        urlSteps.testing().path(MOSKVA_I_MO).path(SNYAT).path(DOM).queryParam(PINNED_OFFER_ID_PARAM, TEST_ID_VALUE)
                .open();
        String priceMin = valueOf(getNormalPrice());
        basePageSteps.onOffersSearchPage().filters().price().input(PRICE_FROM).sendKeys(priceMin);
        basePageSteps.onOffersSearchPage().filters().submitButton().click();
        urlSteps.testing().path(MOSKVA_I_MO).path(SNYAT).path(DOM).queryParam(PRICE_MIN_URL_PARAM, priceMin)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Убираем параметр, параметр скидывается")
    public void shouldSeeCancelParamDeleteFilter() {
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KVARTIRA).path(ODNOKOMNATNAYA)
                .queryParam(PINNED_OFFER_ID_PARAM, TEST_ID_VALUE).open();
        basePageSteps.onOffersSearchPage().filters().button("1").click();
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KVARTIRA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Сортировка не влияет")
    public void shouldSeeNotCancelParamBySortButton() {
        urlSteps.testing().path(MOSKVA_I_MO).path(SNYAT).path(COMMERCIAL).path(OFIS)
                .queryParam(PINNED_OFFER_ID_PARAM, TEST_ID_VALUE).open();
        basePageSteps.onOffersSearchPage().sortSelect().click();
        basePageSteps.onOffersSearchPage().option("цена по убыванию").click();
        urlSteps.queryParam(SORT_URL_PARAM, "PRICE_DESC").shouldNotDiffWithWebDriverUrl();
    }
}
