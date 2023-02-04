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
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.consts.GeneralFeatures.HOMEPAGE_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.NAVIGATION_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CONTACTS;
import static ru.yandex.general.consts.Pages.FAVORITES;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.OFFERS;
import static ru.yandex.general.consts.Pages.STATS;
import static ru.yandex.general.mobile.page.FormPage.FORM_PAGE_H1;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_EXPIRED_DIALOG_WAS_SHOW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(HOMEPAGE_FEATURE)
@Feature(NAVIGATION_FEATURE)
@DisplayName("Навигация с главной в ЛК")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class NavigationFromHomepageToLkTest {

    private static final String HOMEPAGE_H1 = "Объявления в Москве";
    private static final String FAVORITES_TITLE = "Избранное";
    private static final String MY_OFFERS_TITLE = "Мои объявления";
    private static final String STATISTICS_TITLE = "Статистика";
    private static final String PROFILE_SETTINGS_TITLE = "Настройки профиля";
    private static final String CREATE_OFFER = "Разместить объявление";

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
        basePageSteps.setMoscowCookie();
        basePageSteps.setCookie(CLASSIFIED_EXPIRED_DIALOG_WAS_SHOW, "1");
        passportSteps.accountWithOffersLogin();
        urlSteps.testing().path(MOSKVA).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход с главной в «Мои объявления» по клику в таббаре")
    public void shouldSeeHomePageToMyOffersFromTabbar() {
        basePageSteps.onListingPage().tabBar().myOffers().click();

        basePageSteps.onMyOffersPage().lkPageTitle().should(hasText(MY_OFFERS_TITLE));
        urlSteps.testing().path(MY).path(OFFERS).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на главную с «Мои объявления» после перехода по таббару")
    public void shouldSeeBackToHomePageFromMyOffersFromTabbar() {
        basePageSteps.onListingPage().tabBar().myOffers().click();
        basePageSteps.onMyOffersPage().lkPageTitle().waitUntil(hasText(MY_OFFERS_TITLE));
        basePageSteps.onMyOffersPage().header().back().click();

        basePageSteps.onListingPage().h1().should(hasText(HOMEPAGE_H1));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход с главной в «Избранное» по клику в таббаре")
    public void shouldSeeHomePageToFavoritesFromTabbar() {
        basePageSteps.onListingPage().tabBar().favorites().click();

        basePageSteps.onFavoritesPage().lkPageTitle().should(hasText(FAVORITES_TITLE));
        urlSteps.testing().path(MY).path(FAVORITES).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на главную с «Избранное» после перехода по таббару")
    public void shouldSeeBackToHomePageFromFavoritesFromTabbar() {
        basePageSteps.onListingPage().tabBar().favorites().click();
        basePageSteps.onFavoritesPage().lkPageTitle().waitUntil(hasText(FAVORITES_TITLE));
        basePageSteps.onFavoritesPage().header().back().click();

        basePageSteps.onListingPage().h1().should(hasText(HOMEPAGE_H1));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход с главной в «Мои объявления» по клику в юзер-попапе")
    public void shouldSeeHomePageToMyOffersFromUserPopup() {
        basePageSteps.onListingPage().header().burger().click();
        basePageSteps.onMyOffersPage().popup().link(MY_OFFERS_TITLE).waitUntil(isDisplayed()).click();

        basePageSteps.onMyOffersPage().lkPageTitle().should(hasText("Мои объявления"));
        urlSteps.testing().path(MY).path(OFFERS).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на главную с «Мои объявления» после перехода с юзер-попапа")
    public void shouldSeeBackToHomePageFromFromOffersByUserPopup() {
        basePageSteps.onListingPage().header().burger().click();
        basePageSteps.onListingPage().popup().link(MY_OFFERS_TITLE).waitUntil(isDisplayed()).click();
        basePageSteps.onMyOffersPage().lkPageTitle().waitUntil(hasText(MY_OFFERS_TITLE));
        basePageSteps.onMyOffersPage().header().back().click();

        basePageSteps.onListingPage().h1().should(hasText(HOMEPAGE_H1));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход с главной в «Статистика» по клику в юзер-попапе")
    public void shouldSeeHomePageToStatisticsFromUserPopup() {
        basePageSteps.onListingPage().header().burger().click();
        basePageSteps.onListingPage().popup().link(STATISTICS_TITLE).waitUntil(isDisplayed()).click();

        basePageSteps.onStatisticsPage().lkPageTitle().should(hasText(STATISTICS_TITLE));
        urlSteps.testing().path(MY).path(STATS).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на главную с «Статистика» после перехода с юзер-попапа")
    public void shouldSeeBackToHomePageFromStatisticsFromUserPopup() {
        basePageSteps.onListingPage().header().burger().click();
        basePageSteps.onListingPage().popup().link(STATISTICS_TITLE).waitUntil(isDisplayed()).click();
        basePageSteps.onStatisticsPage().lkPageTitle().waitUntil(hasText(STATISTICS_TITLE));
        basePageSteps.onStatisticsPage().header().back().click();

        basePageSteps.onListingPage().h1().should(hasText(HOMEPAGE_H1));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход с главной в «Избранное» по клику в юзер-попапе")
    public void shouldSeeHomePageToFavoritesFromUserPopup() {
        basePageSteps.onListingPage().header().burger().click();
        basePageSteps.onListingPage().popup().link(FAVORITES_TITLE).waitUntil(isDisplayed()).click();

        basePageSteps.onFavoritesPage().lkPageTitle().should(hasText(FAVORITES_TITLE));
        urlSteps.testing().path(MY).path(FAVORITES).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на главную с «Избранное» после перехода с юзер-попапа")
    public void shouldSeeBackToHomePageFromFavoritesFromUserPopup() {
        basePageSteps.onListingPage().header().burger().click();
        basePageSteps.onListingPage().popup().link(FAVORITES_TITLE).waitUntil(isDisplayed()).click();
        basePageSteps.onFavoritesPage().lkPageTitle().waitUntil(hasText(FAVORITES_TITLE));
        basePageSteps.onFavoritesPage().header().back().click();

        basePageSteps.onListingPage().h1().should(hasText(HOMEPAGE_H1));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход с главной в «Настройки профиля» по клику в юзер-попапе")
    public void shouldSeeHomePageToProfileSettingsFromUserPopup() {
        basePageSteps.onListingPage().header().burger().click();
        basePageSteps.onListingPage().popup().link(PROFILE_SETTINGS_TITLE).waitUntil(isDisplayed()).click();

        basePageSteps.onContactsPage().pageTitle().should(hasText(PROFILE_SETTINGS_TITLE));
        urlSteps.testing().path(MY).path(CONTACTS).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на главную с «Настройки профиля» после перехода с юзер-попапа")
    public void shouldSeeBackToHomePageFromProfileSettingsFromUserPopup() {
        basePageSteps.onListingPage().header().burger().click();
        basePageSteps.onListingPage().popup().link(PROFILE_SETTINGS_TITLE).waitUntil(isDisplayed()).click();
        basePageSteps.onContactsPage().pageTitle().waitUntil(hasText(PROFILE_SETTINGS_TITLE));
        basePageSteps.wait500MS();
        basePageSteps.onContactsPage().backButton().click();

        basePageSteps.onListingPage().h1().should(hasText(HOMEPAGE_H1));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход с главной на форму по клику в юзер-попапе")
    public void shouldSeeHomePageToFormFromUserPopup() {
        basePageSteps.onListingPage().header().burger().click();
        basePageSteps.onListingPage().popup().link(CREATE_OFFER).waitUntil(isDisplayed()).click();

        basePageSteps.onListingPage().h1().should(hasText(FORM_PAGE_H1));
        urlSteps.testing().path(FORM).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на главную с формы после перехода с юзер-попапа")
    public void shouldSeeBackToHomePageFromFormFromUserPopup() {
        basePageSteps.onListingPage().header().burger().click();
        basePageSteps.onListingPage().popup().link(CREATE_OFFER).waitUntil(isDisplayed()).click();
        basePageSteps.onFormPage().h1().waitUntil(hasText(FORM_PAGE_H1));
        basePageSteps.onFormPage().close().click();

        basePageSteps.onListingPage().h1().should(hasText(HOMEPAGE_H1));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

}
