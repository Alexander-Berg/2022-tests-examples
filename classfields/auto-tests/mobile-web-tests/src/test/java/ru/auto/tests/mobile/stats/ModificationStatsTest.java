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
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DISPLACEMENT;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.STATS;

@Feature(AutoruFeatures.STATS)
@DisplayName("Статистика - деление по модификациям")
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ModificationStatsTest {

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
                {"/hyundai/solaris/6847474/6847477/6847477_8518368_6847481/", "/hyundai/solaris/6847474/6847477/"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionUnauth",
                "desktop/StatsSummaryModification",
                "desktop/UserFavoritesAllSubscriptionsEmpty",
                "desktop/ProxyPublicApi",
                "desktop/ProxySearcher").post();

        screenshotSteps.setWindowSizeForScreenshot();
        urlSteps.testing().path(STATS).path(CARS).path(url).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class, Screenshooter.class})
    @DisplayName("Деление по модификациям")
    public void shouldSeeModificationStats() {
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onStatsPage().modificationsStats());

        urlSteps.onCurrentUrl().setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onStatsPage().modificationsStats());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Деление по модификациям - клик по двигателю")
    @Category({Regression.class, Testing.class})
    public void shouldClickEngine() {
        basePageSteps.onStatsPage().modificationUrl("1.4\u00a0л (35%)").hover().click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(listingUrl).path(ALL).path(format(DISPLACEMENT, "1400")).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Деление по модификациям - клик по топливу")
    @Category({Regression.class, Testing.class})
    public void shouldClickFuel() {
        basePageSteps.onStatsPage().modificationUrl("Бензин (100%)").hover().click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(listingUrl).path(ALL).path("engine-benzin/")
                .shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Деление по модификациям - клик по коробке")
    @Category({Regression.class, Testing.class})
    public void shouldClickTransmission() {
        basePageSteps.onStatsPage().modificationUrl("Автомат (52%)").hover().click();
        ;
        urlSteps.testing().path(MOSKVA).path(CARS).path(listingUrl).path(ALL).addParam("transmission", "AUTOMATIC")
                .shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Деление по модификациям - клик по приводу")
    @Category({Regression.class, Testing.class})
    public void shouldClickGear() {
        basePageSteps.onStatsPage().modificationUrl("Передний (100%)").hover().click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(listingUrl).path(ALL).path("drive-forward_wheel/")
                .shouldNotSeeDiff();
    }
}
