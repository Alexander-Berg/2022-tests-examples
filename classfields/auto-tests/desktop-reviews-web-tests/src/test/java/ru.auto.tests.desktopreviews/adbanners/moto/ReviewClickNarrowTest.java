package ru.auto.tests.desktopreviews.adbanners.moto;

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
import static ru.auto.tests.desktop.consts.AutoruFeatures.REVIEWS;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.REVIEW;
import static ru.auto.tests.desktop.page.AdsPage.C3;
import static ru.auto.tests.desktop.page.AdsPage.REVIEWS_NARROW_WINDOW;

@DisplayName("Баннеры в листинге отзывов, «MOTO»")
@Feature(BANNERS)
@Story(REVIEWS)
@GuiceModules(DesktopTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ReviewClickNarrowTest {

    private static final String CATEGORY1 = "/motorcycle/";
    private static final String MARK1 = "/honda/";
    private static final String MODEL1 = "/cb_400/";
    private static final String REVIEW1 = "/4022946/";

    private static final String CATEGORY2 = "/scooters/";
    private static final String MARK2 = "/yamaha/";
    private static final String MODEL2 = "/majesty_250/";
    private static final String REVIEW2 = "/4700810276996412510/";


    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    public BasePageSteps basePageSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String mark;

    @Parameterized.Parameter(2)
    public String model;

    @Parameterized.Parameter(3)
    public String review;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {CATEGORY1, MARK1, MODEL1, REVIEW1}, //мотоцикл
                {CATEGORY2, MARK2, MODEL2, REVIEW2}  //скутер
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
    public void shouldOpenBannerMotoReview() {
        urlSteps.testing().path(REVIEW).path(MOTO).path(category).path(mark).path(model).path(review).open();
        basePageSteps.onAdsPage().clickAtBanner(C3);

        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }
}
