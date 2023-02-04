package ru.auto.tests.bem.catalog;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
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
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.not;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Каталог - фильтры - двигатель")
@Feature(AutoruFeatures.CATALOG)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class FiltersEngineTest {

    @Rule
    @Inject
    public RuleChain defaultRules;


    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Parameterized.Parameter
    public String paramTextValue;

    @Parameterized.Parameter(1)
    public String paramValue;

    @Parameterized.Parameters(name = "name = {index}: {0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"Бензин", "GASOLINE"},
                {"Дизель", "DIESEL"},
                {"Гибрид", "HYBRID"},
                {"Газ", "LPG"},
                {"Электрический", "ELECTRO"},
                {"Турбированный", "ENGINE_TURBO"},
                {"Атмосферный", "ENGINE_NONE"}
        });
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Параметр «Двигатель»")
    public void shouldSeeEngineInUrl() {
        urlSteps.testing().path(CATALOG).path(CARS).path(ALL).open();
        basePageSteps.onCatalogPage().filter().engine().should(isDisplayed()).click();
        basePageSteps.onCatalogPage().activePopup().waitUntil(isDisplayed());
        basePageSteps.onCatalogPage().activeListItemByContains(paramTextValue).click();
        basePageSteps.onCatalogPage().filter().engine().click();
        basePageSteps.onListingPage().activePopup().waitUntil(not(isDisplayed()));
        basePageSteps.onCatalogPage().filter().submitButton().waitUntil(isDisplayed()).click();
        urlSteps.addParam("engine_type", paramValue.toLowerCase()).ignoreParam("view_type").shouldNotSeeDiff();
    }
}