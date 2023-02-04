package ru.auto.tests.desktop.garage;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.GARAGE;
import static ru.auto.tests.desktop.consts.QueryParams.OWNER;
import static ru.auto.tests.desktop.consts.QueryParams.POPUP;
import static ru.auto.tests.desktop.mock.MockGarageCard.CURRENT_CAR;
import static ru.auto.tests.desktop.mock.MockGarageCard.garageCardOffer;
import static ru.auto.tests.desktop.mock.MockGarageCards.garageCards;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.GARAGE_USER_CARD;
import static ru.auto.tests.desktop.mock.Paths.GARAGE_USER_CARDS;
import static ru.auto.tests.desktop.page.GarageCardPage.PASS_VERIFICATION;
import static ru.auto.tests.desktop.page.GaragePage.OWNER_CHECK_POPUP_TEXT;
import static ru.auto.tests.desktop.utils.Utils.getRandomId;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Попап проверенного собственника")
@Epic(AutoruFeatures.GARAGE)
@Feature("Попап проверенного собственника")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class GarageOwnerCheckPopupTest {

    private static final String GARAGE_CARD_ID = getRandomId();

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private MockRuleConfigurable mockRule;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser")
        );

        urlSteps.testing().path(GARAGE);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Попап проверенного собственника, при наличии авто в гараже")
    public void shouldSeeOwnerCheckPopupWithCarInGarage() {
        mockRule.setStubs(
                stub().withPostDeepEquals(GARAGE_USER_CARDS)
                        .withResponseBody(
                                garageCards().setCards(
                                        garageCardOffer().setId(GARAGE_CARD_ID)).build()),

                stub().withGetDeepEquals(format("%s/%s", GARAGE_USER_CARD, GARAGE_CARD_ID))
                        .withResponseBody(
                                garageCardOffer()
                                        .setId(GARAGE_CARD_ID)
                                        .setCardType(CURRENT_CAR).getBody())
        ).create();

        urlSteps.addParam(POPUP, OWNER).open();

        basePageSteps.onGaragePage().ownerCheckPopup().waitUntil(isDisplayed()).should(hasText(OWNER_CHECK_POPUP_TEXT));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Попап проверенного собственника, без авто в гараже")
    public void shouldSeeOwnerCheckPopupWithoutCarInGarage() {
        mockRule.setStubs(
                stub().withPostDeepEquals(GARAGE_USER_CARDS)
                        .withResponseBody(
                                garageCards().build())
        ).create();

        urlSteps.addParam(POPUP, OWNER).open();

        basePageSteps.onGaragePage().ownerCheckPopup().waitUntil(isDisplayed()).should(hasText(OWNER_CHECK_POPUP_TEXT));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Попап проверенного собственника, по клику на «Пройти проверку» с карточки гаража")
    public void shouldSeeOwnerCheckPopup() {
        mockRule.setStubs(
                stub().withPostDeepEquals(GARAGE_USER_CARDS)
                        .withResponseBody(
                                garageCards().setCards(
                                        garageCardOffer().setId(GARAGE_CARD_ID)).build()),

                stub().withGetDeepEquals(format("%s/%s", GARAGE_USER_CARD, GARAGE_CARD_ID))
                        .withResponseBody(
                                garageCardOffer()
                                        .setId(GARAGE_CARD_ID)
                                        .setCardType(CURRENT_CAR).getBody())
        ).create();

        urlSteps.open();
        basePageSteps.onGarageCardPage().button(PASS_VERIFICATION).waitUntil(isDisplayed()).click();

        basePageSteps.onGaragePage().ownerCheckPopup().waitUntil(isDisplayed()).should(hasText(OWNER_CHECK_POPUP_TEXT));
    }

}
