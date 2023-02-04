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
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
@DisplayName("Каталог - хлебные крошки")
@Feature(AutoruFeatures.CATALOG)
public class BreadcrumbsTest {

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

    @Parameterized.Parameter(1)
    public String breadcrumbsItem;

    @Parameterized.Parameters(name = "name = {index}: {0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"/cars/", "Марка, модель, поколение "},
                {"/cars/vaz/", "Выбрать модель "},
                {"/cars/vaz/kalina/", "Выбрать поколение "},
                {"/cars/vaz/kalina/9389448/", "Выбрать кузов "},
                {"/cars/vaz/kalina/9389448/9389470/", "Хэтчбек 5 дв. "}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(CATALOG).path(path).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Screenshooter.class})
    @DisplayName("Крошки в свернутом состоянии")
    public void shouldSeeClosedBreadcrumbs() {
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(catalogPageSteps.onCatalogPage().filter().markModelGenBlock());

        urlSteps.setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(catalogPageSteps.onCatalogPage().filter().markModelGenBlock());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Screenshooter.class})
    @DisplayName("Крошки в развернутом состоянии")
    public void shouldSeeOpenBreadcrumbs() {
        catalogPageSteps.onCatalogPage().filter().markModelGenBlock().breadcrumbsItem(breadcrumbsItem)
                .should(isDisplayed()).click();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(catalogPageSteps.onCatalogPage().filter().markModelGenBlock());

        cookieSteps.deleteCookie("mmm-search-accordion-is-open-cars");

        urlSteps.setProduction();
        urlSteps.testing().path(CATALOG).path(path).open();
        catalogPageSteps.onCatalogPage().filter().markModelGenBlock().breadcrumbsItem(breadcrumbsItem)
                .should(isDisplayed()).click();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(catalogPageSteps.onCatalogPage().filter().markModelGenBlock());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }
}