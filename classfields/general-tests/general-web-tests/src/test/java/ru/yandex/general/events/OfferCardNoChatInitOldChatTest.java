package ru.yandex.general.events;


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
import ru.yandex.general.module.GeneralProxyWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.EventSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.consts.Events.CHAT_INIT;
import static ru.yandex.general.consts.GeneralFeatures.EVENTS_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.EVENT_CHAT_INIT;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockChat.chatTemplate;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_USER_HAS_SEEN_PROFILE;
import static ru.yandex.general.step.BasePageSteps.TRUE;

@Epic(EVENTS_FEATURE)
@DisplayName("Нет «chatInit» с карточки оффера при открытии старого чата")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyWebModule.class)
public class OfferCardNoChatInitOldChatTest {

    private static final String ID = "123456";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private EventSteps eventSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Before
    public void before() {
        basePageSteps.setCookie(CLASSIFIED_USER_HAS_SEEN_PROFILE, TRUE);
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(BASIC_CARD).setId(ID).build())
                .setChat(chatTemplate().setIsNew(false).build())
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();

        passportSteps.commonAccountLogin();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(EVENT_CHAT_INIT)
    @DisplayName("Нет «chatInit» с карточки оффера при открытии старого чата")
    public void shouldNotSeeChatInitSendEventOldChat() {
        urlSteps.testing().path(CARD).path(ID).open();
        basePageSteps.onOfferCardPage().sidebar().startChat().click();

        eventSteps.withEventType(CHAT_INIT).withEventsCount(0).shouldExist();
    }

}
