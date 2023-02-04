package ru.auto.tests.desktop.metrics;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopDevToolsTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.METRICS;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.matchers.RequestHasBodyMatcher.hasSiteInfo;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneMetricsRequest;
import static ru.auto.tests.desktop.mock.MockStub.stub;

@DisplayName("Метрики - параметры визитов - шапка")
@Feature(METRICS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopDevToolsTestsModule.class)
public class HeaderVisitParamsTest {

    private static final String VISIT_PARAM = "{\"cars\":{\"index\":{\"tabs\":{\"history\":{}}}}}";

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

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SearchCarsBreadcrumbsRid213")
        ).create();

        urlSteps.testing().open();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отправка метрики при клике на ссылку в шапке")
    public void shouldSendHeaderUrlMetrics() {
        basePageSteps.onMainPage().header().line2().button("ПроАвто").click();

        browserMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasSiteInfo(VISIT_PARAM)));
    }
}
