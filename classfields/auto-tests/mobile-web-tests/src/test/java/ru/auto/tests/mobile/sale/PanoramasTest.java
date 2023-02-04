package ru.auto.tests.mobile.sale;

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
import ru.auto.tests.desktop.DesktopConfig;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Карточка объявления - панорамы")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileTestsModule.class)
public class PanoramasTest {

    private static final String SALE_ID = "/1076842087-f1e84/";
    private static final String IMAGE_PATH = "get-autoru-vos/2049300/ed8e35bbc427dbd89c58e28c7ab75c40/1200x900n";
    private static final String SPOTS_PROMO_COOKIE = "panorama_hot_spots_promo_closed";

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
        mockRule.newMock().with("desktop/OfferCarsUsedUserPanorama",
                "desktop/PanoramaPoi").post();

        cookieSteps.setCookieForBaseDomain("panorama_hot_spots_promo_closed", "true");
        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Закрытие промо точек")
    public void shouldCloseSpotsPromo() {
        cookieSteps.deleteCookie(SPOTS_PROMO_COOKIE);
        urlSteps.refresh();

        basePageSteps.onCardPage().gallery().panorama().click();
        basePageSteps.onCardPage().gallery().panorama().spotsPromo().waitUntil(isDisplayed())
                .should(hasText("Точки на панорамах\nМы добавили новую возможность! Крутите панораму, " +
                        "нажимайте на точки — продавец выделил всё самое интересное\nКруто, спасибо"));

        basePageSteps.onCardPage().gallery().panorama().spotsPromo().button("Круто, спасибо").click();
        basePageSteps.onCardPage().gallery().panorama().spotsPromo().waitUntil(not(isDisplayed()));
        cookieSteps.shouldSeeCookieWithValue(SPOTS_PROMO_COOKIE, "true");
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Скрытие/показ точек")
    public void shouldHideAndShowSpots() {
        basePageSteps.onCardPage().gallery().panorama().click();
        basePageSteps.moveCursorAndClick(basePageSteps.onCardPage().fullScreenGallery().panorama(), 200, 200);
        basePageSteps.onCardPage().fullScreenGallery().panorama().showHideSpotsButton().click();
        basePageSteps.onCardPage().fullScreenGallery().panorama().spotsList().should(hasSize(0));

        basePageSteps.onCardPage().fullScreenGallery().panorama().showHideSpotsButton().click();
        basePageSteps.onCardPage().fullScreenGallery().panorama().spotsList().should(hasSize(9));
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение точек")
    public void shouldSeeSpots() {
        basePageSteps.onCardPage().gallery().panorama().click();
        basePageSteps.onCardPage().gallery().panorama().getSpot(1).click();
        basePageSteps.onCardPage().gallery().panorama().spotText().waitUntil(isDisplayed()).should(hasText("AMG обвес"));
        basePageSteps.onCardPage().gallery().panorama().spotCloseIcon().click();

        basePageSteps.onCardPage().gallery().panorama().getSpot(7).click();
        basePageSteps.onCardPage().gallery().panorama().spotText().waitUntil(isDisplayed())
                .should(hasText("Двухцветная отделка салона, отделка сидений - натуральная кожа"));
        basePageSteps.onCardPage().gallery().panorama().spotPhoto().waitUntil(hasAttribute("src",
                format("%s/%s", config.getAvatarsURI(), IMAGE_PATH)));
    }
}
