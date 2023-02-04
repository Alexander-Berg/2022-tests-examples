package ru.auto.tests.mobile.deal;

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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.DEAL;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Безопасная сделка. Форма. Блок «Средства для оплаты»")
@Feature(AutoruFeatures.SAFE_DEAL)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class DealFormBuyerMoneyTest {

    private final static String DEAL_ID = "e033c078-0aed-464f-b781-b0618a0b34fe";

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

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/User",
                "desktop-lk/SafeDealDealGetWithBuyerDocuments",
                "desktop/SafeDealDealUpdateOfferPrice").post();

        urlSteps.testing().path(DEAL).path(DEAL_ID).open();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Заполняем блок с деньгами под покупателем")
    public void shouldFillMoneyBlockByBuyer() {
        basePageSteps.onDealPage().section("Средства для оплаты").input("Стоимость автомобиля", "1000000");

        basePageSteps.onDealPage().section("Средства для оплаты").paymentAmountTax().should(hasText("Комиссия Авто.ру\n1 ₽"));
        basePageSteps.onDealPage().section("Средства для оплаты").paymentAmountTotal().should(hasText("Итого\n1 000 001 ₽"));

        mockRule.overwriteStub(2, "desktop-lk/SafeDealDealGetWithBuyerOfferPrice");

        basePageSteps.onDealPage().section("Средства для оплаты").button("Подтвердить").click();

        basePageSteps.onDealPage().notifier().should(hasText("Данные сохранены"));
        basePageSteps.onDealPage().section("Средства для оплаты").stepWaitingDescription()
                .should(hasText("Реквизиты для оплаты\nВнесите деньги на специальный счёт. Реквизиты для оплаты " +
                        "появятся здесь, как только продавец подтвердит стоимость автомобиля"));
    }
}
