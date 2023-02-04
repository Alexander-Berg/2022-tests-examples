package ru.auto.tests.desktop.listing.filters;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FILTERS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.LCV;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Базовые фильтры поиска - группы селекторов от/до")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class AdvancedFiltersSelectorGroupsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Parameterized.Parameter
    public String category;

    //@Parameter("Секция")
    @Parameterized.Parameter(1)
    public String section;

    //@Parameter("Селект")
    @Parameterized.Parameter(2)
    public String selectName;

    //@Parameter("Опция в селекте")
    @Parameterized.Parameter(3)
    public String selectItem;

    //@Parameter("Параметр")
    @Parameterized.Parameter(4)
    public String param;

    //@Parameter("Значение параметра")
    @Parameterized.Parameter(5)
    public String paramValue;

    @Parameterized.Parameters(name = "{0} {1} {2}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {LCV, ALL, "Объем", "1.0 л", "displacement", "1000"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(category).path(section).open();
        basePageSteps.onListingPage().filter().showAdvancedFilters();
        basePageSteps.focusElementByScrollingOffset(basePageSteps.onListingPage().getSale(0), 0, 0);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Селектор от")
    public void shouldSelectFrom() {
        basePageSteps.onListingPage().filter().selectGroupItem(selectName, "от", selectItem);
        urlSteps.addParam(format("%s_from", param), paramValue).shouldNotSeeDiff();
        basePageSteps.onListingPage().filter().resultsButton().waitUntil(isDisplayed()).click();
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onListingPage().salesList().waitUntil(hasSize(greaterThan(0)));
        basePageSteps.onListingPage().filter().showAdvancedFilters();
        basePageSteps.onListingPage().filter().selectGroup(selectItem).selectButton(selectItem).should(isDisplayed());
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Селектор до")
    public void shouldSelectTo() {
        basePageSteps.onListingPage().filter().selectGroupItem(selectName, "до", selectItem);
        urlSteps.addParam(format("%s_to", param), paramValue).shouldNotSeeDiff();
        basePageSteps.onListingPage().filter().resultsButton().waitUntil(isDisplayed()).click();
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onListingPage().salesList().waitUntil(hasSize(greaterThan(0)));
        basePageSteps.onListingPage().filter().showAdvancedFilters();
        basePageSteps.onListingPage().filter().selectGroup(selectItem).selectButton(selectItem).should(isDisplayed());
    }
}
