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
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.METRICS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.matchers.RequestHasQueryItemsMatcher.hasGoal;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneMetricsRequest;
import static ru.auto.tests.desktop.mobile.page.ListingPage.TOP_SALES_COUNT;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Метрики - цели - обратный звонок")
@Feature(METRICS)
@GuiceModules(MobileDevToolsTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ListingCallbackGoalsTest {

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
    public String goal;

    @Parameterized.Parameter(3)
    public String listingMock;

    @Parameterized.Parameter(4)
    public String callbackMock;

    @Parameterized.Parameters(name = "name = {index}: {0} {1} {2}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][] {
                {CARS, USED, "CALLBACK_USED_CL_CARS2", "mobile/SearchCarsUsedCommercial",
                        "desktop/OfferCarsRegisterCallback"},
                {CARS, USED, "CONTACT_CARS", "mobile/SearchCarsUsedCommercial",
                        "desktop/OfferCarsRegisterCallback"},
                {CARS, USED, "CONTACT_CARS_MOBILE", "mobile/SearchCarsUsedCommercial",
                        "desktop/OfferCarsRegisterCallback"},
                {CARS, USED, "CONTACT_CARS_CALLBACK", "mobile/SearchCarsUsedCommercial",
                        "desktop/OfferCarsRegisterCallback"},
                {CARS, USED, "CONTACT_CARS_DEALER_USED", "mobile/SearchCarsUsedCommercial",
                        "desktop/OfferCarsRegisterCallback"},

                {CARS, ALL, "CALLBACK_NEW_CL_CARS2", "mobile/SearchCarsAllCommercial",
                        "desktop/OfferCarsRegisterCallbackForNewCategory"},
                {CARS, ALL, "CONTACT_CARS", "mobile/SearchCarsAllCommercial",
                        "desktop/OfferCarsRegisterCallbackForNewCategory"},
                {CARS, ALL, "CONTACT_CARS_MOBILE", "mobile/SearchCarsAllCommercial",
                        "desktop/OfferCarsRegisterCallbackForNewCategory"},
                {CARS, ALL, "CONTACT_CARS_CALLBACK", "mobile/SearchCarsAllCommercial",
                        "desktop/OfferCarsRegisterCallbackForNewCategory"},
                {CARS, ALL, "CONTACT_CARS_DEALER_NEW", "mobile/SearchCarsAllCommercial",
                        "desktop/OfferCarsRegisterCallbackForNewCategory"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsEmpty",
                "desktop/SessionAuthUser",
                "desktop/User",
                listingMock,
                callbackMock).post();

        urlSteps.testing().path(category).path(state).addParam("seller_group", "COMMERCIAL").open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Метрики при клике на «Заказать обратный звонок» в галерее")
    public void shouldSendMetrics() {
        basePageSteps.onListingPage().getSale(TOP_SALES_COUNT).hover();
        basePageSteps.onListingPage().getSale(TOP_SALES_COUNT).gallery().contacts().hover().click();
        basePageSteps.onListingPage().popup().button("Заказать обратный звонок").hover().click();
        basePageSteps.onListingPage().callbackPopup().waitUntil(isDisplayed());
        basePageSteps.onListingPage().callbackPopup().button("Перезвоните мне").waitUntil(isDisplayed()).hover()
                .click();

        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasGoal(
                seleniumMockSteps.formatGoal(goal),
                urlSteps.getCurrentUrl()
        )));
    }
}
