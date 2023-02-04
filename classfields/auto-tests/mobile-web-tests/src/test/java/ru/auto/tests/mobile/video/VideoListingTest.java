package ru.auto.tests.mobile.video;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(AutoruFeatures.VIDEO)
@Ignore //TODO добавить тикет в st
@DisplayName("Блок видео")
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class VideoListingTest {

    private static final int VIDEO_BAR_POSITION = 16;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String breadcrumbsMock;

    @Parameterized.Parameter(2)
    public String listingMock;

    @Parameterized.Parameter(3)
    public String videoMock;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, "desktop/SearchCarsBreadcrumbsEmpty", "mobile/SearchCarsAll", "mobile/VideoSearchCars"},
                {TRUCK, "desktop/SearchTrucksBreadcrumbsEmpty", "mobile/SearchTrucksAll", "mobile/VideoSearchTrucks"},
                {MOTORCYCLE, "desktop/SearchMotoBreadcrumbsEmpty", "mobile/SearchMotoAll", "mobile/VideoSearchMoto"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with(breadcrumbsMock,
                listingMock,
                videoMock).post();

        urlSteps.testing().path(category).path(ALL).open();
        basePageSteps.onListingPage().getSale(VIDEO_BAR_POSITION).hover();
        basePageSteps.onListingPage().videos().waitUntil(isDisplayed()).hover();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение блока «Популярные видео»")
    public void shouldSeePopularVideos() {
        basePageSteps.onListingPage().videos().videosList().should(hasSize(greaterThan(0)))
                .forEach(item -> item.should(isDisplayed()));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по видео")
    public void shouldClickVideo() {
        basePageSteps.onListingPage().videos().getVideo(0).waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().videoFrame().waitUntil(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Закрытие видео")
    public void shouldCloseVideo() {
        basePageSteps.onListingPage().videos().getVideo(0).waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().videoCloseIcon().waitUntil(isDisplayed());
    }
}
