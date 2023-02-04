package ru.auto.tests.mobile.vas;

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
import ru.auto.tests.desktop.categories.Billing;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.mobile.step.PaymentSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import static org.hamcrest.Matchers.startsWith;
import static ru.auto.tests.desktop.consts.AutoruFeatures.VAS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Покупка VAS на карточке через Webmoney")
@Feature(VAS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class VasSaleFreshCarsWebmoneyTest {

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
        mockRule.newMock().with("desktop/OfferCarsUsedUserOwner",
                "desktop/SessionAuthUser",
                "mobile/BillingAutoruPaymentInitFresh",
                "mobile/BillingAutoruPaymentProcessSaleWebmoney").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().vas().service("Поднять в поиске за 297\u00a0₽")
                .title());
        basePageSteps.selectPaymentMethod("Webmoney");
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Должен открыться правильный урл")
    public void shouldSeeCorrectUrl() throws InterruptedException {
        paymentSteps.clickPayButton();
        urlSteps.switchToNextTab();
        urlSteps.shouldUrl(startsWith("https://merchant.webmoney.ru/lmi/payment.asp"));
    }

    @Test
    @Category({Regression.class, Testing.class, Billing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Покупка услуги")
    public void shouldBuyVas() throws InterruptedException {
        mockRule.with("desktop/BillingAutoruPayment").update();

        basePageSteps.clickPayButton();
        paymentSteps.waitForSuccessMessage();
        basePageSteps.onCardPage().notifier().waitUntil(hasText("Опция успешно активирована"));
    }
}
