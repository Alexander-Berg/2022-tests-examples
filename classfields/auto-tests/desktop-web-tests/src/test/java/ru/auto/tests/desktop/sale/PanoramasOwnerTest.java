package ru.auto.tests.desktop.sale;

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
import org.openqa.selenium.Keys;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Карточка объявления - панорамы")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class PanoramasOwnerTest {

    private static final String SALE_ID = "/1076842087-f1e84/";
    private static final String SPOTS_PROMO_COOKIE = "panorama_hot_spots_promo_closed";
    private static final int SPOTS_COUNT = 2;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/OfferCarsUsedUserOwnerPanorama",
                "desktop/PanoramaPoi").post();

        cookieSteps.setCookieForBaseDomain(SPOTS_PROMO_COOKIE, "true");
        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Удаление точки")
    public void shouldDeleteSpot() {
        mockRule.with("desktop/PanoramaPoiDelete").update();

        waitSomething(5, TimeUnit.SECONDS);
        basePageSteps.onCardPage().gallery().panoramaExterior().getSpot(1).click();
        basePageSteps.onCardPage().popup().button("Удалить").click();
        basePageSteps.acceptAlert();
        basePageSteps.onCardPage().gallery().panoramaExterior().spotsList().waitUntil(hasSize(SPOTS_COUNT - 1));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Редактирование точки")
    public void shouldEditSpot() {
        mockRule.with("desktop/PanoramaPoiPut").update();

        waitSomething(5, TimeUnit.SECONDS);
        basePageSteps.onCardPage().gallery().panoramaExterior().getSpot(0).click();
        basePageSteps.onCardPage().popup().button("Редактировать").click();
        basePageSteps.onCardPage().gallery().panoramaExterior().spotEditPopup().input()
                .sendKeys(Keys.HOME, Keys.chord(Keys.SHIFT, Keys.END, Keys.DELETE));
        basePageSteps.onCardPage().gallery().panoramaExterior().spotEditPopup().input().sendKeys("1");
        basePageSteps.onCardPage().gallery().panoramaExterior().spotEditPopup().button("Сохранить").click();
        basePageSteps.onCardPage().notifier().waitUntil(not(isDisplayed()));
        basePageSteps.onCardPage().gallery().panoramaExterior().spotEditPopup().waitUntil(not(isDisplayed()));
        basePageSteps.onCardPage().gallery().panoramaExterior().spotsList().waitUntil(hasSize(SPOTS_COUNT));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Добавление точки")
    public void shouldAddSpot() {
        mockRule.with("desktop/PanoramaPoiPost").update();

        waitSomething(5, TimeUnit.SECONDS);
        basePageSteps.onCardPage().gallery().panoramaExterior().hover();
        basePageSteps.onCardPage().gallery().panoramaExterior().showHideSpotsButton().hover();
        basePageSteps.onCardPage().gallery().panoramaExterior().showHideSpotsButton().should(hasText("Добавить точку")).click();
        basePageSteps.moveCursorAndClick(basePageSteps.onCardPage().gallery().panoramaExterior(), 50, 50);
        basePageSteps.onCardPage().gallery().panoramaExterior().spotAddPopup().button("Добавить заметку").click();
        basePageSteps.onCardPage().gallery().panoramaExterior().spotEditPopup().input().sendKeys("1");
        basePageSteps.onCardPage().gallery().panoramaExterior().spotEditPopup().button("Сохранить").click();
        basePageSteps.onCardPage().notifier().waitUntil(not(isDisplayed()));
        basePageSteps.onCardPage().gallery().panoramaExterior().spotEditPopup().waitUntil(not(isDisplayed()));
        basePageSteps.onCardPage().gallery().panoramaExterior().spotsList().waitUntil(hasSize(SPOTS_COUNT + 1));
    }
}
