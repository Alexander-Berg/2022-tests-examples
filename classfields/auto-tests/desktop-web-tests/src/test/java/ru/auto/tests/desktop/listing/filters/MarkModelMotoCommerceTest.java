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
import org.openqa.selenium.Keys;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FILTERS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.LCV;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;

@DisplayName("КомТС и Мото - фильтр по марке")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class MarkModelMotoCommerceTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String mark;

    @Parameterized.Parameter(2)
    public String model;

    @Parameterized.Parameters(name = "name = {index}: {0} {1} {2}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {LCV, "Citroen", "Berlingo"},
                {MOTORCYCLE, "Honda", "X4"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(category).path(ALL).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор марки")
    public void shouldSelectMark() {
        basePageSteps.onListingPage().filter().selectItem("Марка", mark);
        basePageSteps.onListingPage().body().sendKeys(Keys.ESCAPE);
        basePageSteps.onListingPage().filter().resultsButton().waitUntil(isDisplayed()).click();

        urlSteps.testing().path(MOSKVA).path(category).path(mark.toLowerCase()).path(ALL)
                .shouldNotSeeDiff();

        basePageSteps.onListingPage().salesList().waitUntil(hasSize(greaterThan(0)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор модели")
    public void shouldSelectModel() {
        basePageSteps.onListingPage().filter().selectItem("Марка", mark);
        basePageSteps.onListingPage().filter().select("Модель").waitUntil(isEnabled());
        basePageSteps.onListingPage().filter().selectItem("Модель", model);
        basePageSteps.onListingPage().body().sendKeys(Keys.ESCAPE);
        basePageSteps.onListingPage().filter().resultsButton().waitUntil(isDisplayed()).click();

        urlSteps.testing().path(MOSKVA).path(category).path(mark.toLowerCase()).path(model.toLowerCase())
                .path(ALL).shouldNotSeeDiff();

        basePageSteps.onListingPage().salesList().waitUntil(hasSize(greaterThan(0)));
    }
}