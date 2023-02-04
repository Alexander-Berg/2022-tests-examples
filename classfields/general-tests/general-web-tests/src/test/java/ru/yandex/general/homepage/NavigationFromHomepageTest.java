package ru.yandex.general.homepage;

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
import static ru.yandex.general.consts.GeneralFeatures.HOMEPAGE_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.NAVIGATION_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.SANKT_PETERBURG;
import static ru.yandex.general.consts.QueryParams.DISTRICT_ID_PARAM;
import static ru.yandex.general.consts.QueryParams.GEO_RADIUS_PARAM;
import static ru.yandex.general.consts.QueryParams.LATITUDE_PARAM;
import static ru.yandex.general.consts.QueryParams.LONGITUDE_PARAM;
import static ru.yandex.general.consts.QueryParams.METRO_ID_PARAM;
import static ru.yandex.general.element.SearchBar.MAP_METRO_DISTRICTS;
import static ru.yandex.general.element.SearchBar.SHOW;
import static ru.yandex.general.element.SuggestDropdown.DISTRICT;
import static ru.yandex.general.element.SuggestDropdown.METRO;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(HOMEPAGE_FEATURE)
@Feature(NAVIGATION_FEATURE)
@DisplayName("Навигация с главной")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class NavigationFromHomepageTest {

    private static final String ELEKTRONIKA_TEXT = "Электроника";
    private static final String ELEKTRONIKA_H1 = "Электроника в Москве";
    private static final String HOMEPAGE_H1 = "Объявления в Москве";

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
        basePageSteps.resize(1920, 1080);
        urlSteps.testing().path(MOSKVA).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на категорию из списка главных категорий с главной")
    public void shouldSeeGoToHomeMainCategory() {
        basePageSteps.onListingPage().homeMainCategories().link(ELEKTRONIKA_TEXT).click();

        basePageSteps.onListingPage().h1().should(hasText(ELEKTRONIKA_H1));
        urlSteps.path(ELEKTRONIKA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на главную после перехода на категорию из списка главных категорий")
    public void shouldSeeBackToHomePageFromHomeMainCategory() {
        basePageSteps.onListingPage().homeMainCategories().link(ELEKTRONIKA_TEXT).click();
        basePageSteps.onListingPage().h1().waitUntil(hasText(ELEKTRONIKA_H1));
        basePageSteps.back();
        basePageSteps.wait500MS();

        basePageSteps.onListingPage().h1().should(hasText(HOMEPAGE_H1));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на категорию в сайдбаре с главной")
    public void shouldSeeGoToSidebarCategory() {
        basePageSteps.onListingPage().sidebarCategories().link(ELEKTRONIKA_TEXT).click();

        basePageSteps.onListingPage().h1().should(hasText(ELEKTRONIKA_H1));
        urlSteps.path(ELEKTRONIKA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на главную после перехода на категорию из сайдбара")
    public void shouldSeeBackToHomePageFromSidebarCategory() {
        basePageSteps.onListingPage().sidebarCategories().link(ELEKTRONIKA_TEXT).click();
        basePageSteps.onListingPage().h1().waitUntil(hasText(ELEKTRONIKA_H1));
        basePageSteps.back();
        basePageSteps.wait500MS();

        basePageSteps.onListingPage().h1().should(hasText(HOMEPAGE_H1));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на категорию из футера с главной")
    public void shouldSeeGoToFooterCategory() {
        basePageSteps.scrollToBottom();
        basePageSteps.onListingPage().footer().category(ELEKTRONIKA_TEXT).waitUntil(isDisplayed()).hover().click();

        basePageSteps.onListingPage().h1().should(hasText(ELEKTRONIKA_H1));
        urlSteps.path(ELEKTRONIKA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на город из футера с главной")
    public void shouldSeeGoToFooterCity() {
        basePageSteps.scrollToBottom();
        basePageSteps.onListingPage().footer().city("Санкт-Петербург").waitUntil(isDisplayed()).hover().click();

        basePageSteps.onListingPage().h1().should(hasText("Объявления в Санкт-Петербурге"));
        urlSteps.testing().path(SANKT_PETERBURG).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на главную после перехода на категорию из футера")
    public void shouldSeeBackToHomePageFromFooterCategory() {
        basePageSteps.scrollToBottom();
        basePageSteps.onListingPage().footer().category(ELEKTRONIKA_TEXT).waitUntil(isDisplayed()).hover().click();
        basePageSteps.onListingPage().h1().waitUntil(hasText(ELEKTRONIKA_H1));
        basePageSteps.back();
        basePageSteps.wait500MS();

        basePageSteps.onListingPage().h1().should(hasText(HOMEPAGE_H1));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на главную после текстового поиска")
    public void shouldSeeBackToHomePageFromTextSearch() {
        basePageSteps.onListingPage().searchBar().input().sendKeys(ELEKTRONIKA_TEXT);
        basePageSteps.onListingPage().searchBar().suggestItemWithCategory(ELEKTRONIKA_TEXT).click();
        basePageSteps.onListingPage().h1().waitUntil(hasText(format("Объявления по запросу «%s» в Москве", ELEKTRONIKA_TEXT.toLowerCase())));
        basePageSteps.back();
        basePageSteps.wait500MS();

        basePageSteps.onListingPage().h1().should(hasText(HOMEPAGE_H1));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем фильтр по метро с главной")
    public void shouldSeeSubwaySearch() {
        basePageSteps.onListingPage().searchBar().button(MAP_METRO_DISTRICTS).click();
        basePageSteps.onListingPage().searchBar().suggest().button(METRO).click();
        basePageSteps.onListingPage().searchBar().suggest().station("Павелецкая").click();
        basePageSteps.onListingPage().searchBar().button(SHOW).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.queryParam(METRO_ID_PARAM, "20475").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем фильтр по району с главной")
    public void shouldSeeDistrictSearch() {
        basePageSteps.onListingPage().searchBar().button(MAP_METRO_DISTRICTS).click();
        basePageSteps.onListingPage().searchBar().suggest().button(DISTRICT).click();
        basePageSteps.onListingPage().searchBar().suggest().checkboxWithLabel("Силино").click();
        basePageSteps.onListingPage().searchBar().button(SHOW).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.queryParam(DISTRICT_ID_PARAM, "116978").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем фильтр по адресу с главной")
    public void shouldSeeAddressSearch() {
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
    @DisplayName("Смена региона с главной")
    public void shouldSeeChangeRegion() {
        basePageSteps.onListingPage().region().click();
        basePageSteps.onListingPage().searchBar().suggest().spanLink("Санкт-Петербург").click();

        basePageSteps.onListingPage().h1().should(hasText("Объявления в Санкт-Петербурге"));
        urlSteps.testing().path(SANKT_PETERBURG).shouldNotDiffWithWebDriverUrl();
    }

}