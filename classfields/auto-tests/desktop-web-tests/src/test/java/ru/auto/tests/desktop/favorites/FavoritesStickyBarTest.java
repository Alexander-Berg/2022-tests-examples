package ru.auto.tests.desktop.favorites;

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
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Избранное в плавающей панели на карточке")
@Feature(FAVORITES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class FavoritesStickyBarTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/OfferCarsUsedUser",
                "desktop/UserFavoritesCarsPost").post();

        basePageSteps.setWideWindowSize();
        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.scrollDown(1000);
        basePageSteps.onCardPage().stickyBar().waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Добавление в избранное")
    public void shouldAddToFavorites() {
        mockRule.with("desktop/UserFavoritesAll").update();

        basePageSteps.onCardPage().stickyBar().favoriteButton().click();
        basePageSteps.onCardPage().stickyBar().favoriteDeleteButton().waitUntil(isDisplayed());
        basePageSteps.onCardPage().notifier().waitUntil(isDisplayed()).should(hasText("В избранном 1 предложение" +
                "Перейти в избранное"));
        basePageSteps.onCardPage().notifier().waitUntil("Не исчезла плашка", not(isDisplayed()), 6);

        basePageSteps.onCardPage().header().favoritesButton().click();
        basePageSteps.onCardPage().favoritesPopup().waitUntil(isDisplayed());
        basePageSteps.onCardPage().favoritesPopup().favoritesList().should(hasSize(3));
        basePageSteps.onCardPage().favoritesPopup().getFavorite(0).link()
                .should(hasAttribute("href", urlSteps.getCurrentUrl()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Удаление из избранного")
    public void shouldDeleteFromFavorites() {
        mockRule.with("desktop/UserFavoritesCarsDelete",
                "desktop/UserFavoritesCarsEmpty").update();

        basePageSteps.onCardPage().stickyBar().favoriteButton().click();
        basePageSteps.onCardPage().stickyBar().favoriteDeleteButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().notifier().waitUntil(isDisplayed()).should(hasText("Удалено из избранного"));
        basePageSteps.onCardPage().notifier().waitUntil("Не исчезла плашка", not(isDisplayed()), 6);

        basePageSteps.onCardPage().header().favoritesButton().click();
        basePageSteps.onCardPage().favoritesPopup().waitUntil(isDisplayed());
        basePageSteps.onCardPage().favoritesPopup().favoritesList().should(hasSize(0));
    }
}
