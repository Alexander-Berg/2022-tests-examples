package ru.auto.tests.desktop.favorites;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
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

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.MAIN;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Главная - блок «Избранное»")
@Feature(MAIN)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class FavoritesMainTest {

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
                "desktop/SearchCarsBreadcrumbsEmpty",
                "desktop/UserFavoritesAllPageSize15").post();

        urlSteps.testing().open();
        basePageSteps.onMainPage().favorites().hover();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение блока")
    public void shouldSeeFavorites() {
        basePageSteps.onMainPage().favorites().itemsList().should(hasSize(4))
                .forEach(item -> item.should(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по объявлению авто")
    public void shouldClickCarsSale() {
        mockRule.with("desktop/OfferCarsUsedUser").update();

        basePageSteps.onMainPage().favorites().getItem(0).click();
        urlSteps.testing().path(CARS).path(USED).path(SALE).path("/land_rover/discovery/1076842087-f1e84/")
                .addParam("from", "favorites").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по объявлению мото")
    public void shouldClickMotoSale() {
        mockRule.with("desktop/OfferMotoUsedUser").update();

        basePageSteps.onMainPage().favorites().getItem(1).click();
        urlSteps.testing().path(MOTORCYCLE).path(USED).path(SALE)
                .path("/harley_davidson/dyna_super_glide/1076842087-f1e84/").addParam("from", "favorites")
                .shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по объявлению комТС")
    public void shouldClickTrucksSale() {
        mockRule.with("desktop/OfferTrucksUsedUser").update();

        basePageSteps.onMainPage().favorites().getItem(2).click();
        urlSteps.testing().path(TRUCK).path(USED).path(SALE).path("/zil/5301/1076842087-f1e84/")
                .addParam("from", "favorites").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Смотреть все избранные»")
    public void shouldClickShowAllButton() {
        basePageSteps.onMainPage().favorites().button("Смотреть все избранные").click();
        basePageSteps.onMainPage().favoritesPopup().waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Возврат на главную после клика по объявлению")
    public void shouldReturnToMain() {
        mockRule.with("desktop/OfferCarsUsedUser").update();

        basePageSteps.onMainPage().favorites().getItem(0).click();
        urlSteps.testing().path(CARS).path(USED).path(SALE).path("/land_rover/discovery/1076842087-f1e84/")
                .addParam("from", "favorites").shouldNotSeeDiff();
        basePageSteps.driver().navigate().back();
        urlSteps.testing().path("/").shouldNotSeeDiff();
    }
}