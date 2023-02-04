package ru.auto.tests.mobile.my;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
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
import static ru.auto.tests.desktop.consts.AutoruFeatures.LK;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("ЛК - активация похожего объявления легковых")
@Epic(LK)
@Feature(SALES)
@Story("Активация объявления")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class ActivateSameSaleCarsTest {

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
                "desktop/User",
                "desktop/UserOffersCarsInactive").post();

        urlSteps.testing().path(MY).path(CARS).open();

        mockRule.delete();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Активация объявления")
    public void shouldActivateSale() {
        mockRule.newMock().with("desktop/User",
                "desktop/SessionAuthUser",
                "desktop/UserOffersCarsActivateSameSaleReseller",
                "mobile/BillingAutoruPaymentInitActivateSameSale",
                "mobile/BillingAutoruPaymentProcessLk",
                "desktop/BillingAutoruPayment",
                "desktop/UserOffersCarsActive").post();

        basePageSteps.onLkPage().getSale(0).button("Активировать").click();
        basePageSteps.onLkPage().sameSalePopup().waitUntil(isDisplayed()).should(hasText("Вы уже размещали объявление " +
                "о продаже этого авто недавно. Повторное размещение на 60 дней стоит 1 199 ₽. " +
                "Старое объявление вы можете восстановить бесплатно.\nВосстановить старое\n" +
                "Разместить за\n1 199 ₽"));
        basePageSteps.onLkPage().sameSalePopup().button("Разместить за 1\u00a0199\u00a0₽").click();
        basePageSteps.onCardPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.switchToPaymentMethodsFrame();
        basePageSteps.switchToDefaultFrame();
        basePageSteps.clickPayButton();
        paymentSteps.payByCard();
        paymentSteps.waitForSuccessMessage();
        basePageSteps.onLkPage().notifier().waitUntil(isDisplayed()).should(hasText("Объявление активировано"));
        basePageSteps.onLkPage().sameSalePopup().waitUntil(not(isDisplayed()));
        basePageSteps.onLkPage().getSale(0).button("Активировать").waitUntil(not(isDisplayed()));
        basePageSteps.onLkPage().getSale(0).button("Снять с продажи").waitUntil(isDisplayed());
        basePageSteps.onLkPage().vas().waitUntil(isDisplayed());
    }

    @Test
    @Ignore
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Восстановление старого объявления (бесплатно)")
    public void shouldRestoreSaleFree() {
        mockRule.newMock().with("desktop/User",
                "desktop/SessionAuthUser",
                "desktop/UserOffersCarsActivateSameSaleFree",
                "desktop/UserOffersCarsActive").post();

        basePageSteps.onLkPage().getSale(0).button("Активировать").click();
        basePageSteps.onLkPage().sameSalePopup().button("Восстановить старое").click();
        basePageSteps.onLkPage().sameSalePopup().waitUntil(not(isDisplayed()));
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed()).should(hasText("Платное размещение\n" +
                "Вы уже истратили все бесплатные объявления и теперь можете размещаться только платно.\nРазместить бесплатно"));
        basePageSteps.onCardPage().popup().button("Разместить бесплатно").click();
        basePageSteps.onLkPage().notifier().waitUntil(isDisplayed()).should(hasText("Объявление активировано"));
        basePageSteps.onLkPage().getSale(0).button("Активировать").waitUntil(not(isDisplayed()));
        basePageSteps.onLkPage().getSale(0).button("Снять с продажи").waitUntil(isDisplayed());
        basePageSteps.onLkPage().vas().waitUntil(isDisplayed());
    }

    @Test
    @Ignore
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Восстановление старого объявления (платно)")
    public void shouldRestoreSaleReseller() {
        mockRule.newMock().with("desktop/User",
                "desktop/SessionAuthUser",
                "desktop/UserOffersCarsActivateSameSaleReseller",
                "mobile/BillingAutoruPaymentInitActivateSameSale",
                "mobile/BillingAutoruPaymentProcessLk",
                "desktop/BillingAutoruPayment",
                "desktop/UserOffersCarsActive").post();

        basePageSteps.onLkPage().getSale(0).button("Активировать").click();
        basePageSteps.onLkPage().sameSalePopup().button("Восстановить старое").click();
        basePageSteps.onCardPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.switchToPaymentMethodsFrame();
        basePageSteps.switchToDefaultFrame();
        basePageSteps.clickPayButton();
        paymentSteps.payByCard();
        paymentSteps.waitForSuccessMessage();
        basePageSteps.onLkPage().notifier().waitUntil(isDisplayed()).should(hasText("Объявление активировано"));
        basePageSteps.onLkPage().sameSalePopup().waitUntil(not(isDisplayed()));
        basePageSteps.onLkPage().getSale(0).button("Активировать").waitUntil(not(isDisplayed()));
        basePageSteps.onLkPage().getSale(0).button("Снять с продажи").waitUntil(isDisplayed());
        basePageSteps.onLkPage().vas().waitUntil(isDisplayed());
    }
}
