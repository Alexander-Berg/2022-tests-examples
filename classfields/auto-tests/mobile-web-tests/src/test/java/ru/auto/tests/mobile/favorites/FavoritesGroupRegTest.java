package ru.auto.tests.mobile.favorites;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.FAVORITES;
import static ru.auto.tests.desktop.consts.Notifications.DELETED_FROM_FAV;
import static ru.auto.tests.desktop.consts.Notifications.ONE_OFFER_ADDED_TO_FAV;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Избранное на групповой карточке под зарегом")
@Feature(FAVORITES)
@Story(AutoruFeatures.GROUP)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class FavoritesGroupRegTest {

    private static final String PATH = "/kia/optima/21342050-21342121/";

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
                "desktop/UserFavoritesCarsPost",
                "desktop/UserFavoritesCarsDelete",
                "mobile/SearchCarsBreadcrumbsMarkModelGroup",
                "mobile/SearchCarsGroupContextGroup",
                "mobile/SearchCarsGroupContextListing").post();

        urlSteps.testing().path(CARS).path(NEW).path(GROUP).path(PATH).open();
        basePageSteps.onGroupPage().getSale(0).addToFavoritesIcon().waitUntil(isDisplayed()).click();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Добавление в избранное")
    public void shouldAddToFavorites() {
        basePageSteps.onGroupPage().notifier().waitUntil(isDisplayed()).should(hasText(ONE_OFFER_ADDED_TO_FAV));
        basePageSteps.onGroupPage().getSale(0).deleteFromFavoritesIcon().waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Удаление из избранного")
    public void shouldDeleteFromFavorites() {
        basePageSteps.onGroupPage().getSale(0).deleteFromFavoritesIcon().waitUntil(isDisplayed()).click();
        basePageSteps.onGroupPage().notifier().waitUntil(isDisplayed()).should(hasText(DELETED_FROM_FAV));
        basePageSteps.onGroupPage().getSale(0).addToFavoritesIcon().waitUntil(isDisplayed());
    }
}
