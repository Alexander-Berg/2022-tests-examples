package ru.auto.tests.desktopreviews.adbanners.trucks;

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
import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.page.AdsPage.C3;
import static ru.auto.tests.desktop.page.AdsPage.REVIEWS_NARROW_WINDOW;

@DisplayName("Баннеры в листинге отзывов, «TRUCKS»")
@Feature(BANNERS)
@Story(LISTING)
@GuiceModules(DesktopTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ListingMarkModelClickNarrowTest {

    private static final String CATEGORY1 = "/truck/";
    private static final String MARK1 = "/hyundai/";
    private static final String MODEL1 = "/hd78/";

    private static final String CATEGORY2 = "/lcv/";
    private static final String MARK2 = "/citroen/";
    private static final String MODEL2 = "/berlingo/";
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

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {CATEGORY1, MARK1, MODEL1}, //грузовик
                {CATEGORY2, MARK2, MODEL2}  //ЛТС
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
    public void shouldOpenBannerTrucksMarkModel() {
        urlSteps.testing().path(REVIEWS).path(TRUCKS).path(category).path(mark).path(model).open();
        basePageSteps.onAdsPage().clickAtBanner(C3);

        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }
}
