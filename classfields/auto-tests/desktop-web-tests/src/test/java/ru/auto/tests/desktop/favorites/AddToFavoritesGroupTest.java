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

import static java.lang.String.format;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FAVORITES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Избранное на группе")
@Feature(FAVORITES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class AddToFavoritesGroupTest {

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
                "desktop/UserFavoritesAllNewOffer",
                "desktop/SearchCarsGroupContextGroup",
                "desktop/SearchCarsGroupContextListing",
                "desktop/SearchCarsBreadcrumbsMarkModelGroup",
                "desktop/SearchCarsGroupComplectations",
                "desktop/ReferenceCatalogCarsComplectations",
                "desktop/ReferenceCatalogCarsDictionariesV1Equipment",
                "desktop/ReferenceCatalogCarsConfigurationsSubtree",
                "desktop/ReferenceCatalogCarsTechParam").post();

        urlSteps.testing().path(CARS).path(NEW).path(GROUP).path(PATH).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Добавление в избранное")
    public void shouldAddToFavorites() {
        basePageSteps.onGroupPage().getOffer(0).addToFavoritesIcon().should(isDisplayed()).hover();
        basePageSteps.scrollDown(300);
        basePageSteps.onGroupPage().getOffer(0).addToFavoritesIcon().click();

        basePageSteps.onGroupPage().getOffer(0).deleteFromFavoritesIcon().waitUntil(isDisplayed());
        basePageSteps.onGroupPage().notifier().waitUntil(isDisplayed()).should(hasText("В избранном 1 предложение" +
                "Перейти в избранное"));
        basePageSteps.onGroupPage().notifier().waitUntil("Не исчезла плашка", not(isDisplayed()), 6);

        basePageSteps.onGroupPage().header().favoritesButton().click();
        basePageSteps.onGroupPage().favoritesPopup().waitUntil(isDisplayed());
        basePageSteps.onGroupPage().favoritesPopup().favoritesList().waitUntil(hasSize(1));
        String saleUrl = format("%s/cars/new/group/kia/optima/21342125/21342344/1088375974-3a85dc46/",
                urlSteps.getConfig().getTestingURI());
        basePageSteps.onBasePage().favoritesPopup().getFavorite(0).link()
                .should(hasAttribute("href", saleUrl));
    }
}