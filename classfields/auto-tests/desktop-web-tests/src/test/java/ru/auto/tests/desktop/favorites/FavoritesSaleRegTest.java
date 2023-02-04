package ru.auto.tests.desktop.favorites;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FAVORITES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Избранное на карточке под зарегом")
@Feature(FAVORITES)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
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
    public String postMock;

    @Parameterized.Parameter(3)
    public String deleteMock;

    @Parameterized.Parameter(4)
    public String phonesMock;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object> getParameters() {
        return asList(new Object[][]{
                {CARS, "desktop/OfferCarsUsedUser", "desktop/UserFavoritesCarsPost",
                        "desktop/UserFavoritesCarsDelete", "desktop/OfferCarsPhones"},
                {TRUCK, "desktop/OfferTrucksUsedUser", "desktop/UserFavoritesTrucksPost",
                        "desktop/UserFavoritesTrucksDelete", "desktop/OfferTrucksPhones"},
                {MOTORCYCLE, "desktop/OfferMotoUsedUser", "desktop/UserFavoritesMotoPost",
                        "desktop/UserFavoritesMotoDelete", "desktop/OfferMotoPhones"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                saleMock,
                postMock,
                deleteMock,
                phonesMock).post();

        urlSteps.testing().path(category).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.setWideWindowSize();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Добавление в избранное на карточке")
    public void shouldAddToFavorites() {
        basePageSteps.onCardPage().cardHeader().toolBar().favoriteButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().cardHeader().toolBar().favoriteDeleteButton().waitUntil(isDisplayed());
        basePageSteps.onCardPage().notifier().waitUntil(isDisplayed()).should(hasText("В избранном 1 предложение" +
                "Перейти в избранное"));
        basePageSteps.onCardPage().header().favoritesButton().waitUntil(hasText("Избранное • 1"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Удаление из избранного на карточке")
    public void shouldDeleteFromFavorites() {
        basePageSteps.onCardPage().cardHeader().toolBar().favoriteButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().cardHeader().toolBar().favoriteDeleteButton().waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().notifier().waitUntil(isDisplayed()).should(hasText("Удалено из избранного"));
        basePageSteps.onCardPage().cardHeader().toolBar().favoriteButton().waitUntil(isDisplayed());
        basePageSteps.onCardPage().header().favoritesButton().waitUntil(hasText("Избранное"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Добавление в избранное в поп-апе контактов")
    public void shouldAddToFavoritesInContactsPopup() {
        basePageSteps.onCardPage().contacts().showPhoneButton().click();
        basePageSteps.onCardPage().contactsPopup().favoriteButton().click();
        basePageSteps.onListingPage().notifier().waitUntil(isDisplayed()).should(hasText("В избранном 1 предложение" +
                "Перейти в избранное"));
        basePageSteps.onCardPage().contactsPopup().favoriteDeleteButton().waitUntil(isDisplayed());
        basePageSteps.onCardPage().cardHeader().toolBar().favoriteDeleteButton().waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Удаление из избраннго в поп-апе контактов")
    public void shouldDeleteFromFavoritesInContactsPopup() {
        basePageSteps.onCardPage().contacts().showPhoneButton().click();
        basePageSteps.onCardPage().contactsPopup().favoriteButton().click();
        basePageSteps.onCardPage().contactsPopup().favoriteDeleteButton().waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().notifier().waitUntil(isDisplayed()).should(hasText("Удалено из избранного"));
        basePageSteps.onCardPage().cardHeader().toolBar().favoriteButton().waitUntil(isDisplayed());
    }
}
