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

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.containsString;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Каталог - фильтры - коробка")
@Feature(AutoruFeatures.CATALOG)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class FiltersTransmissionTest {

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
                {"Автомат", "%1$sAUTO&%1$sAUTO_AUTOMATIC&%1$sAUTO_ROBOT&%1$sAUTO_VARIATOR"},
                {"Автоматическая", "%sAUTO_AUTOMATIC"},
                {"Робот", "%sAUTO_ROBOT"},
                {"Вариатор", "%sAUTO_VARIATOR"},
                {"Механическая", "%sMECHANICAL"}
        });
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Параметр «Коробка»")
    public void shouldSeeTransmissionInUrl() {
        urlSteps.testing().path(CATALOG).path(CARS).path(ALL).open();
        basePageSteps.onCatalogPage().filter().transmission().should(isDisplayed()).click();
        basePageSteps.onCatalogPage().activePopup().waitUntil(isDisplayed());
        basePageSteps.onCatalogPage().activeListItemByContains(paramTextValue).click();
        basePageSteps.onCatalogPage().filter().transmission().click();
        basePageSteps.onListingPage().activePopup().waitUntil(not(isDisplayed()));
        basePageSteps.onCatalogPage().filter().resultsButton().waitUntil(isDisplayed()).click();
        urlSteps.shouldUrl(containsString(format(paramValue.toLowerCase(), "transmission_full=")));
    }
}