package ru.yandex.general.favorites;

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

import static org.hamcrest.CoreMatchers.not;
import static ru.yandex.general.consts.GeneralFeatures.FAVORITES_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.NAVIGATION_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CONTACTS;
import static ru.yandex.general.consts.Pages.FAVORITES;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.OFFERS;
import static ru.yandex.general.consts.Pages.STATS;
import static ru.yandex.general.consts.QueryParams.PROFILES_TAB_VALUE;
import static ru.yandex.general.consts.QueryParams.SEARCHES_TAB_VALUE;
import static ru.yandex.general.consts.QueryParams.TAB_PARAM;
import static ru.yandex.general.mobile.page.FavoritesPage.PROFILES;
import static ru.yandex.general.mobile.page.FavoritesPage.SEARCHES;
import static ru.yandex.general.mobile.page.FormPage.FORM_PAGE_H1;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_EXPIRED_DIALOG_WAS_SHOW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(FAVORITES_FEATURE)
@Feature(NAVIGATION_FEATURE)
@DisplayName("Навигация с раздела «Избранное» в ЛК")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class NavigationMyFavoritesTest {

    private static final String FAVORITES_TITLE = "Избранное";
    private static final String MY_OFFERS_TITLE = "Мои объявления";
    private static final String PROFILE_SETTINGS_TITLE = "Настройки профиля";
    private static final String STATISTICS_TITLE = "Статистика";
    private static final String CREATE_OFFER = "Разместить объявление";
    private static final String NOUTBUKI_H1 = "Ноутбуки в Москве";

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
        urlSteps.testing().path(MY).path(FAVORITES).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход с «Избранное» в «Мои объявления» по клику в таббаре")
    public void shouldSeeFavoritesToMyOffersFromTabbar() {
        basePageSteps.onFavoritesPage().tabBar().myOffers().click();

        basePageSteps.onMyOffersPage().lkPageTitle().should(hasText(MY_OFFERS_TITLE));
        urlSteps.testing().path(MY).path(OFFERS).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на «Избранное» с «Мои объявления» после перехода по таббару")
    public void shouldSeeBackToMyOffersFromFavoritesFromTabbar() {
        basePageSteps.onFavoritesPage().tabBar().myOffers().click();
        basePageSteps.onMyOffersPage().lkPageTitle().waitUntil(hasText(MY_OFFERS_TITLE));
        basePageSteps.onMyOffersPage().header().back().click();

        basePageSteps.onMyOffersPage().lkPageTitle().should(hasText(FAVORITES_TITLE));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход с «Избранное» в «Мои объявления» по клику в юзер-попапе")
    public void shouldSeeFavoritesToMyOffersFromUserPopup() {
        basePageSteps.onFavoritesPage().header().burger().click();
        basePageSteps.onFavoritesPage().popup().link(MY_OFFERS_TITLE).waitUntil(isDisplayed()).click();

        basePageSteps.onMyOffersPage().lkPageTitle().should(hasText(MY_OFFERS_TITLE));
        urlSteps.testing().path(MY).path(OFFERS).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на «Избранное» с «Мои объявления» после перехода с юзер-попапа")
    public void shouldSeeBackFavoritesFromMyOffersFromUserPopup() {
        basePageSteps.onFavoritesPage().header().burger().click();
        basePageSteps.onFavoritesPage().popup().link(MY_OFFERS_TITLE).waitUntil(isDisplayed()).click();
        basePageSteps.onMyOffersPage().lkPageTitle().waitUntil(hasText(MY_OFFERS_TITLE));
        basePageSteps.onMyOffersPage().header().back().click();

        basePageSteps.onMyOffersPage().lkPageTitle().should(hasText(FAVORITES_TITLE));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход с «Избранное» в «Настройки профиля» по клику в юзер-попапе")
    public void shouldSeeFavoritesToProfileSettingsFromUserPopup() {
        basePageSteps.onFavoritesPage().header().burger().click();
        basePageSteps.onFavoritesPage().popup().link(PROFILE_SETTINGS_TITLE).waitUntil(isDisplayed()).click();

        basePageSteps.onContactsPage().pageTitle().should(hasText(PROFILE_SETTINGS_TITLE));
        urlSteps.testing().path(MY).path(CONTACTS).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на «Избранное» с «Настройки профиля» после перехода с юзер-попапа")
    public void shouldSeeBackToFavoritesFromProfileSettingsFromUserPopup() {
        basePageSteps.onFavoritesPage().header().burger().click();
        basePageSteps.onFavoritesPage().popup().link(PROFILE_SETTINGS_TITLE).waitUntil(isDisplayed()).click();
        basePageSteps.onContactsPage().pageTitle().waitUntil(hasText(PROFILE_SETTINGS_TITLE));
        basePageSteps.wait500MS();
        basePageSteps.onContactsPage().backButton().click();

        basePageSteps.onFavoritesPage().lkPageTitle().should(hasText(FAVORITES_TITLE));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход с «Избранное» в «Статистика» по клику в юзер-попапе")
    public void shouldSeeFavoritesToStatisticsFromUserPopup() {
        basePageSteps.onFavoritesPage().header().burger().click();
        basePageSteps.onFavoritesPage().popup().link(STATISTICS_TITLE).waitUntil(isDisplayed()).click();

        basePageSteps.onStatisticsPage().lkPageTitle().should(hasText(STATISTICS_TITLE));
        urlSteps.testing().path(MY).path(STATS).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на «Избранное» с «Статистика» после перехода с юзер-попапа")
    public void shouldSeeBackToFavoritesFromStatisticsFromUserPopup() {
        basePageSteps.onFavoritesPage().header().burger().click();
        basePageSteps.onFavoritesPage().popup().link(STATISTICS_TITLE).waitUntil(isDisplayed()).click();
        basePageSteps.onStatisticsPage().lkPageTitle().waitUntil(hasText(STATISTICS_TITLE));
        basePageSteps.wait500MS();
        basePageSteps.onContactsPage().backButton().click();

        basePageSteps.onFavoritesPage().lkPageTitle().should(hasText(FAVORITES_TITLE));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход с «Избранное» на форму по клику в юзер-попапе")
    public void shouldSeeFavoritesToFormFromUserPopup() {
        basePageSteps.onFavoritesPage().header().burger().click();
        basePageSteps.onFavoritesPage().popup().link(CREATE_OFFER).waitUntil(isDisplayed()).click();

        basePageSteps.onFormPage().h1().should(hasText(FORM_PAGE_H1));
        urlSteps.testing().path(FORM).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на «Избранное» с формы после перехода с юзер-попапа")
    public void shouldSeeBackToFavoritesFromFormFromUserPopup() {
        basePageSteps.onFavoritesPage().header().burger().click();
        basePageSteps.onFavoritesPage().popup().link(CREATE_OFFER).waitUntil(isDisplayed()).click();
        basePageSteps.onFormPage().h1().waitUntil(hasText(FORM_PAGE_H1));
        basePageSteps.onFormPage().close().click();

        basePageSteps.onFavoritesPage().lkPageTitle().should(hasText(FAVORITES_TITLE));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на форму подачи с «Избранное» по клику в таббаре")
    public void shouldSeeFavoritesToFormFromTabbar() {
        basePageSteps.onFavoritesPage().tabBar().addOffer().click();

        basePageSteps.onFormPage().h1().should(hasText(FORM_PAGE_H1));
        urlSteps.testing().path(FORM).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на главную с «Избранное» по клику в таббаре")
    public void shouldSeeFavoritesToHomepageFromTabbar() {
        basePageSteps.onFavoritesPage().tabBar().mainPage().click();

        basePageSteps.onListingPage().h1().should(hasText("Объявления в Москве"));
        urlSteps.testing().path(MOSKVA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на карточку оффера с «Избранное»")
    public void shouldSeeFavoritesToOffer() {
        String offerUrl = basePageSteps.onFavoritesPage().firstFavCard().getUrl();
        basePageSteps.onFavoritesPage().firstFavCard().click();

        basePageSteps.onOfferCardPage().priceBuyer().should(isDisplayed());
        urlSteps.fromUri(offerUrl).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на «Избранное» с оффера")
    public void shouldSeeBackToFavoritesFromOffer() {
        basePageSteps.onFavoritesPage().firstFavCard().click();
        basePageSteps.onOfferCardPage().priceBuyer().waitUntil(isDisplayed());
        basePageSteps.onOfferCardPage().backButton().click();

        basePageSteps.onFavoritesPage().lkPageTitle().should(hasText(FAVORITES_TITLE));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на сохраненный поиск с «Избранное»")
    public void shouldSeeFavoritesToSavedSearch() {
        basePageSteps.onFavoritesPage().tab(SEARCHES).click();
        basePageSteps.wait500MS();
        String searchUrl = basePageSteps.onFavoritesPage().firstFavCard().getUrl();
        basePageSteps.onFavoritesPage().firstFavCard().click();

        basePageSteps.onListingPage().h1().should(hasText(NOUTBUKI_H1));
        urlSteps.fromUri(searchUrl).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на «Избранное» с сохраненного поиска")
    public void shouldSeeBackToFavoritesFromSavedSearch() {
        basePageSteps.onFavoritesPage().tab(SEARCHES).click();
        basePageSteps.wait500MS();
        basePageSteps.onFavoritesPage().firstFavCard().click();
        basePageSteps.onListingPage().h1().waitUntil(hasText(NOUTBUKI_H1));
        basePageSteps.onListingPage().header().back().click();

        basePageSteps.onFavoritesPage().lkPageTitle().should(hasText(FAVORITES_TITLE));
        urlSteps.queryParam(TAB_PARAM, SEARCHES_TAB_VALUE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на сохраненный профиль с «Избранное»")
    public void shouldSeeFavoritesToSavedProfile() {
        basePageSteps.onFavoritesPage().tab(PROFILES).click();
        basePageSteps.wait500MS();
        String profileUrl = basePageSteps.onFavoritesPage().firstFavCard().getUrl();
        basePageSteps.onFavoritesPage().firstFavCard().click();

        basePageSteps.onProfilePage().userInfo().should(isDisplayed());
        urlSteps.fromUri(profileUrl).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на «Избранное» с сохраненного профиля")
    public void shouldSeeBackToFavoritesFromSavedProfile() {
        basePageSteps.onFavoritesPage().tab(PROFILES).click();
        basePageSteps.wait500MS();
        basePageSteps.onFavoritesPage().firstFavCard().click();
        basePageSteps.onProfilePage().userInfo().waitUntil(isDisplayed());
        basePageSteps.onProfilePage().header().back().click();

        basePageSteps.onFavoritesPage().lkPageTitle().should(hasText(FAVORITES_TITLE));
        urlSteps.queryParam(TAB_PARAM, PROFILES_TAB_VALUE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет кнопки «Назад» при открытии «Избранное» по прямой ссылке")
    public void shouldNotSeeBackButton() {
        basePageSteps.onFavoritesPage().header().back().should(not(isDisplayed()));
    }

}
