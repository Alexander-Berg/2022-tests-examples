package ru.auto.tests.desktop.metrics;

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
import ru.auto.tests.desktop.module.DesktopDevToolsTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.METRICS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.WindowSize.HEIGHT_1024;
import static ru.auto.tests.desktop.matchers.RequestHasQueryItemsMatcher.hasGoal;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneMetricsRequest;

@DisplayName("Метрики - цели - чат")
@Feature(METRICS)
@GuiceModules(DesktopDevToolsTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SaleChatGoalsTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    public SeleniumMockSteps seleniumMockSteps;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    public BasePageSteps basePageSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String state;

    @Parameterized.Parameter(2)
    public String saleMock;

    @Parameterized.Parameter(3)
    public String goal;

    @Parameterized.Parameters(name = "name = {index}: {0} {1} {3}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][] {
                {CARS, USED, "desktop/OfferCarsUsedUser", "CHAT_OPEN_DESKTOP"},

                {MOTORCYCLE, USED, "desktop/OfferMotoUsedUser", "CHAT_OPEN_DESKTOP"},

                {TRUCK, USED, "desktop/OfferTrucksUsedUser", "CHAT_OPEN_DESKTOP"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                saleMock).post();

        urlSteps.testing().path(category).path(state).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отправка метрики")
    public void shouldSendMetrics() {
        basePageSteps.setWideWindowSize(HEIGHT_1024);
        basePageSteps.onCardPage().contacts().sendMessageButton().click();

        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasGoal(
                seleniumMockSteps.formatGoal(goal),
                urlSteps.getCurrentUrl()
        )));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отправка метрики в галерее")
    public void shouldSendGalleryMetrics() {
        basePageSteps.setWideWindowSize(HEIGHT_1024);
        basePageSteps.onCardPage().gallery().currentImage().click();
        basePageSteps.onCardPage().fullScreenGallery().contacts().sendMessageButton().click();

        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasGoal(
                seleniumMockSteps.formatGoal(goal),
                urlSteps.getCurrentUrl()
        )));
    }
}
