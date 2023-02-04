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
import static ru.yandex.general.consts.GeneralFeatures.FAVORITES_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.FAVORITE_SEARCHES;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FAVORITES;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.QueryParams.SEARCHES_TAB_VALUE;
import static ru.yandex.general.consts.QueryParams.TAB_PARAM;
import static ru.yandex.general.element.Link.HREF;
import static ru.yandex.general.mock.MockFavorites.favoritesResponse;
import static ru.yandex.general.mock.MockFavoritesSearch.mockBasicSearch;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.hasText;

@Epic(FAVORITES_FEATURE)
@Feature(FAVORITE_SEARCHES)
@DisplayName("Поля сохраненного поиска")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class FavoriteSearchFieldsTest {

    private static final String TITLE = "Тайтл поиска";
    private static final String SUBTITLE = "от 1, Новый, Б/У, Apple, Asus, Dell, от 1500";
    private static final String URL = "/moskva/komputernaya-tehnika/noutbuki/?sorting=ByRelevance";

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
                .setFavorites(favoritesResponse().searches(asList(
                        mockBasicSearch()
                                .setTitle(TITLE)
                                .setSubtitle(SUBTITLE)
                                .setSearchLink(URL))).build())
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        basePageSteps.setMoscowCookie();
        urlSteps.testing().path(MY).path(FAVORITES).queryParam(TAB_PARAM, SEARCHES_TAB_VALUE).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Тайтл сохраненного поиска")
    public void shouldSeeFavoriteSearchTitle() {
        basePageSteps.onFavoritesPage().firstFavCard().title().should(hasText(TITLE));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Фильтр сохраненного поиска текстом")
    public void shouldSeeFavoriteSearchFilters() {
        basePageSteps.onFavoritesPage().firstFavCard().filters().should(hasText(SUBTITLE));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ссылка сохраненного поиска")
    public void shouldSeeFavoriteSearchLink() {
        basePageSteps.onFavoritesPage().firstFavCard().link().should(hasAttribute(HREF,
                urlSteps.testing().uri(URL).toString()));
    }

}
