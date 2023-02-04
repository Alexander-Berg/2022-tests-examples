package ru.auto.tests.bem.catalog;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.CatalogPageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;

@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
@DisplayName("Каталог - галерея на карточке модели")
@Feature(AutoruFeatures.CATALOG)
public class ModelCardGalleryTest {

    private static final String MARK = "vaz";
    private static final String MODEL = "kalina";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public CatalogPageSteps catalogPageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    public ScreenshotSteps screenshotSteps;

    @Inject
    public CookieSteps cookieSteps;

    @Parameterized.Parameter
    public Integer photoNum;

    @Parameterized.Parameter(1)
    public String generation;

    @Parameterized.Parameter(2)
    public String body;

    @Parameterized.Parameters(name = "name = {index}: фото {0} {1} {2}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {0, "9389448", "9389470"},
                {1, "2307278", "2307279"}
        });
    }

    @Before
    public void before() {
        screenshotSteps.setWindowSizeForScreenshot();
        urlSteps.testing().path(CATALOG).path(CARS).path(MARK).path(MODEL).open();
        catalogPageSteps.onCatalogPage().modelSummary().hover();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Screenshooter.class})
    @DisplayName("Галерея")
    public void shouldSeeGallery() {
        catalogPageSteps.onCatalogPage().modelSummary().photosList().get(photoNum).hover();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(catalogPageSteps.onCatalogPage().modelSummary().gallery());

        urlSteps.setProduction().open();
        catalogPageSteps.onCatalogPage().modelSummary().hover();
        catalogPageSteps.onCatalogPage().modelSummary().photosList().get(photoNum).hover();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(catalogPageSteps.onCatalogPage().modelSummary().gallery());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по фото")
    public void shouldClickPhoto() {
        catalogPageSteps.onCatalogPage().modelSummary().photosList().get(photoNum).click();
        urlSteps.path(generation).path(body).path("/").shouldNotSeeDiff();
    }
}