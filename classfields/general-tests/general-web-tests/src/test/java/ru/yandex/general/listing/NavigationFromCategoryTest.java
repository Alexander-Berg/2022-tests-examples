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
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.general.consts.GeneralFeatures.LISTING_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.NAVIGATION_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.KOMPUTERI;
import static ru.yandex.general.consts.Pages.KOMPUTERNAYA_TEHNIKA;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.NOUTBUKI;
import static ru.yandex.general.consts.Pages.ROSSIYA;
import static ru.yandex.general.consts.Pages.SANKT_PETERBURG;
import static ru.yandex.general.consts.QueryParams.DISTRICT_ID_PARAM;
import static ru.yandex.general.consts.QueryParams.GEO_RADIUS_PARAM;
import static ru.yandex.general.consts.QueryParams.LATITUDE_PARAM;
import static ru.yandex.general.consts.QueryParams.LONGITUDE_PARAM;
import static ru.yandex.general.consts.QueryParams.METRO_ID_PARAM;
import static ru.yandex.general.consts.QueryParams.REGION_PARAM;
import static ru.yandex.general.consts.QueryParams.SANKT_PETERBURG_ID_VALUE;
import static ru.yandex.general.element.SearchBar.MAP_METRO_DISTRICTS;
import static ru.yandex.general.element.SearchBar.SHOW;
import static ru.yandex.general.element.SuggestDropdown.DISTRICT;
import static ru.yandex.general.element.SuggestDropdown.METRO;
import static ru.yandex.general.page.BasePage.PXLS_TO_FLOAT_HEADER;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_REGION_ID;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(LISTING_FEATURE)
@Feature(NAVIGATION_FEATURE)
@DisplayName("Навигация с листинга категории")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
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
    @DisplayName("Переход на дочернюю категорию в сайдбаре с листинга категории")
    public void shouldSeeGoToChildCategory() {
        basePageSteps.onListingPage().sidebarCategories().link(NOUTBUKI_TEXT).click();

        basePageSteps.onListingPage().h1().should(hasText(categoryInMoscow(NOUTBUKI_TEXT)));
        urlSteps.testing().path(MOSKVA).path(KOMPUTERNAYA_TEHNIKA).path(NOUTBUKI).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на листинг категории с дочерней категории")
    public void shouldSeeBackToCategoryFromChildCategory() {
        basePageSteps.onListingPage().sidebarCategories().link(NOUTBUKI_TEXT).click();
        basePageSteps.onListingPage().h1().waitUntil(hasText(categoryInMoscow(NOUTBUKI_TEXT)));
        basePageSteps.back();
        basePageSteps.wait500MS();

        basePageSteps.onListingPage().h1().should(hasText(categoryInMoscow(KOMPUTERI_TEXT)));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на главную в сайдбаре с листинга категории")
    public void shouldSeeGoToHomepageFromSidebar() {
        basePageSteps.onListingPage().sidebarCategories().link("Все объявления").click();

        basePageSteps.onListingPage().h1().should(hasText(categoryInMoscow(OFFERS_TEXT)));
        urlSteps.testing().path(MOSKVA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на родительскую категорию в сайдбаре с листинга категории")
    public void shouldSeeGoToParentSidebarCategory() {
        basePageSteps.onListingPage().sidebarCategories().link(KOMPUTERNAYA_TEHNIKA_TEXT).click();

        basePageSteps.onListingPage().h1().should(hasText(categoryInMoscow(KOMPUTERNAYA_TEHNIKA_TEXT)));
        urlSteps.testing().path(MOSKVA).path(KOMPUTERNAYA_TEHNIKA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на листинг категории с главной")
    public void shouldSeeBackToCategoryFromHomepage() {
        basePageSteps.onListingPage().sidebarCategories().link("Все объявления").click();

        basePageSteps.onListingPage().h1().should(hasText(categoryInMoscow(OFFERS_TEXT)));
        urlSteps.testing().path(MOSKVA).shouldNotDiffWithWebDriverUrl();
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
    @DisplayName("Переход на «Все объявления» в ХК с листинга категории")
    public void shouldSeeGoToBreadcrumbAllOffers() {
        basePageSteps.onListingPage().breadcrumbsItem("Все объявления").click();

        basePageSteps.onListingPage().h1().should(hasText("Объявления в России"));
        urlSteps.testing().path(ROSSIYA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на «Москва» в ХК с листинга категории")
    public void shouldSeeGoToBreadcrumbCity() {
        basePageSteps.onListingPage().breadcrumbsItem("Москва").click();

        basePageSteps.onListingPage().h1().should(hasText(categoryInMoscow(OFFERS_TEXT)));
        urlSteps.testing().path(MOSKVA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на листинг категории после перехода в категорию из ХК")
    public void shouldSeeBackToCategoryFromBreadcrumbCategory() {
        basePageSteps.onListingPage().breadcrumbsItem(KOMPUTERNAYA_TEHNIKA_TEXT).click();
        basePageSteps.onListingPage().h1().waitUntil(hasText(categoryInMoscow(KOMPUTERNAYA_TEHNIKA_TEXT)));
        basePageSteps.back();
        basePageSteps.wait500MS();

        basePageSteps.onListingPage().h1().should(hasText(categoryInMoscow(KOMPUTERI_TEXT)));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на карточку с категории")
    public void shouldSeeGoToOfferCard() {
        String offerUrl = basePageSteps.onListingPage().firstSnippet().getUrl();
        basePageSteps.onListingPage().firstSnippet().click();
        basePageSteps.switchToNextTab();

        basePageSteps.onOfferCardPage().sidebar().price().should(isDisplayed());
        urlSteps.fromUri(offerUrl).queryParam(REGION_PARAM, "213").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на город из футера с листинга категории")
    public void shouldSeeGoToFooterCity() {
        basePageSteps.scrollToBottom();
        basePageSteps.onListingPage().footer().category("Авто").waitUntil(isDisplayed()).hover();
        basePageSteps.onListingPage().footer().city("Санкт-Петербург").click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);

        basePageSteps.onListingPage().h1().should(hasText("Компьютеры в Санкт-Петербурге"));
        urlSteps.testing().path(SANKT_PETERBURG).path(KOMPUTERNAYA_TEHNIKA).path(KOMPUTERI)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на категорию после перехода на категорию из футера")
    public void shouldSeeBackToCategoryFromFooterCategory() {
        basePageSteps.scrollToBottom();
        basePageSteps.onListingPage().footer().category(ELEKTRONIKA_TEXT).waitUntil(isDisplayed()).hover().click();
        basePageSteps.onListingPage().h1().waitUntil(hasText(categoryInMoscow(ELEKTRONIKA_TEXT)));
        basePageSteps.back();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);

        basePageSteps.onListingPage().h1().should(hasText(categoryInMoscow(KOMPUTERI_TEXT)));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем фильтр по метро с листинга категории")
    public void shouldSeeSubwaySearch() {
        basePageSteps.onListingPage().searchBar().button(MAP_METRO_DISTRICTS).click();
        basePageSteps.onListingPage().searchBar().suggest().button(METRO).click();
        basePageSteps.onListingPage().searchBar().suggest().station("Павелецкая").click();
        basePageSteps.onListingPage().searchBar().button(SHOW).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.testing().path(MOSKVA).path(KOMPUTERI).queryParam(METRO_ID_PARAM, "20475").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем фильтр по району с листинга категории")
    public void shouldSeeDistrictSearch() {
        basePageSteps.onListingPage().searchBar().button(MAP_METRO_DISTRICTS).click();
        basePageSteps.onListingPage().searchBar().suggest().button(DISTRICT).click();
        basePageSteps.onListingPage().searchBar().suggest().checkboxWithLabel("Силино").click();
        basePageSteps.onListingPage().searchBar().button(SHOW).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.testing().path(MOSKVA).path(KOMPUTERI).queryParam(DISTRICT_ID_PARAM, "116978").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем фильтр по адресу с листинга категории")
    public void shouldSeeAddressSearch() {
        basePageSteps.onListingPage().searchBar().button(MAP_METRO_DISTRICTS).click();
        basePageSteps.onListingPage().searchBar().fillSearchInput("Ленинградский проспект, 80к17");
        basePageSteps.onListingPage().searchBar().suggestItem("Ленинградский проспект, 80к17").click();
        basePageSteps.onListingPage().searchBar().button(SHOW).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.testing().path(MOSKVA).path(KOMPUTERI).queryParam(LATITUDE_PARAM, "55.807953")
                .queryParam(LONGITUDE_PARAM, "37.511509")
                .queryParam(GEO_RADIUS_PARAM, "1000").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем фильтр по метро с листинга категории через прилипший хэдер")
    public void shouldSeeSubwaySearchFloatedHeader() {
        basePageSteps.onListingPage().searchBar().button(MAP_METRO_DISTRICTS).click();
        basePageSteps.onListingPage().searchBar().suggest().button(METRO).click();
        basePageSteps.onListingPage().searchBar().suggest().station("Павелецкая").click();
        basePageSteps.onListingPage().searchBar().button(SHOW).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.testing().path(MOSKVA).path(KOMPUTERI).queryParam(METRO_ID_PARAM, "20475").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем фильтр по району с листинга категории через прилипший хэдер")
    public void shouldSeeDistrictSearchFloatedHeader() {
        basePageSteps.onListingPage().searchBar().button(MAP_METRO_DISTRICTS).click();
        basePageSteps.onListingPage().searchBar().suggest().button(DISTRICT).click();
        basePageSteps.onListingPage().searchBar().suggest().checkboxWithLabel("Силино").click();
        basePageSteps.onListingPage().searchBar().button(SHOW).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.testing().path(MOSKVA).path(KOMPUTERI).queryParam(DISTRICT_ID_PARAM, "116978").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем фильтр по адресу с листинга категории через прилипший хэдер")
    public void shouldSeeAddressSearchFloatedHeader() {
        basePageSteps.onListingPage().searchBar().button(MAP_METRO_DISTRICTS).click();
        basePageSteps.onListingPage().searchBar().fillSearchInput("Ленинградский проспект, 80к17");
        basePageSteps.onListingPage().searchBar().suggestItem("Ленинградский проспект, 80к17").click();
        basePageSteps.onListingPage().searchBar().button(SHOW).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.testing().path(MOSKVA).path(KOMPUTERI).queryParam(LATITUDE_PARAM, "55.807953")
                .queryParam(LONGITUDE_PARAM, "37.511509")
                .queryParam(GEO_RADIUS_PARAM, "1000").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на категорию после текстового поиска")
    public void shouldSeeBackToCategoryFromTextSearch() {
        basePageSteps.onListingPage().searchBar().input().sendKeys(ELEKTRONIKA_TEXT);
        basePageSteps.onListingPage().searchBar().suggestItemWithCategory(ELEKTRONIKA_TEXT).click();
        basePageSteps.onListingPage().h1().waitUntil(hasText(format("Объявления по запросу «%s» в Москве", ELEKTRONIKA_TEXT.toLowerCase())));
        basePageSteps.back();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);

        basePageSteps.onListingPage().h1().should(hasText(categoryInMoscow(KOMPUTERI_TEXT)));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на главную через лого")
    public void shouldSeeGoToHomepageFromLogo() {
        basePageSteps.onListingPage().oLogo().click();

        basePageSteps.onListingPage().h1().should(hasText(categoryInMoscow(OFFERS_TEXT)));
        urlSteps.testing().shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на главную через лого из прилишего хэдера")
    public void shouldSeeGoToHomepageFromLogoFloatedHeader() {
        basePageSteps.scrollDown(PXLS_TO_FLOAT_HEADER);
        basePageSteps.onListingPage().floatedHeader().oLogo().click();

        basePageSteps.onProfilePage().h1().should(hasText("Объявления в Москве"));
        urlSteps.testing().shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Смена региона с листинга категории")
    public void shouldSeeChangeRegion() {
        basePageSteps.onListingPage().region().click();
        basePageSteps.onListingPage().searchBar().suggest().spanLink("Санкт-Петербург").click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);

        basePageSteps.onListingPage().h1().should(hasText("Компьютеры в Санкт-Петербурге"));
        basePageSteps.shouldSeeCookie(CLASSIFIED_REGION_ID, SANKT_PETERBURG_ID_VALUE);
        urlSteps.testing().path(SANKT_PETERBURG).path(KOMPUTERNAYA_TEHNIKA).path(KOMPUTERI).shouldNotDiffWithWebDriverUrl();
    }

    private String categoryInMoscow(String categoryName) {
        return format("%s в Москве", categoryName);
    }

}
