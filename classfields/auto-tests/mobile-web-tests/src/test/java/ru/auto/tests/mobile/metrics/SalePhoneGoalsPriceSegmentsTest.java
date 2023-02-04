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
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.matchers.RequestHasQueryItemsMatcher.hasGoal;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneMetricsRequest;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Метрики - цели - телефон")
@Feature(METRICS)
@GuiceModules(MobileDevToolsTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SalePhoneGoalsPriceSegmentsTest {

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

    @Parameterized.Parameters(name = "name = {index}: {0} {3}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][] {
                //User
                {CARS, USED, "desktop/OfferCarsUsedUser", "PHONE_ALL_CARS2_PRICE-500-1500"},

                //Dealer
                {CARS, USED, "desktop/OfferCarsUsedDealer", "PHONE_ALL_CARS2_PRICE-300-500"},

                {CARS, NEW, "desktop/OfferCarsNewDealer", "PHONE_ALL_CARS2_PRICE-500-1500"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with(saleMock,
                "desktop/OfferCarsPhones").post();

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
