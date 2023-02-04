package ru.auto.tests.mobile.buyout;

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
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static ru.auto.tests.desktop.consts.Owners.DENISKOROBOV;
import static ru.auto.tests.desktop.consts.Pages.BUYOUT;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserOffer.car;
import static ru.auto.tests.desktop.mock.MockUserOffers.userOffersResponse;
import static ru.auto.tests.desktop.mock.Paths.USER_OFFERS_CARS;
import static ru.auto.tests.desktop.mock.beans.offer.seller.Phones.phones;
import static ru.auto.tests.desktop.mock.beans.stub.Query.query;
import static ru.auto.tests.desktop.page.BuyoutPage.ESTIMATE_CAR;
import static ru.auto.tests.desktop.page.BuyoutPage.PHONE_NUMBER;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;

@DisplayName("Автозаполение номера из оффера")
@Feature(BUYOUT)
@Story("Автозаполение номера из оффера")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class BuyoutUserPhoneTest {

    private static final String ID = "1114782187-3302e085";
    private static final String USER_PHONE = "79104557054";
    private static final String USER_ORIGINAL_PHONE = "79104557055";
    private static final String USER_ORIGIN_PHONE_FORMATED = "+7 910 455-70-55";

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
                stub("desktop/SessionAuthUser"),
                stub("desktop/User")
        ).create();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DENISKOROBOV)
    @DisplayName("Отображение настоящего телефона")
    public void shouldSeeOriginalPhoneInApplication() {
        mockRule.setStubs(
                stub().withGetDeepEquals(USER_OFFERS_CARS)
                        .withRequestQuery(
                                query().setStatus("ACTIVE"))
                        .withResponseBody(
                                userOffersResponse().setOffers(
                                        car().setId(ID).setPhones(
                                                phones()
                                                        .setOriginal(USER_ORIGINAL_PHONE)
                                                        .setPhone(USER_PHONE)
                                                        .setCallHoursStart(10)
                                                        .setCallHoursEnd(19)
                                        )).build()),
                stub("desktop/UserOffersCarsC2bCanApply")
        ).update();

        urlSteps.testing().path(BUYOUT).open();
        basePageSteps.onBuyoutPage().button(ESTIMATE_CAR).click();

        basePageSteps.onBuyoutPage().popup().input(PHONE_NUMBER).should(hasValue(USER_ORIGIN_PHONE_FORMATED));
    }

}
