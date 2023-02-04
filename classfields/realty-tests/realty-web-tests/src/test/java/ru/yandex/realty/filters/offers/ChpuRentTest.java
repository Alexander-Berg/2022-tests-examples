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
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.Filters.DVUHKOMNATNAYA;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.ROSSIYA;
import static ru.yandex.realty.consts.Filters.SNYAT;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.element.saleads.FiltersBlock.POSUTOCHO_BUTTON;
import static ru.yandex.realty.element.saleads.FiltersBlock.SNYAT_BUTTON;
import static ru.yandex.realty.step.UrlSteps.REDIRECT_FROM_RGID;
import static ru.yandex.realty.step.UrlSteps.TRUE_VALUE;

@DisplayName("Фильтры поиска по объявлениям.")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ChpuRentTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void openSaleAdsPage() {
        urlSteps.testing().path(ROSSIYA).path(SNYAT).path(KVARTIRA).queryParam(REDIRECT_FROM_RGID, TRUE_VALUE).open();
        basePageSteps.onOffersSearchPage().openExtFilter();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("2 параметра Снять квартиру + современный ремонт + с мебелью")
    public void shouldSee2ParamsRentInUrl() {
        basePageSteps.onOffersSearchPage().extendFilters().selectButton("Современный");
        basePageSteps.scrollElementToCenter(basePageSteps.onOffersSearchPage().extendFilters().button("Есть"));
        basePageSteps.onOffersSearchPage().extendFilters().checkButton("Есть");
        basePageSteps.onOffersSearchPage().extendFilters().applyFiltersButton().click();
        basePageSteps.loaderWait();
        urlSteps.path("/sovremenniy-remont-i-s-mebeliu/").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Комнатность + 2 параметра Снять квартиру + двухкомнатную + посуточно + c животными")
    public void shouldSee2ParamsWithRoomsRentInUrl() {
        basePageSteps.onOffersSearchPage().extendFilters().button(SNYAT_BUTTON).click();
        basePageSteps.onOffersSearchPage().extendFilters().selectPopup().item(POSUTOCHO_BUTTON).click();
        basePageSteps.onOffersSearchPage().extendFilters().checkButton("2");
        basePageSteps.onOffersSearchPage().extendFilters().checkButton("Можно с животными");
        basePageSteps.onOffersSearchPage().extendFilters().applyFiltersButton().click();
        basePageSteps.loaderWait();
        urlSteps.path(DVUHKOMNATNAYA).path("/posutochno-i-s-zhivotnymi/").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Много параметров Снять квартиру + посуточно + с животными + Холодильник")
    public void shouldSee3ParamsRentInUrl() {
        basePageSteps.onOffersSearchPage().extendFilters().button(SNYAT_BUTTON).click();
        basePageSteps.onOffersSearchPage().extendFilters().selectPopup().item(POSUTOCHO_BUTTON).click();
        basePageSteps.onOffersSearchPage().extendFilters().checkButton("2");
        basePageSteps.onOffersSearchPage().extendFilters().checkButton("Можно с животными");
        basePageSteps.onOffersSearchPage().extendFilters().checkButton("Холодильник");
        basePageSteps.onOffersSearchPage().extendFilters().applyFiltersButton().click();
        basePageSteps.loaderWait();
        urlSteps.path(DVUHKOMNATNAYA).queryParam("rentTime", "SHORT").queryParam("withPets", "YES")
                .queryParam("hasRefrigerator", "YES").shouldNotDiffWithWebDriverUrl();
    }
}
