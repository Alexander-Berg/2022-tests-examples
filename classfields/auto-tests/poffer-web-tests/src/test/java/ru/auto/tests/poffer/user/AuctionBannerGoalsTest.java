package ru.auto.tests.poffer.user;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
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
import ru.auto.tests.desktop.module.DesktopDevToolsTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.poffer.BetaPofferSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.AutoruFeatures.AUCTION_BANNER;
import static ru.auto.tests.desktop.consts.AutoruFeatures.BETA_POFFER;
import static ru.auto.tests.desktop.consts.Goals.FORM_ADD_DRAFT_BUYOUT;
import static ru.auto.tests.desktop.consts.Metrics.PAGE_URL;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.EDIT;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.element.poffer.beta.BetaAuctionBanner.ACCEPT;
import static ru.auto.tests.desktop.element.poffer.beta.BetaAuctionBanner.SIGN_UP_FOR_INSPECTION;
import static ru.auto.tests.desktop.matchers.RequestHasBodyMatcher.hasSiteInfo;
import static ru.auto.tests.desktop.matchers.RequestHasQueryItemsMatcher.hasGoal;
import static ru.auto.tests.desktop.matchers.RequestHasQueryItemsMatcher.hasQuery;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneMetricsRequest;
import static ru.auto.tests.desktop.mock.MockC2BApplicationInfo.c2bApplicationInfoExample;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserDraft.userDraftExample;
import static ru.auto.tests.desktop.mock.Paths.C2B_AUCTION_APPLICATION_CARS;
import static ru.auto.tests.desktop.mock.Paths.USER_DRAFT_CARS;
import static ru.auto.tests.desktop.mock.Paths.USER_DRAFT_CARS_ID;
import static ru.auto.tests.desktop.mock.Paths.USER_DRAFT_CARS_ID_C2B_APPLICATION_INFO;
import static ru.auto.tests.desktop.step.CookieSteps.EXP_AUTORUFRONT_20574;
import static ru.auto.tests.desktop.step.SeleniumMockSteps.queryPair;
import static ru.auto.tests.desktop.utils.Utils.getRandomBetween;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;

@DisplayName("Тесты на баннер аукциона")
@Feature(BETA_POFFER)
@Story(AUCTION_BANNER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopDevToolsTestsModule.class)
public class AuctionBannerGoalsTest {

    private static final String DRAFT_ID = "4848705651719180864-7ac6416a";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BetaPofferSteps pofferSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Inject
    public SeleniumMockSteps seleniumMockSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),

                stub().withGetDeepEquals(USER_DRAFT_CARS)
                        .withResponseBody(userDraftExample().getBody()),

                stub().withGetDeepEquals(format(USER_DRAFT_CARS_ID_C2B_APPLICATION_INFO, DRAFT_ID))
                        .withResponseBody(c2bApplicationInfoExample().setCanApply(true).getBody())
        ).create();

        cookieSteps.setExpFlags(EXP_AUTORUFRONT_20574);

        urlSteps.testing().path(CARS).path(USED).path(ADD).open();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отправляется цель «FORM_ADD_DRAFT_BUYOUT» при показе банера")
    public void shouldSeeBannerAuctionShowGoal() {
        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(
                hasGoal(
                        seleniumMockSteps.formatGoal(FORM_ADD_DRAFT_BUYOUT),
                        urlSteps.getCurrentUrl()
                ),
                hasSiteInfo("{\"draft_id\":\"4848705651719180864-7ac6416a\"}")
        ));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отправляется метрика при показе банера с драфта")
    public void shouldSeeBannerAuctionShowMetricFromDraft() {
        pofferSteps.onBetaPofferPage().auctionBanner().hover();

        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(
                hasQuery(queryPair(PAGE_URL, urlSteps.getCurrentUrl())),
                hasSiteInfo("{\"FORM_ADD_DRAFT\":{\"CARS\":{\"BUYOUT\":{\"SHOW\":{}}}}}")
        ));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отправляется метрика при показе банера с формы редактирования")
    public void shouldSeeBannerAuctionShowMetricFromEdit() {
        mockRule.setStubs(
                stub().withPostDeepEquals(format("/1.0/user/offers/cars/%s/edit", DRAFT_ID))
                        .withResponseBody("poffer/beta/UserOffersCarsEditResponse"),

                stub().withGetDeepEquals(format(USER_DRAFT_CARS_ID, DRAFT_ID))
                        .withResponseBody(userDraftExample().getBody())
        ).update();

        urlSteps.testing().path(CARS).path(USED).path(EDIT).path(DRAFT_ID).open();

        pofferSteps.onBetaPofferPage().auctionBanner().hover();

        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(
                hasQuery(queryPair(PAGE_URL, urlSteps.getCurrentUrl())),
                hasSiteInfo("{\"FORM_EDIT\":{\"CARS\":{\"BUYOUT\":{\"SHOW\":{}}}}}")
        ));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отправляется метрика при клике на «Записаться на осмотр» в банере")
    public void shouldSeeBuyoutSuccessMetric() {
        JsonObject responseBody = new JsonObject();
        responseBody.addProperty("application_id", String.valueOf(getRandomBetween(1000, 5000)));

        mockRule.setStubs(
                stub().withPostDeepEquals(format("%s/%s", C2B_AUCTION_APPLICATION_CARS, DRAFT_ID))
                        .withResponseBody(responseBody)
        ).update();

        String formPageUrl = urlSteps.getCurrentUrl();

        pofferSteps.onBetaPofferPage().auctionBanner().checkboxContains(ACCEPT).click();

        pofferSteps.onBetaPofferPage().auctionBanner().button(SIGN_UP_FOR_INSPECTION).waitUntil(isEnabled()).click();

        seleniumMockSteps.assertWithWaiting(onlyOneMetricsRequest(
                hasQuery(queryPair(PAGE_URL, formPageUrl)),
                hasSiteInfo("{\"FORM_ADD_DRAFT\":{\"CARS\":{\"BUYOUT\":{\"SUCCESS\":{}}}}}")
        ));
    }

}
