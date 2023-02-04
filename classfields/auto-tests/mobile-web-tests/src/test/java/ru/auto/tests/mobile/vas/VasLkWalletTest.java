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

@DisplayName("Личный кабинет. Покупка услуг продвижения. Оплата кошельком")
@Feature(VAS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class VasLkWalletTest {

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
                "mobile/BillingAutoruPaymentInitTurboLkWallet",
                "mobile/BillingAutoruPaymentProcessLkWallet",
                "desktop/BillingAutoruPayment").post();

        urlSteps.testing().path(MY).path(CARS).open();

        mockRule.overwriteStub(1, "desktop/UserOffersCarsWithServices");

        basePageSteps.openVasPopup("Турбо-продажа");
    }

    @Test
    @Category({Regression.class, Testing.class, Billing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Покупка услуги")
    public void shouldBuyVas() {
        basePageSteps.switchToPaymentMethodsFrame();
        basePageSteps.onBasePage().paymentMethodsFrameContent().should(hasText("Турбо продажа\n" +
                "Автоматически продлевать\n897 ₽ каждые 3 дня\nКошелёк · 1 001 ₽\nИзменить\nОплатить 897 ₽\n" +
                "Совершая платеж, вы соглашаетесь с условиями Оферты"));
        basePageSteps.switchToDefaultFrame();
        basePageSteps.clickPayButton();
        paymentSteps.waitForSuccessMessage();
        basePageSteps.onLkPage().notifier().waitUntil(isDisplayed()).should(hasText("Опция успешно активирована"));
        basePageSteps.onLkPage().vas().service("Турбо-продажа").status()
                .should(hasText("20 просмотров · Включено 3 опции · 3 дня"));
    }

    @Test
    @Category({Regression.class, Testing.class, Billing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Смена способа оплаты на другой")
    public void shouldChangePaymentMethod() {
        basePageSteps.switchToPaymentMethodsFrame();
        basePageSteps.onLkPage().paymentMethodsFrameContent().button("Изменить").click();
        basePageSteps.onLkPage().paymentMethodsFrameContent().waitUntil(hasText("Турбо продажа\nАвтоматически " +
                "продлевать\n897 ₽ каждые 3 дня\nКошелёк · 1 001 ₽\nVisa **** 1111\nСбербанк Онлайн\nЮMoney\n" +
                "QIWI Кошелек\nWebmoney\nНовая карта\nОплатить 897 ₽\nСовершая платеж, вы соглашаетесь с условиями " +
                "Оферты"));
    }
}
