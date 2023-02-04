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
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Smoke;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.Filters.COMMERCIAL;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.SNYAT;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.matchers.AttributeMatcher.isChecked;

@DisplayName("Фильтры поиска по коммерческой недвижимости")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ExtendedFiltersRentPricingPeriodTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps user;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void openCommercialPage() {
        urlSteps.testing().path(MOSKVA).path(SNYAT).path(COMMERCIAL).open();
        user.onCommercialPage().openExtFilter();
        user.scrollToElement(user.onCommercialPage().extendFilters().byName("Стоимость"));
    }

    @Test
    @Category({Regression.class, Smoke.class, Production.class})
    @Owner(KURAU)
    @DisplayName("Параметр «Цена за»")
    public void shouldSeePricingPeriodInUrl() {
        user.onCommercialPage().extendFilters().checkButton("За год");
        user.onCommercialPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam("pricingPeriod", "PER_YEAR").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Smoke.class, Production.class})
    @Owner(KURAU)
    @DisplayName("Параметр «Цена за месяц» зачекана")
    public void shouldSeeDefaultPricingPeriodInUrl() {
        user.onCommercialPage().extendFilters().button("За месяц").should(isChecked());
    }
}
