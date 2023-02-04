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

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.not;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Каталог - фильтры - число мест")
@Feature(AutoruFeatures.CATALOG)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class FiltersSeatsTest {

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
    public String paramTextValue;

    @Parameterized.Parameter(2)
    public String paramValue;

    @Parameterized.Parameters(name = "name = {index}: {0} {1} {2}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"/all/", "2 места", "2"},
                {"/all/", "4-5 мест", "4_5"},
                {"/all/", "6-8 мест", "6_7_8"},
                {"/audi/", "2 места", "2"},
                {"/audi/", "4-5 мест", "4_5"},
                {"/audi/", "6-8 мест", "6_7_8"},
                {"/audi/tt/", "2 места", "2"},
                {"/audi/tt/", "4-5 мест", "4_5"},
                {"/audi/q7/", "6-8 мест", "6_7_8"},
                {"/audi/tt/20176878/", "2 места", "2"},
                {"/audi/tt/20176878/", "4-5 мест", "4_5"},
                {"/audi/q7/6468119/", "6-8 мест", "6_7_8"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(CATALOG).path(CARS).path(url).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Параметр 'Количество мест'")
    public void shouldSeeSeatsInUrl() {
        user.onCatalogPage().filter().seats().should(isDisplayed()).click();
        user.onCatalogPage().activePopup().waitUntil(isDisplayed());
        user.onCatalogPage().activeListItemByContains(paramTextValue).click();
        user.onCatalogPage().activePopup().waitUntil(not(isDisplayed()));
        user.onCatalogPage().filter().submitButton().waitUntil(isDisplayed()).click();
        urlSteps.addParam("seats", paramValue).ignoreParam("view_type").shouldNotSeeDiff();
    }
}