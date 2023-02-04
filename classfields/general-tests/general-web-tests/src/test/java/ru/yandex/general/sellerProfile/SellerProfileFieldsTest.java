package ru.yandex.general.sellerProfile;

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
import ru.yandex.general.mock.MockResponse;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.UrlSteps;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.GeneralFeatures.PROFILE_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.PROFILE;
import static ru.yandex.general.consts.Pages.SELLER_PATH;
import static ru.yandex.general.element.Image.SRC;
import static ru.yandex.general.mock.MockPublicProfile.AVATAR_DUMMY_IMG_URL;
import static ru.yandex.general.mock.MockPublicProfile.AVATAR_URL;
import static ru.yandex.general.mock.MockPublicProfile.profileResponse;
import static ru.yandex.general.mock.MockPublicProfileSnippet.PROFILE_BASIC_SNIPPET;
import static ru.yandex.general.mock.MockPublicProfileSnippet.PROFILE_REZUME_SNIPPET;
import static ru.yandex.general.mock.MockPublicProfileSnippet.mockSnippet;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.page.PublicProfilePage.ACTIVE;
import static ru.yandex.general.page.PublicProfilePage.EXPIRED;
import static ru.yandex.general.utils.Utils.getRandomIntInRange;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.hasText;

@Epic(PROFILE_FEATURE)
@DisplayName("Страница профиля")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class SellerProfileFieldsTest {

    private static final String SELLER_NAME = "Евгений Олегович";
    private static final int ACTIVE_COUNT = 31;
    private static final int EXPIRED_COUNT = 125;

    private int count;
    private MockResponse mockResponse = mockResponse()
            .setCurrentUserExample()
            .setCategoriesTemplate()
            .setRegionsTemplate();

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Before
    public void before() {
        basePageSteps.setMoscowCookie();
        count = getRandomIntInRange(1, 1000);
        urlSteps.testing().path(PROFILE).path(SELLER_PATH);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Имя продавца")
    public void shouldSeeSellerName() {
        mockRule.graphqlStub(mockResponse
                .setPublicProfile(profileResponse().setSellerName(SELLER_NAME).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onProfilePage().sellerName().should(hasText(SELLER_NAME));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Аватар продавца")
    public void shouldSeeSellerAvatar() {
        mockRule.graphqlStub(mockResponse
                .setPublicProfile(profileResponse().setAvatar().build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onProfilePage().sidebar().image().should(hasAttribute(SRC, AVATAR_URL));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Заглушка аватара продавца")
    public void shouldSeeSellerNoAvatar() {
        mockRule.graphqlStub(mockResponse
                .setPublicProfile(profileResponse().removeAvatar().build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onProfilePage().sidebar().image().should(hasAttribute(SRC, AVATAR_DUMMY_IMG_URL));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кол-во подписчиков")
    public void shouldSeeSellerFollowers() {
        mockRule.graphqlStub(mockResponse
                .setPublicProfile(profileResponse().setFollowers(count).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onProfilePage().sidebar().followersCount().should(hasText(String.valueOf(count)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кол-во подписок")
    public void shouldSeeSellerFollowing() {
        mockRule.graphqlStub(mockResponse
                .setPublicProfile(profileResponse().setFollowing(count).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onProfilePage().sidebar().followingCount().should(hasText(String.valueOf(count)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Количество в табе активных")
    public void shouldSeeTabActiveCount() {
        mockRule.graphqlStub(mockResponse
                .setPublicProfile(profileResponse().snippets(asList(
                        mockSnippet(PROFILE_BASIC_SNIPPET),
                        mockSnippet(PROFILE_REZUME_SNIPPET)))
                        .setActiveCount(ACTIVE_COUNT)
                        .setExpiredCount(EXPIRED_COUNT).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onProfilePage().tab(ACTIVE).count().should(hasText(String.valueOf(ACTIVE_COUNT)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Количество в табе завершенных")
    public void shouldSeeTabExpiredCount() {
        mockRule.graphqlStub(mockResponse
                .setPublicProfile(profileResponse().snippets(asList(
                        mockSnippet(PROFILE_BASIC_SNIPPET),
                        mockSnippet(PROFILE_REZUME_SNIPPET)))
                        .setActiveCount(ACTIVE_COUNT)
                        .setExpiredCount(EXPIRED_COUNT).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onProfilePage().tab(EXPIRED).count().should(hasText(String.valueOf(EXPIRED_COUNT)));
    }

}
