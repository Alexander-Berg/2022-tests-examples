package ru.yandex.realty.filters.offers;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.SNYAT;
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;

@DisplayName("Базовые фильтры поиска по объявлениям")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class BaseFiltersRenovationNotChpuTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String label;

    @Parameterized.Parameter(1)
    public String expected;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testParams() {
        return asList(new Object[][]{
                {"Косметический", "COSMETIC_DONE"},
                {"Евро", "EURO"}
        });
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «ремонт»")
    public void shouldSeeBuyTypeOfRenovationUrl() {
        urlSteps.testing().path(SPB_I_LO).path(SNYAT).path(KVARTIRA).open();
        basePageSteps.onOffersSearchPage().openExtFilter();
        basePageSteps.onOffersSearchPage().extendFilters().selectButton(label);
        basePageSteps.onOffersSearchPage().extendFilters().applyFiltersButton().click();
        basePageSteps.loaderWait();
        urlSteps.queryParam("renovation", expected).shouldNotDiffWithWebDriverUrl();
    }
}
