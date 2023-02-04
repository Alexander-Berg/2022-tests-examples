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

import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.ODNOKOMNATNAYA;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.element.saleads.FiltersBlock.GEO_INPUT;

@DisplayName("Фильтры поиска по объявлениям.")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ChpuBuyTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void openSaleAdsPage() {
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onOffersSearchPage().openExtFilter();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("2 параметра: Купить квартиру + вторичка + от собственника:")
    public void shouldSee2ParamsInUrl() {
        basePageSteps.onOffersSearchPage().extendFilters().button("Вторичка, новостройки").click();
        basePageSteps.onOffersSearchPage().extendFilters().selectPopup().item("Вторичный рынок").click();
        basePageSteps.scrollToElement(basePageSteps.onOffersSearchPage().extendFilters().button("От собственников"));
        basePageSteps.onOffersSearchPage().extendFilters().checkButton("От собственников");
        basePageSteps.onOffersSearchPage().extendFilters().applyFiltersButton().click();
        basePageSteps.loaderWait();
        urlSteps.path("/vtorichniy-rynok-i-bez-posrednikov/").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Комнатность + 2 параметра - Купить квартиру + однокомнатная + новостройки + рядом парк")
    public void shouldSee2ParamsWithRoomsInUrl() {
        basePageSteps.onOffersSearchPage().extendFilters().checkButton("1");
        basePageSteps.onOffersSearchPage().extendFilters().button("Вторичка, новостройки").click();
        basePageSteps.onOffersSearchPage().extendFilters().selectPopup().item("Новостройки").click();
        basePageSteps.scrollToElement(basePageSteps.onOffersSearchPage().extendFilters().button("Парк"));
        basePageSteps.onOffersSearchPage().extendFilters().checkButton("Парк");
        basePageSteps.onOffersSearchPage().extendFilters().applyFiltersButton().click();
        basePageSteps.loaderWait();
        urlSteps.path(ODNOKOMNATNAYA).path("/novostroyki-i-s-parkom/").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Комнатность + метро + 2 параметра: квартиру + однокомнатная + метро Чернышевская + кухня 10м2 + с балконом")
    public void shouldSee2ParamsWithRoomsAndMetroInUrl() {
        basePageSteps.onOffersSearchPage().extendFilters().input(GEO_INPUT).sendKeys("Чернышевская");
        basePageSteps.onOffersSearchPage().extendFilters().suggest().get(0).click();
        basePageSteps.onOffersSearchPage().extendFilters().checkButton("1");
        basePageSteps.onOffersSearchPage().extendFilters().checkButton("Балкон");
        basePageSteps.scrollElementToCenter(basePageSteps.onOffersSearchPage().extendFilters().button("от 10 м²"));
        basePageSteps.onOffersSearchPage().extendFilters().checkButton("от 10 м²");
        basePageSteps.onOffersSearchPage().extendFilters().applyFiltersButton().click();
        basePageSteps.loaderWait();
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(KVARTIRA).path(ODNOKOMNATNAYA).path("/metro-chernyshevskaya/")
                .path("/s-balkonom-i-s-bolshoy-kuhney/").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("3 параметра: Купить квартиру + с парком + с водоёмом + кухня от 10м2")
    public void shouldSee3ParamsInUrl() {
        basePageSteps.scrollElementToCenter(basePageSteps.onOffersSearchPage().extendFilters().button("от 10 м²"));
        basePageSteps.onOffersSearchPage().extendFilters().checkButton("от 10 м²");
        basePageSteps.scrollToElement(basePageSteps.onOffersSearchPage().extendFilters().button("Парк"));
        basePageSteps.onOffersSearchPage().extendFilters().checkButton("Парк");
        basePageSteps.onOffersSearchPage().extendFilters().checkButton("Водоём");
        basePageSteps.onOffersSearchPage().extendFilters().applyFiltersButton().click();
        basePageSteps.loaderWait();
        urlSteps.queryParam("hasPark", "YES").queryParam("hasPond", "YES").queryParam("kitchenSpaceMin", "10")
                .shouldNotDiffWithWebDriverUrl();
    }
}
