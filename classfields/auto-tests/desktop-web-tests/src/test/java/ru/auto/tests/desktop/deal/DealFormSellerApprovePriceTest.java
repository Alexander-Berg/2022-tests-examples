package ru.auto.tests.desktop.deal;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.DEAL;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Безопасная сделка. Форма. Блок «Банковские реквизиты»")
@Feature(AutoruFeatures.SAFE_DEAL)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class DealFormSellerApprovePriceTest {

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
                "desktop-lk/SafeDealDealGetWithOfferPrice",
                "desktop/SafeDealDealUpdateOfferPriceApprove").post();

        urlSteps.testing().path(DEAL).path(DEAL_ID).open();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Подтверждаем цену автомобиля под продавцом")
    public void shouldApproveOfferPriceBySeller() {
        basePageSteps.onDealPage().section("Банковские реквизиты").should(hasText("Банковские реквизиты\nШаг 3 из 4.\n " +
                "Подтверждение стоимости\nПодтвердите стоимость автомобиля, о которой вы договорились с покупателем\n" +
                "Стоимость автомобиля\n370 000 ₽\nПодтвердить\nСвязаться с покупателем"));

        mockRule.overwriteStub(2, "desktop-lk/SafeDealDealGetWithOfferPriceApproved");

        basePageSteps.onDealPage().section("Банковские реквизиты").button("Подтвердить").click();

        basePageSteps.onDealPage().section("Банковские реквизиты").should(hasText("Банковские реквизиты\nШаг 3 из 4.\n " +
                "Стоимость автомобиля\n370 000 ₽\nСвязаться с покупателем\nПлатежные реквизиты\nУкажите реквизиты " +
                "счёта, на который поступят деньги после завершения сделки\nНомер счета получателя\nБИК банка " +
                "получателя\nПодтвердить"));
    }
}