package ru.auto.tests.poffer.vas;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.YaKassaSteps;
import ru.auto.tests.desktop.step.poffer.BetaPofferSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.BETA_POFFER;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.EDIT;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.Pages.VAS;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserDraft.userDraftExample;
import static ru.auto.tests.desktop.mock.MockUserOffer.carAfterPublish;
import static ru.auto.tests.desktop.mock.Paths.USER_DRAFT_CARS_ID;
import static ru.auto.tests.desktop.mock.Paths.USER_DRAFT_CARS_ID_PUBLISH;
import static ru.auto.tests.desktop.mock.beans.stub.Query.query;
import static ru.auto.tests.desktop.step.CookieSteps.FORCE_DISABLE_TRUST;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Частник - покупка услуги «Поднятие в поиске» при редактировании объявления")
@Feature(BETA_POFFER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class VasFreshEditTest {

    private static final String OFFER_ID = "1076842087-f1e84";
    private static final String DRAFT_ID = "4848705651719180864-7ac6416a";
    private static final String VAS_TITLE = "Поднятие в поиске";
    private static final String VAS_PRICE = "457 ₽";

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
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        cookieSteps.setExpFlags(FORCE_DISABLE_TRUST);

        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("poffer/BillingAutoruPaymentInitFreshEdit"),
                stub("poffer/BillingAutoruPaymentProcess"),
                stub("desktop/BillingAutoruPayment"),

                stub().withPostDeepEquals(format("/1.0/user/offers/cars/%s/edit", OFFER_ID))
                        .withResponseBody("poffer/beta/UserOffersCarsEditResponse"),

                stub().withGetDeepEquals(format(USER_DRAFT_CARS_ID, DRAFT_ID))
                        .withResponseBody(userDraftExample().getBody()),

                stub().withPutDeepEquals(format(USER_DRAFT_CARS_ID, DRAFT_ID))
                        .withRequestBody("poffer/beta/UserDraftCarsEditedRequest")
                        .withResponseBody(userDraftExample().getBody()),

                stub().withPostMatches(format(USER_DRAFT_CARS_ID_PUBLISH, DRAFT_ID))
                        .withRequestQuery(query().setFingerprint(".*").setPostTradeIn(false))
                        .withResponseBody(carAfterPublish().getBody())
        ).create();

        urlSteps.testing().path(CARS).path(USED).path(EDIT).path(OFFER_ID).open();
        pofferSteps.hideFloatingSupportButtonAndDiscountTimer();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Покупка услуги «Поднятие в поиске» при редактировании объявления")
    public void shouldBuyFresh() {
        pofferSteps.setWideWindowSize();

        pofferSteps.onPofferPage().userVas().getSnippet(2).option(VAS_TITLE).checkbox().click();
        pofferSteps.onPofferPage().userVas().getSnippet(2).waitUntil(hasText("Опции отдельно\n457 ₽/ день\n" +
                "Комбинируйте опции по своему вкусу.\nПоказ в Историях\nНа 3 дня\n3 505 ₽\n" +
                "Поднятие в поиске\n457 ₽\nВыделение цветом\nНа 3 дня\n597 ₽\nСпецпредложение\n" +
                "На 3 дня\n879 ₽\nПоднятие в ТОП\nНа 3 дня\n2 157 ₽\nСохранить и ускорить за 457 ₽\nНа 1 день"
        ));
        pofferSteps.onPofferPage().submitButton()
                .waitUntil(hasText(format("Сохранить и ускорить за %s\nНа 1 день", VAS_PRICE)));
        pofferSteps.submitForm();

        pofferSteps.onPofferPage().billingPopupFrame().waitUntil("Не появился поп-ап оплаты", isDisplayed(), 30);
        pofferSteps.onPofferPage().switchToBillingFrame();
        pofferSteps.onPofferPage().billingPopup().waitUntil(isDisplayed());
        pofferSteps.onPofferPage().billingPopup().header().waitUntil(hasText(VAS_TITLE));
        pofferSteps.onPofferPage().billingPopup().priceHeader().waitUntil(hasText("97 ₽"));
        yaKassaSteps.payWithCard();
        yaKassaSteps.waitForSuccessMessage();
        pofferSteps.onPofferPage().billingPopupCloseButton().click();
        pofferSteps.onPofferPage().billingPopup().waitUntil(not(isDisplayed()));

        urlSteps.testing().path(VAS).path(CARS).path(OFFER_ID).path(SLASH).shouldNotSeeDiff();
    }
}
