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
import static ru.auto.tests.desktop.consts.Pages.LCV;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.WindowSize.HEIGHT_1024;
import static ru.auto.tests.desktop.matchers.RequestHasQueryItemsMatcher.hasGoal;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneMetricsRequest;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Метрики - цели - телефон")
@Feature(METRICS)
@GuiceModules(DesktopDevToolsTestsModule.class)
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
    public String goal;

    @Parameterized.Parameters(name = "name = {index}: {0} {1} {3}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][] {
                //User
                {CARS, USED, "desktop/OfferCarsUsedUser", "PHONE_CARS2_ALL"},
                {CARS, USED, "desktop/OfferCarsUsedUser", "PHONE_US_CARS2"},
                {CARS, USED, "desktop/OfferCarsUsedUser", "CONTACT_CARS"},
                {CARS, USED, "desktop/OfferCarsUsedUser", "CONTACT_CARS_DESKTOP"},
                {CARS, USED, "desktop/OfferCarsUsedUser", "CONTACT_CARS_PHONE"},
                {CARS, USED, "desktop/OfferCarsUsedUser", "CONTACT_CARS_REGULAR"},
                {CARS, USED, "desktop/OfferCarsUsedUser", "CONTACT_CARS_PHONE_USED"},

                {MOTORCYCLE, USED, "desktop/OfferMotoUsedUser", "PHONE_MOTO_ALL"},
                {MOTORCYCLE, USED, "desktop/OfferMotoUsedUser", "PHONE_MOTO_USER"},

                {TRUCK, USED, "desktop/OfferTrucksUsedUser", "PHONE_COMMERCIAL_ALL"},
                {TRUCK, USED, "desktop/OfferTrucksUsedUser", "PHONE_COMMERCIAL_USER"},
                {TRUCK, USED, "desktop/OfferTrucksUsedUser", "PHONE_COMMERCIAL_HCV_USER"},

                {LCV, USED, "desktop/OfferTrucksUsedUserLcv", "PHONE_COMMERCIAL_ALL"},
                {LCV, USED, "desktop/OfferTrucksUsedUserLcv", "PHONE_COMMERCIAL_USER"},
                {LCV, USED, "desktop/OfferTrucksUsedUserLcv", "PHONE_COMMERCIAL_LCV_USER"},

                //Dealer
                {CARS, USED, "desktop/OfferCarsUsedDealer", "PHONE_CARS2_ALL"},
                {CARS, USED, "desktop/OfferCarsUsedDealer", "PHONE_CL_CARS2"},
                {CARS, USED, "desktop/OfferCarsUsedDealer", "PHONE_USED_CL_CARS2"},
                {CARS, USED, "desktop/OfferCarsUsedDealer", "CONTACT_CARS"},
                {CARS, USED, "desktop/OfferCarsUsedDealer", "CONTACT_CARS_DESKTOP"},
                {CARS, USED, "desktop/OfferCarsUsedDealer", "CONTACT_CARS_PHONE"},
                {CARS, USED, "desktop/OfferCarsUsedDealer", "CONTACT_CARS_DEALER_USED"},
                {CARS, USED, "desktop/OfferCarsUsedDealerExpensive", "CONTACT_CARS_DEALER_USED_PRICE-1000-1999"},
                {CARS, USED, "desktop/OfferCarsUsedDealer", "CONTACT_CARS_PHONE_USED"},

                {MOTORCYCLE, USED, "desktop/OfferMotoUsedDealer", "PHONE_MOTO_ALL"},
                {MOTORCYCLE, USED, "desktop/OfferMotoUsedDealer", "PHONE_MOTO_CLIENT"},
                {MOTORCYCLE, USED, "desktop/OfferMotoUsedDealer", "PHONE_USED_CL_MOTO"},

                {MOTORCYCLE, NEW, "desktop/OfferMotoNew", "PHONE_MOTO_ALL"},
                {MOTORCYCLE, NEW, "desktop/OfferMotoNew", "PHONE_MOTO_CLIENT"},
                {MOTORCYCLE, NEW, "desktop/OfferMotoNew", "PHONE_NEW_CL_MOTO"},

                {TRUCK, USED, "desktop/OfferTrucksUsedDealer", "PHONE_COMMERCIAL_ALL"},
                {TRUCK, USED, "desktop/OfferTrucksUsedDealer", "PHONE_COMMERCIAL_CLIENT"},
                {TRUCK, USED, "desktop/OfferTrucksUsedDealer", "PHONE_USED_CL_COMMERCIAL"},
                {TRUCK, USED, "desktop/OfferTrucksUsedDealer", "PHONE_COMMERCIAL_HCV_DEALER"},

                {TRUCK, NEW, "desktop/OfferTrucksNew", "PHONE_COMMERCIAL_ALL"},
                {TRUCK, NEW, "desktop/OfferTrucksNew", "PHONE_COMMERCIAL_CLIENT"},
                {TRUCK, NEW, "desktop/OfferTrucksNew", "PHONE_NEW_CL_COMMERCIAL"},
                {TRUCK, NEW, "desktop/OfferTrucksNew", "PHONE_COMMERCIAL_HCV_DEALER"},

                {LCV, USED, "desktop/OfferTrucksUsedDealerLcv", "PHONE_COMMERCIAL_ALL"},
                {LCV, USED, "desktop/OfferTrucksUsedDealerLcv", "PHONE_COMMERCIAL_CLIENT"},
                {LCV, USED, "desktop/OfferTrucksUsedDealerLcv", "PHONE_USED_CL_COMMERCIAL"},
                {LCV, USED, "desktop/OfferTrucksUsedDealerLcv", "PHONE_COMMERCIAL_LCV_DEALER"},

                {LCV, NEW, "desktop/OfferTrucksNewLcv", "PHONE_COMMERCIAL_ALL"},
                {LCV, NEW, "desktop/OfferTrucksNewLcv", "PHONE_COMMERCIAL_CLIENT"},
                {LCV, NEW, "desktop/OfferTrucksNewLcv", "PHONE_NEW_CL_COMMERCIAL"},
                {LCV, NEW, "desktop/OfferTrucksNewLcv", "PHONE_COMMERCIAL_LCV_DEALER"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with(saleMock,
                "desktop/OfferCarsPhones",
                "desktop/OfferMotoPhones",
                "desktop/OfferTrucksPhones").post();

        urlSteps.testing().path(category).path(state).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Цели при клике на «Показать телефон»")
    public void shouldSendGoals() {
        basePageSteps.onCardPage().contacts().showPhoneButton().should(isDisplayed()).click();

        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasGoal(
                seleniumMockSteps.formatGoal(goal),
                urlSteps.getCurrentUrl()
        )));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Цели при клике на «Показать телефон» в плавающей панели")
    public void shouldSendStickyBarGoals() {
        basePageSteps.scrollDown(1000);
        basePageSteps.onCardPage().stickyBar().waitUntil(isDisplayed());
        basePageSteps.onCardPage().stickyBar().showPhoneButton().click();

        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasGoal(
                seleniumMockSteps.formatGoal(goal),
                urlSteps.getCurrentUrl()
        )));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Цели при клике на «Показать телефон» в галерее")
    public void shouldSendGalleryGoals() {
        basePageSteps.setWideWindowSize(HEIGHT_1024);
        basePageSteps.onCardPage().gallery().currentImage().click();
        basePageSteps.onCardPage().fullScreenGallery().contacts().showPhoneButton().click();

        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(hasGoal(
                seleniumMockSteps.formatGoal(goal),
                urlSteps.getCurrentUrl()
        )));
    }
}
