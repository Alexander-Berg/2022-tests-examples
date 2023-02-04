package ru.yandex.general.listing;

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
import static ru.yandex.general.consts.GeneralFeatures.LISTING_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.NAVIGATION_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.consts.Pages.KOMPUTERI;
import static ru.yandex.general.consts.Pages.KOMPUTERNAYA_TEHNIKA;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.NOUTBUKI;
import static ru.yandex.general.consts.QueryParams.NEW_VALUE;
import static ru.yandex.general.consts.QueryParams.OFFER_STATE_PARAM;
import static ru.yandex.general.consts.QueryParams.PRICE_MIN_URL_PARAM;
import static ru.yandex.general.consts.QueryParams.REGION_PARAM;
import static ru.yandex.general.consts.QueryParams.SORTING_PARAM;
import static ru.yandex.general.consts.QueryParams.SORT_BY_RELEVANCE_VALUE;
import static ru.yandex.general.consts.QueryParams.TEXT_PARAM;
import static ru.yandex.general.mobile.element.FiltersPopup.NOVIY;
import static ru.yandex.general.mobile.element.Wrapper.FIND;
import static ru.yandex.general.mobile.page.FormPage.FORM_PAGE_H1;
import static ru.yandex.general.mobile.page.ListingPage.PARAMETERS;
import static ru.yandex.general.mobile.page.ListingPage.PRICE;
import static ru.yandex.general.mobile.page.ListingPage.PRICE_FROM;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(LISTING_FEATURE)
@Feature(NAVIGATION_FEATURE)
@DisplayName("Навигация с листинга категории")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class NavigationFromCategoryTest {

    private static final String NOUTBUKI_TEXT = "Ноутбуки";
    private static final String KOMPUTERI_TEXT = "Компьютеры";
    private static final String OFFERS_TEXT = "Объявления";
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
        urlSteps.testing().path(MOSKVA).path(KOMPUTERNAYA_TEHNIKA).path(KOMPUTERI).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на уточняющую категорию с листинга категории")
    public void shouldSeeGoToChildCategory() {
        basePageSteps.onListingPage().categories().link(NOUTBUKI_TEXT).click();

        basePageSteps.onListingPage().h1().should(hasText(categoryInMoscow(NOUTBUKI_TEXT)));
        urlSteps.testing().path(MOSKVA).path(KOMPUTERNAYA_TEHNIKA).path(NOUTBUKI).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на листинг категории с дочерней категории")
    public void shouldSeeBackToCategoryFromChildCategory() {
        basePageSteps.onListingPage().categories().link(NOUTBUKI_TEXT).click();
        basePageSteps.onListingPage().h1().waitUntil(hasText(categoryInMoscow(NOUTBUKI_TEXT)));
        basePageSteps.onListingPage().header().back().click();

        basePageSteps.onListingPage().h1().should(hasText(categoryInMoscow(KOMPUTERI_TEXT)));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на категорию в ХК с листинга категории")
    public void shouldSeeGoToBreadcrumbCategory() {
        basePageSteps.onListingPage().breadcrumbsItem(KOMPUTERNAYA_TEHNIKA_TEXT).click();

        basePageSteps.onListingPage().h1().should(hasText(categoryInMoscow(KOMPUTERNAYA_TEHNIKA_TEXT)));
        urlSteps.testing().path(MOSKVA).path(KOMPUTERNAYA_TEHNIKA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на листинг категории после перехода в категорию из ХК")
    public void shouldSeeBackToCategoryFromBreadcrumbCategory() {
        basePageSteps.onListingPage().breadcrumbsItem(KOMPUTERNAYA_TEHNIKA_TEXT).click();
        basePageSteps.onListingPage().h1().waitUntil(hasText(categoryInMoscow(KOMPUTERNAYA_TEHNIKA_TEXT)));
        basePageSteps.onListingPage().header().back().click();

        basePageSteps.onListingPage().h1().should(hasText(categoryInMoscow(KOMPUTERI_TEXT)));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на карточку с категории")
    public void shouldSeeGoToOfferCard() {
        String offerUrl = basePageSteps.onListingPage().firstSnippet().getUrl();
        basePageSteps.onListingPage().firstSnippet().click();

        basePageSteps.onOfferCardPage().priceBuyer().should(isDisplayed());
        urlSteps.fromUri(offerUrl).queryParam(REGION_PARAM, "213").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на категорию после перехода на карточку")
    public void shouldSeeBackToCategoryFromOfferCard() {
        basePageSteps.onListingPage().firstSnippet().click();
        basePageSteps.onOfferCardPage().priceBuyer().waitUntil(isDisplayed());
        basePageSteps.onListingPage().header().back().click();

        basePageSteps.onListingPage().h1().should(hasText(categoryInMoscow(KOMPUTERI_TEXT)));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на категорию после перехода на карточку и открытия галереи")
    public void shouldSeeBackToCategoryFromOfferCardAfterOpenGallery() {
        basePageSteps.onListingPage().firstSnippet().click();
        basePageSteps.onOfferCardPage().priceBuyer().waitUntil(isDisplayed());
        basePageSteps.onOfferCardPage().photoPreviewList().get(0).click();
        basePageSteps.wait500MS();
        basePageSteps.onOfferCardPage().fullscreenGallery().close().click();
        basePageSteps.onListingPage().header().back().click();

        basePageSteps.onListingPage().h1().should(hasText(categoryInMoscow(KOMPUTERI_TEXT)));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на категорию с фильтрами после перехода на карточку")
    public void shouldSeeBackToCategoryWithFiltersFromOfferCard() {
        urlSteps.queryParam(PRICE_MIN_URL_PARAM, "1000").queryParam(OFFER_STATE_PARAM, NEW_VALUE).open();
        basePageSteps.onListingPage().firstSnippet().click();
        basePageSteps.onOfferCardPage().priceBuyer().waitUntil(isDisplayed());
        basePageSteps.onListingPage().header().back().click();

        basePageSteps.onListingPage().h1().should(hasText(categoryInMoscow(KOMPUTERI_TEXT)));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на форму подачи с категории")
    public void shouldSeeGoToForm() {
        basePageSteps.onListingPage().tabBar().addOffer().click();

        basePageSteps.onListingPage().h1().should(hasText(FORM_PAGE_H1));
        urlSteps.testing().path(FORM).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на категорию с формы")
    public void shouldSeeBackToCategoryFromForm() {
        basePageSteps.onListingPage().tabBar().addOffer().click();
        basePageSteps.onListingPage().h1().waitUntil(hasText(FORM_PAGE_H1));
        basePageSteps.onFormPage().close().click();

        basePageSteps.onListingPage().h1().should(hasText(categoryInMoscow(KOMPUTERI_TEXT)));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на главную из фильтров категории, кнопки «Назад» нет")
    public void shouldSeeGoToHomepageFromFilter() {
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().filters().categorySelector().clearButton().click();
        basePageSteps.onListingPage().filters().showOffers().click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        basePageSteps.onListingPage().h1().should(hasText(categoryInMoscow(OFFERS_TEXT)));
        basePageSteps.onListingPage().header().back().should(not(isDisplayed()));
        urlSteps.testing().path(MOSKVA).queryParam(SORTING_PARAM, SORT_BY_RELEVANCE_VALUE)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на категорию из фильтров категории")
    public void shouldSeeGoToAnotherCategoryFromFilter() {
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().filters().categorySelector().click();
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().popup().spanLink(NOUTBUKI_TEXT).waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().filters().showOffers().click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        basePageSteps.onListingPage().h1().should(hasText(categoryInMoscow(NOUTBUKI_TEXT)));
        urlSteps.testing().path(MOSKVA).path(NOUTBUKI).queryParam(SORTING_PARAM, SORT_BY_RELEVANCE_VALUE)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение назад после перехода на категорию из фильтров категории")
    public void shouldSeeGoBackFromAnotherCategoryFromFilter() {
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().filters().categorySelector().click();
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().popup().spanLink(NOUTBUKI_TEXT).waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().filters().showOffers().click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));
        basePageSteps.onListingPage().header().back().click();

        basePageSteps.onListingPage().wrapper(PARAMETERS).should(isDisplayed());
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение назад на категорию после применения фильтров")
    public void shouldSeeGoBackAfterChangeFilters() {
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().filters().filterBlock(PRICE).inputWithFloatedPlaceholder(PRICE_FROM).sendKeys("1");
        basePageSteps.onListingPage().filters().checkboxWithLabel(NOVIY).click();
        basePageSteps.onListingPage().filters().showOffers().click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));
        basePageSteps.onListingPage().header().back().click();

        basePageSteps.onListingPage().h1().should(hasText(categoryInMoscow(KOMPUTERI_TEXT)));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на категорию из футера с листинга категории")
    public void shouldSeeGoToFooterCategory() {
        basePageSteps.scrollToBottom();
        basePageSteps.onListingPage().footer().category(ELEKTRONIKA_TEXT).waitUntil(isDisplayed()).click();

        basePageSteps.onListingPage().h1().should(hasText(categoryInMoscow(ELEKTRONIKA_TEXT)));
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на категорию после перехода на категорию из футера")
    public void shouldSeeBackToCategoryFromFooterCategory() {
        basePageSteps.scrollToBottom();
        basePageSteps.onListingPage().footer().category(ELEKTRONIKA_TEXT).waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().h1().waitUntil(hasText(categoryInMoscow(ELEKTRONIKA_TEXT)));
        basePageSteps.onListingPage().header().back().click();

        basePageSteps.onListingPage().h1().should(hasText(categoryInMoscow(KOMPUTERI_TEXT)));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на текстовый поиск с листинга категории")
    public void shouldSeeTextSearchFromCategoryListing() {
        basePageSteps.onListingPage().searchBar().openSearch().click();
        basePageSteps.onListingPage().wrapper().input().sendKeys(ELEKTRONIKA_TEXT);
        basePageSteps.onListingPage().wrapper().button(FIND).click();

        basePageSteps.onListingPage().h1().should(hasText(format("Объявления по запросу «%s» в Москве", ELEKTRONIKA_TEXT)));
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, ELEKTRONIKA_TEXT).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на категорию после текстового поиска")
    public void shouldSeeBackToCategoryFromTextSearch() {
        basePageSteps.onListingPage().searchBar().openSearch().click();
        basePageSteps.onListingPage().wrapper().input().sendKeys(ELEKTRONIKA_TEXT);
        basePageSteps.onListingPage().wrapper().button(FIND).click();
        basePageSteps.onListingPage().h1().waitUntil(hasText(format("Объявления по запросу «%s» в Москве", ELEKTRONIKA_TEXT)));
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().header().back().click();
        basePageSteps.wait500MS();

        basePageSteps.onListingPage().h1().should(hasText(categoryInMoscow(KOMPUTERI_TEXT)));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на главную через лого, кнопки «Назад» нет")
    public void shouldSeeGoToHomepageFromLogo() {
        basePageSteps.onListingPage().header().oLogo().click();

        basePageSteps.onListingPage().h1().should(hasText(categoryInMoscow(OFFERS_TEXT)));
        basePageSteps.onListingPage().header().back().should(not(isDisplayed()));
        urlSteps.testing().shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на главную через таббар, кнопки «Назад» нет")
    public void shouldSeeGoToHomepageFromTabbar() {
        basePageSteps.onListingPage().tabBar().mainPage().click();

        basePageSteps.onListingPage().h1().should(hasText(categoryInMoscow(OFFERS_TEXT)));
        basePageSteps.onListingPage().header().back().should(not(isDisplayed()));
        urlSteps.testing().path(MOSKVA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет кнопки «Назад» при открытии листинга категории по прямой ссылке")
    public void shouldNotSeeBackButton() {
        basePageSteps.onListingPage().header().back().should(not(isDisplayed()));
    }

    private String categoryInMoscow(String categoryName) {
        return format("%s в Москве", categoryName);
    }

}
