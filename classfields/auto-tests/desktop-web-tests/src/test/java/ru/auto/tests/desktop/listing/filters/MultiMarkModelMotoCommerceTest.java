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

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FILTERS;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.LCV;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;

//import io.qameta.allure.Parameter;

@DisplayName("Лёгкие коммерческие, мото - Мультивыбор марок/моделей")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class MultiMarkModelMotoCommerceTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    //@Parameter("Категория ТС")
    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String subCategory;

    @Parameterized.Parameter(2)
    public String mark1;

    @Parameterized.Parameter(3)
    public String model1;

    @Parameterized.Parameter(4)
    public String mark2;

    @Parameterized.Parameter(5)
    public String model2;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {LCV, TRUCKS, "Citroen", "Berlingo", "Ford", "Tourneo"},
                {MOTORCYCLE, MOTO, "Honda", "X4", "Ducati", "Diavel"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(category).path(ALL).open();
    }


    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Мультивыбор марка + марка")
    public void shouldSeeMarksInUrl() {
        basePageSteps.onListingPage().filter().selectItem("Марка", mark1);
        basePageSteps.onListingPage().filter().addIcon().click();
        basePageSteps.onListingPage().filter().selectPopup().item(mark2).click();

        urlSteps
                .addParam("catalog_filter", format("mark=%s", mark1.toUpperCase()))
                .addParam("catalog_filter", format("mark=%s", mark2.toUpperCase())).shouldNotSeeDiff();

        basePageSteps.onListingPage().filter().resultsButton().waitUntil(isDisplayed()).click();

        urlSteps.shouldNotSeeDiff();

        basePageSteps.onListingPage().salesList().should(hasSize(greaterThan(0)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Мультивыбор модель + модель")
    public void shouldSeeModelsInUrl() {
        basePageSteps.onListingPage().filter().selectItem("Марка", mark1);
        basePageSteps.onListingPage().filter().select("Модель").waitUntil(isEnabled());
        basePageSteps.onListingPage().filter().selectItem("Модель", model1);
        basePageSteps.onListingPage().body().sendKeys(Keys.ESCAPE);
        basePageSteps.onListingPage().filter().addIcon().click();
        basePageSteps.onListingPage().filter().selectPopup().item(mark2).click();
        basePageSteps.onListingPage().filter().select("Модель").waitUntil(isEnabled());
        basePageSteps.onListingPage().filter().selectItem("Модель", model2);
        basePageSteps.onListingPage().body().sendKeys(Keys.ESCAPE);

        String expectedParam1 = format("mark%%3D%s%%2Cmodel%%3D%s", mark1.toUpperCase(), model1.toUpperCase());
        String expectedParam2 = format("mark%%3D%s%%2Cmodel%%3D%s", mark2.toUpperCase(), model2.toUpperCase());

        urlSteps
                .addParam("catalog_filter", expectedParam1)
                .addParam("catalog_filter", expectedParam2).shouldNotSeeDiff();

        basePageSteps.onListingPage().filter().submitButton().waitUntil(isDisplayed()).click();

        urlSteps.shouldNotSeeDiff();

        basePageSteps.onListingPage().salesList().should(hasSize(greaterThan(0)));
    }
}