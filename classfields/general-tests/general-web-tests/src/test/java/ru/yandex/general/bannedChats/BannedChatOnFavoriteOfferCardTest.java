package ru.yandex.general.bannedChats;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.GeneralFeatures.BANNED_CHATS_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FAVORITES;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.UserStatus.BANNED;
import static ru.yandex.general.mock.MockCurrentUser.currentUserExample;
import static ru.yandex.general.mock.MockFavorites.favoritesResponse;
import static ru.yandex.general.mock.MockFavoritesSnippet.BASIC_SNIPPET;
import static ru.yandex.general.mock.MockFavoritesSnippet.mockSnippet;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(BANNED_CHATS_FEATURE)
@DisplayName("Забаненный в чатах пользователь на избранных офферах")
@RunWith(Parameterized.class)
@GuiceModules(GeneralWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class BannedChatOnFavoriteOfferCardTest {

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

    @Rule
    @Inject
    public MockRule mockRule;

    @Parameterized.Parameter
    public String title;

    @Parameterized.Parameter(1)
    public String preferContactWay;

    @Parameterized.Parameters(name = "{index}. {0}")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {"Снипет с любым способом связи", ANY},
                {"Сниппет только с чатом", CHAT}

        });
    }

    @Before
    public void before() {
        mockRule.graphqlStub(mockResponse().setCurrentUser(
                currentUserExample().setModerationChatStatus(BANNED).build())
                .setFavorites(favoritesResponse().offers(asList(
                        mockSnippet(BASIC_SNIPPET).getMockSnippet().setPreferContactWay(preferContactWay))).build())
                .build()).withDefaults().create();
        passportSteps.commonAccountLogin();
        urlSteps.testing().path(MY).path(FAVORITES).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Задизейблена кнопка «Написать» для юзера с забаненными чатами")
    public void shouldSeeDisabledStartChatButton() {
        basePageSteps.onFavoritesPage().firstFavCard().chatButton().should(hasAttribute("aria-disabled", "true"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается тултип забаненности в чатах по ховеру на «Написать»")
    public void shouldSeeBannedInChatsTooltip() {
        basePageSteps.onFavoritesPage().firstFavCard().chatButton().hover();
        basePageSteps.onFavoritesPage().popup().bannedInChatsTooltip().should(isDisplayed());
    }

}
