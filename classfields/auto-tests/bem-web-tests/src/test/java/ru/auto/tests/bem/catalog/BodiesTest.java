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
import org.openqa.selenium.Keys;
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

import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
@DisplayName("Каталог - блок кузовов на карточках")
@Feature(AutoruFeatures.CATALOG)
public class BodiesTest {

    private static final String MARK = "hyundai";
    private static final String MODEL = "solaris";
    private static final int BODY_ID = 1;
    private static final String GENERATION = "20922677";
    private static final String BODY = "20922742";

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
    public String path;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {format("/%s/%s/", MARK, MODEL)}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(CATALOG).path(path).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Screenshooter.class})
    @DisplayName("Кузова")
    public void shouldSeeBodies() {
        screenshotSteps.setWindowSizeForScreenshot();

        String ignore = "//div[@class='mosaic__p']";
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotIgnoreElements(catalogPageSteps.onCatalogPage().cardGenerations()
                        .waitUntil(isDisplayed()), newHashSet(ignore));

        urlSteps.setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotIgnoreElements(catalogPageSteps.onCatalogPage().cardGenerations()
                        .waitUntil(isDisplayed()), newHashSet(ignore));

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по кузову")
    public void shouldClickBody() {
        catalogPageSteps.onCatalogPage().bodiesList().should(hasSize(greaterThan(0))).get(BODY_ID)
                .waitUntil(isDisplayed()).click();
        urlSteps.testing().path(CATALOG).path(CARS).path(MARK).path(MODEL)
                .path(GENERATION).path(BODY).path("/").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по ссылке на объявления с пробегом в списке кузовов")
    public void shouldClickUsedSalesUrl() {
        catalogPageSteps.onCatalogPage().bodiesList().should(hasSize(greaterThan(0))).get(BODY_ID).usedUrl()
                .should(hasAttribute("href", urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(MODEL)
                        .path(GENERATION).path(BODY).path(USED).toString()))
                .sendKeys(Keys.chord(Keys.CONTROL, Keys.RETURN));
        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по ссылке на новые объявления в списке кузовов")
    public void shouldClickNewSalesUrl() {
        catalogPageSteps.onCatalogPage().bodiesList().should(hasSize(greaterThan(0))).get(BODY_ID).newUrl()
                .should(hasAttribute("href", urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(MODEL)
                        .path(NEW).toString()))
                .sendKeys(Keys.chord(Keys.CONTROL, Keys.RETURN));
        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }
}