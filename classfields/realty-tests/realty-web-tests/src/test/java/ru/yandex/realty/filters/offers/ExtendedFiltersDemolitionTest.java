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
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.VTORICHNIY_RYNOK;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;

@DisplayName("Расширенные фильтры поиска по объявлениям.")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ExtendedFiltersDemolitionTest {

    private static final String RENOVATION = "Реновация";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).path(VTORICHNIY_RYNOK).open();
        basePageSteps.onOffersSearchPage().openExtFilter();
        basePageSteps.scrollToElement(
                basePageSteps.onOffersSearchPage().extendFilters().byName(RENOVATION));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Показать дома под снос»")
    public void shouldSeeIncludeTagInUrl() {
        basePageSteps.onOffersSearchPage().extendFilters().byName(RENOVATION).button("Показать дома под снос").click();
        basePageSteps.onOffersSearchPage().extendFilters().applyFiltersButton().click();
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA)
                .path("/vtorichniy-rynok-i-pod-snos/").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Не показывать дома под снос»")
    public void shouldSeeExcludeTagInUrl() {
        basePageSteps.onOffersSearchPage().extendFilters().byName(RENOVATION)
                .button("Не показывать дома под снос").click();
        basePageSteps.onOffersSearchPage().extendFilters().applyFiltersButton().click();
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).queryParam("newFlat", "NO")
                .queryParam("expectDemolition", "NO").shouldNotDiffWithWebDriverUrl();
    }
}
