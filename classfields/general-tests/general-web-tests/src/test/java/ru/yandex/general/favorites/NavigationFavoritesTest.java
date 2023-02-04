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
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.consts.GeneralFeatures.FAVORITES_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.NAVIGATION_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FAVORITES;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.OFFERS;
import static ru.yandex.general.mobile.page.FavoritesPage.PROFILES;
import static ru.yandex.general.mobile.page.FavoritesPage.SEARCHES;
import static ru.yandex.general.page.BasePage.PXLS_TO_FLOAT_HEADER;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_EXPIRED_DIALOG_WAS_SHOW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(FAVORITES_FEATURE)
@Feature(NAVIGATION_FEATURE)
@DisplayName("Навигация с раздела «Избранное» в ЛК")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class NavigationFavoritesTest {

    private static final String MY_OFFERS_TITLE = "Мои объявления";
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
    @DisplayName("Переход на карточку оффера с «Избранное»")
    public void shouldSeeFavoritesToOffer() {
        String offerUrl = basePageSteps.onFavoritesPage().firstFavCard().getUrl();
        basePageSteps.onFavoritesPage().firstFavCard().click();
        basePageSteps.switchToNextTab();

        basePageSteps.onOfferCardPage().sidebar().price().should(isDisplayed());
        urlSteps.fromUri(offerUrl).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на сохраненный поиск с «Избранное»")
    public void shouldSeeFavoritesToSavedSearch() {
        basePageSteps.onFavoritesPage().tab(SEARCHES).click();
        basePageSteps.wait500MS();
        String searchUrl = basePageSteps.onFavoritesPage().firstFavCard().getUrl();
        basePageSteps.onFavoritesPage().firstFavCard().click();
        basePageSteps.switchToNextTab();

        basePageSteps.onListingPage().h1().should(hasText(NOUTBUKI_H1));
        urlSteps.fromUri(searchUrl).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на сохраненный профиль с «Избранное»")
    public void shouldSeeFavoritesToSavedProfile() {
        basePageSteps.onFavoritesPage().tab(PROFILES).click();
        basePageSteps.wait500MS();
        String profileUrl = basePageSteps.onFavoritesPage().firstFavCard().getUrl();
        basePageSteps.onFavoritesPage().firstFavCard().click();
        basePageSteps.switchToNextTab();

        basePageSteps.onProfilePage().sidebar().followersCount().should(isDisplayed());
        urlSteps.fromUri(profileUrl).shouldNotDiffWithWebDriverUrl();
    }

}
