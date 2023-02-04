package ru.auto.tests.mobile.sale;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.mobile.step.PaymentSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.VAS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Карточка объявления - активация сверх квоты")
@Feature(VAS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class ActivateSaleCarsQuotaTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private PaymentSteps paymentSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/OfferCarsUsedUserOwnerInactiveNotFreeActivation",
                "desktop/UserOffersCarsActivateReseller").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Платная активация оффера")
    public void shouldActivateSale() throws InterruptedException {
        basePageSteps.onCardPage().ownerControls().button("Активировать").click();
        basePageSteps.onCardPage().vasPopup().waitUntil(hasText("Платное размещение\nБесплатный лимит отключён. Вы " +
                "можете размещать объявления только платно. Срок размещения — 60 дней.\nРазместить за 2 499 ₽"));
        mockRule.delete();
        mockRule.newMock().with("desktop/SessionAuthUser",
                "mobile/BillingAutoruPaymentInitActivate",
                "mobile/BillingAutoruPaymentProcessSaleCars",
                "desktop/BillingAutoruPayment",
                "desktop/OfferCarsUsedUserOwner").post();

        basePageSteps.onCardPage().vasPopup().button("Разместить за  2\u00a0499\u00a0₽").click();
        basePageSteps.clickPayButton();
        paymentSteps.payByCard();
        paymentSteps.waitForSuccessMessage();
        basePageSteps.onCardPage().notifier().waitUntil(isDisplayed()).should(hasText("Платёж прошёл"));

        basePageSteps.onCardPage().ownerControls().button("Активировать").waitUntil(not(isDisplayed()));
        basePageSteps.onCardPage().ownerControls().button("Снять с продажи").waitUntil(isDisplayed());
    }
}
