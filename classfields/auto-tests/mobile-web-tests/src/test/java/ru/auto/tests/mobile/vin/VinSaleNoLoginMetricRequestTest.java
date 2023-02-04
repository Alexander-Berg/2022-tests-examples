package ru.auto.tests.mobile.vin;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileDevToolsTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static ru.auto.tests.desktop.consts.AutoruFeatures.VIN;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.element.card.VinReport.BUY_FULL_REPORT;
import static ru.auto.tests.desktop.element.card.VinReport.SHOW_FREE_REPORT;
import static ru.auto.tests.desktop.matchers.RequestHasQueryItemsMatcher.hasQuery;
import static ru.auto.tests.desktop.matchers.RequestHasBodyMatcher.hasSiteInfo;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneMetricsRequest;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.step.SeleniumMockSteps.queryPair;

@DisplayName("Проверка запросов в метрику под незарегом при запросе отчёта")
@Feature(VIN)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileDevToolsTestsModule.class)
public class VinSaleNoLoginMetricRequestTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    public SeleniumMockSteps seleniumMockSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/OfferCarsUsedUser"),
                stub("desktop/CarfaxOfferCarsRawNotPaid")
        ).create();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path("/land_rover/").path("/discovery/").path(SALE_ID).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Проверка запроса в метрику незалогином по кнопке «Показать бесплатный отчёт», событие «cars»")
    public void shouldSeeMetricRequestCarsFreeReport() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().vinReport().button(SHOW_FREE_REPORT));

        shouldSeeMetricsRequest(
                "{\"cars\":{\"card\":{\"history_report_mini\":{\"click_show_free_report\":{\"no_login\":{}}}}}}");
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Проверка запроса в метрику незалогином по кнопке «Показать бесплатный отчёт», событие «vas»")
    public void shouldSeeMetricRequestVasFreeReport() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().vinReport().button(SHOW_FREE_REPORT));

        shouldSeeMetricsRequest(
                "{\"vas\":{\"m\":{\"cars\":{\"other\":{\"shows\":{\"reports_10\":{}}}}}}}");
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Проверка запроса в метрику незалогином по кнопке «Купить полный отчёт», событие «cars»")
    public void shouldSeeMetricRequestCarsBuyFullReport() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().vinReport().button(BUY_FULL_REPORT));

        shouldSeeMetricsRequest(
                "{\"cars\":{\"card\":{\"history_report_mini\":{\"click_unauthorized_button\":{}}}}}");
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Проверка запроса в метрику незалогином по кнопке «Купить полный отчёт», событие «vas»")
    public void shouldSeeMetricRequestVasBuyFullReport() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().vinReport().button(BUY_FULL_REPORT));

        shouldSeeMetricsRequest(
                "{\"vas\":{\"m\":{\"cars\":{\"other\":{\"clicks\":{\"reports_10\":{}}}}}}}");
    }

    @Step("Проверяем наличие запроса в метрику с телом «{postData}»")
    private void shouldSeeMetricsRequest(String postData) {
        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(
                hasQuery(queryPair("page-url", urlSteps.toString())),
                hasSiteInfo(postData)
        ));
    }

}
