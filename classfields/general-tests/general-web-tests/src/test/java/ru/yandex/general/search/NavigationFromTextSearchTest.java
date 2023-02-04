package ru.yandex.general.search;

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
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.UrlSteps;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.general.consts.GeneralFeatures.NAVIGATION_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.SEARCH_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.consts.Pages.KOMPUTERNAYA_TEHNIKA;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.NOUTBUKI;
import static ru.yandex.general.consts.Pages.SANKT_PETERBURG;
import static ru.yandex.general.consts.QueryParams.CATEGORY_VALUE;
import static ru.yandex.general.consts.QueryParams.DISTRICT_ID_PARAM;
import static ru.yandex.general.consts.QueryParams.GEO_RADIUS_PARAM;
import static ru.yandex.general.consts.QueryParams.LATITUDE_PARAM;
import static ru.yandex.general.consts.QueryParams.LOCKED_FIELDS;
import static ru.yandex.general.consts.QueryParams.LONGITUDE_PARAM;
import static ru.yandex.general.consts.QueryParams.METRO_ID_PARAM;
import static ru.yandex.general.consts.QueryParams.REGION_PARAM;
import static ru.yandex.general.consts.QueryParams.SORTING_PARAM;
import static ru.yandex.general.consts.QueryParams.SORT_BY_RELEVANCE_VALUE;
import static ru.yandex.general.consts.QueryParams.TEXT_PARAM;
import static ru.yandex.general.element.Header.LOGIN;
import static ru.yandex.general.element.SearchBar.MAP_METRO_DISTRICTS;
import static ru.yandex.general.element.SearchBar.SHOW;
import static ru.yandex.general.element.SidebarCategories.ALL_CATEGORIES;
import static ru.yandex.general.element.SuggestDropdown.DISTRICT;
import static ru.yandex.general.element.SuggestDropdown.METRO;
import static ru.yandex.general.page.BasePage.LOGIN_WITH_YANDEX_ID;
import static ru.yandex.general.page.FormPage.FORM_PAGE_H1;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(SEARCH_FEATURE)
@Feature(NAVIGATION_FEATURE)
@DisplayName("Навигация с текстового поиска")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class NavigationFromTextSearchTest {

    private static final String NOUTBUK_MACBOOK_TEXT = "ноутбук macbook";
    private static final String NOUTBUKI_TEXT = "Ноутбуки";
    private static final String OFFERS_TEXT = "Объявления";
    private static final String PLANSHETI_TEXT = "Планшеты";
    private static final String KOMPUTERNAYA_TEHNIKA_TEXT = "Компьютерная техника";
    private static final String ELEKTRONIKA_TEXT = "Электроника";


    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        basePageSteps.setMoscowCookie();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Новый текстовый поиск с текстового поиска")
    public void shouldSeeGoToNewTextSearch() {
        urlSteps.testing().path(MOSKVA).path(KOMPUTERNAYA_TEHNIKA).path(NOUTBUKI)
                .queryParam(TEXT_PARAM, NOUTBUK_MACBOOK_TEXT).open();
        basePageSteps.onListingPage().searchBar().clearInput().click();
        basePageSteps.onListingPage().searchBar().input().sendKeys(ELEKTRONIKA_TEXT);
        basePageSteps.onListingPage().searchBar().suggestItemWithCategory(ELEKTRONIKA_TEXT).click();

        basePageSteps.onListingPage().h1().should(hasText(format("Объявления по запросу «%s» в Москве", ELEKTRONIKA_TEXT.toLowerCase())));
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).queryParam(TEXT_PARAM, ELEKTRONIKA_TEXT.toLowerCase())
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на категорию в ХК с уточненного текстового поиска")
    public void shouldSeeGoToBreadcrumbCategory() {
        urlSteps.testing().path(MOSKVA).path(KOMPUTERNAYA_TEHNIKA).path(NOUTBUKI)
                .queryParam(TEXT_PARAM, NOUTBUK_MACBOOK_TEXT).open();
        basePageSteps.onListingPage().breadcrumbsItem(KOMPUTERNAYA_TEHNIKA_TEXT).click();

        basePageSteps.onListingPage().h1().should(hasText(resultH1(NOUTBUK_MACBOOK_TEXT)));
        urlSteps.testing().path(MOSKVA).path(KOMPUTERNAYA_TEHNIKA).queryParam(TEXT_PARAM, NOUTBUK_MACBOOK_TEXT)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на уточненный текстовый поиск после перехода в категорию из ХК")
    public void shouldSeeBackToCategoryFromBreadcrumbCategory() {
        urlSteps.testing().path(MOSKVA).path(KOMPUTERNAYA_TEHNIKA).path(NOUTBUKI)
                .queryParam(TEXT_PARAM, NOUTBUK_MACBOOK_TEXT).open();
        basePageSteps.onListingPage().breadcrumbsItem(KOMPUTERNAYA_TEHNIKA_TEXT).click();
        basePageSteps.onListingPage().sidebarCategories().activeCategory(KOMPUTERNAYA_TEHNIKA_TEXT).waitUntil(isDisplayed());
        basePageSteps.back();
        basePageSteps.wait500MS();

        basePageSteps.onListingPage().h1().should(hasText(resultH1(NOUTBUK_MACBOOK_TEXT)));
        basePageSteps.onListingPage().sidebarCategories().activeCategory(NOUTBUKI_TEXT).should(isDisplayed());
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на карточку с текстового поиска")
    public void shouldSeeGoToOfferCard() {
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, NOUTBUK_MACBOOK_TEXT).open();
        String offerUrl = basePageSteps.onListingPage().firstSnippet().getUrl();
        basePageSteps.onListingPage().firstSnippet().click();
        basePageSteps.switchToNextTab();

        basePageSteps.onOfferCardPage().sidebar().price().should(isDisplayed());
        urlSteps.fromUri(offerUrl).queryParam(REGION_PARAM, "213").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сброс уточнения текстового поиска по «Все категории» в сайдбаре")
    public void shouldSeeGoToParentTextSearchFromAllCategoriesSidebar() {
        urlSteps.testing().path(MOSKVA).path(KOMPUTERNAYA_TEHNIKA).path(NOUTBUKI)
                .queryParam(TEXT_PARAM, NOUTBUK_MACBOOK_TEXT).open();
        basePageSteps.onListingPage().sidebarCategories().spanLink(ALL_CATEGORIES).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        basePageSteps.onListingPage().h1().should(hasText(resultH1(NOUTBUK_MACBOOK_TEXT)));
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, NOUTBUK_MACBOOK_TEXT)
                .queryParam(LOCKED_FIELDS, CATEGORY_VALUE)
                .queryParam(SORTING_PARAM, SORT_BY_RELEVANCE_VALUE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на родительскую категорию в сайдбаре с текстового поиска")
    public void shouldSeeGoToSidebarParentCategory() {
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, NOUTBUK_MACBOOK_TEXT).open();
        basePageSteps.onListingPage().sidebarCategories().link(KOMPUTERNAYA_TEHNIKA_TEXT).click();

        basePageSteps.onListingPage().h1().should(hasText(resultH1(NOUTBUK_MACBOOK_TEXT)));
        urlSteps.testing().path(MOSKVA).path(KOMPUTERNAYA_TEHNIKA).queryParam(TEXT_PARAM, NOUTBUK_MACBOOK_TEXT)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на категорию из футера с текстового поиска")
    public void shouldSeeGoToFooterCategory() {
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, NOUTBUK_MACBOOK_TEXT).open();
        basePageSteps.scrollToBottom();
        basePageSteps.onListingPage().footer().category(ELEKTRONIKA_TEXT).waitUntil(isDisplayed()).hover().click();

        basePageSteps.onListingPage().h1().should(hasText(categoryInMoscow(ELEKTRONIKA_TEXT)));
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на город из футера с текстового поиска")
    public void shouldSeeGoToFooterCity() {
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, NOUTBUK_MACBOOK_TEXT).open();
        basePageSteps.scrollToBottom();
        basePageSteps.onListingPage().footer().category("Авто").waitUntil(isDisplayed()).hover();
        basePageSteps.onListingPage().footer().city("Санкт-Петербург").click();

        basePageSteps.onListingPage().h1().should(hasText(format("Объявления по запросу «%s» в Санкт-Петербурге", NOUTBUK_MACBOOK_TEXT)));
        urlSteps.testing().path(SANKT_PETERBURG).queryParam(TEXT_PARAM, NOUTBUK_MACBOOK_TEXT).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на форму подачи с текстового поиска")
    public void shouldSeeGoToForm() {
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, NOUTBUK_MACBOOK_TEXT).open();
        basePageSteps.onListingPage().createOffer().click();
        basePageSteps.switchToNextTab();

        basePageSteps.onListingPage().h1().should(hasText(FORM_PAGE_H1));
        urlSteps.testing().path(FORM).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем фильтр по метро с текстового поиска")
    public void shouldSeeSubwaySearch() {
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, NOUTBUK_MACBOOK_TEXT).open();
        basePageSteps.onListingPage().searchBar().button(MAP_METRO_DISTRICTS).click();
        basePageSteps.onListingPage().searchBar().suggest().button(METRO).click();
        basePageSteps.onListingPage().searchBar().suggest().station("Павелецкая").click();
        basePageSteps.onListingPage().searchBar().button(SHOW).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.queryParam(METRO_ID_PARAM, "20475").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем фильтр по району с текстового поиска")
    public void shouldSeeDistrictSearch() {
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, NOUTBUK_MACBOOK_TEXT).open();
        basePageSteps.onListingPage().searchBar().button(MAP_METRO_DISTRICTS).click();
        basePageSteps.onListingPage().searchBar().suggest().button(DISTRICT).click();
        basePageSteps.onListingPage().searchBar().suggest().checkboxWithLabel("Силино").click();
        basePageSteps.onListingPage().searchBar().button(SHOW).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.queryParam(DISTRICT_ID_PARAM, "116978").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем фильтр по адресу с текстового поиска")
    public void shouldSeeAddressSearch() {
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, NOUTBUK_MACBOOK_TEXT).open();
        basePageSteps.onListingPage().searchBar().button(MAP_METRO_DISTRICTS).click();
        basePageSteps.onListingPage().searchBar().fillSearchInput("Ленинградский проспект, 80к17");
        basePageSteps.onListingPage().searchBar().suggestItem("Ленинградский проспект, 80к17").click();
        basePageSteps.onListingPage().searchBar().button(SHOW).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.queryParam(LATITUDE_PARAM, "55.807953")
                .queryParam(LONGITUDE_PARAM, "37.511509")
                .queryParam(GEO_RADIUS_PARAM, "1000").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход по кнопке «Войти» на текстовом поиске")
    public void shouldSeeLoginButtonOnTextSearch() {
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, NOUTBUK_MACBOOK_TEXT).open();
        basePageSteps.onListingPage().header().link(LOGIN).click();
        basePageSteps.onBasePage().h1().waitUntil(hasText(LOGIN_WITH_YANDEX_ID));

        urlSteps.shouldNotDiffWith(format("https://passport.yandex.ru/auth?mode=auth&retpath=%s", urlSteps));
    }

    private String categoryInMoscow(String categoryName) {
        return format("%s в Москве", categoryName);
    }

    private String resultH1(String text) {
        return format("Объявления по запросу «%s» в Москве", text);
    }

}
