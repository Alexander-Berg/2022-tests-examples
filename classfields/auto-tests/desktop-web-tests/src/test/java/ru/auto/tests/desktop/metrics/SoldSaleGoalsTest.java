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
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.matchers.RequestHasQueryItemsMatcher.hasGoal;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneMetricsRequest;

@DisplayName("Метрики - цели - карточка проданного объявления")
@Feature(METRICS)
@GuiceModules(DesktopDevToolsTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SoldSaleGoalsTest {

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
                {CARS, USED, "desktop/OfferCarsUsedUserInactive", "VIEW_SOLD_CAR_CARD"},
                {CARS, USED, "desktop/OfferCarsUsedDealerInactive", "VIEW_SOLD_CAR_CARD"},
                {CARS, NEW, "desktop/OfferCarsNewInactive", "VIEW_SOLD_CAR_CARD"},

                {TRUCK, USED, "desktop/OfferTrucksUsedUserInactive", "VIEW_SOLD_CAR_CARD"},
                {TRUCK, USED, "desktop/OfferTrucksUsedDealerInactive", "VIEW_SOLD_CAR_CARD"},
                {TRUCK, NEW, "desktop/OfferTrucksNewInactive", "VIEW_SOLD_CAR_CARD"},

                {MOTORCYCLE, USED, "desktop/OfferMotoUsedUserInactive", "VIEW_SOLD_CAR_CARD"},
                {MOTORCYCLE, USED, "desktop/OfferMotoUsedDealerInactive", "VIEW_SOLD_CAR_CARD"},
                {MOTORCYCLE, NEW, "desktop/OfferMotoNewInactive", "VIEW_SOLD_CAR_CARD"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionUnauth",
                saleMock).post();

        urlSteps.testing().path(category).path(state).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отправка метрики")
    public void shouldSendMetrics() {
        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasGoal(
                seleniumMockSteps.formatGoal(goal),
                urlSteps.getCurrentUrl()
        )));
    }
}
