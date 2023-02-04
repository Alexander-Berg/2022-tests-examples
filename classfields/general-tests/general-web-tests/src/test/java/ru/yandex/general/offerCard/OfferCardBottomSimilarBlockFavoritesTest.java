package ru.yandex.general.offerCard;

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
import static ru.yandex.general.consts.GeneralFeatures.OFFER_CARD_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FAVORITES;
import static ru.yandex.general.consts.Pages.KOMPUTERNAYA_TEHNIKA;
import static ru.yandex.general.consts.Pages.MOSKVA;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.NOUTBUKI;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;
import static ru.yandex.general.step.BasePageSteps.GRID;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(OFFER_CARD_FEATURE)
@Feature("Блок похожих снизу")
@DisplayName("Добавление/удаление в избранное из блока похожих снизу")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class OfferCardBottomSimilarBlockFavoritesTest {

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
        urlSteps.testing().path(MOSKVA).path(KOMPUTERNAYA_TEHNIKA).path(NOUTBUKI).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем в избранное из блока похожих снизу")
    public void shouldSeeAddToFavoritesFromBottomSimilarBlock() {
        basePageSteps.onListingPage().firstSnippet().click();
        basePageSteps.switchToNextTab();
        basePageSteps.onOfferCardPage().firstSnippet().hover();
        basePageSteps.onOfferCardPage().firstSnippet().addToFavorite().click();
        basePageSteps.onOfferCardPage().popupNotification("Добавили в избранное").waitUntil(isDisplayed());
        urlSteps.testing().path(MY).path(FAVORITES).open();

        basePageSteps.onFavoritesPage().favoritesCards().should(hasSize(1));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Удаляем из избранного из блока похожих снизу")
    public void shouldRemoveFavoritesFromBottomSimilarBlock() {
        basePageSteps.onListingPage().firstSnippet().click();
        basePageSteps.switchToNextTab();
        basePageSteps.onOfferCardPage().firstSnippet().hover();
        basePageSteps.onOfferCardPage().firstSnippet().addToFavorite().click();
        basePageSteps.onOfferCardPage().popupNotification("Добавили в избранное").waitUntil(isDisplayed());
        basePageSteps.onOfferCardPage().firstSnippet().addToFavorite().click();
        basePageSteps.onOfferCardPage().popupNotification("Объявление удалено из избранного").waitUntil(isDisplayed());
        urlSteps.testing().path(MY).path(FAVORITES).open();

        basePageSteps.onFavoritesPage().favoritesCards().should(hasSize(0));
    }

}
