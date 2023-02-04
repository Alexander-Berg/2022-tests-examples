package ru.auto.tests.mobile.garage;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.mobile.step.PaymentSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.GARAGE;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Гараж")
@Story("Карточка автомобиля в гараже - отчет")
@Feature(AutoruFeatures.GARAGE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class GarageCardVinReportTest {

    private static final String VIN_CARD_ID = "/1146321503/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private PaymentSteps paymentSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/SearchCarsBreadcrumbsEmpty"),
                stub("desktop/GarageUserCardsVinPost"),
                stub("desktop/GarageUserCardVin"),
                stub("desktop/CarfaxReportRawVinNotPaidWVWZZZ16ZBM121912")
        ).create();

        urlSteps.testing().path(GARAGE).path(VIN_CARD_ID).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение отчёта")
    public void shouldSeeReport() {
        basePageSteps.onGarageCardPage().vinReport().should(hasText("Отчёт о проверке по VIN\nДанные техосмотра\n" +
                "Опции по VIN\nХарактеристики автомобиля\nТранспортный налог\nОценка стоимости\nЕще 19 пунктов " +
                "проверки\nКупить отчёт от 99 ₽"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Покупка пакета из 10 отчётов по кнопке «Купить отчёт от»")
    public void shouldBuy10ReportsPackage() {
        mockRule.setStubs(
                stub("mobile/BillingAutoruPaymentInitVinGarage"),
                stub("desktop/BillingSubscriptionsOffersHistoryReportsPricesGarage"),
                stub("mobile/BillingAutoruPaymentProcessGarage"),
                stub("desktop/BillingAutoruPayment"),
                stub("desktop/CarfaxReportRawVinPaidWVWZZZ16ZBM121912")
        ).update();

        basePageSteps.onGarageCardPage().vinReport().button("Купить отчёт от 99\u00a0₽").click();
        basePageSteps.onGarageCardPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.selectPaymentMethod("Банковская карта");
        basePageSteps.clickPayButton();
        paymentSteps.payByCard();

        basePageSteps.onGarageCardPage().billingPopup().should(not(isDisplayed()));
        urlSteps.shouldNotSeeDiff();
    }

}
