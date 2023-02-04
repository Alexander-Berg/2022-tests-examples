package ru.auto.tests.desktop.listing.filters;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FILTERS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.RUSSIA;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг - расширенный фильтр - чекбоксы - комплектация")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class AdvancedFiltersCheckboxesCatalogEquipmentCarsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;


    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    //@Parameter("Ссылка")
    @Parameterized.Parameter
    public String path;

    //@Parameter("Название чекбокса")
    @Parameterized.Parameter(1)
    public String checkboxTitle;

    //@Parameter("Значение параметра")
    @Parameterized.Parameter(2)
    public String paramValue;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"/cars/all/", "Датчик дождя", "rain-sensor"},
                {"/cars/new/", "Антиблокировочная система (ABS)", "abs"},
                {"/cars/audi/used/", "Омыватель фар", "light-cleaner"}
        });
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор параметра-чекбокса в комплектации")
    public void shouldSeeCheckboxParamInUrl() {
        urlSteps.testing().path(RUSSIA).path(path).open();
        basePageSteps.onListingPage().filter().showAdvancedFilters();
        basePageSteps.onListingPage().filter().checkbox(checkboxTitle).should(isDisplayed()).hover();
        basePageSteps.scrollDown(
                basePageSteps.onListingPage().filter().stickyPanel().waitUntil(isDisplayed()).getRect().getY());
        basePageSteps.onListingPage().filter().checkbox(checkboxTitle).should(isDisplayed()).click();
        urlSteps.addParam("catalog_equipment", paramValue).shouldNotSeeDiff();
        basePageSteps.onListingPage().filter().resultsButton().waitUntil(isDisplayed()).click();
        urlSteps.shouldNotSeeDiff();
    }
}