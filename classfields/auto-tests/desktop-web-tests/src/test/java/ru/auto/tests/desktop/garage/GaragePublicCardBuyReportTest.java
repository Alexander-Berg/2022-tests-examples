package ru.auto.tests.desktop.garage;

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
import ru.auto.tests.desktop.categories.Billing;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.YaKassaSteps;

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
import static ru.auto.tests.desktop.step.CookieSteps.FORCE_DISABLE_TRUST;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Покупка пакета из 10 отчётов по кнопке «Купить отчёт от» на публичной карточке")
@Feature(AutoruFeatures.GARAGE)
@Story("Отчет")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
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
    private YaKassaSteps yaKassaSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        cookieSteps.setExpFlags(FORCE_DISABLE_TRUST);

        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/ReferenceCatalogCarsSuggestVolkswagenJetta"),
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
    @Category({Regression.class, Testing.class, Billing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Покупка пакета из 10 отчётов по кнопке «Купить отчёт от» на публичной карточке")
    public void shouldBuy10ReportsPackagePublicCard() {
        mockRule.setStubs(
                stub("desktop/BillingAutoruPaymentInitVinGarage"),
                stub("desktop/BillingSubscriptionsOffersHistoryReportsPricesGarage"),
                stub("desktop/BillingAutoruPaymentProcessGaragePublicCard"),
                stub("desktop/BillingAutoruPayment"),
                stub("desktop/CarfaxReportRawVinPaidWVWZZZ16ZBM121912")
        ).update();

        basePageSteps.onGarageCardPage().vinReport().button("Купить отчёт от 69\u00a0₽").click();
        basePageSteps.onGarageCardPage().switchToBillingFrame();
        basePageSteps.onGarageCardPage().billingPopup().waitUntil(isDisplayed());
        basePageSteps.onGarageCardPage().billingPopup().header().should(hasText("Отчёт о проверке по VIN"));
        basePageSteps.onGarageCardPage().billingPopup()
                .vinSwitcherSelected("Пакет из 10\u00a0отчётов\u00a0•\u00a0действует 1\u00a0год99\u00a0₽ / отчёт" +
                        "Выгода 33%990\u00a0₽").should(isDisplayed());
        basePageSteps.onGarageCardPage().billingPopup().checkbox("Запомнить карту").hover().click();
        yaKassaSteps.payWithCard();

        basePageSteps.onGarageCardPage().billingPopup().should(not(isDisplayed()));
        urlSteps.shouldNotSeeDiff();
    }

}
