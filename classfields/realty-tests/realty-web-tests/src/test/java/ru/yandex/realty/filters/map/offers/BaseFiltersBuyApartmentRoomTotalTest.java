package ru.yandex.realty.filters.map.offers;

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
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAPFILTERS;

@DisplayName("Карта. Базовые фильтры поиска по объявлениям")
@Feature(MAPFILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class BaseFiltersBuyApartmentRoomTotalTest {

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

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testParams() {
        return asList(new Object[][]{
                {"Студия", "studiya/"},
                {"1", "odnokomnatnaya/"},
                {"2", "dvuhkomnatnaya/"},
                {"3", "tryohkomnatnaya/"},
                {"4", "4-i-bolee/"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(KVARTIRA).path(KARTA).open();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «количество комнат»")
    public void shouldSeeRoomTotalParamInUrl() {
        basePageSteps.onMapPage().filters().button(name).click();
        basePageSteps.loaderWait();
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(KVARTIRA).path(expected).path(KARTA)
                .shouldNotDiffWithWebDriverUrl();
    }
}
