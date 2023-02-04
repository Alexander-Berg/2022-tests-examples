package ru.yandex.realty.filters.villages;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.realty.consts.Filters.KOTTEDZHNYE_POSELKI;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;

@DisplayName("Поиск коттеджных поселков - фильтр 'Удаленность от Москвы'(ЧПУ)")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class DirectionDistanceMaxVillagesExtendedFilterTestChpu {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public String expected;

    @Parameterized.Parameters(name = "{index} -{0}")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {"До 10 км", "/v-10-km-ot-mkad/"},
                {"До 20 км", "/v-20-km-ot-mkad/"},
                {"До 30 км", "/v-30-km-ot-mkad/"},
                {"До 50 км", "/v-50-km-ot-mkad/"}
        });
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «Удаленность от Москвы»")
    public void shouldSeeDirectionDistanceMax() {
        urlSteps.testing().path(MOSKVA_I_MO).path(KUPIT).path(KOTTEDZHNYE_POSELKI).open();
        basePageSteps.onVillageListing().openExtFilter();
        basePageSteps.onVillageListing().extendFilters().select("Удаленность от Москвы", name);
        basePageSteps.onVillageListing().extendFilters().applyFiltersButton().click();
        urlSteps.path(expected).shouldNotDiffWithWebDriverUrl();
    }
}
