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
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.step.UrlSteps;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.general.consts.GeneralFeatures.HOMEPAGE_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.NAVIGATION_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.mobile.element.FiltersPopup.METRO;
import static ru.yandex.general.mobile.element.Wrapper.FIND;
import static ru.yandex.general.mobile.page.FormPage.FORM_PAGE_H1;
import static ru.yandex.general.mobile.page.ListingPage.DONE;
import static ru.yandex.general.mobile.page.ListingPage.PARAMETERS;
import static ru.yandex.general.mobile.page.ListingPage.SHOW_BUTTON;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(HOMEPAGE_FEATURE)
@Feature(NAVIGATION_FEATURE)
@DisplayName("Навигация с главной")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
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
        urlSteps.testing().path(MOSKVA).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на категорию из списка главных категорий с главной")
    public void shouldSeeGoToHomeMainCategory() {
        basePageSteps.onListingPage().homeCategory(ELEKTRONIKA_TEXT).waitUntil(isDisplayed()).click();

        basePageSteps.onListingPage().h1().should(hasText(ELEKTRONIKA_H1));
        urlSteps.path(ELEKTRONIKA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на главную после перехода на категорию из списка главных категорий")
    public void shouldSeeBackToHomePageFromHomeMainCategory() {
        basePageSteps.onListingPage().homeCategory(ELEKTRONIKA_TEXT).waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().h1().waitUntil(hasText(ELEKTRONIKA_H1));
        basePageSteps.onListingPage().header().back().click();

        basePageSteps.onListingPage().h1().should(hasText(HOMEPAGE_H1));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение назад на главную после выбора метро в фильтрах")
    public void shouldSeeGoBackAfterChoseMetroInFilters() {
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().screen().inputWithFloatedPlaceholder(METRO).click();
        basePageSteps.onListingPage().popup().menuItem("Пролетарская").click();
        basePageSteps.onListingPage().popup().button(SHOW_BUTTON).click();
        basePageSteps.onListingPage().filters().showOffers().click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));
        basePageSteps.onListingPage().header().back().click();

        basePageSteps.onListingPage().wrapper(PARAMETERS).should(isDisplayed());
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на категорию из футера с главной")
    public void shouldSeeGoToFooterCategory() {
        basePageSteps.scrollToBottom();
        basePageSteps.onListingPage().footer().category(ELEKTRONIKA_TEXT).waitUntil(isDisplayed()).click();

        basePageSteps.onListingPage().h1().should(hasText(ELEKTRONIKA_H1));
        urlSteps.path(ELEKTRONIKA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на главную после перехода на категорию из футера")
    public void shouldSeeBackToHomePageFromFooterCategory() {
        basePageSteps.scrollToBottom();
        basePageSteps.onListingPage().footer().category(ELEKTRONIKA_TEXT).waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().h1().waitUntil(hasText(ELEKTRONIKA_H1));
        basePageSteps.onListingPage().header().back().click();

        basePageSteps.onListingPage().h1().should(hasText(HOMEPAGE_H1));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на карточку с главной")
    public void shouldSeeGoToOfferCard() {
        String offerUrl = basePageSteps.onListingPage().firstSnippet().getUrl();
        basePageSteps.onListingPage().firstSnippet().click();

        basePageSteps.onOfferCardPage().priceBuyer().should(isDisplayed());
        urlSteps.fromUri(offerUrl).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на главную после перехода на карточку")
    public void shouldSeeBackToHomePageFromOfferCard() {
        basePageSteps.onListingPage().firstSnippet().click();
        basePageSteps.onOfferCardPage().priceBuyer().waitUntil(isDisplayed());
        basePageSteps.onOfferCardPage().header().back().click();

        basePageSteps.onListingPage().h1().should(hasText(HOMEPAGE_H1));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на главную после текстового поиска")
    public void shouldSeeBackToHomePageFromTextSearch() {
        basePageSteps.onListingPage().searchBar().openSearch().click();
        basePageSteps.onListingPage().wrapper().input().sendKeys(ELEKTRONIKA_TEXT);
        basePageSteps.onListingPage().wrapper().button(FIND).click();
        basePageSteps.onListingPage().h1().waitUntil(
                hasText(format("Объявления по запросу «%s» в Москве", ELEKTRONIKA_TEXT)));
        basePageSteps.onListingPage().header().back().click();

        basePageSteps.onListingPage().h1().should(hasText(HOMEPAGE_H1));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на форму подачи с главной")
    public void shouldSeeGoToForm() {
        basePageSteps.onListingPage().tabBar().addOffer().click();

        basePageSteps.onListingPage().h1().should(hasText(FORM_PAGE_H1));
        urlSteps.testing().path(FORM).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на главную с формы")
    public void shouldSeeBackToHomepageFromForm() {
        basePageSteps.onListingPage().tabBar().addOffer().click();
        basePageSteps.onListingPage().h1().waitUntil(hasText(FORM_PAGE_H1));
        basePageSteps.onFormPage().close().click();

        basePageSteps.onListingPage().h1().should(hasText(HOMEPAGE_H1));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на категорию из фильтров с главной")
    public void shouldSeeGoToCategoryFromFilter() {
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().filters().categorySelector().click();
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().popup().spanLink(ELEKTRONIKA_TEXT).waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().popup().button(DONE).click();
        basePageSteps.onListingPage().filters().showOffers().click();
        basePageSteps.onListingPage().skeleton().waitUntil(not(isDisplayed()));

        basePageSteps.onListingPage().h1().should(hasText(ELEKTRONIKA_H1));
        urlSteps.path(ELEKTRONIKA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на главную после перехода на категорию с фильтров")
    public void shouldSeeBackToHomepageFromCategoryFromFilter() {
        basePageSteps.onListingPage().searchBar().filters().click();
        basePageSteps.onListingPage().filters().categorySelector().click();
        basePageSteps.wait500MS();
        basePageSteps.onListingPage().popup().spanLink(ELEKTRONIKA_TEXT).waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().popup().button(DONE).click();
        basePageSteps.onListingPage().filters().showOffers().click();
        basePageSteps.onListingPage().h1().waitUntil(hasText(ELEKTRONIKA_H1));
        basePageSteps.onListingPage().header().back().click();

        basePageSteps.onListingPage().wrapper(PARAMETERS).should(isDisplayed());
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет кнопки «Назад» при открытии главной по прямой ссылке")
    public void shouldNotSeeBackButton() {
        basePageSteps.onListingPage().header().back().should(not(isDisplayed()));
    }

}
