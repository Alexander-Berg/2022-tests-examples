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
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.LCV;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.matchers.RequestHasQueryItemsMatcher.hasGoal;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneMetricsRequest;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Метрики - цели - телефон")
@Feature(METRICS)
@GuiceModules(MobileDevToolsTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SalePhoneGoalsTest {

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
    public String phonesMock;

    @Parameterized.Parameter(4)
    public String goal;

    @Parameterized.Parameters(name = "name = {index}: {0} {1} {4}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][] {
                //User
                {CARS, USED, "desktop/OfferCarsUsedUser", "desktop/OfferCarsPhones", "MAUTORU_PHONE_CARS_SHOW"},
                {CARS, USED, "desktop/OfferCarsUsedUser", "desktop/OfferCarsPhones", "MAUTORU_PHONE_CARS_SHOW_PRIV"},
                {CARS, USED, "desktop/OfferCarsUsedUser", "desktop/OfferCarsPhones", "CONTACT_CARS"},
                {CARS, USED, "desktop/OfferCarsUsedUser", "desktop/OfferCarsPhones", "CONTACT_CARS_MOBILE"},
                {CARS, USED, "desktop/OfferCarsUsedUser", "desktop/OfferCarsPhones", "CONTACT_CARS_PHONE"},
                {CARS, USED, "desktop/OfferCarsUsedUser", "desktop/OfferCarsPhones", "CONTACT_CARS_REGULAR"},
                {CARS, USED, "desktop/OfferCarsUsedUser", "desktop/OfferCarsPhones", "CONTACT_CARS_PHONE_USED"},

                {MOTORCYCLE, USED, "desktop/OfferMotoUsedUser", "desktop/OfferMotoPhones", "MAUTORU_PHONE_MOTO_SHOW"},

                {TRUCK, USED, "desktop/OfferTrucksUsedUser", "desktop/OfferTrucksPhones", "MAUTORU_PHONE_TRUCKS_SHOW"},
                {TRUCK, USED, "desktop/OfferTrucksUsedUser", "desktop/OfferTrucksPhones",
                        "MAUTORU_PHONE_TRUCKS_SHOW_PRIV"},
                {TRUCK, USED, "desktop/OfferTrucksUsedUser", "desktop/OfferTrucksPhones", "PHONE_COMMERCIAL_HCV_USER"},

                {LCV, USED, "desktop/OfferTrucksUsedUserLcv", "desktop/OfferTrucksPhones", "MAUTORU_PHONE_TRUCKS_SHOW"},
                {LCV, USED, "desktop/OfferTrucksUsedUserLcv", "desktop/OfferTrucksPhones",
                        "MAUTORU_PHONE_TRUCKS_SHOW_PRIV"},
                {LCV, USED, "desktop/OfferTrucksUsedUserLcv", "desktop/OfferTrucksPhones", "PHONE_COMMERCIAL_LCV_USER"},

                //Dealer
                {CARS, USED, "desktop/OfferCarsUsedDealer", "desktop/OfferCarsPhones", "MAUTORU_PHONE_CARS_SHOW"},
                {CARS, USED, "desktop/OfferCarsUsedDealer", "desktop/OfferCarsPhones",
                        "MAUTORU_PHONE_CARS_SHOW_DEALER"},
                {CARS, USED, "desktop/OfferCarsUsedDealer", "desktop/OfferCarsPhones", "CONTACT_CARS"},
                {CARS, USED, "desktop/OfferCarsUsedDealer", "desktop/OfferCarsPhones", "CONTACT_CARS_MOBILE"},
                {CARS, USED, "desktop/OfferCarsUsedDealer", "desktop/OfferCarsPhones", "CONTACT_CARS_PHONE"},
                {CARS, USED, "desktop/OfferCarsUsedDealer", "desktop/OfferCarsPhones", "CONTACT_CARS_DEALER_USED"},
                {CARS, USED, "desktop/OfferCarsUsedDealer", "desktop/OfferCarsPhones", "CONTACT_CARS_PHONE_USED"},

                {CARS, NEW, "desktop/OfferCarsNewDealer", "desktop/OfferCarsPhones", "MAUTORU_PHONE_CARS_SHOW"},
                {CARS, NEW, "desktop/OfferCarsNewDealer", "desktop/OfferCarsPhones", "MAUTORU_PHONE_CARS_SHOW_DEALER"},
                {CARS, NEW, "desktop/OfferCarsNewDealer", "desktop/OfferCarsPhones", "MAUTORU_PHONE_NEW_CL_CARS2"},
                {CARS, NEW, "desktop/OfferCarsNewDealer", "desktop/OfferCarsPhones", "CONTACT_CARS"},
                {CARS, NEW, "desktop/OfferCarsNewDealer", "desktop/OfferCarsPhones", "CONTACT_CARS_MOBILE"},
                {CARS, NEW, "desktop/OfferCarsNewDealer", "desktop/OfferCarsPhones", "CONTACT_CARS_PHONE"},
                {CARS, NEW, "desktop/OfferCarsNewDealer", "desktop/OfferCarsPhones", "CONTACT_CARS_DEALER_NEW"},

                {MOTORCYCLE, USED, "desktop/OfferMotoUsedDealer", "desktop/OfferMotoPhones", "MAUTORU_PHONE_MOTO_SHOW"},

                {MOTORCYCLE, NEW, "desktop/OfferMotoNew", "desktop/OfferMotoPhones", "MAUTORU_PHONE_MOTO_SHOW"},

                {TRUCK, USED, "desktop/OfferTrucksUsedDealer", "desktop/OfferTrucksPhones",
                        "MAUTORU_PHONE_TRUCKS_SHOW"},
                {TRUCK, USED, "desktop/OfferTrucksUsedDealer", "desktop/OfferTrucksPhones",
                        "MAUTORU_PHONE_TRUCKS_SHOW_DEALER"},
                {TRUCK, USED, "desktop/OfferTrucksUsedDealer", "desktop/OfferTrucksPhones",
                        "PHONE_COMMERCIAL_HCV_DEALER"},

                {TRUCK, NEW, "desktop/OfferTrucksNew", "desktop/OfferTrucksPhones", "MAUTORU_PHONE_TRUCKS_SHOW"},
                {TRUCK, NEW, "desktop/OfferTrucksNew", "desktop/OfferTrucksPhones", "MAUTORU_PHONE_TRUCKS_SHOW_DEALER"},
                {TRUCK, NEW, "desktop/OfferTrucksNew", "desktop/OfferTrucksPhones", "PHONE_COMMERCIAL_HCV_DEALER"},

                {LCV, USED, "desktop/OfferTrucksUsedDealerLcv", "desktop/OfferTrucksPhones",
                        "MAUTORU_PHONE_TRUCKS_SHOW"},
                {LCV, USED, "desktop/OfferTrucksUsedDealerLcv", "desktop/OfferTrucksPhones",
                        "MAUTORU_PHONE_TRUCKS_SHOW_DEALER"},
                {LCV, USED, "desktop/OfferTrucksUsedDealerLcv", "desktop/OfferTrucksPhones",
                        "PHONE_COMMERCIAL_LCV_DEALER"},

                {LCV, NEW, "desktop/OfferTrucksNewLcv", "desktop/OfferTrucksPhones", "MAUTORU_PHONE_TRUCKS_SHOW"},
                {LCV, NEW, "desktop/OfferTrucksNewLcv", "desktop/OfferTrucksPhones",
                        "MAUTORU_PHONE_TRUCKS_SHOW_DEALER"},
                {LCV, NEW, "desktop/OfferTrucksNewLcv", "desktop/OfferTrucksPhones", "PHONE_COMMERCIAL_LCV_DEALER"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with(saleMock,
                phonesMock).post();

        urlSteps.testing().path(category).path(state).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Метрики при клике на «Показать телефон»")
    public void shouldSendMetrics() {
        basePageSteps.onCardPage().floatingContacts().callButton().should(isDisplayed()).click();

        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasGoal(
                seleniumMockSteps.formatGoal(goal),
                urlSteps.getCurrentUrl()
        )));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Метрики при клике на «Показать телефон» в галерее")
    public void shouldSendMetricsInGallery() {
        basePageSteps.onCardPage().gallery().getItem(0).click();
        basePageSteps.onCardPage().fullScreenGallery().callButton().click();

        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasGoal(
                seleniumMockSteps.formatGoal(goal),
                urlSteps.getCurrentUrl()
        )));
    }
}
