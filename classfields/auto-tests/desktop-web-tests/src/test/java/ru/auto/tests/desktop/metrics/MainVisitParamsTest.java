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

@DisplayName("Метрики - параметры визитов - главная")
@Feature(METRICS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopDevToolsTestsModule.class)
public class MainVisitParamsTest {

    private static final String SEARCH = "{\"cars\":{\"index\":{\"searchline\":{\"click_suggest\":{}}}}}";
    private static final String WATCHED = "{\"cars\":{\"index\":{\"watched\":{\"clicks\":{\"1\":{\"from_watched\":{}}}}}}}";
    private static final String LCV_BLOCK = "{\"cars\":{\"index\":{\"commercial-categories\":{\"from_index\":{}}}}}";
    private static final String CATALOG_NEWS = "{\"cars\":{\"index\":{\"catalog\":{\"from_index\":{}}}}}";

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
                stub("desktop/SessionAuthUser"),
                stub("desktop/SearchCarsBreadcrumbsEmpty"),
                stub("desktop/SearchlineSuggest"),
                stub("desktop/OfferCarsUsedUser"),
                stub("desktop/HistoryLastCars"),
                stub("desktop/SearchCarsShowcase")
        ).create();

        urlSteps.testing().open();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Метрики поисковой строки")
    public void shouldSendSearchLineMetrics() {
        basePageSteps.onMainPage().header().searchLine().input("Поиск по объявлениям", "BMW");
        basePageSteps.onMainPage().header().searchLine().suggest().getItem(0).click();

        browserMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasSiteInfo(SEARCH)));
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Метрики блока «Вы смотрели»")
    public void shouldSendWatchedMetrics() {
        basePageSteps.onMainPage().watched().getItem(0).click();

        browserMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasSiteInfo(WATCHED)));
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Метрики блока «Коммерческий транспорт»")
    public void shouldSendCommercialBlockMetrics() {
        basePageSteps.onMainPage().commercialBlock().button("Лёгкие коммерческие").click();

        browserMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasSiteInfo(LCV_BLOCK)));
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Метрики блока «Новинки каталога»")
    public void shouldSendCatalogNewsMetrics() {
        basePageSteps.onMainPage().catalogNews().title().click();

        browserMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasSiteInfo(CATALOG_NEWS)));
    }
}
