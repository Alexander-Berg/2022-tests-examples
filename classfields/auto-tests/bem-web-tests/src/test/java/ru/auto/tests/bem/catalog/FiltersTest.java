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
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;
import java.util.Collection;

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.not;
import static ru.auto.tests.commons.util.Utils.getRandomShortInt;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.utils.Utils.getIntAsString;
import static ru.auto.tests.desktop.utils.Utils.getRandomBetween;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;


@DisplayName("Каталог - фильтры")
@Feature(AutoruFeatures.CATALOG)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class FiltersTest {

    @Rule
    @Inject
    public RuleChain defaultRules;


    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    public ScreenshotSteps screenshotSteps;

    @Parameterized.Parameter
    public String url;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"/all/"},
                {"/audi/"},
                {"/audi/a5/"},
                {"/audi/a5/20795592/"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(CATALOG).path(CARS).path(url).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Screenshooter.class})
    @DisplayName("Отображение фильтров")
    public void shouldSeeFilters() {
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onCatalogPage().filter());

        urlSteps.setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onCatalogPage().filter());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Параметр 'Мощность от'")
    public void shouldSeePowerFromInUrl() {
        String paramValue = getIntAsString(getRandomShortInt());

        basePageSteps.onCatalogPage().filter().powerFrom().should(isDisplayed()).sendKeys(paramValue);
        basePageSteps.onCatalogPage().filter().submitButton().waitUntil(isDisplayed()).click();
        urlSteps.addParam("power_from", format("%s л.с.", paramValue)).ignoreParam("view_type")
                .shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Параметр 'Мощность до'")
    public void shouldSeePowerToInUrl() {
        String paramValue = getIntAsString(getRandomBetween(100, 500));

        basePageSteps.onCatalogPage().filter().powerTo().should(isDisplayed()).sendKeys(paramValue);
        basePageSteps.onCatalogPage().filter().submitButton().waitUntil(isDisplayed()).click();
        urlSteps.addParam("power_to", format("%s л.с.", paramValue)).ignoreParam("view_type").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Параметр 'Разгон от'")
    public void shouldSeeAccelerationFromInUrl() {
        String paramValue = getIntAsString(getRandomShortInt());

        basePageSteps.onCatalogPage().filter().accelerationFrom().should(isDisplayed()).sendKeys(paramValue);
        basePageSteps.onCatalogPage().filter().submitButton().waitUntil(isDisplayed()).click();
        urlSteps.addParam("acceleration_from", format("%s с", paramValue)).ignoreParam("view_type")
                .shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Параметр 'Разгон до'")
    public void shouldSeeAccelerationToInUrl() {
        String paramValue = getIntAsString(getRandomBetween(10, 30));

        basePageSteps.onCatalogPage().filter().accelerationTo().should(isDisplayed()).sendKeys(paramValue);
        basePageSteps.onCatalogPage().filter().submitButton().waitUntil(isDisplayed()).click();
        urlSteps.addParam("acceleration_to", format("%s с", paramValue)).ignoreParam("view_type")
                .shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Параметр 'Год от'")
    public void shouldSeeYearFromInUrl() {
        String paramValue = "2016";

        basePageSteps.onCatalogPage().filter().yearFrom().should(isDisplayed()).click();
        basePageSteps.onCatalogPage().activePopup().waitUntil(isDisplayed());
        basePageSteps.onCatalogPage().activeListItemByContains(paramValue).click();
        basePageSteps.onCatalogPage().activePopup().waitUntil(not(isDisplayed()));
        basePageSteps.onCatalogPage().filter().submitButton().waitUntil(isDisplayed()).click();
        urlSteps.addParam("year_from", paramValue).ignoreParam("view_type").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Параметр 'Год до'")
    public void shouldSeeYearToInUrl() {
        String paramValue = "2017";

        basePageSteps.onCatalogPage().filter().yearTo().should(isDisplayed()).click();
        basePageSteps.onCatalogPage().activePopup().waitUntil(isDisplayed());
        basePageSteps.onCatalogPage().activeListItemByContains(paramValue).click();
        basePageSteps.onCatalogPage().activePopup().waitUntil(not(isDisplayed()));
        basePageSteps.onCatalogPage().filter().submitButton().waitUntil(isDisplayed()).click();
        urlSteps.addParam("year_to", paramValue).ignoreParam("view_type").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Параметр 'Цена от'")
    public void shouldSeePriceFromInUrl() {
        String paramValue = valueOf(getRandomShortInt());

        basePageSteps.onCatalogPage().filter().priceFrom().should(isDisplayed()).sendKeys(paramValue);
        basePageSteps.onCatalogPage().filter().submitButton().waitUntil(isDisplayed()).click();
        urlSteps.addParam("price_from", format("%s \u20BD", paramValue)).ignoreParam("view_type")
                .shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Параметр 'Цена до'")
    public void shouldSeePriceToInUrl() {
        basePageSteps.onCatalogPage().filter().priceTo().should(isDisplayed()).sendKeys("5000000");
        basePageSteps.onCatalogPage().filter().submitButton().waitUntil(isDisplayed()).click();
        urlSteps.addParam("price_to", "5%20000%20000%20\u20BD").ignoreParam("view_type")
                .shouldNotSeeDiff();
    }
}