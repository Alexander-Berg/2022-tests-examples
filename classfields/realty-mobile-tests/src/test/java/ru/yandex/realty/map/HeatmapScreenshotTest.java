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
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.rules.MockRuleConfigurable;
import ru.yandex.realty.step.CompareSteps;
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
import static ru.yandex.realty.mock.PointStatisticSearchTemplate.pointStatisticSearchTemplate;
import static ru.yandex.realty.step.UrlSteps.LAYER_URL_PARAM;

@Issue("VERTISTEST-1407")
@Epic(HEATMAPS)
@DisplayName("Тепловая карта")
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebMobileModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class HeatmapScreenshotTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRuleConfigurable;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CompareSteps compareSteps;

    @Parameterized.Parameter
    public String title;

    @Parameterized.Parameter(1)
    public String layerValue;

    @Parameterized.Parameters(name = "Тепловая карта «{0}»")
    public static Collection<Object[]> testParams() {
        return asList(new Object[][]{
                {"Инфраструктура для жизни и развлечений", "infrastructure"},
                {"Доступность на общественном транспорте", "transport"},
                {"Цена продажи квартир", "price-sell"},
                {"Цена долгосрочной аренды квартиры", "price-rent"},
                {"Прогноз окупаемости квартир", "profitability"},
                {"Доступность автомобилей Яндекс.Драйва", "carsharing"},
                {"Школы", "education"}
        });
    }

    @Before
    public void before() {
        mockRuleConfigurable
                .pointStatisticSearchStub(pointStatisticSearchTemplate().build())
                .createWithDefaults();
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).path(KARTA)
                .queryParam(LAYER_URL_PARAM, layerValue).open();
        basePageSteps.onMobileMapPage().paranja().clickIf(isDisplayed());
        basePageSteps.onMobileMapPage().paranja().waitUntil(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот тепловой карты")
    public void shouldSeeHeatmapScreenshot() {
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onMobileMapPage().pageRoot());

        urlSteps.setMobileProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onMobileMapPage().pageRoot());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}
