package ru.auto.tests.desktop.main;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import java.util.Collection;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.MAIN;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.VLADIVOSTOK;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Главная - блок марок - отображение в разных регионах")
@Feature(MAIN)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class MarksBlockRegionsTest {

    private static final Set<String> IGNORE = newHashSet("//div[@class = 'IndexMarks__item-count']",
            "//div[@class = 'IndexSelector__submit']/button", ".//a[contains(@class, 'Banner')]");

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Inject
    private CookieSteps cookieSteps;

    //@Parameter("Регион")
    @Parameterized.Parameter
    public String region;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {MOSKVA},
                {VLADIVOSTOK}
        });
    }

    @Before
    public void before() {
        cookieSteps.setCookieForBaseDomain("noads", "1");
        basePageSteps.setNarrowWindowSize();

        urlSteps.testing().path(region).open();
    }

    @Test
    @Category({Regression.class, Screenshooter.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Популярные марки")
    public void shouldSeeMarksBlock() {
        basePageSteps.onMainPage().marksBlock().allMarksUrl().hover();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotIgnoreElements(basePageSteps.onMainPage().marksBlock().waitUntil(isDisplayed()),
                        newHashSet(IGNORE));

        urlSteps.setProduction().open();
        basePageSteps.onMainPage().marksBlock().allMarksUrl().hover();
        Screenshot prodScreenshot = screenshotSteps
                .getElementScreenshotIgnoreElements(basePageSteps.onMainPage().marksBlock().waitUntil(isDisplayed()),
                        newHashSet(IGNORE));

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, prodScreenshot);
    }

    @Test
    @Category({Regression.class, Screenshooter.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Все марки")
    public void shouldSeeAllMarks() {
        basePageSteps.onMainPage().marksBlock().allMarksUrl().waitUntil(isDisplayed()).click();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotIgnoreElements(basePageSteps.onMainPage().marksBlock().waitUntil(isDisplayed()),
                        newHashSet(IGNORE));

        urlSteps.setProduction().open();
        basePageSteps.onMainPage().marksBlock().allMarksUrl().waitUntil(isDisplayed()).click();
        Screenshot prodScreenshot = screenshotSteps
                .getElementScreenshotIgnoreElements(basePageSteps.onMainPage().marksBlock().waitUntil(isDisplayed()),
                        newHashSet(IGNORE));

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, prodScreenshot);
    }
}
