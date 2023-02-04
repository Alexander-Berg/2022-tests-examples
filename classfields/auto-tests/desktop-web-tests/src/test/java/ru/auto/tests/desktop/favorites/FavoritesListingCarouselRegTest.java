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
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FAVORITES;
import static ru.auto.tests.desktop.consts.Notifications.DELETED_FROM_FAV;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.QueryParams.CAROUSEL;
import static ru.auto.tests.desktop.consts.QueryParams.OUTPUT_TYPE;
import static ru.auto.tests.desktop.mock.MockStub.sessionAuthUserStub;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Избранное в листинге под зарегом")
@Feature(FAVORITES)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class FavoritesListingCarouselRegTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String searchMock;

    @Parameterized.Parameter(2)
    public String breadcrumbsMock;

    @Parameterized.Parameter(3)
    public String favoritesPostMock;

    @Parameterized.Parameter(4)
    public String favoritesDeleteMock;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object> getParameters() {
        return asList(new Object[][]{
                {CARS, "desktop/SearchCarsAll", "desktop/SearchCarsBreadcrumbsEmpty",
                        "desktop/UserFavoritesCarsPost", "desktop/UserFavoritesCarsDelete"},
                {TRUCK, "desktop/SearchTrucksAll", "desktop/SearchTrucksBreadcrumbs",
                        "desktop/UserFavoritesTrucksPost", "desktop/UserFavoritesTrucksDelete"},
                {MOTORCYCLE, "desktop/SearchMotoAll", "desktop/SearchMotoBreadcrumbs",
                        "desktop/UserFavoritesMotoPost", "desktop/UserFavoritesMotoDelete"}
        });
    }

    @Before
    public void before() {
        mockRule.setStubs(
                sessionAuthUserStub(),
                stub(searchMock),
                stub(breadcrumbsMock),
                stub(favoritesPostMock),
                stub(favoritesDeleteMock)
        ).create();

        urlSteps.testing().path(MOSKVA).path(category).path(ALL).addParam(OUTPUT_TYPE, CAROUSEL).open();
        basePageSteps.onListingPage().getCarouselSale(0).waitUntil(isDisplayed()).hover();
        basePageSteps.onListingPage().getCarouselSale(0).toolBar().favoriteButton().waitUntil(isDisplayed()).click();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Добавление в избранное, тип листинга «Карусель»")
    public void shouldAddToFavorites() {
        basePageSteps.onListingPage().notifier().waitUntil(isDisplayed())
                .should(hasText("В избранном 1 предложениеПерейти в избранное"));
        basePageSteps.onListingPage().getCarouselSale(0).toolBar().favoriteDeleteButton().should(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Удаление из избранного, тип листинга «Карусель»")
    public void shouldDeleteFromFavorites() {
        basePageSteps.onListingPage().getCarouselSale(0).toolBar().favoriteDeleteButton()
                .waitUntil(isDisplayed()).hover().click();

        basePageSteps.onListingPage().notifier(DELETED_FROM_FAV).should(isDisplayed());
        basePageSteps.onListingPage().getCarouselSale(0).toolBar().favoriteButton().should(isDisplayed());
    }

}
