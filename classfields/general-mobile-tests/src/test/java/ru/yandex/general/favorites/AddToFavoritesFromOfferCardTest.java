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

import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.general.consts.GeneralFeatures.FAVORITES_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.FAVORITE_OFFERS;
import static ru.yandex.general.consts.Owners.ILUHA;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.FAVORITES;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.mobile.step.BasePageSteps.CLASSIFIED_USER_HAS_SEEN_PROFILE;
import static ru.yandex.general.mobile.step.BasePageSteps.TRUE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(FAVORITES_FEATURE)
@Feature(FAVORITE_OFFERS)
@DisplayName("Добавление в избранное")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class AddToFavoritesFromOfferCardTest {

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
        passportSteps.createAccountAndLogin();
        basePageSteps.setMoscowCookie();
        basePageSteps.setCookie(CLASSIFIED_USER_HAS_SEEN_PROFILE, TRUE);
        urlSteps.testing().path(ELEKTRONIKA).open();
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Добавление в избранное в карточке оффера")
    public void shouldAddFromOfferCard() {
        basePageSteps.onListingPage().snippetFirst().link().click();
        basePageSteps.onOfferCardPage().addToFavorite().click();
        basePageSteps.onOfferCardPage().popupNotification("Добавили в избранное").waitUntil(isDisplayed());
        urlSteps.testing().path(MY).path(FAVORITES).open();
        basePageSteps.onFavoritesPage().favoritesCards().should(hasSize(1));
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Удаляем из избранного в карточке оффера. Избранное будет пусто")
    public void shouldDeleteFromOfferCard() {
        basePageSteps.onListingPage().snippetFirst().link().click();
        basePageSteps.onOfferCardPage().addToFavorite().click();
        basePageSteps.onOfferCardPage().popupNotification("Добавили в избранное").waitUntil(isDisplayed());
        basePageSteps.onOfferCardPage().addToFavorite().click();
        basePageSteps.onOfferCardPage().popupNotification("Объявление удалено из избранного").waitUntil(isDisplayed());
        urlSteps.testing().path(MY).path(FAVORITES).open();
        basePageSteps.onFavoritesPage().favoritesCards().should(hasSize(0));
    }

}
