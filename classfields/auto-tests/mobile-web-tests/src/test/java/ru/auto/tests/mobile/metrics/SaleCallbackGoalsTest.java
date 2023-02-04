package ru.auto.tests.mobile.metrics;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
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
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.METRICS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.matchers.RequestHasQueryItemsMatcher.hasGoal;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneMetricsRequest;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Метрики - цели - обратный звонок")
@Feature(METRICS)
@GuiceModules(MobileDevToolsTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SaleCallbackGoalsTest {

    private static final String SALE_ID = "1076842087-f1e84";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    public SeleniumMockSteps seleniumMockSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String state;

    @Parameterized.Parameter(2)
    public String goal;

    @Parameterized.Parameter(3)
    public String saleMock;

    @Parameterized.Parameter(4)
    public String callbackMock;

    @Parameterized.Parameters(name = "name = {index}: {0} {1} {2}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][] {
                {CARS, USED, "CALLBACK_USED_CL_CARS2", "desktop/OfferCarsUsedDealer",
                        "desktop/OfferCarsRegisterCallback"},
                {CARS, USED, "CONTACT_CARS", "desktop/OfferCarsUsedDealer",
                        "desktop/OfferCarsRegisterCallback"},
                {CARS, USED, "CONTACT_CARS_MOBILE", "desktop/OfferCarsUsedDealer",
                        "desktop/OfferCarsRegisterCallback"},
                {CARS, USED, "CONTACT_CARS_CALLBACK", "desktop/OfferCarsUsedDealer",
                        "desktop/OfferCarsRegisterCallback"},
                {CARS, USED, "CONTACT_CARS_DEALER_USED", "desktop/OfferCarsUsedDealer",
                        "desktop/OfferCarsRegisterCallback"},

                {CARS, NEW, "CALLBACK_NEW_CL_CARS2", "desktop/OfferCarsNewDealer",
                        "desktop/OfferCarsRegisterCallbackForNewCategory"},
                {CARS, NEW, "CONTACT_CARS", "desktop/OfferCarsNewDealer",
                        "desktop/OfferCarsRegisterCallbackForNewCategory"},
                {CARS, NEW, "CONTACT_CARS_MOBILE", "desktop/OfferCarsNewDealer",
                        "desktop/OfferCarsRegisterCallbackForNewCategory"},
                {CARS, NEW, "CONTACT_CARS_CALLBACK", "desktop/OfferCarsNewDealer",
                        "desktop/OfferCarsRegisterCallbackForNewCategory"},
                {CARS, NEW, "CONTACT_CARS_DEALER_NEW", "desktop/OfferCarsNewDealer",
                        "desktop/OfferCarsRegisterCallbackForNewCategory"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/User",
                saleMock,
                callbackMock).post();

        urlSteps.testing().path(category).path(state).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отправка метрики")
    public void shouldSendMetrics() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().callbackButton());
        basePageSteps.onCardPage().callbackPopup().button("Перезвоните мне").waitUntil(isDisplayed()).click();

        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasGoal(
                seleniumMockSteps.formatGoal(goal),
                urlSteps.getCurrentUrl()
        )));
    }
}
