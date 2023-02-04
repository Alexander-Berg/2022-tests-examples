package ru.yandex.general.myOffers;

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
import static ru.yandex.general.consts.GeneralFeatures.MY_OFFERS_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.NAVIGATION_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CONTACTS;
import static ru.yandex.general.consts.Pages.FAVORITES;
import static ru.yandex.general.consts.Pages.FORM;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.OFFERS;
import static ru.yandex.general.consts.Pages.STATS;
import static ru.yandex.general.element.MyOfferSnippet.EDIT;
import static ru.yandex.general.mobile.page.FormPage.FORM_PAGE_H1;
import static ru.yandex.general.page.FormPage.SAVE;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_EXPIRED_DIALOG_WAS_SHOW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(MY_OFFERS_FEATURE)
@Feature(NAVIGATION_FEATURE)
@DisplayName("Навигация с раздела «Мои объявления» в ЛК")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class NavigationMyOffersTest {

    private static final String FAVORITES_TITLE = "Избранное";
    private static final String STATISTICS_TITLE = "Статистика";
    private static final String MY_OFFERS_TITLE = "Мои объявления";
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
        urlSteps.testing().path(MY).path(OFFERS).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход с «Мои объявления» в «Избранное» по клику в таббаре")
    public void shouldSeeMyOffersToFavoritesFromTabbar() {
        basePageSteps.onMyOffersPage().tabBar().favorites().click();

        basePageSteps.onFavoritesPage().lkPageTitle().should(hasText(FAVORITES_TITLE));
        urlSteps.testing().path(MY).path(FAVORITES).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на «Мои объявления» с «Избранное» после перехода по таббару")
    public void shouldSeeBackToMyOffersFromFavoritesFromTabbar() {
        basePageSteps.onMyOffersPage().tabBar().favorites().click();
        basePageSteps.onFavoritesPage().lkPageTitle().waitUntil(hasText(FAVORITES_TITLE));
        basePageSteps.onFavoritesPage().header().back().click();

        basePageSteps.onMyOffersPage().lkPageTitle().should(hasText(MY_OFFERS_TITLE));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход с «Мои объявления» в «Избранное» по клику в юзер-попапе")
    public void shouldSeeMyOffersToFavoritesFromUserPopup() {
        basePageSteps.onMyOffersPage().header().burger().click();
        basePageSteps.onMyOffersPage().popup().link(FAVORITES_TITLE).waitUntil(isDisplayed()).click();

        basePageSteps.onFavoritesPage().lkPageTitle().should(hasText(FAVORITES_TITLE));
        urlSteps.testing().path(MY).path(FAVORITES).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на «Мои объявления» с «Избранное» после перехода с юзер-попапа")
    public void shouldSeeBackToMyOffersFromFavoritesFromUserPopup() {
        basePageSteps.onMyOffersPage().header().burger().click();
        basePageSteps.onMyOffersPage().popup().link(FAVORITES_TITLE).waitUntil(isDisplayed()).click();
        basePageSteps.onFavoritesPage().lkPageTitle().waitUntil(hasText(FAVORITES_TITLE));
        basePageSteps.onFavoritesPage().header().back().click();

        basePageSteps.onMyOffersPage().lkPageTitle().should(hasText(MY_OFFERS_TITLE));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход с «Мои объявления» в «Статистика» по клику в юзер-попапе")
    public void shouldSeeMyOffersToStatisticsFromUserPopup() {
        basePageSteps.onMyOffersPage().header().burger().click();
        basePageSteps.onMyOffersPage().popup().link(STATISTICS_TITLE).waitUntil(isDisplayed()).click();

        basePageSteps.onStatisticsPage().lkPageTitle().should(hasText(STATISTICS_TITLE));
        urlSteps.testing().path(MY).path(STATS).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на «Мои объявления» с «Статистика» после перехода с юзер-попапа")
    public void shouldSeeBackToMyOffersFromStatisticsFromUserPopup() {
        basePageSteps.onMyOffersPage().header().burger().click();
        basePageSteps.onMyOffersPage().popup().link(STATISTICS_TITLE).waitUntil(isDisplayed()).click();
        basePageSteps.onStatisticsPage().lkPageTitle().waitUntil(hasText(STATISTICS_TITLE));
        basePageSteps.wait500MS();
        basePageSteps.onStatisticsPage().backButton().click();

        basePageSteps.onMyOffersPage().lkPageTitle().should(hasText(MY_OFFERS_TITLE));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход с «Мои объявления» в «Настройки профиля» по клику в юзер-попапе")
    public void shouldSeeMyOffersToProfileSettingsFromUserPopup() {
        basePageSteps.onMyOffersPage().header().burger().click();
        basePageSteps.onMyOffersPage().popup().link(PROFILE_SETTINGS_TITLE).waitUntil(isDisplayed()).click();

        basePageSteps.onContactsPage().pageTitle().should(hasText(PROFILE_SETTINGS_TITLE));
        urlSteps.testing().path(MY).path(CONTACTS).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на «Мои объявления» с «Настройки профиля» после перехода с юзер-попапа")
    public void shouldSeeBackToMyOffersFromProfileSettingsFromUserPopup() {
        basePageSteps.onMyOffersPage().header().burger().click();
        basePageSteps.onMyOffersPage().popup().link(PROFILE_SETTINGS_TITLE).waitUntil(isDisplayed()).click();
        basePageSteps.onContactsPage().pageTitle().waitUntil(hasText(PROFILE_SETTINGS_TITLE));
        basePageSteps.wait500MS();
        basePageSteps.onContactsPage().backButton().click();

        basePageSteps.onMyOffersPage().lkPageTitle().should(hasText(MY_OFFERS_TITLE));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход с «Мои объявления» на форму по клику в юзер-попапе")
    public void shouldSeeMyOffersToFormFromUserPopup() {
        basePageSteps.onMyOffersPage().header().burger().click();
        basePageSteps.onMyOffersPage().popup().link(CREATE_OFFER).waitUntil(isDisplayed()).click();

        basePageSteps.onFormPage().h1().should(hasText(FORM_PAGE_H1));
        urlSteps.testing().path(FORM).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на «Мои объявления» с формы после перехода с юзер-попапа")
    public void shouldSeeBackToMyOffersFromFormFromUserPopup() {
        basePageSteps.onMyOffersPage().header().burger().click();
        basePageSteps.onMyOffersPage().popup().link(CREATE_OFFER).waitUntil(isDisplayed()).click();
        basePageSteps.onFormPage().h1().waitUntil(hasText(FORM_PAGE_H1));
        basePageSteps.onFormPage().close().click();

        basePageSteps.onMyOffersPage().lkPageTitle().should(hasText(MY_OFFERS_TITLE));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на форму подачи с «Мои объявления» по клику в таббаре")
    public void shouldSeeMyOffersToFormFromTabbar() {
        basePageSteps.onMyOffersPage().tabBar().addOffer().click();

        basePageSteps.onFormPage().h1().should(hasText(FORM_PAGE_H1));
        urlSteps.testing().path(FORM).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на главную с «Мои объявления» по клику в таббаре")
    public void shouldSeeMyOffersToHomepageFromTabbar() {
        basePageSteps.onMyOffersPage().tabBar().mainPage().click();

        basePageSteps.onListingPage().h1().should(hasText("Объявления в Москве"));
        urlSteps.testing().path(MOSKVA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на карточку оффера с «Мои объявления»")
    public void shouldSeeMyOffersToOffer() {
        String offerUrl = basePageSteps.onMyOffersPage().snippetFirst().getUrl();
        basePageSteps.onMyOffersPage().snippetFirst().click();

        basePageSteps.onOfferCardPage().priceOwner().should(isDisplayed());
        urlSteps.fromUri(offerUrl).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Возвращение на «Мои объявления» с оффера")
    public void shouldSeeBackToMyOffersFromOffer() {
        basePageSteps.onMyOffersPage().snippetFirst().click();
        basePageSteps.onOfferCardPage().priceOwner().waitUntil(isDisplayed());
        basePageSteps.onOfferCardPage().backButton().click();

        basePageSteps.onMyOffersPage().lkPageTitle().should(hasText(MY_OFFERS_TITLE));
        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображаются «Мои объявления» после редактирования оффера с «Мои объявления»")
    public void shouldSeeMyOffersAfterSaveEdit() {
        basePageSteps.onMyOffersPage().snippetFirst().offerAction().click();
        basePageSteps.onMyOffersPage().popup().spanLink(EDIT).click();
        basePageSteps.onFormPage().button(SAVE).click();

        urlSteps.shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет кнопки «Назад» при открытии «Мои объявления» по прямой ссылке")
    public void shouldNotSeeBackButton() {
        basePageSteps.onMyOffersPage().header().back().should(not(isDisplayed()));
    }

}
