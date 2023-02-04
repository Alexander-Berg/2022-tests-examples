package ru.yandex.general.bannedChats;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.CompareSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;
import ru.yandex.qatools.ashot.Screenshot;

import static ru.yandex.general.consts.GeneralFeatures.BANNED_CHATS_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.consts.UserStatus.BANNED;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockCurrentUser.currentUserExample;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;

@Epic(BANNED_CHATS_FEATURE)
@DisplayName("Забаненный в чатах пользователь на карточке оффера")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class BannedChatOnOfferCard {

    private static final String ID = "12345";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Inject
    private CompareSteps compareSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Before
    public void before() {
        mockRule.graphqlStub(mockResponse().setCurrentUser(
                currentUserExample().setModerationChatStatus(BANNED).build())
                .setCard(mockCard(BASIC_CARD).setIsOwner(false).build())
                .build()).withDefaults().create();
        passportSteps.commonAccountLogin();
        compareSteps.resize(1920, 1080);
        urlSteps.testing().path(CARD).path(ID).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Задизейблена кнопка «Написать» для юзера с забаненными чатами")
    public void shouldSeeDisabledStartChatButton() {
        basePageSteps.onOfferCardPage().sidebar().startChat().should(hasAttribute("aria-disabled", "true"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Скриншот попапа по ховеру кнопку «Написать» для юзера с забаненными чатами")
    public void shouldSeeScreenshotDisabledChatButtonPopup() {
        basePageSteps.onOfferCardPage().sidebar().startChat().hover();
        Screenshot testing = compareSteps.takeScreenshot(basePageSteps.onOfferCardPage().popup());

        urlSteps.setProductionHost().open();
        basePageSteps.onOfferCardPage().sidebar().startChat().hover();
        Screenshot production = compareSteps.takeScreenshot(basePageSteps.onOfferCardPage().popup());

        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

}
