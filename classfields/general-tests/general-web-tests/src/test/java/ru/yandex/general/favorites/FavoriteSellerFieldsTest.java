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
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.yandex.general.consts.GeneralFeatures.FAVORITES_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.FAVORITE_SELLERS;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FAVORITES;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.QueryParams.PROFILES_TAB_VALUE;
import static ru.yandex.general.consts.QueryParams.TAB_PARAM;
import static ru.yandex.general.element.Link.HREF;
import static ru.yandex.general.mock.MockFavorites.favoritesResponse;
import static ru.yandex.general.mock.MockFavoritesSeller.mockBasicSeller;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.hasText;

@Epic(FAVORITES_FEATURE)
@Feature(FAVORITE_SELLERS)
@DisplayName("Поля сохраненного продавца")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class FavoriteSellerFieldsTest {

    private static final String SELLER_NAME = "Тестовый Продавец";
    private static final int OFFERS_COUNT = 29;
    private static final String PROFILE_URL = "/profile/testprofileurl/";

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
        mockRule.graphqlStub(mockResponse()
                .setFavorites(favoritesResponse().sellers(asList(
                        mockBasicSeller()
                                .setSellerName(SELLER_NAME)
                                .setOffersCount(OFFERS_COUNT)
                                .setPublicProfileUrl(PROFILE_URL))).build())
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        basePageSteps.setMoscowCookie();
        urlSteps.testing().path(MY).path(FAVORITES).queryParam(TAB_PARAM, PROFILES_TAB_VALUE).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Тайтл сохраненного продавца")
    public void shouldSeeFavoriteSellerTitle() {
        basePageSteps.onFavoritesPage().firstFavCard().title().should(hasText(SELLER_NAME));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кол-во офферов сохраненного продавца")
    public void shouldSeeFavoriteSellerOffersCount() {
        basePageSteps.onFavoritesPage().firstFavCard().subtitle().should(hasText(format("%d объявлений", OFFERS_COUNT)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылка сохраненного продавца")
    public void shouldSeeFavoriteSellerLink() {
        basePageSteps.onFavoritesPage().firstFavCard().link().should(hasAttribute(HREF,
                urlSteps.testing().uri(PROFILE_URL).toString()));
    }

}
