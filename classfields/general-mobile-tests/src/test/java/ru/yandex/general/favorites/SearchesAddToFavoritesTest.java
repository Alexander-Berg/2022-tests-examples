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
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.lanwen.diff.uri.core.util.URLCoder.encode;
import static ru.yandex.general.consts.GeneralFeatures.FAVORITES_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.FAVORITE_SEARCHES;
import static ru.yandex.general.consts.Notifications.DONE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Owners.ILUHA;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.FAVORITES;
import static ru.yandex.general.consts.Pages.KOMPUTERNAYA_TEHNIKA;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.NOUTBUKI;
import static ru.yandex.general.consts.Pages.STATE_USED;
import static ru.yandex.general.consts.QueryParams.LOCKED_FIELDS;
import static ru.yandex.general.consts.QueryParams.MOSCOW_ID_VALUE;
import static ru.yandex.general.consts.QueryParams.OFFER_STATE_PARAM;
import static ru.yandex.general.consts.QueryParams.PRICE_MAX_URL_PARAM;
import static ru.yandex.general.consts.QueryParams.PRICE_MIN_URL_PARAM;
import static ru.yandex.general.consts.QueryParams.REGION_ID_PARAM;
import static ru.yandex.general.consts.QueryParams.SORTING_PARAM;
import static ru.yandex.general.consts.QueryParams.SORT_BY_RELEVANCE_VALUE;
import static ru.yandex.general.consts.QueryParams.TEXT_PARAM;
import static ru.yandex.general.consts.QueryParams.TEXT_VALUE;
import static ru.yandex.general.consts.QueryParams.USED_VALUE;
import static ru.yandex.general.matchers.UrlMatcher.hasNoDiffWithUrl;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(FAVORITES_FEATURE)
@Feature(FAVORITE_SEARCHES)
@DisplayName("Добавление в избранное. Поиски")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class SearchesAddToFavoritesTest {

    private static final String TEXT_SEARCH = "книги";
    private static final String NOT_INTERESTED = "Вам это больше не интересно?";
    private static final String TAB_URL_PARAM = "tab";
    private static final String SEARCHES_URL_VALUE = "searches";
    private static final String HREF_ATTRIBUTE = "href";
    private static final String PROIZVODITEL_NOUTBUKOV_APPLE = "/proizvoditel-noutbukov-apple/";
    private static final String PRICE_MAX_URL_VALUE = "100000";
    private static final String PRICE_MIN_URL_VALUE = "1000";
    private static final String PROIZVODITEL_PARAM = "offer.attributes.proizvoditel-noutbukov_vAeFtC";
    private static final String APPLE = "apple";
    private static final String YES = "Да";
    private static final String TEXT_WITH_MISSPELL = "акамулятор";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Before
    public void before() {
        passportSteps.createAccountAndLogin();
        basePageSteps.setMoscowCookie();
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Добавление в избранное поиска из базовой категории")
    public void shouldAddFromBaseCategory() {
        urlSteps.testing().path(ELEKTRONIKA).open();
        basePageSteps.onListingPage().searchBar().saveSearch().click();
        basePageSteps.onListingPage().popup().button(YES).click();
        basePageSteps.onListingPage().popupNotification(DONE).waitUntil(isDisplayed());
        urlSteps.testing().path(MY).path(FAVORITES).queryParam(TAB_URL_PARAM, SEARCHES_URL_VALUE).open();

        basePageSteps.onFavoritesPage().favoritesCards().should(hasSize(1));
        basePageSteps.onFavoritesPage().favoritesCards().get(0).link()
                .should(hasAttribute(HREF_ATTRIBUTE, containsString(ELEKTRONIKA)));
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Удаление избранного поиска из базовой категории")
    public void shouldRemoveFromBaseCategory() {
        urlSteps.testing().path(ELEKTRONIKA).open();
        basePageSteps.onListingPage().searchBar().saveSearch().click();
        basePageSteps.onListingPage().popup().button(YES).click();
        basePageSteps.onListingPage().popupNotification(DONE).waitUntil(isDisplayed());
        basePageSteps.onListingPage().searchBar().saveSearch().click();
        basePageSteps.onListingPage().popup().waitUntil(isDisplayed())
                .waitUntil(hasText(containsString(NOT_INTERESTED)));
        waitPopupAnimation();
        basePageSteps.onListingPage().popup().button("Удалить").click();
        basePageSteps.onListingPage().popupNotification("Поиск удалён").waitUntil(isDisplayed());
        urlSteps.testing().path(MY).path(FAVORITES).queryParam(TAB_URL_PARAM, SEARCHES_URL_VALUE).open();

        basePageSteps.onFavoritesPage().favoritesCards().should(hasSize(0));
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Добавление в избранное поиска из плавающего хедера")
    public void shouldAddFromSearch() {
        urlSteps.testing().queryParam(TEXT_PARAM, TEXT_SEARCH).open();
        basePageSteps.scrollDown(700);
        basePageSteps.waitSomething(500, TimeUnit.MILLISECONDS);
        basePageSteps.scrollUp(200);
        basePageSteps.onListingPage().stickySearchBar().saveSearch().waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().popup().button(YES).click();
        basePageSteps.onListingPage().popupNotification(DONE).waitUntil(isDisplayed());
        urlSteps.testing().path(MY).path(FAVORITES).queryParam(TAB_URL_PARAM, SEARCHES_URL_VALUE).open();

        basePageSteps.onFavoritesPage().favoritesCards().should(hasSize(1));
        basePageSteps.onFavoritesPage().favoritesCards().get(0).link()
                .should(hasAttribute(HREF_ATTRIBUTE, containsString(encode(TEXT_SEARCH))));
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Добавление в избранное поиска с несколькими фильтрами и переход обратно в поиск")
    public void shouldAddFromFilters() {
        urlSteps.testing().path(KOMPUTERNAYA_TEHNIKA).path(NOUTBUKI).path(STATE_USED)
                .path(PROIZVODITEL_NOUTBUKOV_APPLE)
                .queryParam(PRICE_MAX_URL_PARAM, PRICE_MAX_URL_VALUE)
                .queryParam(PRICE_MIN_URL_PARAM, PRICE_MIN_URL_VALUE)
                .queryParam(REGION_ID_PARAM, MOSCOW_ID_VALUE).open();
        basePageSteps.onListingPage().searchBar().saveSearch().waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().popup().button(YES).click();
        basePageSteps.onListingPage().popupNotification(DONE).waitUntil(isDisplayed());
        urlSteps.testing().path(MY).path(FAVORITES).queryParam(TAB_URL_PARAM, SEARCHES_URL_VALUE).open();
        basePageSteps.onFavoritesPage().favoritesCards().waitUntil(hasSize(1));
        basePageSteps.onFavoritesPage().favoritesCards().get(0).hover();
        basePageSteps.onFavoritesPage().favoritesCards().get(0).link().click();
        basePageSteps.onListingPage().searchBar().waitUntil(isDisplayed());

        assertThat("URL соответствует", basePageSteps.getDriver(), anyOf(
                hasNoDiffWithUrl(urlSteps.testing().path(MOSKVA).path(KOMPUTERNAYA_TEHNIKA).path(NOUTBUKI)
                        .path(STATE_USED)
                        .queryParam(PROIZVODITEL_PARAM, APPLE)
                        .queryParam(PRICE_MAX_URL_PARAM, PRICE_MAX_URL_VALUE)
                        .queryParam(PRICE_MIN_URL_PARAM, PRICE_MIN_URL_VALUE)
                        .queryParam(SORTING_PARAM, SORT_BY_RELEVANCE_VALUE).toString(), newArrayList()),
                hasNoDiffWithUrl(urlSteps.testing().path(MOSKVA).path(KOMPUTERNAYA_TEHNIKA).path(NOUTBUKI)
                        .path(PROIZVODITEL_NOUTBUKOV_APPLE)
                        .queryParam(PRICE_MAX_URL_PARAM, PRICE_MAX_URL_VALUE)
                        .queryParam(PRICE_MIN_URL_PARAM, PRICE_MIN_URL_VALUE)
                        .queryParam(SORTING_PARAM, SORT_BY_RELEVANCE_VALUE)
                        .queryParam(OFFER_STATE_PARAM, USED_VALUE).toString(), newArrayList())));
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Переход в избранное из всплывающего попапа")
    public void shouldPassToSearches() {
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).open();
        basePageSteps.onListingPage().searchBar().saveSearch().click();
        basePageSteps.onListingPage().popup().button(YES).click();
        basePageSteps.onListingPage().popupNotification(DONE).button("Перейти").click();
        urlSteps.testing().path(MY).path(FAVORITES)
                .queryParam(TAB_URL_PARAM, SEARCHES_URL_VALUE).shouldNotDiffWithWebDriverUrl();

        basePageSteps.onFavoritesPage().favoritesCards().should(hasSize(1));
        basePageSteps.onFavoritesPage().favoritesCards().get(0).link()
                .should(hasAttribute(HREF_ATTRIBUTE,
                        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA)
                                .queryParam(SORTING_PARAM, SORT_BY_RELEVANCE_VALUE).toString()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавление в избранное поиска с опечаткой")
    public void shouldAddSearchWithMisspell() {
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, TEXT_WITH_MISSPELL)
                .queryParam(LOCKED_FIELDS, TEXT_VALUE).open();
        basePageSteps.onListingPage().searchBar().saveSearch().click();
        basePageSteps.onListingPage().popup().button(YES).click();
        basePageSteps.onListingPage().popupNotification(DONE).waitUntil(isDisplayed());
        urlSteps.testing().path(MY).path(FAVORITES).queryParam(TAB_URL_PARAM, SEARCHES_URL_VALUE).open();

        basePageSteps.onFavoritesPage().favoritesCards().should(hasSize(1));
        basePageSteps.onFavoritesPage().favoritesCards().get(0).link()
                .should(hasAttribute(HREF_ATTRIBUTE,
                        urlSteps.testing().path(MOSKVA).queryParam(SORTING_PARAM, SORT_BY_RELEVANCE_VALUE)
                                .queryParam(TEXT_PARAM, TEXT_WITH_MISSPELL)
                                .queryParam(LOCKED_FIELDS, TEXT_VALUE).toString()));
    }

    private void waitPopupAnimation() {
        waitSomething(2, TimeUnit.SECONDS);
    }

}
