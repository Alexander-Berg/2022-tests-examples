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
import static ru.yandex.general.consts.GeneralFeatures.FAVORITE_SEARCHES;
import static ru.yandex.general.consts.Notifications.DONE;
import static ru.yandex.general.consts.Notifications.SEARCH_SAVED;
import static ru.yandex.general.consts.Owners.ILUHA;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.FAVORITES;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.NOUTBUKI;
import static ru.yandex.general.consts.QueryParams.SEARCHES_TAB_VALUE;
import static ru.yandex.general.consts.QueryParams.TAB_PARAM;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(FAVORITES_FEATURE)
@Feature(FAVORITE_SEARCHES)
@DisplayName("Удаление из избранного. Поиски")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class SearchesRemoveFromFavoritesTest {

    private static final String DELETE_BUTTON = "Удалить";
    private static final String YES = "Да";

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
    }

    @Test
    @Owner(ILUHA)
    @DisplayName("Удаление одного поиска")
    public void shouldDeleteOneSearch() {
        urlSteps.testing().path(ELEKTRONIKA).open();
        basePageSteps.onListingPage().saveSearch().click();
        basePageSteps.onListingPage().popup().button(YES).click();
        basePageSteps.onListingPage().popupNotification(DONE).waitUntil(isDisplayed());
        urlSteps.testing().path(MY).path(FAVORITES).queryParam(TAB_PARAM, SEARCHES_TAB_VALUE).open();
        basePageSteps.onFavoritesPage().favoritesCards().waitUntil(hasSize(1));
        basePageSteps.onFavoritesPage().firstFavCard().hover();
        basePageSteps.onFavoritesPage().firstFavCard().deleteButton().click();
        waitPopupAnimation();
        basePageSteps.onFavoritesPage().modal().button(DELETE_BUTTON).click();
        basePageSteps.onFavoritesPage().favoritesCards().should(hasSize(0));
    }

    @Test
    @Ignore("Ждем когда добавят контектсное меню в ЛК")
    @Owner(ILUHA)
    @DisplayName("Удаление всех поисков")
    public void shouldDeleteAllSearches() {
        urlSteps.testing().path(ELEKTRONIKA).open();
        basePageSteps.onListingPage().saveSearch().click();
        basePageSteps.onListingPage().popup().button(YES).click();
        basePageSteps.onListingPage().popupNotification(DONE).waitUntil(isDisplayed());
        urlSteps.testing().path(NOUTBUKI).open();
        basePageSteps.onListingPage().saveSearch().click();
        basePageSteps.onListingPage().popupNotification(SEARCH_SAVED).waitUntil(isDisplayed());
        urlSteps.testing().path(MY).path(FAVORITES).queryParam(TAB_PARAM, SEARCHES_TAB_VALUE).open();
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
