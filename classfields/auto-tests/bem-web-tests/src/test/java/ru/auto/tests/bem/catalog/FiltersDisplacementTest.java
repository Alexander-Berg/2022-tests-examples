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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.not;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;


@DisplayName("Каталог - фильтры - мощность")
@Feature(AutoruFeatures.CATALOG)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class FiltersDisplacementTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps user;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    public ScreenshotSteps screenshotSteps;

    @Parameterized.Parameter
    public String url;

    @Parameterized.Parameter(1)
    public Double paramValue;

    @Parameterized.Parameters(name = "name = {index}: {0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"/all/", 0.8},
                {"/all/", 1.0},
                {"/all/", 2.2},
                {"/vaz/", 0.8},
                {"/vaz/", 1.0},
                {"/nissan/", 2.2},
                {"/vaz/1111/", 0.8},
                {"/vaz/1111/", 1.0},
                {"/nissan/almera/", 2.2},
                {"/vaz/1111/6268019/", 0.8},
                {"/vaz/1111/6268019/", 1.0},
                {"/nissan/almera/4602011/", 2.2}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(CATALOG).path(CARS).path(url).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Параметр 'Объем от'")
    public void shouldSeeDisplacementFromInUrl() {
        user.onCatalogPage().filter().displacementFrom().should(isDisplayed()).click();
        user.onCatalogPage().activePopup().waitUntil(isDisplayed());
        user.onCatalogPage().activeListItemByContains(format("%s л", paramValue)).click();
        user.onCatalogPage().activePopup().waitUntil(not(isDisplayed()));
        user.onCatalogPage().filter().submitButton().waitUntil(isDisplayed()).click();
        paramValue = paramValue * 1000;
        urlSteps.addParam("displacement_from", format("%s", paramValue.intValue())).ignoreParam("view_type")
                .shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Параметр 'Объем до'")
    public void shouldSeeDisplacementToInUrl() {
        user.onCatalogPage().filter().displacementTo().should(isDisplayed()).click();
        user.onCatalogPage().activePopup().waitUntil(isDisplayed());
        user.onCatalogPage().activeListItemByContains(format("%s л", paramValue)).click();
        user.onCatalogPage().activePopup().waitUntil(not(isDisplayed()));
        user.onCatalogPage().filter().submitButton().waitUntil(isDisplayed()).click();
        paramValue = paramValue * 1000;
        urlSteps.addParam("displacement_to", format("%s", paramValue.intValue())).ignoreParam("view_type")
                .shouldNotSeeDiff();
    }
}