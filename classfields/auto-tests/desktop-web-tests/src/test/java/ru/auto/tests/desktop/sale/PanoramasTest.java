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
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Карточка объявления - панорамы")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class PanoramasTest {

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
        mockRule.newMock().with("desktop/OfferCarsUsedUserPanorama",
                "desktop/PanoramaPoi").post();

        cookieSteps.setCookieForBaseDomain("panorama_hot_spots_promo_closed", "true");
        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение промо")
    public void shouldSeeSpotsPromo() {
        cookieSteps.deleteCookie(SPOTS_PROMO_COOKIE);
        urlSteps.open();
        basePageSteps.onCardPage().gallery().panoramaExterior().spotsPromo().waitUntil(isDisplayed())
                .should(hasText("Точки на панорамах\nМы добавили новую возможность! Крутите панораму, " +
                        "нажимайте на точки — продавец выделил всё самое интересное\nКруто, спасибо"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Закрытие промо точек")
    public void shouldCloseSpotsPromo() {
        cookieSteps.deleteCookie(SPOTS_PROMO_COOKIE);
        urlSteps.open();
        basePageSteps.onCardPage().gallery().panoramaExterior().spotsPromo().waitUntil(isDisplayed());
        basePageSteps.onCardPage().gallery().panoramaExterior().spotsPromo().button("Круто, спасибо").click();
        basePageSteps.onCardPage().gallery().panoramaExterior().spotsPromo().waitUntil(not(isDisplayed()));
        cookieSteps.shouldSeeCookieWithValue(SPOTS_PROMO_COOKIE, "true");
        urlSteps.refresh();
        basePageSteps.onCardPage().gallery().panoramaExterior().spotsPromo().waitUntil(not(isDisplayed()));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Скрытие/показ точек")
    public void shouldHideAndShowSpots() {
        waitSomething(5, TimeUnit.SECONDS);
        basePageSteps.onCardPage().gallery().panoramaExterior().showHideSpotsButton().hover();
        basePageSteps.onCardPage().gallery().panoramaExterior().showHideSpotsButton().should(hasText("Скрыть точки")).click();
        basePageSteps.onCardPage().gallery().panoramaExterior().spotsList().waitUntil(hasSize(0));

        basePageSteps.onCardPage().gallery().panoramaExterior().showHideSpotsButton().hover();
        basePageSteps.onCardPage().gallery().panoramaExterior().showHideSpotsButton().should(hasText("Показать точки")).click();
        basePageSteps.onCardPage().gallery().panoramaExterior().spotsList().waitUntil(hasSize(SPOTS_COUNT));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение точек")
    public void shouldSeeSpots() {
        waitSomething(5, TimeUnit.SECONDS);

        basePageSteps.onCardPage().gallery().panoramaExterior().getSpot(0).hover();
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed()).should(hasText("AMG обвес"));
        basePageSteps.onCardPage().popup().click();

        basePageSteps.onCardPage().gallery().panoramaExterior().getSpot(1).hover();
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed()).should(hasText("Двухцветная отделка салона, " +
                "отделка сидений - натуральная кожа"));

        basePageSteps.onCardPage().gallery().panoramaExterior().getSpot(1).click();
        basePageSteps.onCardPage().gallery().panoramaExterior().spotPhotoPopup().waitUntil(isDisplayed())
                .should(hasText("Двухцветная отделка салона, отделка сидений - натуральная кожа"));
        basePageSteps.onCardPage().gallery().panoramaExterior().spotPhotoPopup().photo()
                .waitUntil(hasAttribute("src",
                        "https://avatars.mds.yandex.net/get-autoru-panorama/1184659/1IjRAQKvRLSV9J7me6KzfUdCExyTp4dtY/1200x900"));
    }
}
