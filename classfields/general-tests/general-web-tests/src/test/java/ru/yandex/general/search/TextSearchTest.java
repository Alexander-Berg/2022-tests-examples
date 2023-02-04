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
import org.openqa.selenium.Keys;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.UrlSteps;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.general.consts.GeneralFeatures.SEARCH_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.NOUTBUKI;
import static ru.yandex.general.consts.QueryParams.MOSCOW_ID_VALUE;
import static ru.yandex.general.consts.QueryParams.REGION_ID_PARAM;
import static ru.yandex.general.consts.QueryParams.TEXT_PARAM;
import static ru.yandex.general.element.Link.HREF;
import static ru.yandex.general.element.SearchBar.FIND;
import static ru.yandex.general.page.BasePage.PXLS_TO_FLOAT_HEADER;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.common.HasAttributeMatcher.hasAttribute;

@Epic(SEARCH_FEATURE)
@Feature("Текстовый поиск")
@DisplayName("Тесты поиска по тексту")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
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
        basePageSteps.onListingPage().searchBar().input().sendKeys(TEXT);
        basePageSteps.onListingPage().searchBar().button(FIND).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.queryParam(TEXT_PARAM, TEXT).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Запрос из саджеста без категории, text тянется в URL")
    public void shouldSeeTextInUrlFromSuggest() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onListingPage().searchBar().input().sendKeys(TEXT);
        basePageSteps.onListingPage().searchBar().suggestItemWithoutCategory(TEXT).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.queryParam(TEXT_PARAM, TEXT).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Запрос из саджеста с категорией, text тянется в URL")
    public void shouldSeeTextInUrlFromSuggestWithCategory() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onListingPage().searchBar().input().sendKeys(TEXT);
        basePageSteps.onListingPage().searchBar().suggestItemWithCategory(TEXT).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.path(NOUTBUKI).queryParam(TEXT_PARAM, TEXT).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Текстовый поиск по Enter, text тянется в URL")
    public void shouldSeeTextInUrlFromEnterClick() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onListingPage().searchBar().input().sendKeys(TEXT);
        basePageSteps.onListingPage().searchBar().input().sendKeys(Keys.RETURN);
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.queryParam(TEXT_PARAM, TEXT).shouldNotDiffWithWebDriverUrl();
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
                hasAttribute(HREF, urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).toString()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Поиск из прилипшего хэдера")
    public void shouldSeeSearchFromFloatedHeader() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.scrollDown(PXLS_TO_FLOAT_HEADER);
        basePageSteps.onListingPage().floatedHeader().searchBar().input().sendKeys(TEXT);
        basePageSteps.onListingPage().floatedHeader().searchBar().button(FIND).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.queryParam(TEXT_PARAM, TEXT).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Запрос из саджеста прилипшего хэдера, text тянется в URL")
    public void shouldSeeTextInUrlFromSuggestFloatedHeader() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.scrollDown(PXLS_TO_FLOAT_HEADER);
        basePageSteps.onListingPage().floatedHeader().searchBar().input().sendKeys(TEXT);
        basePageSteps.onListingPage().floatedHeader().searchBar().suggestItemWithCategory(TEXT).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.path(NOUTBUKI).queryParam(TEXT_PARAM, TEXT).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Запрос из саджеста прилипшего хэдера с категорией, text тянется в URL")
    public void shouldSeeTextInUrlFromSuggestWithCategoryFloatedHeader() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.scrollDown(PXLS_TO_FLOAT_HEADER);
        basePageSteps.onListingPage().floatedHeader().searchBar().input().sendKeys(TEXT);
        basePageSteps.onListingPage().floatedHeader().searchBar().suggestItemWithCategory(TEXT).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        urlSteps.path(NOUTBUKI).queryParam(TEXT_PARAM, TEXT).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сброс текстового поиска")
    public void shouldSeeResetTextSearch() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onListingPage().searchBar().input().sendKeys(TEXT);
        basePageSteps.onListingPage().searchBar().button(FIND).click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        basePageSteps.onListingPage().searchBar().clearInput().click();
        basePageSteps.onListingPage().searchBar().button(FIND).click();

        basePageSteps.onListingPage().h1().should(hasText("Объявления в Москве"));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

}
