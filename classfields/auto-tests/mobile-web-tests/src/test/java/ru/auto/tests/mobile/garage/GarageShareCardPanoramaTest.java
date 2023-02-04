package ru.auto.tests.mobile.garage;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
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
import ru.auto.tests.desktop.DesktopConfig;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.GARAGE;
import static ru.auto.tests.desktop.consts.Pages.SHARE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Гараж")
@Story("Публичная карточка гаража")
@Feature(AutoruFeatures.GARAGE)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class GarageShareCardPanoramaTest {

    private static final String CARD_ID = "/1038497530/";
    private static final String HOT_SPOT_COOKIE_NAME = "panorama_hot_spots_promo_closed";
    private static final String IMAGE_PATH = "get-autoru-panorama/1184659/1IjRAQKvRLSV9J7me6KzfUdCExyTp4dtY/1200x900";

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

    @Inject
    private DesktopConfig config;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/GarageUserCardsVinPost",
                "desktop/GarageCardPanorama",
                "desktop/PanoramaPoi").post();

        cookieSteps.setCookieForBaseDomain(HOT_SPOT_COOKIE_NAME, "true");
        urlSteps.testing().path(GARAGE).path(SHARE).path(CARD_ID).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Открываем полноэкранную галерею")
    public void shouldOpenGallery() {
        basePageSteps.onGarageCardPage().panorama().should(isDisplayed());
        basePageSteps.onGarageCardPage().panorama().click();
        basePageSteps.onGarageCardPage().fullScreenGallery().should(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Закрываем полноэкранную галерею")
    public void shouldCloseFullscreenGallery() {
        basePageSteps.onGarageCardPage().panorama().should(isDisplayed());
        basePageSteps.onGarageCardPage().panorama().click();
        basePageSteps.onGarageCardPage().fullScreenGallery().should(isDisplayed());
        basePageSteps.onGarageCardPage().fullScreenGallery().closeButton().click();
        basePageSteps.onGarageCardPage().fullScreenGallery().should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Отображение промо точек")
    public void shouldSeeSpotsPromo() {
        cookieSteps.deleteCookie(HOT_SPOT_COOKIE_NAME);
        urlSteps.open();

        basePageSteps.onGarageCardPage().panorama().click();
        basePageSteps.onGarageCardPage().panorama().spotsPromo().waitUntil(isDisplayed())
                .should(hasText("Точки на панорамах\nМы добавили новую возможность! Крутите панораму, " +
                        "нажимайте на точки — владалец выделил всё самое интересное\nКруто, спасибо"));

        basePageSteps.onGarageCardPage().panorama().spotsPromo().button("Круто, спасибо").click();
        basePageSteps.onGarageCardPage().panorama().spotsPromo().waitUntil(not(isDisplayed()));
        cookieSteps.shouldSeeCookieWithValue(HOT_SPOT_COOKIE_NAME, "true");
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение точек")
    public void shouldSeeSpots() {
        waitSomething(5, TimeUnit.SECONDS);

        basePageSteps.onGarageCardPage().panorama().click();
        basePageSteps.onGarageCardPage().panorama().getSpot(1).click();
        basePageSteps.onGarageCardPage().panorama().spotText().waitUntil(isDisplayed()).should(hasText("AMG обвес"));
        basePageSteps.onGarageCardPage().panorama().spotCloseIcon().click();

        basePageSteps.onGarageCardPage().panorama().getSpot(7).click();
        basePageSteps.onGarageCardPage().panorama().spotText().waitUntil(isDisplayed())
                .should(hasText("Двухцветная отделка салона, отделка сидений - натуральная кожа"));
        basePageSteps.onGarageCardPage().panorama().spotPhoto().waitUntil(hasAttribute("src",
                format("%s/%s", config.getAvatarsURI(), IMAGE_PATH)));
    }
}
