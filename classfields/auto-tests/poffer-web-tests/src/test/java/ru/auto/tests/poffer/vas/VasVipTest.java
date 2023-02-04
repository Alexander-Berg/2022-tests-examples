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
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.QueryParams.ADD_PAGE;
import static ru.auto.tests.desktop.consts.QueryParams.PAGE_FROM;
import static ru.auto.tests.desktop.consts.Regions.MOSCOW_IP;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserDraft.userVipDraftExample;
import static ru.auto.tests.desktop.mock.MockUserOffer.carAfterPublish;
import static ru.auto.tests.desktop.mock.Paths.USER_DRAFT_CARS;
import static ru.auto.tests.desktop.mock.Paths.USER_DRAFT_CARS_ID;
import static ru.auto.tests.desktop.mock.Paths.USER_DRAFT_CARS_ID_PUBLISH;
import static ru.auto.tests.desktop.mock.beans.stub.Query.query;
import static ru.auto.tests.desktop.step.CookieSteps.FORCE_DISABLE_TRUST;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Частник - добавление объявления с пакетом услуг «VIP»")
@Feature(BETA_POFFER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class VasVipTest {

    private static final String OFFER_ID = "1098500720-92c92e49";
    private static final String DRAFT_ID = "4848705651719180864-7ac6416a";
    private static final String VAS_TITLE = "Пакет VIP";

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
                stub("poffer/BillingAutoruPaymentInitVip"),
                stub("poffer/BillingAutoruPaymentProcess"),
                stub("desktop/BillingAutoruPayment"),

                stub().withGetDeepEquals(USER_DRAFT_CARS)
                        .withResponseBody(userVipDraftExample().getBody()),

                stub().withPutDeepEquals(format(USER_DRAFT_CARS_ID, DRAFT_ID))
                        .withRequestBody("poffer/beta/UserDraftCarsFullRequest")
                        .withResponseBody(userVipDraftExample().getBody()),

                stub().withPostMatches(format(USER_DRAFT_CARS_ID_PUBLISH, DRAFT_ID))
                        .withRequestQuery(query().setFingerprint(".*").setPostTradeIn(false))
                        .withResponseBody(carAfterPublish().getBody())
        ).create();

        urlSteps.testing().path(CARS).path(USED).path(ADD).addXRealIP(MOSCOW_IP).open();
        pofferSteps.hideFloatingSupportButtonAndDiscountTimer();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Добавление объявления с пакетом услуг «VIP»")
    public void shouldAddVipSale() {
        pofferSteps.onPofferPage().userVas().getSnippet(1).waitUntil(hasText("VIP\n383 ₽/ день\n" +
                "Всё внимание покупателей будет направлено только на вас. Собрали все основные опции " +
                "в один пакет для яркого размещения. Опции действуют 60 дней.\nПоднятие в поиске\n" +
                "Выделение цветом\nСпецпредложение\nПоднятие в ТОП\n×5\nбыстрее\nпродажа\n" +
                "Разместить за 22 999 ₽ на 60 дней\nВместо 38 331 ₽\n-40%"));
        pofferSteps.onPofferPage().userVas().getSnippet(1).submitButton().click();

        pofferSteps.onPofferPage().billingPopupFrame().waitUntil("Не появился поп-ап оплаты", isDisplayed(), 30);
        pofferSteps.onPofferPage().switchToBillingFrame();
        pofferSteps.onPofferPage().billingPopup().waitUntil(isDisplayed());
        pofferSteps.onPofferPage().billingPopup().header().waitUntil(hasText(VAS_TITLE));
        pofferSteps.onPofferPage().billingPopup().priceHeader().waitUntil(hasText("1 547 ₽"));
        yaKassaSteps.payWithCard();
        yaKassaSteps.waitForSuccessMessage();
        pofferSteps.onPofferPage().billingPopupCloseButton().click();
        pofferSteps.onPofferPage().billingPopup().waitUntil(not(isDisplayed()));

        urlSteps.testing().path(CARS).path(USED).path(SALE).path("ferrari").path("f40").path(OFFER_ID).path(SLASH)
                .addParam(PAGE_FROM, ADD_PAGE)
                .shouldNotSeeDiff();
    }
}
