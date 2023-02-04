package ru.yandex.realty.filters.offers;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
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

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;

@Link("https://st.yandex-team.ru/VERTISTEST-2120")
@DisplayName("Расширенные фильтры поиска по объявлениям.")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ExtendedExtraFiltersToMortgageTest {

    private static final String MORTGAGE_ABILITY = "Возможна ипотека";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void openSaleAdsPage() {
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onOffersSearchPage().openExtFilter();
        basePageSteps.scrollToElement(basePageSteps.onOffersSearchPage().extendFilters().button(MORTGAGE_ABILITY));
        basePageSteps.onOffersSearchPage().extendFilters().checkButton(MORTGAGE_ABILITY);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Возможна ипотека» применяем - видим урл")
    public void shouldSeeWithoutPledgeInUrl() {
        basePageSteps.onOffersSearchPage().extendFilters().applyFiltersButton().click();
        urlSteps.path("/v-ipoteku/").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Возможна ипотека» сворачиваем фильтры - видим бейдж")
    public void shouldSeeWithoutPledgeBadge() {
        basePageSteps.onOffersSearchPage().closeExtFilter();
        basePageSteps.onOffersSearchPage().filters().checkBox(MORTGAGE_ABILITY).waitUntil(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Возможна ипотека» применяем - видим h1")
    public void shouldSeeWithoutPledgeH1() {
        basePageSteps.onOffersSearchPage().extendFilters().applyFiltersButton().click();
        basePageSteps.onOffersSearchPage().pageH1().waitUntil(hasText("Купить квартиру в ипотеку в Москве и МО"));
    }
}
