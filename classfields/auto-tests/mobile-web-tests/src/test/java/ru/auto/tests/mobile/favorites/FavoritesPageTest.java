package ru.auto.tests.mobile.favorites;

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
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.UseParametersRunnerFactory;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.Notifications.DELETED_FROM_FAV;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.LIKE;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Страница избранного")
@Feature(AutoruFeatures.FAVORITES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class FavoritesPageTest {

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
    private ScreenshotSteps screenshotSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "mobile/UserFavoritesAll",
                "desktop/UserFavoritesCarsDelete",
                "desktop/OfferCarsCallsStats").post();

        urlSteps.testing().path(LIKE).open();
    }

    @Test
    @Category({Regression.class, Screenshooter.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение избранного")
    public void shouldSeeFavorites() {
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onFavoritesPage().getSale(0));

        urlSteps.setProduction().open();
        basePageSteps.hideElement(basePageSteps.onFavoritesPage().devToolsBranch());
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onFavoritesPage().getSale(0));

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по объявлению в избранном")
    public void shouldClickSale() {
        mockRule.with("desktop/OfferCarsUsedUser").update();

        basePageSteps.onFavoritesPage().getSale(0).title().click();
        urlSteps.testing().path(CARS).path(USED).path(SALE).path("/land_rover/discovery/").path(SALE_ID)
                .shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Удаление из избранного")
    public void shouldDeleteFavorite() {
        basePageSteps.onFavoritesPage().getSale(0).deleteFromFavoritesIcon().click();
        basePageSteps.onFavoritesPage().notifier().waitUntil(isDisplayed()).should(hasText(DELETED_FROM_FAV));
        basePageSteps.onFavoritesPage().salesList().waitUntil(hasSize(0));
        basePageSteps.onFavoritesPage().stub().waitUntil(isDisplayed()).should(hasText("Сохраняйте понравившиеся " +
                "объявления, узнавайте об изменении цен."));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Позвонить»")
    public void shouldClickCallButton() {
        mockRule.with("desktop/OfferCarsPhones").update();

        basePageSteps.onFavoritesPage().getSale(0).callButton().click();
        basePageSteps.onFavoritesPage().popup().should(hasText("Телефон\n+7 916 039-84-27\nс 10:00 до 23:00\n" +
                "+7 916 039-84-28\nс 12:00 до 20:00"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Смотреть отчёт»")
    public void shouldClickShowVinReportButton() {
        basePageSteps.onFavoritesPage().getSale(0).showVinReportButton().click();
        urlSteps.testing().path(CARS).path(USED).path(SALE).path("/land_rover/discovery/").path(SALE_ID)
                .addParam("action", "showVinReport")
                .addParam("vinReportButtonFrom", "api_m_favorites").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение статистики звонков")
    public void shouldSeeCallsStats() {
        basePageSteps.onFavoritesPage().getSale(0).callsStats().waitUntil(isDisplayed())
                .should(hasText("10 звонков за 24 часа"));
    }
}
