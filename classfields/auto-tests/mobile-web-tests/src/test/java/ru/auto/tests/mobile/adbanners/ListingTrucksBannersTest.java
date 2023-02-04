package ru.auto.tests.mobile.adbanners;

import com.carlosbecker.guice.GuiceModules;
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
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.BANNERS;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.LCV;
import static ru.auto.tests.desktop.page.AdsPage.LISTING1;
import static ru.auto.tests.desktop.page.AdsPage.LISTING2;
import static ru.auto.tests.desktop.page.AdsPage.LISTING3;
import static ru.auto.tests.desktop.page.AdsPage.LISTING4;

@DisplayName("Баннеры в листинге trucks")
@Feature(BANNERS)
@GuiceModules(MobileTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ListingTrucksBannersTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    public BasePageSteps basePageSteps;

    @Parameterized.Parameter
    public String banner;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {LISTING1},
                {LISTING2},
                {LISTING3},
                {LISTING4}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(LCV).path(ALL).open();
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class})
    @DisplayName("Клик по баннеру на первой странице")
    public void shouldClickBannerFirstPage() {
        int saleIndexCloseToFirstPageEnd = basePageSteps.onListingPage().salesList().size() - 5;
        basePageSteps.onListingPage().salesList().get(saleIndexCloseToFirstPageEnd).hover();
        basePageSteps.onAdsPage().shouldSeeAds(greaterThan(0));

        basePageSteps.onAdsPage().ad(banner).hover().click();

        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class})
    @DisplayName("Клик по баннеру на второй странице")
    public void shouldClickBannerSecondPage() {
        int salesCountOnFirstPage = basePageSteps.onListingPage().salesList().size();
        basePageSteps.onListingPage().salesList().get(salesCountOnFirstPage - 1).hover();
        basePageSteps.onListingPage().salesList().waitUntil(hasSize(greaterThan(salesCountOnFirstPage)));

        int saleIndexCloseToSecondPageEnd = basePageSteps.onListingPage().salesList().size() - 5;
        basePageSteps.onListingPage().salesList().get(saleIndexCloseToSecondPageEnd).hover();

        basePageSteps.onAdsPage().adPage2(banner).hover().click();

        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }
}
