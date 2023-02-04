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
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.BANNERS;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.REVIEW;
import static ru.auto.tests.desktop.page.AdsPage.C3;
import static ru.auto.tests.desktop.page.AdsPage.REVIEWS_NARROW_WINDOW;

@DisplayName("Баннеры в листинге отзывов, «CARS»")
@Feature(BANNERS)
@Story(REVIEW)
@GuiceModules(DesktopTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ReviewClickNarrowTest {

    private static final String MARK1 = "/hyundai/";
    private static final String MODEL1 = "/solaris/";
    private static final String REVIEW_1 = "/20922677/1065312859959992809/";
    private static final String MARK2 = "/mitsubishi/";
    private static final String MODEL2 = "/outlander/";
    private static final String REVIEW_2 = "/2308026/4037054/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    public BasePageSteps basePageSteps;

    @Parameterized.Parameter
    public String mark;

    @Parameterized.Parameter(1)
    public String model;

    @Parameterized.Parameter(2)
    public String review;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {MARK1, MODEL1, REVIEW_1}, //седан
                {MARK2, MODEL2, REVIEW_2}  //внедородник
        });
    }

    @Before
    public void before() {
        urlSteps.setWindowSize(REVIEWS_NARROW_WINDOW, 2000);
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class})
    @DisplayName("Узкий экран. Клик по банеру")
    public void shouldOpenBannerCarsReview() {
        urlSteps.testing().path(REVIEW).path(CARS).path(mark).path(model).path(review).open();
        basePageSteps.onAdsPage().clickAtBanner(C3);

        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }
}
