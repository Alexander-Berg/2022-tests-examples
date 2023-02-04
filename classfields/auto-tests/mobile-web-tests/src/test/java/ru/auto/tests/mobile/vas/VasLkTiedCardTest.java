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

import static ru.auto.tests.desktop.consts.AutoruFeatures.VAS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Личный кабинет. Покупка услуги, оплата привязанной картой")
@Feature(VAS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class VasLkTiedCardTest {

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
        mockRule.newMock().with("desktop/User",
                "desktop/SessionAuthUser",
                "desktop/UserOffersCarsActive",
                "mobile/BillingAutoruPaymentInitLkTurboTiedCard",
                "mobile/BillingAutoruPaymentProcessLkTiedCard",
                "desktop/BillingAutoruPayment").post();

        urlSteps.testing().path(MY).path(CARS).open();

        mockRule.overwriteStub(1, "desktop/UserOffersCarsActiveWithServices");
    }

    @Test
    @Category({Regression.class, Testing.class, Billing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Покупка услуги")
    public void shouldBuyVas() {
        basePageSteps.openVasPopup("Турбо-продажа");
        basePageSteps.switchToPaymentMethodsFrame();
        basePageSteps.onBasePage().paymentMethodsFrameContent().should(hasText("Турбо продажа\nАвтоматически " +
                "продлевать\n897 ₽ каждые 3 дня\nVisa **** 1111\nИзменить\nОплатить 897 ₽\nСовершая платеж, " +
                "вы соглашаетесь с условиями Оферты"));
        basePageSteps.switchToDefaultFrame();
        basePageSteps.clickPayButton();
        paymentSteps.waitForSuccessMessage();
        basePageSteps.onLkPage().notifier().waitUntil(isDisplayed()).should(hasText("Опция успешно активирована"));
        basePageSteps.onLkPage().vas().service("Турбо-продажа").activePeriod().should(hasText("до окончания"));
    }
}
