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

import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.general.consts.GeneralFeatures.FAVORITES_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.FAVORITE_OFFERS;
import static ru.yandex.general.consts.Notifications.ADDED_TO_FAV;
import static ru.yandex.general.consts.Notifications.OFFER_DELETED_FROM_FAV;
import static ru.yandex.general.consts.Owners.ILUHA;
import static ru.yandex.general.consts.Pages.FAVORITES;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;
import static ru.yandex.general.step.BasePageSteps.GRID;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(FAVORITES_FEATURE)
@Feature(FAVORITE_OFFERS)
@DisplayName("Добавление в избранное")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class RemoveFromFavoritesFromListingTest {

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
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, GRID);
        urlSteps.testing().open();
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Удаление из избранного с листинга")
    public void shouldRemoveFromListing() {
        basePageSteps.onListingPage().snippetFirst().hover();
        basePageSteps.onListingPage().snippetFirst().addToFavorite().click();
        basePageSteps.onOfferCardPage().popupNotification(ADDED_TO_FAV).waitUntil(isDisplayed());
        basePageSteps.onListingPage().snippetFirst().hover();
        basePageSteps.onListingPage().snippetFirst().addToFavorite().click();
        basePageSteps.onOfferCardPage().popupNotification(OFFER_DELETED_FROM_FAV).waitUntil(isDisplayed());
        urlSteps.testing().path(MY).path(FAVORITES).open();
        basePageSteps.onFavoritesPage().favoritesCards().should(hasSize(0));
    }
}
