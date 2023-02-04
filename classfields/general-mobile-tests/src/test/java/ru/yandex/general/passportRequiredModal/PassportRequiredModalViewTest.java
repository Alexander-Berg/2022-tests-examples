package ru.yandex.general.passportRequiredModal;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.consts.GeneralFeatures.PASSPORT_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(PASSPORT_FEATURE)
@DisplayName("Отображение модалки паспортного логина")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class PassportRequiredModalViewTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        basePageSteps.setMoscowCookie();
        urlSteps.testing().path(ELEKTRONIKA).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение модалки паспортного логина по тапу на «Добавить в избранное»")
    public void shouldSeePassportModalFromOfferLike() {
        basePageSteps.onListingPage().snippetFirst().addToFavorite().click();

        basePageSteps.onListingPage().passportRequiredModal().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение модалки паспортного логина по тапу на «Сохранить поиск»")
    public void shouldSeePassportModalFromSaveSearch() {
        basePageSteps.onListingPage().searchBar().saveSearch().click();

        basePageSteps.onListingPage().passportRequiredModal().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение модалки паспортного логина по тапу на «Мои объявления» в таббаре")
    public void shouldSeePassportModalFromMyOffersTabBar() {
        basePageSteps.onListingPage().tabBar().myOffers().click();

        basePageSteps.onListingPage().passportRequiredModal().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение модалки паспортного логина по тапу на «Избранное» в таббаре")
    public void shouldSeePassportModalFromFavoritesTabBar() {
        basePageSteps.onListingPage().tabBar().favorites().click();

        basePageSteps.onListingPage().passportRequiredModal().should(isDisplayed());
    }

}
