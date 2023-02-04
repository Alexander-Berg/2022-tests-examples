package ru.yandex.general.goals;

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
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.GoalsSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.GeneralFeatures.GOALS_FEATURE;
import static ru.yandex.general.consts.Goals.CHAT_INIT;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FAVORITES;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.mock.MockChat.chatTemplate;
import static ru.yandex.general.mock.MockFavorites.favoritesResponse;
import static ru.yandex.general.mock.MockFavoritesSnippet.BASIC_SNIPPET;
import static ru.yandex.general.mock.MockFavoritesSnippet.mockSnippet;
import static ru.yandex.general.mock.MockResponse.mockResponse;

@Epic(GOALS_FEATURE)
@Feature(CHAT_INIT)
@DisplayName("Цель «CHAT_INIT» для нового чата")
@RunWith(Parameterized.class)
@GuiceModules(GeneralProxyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class FavoritesChatInitGoalTest {

    private static final String ANY = "Any";
    private static final String CHAT = "Chat";

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
    private GoalsSteps goalsSteps;

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
                {"Отправка «chatInit» со страницы избранного по нажатию на иконку сообщений",
                        mockSnippet(BASIC_SNIPPET).getMockSnippet().setPreferContactWay(ANY)
                },
                {"Отправка «chatInit» со страницы избранного по нажатию на кнопку «Написать»",
                        mockSnippet(BASIC_SNIPPET).getMockSnippet().setPreferContactWay(CHAT)
                }
        });
    }

    @Before
    public void before() {
        passportSteps.commonAccountLogin();

        mockRule.graphqlStub(mockResponse()
                .setFavorites(favoritesResponse().offers(asList(snippet)).build())
                .setChat(chatTemplate().setIsNew(true).build())
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();

        urlSteps.testing().path(MY).path(FAVORITES).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Цель «CHAT_INIT» для нового чата")
    public void shouldSeeFavoritesChatInitGoal() {
        basePageSteps.onFavoritesPage().firstFavCard().chatButton().hover().click();

        goalsSteps.withGoalType(CHAT_INIT)
                .withCurrentPageRef()
                .withCount(1)
                .shouldExist();
    }

}
