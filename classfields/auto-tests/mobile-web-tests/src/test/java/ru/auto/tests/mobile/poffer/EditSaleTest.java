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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mobile.step.PofferSteps;
import ru.auto.tests.desktop.models.MobileOffer;
import ru.auto.tests.desktop.module.MobileDevToolsTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.AutoruFeatures.BETA_POFFER;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.QueryParams.ADD_PAGE;
import static ru.auto.tests.desktop.consts.QueryParams.PAGE_FROM;
import static ru.auto.tests.desktop.consts.Regions.MOSCOW_IP;
import static ru.auto.tests.desktop.matchers.RequestHasBodyMatcher.hasJsonBody;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneRequest;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.requestsMatch;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserDraft.userDraftExample;
import static ru.auto.tests.desktop.mock.MockUserOffer.carAfterPublish;
import static ru.auto.tests.desktop.mock.Paths.USER_DRAFT_CARS;
import static ru.auto.tests.desktop.mock.Paths.USER_DRAFT_CARS_ID;
import static ru.auto.tests.desktop.mock.Paths.USER_DRAFT_CARS_ID_PUBLISH;
import static ru.auto.tests.desktop.mock.beans.stub.Query.query;
import static ru.auto.tests.desktop.step.CookieSteps.EXP_AUTORUFRONT_21494;

@DisplayName("Частник - редактирование объявления")
@Epic(BETA_POFFER)
@Feature("Редактирование объявления")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileDevToolsTestsModule.class)
public class EditSaleTest {

    private static final String OFFER_ID = "1098500720-92c92e49";
    private static final String DRAFT_ID = "4848705651719180864-7ac6416a";
    private static final String OFFER_TEMPLATE = "offers/beta_cars_user_used_edit_offer.json";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private PofferSteps pofferSteps;

    @Inject
    private SeleniumMockSteps seleniumMockSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private MobileOffer offer;

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/User"),

                stub().withPostDeepEquals(format("/1.0/user/offers/cars/%s/edit", OFFER_ID))
                        .withResponseBody("poffer/beta/UserOffersCarsEditResponse"),

                stub().withGetDeepEquals(USER_DRAFT_CARS)
                        .withResponseBody(userDraftExample().getBody()),

                stub().withPutDeepEquals(format(USER_DRAFT_CARS_ID, DRAFT_ID))
                        .withRequestBody("poffer/beta/UserDraftCarsEditedRequest")
                        .withResponseBody(userDraftExample().getBody()),

                stub().withPostMatches(format(USER_DRAFT_CARS_ID_PUBLISH, DRAFT_ID))
                        .withRequestQuery(query().setFingerprint(".*").setPostTradeIn(false))
                        .withResponseBody(carAfterPublish().getBody())
        ).create();

        cookieSteps.setExpFlags(EXP_AUTORUFRONT_21494);

        urlSteps.desktopURI().path(CARS).path(USED).path(ADD).addXRealIP(MOSCOW_IP).open();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Редактирование объявления")
    public void shouldEditSale() {
        offer.setMileage("10000");
        offer.setPrice("1900001");
        offer.setColor("синий");
        offer.setContactName("Дмитрий");
        offer.setContactEmail("edit@ya.ru");
        offer.setPtsType("Дубликат");
        offer.setOwnersCount("2");
        offer.setPlateNumber("x222xx 07");
        offer.setVin("EDTGD18508S219350");
        offer.setDescription("Отредактировано");

        pofferSteps.fillEditForm();
        pofferSteps.submitForm();

        seleniumMockSteps.assertWithWaiting(requestsMatch(
                "/-/ajax/poffer/saveDraftFormsToPublicApi/",
                hasJsonBody(OFFER_TEMPLATE),
                2
        ));
        urlSteps.testing().path(CARS).path(USED).path(SALE).path("kia").path("optima").path(OFFER_ID).path(SLASH)
                .addParam(PAGE_FROM, ADD_PAGE).shouldNotSeeDiff();
    }

}
