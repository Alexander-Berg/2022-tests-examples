package ru.auto.tests.mobile.listing;

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
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.RUSSIA;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;

@DisplayName("Листинг - сниппет при поиске по недефолтным параметрам")
@Feature(LISTING)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SnippetNotDefaultParamsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String searchParam;

    @Parameterized.Parameter(2)
    public String searchParamValue;

    @Parameterized.Parameters(name = "name = {index}: {0} {1} {2}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, "beaten", "2"},
                {CARS, "customs_state", "2"},
                {CARS, "steering_wheel", "RIGHT"},

                {TRUCK, "beaten", "2"},
                {TRUCK, "custom_state_key", "NOT_CLEARED"},
                {TRUCK, "steering_wheel", "RIGHT"},

                {MOTORCYCLE, "beaten", "2"},
                {MOTORCYCLE, "custom_state_key", "NOT_CLEARED"}
        });
    }

    @Before
    public void before() {
        basePageSteps.setWindowHeight(5000);
        urlSteps.testing().path(RUSSIA).path(category).path(ALL).addParam("sort_offers", "price-DESC")
                .addParam(searchParam, searchParamValue).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Screenshooter.class})
    @DisplayName("Отображение сниппета")
    public void shouldSeeSnippet() {
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onListingPage().getSale(0));

        urlSteps.setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onListingPage().getSale(0));

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }
}
