package ru.yandex.realty.map;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.RealtyFeatures.HEATMAPS;

@Issue("VERTISTEST-1407")
@Epic(HEATMAPS)
@DisplayName("Скрытие и отображение пинов")
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebMobileModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class HidePinsTest {

    private static final String PRICE_SELL_LAYER = "?layer=price-sell";
    private static final String MAP_POLYGON = "?mapPolygon=55.697%2C37.65788%3B55.72812%2C37.62727%3B55.82812%2C37.65281%3B55" +
            ".72638%2C37.71372";
    private static final String TIME_FOR_ROAD = "?commuteAddress=Павелецкая&commutePointLatitude=55.731537" +
            "&commutePointLongitude=37.63633&commuteTime=20&commuteTransport=PUBLIC";

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
    public String params;

    @Parameterized.Parameters(name = "«{0}»")
    public static Collection<Object[]> testParams() {
        return asList(new Object[][]{
                {"Тепловая карта «Цена продажи»", PRICE_SELL_LAYER},
                {"Нарисованная область", MAP_POLYGON},
                {"Время на дорогу", TIME_FOR_ROAD},
                {"Не выбрана тепловая карта", ""}
        });
    }

    @Before
    public void before() {
        urlSteps.fromUri(urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).path(KARTA) + params).open();
        basePageSteps.onMobileMapPage().paranja().clickIf(isDisplayed());
        basePageSteps.onMobileMapPage().paranja().waitUntil(not(isDisplayed()));
        basePageSteps.onMobileMapPage().pinsList().should(hasSize(greaterThan(0)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скрытие пинов")
    public void shouldSeeNoPins() {
        basePageSteps.onMobileMapPage().hidePins().click();

        basePageSteps.onMobileMapPage().pinsList().should(hasSize(0));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Повторное отображение пинов")
    public void shouldSeePins() {
        basePageSteps.onMobileMapPage().hidePins().click();
        basePageSteps.onMobileMapPage().pinsList().waitUntil(hasSize(0));
        basePageSteps.onMobileMapPage().hidePins().click();

        basePageSteps.onMobileMapPage().pinsList().should(hasSize(greaterThan(0)));
    }

}
