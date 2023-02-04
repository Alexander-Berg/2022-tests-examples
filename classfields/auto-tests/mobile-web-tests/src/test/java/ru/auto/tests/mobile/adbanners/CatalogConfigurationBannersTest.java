package ru.auto.tests.mobile.adbanners;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.greaterThan;
import static ru.auto.tests.desktop.consts.AutoruFeatures.BANNERS;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.page.AdsPage.C2;
import static ru.auto.tests.desktop.page.AdsPage.TOP_MOBILE;

@DisplayName("Баннеры в каталоге")
@Feature(BANNERS)
@GuiceModules(MobileEmulationTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class CatalogConfigurationBannersTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    public BasePageSteps basePageSteps;

    @Before
    public void before() {
        urlSteps.testing().path(CATALOG).path(CARS).path("bmw").path("5er").path("22212692").path("22213464").open();
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class})
    @DisplayName("Клик по баннеру ТОП на странице кузова в каталоге")
    public void shouldClickBannerConfigTop() {
        basePageSteps.onAdsPage().shouldSeeAds(greaterThan(0));
        basePageSteps.scrollAndClick(basePageSteps.onAdsPage().ad(TOP_MOBILE));
        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class})
    @DisplayName("Клик по баннеру С2 на странице кузова в каталоге")
    public void shouldClickBannerConfigC2() {
        basePageSteps.onAdsPage().shouldSeeAds(greaterThan(0));
        basePageSteps.scrollAndClick(basePageSteps.onAdsPage().ad(C2));
        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }

}
