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

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.general.consts.GeneralFeatures.FAVORITES_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.FAVORITE_OFFERS;
import static ru.yandex.general.consts.GeneralFeatures.FAVORITE_SEARCHES;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FAVORITES;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.QueryParams.SEARCHES_TAB_VALUE;
import static ru.yandex.general.consts.QueryParams.TAB_PARAM;
import static ru.yandex.general.mock.MockFavorites.favoritesResponse;
import static ru.yandex.general.mock.MockFavoritesSearch.mockBasicSearch;
import static ru.yandex.general.mock.MockFavoritesSnippet.BASIC_SNIPPET;
import static ru.yandex.general.mock.MockFavoritesSnippet.mockSnippet;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.isDisplayed;

@Epic(FAVORITES_FEATURE)
@DisplayName("Отображение заглушки фото")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class FavoritesDummyImageTest {

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
                .setFavorites(favoritesResponse()
                        .offers(asList(mockSnippet(BASIC_SNIPPET).getMockSnippet().removePhotos()))
                        .searches(asList(mockBasicSearch().removePhotos())).build())
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        basePageSteps.setMoscowCookie();
        urlSteps.testing().path(MY).path(FAVORITES);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FAVORITE_OFFERS)
    @DisplayName("Заглушка фото карточки сохраненного оффера")
    public void shouldSeeFavoriteCardDummyImg() {
        urlSteps.open();

        basePageSteps.onFavoritesPage().firstFavCard().dummyImg().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Feature(FAVORITE_SEARCHES)
    @DisplayName("Заглушки фото сохраненного поиска")
    public void shouldSeeFavoriteSearchDummyImgs() {
        urlSteps.queryParam(TAB_PARAM, SEARCHES_TAB_VALUE).open();

        basePageSteps.onFavoritesPage().firstFavCard().dummyImgs().should(hasSize(4));
    }

}
