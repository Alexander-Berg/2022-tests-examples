package ru.yandex.general.favorites;

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
import ru.yandex.general.mock.MockResponse;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static java.util.Arrays.asList;
import static org.hamcrest.core.StringContains.containsString;
import static ru.yandex.general.consts.GeneralFeatures.FAVORITES_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.FAVORITE_SELLERS;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FAVORITES;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.QueryParams.PROFILES_TAB_VALUE;
import static ru.yandex.general.consts.QueryParams.TAB_PARAM;
import static ru.yandex.general.mock.MockFavorites.favoritesResponse;
import static ru.yandex.general.mock.MockFavoritesSeller.mockBasicSeller;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.qatools.htmlelements.matchers.common.HasAttributeMatcher.hasAttribute;

@Epic(FAVORITES_FEATURE)
@Feature(FAVORITE_SELLERS)
@DisplayName("Поля сохраненного продавца")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class FavoriteSellerAvatarTest {

    private static final String DUMMY_IMG = "https://avatars.mds.yandex.net/get-yapic/0/0-0/";
    private static final String AVATAR_IMG = "https://avatars.mdst.yandex.net/get-yapic/1450/wSPcK5KpK2UKWEnj2Al8V1h88-1/";

    private MockResponse mockResponse;

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

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
        mockResponse = mockResponse()
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate();
        urlSteps.testing().path(MY).path(FAVORITES).queryParam(TAB_PARAM, PROFILES_TAB_VALUE);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Заглушка аватара сохраненного продавца")
    public void shouldSeeFavoriteSellerAvatarDummy() {
        mockResponse.setFavorites(favoritesResponse().sellers(asList(
                mockBasicSeller())).build());
        mockRule.graphqlStub(mockResponse.build()).withDefaults().create();

        urlSteps.open();
        basePageSteps.onFavoritesPage().firstFavCard().avatar().should(hasAttribute("src", containsString(DUMMY_IMG)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Аватар сохраненного продавца")
    public void shouldSeeFavoriteSellerAvatar() {
        mockResponse.setFavorites(favoritesResponse().sellers(asList(
                mockBasicSeller().setAvatar())).build());
        mockRule.graphqlStub(mockResponse.build()).withDefaults().create();

        urlSteps.open();
        basePageSteps.onFavoritesPage().firstFavCard().avatar().should(hasAttribute("src", containsString(AVATAR_IMG)));
    }

}
