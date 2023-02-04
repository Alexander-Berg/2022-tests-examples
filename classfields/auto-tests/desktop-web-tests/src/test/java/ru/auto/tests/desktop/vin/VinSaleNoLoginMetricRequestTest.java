package ru.auto.tests.desktop.vin;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopDevToolsTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.VIN;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.element.card.VinReport.BUY_FULL_REPORT;
import static ru.auto.tests.desktop.element.card.VinReport.SHOW_FREE_REPORT;
import static ru.auto.tests.desktop.matchers.RequestHasBodyMatcher.hasSiteInfo;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneMetricsRequest;

@DisplayName("Проверка запросов в метрику под незарегом при запросе отчёта")
@Feature(VIN)
@RunWith(Parameterized.class)
@GuiceModules(DesktopDevToolsTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class VinSaleNoLoginMetricRequestTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

    private static final String FREE_REPORT_CAR = "{\"cars\":{\"card\":{\"history_report_mini\":{\"click_show_free_report\":{\"no_login\":{}}}}}}";
    private static final String FREE_REPORT_VAS = "{\"vas\":{\"cars\":{\"card\":{\"shows\":{\"reports_10\":{}}}}}}";
    private static final String BUY_REPORT_CAR = "{\"cars\":{\"card\":{\"history_report_mini\":{\"click_unauthorized_button\":{}}}}}";
    private static final String BUY_REPORT_VAS = "{\"vas\":{\"cars\":{\"card\":{\"shows\":{\"reports_10\":{}}}}}}";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    public SeleniumMockSteps seleniumMockSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String report;

    @Parameterized.Parameter(1)
    public String visitParam;

    @Parameterized.Parameter(2)
    public String visitParamName;

    @Parameterized.Parameters(name = "name = {index}: {0} {2}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {SHOW_FREE_REPORT, FREE_REPORT_CAR, "Событие «cars»"},
                {SHOW_FREE_REPORT, FREE_REPORT_VAS, "Событие «vas»"},
                {BUY_FULL_REPORT, BUY_REPORT_CAR, "Событие «cars»"},
                {BUY_FULL_REPORT, BUY_REPORT_VAS, "Событие «vas»"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/OfferCarsUsedUser",
                "desktop/CarfaxOfferCarsRawNotPaid").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE)
                .path("/land_rover/")
                .path("/discovery/")
                .path(SALE_ID)
                .open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Проверка запроса в метрику незалогином по кнопке «Показать бесплатный отчёт»")
    public void shouldSeeMetricRequestCarsFreeReport() {
        basePageSteps.onCardPage().vinReport().button(report).click();

        shouldSeeMetrics(visitParam);
    }

    @Step("Проверяем метрики")
    private void shouldSeeMetrics(String visitParam) {
        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasSiteInfo(visitParam)));
    }
}
