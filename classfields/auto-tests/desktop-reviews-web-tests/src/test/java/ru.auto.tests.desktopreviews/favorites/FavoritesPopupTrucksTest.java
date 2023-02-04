package ru.auto.tests.desktopreviews.favorites;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FAVORITES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Поп-ап избранного")
@Feature(FAVORITES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class FavoritesPopupTrucksTest {

    private static final String SALE_ID = "/19168121-abb524a9/";
    private static final int FAVORITES_COUNT = 3;
    private static final int FAVORITE_ID = 2;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/SearchCarsBreadcrumbsEmpty",
                "desktop/UserFavoritesAll").post();

        urlSteps.testing().path(REVIEWS).path(CARS).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Переход на карточку объявления из поп-апа избранного")
    public void shouldClickSale() {
        basePageSteps.onReviewsMainPage().header().favoritesButton().click();
        basePageSteps.onReviewsMainPage().favoritesPopup().favoritesList().should(hasSize(FAVORITES_COUNT))
                .get(FAVORITE_ID).link().click();
        basePageSteps.onReviewsMainPage().favoritesPopup().waitUntil(not(isDisplayed()));
        urlSteps.testing().path(TRUCK).path(USED).path(SALE).path("/zil/5301/").path(SALE_ID).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Показать телефон»")
    public void shouldClickShowPhoneButton() {
        mockRule.with("desktop/OfferTrucksPhones").update();

        basePageSteps.onReviewsMainPage().header().favoritesButton().click();
        basePageSteps.onReviewsMainPage().favoritesPopup().getFavorite(FAVORITE_ID).button("Показать телефон")
                .click();
        basePageSteps.onReviewsMainPage().contactsPopup().phones().waitUntil(hasText("+7 916 039-84-27\n" +
                "c 10:00 до 23:00\n+7 916 039-84-28\nc 12:00 до 20:00"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение статистики звонков")
    public void shouldSeeCallsStats() {
        mockRule.with("desktop/OfferTrucksCallsStats").update();

        basePageSteps.onReviewsMainPage().header().favoritesButton().click();
        basePageSteps.onReviewsMainPage().favoritesPopup().getFavorite(FAVORITE_ID).callsStats()
                .should(hasText("5 звонков за 24 часа"));
    }
}