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
import java.io.IOException;

import static ru.auto.tests.desktop.consts.AutoruFeatures.BANNERS;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.LCV;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.page.AdsPage.C3;
import static ru.auto.tests.desktop.page.AdsPage.R1;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Баннеры на карточке COMMERCIAL/NEW, широкий экран. Клик")
@Epic(BANNERS)
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class SaleCommercialNewClickTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    public BasePageSteps basePageSteps;

    @Before
    public void before() throws IOException {
        basePageSteps.setWideWindowSize();
        urlSteps.testing().path(MOSKVA).path(LCV).path(NEW).addParam("seller_group", "COMMERCIAL")
                .addParam("output_type", "list").open();
        urlSteps.open(basePageSteps.onListingPage().getNonPremiumSale(0).nameLink().getAttribute("href"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("TRUCKS/NEW, R1")
    public void shouldClickR1() {
        basePageSteps.onAdsPage().ad(R1).waitUntil(isDisplayed());
        basePageSteps.onAdsPage().ad(R1).hover().click();
        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("TRUCKS/NEW, C3")
    public void shouldClickC3() {
        basePageSteps.onCardPage().footer().hover();
        basePageSteps.onAdsPage().ad(C3).waitUntil(isDisplayed());
        basePageSteps.onAdsPage().ad(C3).hover().click();
        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }
}
