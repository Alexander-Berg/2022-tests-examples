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
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.METRICS;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.QueryParams.AUTORU_BILLING_SERVICE_TYPE;
import static ru.auto.tests.desktop.consts.QueryParams.CAROUSEL;
import static ru.auto.tests.desktop.consts.QueryParams.EXTENDED;
import static ru.auto.tests.desktop.consts.QueryParams.OUTPUT_TYPE;
import static ru.auto.tests.desktop.matchers.RequestHasQueryItemsMatcher.hasGoal;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneMetricsRequest;
import static ru.auto.tests.desktop.mock.MockStub.stub;

@DisplayName("Метрики - цели - телефон в листинге")
@Feature(METRICS)
@GuiceModules(DesktopDevToolsTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ListingPhoneGoalsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    public SeleniumMockSteps seleniumMockSteps;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    public BasePageSteps basePageSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String goal;

    @Parameterized.Parameter(2)
    public String breacdrumbsMock;

    @Parameterized.Parameter(3)
    public String searchMock;

    @Parameterized.Parameter(4)
    public String phonesMock;

    @Parameterized.Parameters(name = "name = {index}: {0} {1} {2}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, "PHONE_CARS2_ALL", "desktop/SearchCarsBreadcrumbsEmpty",
                        "desktop/SearchCarsExtended", "desktop/OfferCarsPhones"},
                {CARS, "PHONE_CL_CARS2", "desktop/SearchCarsBreadcrumbsEmpty",
                        "desktop/SearchCarsExtended", "desktop/OfferCarsPhones"},
                {CARS, "PHONE_NEW_PREMIUM", "desktop/SearchCarsBreadcrumbsEmpty",
                        "desktop/SearchCarsExtended", "desktop/OfferCarsPhones"},
                {CARS, "PHONE_NEW_CL_CARS2", "desktop/SearchCarsBreadcrumbsEmpty",
                        "desktop/SearchCarsExtended", "desktop/OfferCarsPhones"},
                {CARS, "CONTACT_CARS", "desktop/SearchCarsBreadcrumbsEmpty",
                        "desktop/SearchCarsExtended", "desktop/OfferCarsPhones"},
                {CARS, "CONTACT_CARS_DESKTOP", "desktop/SearchCarsBreadcrumbsEmpty",
                        "desktop/SearchCarsExtended", "desktop/OfferCarsPhones"},
                {CARS, "CONTACT_CARS_DEALER_NEW", "desktop/SearchCarsBreadcrumbsEmpty",
                        "desktop/SearchCarsExtended", "desktop/OfferCarsPhones"},
                {CARS, "CONTACT_CARS_PHONE", "desktop/SearchCarsBreadcrumbsEmpty",
                        "desktop/SearchCarsExtended", "desktop/OfferCarsPhones"},
                {CARS, "CONTACT_CARS_PHONE_NEW", "desktop/SearchCarsBreadcrumbsEmpty",
                        "desktop/SearchCarsExtended", "desktop/OfferCarsPhones"},
                {CARS, "PHONE_ALL_CARS2_PRICE-1500-HIGH", "desktop/SearchCarsBreadcrumbsEmpty",
                        "desktop/SearchCarsExtended", "desktop/OfferCarsPhones"},

                {TRUCK, "PHONE_COMMERCIAL_ALL", "desktop/SearchTrucksBreadcrumbs",
                        "desktop/SearchTrucksExtended", "desktop/OfferTrucksPhones"},
                {TRUCK, "PHONE_COMMERCIAL_CLIENT", "desktop/SearchTrucksBreadcrumbs",
                        "desktop/SearchTrucksExtended", "desktop/OfferTrucksPhones"},
                {TRUCK, "PHONE_USED_CL_COMMERCIAL", "desktop/SearchTrucksBreadcrumbs",
                        "desktop/SearchTrucksExtended", "desktop/OfferTrucksPhones"},
                {TRUCK, "PHONE_COMMERCIAL_HCV_DEALER", "desktop/SearchTrucksBreadcrumbs",
                        "desktop/SearchTrucksExtended", "desktop/OfferTrucksPhones"},

                {MOTORCYCLE, "PHONE_MOTO_ALL", "desktop/SearchMotoBreadcrumbs",
                        "desktop/SearchMotoExtended", "desktop/OfferMotoPhones"},
                {MOTORCYCLE, "PHONE_MOTO_CLIENT", "desktop/SearchMotoBreadcrumbs",
                        "desktop/SearchMotoExtended", "desktop/OfferMotoPhones"},
                {MOTORCYCLE, "PHONE_USED_CL_MOTO", "desktop/SearchMotoBreadcrumbs",
                        "desktop/SearchMotoExtended", "desktop/OfferMotoPhones"}
        });
    }

    @Before
    public void before() {
        mockRule.setStubs(
                stub(breacdrumbsMock),
                stub(searchMock),
                stub(phonesMock)
        ).create();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отправка метрики")
    public void shouldSendMetrics() {
        urlSteps.testing().path(MOSKVA).path(category).path(ALL)
                .addParam(AUTORU_BILLING_SERVICE_TYPE, EXTENDED).open();

        basePageSteps.onListingPage().getSale(0).showPhonesButton().hover().click();

        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(
                hasGoal(seleniumMockSteps.formatGoal(goal))
        ));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отправка метрики, тип листинга «Карусель»")
    public void shouldSendMetricsCarouselListing() {
        urlSteps.testing().path(MOSKVA).path(category).path(ALL)
                .addParam(AUTORU_BILLING_SERVICE_TYPE, EXTENDED).addParam(OUTPUT_TYPE, CAROUSEL).open();

        basePageSteps.onListingPage().getCarouselSale(0).showPhonesButton().hover().click();

        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(
                hasGoal(seleniumMockSteps.formatGoal(goal))
        ));
    }

}
