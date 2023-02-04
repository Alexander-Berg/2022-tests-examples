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
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.matchers.RequestHasQueryItemsMatcher.hasGoal;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneMetricsRequest;
import static ru.auto.tests.desktop.mobile.page.ListingPage.TOP_SALES_COUNT;

@DisplayName("Метрики - цели - телефон в листинге")
@Feature(METRICS)
@GuiceModules(MobileDevToolsTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ListingPhoneGoalsDealerTest {

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
    public String listingMock;

    @Parameterized.Parameter(3)
    public String breadcrumbsMock;

    @Parameterized.Parameter(4)
    public String goal;

    @Parameterized.Parameters(name = "name = {index}: {0} {1} {4}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][] {
                {CARS, USED, "mobile/SearchCarsUsedCommercial", "desktop/SearchCarsBreadcrumbsEmpty",
                        "CONTACT_CARS"},
                {CARS, USED, "mobile/SearchCarsUsedCommercial", "desktop/SearchCarsBreadcrumbsEmpty",
                        "CONTACT_CARS_MOBILE"},
                {CARS, USED, "mobile/SearchCarsUsedCommercial", "desktop/SearchCarsBreadcrumbsEmpty",
                        "CONTACT_CARS_DEALER_USED"},
                {CARS, USED, "mobile/SearchCarsUsedCommercial", "desktop/SearchCarsBreadcrumbsEmpty",
                        "CONTACT_CARS_PHONE"},
                {CARS, USED, "mobile/SearchCarsUsedCommercial", "desktop/SearchCarsBreadcrumbsEmpty",
                        "CONTACT_CARS_PHONE_USED"},
                {CARS, USED, "mobile/SearchCarsUsedCommercial", "desktop/SearchCarsBreadcrumbsEmpty",
                        "MAUTORU_PHONE_CARS_SHOW"},
                {CARS, USED, "mobile/SearchCarsUsedCommercial", "desktop/SearchCarsBreadcrumbsEmpty",
                        "MAUTORU_PHONE_CARS_SHOW_DEALER"},

                {CARS, ALL, "mobile/SearchCarsAllCommercial", "desktop/SearchCarsBreadcrumbsEmpty",
                        "CONTACT_CARS"},
                {CARS, ALL, "mobile/SearchCarsAllCommercial", "desktop/SearchCarsBreadcrumbsEmpty",
                        "CONTACT_CARS_MOBILE"},
                {CARS, ALL, "mobile/SearchCarsAllCommercial", "desktop/SearchCarsBreadcrumbsEmpty",
                        "CONTACT_CARS_DEALER_NEW"},
                {CARS, ALL, "mobile/SearchCarsAllCommercial", "desktop/SearchCarsBreadcrumbsEmpty",
                        "CONTACT_CARS_PHONE"},
                {CARS, ALL, "mobile/SearchCarsAllCommercial", "desktop/SearchCarsBreadcrumbsEmpty",
                        "CONTACT_CARS_PHONE_NEW"},
                {CARS, ALL, "mobile/SearchCarsAllCommercial", "desktop/SearchCarsBreadcrumbsEmpty",
                        "MAUTORU_PHONE_CARS_SHOW"},
                {CARS, ALL, "mobile/SearchCarsAllCommercial", "desktop/SearchCarsBreadcrumbsEmpty",
                        "MAUTORU_PHONE_CARS_SHOW_DEALER"},

                {TRUCK, ALL, "mobile/SearchTrucksAllCommercial", "desktop/SearchTrucksBreadcrumbsEmpty",
                        "MAUTORU_PHONE_TRUCKS_SHOW"},
                {TRUCK, ALL, "mobile/SearchTrucksAllCommercial", "desktop/SearchTrucksBreadcrumbsEmpty",
                        "MAUTORU_PHONE_TRUCKS_SHOW_DEALER"},
                {TRUCK, ALL, "mobile/SearchTrucksAllCommercial", "desktop/SearchTrucksBreadcrumbsEmpty",
                        "PHONE_COMMERCIAL_HCV_DEALER"},

                {MOTORCYCLE, ALL, "mobile/SearchMotoAllCommercial", "desktop/SearchMotoBreadcrumbsEmpty",
                        "MAUTORU_PHONE_MOTO_SHOW"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with(breadcrumbsMock,
                listingMock,
                "desktop/OfferCarsPhones").post();

        urlSteps.testing().path(category).path(state).addParam("seller_group", "COMMERCIAL").open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Метрики при клике на «Показать телефон» на топ-объявлении")
    public void shouldSendMetrics() {
        basePageSteps.onListingPage().getSale(0).callButton().click();

        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasGoal(
                seleniumMockSteps.formatGoal(goal),
                urlSteps.getCurrentUrl()
        )));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Метрики при клике на «Позвонить» в галерее")
    public void shouldSendMetricsInGallery() {
        basePageSteps.onListingPage().getSale(TOP_SALES_COUNT).hover();
        basePageSteps.onListingPage().getSale(TOP_SALES_COUNT).gallery().callButton().click();

        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasGoal(
                seleniumMockSteps.formatGoal(goal),
                urlSteps.getCurrentUrl()
        )));
    }
}
