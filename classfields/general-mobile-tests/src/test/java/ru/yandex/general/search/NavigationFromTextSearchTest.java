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
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
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
import static ru.yandex.general.consts.Pages.PLANSHETI;
import static ru.yandex.general.consts.Pages.TAG;
import static ru.yandex.general.consts.QueryParams.CATEGORY_VALUE;
import static ru.yandex.general.consts.QueryParams.LOCKED_FIELDS;
import static ru.yandex.general.consts.QueryParams.NEW_VALUE;
import static ru.yandex.general.consts.QueryParams.OFFER_STATE_PARAM;
import static ru.yandex.general.consts.QueryParams.PRICE_MIN_URL_PARAM;
import static ru.yandex.general.consts.QueryParams.REGION_PARAM;
import static ru.yandex.general.consts.QueryParams.SORTING_PARAM;
import static ru.yandex.general.consts.QueryParams.SORT_BY_RELEVANCE_VALUE;
import static ru.yandex.general.consts.QueryParams.TEXT_PARAM;
import static ru.yandex.general.mobile.element.Wrapper.DONE;
import static ru.yandex.general.mobile.element.Wrapper.FIND;
import static ru.yandex.general.mobile.page.FormPage.FORM_PAGE_H1;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(SEARCH_FEATURE)
@Feature(NAVIGATION_FEATURE)
@DisplayName("Навигация с текстового поиска")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class NavigationFromTextSearchTest {

    private static final String NOUTBUK_MACBOOK_TEXT = "ноутбук macbook";
    private static final String NOUTBUKI_TEXT = "Ноутбуки";
    private static final String ACCESSORIES_TEXT = "Аксессуары";
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
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().header().back().click();

        basePageSteps.onListingPage().h1().should(hasText(resultH1(NOUTBUK_MACBOOK_TEXT)));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на карточку с текстового поиска")
    public void shouldSeeGoToOfferCard() {
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, NOUTBUK_MACBOOK_TEXT).open();
        String offerUrl = basePageSteps.onListingPage().firstSnippet().getUrl();
        basePageSteps.onListingPage().firstSnippet().click();

        basePageSteps.onOfferCardPage().priceBuyer().should(isDisplayed());
        urlSteps.fromUri(offerUrl).queryParam(REGION_PARAM, "213").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на текстовый поиск после перехода на карточку")
    public void shouldSeeBackToTextSearchFromOfferCard() {
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, NOUTBUK_MACBOOK_TEXT).open();
        basePageSteps.onListingPage().firstSnippet().click();
        basePageSteps.onOfferCardPage().priceBuyer().waitUntil(isDisplayed());
        basePageSteps.onListingPage().header().back().click();

        basePageSteps.onListingPage().h1().should(hasText(resultH1(NOUTBUK_MACBOOK_TEXT)));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на уточненный текстовый поиск после перехода на карточку")
    public void shouldSeeBackToTextSearchWithCategoryFromOfferCard() {
        urlSteps.testing().path(MOSKVA).path(KOMPUTERNAYA_TEHNIKA).path(NOUTBUKI)
                .queryParam(TEXT_PARAM, NOUTBUK_MACBOOK_TEXT).open();
        basePageSteps.onListingPage().firstSnippet().click();
        basePageSteps.onOfferCardPage().priceBuyer().waitUntil(isDisplayed());
        basePageSteps.onListingPage().header().back().click();

        basePageSteps.onListingPage().h1().should(hasText(resultH1(NOUTBUK_MACBOOK_TEXT)));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на текстовый поиск «tag» после перехода на карточку")
    public void shouldSeeBackToTagTextSearchFromOfferCard() {
        urlSteps.testing().path(MOSKVA).path(TAG).path("/noutbuk/").open();
        basePageSteps.onListingPage().firstSnippet().click();
        basePageSteps.onOfferCardPage().priceBuyer().waitUntil(isDisplayed());
        basePageSteps.onListingPage().header().back().click();

        basePageSteps.onListingPage().h1().should(hasText(resultH1("ноутбук")));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на текстовый поиск с фильтрами после перехода на карточку")
    public void shouldSeeBackToCategoryWithFiltersFromOfferCard() {
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, NOUTBUK_MACBOOK_TEXT)
                .queryParam(PRICE_MIN_URL_PARAM, "1000").queryParam(OFFER_STATE_PARAM, NEW_VALUE).open();
        basePageSteps.onListingPage().firstSnippet().click();
        basePageSteps.onOfferCardPage().priceBuyer().waitUntil(isDisplayed());
        basePageSteps.onListingPage().header().back().click();

        basePageSteps.onListingPage().h1().should(hasText(resultH1(NOUTBUK_MACBOOK_TEXT)));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на форму подачи с текстового поиска")
    public void shouldSeeGoToForm() {
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, NOUTBUK_MACBOOK_TEXT).open();
        basePageSteps.onListingPage().tabBar().addOffer().click();

        basePageSteps.onFormPage().h1().should(hasText(FORM_PAGE_H1));
        urlSteps.testing().path(FORM).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на текстовый поиск с формы")
    public void shouldSeeBackToTextSearchFromForm() {
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, NOUTBUK_MACBOOK_TEXT).open();
        basePageSteps.onListingPage().tabBar().addOffer().click();
        basePageSteps.onFormPage().h1().waitUntil(hasText(FORM_PAGE_H1));
        basePageSteps.onFormPage().close().click();

        basePageSteps.onListingPage().h1().should(hasText(resultH1(NOUTBUK_MACBOOK_TEXT)));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сброс уточнения текстового поиска по чипсине")
    public void shouldSeeGoToParentTextSearchFromChips() {
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, NOUTBUK_MACBOOK_TEXT).open();
        basePageSteps.onListingPage().categories().spanLink(NOUTBUKI_TEXT).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        basePageSteps.onListingPage().h1().should(hasText(resultH1(NOUTBUK_MACBOOK_TEXT)));
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, NOUTBUK_MACBOOK_TEXT)
                .queryParam(LOCKED_FIELDS, CATEGORY_VALUE)
                .queryParam(SORTING_PARAM, SORT_BY_RELEVANCE_VALUE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сброс уточнения текстового поиска из фильтров")
    public void shouldSeeGoToParentTextSearchFromFilter() {
        urlSteps.testing().path(MOSKVA).path(KOMPUTERNAYA_TEHNIKA).path(NOUTBUKI)
                .queryParam(TEXT_PARAM, NOUTBUK_MACBOOK_TEXT).open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().filters().categorySelector().clearButton().click();
        basePageSteps.onListingPage().filters().showOffers().click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        basePageSteps.onListingPage().h1().should(hasText(resultH1(NOUTBUK_MACBOOK_TEXT)));
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, NOUTBUK_MACBOOK_TEXT)
                .queryParam(SORTING_PARAM, SORT_BY_RELEVANCE_VALUE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Смена уточняющей категории из фильтров текстового поиска")
    public void shouldSeeGoToAnotherCategoryTextSearchFromFilter() {
        basePageSteps.resize(375, 1500);
        urlSteps.testing().path(MOSKVA).path(KOMPUTERNAYA_TEHNIKA).path(NOUTBUKI)
                .queryParam(TEXT_PARAM, NOUTBUK_MACBOOK_TEXT).open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().filters().categorySelector().click();
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().popup().backButton().click();
        basePageSteps.onListingPage().popup().spanLink(ACCESSORIES_TEXT).waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().popup().button(DONE).waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().filters().showOffers().click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        basePageSteps.onListingPage().h1().should(hasText(resultH1(NOUTBUK_MACBOOK_TEXT)));
        urlSteps.testing().path(MOSKVA).path("/aksessuary-dlya-kompyuternoy-tehniki/")
                .queryParam(TEXT_PARAM, NOUTBUK_MACBOOK_TEXT)
                .queryParam(SORTING_PARAM, SORT_BY_RELEVANCE_VALUE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на категорию из футера с текстового поиска")
    public void shouldSeeGoToFooterCategory() {
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, NOUTBUK_MACBOOK_TEXT).open();
        basePageSteps.scrollToBottom();
        basePageSteps.onListingPage().footer().category(ELEKTRONIKA_TEXT).waitUntil(isDisplayed()).click();

        basePageSteps.onListingPage().h1().should(hasText(categoryInMoscow(ELEKTRONIKA_TEXT)));
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на текстовый поиск после перехода на категорию из футера")
    public void shouldSeeBackToTextSearchFromFooterCategory() {
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, NOUTBUK_MACBOOK_TEXT).open();
        basePageSteps.scrollToBottom();
        basePageSteps.onListingPage().footer().category("Компьютерная техника").click();
        basePageSteps.onListingPage().h1().waitUntil(hasText(categoryInMoscow("Компьютерная техника")));
        basePageSteps.onListingPage().header().back().click();

        basePageSteps.onListingPage().h1().should(hasText(resultH1(NOUTBUK_MACBOOK_TEXT)));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на другой текстовый поиск с текстового поиска")
    public void shouldSeeTextSearchFromCategoryListing() {
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, NOUTBUK_MACBOOK_TEXT).open();
        basePageSteps.onListingPage().searchBar().openSearch().click();
        basePageSteps.onListingPage().wrapper().searchClearButton().click();
        basePageSteps.onListingPage().wrapper().input().sendKeys(ELEKTRONIKA_TEXT);
        basePageSteps.onListingPage().wrapper().button(FIND).click();

        basePageSteps.onListingPage().h1().should(hasText(resultH1(ELEKTRONIKA_TEXT)));
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, ELEKTRONIKA_TEXT).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на предыдущий текстовый поиск с текстового поиска")
    public void shouldSeeBackToTextSearchFromAnotherTextSearch() {
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, NOUTBUK_MACBOOK_TEXT).open();
        basePageSteps.onListingPage().searchBar().openSearch().click();
        basePageSteps.onListingPage().wrapper().searchClearButton().click();
        basePageSteps.onListingPage().wrapper().input().sendKeys(ELEKTRONIKA_TEXT);
        basePageSteps.onListingPage().wrapper().button(FIND).click();
        basePageSteps.onListingPage().h1().waitUntil(hasText(resultH1(ELEKTRONIKA_TEXT)));
        basePageSteps.onListingPage().header().back().click();

        basePageSteps.onListingPage().h1().should(hasText(resultH1(NOUTBUK_MACBOOK_TEXT)));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет кнопки «Назад» при открытии текстового поиска по прямой ссылке")
    public void shouldNotSeeBackButton() {
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, NOUTBUK_MACBOOK_TEXT).open();

        basePageSteps.onListingPage().header().back().should(not(isDisplayed()));
    }

    private String categoryInMoscow(String categoryName) {
        return format("%s в Москве", categoryName);
    }

    private String resultH1(String text) {
        return format("Объявления по запросу «%s» в Москве", text);
    }

}
