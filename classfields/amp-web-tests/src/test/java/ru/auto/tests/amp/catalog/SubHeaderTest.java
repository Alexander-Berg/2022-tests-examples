package ru.auto.tests.amp.catalog;

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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.AMP;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;

@RunWith(Parameterized.class)
@GuiceModules(MobileTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
@DisplayName("Каталог - подшапка")
@Feature(AutoruFeatures.AMP)
public class SubHeaderTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    public ScreenshotSteps screenshotSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String url;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {""},
                {"/vaz/"},
                {"/vaz/vesta/"},
                {"/vaz/vesta/20417749/"},
                {"/vaz/vesta/20417749/20417777/"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(AMP).path(CATALOG).path(CARS).path(url).open();
    }

    @Test
    @DisplayName("Отображение подшапки")
    @Category({Regression.class, Screenshooter.class})
    @Owner(DSVICHIHIN)
    public void shouldSeeSubheader() {
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onCatalogPage().subHeader());

        urlSteps.setProduction().open();
        Screenshot prodScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onCatalogPage().subHeader());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, prodScreenshot);
    }
}