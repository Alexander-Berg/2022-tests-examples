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
import static ru.yandex.general.consts.GeneralFeatures.FAVORITE_SELLERS;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FAVORITES;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.PROFILE;
import static ru.yandex.general.consts.Pages.SELLER_PATH;
import static ru.yandex.general.consts.QueryParams.PROFILES_TAB_VALUE;
import static ru.yandex.general.consts.QueryParams.TAB_PARAM;
import static ru.yandex.general.page.PublicProfilePage.LET;
import static ru.yandex.general.page.PublicProfilePage.SUBSCRIBE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(FAVORITES_FEATURE)
@Feature(FAVORITE_SELLERS)
@DisplayName("Удаление сохраненного продавца")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class FavoritesSellerRemoveTest {

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
        basePageSteps.resize(1920, 1080);
        urlSteps.testing().path(PROFILE).path(SELLER_PATH).open();
        basePageSteps.onProfilePage().sidebar().button(SUBSCRIBE).click();
        basePageSteps.onProfilePage().popup().button(LET).click();
        basePageSteps.onListingPage().popupNotification("Вы успешно подписались!").waitUntil(isDisplayed());
        urlSteps.testing().path(MY).path(FAVORITES).queryParam(TAB_PARAM, PROFILES_TAB_VALUE).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Удаление сохраненного продавца")
    public void shouldDeleteFavoriteSeller() {
        basePageSteps.onFavoritesPage().favoritesCards().waitUntil(hasSize(1));
        basePageSteps.onFavoritesPage().firstFavCard().hover();
        basePageSteps.onFavoritesPage().firstFavCard().deleteButton().click();
        basePageSteps.onFavoritesPage().modal().button(DELETE_BUTTON).click();
        basePageSteps.onFavoritesPage().favoritesCards().should(hasSize(0));
    }

}
