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
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.YaKassaSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.VAS;
import static ru.auto.tests.desktop.consts.Owners.KRISKOLU;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.step.CookieSteps.FORCE_DISABLE_TRUST;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Карточка объявления - покупка пакета VIP")
@Feature(VAS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class VasBuyVIPCarsTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private YaKassaSteps yaKassaSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        cookieSteps.setExpFlags(FORCE_DISABLE_TRUST);

        mockRule.newMock().with("desktop/OfferCarsUsedUserOwnerWithVip",
                "desktop/SessionAuthUser",
                "desktop/BillingAutoruPaymentInitVip",
                "desktop/BillingAutoruPaymentProcessCarsSale",
                "desktop/BillingAutoruPayment").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();

        mockRule.overwriteStub(0, "desktop/OfferCarsUsedUserOwnerWithVipPaid");
    }

    @Test
    @Category({Regression.class, Testing.class, Billing.class})
    @Owner(KRISKOLU)
    @DisplayName("Покупка услуги")
    public void shouldBuyVIP() {
        basePageSteps.onCardPage().cardVas().buyButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().switchToBillingFrame();
        basePageSteps.onCardPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.onCardPage().billingPopup().checkbox("Запомнить карту").click();
        basePageSteps.onCardPage().billingPopup().header().waitUntil(hasText("Пакет VIP"));
        basePageSteps.onCardPage().billingPopup().priceHeader().waitUntil(hasText("4 475 \u20BD"));
        yaKassaSteps.payWithCard();
        yaKassaSteps.waitForSuccessMessage();
        basePageSteps.onCardPage().notifier().waitUntil(isDisplayed()).should(hasText("Пакет «VIP» подключён"));
        basePageSteps.onCardPage().billingPopupCloseButton().click();
        basePageSteps.onCardPage().billingPopupFrame().waitUntil(not(isDisplayed()));
        basePageSteps.onCardPage().cardHeader().should(isDisplayed());
        basePageSteps.onCardPage().cardVas().should(not(isDisplayed()));
    }
}
