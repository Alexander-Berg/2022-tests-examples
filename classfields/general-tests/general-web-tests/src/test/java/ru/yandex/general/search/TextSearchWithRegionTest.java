package ru.yandex.general.search;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.UrlSteps;

import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.general.consts.GeneralFeatures.SEARCH_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.KOMPUTERNAYA_TEHNIKA;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.NOUTBUKI;
import static ru.yandex.general.consts.Pages.NOVOSIBIRSK;
import static ru.yandex.general.consts.QueryParams.CATEGORY_VALUE;
import static ru.yandex.general.consts.QueryParams.LOCKED_FIELDS;
import static ru.yandex.general.consts.QueryParams.REGION_VALUE;
import static ru.yandex.general.consts.QueryParams.SORTING_PARAM;
import static ru.yandex.general.consts.QueryParams.SORT_BY_RELEVANCE_VALUE;
import static ru.yandex.general.consts.QueryParams.TEXT_PARAM;
import static ru.yandex.general.element.SearchBar.FIND;
import static ru.yandex.general.element.SidebarCategories.ALL_CATEGORIES;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_REGION_ID;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(SEARCH_FEATURE)
@Feature("Текстовый поиск с регионом")
@DisplayName("Тесты поиска по тексту")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class TextSearchWithRegionTest {

    private static final String TEXT = "ноутбук apple macbook в Москве";
    private static final String TEXT_ONLY_REGION = "Москва";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Текстовый поиск с регионом в регионе поиска, нет чипсины с регионом")
    public void shouldSeeNoRegionChips() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onListingPage().searchBar().input().sendKeys(TEXT);
        basePageSteps.onListingPage().searchBar().button(FIND).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.queryParam(TEXT_PARAM, TEXT).shouldNotDiffWithWebDriverUrl();
        basePageSteps.onListingPage().chips().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Текстовый поиск с регионом в другом регионе, есть чипсина с регионом")
    public void shouldSeeRegionChips() {
        urlSteps.testing().path(NOVOSIBIRSK).open();
        basePageSteps.onListingPage().searchBar().input().sendKeys(TEXT);
        basePageSteps.onListingPage().searchBar().button(FIND).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.queryParam(TEXT_PARAM, TEXT).shouldNotDiffWithWebDriverUrl();
        basePageSteps.onListingPage().chips().should(hasText("Регион поиска: Москва"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Текстовый поиск с регионом в другом регионе, регион в хэдере не меняется")
    public void shouldSeeRegionInHeaderNotChange() {
        urlSteps.testing().path(NOVOSIBIRSK).open();
        basePageSteps.onListingPage().searchBar().input().sendKeys(TEXT);
        basePageSteps.onListingPage().searchBar().button(FIND).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        basePageSteps.onListingPage().region().should(hasText("Новосибирск"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сбрасываем чипсину региона в текстовом поиске с регионом")
    public void shouldSeeRegionChipsReset() {
        urlSteps.testing().path(NOVOSIBIRSK).open();
        basePageSteps.onListingPage().searchBar().input().sendKeys(TEXT);
        basePageSteps.onListingPage().searchBar().button(FIND).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));
        basePageSteps.onListingPage().chips().reset().click();
        basePageSteps.wait500MS();

        urlSteps.queryParam(TEXT_PARAM, TEXT).queryParam(LOCKED_FIELDS, REGION_VALUE).shouldNotDiffWithWebDriverUrl();
        basePageSteps.onListingPage().chips().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не меняется кука «CLASSIFIED_REGION_ID» при текстовои поиске с регионом")
    public void shouldSeeNoChangeGeoCookie() {
        basePageSteps.setCookie(CLASSIFIED_REGION_ID, "65");
        urlSteps.testing().open();
        basePageSteps.onListingPage().searchBar().input().sendKeys(TEXT);
        basePageSteps.onListingPage().searchBar().button(FIND).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        basePageSteps.shouldSeeCookie(CLASSIFIED_REGION_ID, "65");
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сбрасываем текстовый поиск с регионом")
    public void shouldSeeTextSearchReset() {
        urlSteps.testing().path(NOVOSIBIRSK).open();
        basePageSteps.onListingPage().searchBar().input().sendKeys(TEXT);
        basePageSteps.onListingPage().searchBar().button(FIND).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        basePageSteps.onListingPage().searchBar().clearInput().click();
        basePageSteps.onListingPage().searchBar().button(FIND).click();

        basePageSteps.onListingPage().h1().should(hasText("Объявления в Новосибирске"));
        basePageSteps.onListingPage().chips().should(not(isDisplayed()));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Текстовый поиск с регионом в другом регионе, сбрасываем  уточняющую категорию")
    public void shouldResetCategoryRegionTextSearch() {
        urlSteps.testing().path(NOVOSIBIRSK).path(KOMPUTERNAYA_TEHNIKA).path(NOUTBUKI).queryParam(TEXT_PARAM, TEXT).open();
        basePageSteps.onListingPage().sidebarCategories().spanLink(ALL_CATEGORIES).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.testing().path(NOVOSIBIRSK).queryParam(TEXT_PARAM, TEXT)
                .queryParam(LOCKED_FIELDS, CATEGORY_VALUE)
                .queryParam(SORTING_PARAM, SORT_BY_RELEVANCE_VALUE).shouldNotDiffWithWebDriverUrl();
        basePageSteps.onListingPage().chips().should(hasText("Регион поиска: Москва"));
        basePageSteps.onListingPage().sidebarCategories().spanLink(ALL_CATEGORIES).should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Текстовый поиск только с городом - «Москва», есть чипсина с регионом")
    public void shouldSeeRegionChipsWithTextSearchOnlyRegion() {
        urlSteps.testing().path(NOVOSIBIRSK).open();
        basePageSteps.onListingPage().searchBar().input().sendKeys(TEXT_ONLY_REGION);
        basePageSteps.onListingPage().searchBar().button(FIND).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.queryParam(TEXT_PARAM, TEXT_ONLY_REGION).shouldNotDiffWithWebDriverUrl();
        basePageSteps.onListingPage().chips().should(hasText("Регион поиска: Москва"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сбрасываем чипсину региона в текстовом поиске состоящем только из региона")
    public void shouldSeeRegionChipsResetTextSearchOnlyRegion() {
        urlSteps.testing().path(NOVOSIBIRSK).open();
        basePageSteps.onListingPage().searchBar().input().sendKeys(TEXT_ONLY_REGION);
        basePageSteps.onListingPage().searchBar().button(FIND).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));
        basePageSteps.onListingPage().chips().reset().click();
        basePageSteps.wait500MS();

        urlSteps.queryParam(TEXT_PARAM, TEXT_ONLY_REGION).queryParam(LOCKED_FIELDS, REGION_VALUE).shouldNotDiffWithWebDriverUrl();
        basePageSteps.onListingPage().chips().should(not(isDisplayed()));
    }

}