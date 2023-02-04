package ru.yandex.general.offerCard;

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
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.JSoupSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.consts.GeneralFeatures.CHAT_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.HOMEPAGE_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.OFFER_CARD_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.element.Header.CHATS;
import static ru.yandex.general.page.BasePage.PXLS_TO_FLOAT_HEADER;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(OFFER_CARD_FEATURE)
@Feature(CHAT_FEATURE)
@DisplayName("Открытие чата с карточки")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class ChatOpenOnOfferCardTest {

    private String offerCardPath;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public JSoupSteps jSoupSteps;

    @Inject
    private PassportSteps passportSteps;

    @Before
    public void before() {
        basePageSteps.setMoscowCookie();
        passportSteps.commonAccountLogin();
        offerCardPath = jSoupSteps.getActualOfferCardUrl();
        urlSteps.testing().path(offerCardPath).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Открытие чатов с карточки по клику в хэдере")
    public void shouldSeeOfferCardChatOpenFromHeader() {
        basePageSteps.onOfferCardPage().header().spanLinkWithTitle(CHATS).click();

        basePageSteps.onOfferCardPage().chatPopup().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Открытие чатов с карточки по клику в прилипшем хэдере")
    public void shouldSeeOfferCardChatOpenFromFloatedHeader() {
        basePageSteps.scrollDown(PXLS_TO_FLOAT_HEADER);
        basePageSteps.onOfferCardPage().floatedHeader().spanLinkWithTitle(CHATS).click();

        basePageSteps.onOfferCardPage().chatPopup().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Открытие чатов с карточки по клику в попапе «Юзер инфо»")
    public void shouldSeeOfferCardChatOpenFromUserInfoPopup() {
        basePageSteps.onOfferCardPage().header().avatar().click();
        basePageSteps.onOfferCardPage().userInfoPopup().spanLink(CHATS).waitUntil(isDisplayed()).click();

        basePageSteps.onOfferCardPage().chatPopup().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Открытие чатов с карточки по клику в попапе «Юзер инфо» из прилипшего хэдера")
    public void shouldSeeOfferCardChatOpenFromUserInfoPopupFloatedHeader() {
        basePageSteps.scrollDown(PXLS_TO_FLOAT_HEADER);
        basePageSteps.onOfferCardPage().floatedHeader().avatar().click();
        basePageSteps.onOfferCardPage().userInfoPopup().spanLink(CHATS).waitUntil(isDisplayed()).click();

        basePageSteps.onOfferCardPage().chatPopup().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Открытие чатов с карточки по клику виджет чатов")
    public void shouldSeeOfferCardChatOpenFromWidget() {
        basePageSteps.onOfferCardPage().chatWidgetButton().click();

        basePageSteps.onOfferCardPage().chatPopup().should(isDisplayed());
    }

}
