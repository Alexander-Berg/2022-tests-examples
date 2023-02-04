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

import static java.lang.String.format;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.commons.mountebank.http.predicates.PredicateType.DEEP_EQUALS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.GARAGE;
import static ru.auto.tests.desktop.consts.Pages.SHARE;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.mock.MockGarageCard.GARAGE_PUBLIC_CARD;
import static ru.auto.tests.desktop.mock.MockGarageCard.garageCard;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.GARAGE_CARD;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Покупка пакета из 10 отчётов по кнопке «Купить отчёт от» на публичной карточке")
@Feature(AutoruFeatures.GARAGE)
@Story("Карточка автомобиля в гараже - отчет")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class GaragePublicCardBuyReportTest {

    private static final String GARAGE_CARD_ID = "1146321503";

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
                stub("desktop/CarfaxReportRawVinNotPaidWVWZZZ16ZBM121912"),
                stub("desktop/GarageCardReportRaw"),
                stub("desktop/GarageUserCardsVinPost").withPredicateType(DEEP_EQUALS)
                        .withStatusCode(SC_UNAUTHORIZED),
                stub().withGetDeepEquals(format("%s/%s", GARAGE_CARD, GARAGE_CARD_ID))
                        .withResponseBody(
                                garageCard(GARAGE_PUBLIC_CARD).setId(GARAGE_CARD_ID).getBody())
        ).create();

        urlSteps.testing().path(GARAGE).path(SHARE).path(GARAGE_CARD_ID).path(SLASH).open();
        basePageSteps.onGarageCardPage().vinReport().waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Покупка пакета из 10 отчётов по кнопке «Купить отчёт от» на публичной карточке")
    public void shouldBuy10ReportsPackagePublicCard() {
        mockRule.setStubs(
                stub("mobile/BillingAutoruPaymentInitVinGarage"),
                stub("desktop/BillingSubscriptionsOffersHistoryReportsPricesGarage"),
                stub("mobile/BillingAutoruPaymentProcessGaragePublicCard"),
                stub("desktop/BillingAutoruPayment"),
                stub("desktop/CarfaxReportRawVinPaidWVWZZZ16ZBM121912")
        ).update();

        basePageSteps.onGarageCardPage().vinReport().button("Купить отчёт от 69\u00a0₽").click();
        basePageSteps.onGarageCardPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.selectPaymentMethod("Банковская карта");
        basePageSteps.clickPayButton();
        paymentSteps.payByCard();

        basePageSteps.onGarageCardPage().billingPopup().should(not(isDisplayed()));
        urlSteps.shouldNotSeeDiff();
    }

}
