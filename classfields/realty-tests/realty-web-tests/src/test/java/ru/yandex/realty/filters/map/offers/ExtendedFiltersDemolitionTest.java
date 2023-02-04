package ru.yandex.realty.filters.map.offers;

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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAPFILTERS;
import static ru.yandex.realty.step.UrlSteps.NO_VALUE;
import static ru.yandex.realty.step.UrlSteps.YES_VALUE;

@DisplayName("Карта. Расширенные фильтры поиска по объявлениям.")
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ExtendedFiltersDemolitionTest {

    private static final String RENOVATION = "Реновация";
    private static final String EXPECT_DEMOLITION = "expectDemolition";

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

    @Parameterized.Parameters(name = "{1}")
    public static Collection<Object[]> testParams() {
        return asList(new Object[][]{
                {"Показать дома под снос", YES_VALUE},
                {"Не показывать дома под снос", NO_VALUE}
        });
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Реновация»")
    public void shouldSeeDemolitionInUrl() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).path(KARTA).queryParam("newFlat", UrlSteps.NO_VALUE)
                .open();
        basePageSteps.onMapPage().openExtFilter();
        basePageSteps.onMapPage().extendFilters().byName("Реновация").button(label).click();
        basePageSteps.loaderWait();
        urlSteps.queryParam("expectDemolition", expected).shouldNotDiffWithWebDriverUrl();
    }
}
