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
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.GeneralFeatures.BANNED_CHATS_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FAVORITES;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.UserStatus.BANNED;
import static ru.yandex.general.mobile.element.FavCard.WRITE;
import static ru.yandex.general.mock.MockCurrentUser.currentUserExample;
import static ru.yandex.general.mock.MockFavorites.favoritesResponse;
import static ru.yandex.general.mock.MockFavoritesSnippet.BASIC_SNIPPET;
import static ru.yandex.general.mock.MockFavoritesSnippet.mockSnippet;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(BANNED_CHATS_FEATURE)
@DisplayName("Забаненный в чатах пользователь на избранных офферах")
@RunWith(Parameterized.class)
@GuiceModules(GeneralMobileWebModule.class)
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
    @DisplayName("Отображается попап забаненности в чатах по «Написать»")
    public void shouldSeeBannedInChatsTooltip() {
        basePageSteps.onFavoritesPage().firstFavCard().button(WRITE).click();
        basePageSteps.onFavoritesPage().popup().chatBannedModal().should(isDisplayed());
    }

}
