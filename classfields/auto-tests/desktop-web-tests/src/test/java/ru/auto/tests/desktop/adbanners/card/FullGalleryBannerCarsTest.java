package ru.auto.tests.desktop.adbanners.card;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Epic;
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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.BANNERS;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.QueryParams.OUTPUT_TYPE;
import static ru.auto.tests.desktop.consts.QueryParams.SELLER_GROUP;
import static ru.auto.tests.desktop.consts.QueryParams.TABLE;
import static ru.auto.tests.desktop.consts.WindowSize.HEIGHT_1024;
import static ru.auto.tests.desktop.consts.WindowSize.WIDTH_1920;
import static ru.auto.tests.desktop.page.AdsPage.GALLERY;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Баннеры на карточке в полной галерее")
@Epic(BANNERS)
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class FullGalleryBannerCarsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    public BasePageSteps basePageSteps;

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(CARS).path(USED).addParam(SELLER_GROUP, "PRIVATE")
                .addParam(OUTPUT_TYPE, TABLE).open();
        basePageSteps.onListingPage().getSale(0).hover().click();
        basePageSteps.switchToNextTab();
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class})
    @DisplayName("Карточка. Баннер в полной галерее")
    public void shouldClickFullGalleryBanner() {
        basePageSteps.onCardPage().gallery().waitUntil(isDisplayed()).hover().click();
        basePageSteps.onCardPage().fullScreenGallery().waitUntil(isDisplayed());
        basePageSteps.setWindowSize(WIDTH_1920, HEIGHT_1024);
        basePageSteps.scrollDown(200); // на панорамах баннера нет
        basePageSteps.onAdsPage().ad(GALLERY).waitUntil(isDisplayed(), 10).click();

        urlSteps.shouldSeeCertainNumberOfTabs(3);
    }
}
