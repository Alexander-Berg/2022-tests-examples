package ru.yandex.realty.map;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
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
import static org.hamcrest.Matchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.RealtyFeatures.HEATMAPS;
import static ru.yandex.realty.step.UrlSteps.LAYER_URL_PARAM;

@Issue("VERTISTEST-1407")
@Epic(HEATMAPS)
@DisplayName("Тепловая карта")
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebMobileModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class HeatmapLayerURLTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String layerName;

    @Parameterized.Parameter(1)
    public String layerValue;

    @Parameterized.Parameters(name = "Тепловая карта «{0}»")
    public static Collection<Object[]> testParams() {
        return asList(new Object[][]{
                {"Инфраструктура", "infrastructure"},
                {"Транспортная доступность", "transport"},
                {"Цена продажи", "price-sell"},
                {"Цена аренды", "price-rent"},
                {"Прогноз окупаемости", "profitability"},
                {"Доступность Яндекс.Драйва", "carsharing"},
                {"Школы и их рейтинг", "education"}
        });
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Формирование URL при выборе слоя тепловой карты")
    public void shouldSeeChooseLayerURL() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).path(KARTA).open();
        basePageSteps.onMobileMapPage().paranja().clickIf(isDisplayed());
        basePageSteps.onMobileMapPage().paranja().waitUntil(not(isDisplayed()));
        basePageSteps.onMobileMapPage().layers().click();
        basePageSteps.onMobileMapPage().spanLink(layerName).click();

        urlSteps.queryParam(LAYER_URL_PARAM, layerValue).ignoreMapCoordinate().shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Формирование URL при переключении слоя тепловой карты")
    public void shouldSeeSwitchLayerURL() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).path(KARTA)
                .queryParam(LAYER_URL_PARAM, "infrastructure").open();
        basePageSteps.onMobileMapPage().paranja().clickIf(isDisplayed());
        basePageSteps.onMobileMapPage().paranja().waitUntil(not(isDisplayed()));
        basePageSteps.onMobileMapPage().layers().click();
        basePageSteps.onMobileMapPage().spanLink(layerName).click();

        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).path(KARTA)
                .queryParam(LAYER_URL_PARAM, layerValue).ignoreMapCoordinate().shouldNotDiffWithWebDriverUrl();
    }

}
