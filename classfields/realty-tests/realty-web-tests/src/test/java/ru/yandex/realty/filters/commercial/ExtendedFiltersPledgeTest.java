package ru.yandex.realty.filters.commercial;

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

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.COMMERCIAL;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Filters.SNYAT;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;

@Link("https://st.yandex-team.ru/VERTISTEST-2120")
@DisplayName("Фильтры поиска по коммерческой недвижимости")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ExtendedFiltersPledgeTest {

    private static final String WITHOUT_PLEDGE = "Без залога";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void openSaleAdsPage() {
        urlSteps.testing().path(MOSKVA_I_MO).path(SNYAT).path(COMMERCIAL).open();
        basePageSteps.onCommercialPage().openExtFilter();
        basePageSteps.scrollToElement(basePageSteps.onCommercialPage().extendFilters().button(WITHOUT_PLEDGE));
        basePageSteps.onCommercialPage().extendFilters().checkButton(WITHOUT_PLEDGE);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Без залога» применяем - видим урл")
    public void shouldSeeWithoutPledgeInUrl() {
        basePageSteps.onCommercialPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam("rentPledge", UrlSteps.NO_VALUE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Без залога» сворачиваем фильтры - видим бейдж")
    public void shouldSeeWithoutPledgeBadge() {
        basePageSteps.onCommercialPage().closeExtFilter();
        basePageSteps.onCommercialPage().filters().checkBox(WITHOUT_PLEDGE).waitUntil(isDisplayed());
    }
}
