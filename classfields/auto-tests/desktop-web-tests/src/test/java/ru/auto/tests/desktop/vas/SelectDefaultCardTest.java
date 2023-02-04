package ru.auto.tests.desktop.vas;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.VAS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Выбор карты для постоянной оплаты")
@Feature(VAS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class SelectDefaultCardTest {

    private static final String SALE_ID = "/1076842087-f1e84/";
    private static final String FIRST_CARD_NUMBER = "5555 55** **** 4444";
    private static final String SECOND_CARD_NUMBER = "4111 11** **** 1111";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/OfferCarsUsedUserOwner",
                "desktop/SessionAuthUser",
                "desktop/BillingAutoruPaymentInitTurboTiedCards",
                "desktop/BillingAutoruTiedCards",
                "desktop/BillingAutoruPaymentProcessCarsSale",
                "desktop/BillingAutoruPayment").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Category({Regression.class, Testing.class, Billing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Выбор карты для постоянной оплаты в поп-апе оплаты")
    public void shouldSelectDefaultCardInBillingPopup() {
        basePageSteps.onCardPage().cardVas().buyButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().switchToBillingFrame();
        basePageSteps.onCardPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.onCardPage().billingPopup().select(FIRST_CARD_NUMBER).click();
        basePageSteps.onCardPage().billingPopup().selectPopup().itemsList().should(hasSize(3));
        basePageSteps.onCardPage().billingPopup().selectPopup().getItem(0).should(hasText(FIRST_CARD_NUMBER));
        basePageSteps.onCardPage().billingPopup().selectPopup().getItem(1).should(hasText(SECOND_CARD_NUMBER));
        basePageSteps.onCardPage().billingPopup().selectPopup().getItem(2).should(hasText("Добавить карту"));
        basePageSteps.onCardPage().billingPopup().selectPopup().item(FIRST_CARD_NUMBER).click();
        basePageSteps.onCardPage().billingPopup().checkbox("Всегда оплачивать с этой карты").click();
        basePageSteps.onCardPage().billingPopup().checkboxChecked("Всегда оплачивать с этой карты")
                .waitUntil(isDisplayed());
    }
}