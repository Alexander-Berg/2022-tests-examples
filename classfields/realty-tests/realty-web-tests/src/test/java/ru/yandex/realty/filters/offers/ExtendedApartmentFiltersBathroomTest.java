package ru.yandex.realty.filters.offers;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
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
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;


@DisplayName("Расширенные фильтры поиска по объявлениям.")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ExtendedApartmentFiltersBathroomTest {

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
                {"Совмещённый", "MATCHED"},
                {"Несколько", "TWO_AND_MORE"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(SPB_I_LO).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onOffersSearchPage().openExtFilter();
    }

    @Test
    @Owner(KURAU)
    @DisplayName("Параметр «Санузел»")
    public void shouldSeeBathroomInUrl() {
        basePageSteps.scrollElementToCenter(basePageSteps.onOffersSearchPage().extendFilters().button(label));
        basePageSteps.onOffersSearchPage().extendFilters().checkButton(label);
        basePageSteps.onOffersSearchPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam("bathroomUnit", expected).shouldNotDiffWithWebDriverUrl();
    }
}
