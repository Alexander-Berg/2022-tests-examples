package ru.yandex.general.bannedUser;

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
import static ru.yandex.general.consts.GeneralFeatures.BANNED_USER_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.SLASH;
import static ru.yandex.general.consts.UserStatus.UserBanDescriptions.FRAUD;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockCurrentUser.currentUserExample;
import static ru.yandex.general.mock.MockHomepage.homepageResponse;
import static ru.yandex.general.mock.MockPublicProfile.profileResponse;
import static ru.yandex.general.mock.MockPublicProfileSnippet.PROFILE_BASIC_SNIPPET;
import static ru.yandex.general.mock.MockPublicProfileSnippet.mockSnippet;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.mock.MockSearch.categoryListingExample;
import static ru.yandex.qatools.htmlelements.matchers.common.HasAttributeMatcher.hasAttribute;

@Epic(BANNED_USER_FEATURE)
@DisplayName("Кнопка «Разместить» задизейблена для забанненого юзера на разных страницах")
@RunWith(Parameterized.class)
@GuiceModules(GeneralWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class InactiveSellButtonTest {

    private static final String CARD_ID = "1111111";

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
    public String path;

    @Parameterized.Parameters(name = "{index}. {0}")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {"Кнопка «Разместить» задизейблена на главной»", SLASH
                },
                {"Кнопка «Разместить» задизейблена на листинге категории", ELEKTRONIKA
                },
                {"Кнопка «Разместить» задизейблена на карточке оффера", CARD + CARD_ID + SLASH
                },
                {"Кнопка «Разместить» задизейблена на листинге своих офферов", "/my/offers/"
                },
                {"Кнопка «Разместить» задизейблена на избранных", "/my/favorites/"
                },
                {"Кнопка «Разместить» задизейблена на странице фидов", "/my/feed/"
                },
                {"Кнопка «Разместить» задизейблена на настройках профиля", "/my/contacts/"
                },
                {"Кнопка «Разместить» задизейблена на публичном профиле юзера", "/profile/testprofile/"
                }

        });
    }

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
        mockRule.graphqlStub(mockResponse()
                .setCurrentUser(currentUserExample().setUserBannedWithDescription(FRAUD).build())
                .setCard(mockCard(BASIC_CARD).setId(CARD_ID).build())
                .setCategoriesTemplate()
                .setHomepage(homepageResponse().addOffers(5).build())
                .setSearch(categoryListingExample().build())
                .setPublicProfile(profileResponse().snippets(asList(mockSnippet(PROFILE_BASIC_SNIPPET))).build())
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.testing().path(path).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кнопка «Разместить» задизейблена для забанненого юзера на разных страницах")
    public void shouldSeeInactiveSellButton() {
        basePageSteps.onBasePage().createOffer().should(hasAttribute("aria-disabled", "true"));
    }

}
