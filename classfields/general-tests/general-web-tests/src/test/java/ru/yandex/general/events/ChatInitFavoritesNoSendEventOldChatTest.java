package ru.yandex.general.events;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.general.mock.MockFavoritesSnippet;
import ru.yandex.general.module.GeneralProxyWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.EventSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.Events.CHAT_INIT;
import static ru.yandex.general.consts.GeneralFeatures.EVENTS_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.EVENT_CHAT_INIT;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FAVORITES;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.mock.MockChat.chatTemplate;
import static ru.yandex.general.mock.MockFavorites.favoritesResponse;
import static ru.yandex.general.mock.MockFavoritesSnippet.BASIC_SNIPPET;
import static ru.yandex.general.mock.MockFavoritesSnippet.mockSnippet;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_REGION_ID;

@Epic(EVENTS_FEATURE)
@Feature(EVENT_CHAT_INIT)
@DisplayName("Нет «chatInit» со страницы избранного, для старого чата")
@RunWith(Parameterized.class)
@GuiceModules(GeneralProxyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ChatInitFavoritesNoSendEventOldChatTest {

    private static final String ANY = "Any";
    private static final String CHAT = "Chat";
    private static final String REGION_ID = "213";

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
    private EventSteps eventSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Parameterized.Parameter
    public String title;

    @Parameterized.Parameter(1)
    public MockFavoritesSnippet snippet;

    @Parameterized.Parameters(name = "{index}. {0}")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {"Нет «chatInit» со страницы избранного для старого чата по нажатию на иконку сообщений",
                        mockSnippet(BASIC_SNIPPET).getMockSnippet().setPreferContactWay(ANY)
                },
                {"Нет «chatInit» со страницы избранного для старого чата по нажатию на кнопку «Написать»",
                        mockSnippet(BASIC_SNIPPET).getMockSnippet().setPreferContactWay(CHAT)
                }
        });
    }

    @Before
    public void before() {
        passportSteps.commonAccountLogin();

        mockRule.graphqlStub(mockResponse()
                .setFavorites(favoritesResponse().offers(asList(
                        mockSnippet(BASIC_SNIPPET).getMockSnippet())).build())
                .setChat(chatTemplate().setIsNew(false).build())
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();

        basePageSteps.setCookie(CLASSIFIED_REGION_ID, REGION_ID);

        urlSteps.testing().path(MY).path(FAVORITES).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет «chatInit» со страницы избранного, для старого чата")
    public void shouldNotSeeChatInitOldChat() {
        basePageSteps.onFavoritesPage().firstFavCard().chatButton().hover().click();

        eventSteps.withEventType(CHAT_INIT).withEventsCount(0).shouldExist();
    }

}
