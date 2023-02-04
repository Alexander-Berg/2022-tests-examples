package ru.yandex.general.favorites;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.yandex.general.consts.GeneralFeatures.FAVORITES_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.FAVORITE_OFFERS;
import static ru.yandex.general.consts.Owners.ILUHA;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.FAVORITES;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;
import static ru.yandex.general.step.BasePageSteps.GRID;

@Epic(FAVORITES_FEATURE)
@Feature(FAVORITE_OFFERS)
@DisplayName("Удаление из избранного")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class OfferRemoveFromFavoritesTest {

    private static final String DELETE_BUTTON = "Удалить";

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
        urlSteps.testing().path(ELEKTRONIKA).open();
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Удаление одного оффера")
    public void shouldDeleteOneOffer() {
        basePageSteps.onListingPage().snippetFirst().hover();
        basePageSteps.onListingPage().snippetFirst().addToFavorite().click();
        basePageSteps.onListingPage().snippetFirst().addToFavorite().waitUntilChecked();
        basePageSteps.wait500MS();
        urlSteps.testing().path(MY).path(FAVORITES).open();
        basePageSteps.onFavoritesPage().firstFavCard().favButton().click();
        basePageSteps.onFavoritesPage().favoritesCards().waitUntil(hasSize(0));
    }

    @Test
    @Ignore("Ждем когда добавят контектсное меню в ЛК")
    @Owner(ILUHA)
    @DisplayName("Удаление всех офферов")
    public void shouldDeleteAllOffers() {
        basePageSteps.onListingPage().snippetFirst().hover();
        basePageSteps.onListingPage().snippetFirst().addToFavorite().click();
        basePageSteps.onListingPage().snippetFirst().addToFavorite().waitUntilChecked();
        basePageSteps.onListingPage().snippetSecond().hover();
        basePageSteps.onListingPage().snippetSecond().addToFavorite().click();
        basePageSteps.onListingPage().snippetFirst().addToFavorite().waitUntilChecked();
        basePageSteps.wait500MS();
        urlSteps.testing().path(MY).path(FAVORITES).open();
        basePageSteps.onFavoritesPage().favoritesCards().waitUntil(hasSize(2));
        basePageSteps.onFavoritesPage().checkAll().click();
        basePageSteps.onFavoritesPage().button(DELETE_BUTTON).click();
        waitPopupAnimation();
        basePageSteps.onFavoritesPage().popup().button(DELETE_BUTTON).click();
        basePageSteps.onFavoritesPage().favoritesCards().waitUntil(hasSize(0));
    }

    private void waitPopupAnimation() {
        waitSomething(2, TimeUnit.SECONDS);
    }
}
