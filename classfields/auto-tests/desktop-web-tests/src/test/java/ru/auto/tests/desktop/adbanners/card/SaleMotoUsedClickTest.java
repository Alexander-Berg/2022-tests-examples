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
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.BANNERS;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.page.AdsPage.C3;
import static ru.auto.tests.desktop.page.AdsPage.R1;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Баннеры на карточке MOTO/USED, широкий экран. Клик")
@Epic(BANNERS)
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class SaleMotoUsedClickTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    public BasePageSteps basePageSteps;

    @Before
    public void before() {
        basePageSteps.setWideWindowSize();
        urlSteps.testing().path(MOSKVA).path(MOTORCYCLE).path(USED).addParam("seller_group", "PRIVATE")
                .addParam("output_type", "list").open();
        urlSteps.open(basePageSteps.onListingPage().getNonPremiumSale(0).nameLink().getAttribute("href"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("MOTO/NEW, R1")
    public void shouldClickR1() {
        basePageSteps.onAdsPage().ad(R1).waitUntil(isDisplayed());
        basePageSteps.onAdsPage().ad(R1).hover().click();
        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("MOTO/NEW, C3")
    public void shouldClickC3() {
        await().ignoreExceptions().atMost(30, SECONDS).pollInterval(5, SECONDS)
                .until(() -> {
                    urlSteps.refresh();
                    basePageSteps.onCardPage().footer().hover();
                    waitSomething(3, TimeUnit.SECONDS);
                    return basePageSteps.onAdsPage().ad(C3).isDisplayed();
                });

        basePageSteps.onAdsPage().ad(C3).hover().click();
        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }
}
