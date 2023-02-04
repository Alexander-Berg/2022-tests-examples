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
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.step.UrlSteps;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.general.consts.GeneralFeatures.SEARCH_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.NOUTBUKI;
import static ru.yandex.general.consts.QueryParams.CATEGORY_VALUE;
import static ru.yandex.general.consts.QueryParams.LOCKED_FIELDS;
import static ru.yandex.general.consts.QueryParams.MOSCOW_ID_VALUE;
import static ru.yandex.general.consts.QueryParams.REGION_ID_PARAM;
import static ru.yandex.general.consts.QueryParams.SORTING_PARAM;
import static ru.yandex.general.consts.QueryParams.SORT_BY_RELEVANCE_VALUE;
import static ru.yandex.general.consts.QueryParams.TEXT_PARAM;
import static ru.yandex.general.mobile.element.Link.HREF;
import static ru.yandex.general.mobile.element.Wrapper.FIND;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.common.HasAttributeMatcher.hasAttribute;

@Epic(SEARCH_FEATURE)
@Feature("Текстовый поиск")
@DisplayName("Тесты поиска по тексту")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class TextSearchTest {

    private static final String TEXT = "ноутбук apple macbook";
    private static final String ELEKTRONIKA_NAME = "Электроника";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Поле text тянется в URL")
    public void shouldSeeTextInUrl() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onListingPage().searchBar().openSearch().click();
        basePageSteps.onListingPage().wrapper().input().sendKeys(TEXT);
        basePageSteps.onListingPage().wrapper().button(FIND).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.queryParam(TEXT_PARAM, TEXT).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Поле text тянется в URL, при выборе запроса из саджеста без категории")
    public void shouldSeeTextInUrlFromSuggest() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onListingPage().searchBar().openSearch().click();
        basePageSteps.onListingPage().wrapper().input().sendKeys(TEXT);
        basePageSteps.onListingPage().wrapper().suggestItemWithoutCategory(TEXT).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.queryParam(TEXT_PARAM, TEXT).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Поле text тянется в URL, при выборе запроса из саджеста с категорией")
    public void shouldSeeTextInUrlFromSuggestWithCategory() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onListingPage().searchBar().openSearch().click();
        basePageSteps.onListingPage().wrapper().input().sendKeys(TEXT);
        basePageSteps.onListingPage().wrapper().suggestItemWithCategory(TEXT).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.path(NOUTBUKI).queryParam(TEXT_PARAM, TEXT).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Текстовый запрос в H1")
    public void shouldSeeH1WithText() {
        urlSteps.testing().queryParam(TEXT_PARAM, TEXT).queryParam(REGION_ID_PARAM, MOSCOW_ID_VALUE).open();

        basePageSteps.onListingPage().h1().should(hasText(format("Объявления по запросу «%s» в Москве", TEXT)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Поле text не тянется в URL категорий в футере")
    public void shouldNotSeeTextInCategoryFooter() {
        urlSteps.testing().queryParam(TEXT_PARAM, TEXT).queryParam(REGION_ID_PARAM, MOSCOW_ID_VALUE).open();

        basePageSteps.onListingPage().footer().category(ELEKTRONIKA_NAME).should(
                hasAttribute(HREF,
                        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).toString()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сброс текстового поиска")
    public void shouldSeeResetTextSearch() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onListingPage().searchBar().openSearch().click();
        basePageSteps.onListingPage().wrapper().input().sendKeys(TEXT);
        basePageSteps.onListingPage().wrapper().button(FIND).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        basePageSteps.onListingPage().searchBar().openSearch().click();
        basePageSteps.onListingPage().wrapper().searchClearButton().click();
        basePageSteps.onListingPage().wrapper().button(FIND).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        basePageSteps.onListingPage().h1().should(hasText("Объявления в Москве"));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Чипсина категории при текстовом поиске")
    public void shouldSeeCategoryChipWithTextSearch() {
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, TEXT).open();

        basePageSteps.onListingPage().categoryInChips().should(hasText("Ноутбуки"));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сбрасываем чипсину категории при текстовом поиске - чипсина перестает отображаться")
    public void shouldSeeCategoryChipWithTextSearchReset() {
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, TEXT).open();
        basePageSteps.onListingPage().categoryInChips().arrowLeft().click();

        basePageSteps.onListingPage().categoryInChips().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сбрасываем чипсину категории при текстовом поиске - проверям URL")
    public void shouldSeeUrlAfterCategoryChipWithTextSearchReset() {
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, TEXT).open();
        basePageSteps.onListingPage().categoryInChips().arrowLeft().click();

        urlSteps.queryParam(SORTING_PARAM, SORT_BY_RELEVANCE_VALUE).queryParam(LOCKED_FIELDS, CATEGORY_VALUE)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сбрасываем категорию в попапе фильтров - чипсина категории перестает отображаться")
    public void shouldSeeNoCategoryChipAfterCategoryInPopupReset() {
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, TEXT).open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().filters().categorySelector().clearButton().click();
        basePageSteps.onListingPage().filters().showOffers().click();

        basePageSteps.onListingPage().categoryInChips().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сбрасываем категорию в попапе фильтров - проверяем URL")
    public void shouldSeeUrlAfterCategoryInPopupReset() {
        urlSteps.testing().path(MOSKVA).queryParam(TEXT_PARAM, TEXT).open();
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().filters().categorySelector().clearButton().click();
        basePageSteps.onListingPage().filters().showOffers().click();

        urlSteps.queryParam(SORTING_PARAM, SORT_BY_RELEVANCE_VALUE).queryParam(LOCKED_FIELDS, CATEGORY_VALUE)
                .shouldNotDiffWithWebDriverUrl();
    }

}
