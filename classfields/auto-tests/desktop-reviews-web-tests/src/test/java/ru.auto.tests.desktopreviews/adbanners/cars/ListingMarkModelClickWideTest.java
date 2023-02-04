package ru.auto.tests.desktopreviews.adbanners.cars;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.BANNERS;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.auto.tests.desktop.page.AdsPage.C3;
import static ru.auto.tests.desktop.page.AdsPage.R1;
import static ru.auto.tests.desktop.page.AdsPage.REVIEWS_WIDE_SCREEN;

@DisplayName("Баннеры в листинге отзывов, «CARS»")
@Feature(BANNERS)
@Story(LISTING)
@GuiceModules(DesktopTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ListingMarkModelClickWideTest {

    private static final String MARK1 = "/hyundai/";
    private static final String MODEL1 = "/solaris/";
    private static final String MARK2 = "/mitsubishi/";
    private static final String MODEL2 = "/outlander/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public ScreenshotSteps screenshotSteps;

    @Parameterized.Parameter
    public String bannerNumber;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {C3},
                {R1}
        });
    }

    @Before
    public void before() {
        screenshotSteps.setWindowSize(REVIEWS_WIDE_SCREEN, 2000);
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class})
    @DisplayName("Широкий экран + Седан. Клик по банеру")
    public void shouldOpenBannerCarsMarkModelSedan() {
        urlSteps.testing().path(REVIEWS).path(CARS).path(MARK1).path(MODEL1).open();
        basePageSteps.onAdsPage().clickAtBanner(bannerNumber);

        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class})
    @DisplayName("Широкий экран + Внедорожник. Клик по банеру")
    public void shouldOpenBannerCarsMarkModelAllroad() {
        urlSteps.testing().path(REVIEWS).path(CARS).path(MARK2).path(MODEL2).open();
        basePageSteps.onAdsPage().clickAtBanner(bannerNumber);

        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }
}
