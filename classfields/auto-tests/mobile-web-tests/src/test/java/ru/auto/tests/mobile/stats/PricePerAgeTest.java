package ru.auto.tests.mobile.stats;

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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;
import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.STATS;
import static ru.auto.tests.desktop.utils.Utils.getCurrentYear;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(AutoruFeatures.STATS)
@DisplayName("Статистика - как дешевеет авто с возрастом")
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class PricePerAgeTest {

    private static final int CURRENT_YEAR = getCurrentYear();

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

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Parameterized.Parameter
    public String url;

    @Parameterized.Parameter(1)
    public String listingUrl;

    @Parameterized.Parameters(name = "name = {index}: {0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"/audi/a3/", format("/moskva/cars/audi/a3/%s-year/all/", CURRENT_YEAR)},
                {"/audi/a3/20785010/", format("/moskva/cars/audi/a3/%s-year/20785010/all/", CURRENT_YEAR)},
                {"/audi/a3/20785010/20785541/", format("/moskva/cars/audi/a3/%s-year/20785010/20785541/all/",
                        CURRENT_YEAR)},
                {"/hyundai/solaris/6847474/6847477/6847477_8518368_6847481/",
                        format("/moskva/cars/hyundai/solaris/%s-year/6847474/6847477/6847481/all/", CURRENT_YEAR)}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionUnauth",
                "desktop/StatsSummaryModification",
                "desktop/StatsSummaryBody",
                "desktop/StatsSummaryGeneration",
                "desktop/StatsSummaryModel",
                "desktop/UserFavoritesAllSubscriptionsEmpty",
                "desktop/ProxyPublicApi",
                "desktop/ProxySearcher").post();

        urlSteps.testing().path(STATS).path(CARS).path(url).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class, Screenshooter.class})
    @DisplayName("Как дешевеет это авто с возрастом")
    public void shouldSeePricePerAge() {
        basePageSteps.setWindowMaxHeight();

        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onStatsPage().pricePerAge());

        urlSteps.onCurrentUrl().setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onStatsPage().pricePerAge());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по возрасту авто")
    @Category({Regression.class, Testing.class})
    public void shouldClickAge() {
        basePageSteps.onStatsPage().pricePerAgeItem("Новая").should(isDisplayed()).click();
        urlSteps.testing().path(listingUrl).shouldNotSeeDiff();
    }
}
