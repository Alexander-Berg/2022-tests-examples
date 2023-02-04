package ru.auto.tests.poffer.user;

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
import ru.auto.tests.desktop.module.DesktopDevToolsTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.YaKassaSteps;
import ru.auto.tests.desktop.step.poffer.BetaPofferSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.BETA_POFFER;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.QueryParams.ADD_PAGE;
import static ru.auto.tests.desktop.consts.QueryParams.PAGE_FROM;
import static ru.auto.tests.desktop.matchers.RequestHasBodyMatcher.pofferHasJsonBody;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneRequest;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserDraft.paidUserDraftExample;
import static ru.auto.tests.desktop.mock.MockUserOffer.carAfterPublish;
import static ru.auto.tests.desktop.mock.Paths.USER_DRAFT_CARS;
import static ru.auto.tests.desktop.mock.Paths.USER_DRAFT_CARS_ID;
import static ru.auto.tests.desktop.mock.Paths.USER_DRAFT_CARS_ID_PUBLISH;
import static ru.auto.tests.desktop.mock.beans.stub.Query.query;
import static ru.auto.tests.desktop.step.CookieSteps.FORCE_DISABLE_TRUST;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Частник - добавление платного объявления")
@Feature(BETA_POFFER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopDevToolsTestsModule.class)
public class PaidSaleTest {

    private static final String OFFER_TEMPLATE = "offers/beta/beta_cars_user_used_paid_offer.json";
    private static final String DRAFT_ID = "4848705651719180864-7ac6416a";
    private static final String PRICE = "1 999 ₽";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private YaKassaSteps yaKassaSteps;

    @Inject
    private BetaPofferSteps pofferSteps;

    @Inject
    private SeleniumMockSteps seleniumMockSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        cookieSteps.setExpFlags(FORCE_DISABLE_TRUST);

        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("poffer/beta/BillingAutoruPaymentInitActivateFreeLimit"),
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

        urlSteps.testing().path(CARS).path(USED).path(ADD).open();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Должны увидеть VAS платного объявления")
    public void shouldSeePaidOfferVas() {
        pofferSteps.onBetaPofferPage().vasBlock().free().waitUntil(hasText("Опции отдельно\n771 ₽/ день\n" +
                "Комбинируйте опции по своему вкусу.\nПоказ в Историях\nНа 3 дня\n3 505 ₽\nВыделение цветом\n" +
                "На 3 дня\n597 ₽\nСпецпредложение\nНа 3 дня\n879 ₽\nПоднятие в ТОП\nНа 3 дня\n2 157 ₽\n" +
                "Платное размещение\n5 399 ₽\nПервая неделя размещения — 5 399 ₽. Далее — 1 ₽ / 7 дней. " +
                "Выгода 99%\nРазместить за 5 399 ₽ / 7 дней\nСо 2 недели — 1 ₽"));
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class, Billing.class})
    @DisplayName("Добавление платного объявления")
    public void shouldAddPaidSale() {
        pofferSteps.hideElement(pofferSteps.onBetaPofferPage().supportFloatingButton());
        pofferSteps.submitForm();

        seleniumMockSteps.assertWithWaiting(onlyOneRequest(
                "/-/ajax/poffer/saveDraftFormsToPublicApi/",
                pofferHasJsonBody(OFFER_TEMPLATE)
        ));

        pofferSteps.onBetaPofferPage().billingPopupFrame().waitUntil("Не появился поп-ап оплаты", isDisplayed(), 30);
        pofferSteps.onBetaPofferPage().switchToBillingFrame();
        pofferSteps.onBetaPofferPage().billingPopup().waitUntil(isDisplayed());
        pofferSteps.onBetaPofferPage().billingPopup().header().waitUntil(hasText("Активация объявления"));
        pofferSteps.onBetaPofferPage().billingPopup().priceHeader().waitUntil(hasText(PRICE));
        yaKassaSteps.payWithCard();
        yaKassaSteps.waitForSuccessMessage();
        pofferSteps.onBetaPofferPage().billingPopupCloseButton().click();
        pofferSteps.onBetaPofferPage().billingPopup().waitUntil(not(isDisplayed()));

        urlSteps.testing().path(CARS).path(USED).path(SALE)
                .path("kia").path("optima")
                .path("/1098500720-92c92e49/")
                .addParam(PAGE_FROM, ADD_PAGE).shouldNotSeeDiff();
    }
}
