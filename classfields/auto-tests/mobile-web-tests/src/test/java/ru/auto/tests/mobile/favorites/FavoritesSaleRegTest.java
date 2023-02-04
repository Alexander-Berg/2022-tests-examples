package ru.auto.tests.mobile.favorites;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.UseParametersRunnerFactory;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FAVORITES;
import static ru.auto.tests.desktop.consts.Notifications.DELETED_FROM_FAV;
import static ru.auto.tests.desktop.consts.Notifications.ONE_OFFER_ADDED_TO_FAV;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

//import io.qameta.allure.Parameter;

@DisplayName("Избранное на карточке под зарегом")
@Feature(FAVORITES)
@RunWith(Parameterized.class)
@GuiceModules(MobileTestsModule.class)
@UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class FavoritesSaleRegTest {

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

    //@Parameter("Категория ТС")
    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String saleMock;

    @Parameterized.Parameter(2)
    public String favoritePostMock;

    @Parameterized.Parameter(3)
    public String favoriteDeleteMock;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object> getParameters() {
        return asList(new Object[][]{
                {CARS, "desktop/OfferCarsUsedUser",
                        "desktop/UserFavoritesCarsPost", "desktop/UserFavoritesCarsDelete"},
                {TRUCK, "desktop/OfferTrucksUsedUser",
                        "desktop/UserFavoritesTrucksPost", "desktop/UserFavoritesTrucksDelete"},
                {MOTORCYCLE, "desktop/OfferMotoUsedUser",
                        "desktop/UserFavoritesMotoPost", "desktop/UserFavoritesMotoDelete"},
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                saleMock,
                favoritePostMock,
                favoriteDeleteMock).post();

        urlSteps.testing().path(category).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Добавление в избранное на карточке")
    public void shouldAddToFavorites() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().cardActions().addToFavoritesButton());
        basePageSteps.onCardPage().notifier().waitUntil(isDisplayed()).should(hasText(ONE_OFFER_ADDED_TO_FAV));
        basePageSteps.onCardPage().cardActions().deleteFromFavoritesButton().waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Удаление из избранного на карточке")
    public void shouldDeleteFromFavorites() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().cardActions().addToFavoritesButton());
        basePageSteps.onCardPage().cardActions().deleteFromFavoritesButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().notifier().waitUntil(isDisplayed()).should(hasText(DELETED_FROM_FAV));
        basePageSteps.onCardPage().cardActions().addToFavoritesButton().waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Добавление в избранное в галерее")
    public void shouldAddToFavoritesInGallery() {
        basePageSteps.onCardPage().gallery().getItem(0).waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().fullScreenGallery().addToFavoritesButton().click();
        basePageSteps.onCardPage().notifier().waitUntil(isDisplayed()).should(hasText(ONE_OFFER_ADDED_TO_FAV));
        basePageSteps.onCardPage().fullScreenGallery().deleteFromFavoritesButton().waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Удаление из избранного в галерее")
    public void shouldDeleteFromFavoritesInGallery() {
        basePageSteps.onCardPage().gallery().getItem(0).waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().fullScreenGallery().addToFavoritesButton().click();
        basePageSteps.onCardPage().fullScreenGallery().deleteFromFavoritesButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().notifier().waitUntil(isDisplayed()).should(hasText(DELETED_FROM_FAV));
        basePageSteps.onCardPage().fullScreenGallery().addToFavoritesButton().waitUntil(isDisplayed());
    }
}
