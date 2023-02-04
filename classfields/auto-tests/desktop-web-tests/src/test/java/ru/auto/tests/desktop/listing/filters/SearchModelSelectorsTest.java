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
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FILTERS;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Легковые - поиск в селекте модели")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SearchModelSelectorsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Parameterized.Parameter
    public String mark;

    @Parameterized.Parameter(1)
    public String text;

    @Parameterized.Parameter(2)
    public String model;

    @Parameterized.Parameters(name = "name = {index}: {0} {1} {2}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"gaz", "зим", "12 ЗИМ"},
                {"mitsubishi", "cross", "Eclipse Cross"},
                {"mitsubishi", "asx ", "ASX"},
                {"mitsubishi", "i-", "i-MiEV"},
                {"infiniti", "55", "QX55"}
        });
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class})
    @DisplayName("Поиск по модели")
    public void shouldSearchModelSelector() {
        urlSteps.testing().path(MOSKVA).path(CARS).path(mark).path(ALL).open();
        basePageSteps.onListingPage().filter().select("Модель").click();
        basePageSteps.onListingPage().filter().selectPopup().waitUntil(isDisplayed());
        basePageSteps.onListingPage().filter().select("Модель").input().waitUntil(isDisplayed())
                .sendKeys(text);
        basePageSteps.onListingPage().filter().selectPopup().itemsList().should(hasSize(1));
        basePageSteps.onListingPage().filter().selectPopup().item(model).waitUntil(isDisplayed());
    }

}
