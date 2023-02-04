package ru.auto.tests.mobile.vas;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
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
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Покупка VAS на карточке")
@Feature(VAS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class VasSaleExpressMotoTest {

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
        mockRule.newMock().with("desktop/OfferMotoUsedUserOwner",
                "desktop/SessionAuthUser",
                "mobile/BillingAutoruPaymentInitExpress",
                "mobile/BillingAutoruPaymentProcessSaleMoto",
                "desktop/BillingAutoruPayment").post();

        urlSteps.testing().path(MOTORCYCLE).path(USED).path(SALE).path(SALE_ID).open();

        mockRule.overwriteStub(0, "desktop/OfferMotoUsedUserOwnerWithServices");
    }

    @Test
    @Category({Regression.class, Testing.class, Billing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Покупка услуги")
    public void shouldBuyVas() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().vas().service("Экспресс-продажа").buyButton());
        buyVas();
    }

    @Test
    @Category({Regression.class, Testing.class, Billing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Покупка услуги в поп-апе")
    public void shouldBuyVasInPopup() {
        basePageSteps.openVasPopup("Экспресс-продажа");
        buyVas();
    }

    @Step("Покупаем VAS")
    private void buyVas() {
        basePageSteps.clickPayButton();
        paymentSteps.payByCard();
        paymentSteps.waitForSuccessMessage();
        basePageSteps.onCardPage().notifier().waitUntil(hasText("Опция успешно активирована"));
        basePageSteps.onCardPage().vas().activeService("Поднятие в ТОП")
                .waitUntil(hasText("Поднятие в ТОП\nДействует 3 дня\nдо окончания"));
    }
}
