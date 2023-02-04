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
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;


@DisplayName("Расширенные фильтры поиска по объявлениям.")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ExtendedApartmentFiltersBathroomSeparatedTest {

    private static final String BUTTON_NAME = "Раздельный";

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
        basePageSteps.onOffersSearchPage().openExtFilter();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Санузел»")
    public void shouldSeeBathroomInUrl() {
        basePageSteps.scrollElementToCenter(basePageSteps.onOffersSearchPage().extendFilters().button(BUTTON_NAME));
        basePageSteps.onOffersSearchPage().extendFilters().checkButton(BUTTON_NAME);
        basePageSteps.onOffersSearchPage().extendFilters().applyFiltersButton().click();
        urlSteps.path("/razdelniy-sanuzel/").shouldNotDiffWithWebDriverUrl();
    }
}
