package ru.yandex.realty.filters.map.offers;

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
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAPFILTERS;
import static ru.yandex.realty.step.UrlSteps.NO_VALUE;
import static ru.yandex.realty.step.UrlSteps.YES_VALUE;

@DisplayName("Карта. Базовые фильтры поиска по объявлениям")
@Feature(MAPFILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class BaseFiltersNewFlatTest {

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
                {"Вторичный рынок", NO_VALUE},
                {"Новостройки", YES_VALUE}
        });
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «новостройки/вторичного рынка»")
    public void shouldSeeBuyTypeOfNewFlatUrl() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).path(KARTA).open();
        basePageSteps.onMapPage().filters().button("Вторичка, новостройки").click();
        basePageSteps.onMapPage().filters().selectPopup().item(label).click();
        basePageSteps.loaderWait();
        urlSteps.queryParam("newFlat", expected).shouldNotDiffWithWebDriverUrl();
    }
}
