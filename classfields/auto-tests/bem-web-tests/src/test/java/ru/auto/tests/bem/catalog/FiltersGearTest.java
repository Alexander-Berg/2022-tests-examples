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


@DisplayName("Каталог - фильтры - привод")
@Feature(AutoruFeatures.CATALOG)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class FiltersGearTest {

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
                {"/all/", "Передний", "FORWARD_CONTROL"},
                {"/all/", "Задний", "REAR_DRIVE"},
                {"/all/", "Полный", "ALL_WHEEL_DRIVE"},
                {"/toyota/", "Передний", "FORWARD_CONTROL"},
                {"/toyota/", "Задний", "REAR_DRIVE"},
                {"/toyota/", "Полный", "ALL_WHEEL_DRIVE"},
                {"/toyota/corolla/", "Передний", "FORWARD_CONTROL"},
                {"/toyota/corolla/", "Задний", "REAR_DRIVE"},
                {"/toyota/corolla/", "Полный", "ALL_WHEEL_DRIVE"},
                {"/toyota/corolla/20807898/", "Передний", "FORWARD_CONTROL"},
                {"/toyota/corolla/20807898/", "Полный", "ALL_WHEEL_DRIVE"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(CATALOG).path(CARS).path(url).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Параметр 'Привод'")
    public void shouldSeeGearInUrl() {
        user.onCatalogPage().filter().gear().should(isDisplayed()).click();
        user.onCatalogPage().activePopup().waitUntil(isDisplayed());
        user.onCatalogPage().activeListItemByContains(paramTextValue).click();
        user.onCatalogPage().filter().gear().click();
        user.onCatalogPage().activePopup().waitUntil(not(isDisplayed()));
        user.onCatalogPage().filter().submitButton().waitUntil(isDisplayed()).click();
        urlSteps.addParam("gear_type", paramValue.toLowerCase()).ignoreParam("view_type").shouldNotSeeDiff();
    }
}