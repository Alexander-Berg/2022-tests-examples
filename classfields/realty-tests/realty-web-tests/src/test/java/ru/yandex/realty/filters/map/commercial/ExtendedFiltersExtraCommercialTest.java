package ru.yandex.realty.filters.map.commercial;

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
import static ru.yandex.realty.consts.Filters.COMMERCIAL;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAPFILTERS;
import static ru.yandex.realty.step.UrlSteps.NO_VALUE;
import static ru.yandex.realty.step.UrlSteps.YES_VALUE;

@DisplayName("Карта. Фильтры поиска по коммерческой недвижимости")
@Feature(MAPFILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ExtendedFiltersExtraCommercialTest {

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

    @Parameterized.Parameter(2)
    public String value;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testParams() {
        return asList(new Object[][]{
                {"Без похожих", "showSimilar", NO_VALUE},
                {"Только с фото", "hasPhoto", YES_VALUE},
                {"Есть видео", "videoUrl", YES_VALUE},
                {"Онлайн-показ", "onlineShow", YES_VALUE},
        });
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Блок «Дополнительно»")
    public void shouldSeeExtraFilters() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(COMMERCIAL).path(KARTA).open();
        basePageSteps.onMapPage().openExtFilter();
        basePageSteps.onMapPage().extendFilters().checkButton(label);
        basePageSteps.loaderWait();
        urlSteps.queryParam(expected, value).shouldNotDiffWithWebDriverUrl();
    }
}
