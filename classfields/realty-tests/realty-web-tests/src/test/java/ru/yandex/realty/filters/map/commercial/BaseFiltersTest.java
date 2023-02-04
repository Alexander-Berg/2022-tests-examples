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
import static ru.yandex.realty.consts.Filters.AVTOSERVIS;
import static ru.yandex.realty.consts.Filters.COMMERCIAL;
import static ru.yandex.realty.consts.Filters.GOSTINICA;
import static ru.yandex.realty.consts.Filters.GOTOVYJ_BIZNES;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.OBSHCHEPIT;
import static ru.yandex.realty.consts.Filters.OFIS;
import static ru.yandex.realty.consts.Filters.POMESHCHENIE_SVOBODNOGO_NAZNACHENIYA;
import static ru.yandex.realty.consts.Filters.PROIZVODSTVENNOE_POMESHCHENIE;
import static ru.yandex.realty.consts.Filters.SKLADSKOE_POMESHCHENIE;
import static ru.yandex.realty.consts.Filters.TORGOVOE_POMESHCHENIE;
import static ru.yandex.realty.consts.Filters.UCHASTOK_KOMMERCHESKOGO_NAZNACHENIYA;
import static ru.yandex.realty.consts.Filters.YURIDICHESKIJ_ADRES;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAPFILTERS;
import static ru.yandex.realty.element.saleads.FiltersBlock.TYPE_BUTTON;

@DisplayName("Карта. Фильтры поиска по коммерческой недвижимости")
@Feature(MAPFILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class BaseFiltersTest {

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
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {"Офисное помещение", OFIS},
                {"Торговое помещение", TORGOVOE_POMESHCHENIE},
                {"Помещение свободного назначения", POMESHCHENIE_SVOBODNOGO_NAZNACHENIYA},
                {"Складское помещение", SKLADSKOE_POMESHCHENIE},
                {"Производственное помещение", PROIZVODSTVENNOE_POMESHCHENIE},
                {"Земельный участок", UCHASTOK_KOMMERCHESKOGO_NAZNACHENIYA},
                {"Общепит", OBSHCHEPIT},
                {"Автосервис", AVTOSERVIS},
                {"Гостиница", GOSTINICA},
                {"Готовый бизнес", GOTOVYJ_BIZNES},
        });
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр типа недвижимости")
    public void shouldSeeTypeOfRealtyUrl() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(COMMERCIAL).path(KARTA).open();
        basePageSteps.onMapPage().filters().button(TYPE_BUTTON).click();
        basePageSteps.onMapPage().filters().selectPopup().item(label).click();
        basePageSteps.loaderWait();
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(COMMERCIAL).path(expected).path(KARTA)
                .shouldNotDiffWithWebDriverUrl();
    }
}
