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
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
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
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(OFFER_CARD_FEATURE)
@Feature("Блок похожих сверху")
@DisplayName("Добавление/удаление в избранное из блока похожих сверху")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class OfferCardTopSimilarBlockFavoritesTest {

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
        urlSteps.testing().path(MOSKVA).path(KOMPUTERNAYA_TEHNIKA).path(NOUTBUKI).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем в избранное из блока похожих сверху")
    public void shouldSeeAddToFavoritesFromTopSimilarBlock() {
        basePageSteps.onListingPage().firstSnippet().click();
        basePageSteps.onOfferCardPage().firstSnippet().hover();
        basePageSteps.scrollToTop();
        basePageSteps.onOfferCardPage().similarCarouseItems().get(0).favorite().click();
        basePageSteps.onOfferCardPage().popupNotification("Добавили в избранное").waitUntil(isDisplayed());
        urlSteps.testing().path(MY).path(FAVORITES).open();

        basePageSteps.onFavoritesPage().favoritesCards().should(hasSize(1));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Удаляем из избранного из блока похожих сверху")
    public void shouldRemoveFavoritesFromTopSimilarBlock() {
        basePageSteps.onListingPage().firstSnippet().click();
        basePageSteps.onOfferCardPage().firstSnippet().hover();
        basePageSteps.scrollToTop();
        basePageSteps.onOfferCardPage().similarCarouseItems().get(0).favorite().click();
        basePageSteps.onOfferCardPage().popupNotification("Добавили в избранное").waitUntil(isDisplayed());
        basePageSteps.onOfferCardPage().similarCarouseItems().get(0).favorite().click();
        basePageSteps.onOfferCardPage().popupNotification("Объявление удалено из избранного").waitUntil(isDisplayed());
        urlSteps.testing().path(MY).path(FAVORITES).open();

        basePageSteps.onFavoritesPage().favoritesCards().should(hasSize(0));
    }

}
