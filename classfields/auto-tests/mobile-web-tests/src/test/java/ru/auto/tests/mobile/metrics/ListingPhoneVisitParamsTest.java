package ru.auto.tests.mobile.metrics;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileDevToolsTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.METRICS;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.matchers.RequestHasBodyMatcher.hasSiteInfo;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneMetricsRequest;
import static ru.auto.tests.desktop.mock.MockStub.stub;

@DisplayName("Метрики - параметры визитов - телефон в листинге")
@Feature(METRICS)
@GuiceModules(MobileDevToolsTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ListingPhoneVisitParamsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    public SeleniumMockSteps browserMockSteps;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    public BasePageSteps basePageSteps;

    @Parameterized.Parameter
    public String testNum;

    @Parameterized.Parameter(1)
    public String category;

    @Parameterized.Parameter(2)
    public String state;

    @Parameterized.Parameter(3)
    public String visitParams;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"1", CARS, ALL, "{\"cars\":{\"listing\":{\"show-phone\":{\"user\":{\"listing\":{}}}}}}"},
                {"2", CARS, ALL, "{\"remarketing\":{\"cars\":{\"phoneview\":{\"mark\":{\"mercedes\":{\"seller\":{\"user\":{}},\"status\":{\"used\":{}}}},\"model\":{\"e-klasse\":{\"seller\":{\"user\":{}},\"status\":{\"used\":{}}}}}}}}"},
                {"3", CARS, ALL, "{\"__ym\":{\"ecommerce\":[{\"purchase\":{\"actionField\":{\""},
                {"4", CARS, ALL, "revenue\":50},\"products\":[{\"id\":\"mercedes-e_klasse-1076842087\",\"name\":\"E-Класс\",\"price\":980000,\"brand\":\"Mercedes-Benz\",\"category\":\"cars\"}]}}]}}"}
        });
    }

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SearchCarsBreadcrumbsEmpty"),
                stub("mobile/SearchCarsAll"),
                stub("desktop/OfferCarsPhones")
        ).create();

        urlSteps.testing().path(category).path(state).open();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отправка метрики")
    public void shouldSendMetrics() {
        basePageSteps.onListingPage().getSale(0).callButton().click();

        browserMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasSiteInfo(visitParams)));
    }
}
