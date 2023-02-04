package ru.auto.tests.mobile.c2bauction;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.AuctionApplicationStatus.StatusName.NEW;
import static ru.auto.tests.desktop.consts.AuctionApplicationStatus.StatusName.REJECTED;
import static ru.auto.tests.desktop.consts.Owners.DENISKOROBOV;
import static ru.auto.tests.desktop.consts.Pages.C2B_AUCTION;
import static ru.auto.tests.desktop.consts.Pages.FROM_WEB_TO_APP;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.PROMO;
import static ru.auto.tests.desktop.mobile.element.FromWebToAppSplash.SPLASH_EDIT_OFFER_TEXT;
import static ru.auto.tests.desktop.mock.MockC2bAuctionApplication.c2bAuctionApplication;
import static ru.auto.tests.desktop.mock.MockC2bAuctionApplicationsList.userApplicationsResponse;
import static ru.auto.tests.desktop.mock.MockStub.sessionAuthUserStub;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.C2B_AUCTION_APPLICATION_LIST;
import static ru.auto.tests.desktop.utils.Utils.getRandomId;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Деактивация заявки аукциона")
@Epic(AutoruFeatures.LK)
@Feature(AutoruFeatures.C2B_AUCTION)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class DeactivateAuctionTest {

    private static final String ID = getRandomId();
    private static final String LINK = "https://sb76.adj.st/promo/from-web-to-app/?action=edit&adjust_t=m1nelw7_eb04l75&adjust_campaign=touch_edit_ad_splash&adjust_adgroup=applogo_phone_white&adjust_deeplink=autoru%3A%2F%2Fapp%2Fpromo%2Ffrom-web-to-app%2F%3Faction%3Dedit";


    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub().withGetDeepEquals(C2B_AUCTION_APPLICATION_LIST).
                        withResponseBody(
                                userApplicationsResponse().setApplications(
                                        c2bAuctionApplication().setId(ID).setStatus(NEW)
                                ).build()),
                stub().withPostDeepEquals(format("/1.0/c2b-auction/application/%s/close", ID)).withStatusSuccessResponse(),
                sessionAuthUserStub(),
                stub("desktop/User")
        ).create();

        urlSteps.testing().path(MY).path(C2B_AUCTION).open();

        basePageSteps.onLkPage().getApplication(0).button("Отозвать заявку").click();
        basePageSteps.onLkPage().popup().isDisplayed();
        basePageSteps.onLkPage().popup().button("Отозвать").click();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DENISKOROBOV)
    @DisplayName("Отзываем заявку")
    public void shouldCancelApplication() {
        basePageSteps.onLkPage().fromWebToAppSlash().waitUntil(isDisplayed()).should(hasText(SPLASH_EDIT_OFFER_TEXT));
        urlSteps.testing().path(PROMO).path(FROM_WEB_TO_APP).addParam("action", "edit").shouldNotSeeDiff();

    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DENISKOROBOV)
    @DisplayName("Ссылка в кнопке «Отредактировать в приложении» на сплэш-скрине")
    public void shouldSeeEditInAppLink() {
        basePageSteps.onLkPage().fromWebToAppSlash().button().waitUntil(isDisplayed()).should(hasAttribute(
                "href", LINK));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DENISKOROBOV)
    @DisplayName("Отображение отмененной заявки")
    public void shouldSeeCanceledApplication() {
        mockRule.overwriteStub(0,
                stub().withGetDeepEquals(C2B_AUCTION_APPLICATION_LIST).
                        withResponseBody(
                                userApplicationsResponse().setApplications(
                                        c2bAuctionApplication().setId(ID).setStatus(REJECTED)
                                ).build()));

        basePageSteps.onLkPage().fromWebToAppSlash().closeButton().click();

        basePageSteps.onLkPage().getApplication(0).auctionResult().should(hasText("Сделка\nне состоялась"));

    }
}
