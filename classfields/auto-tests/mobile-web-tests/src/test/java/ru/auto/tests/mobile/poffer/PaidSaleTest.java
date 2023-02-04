package ru.auto.tests.mobile.poffer;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Epic;
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
import ru.auto.tests.desktop.mobile.step.PaymentSteps;
import ru.auto.tests.desktop.mobile.step.PofferSteps;
import ru.auto.tests.desktop.module.MobileDevToolsTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.ADD_OFFER;
import static ru.auto.tests.desktop.consts.AutoruFeatures.BETA_POFFER;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.QueryParams.ADD_PAGE;
import static ru.auto.tests.desktop.consts.QueryParams.PAGE_FROM;
import static ru.auto.tests.desktop.matchers.RequestHasBodyMatcher.hasJsonBody;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneRequest;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.requestsMatch;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserDraft.paidUserDraftExample;
import static ru.auto.tests.desktop.mock.MockUserOffer.carAfterPublish;
import static ru.auto.tests.desktop.mock.Paths.USER_DRAFT_CARS;
import static ru.auto.tests.desktop.mock.Paths.USER_DRAFT_CARS_ID;
import static ru.auto.tests.desktop.mock.Paths.USER_DRAFT_CARS_ID_PUBLISH;
import static ru.auto.tests.desktop.mock.beans.stub.Query.query;
import static ru.auto.tests.desktop.step.CookieSteps.EXP_AUTORUFRONT_21494;
import static ru.auto.tests.desktop.step.CookieSteps.FORCE_DISABLE_TRUST;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Добавление платного объявления")
@Epic(BETA_POFFER)
@Feature(ADD_OFFER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileDevToolsTestsModule.class)
public class PaidSaleTest {

    private static final String OFFER_TEMPLATE = "offers/beta_cars_user_used_paid_offer.json";
    private static final String DRAFT_ID = "4848705651719180864-7ac6416a";

    private static final String VAS_TEXT = "Платное размещение\nПубликация объявления происходит на платной основе\n" +
            "Первая неделя размещения\n5 399 ₽\nСо второй недели‑99 %\n1 ₽ / 7 дней\nРазместить за 5 399 ₽ / 7 дней";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private PaymentSteps paymentSteps;

    @Inject
    private PofferSteps pofferSteps;

    @Inject
    private SeleniumMockSteps seleniumMockSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        cookieSteps.setExpFlags(FORCE_DISABLE_TRUST, EXP_AUTORUFRONT_21494);

        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("mobile/poffer/BillingAutoruPaymentInit"),
                stub("poffer/user/BillingAutoruPaymentProcessFreeLimit"),
                stub("desktop/BillingAutoruPayment"),

                stub().withGetDeepEquals(USER_DRAFT_CARS)
                        .withResponseBody(paidUserDraftExample().getBody()),

                stub().withPutDeepEquals(format(USER_DRAFT_CARS_ID, DRAFT_ID))
                        .withRequestBody("poffer/beta/UserDraftCarsFullRequest")
                        .withResponseBody(paidUserDraftExample().getBody()),

                stub().withPostMatches(format(USER_DRAFT_CARS_ID_PUBLISH, DRAFT_ID))
                        .withRequestQuery(query().setFingerprint(".*").setPostTradeIn(false))
                        .withResponseBody(carAfterPublish().getBody())
        ).create();

        urlSteps.desktopURI().path(CARS).path(USED).path(ADD).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Текст VAS платного объявления")
    public void shouldSeePaidOfferVas() {
        pofferSteps.onPofferPage().submitBlock().waitUntil(hasText(VAS_TEXT));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class, Billing.class})
    @DisplayName("Добавление платного объявления")
    public void shouldAddPaidSale() {
        pofferSteps.submitForm();

        seleniumMockSteps.assertWithWaiting(requestsMatch(
                "/-/ajax/poffer/saveDraftFormsToPublicApi/",
                hasJsonBody(OFFER_TEMPLATE),
                2
        ));

        pofferSteps.onPofferPage().billingPopup().waitUntil(isDisplayed());
        pofferSteps.selectPaymentMethod("Банковская карта");
        pofferSteps.clickPayButton();
        paymentSteps.payByCard();
        paymentSteps.waitForSuccessMessage();

        pofferSteps.onPofferPage().popup().closeIcon().click();
        pofferSteps.onPofferPage().billingPopup().waitUntil(not(isDisplayed()), 10);

        urlSteps.testing().path(CARS).path(USED).path(SALE)
                .path("kia").path("optima")
                .path("/1098500720-92c92e49/")
                .addParam(PAGE_FROM, ADD_PAGE).shouldNotSeeDiff();
    }

}
