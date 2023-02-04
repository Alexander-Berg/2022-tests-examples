package ru.auto.tests.mobile.filters;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.Collection;

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static ru.auto.tests.commons.util.Utils.getRandomShortInt;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FILTERS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.AGRICULTURAL;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.ARTIC;
import static ru.auto.tests.desktop.consts.Pages.BULLDOZERS;
import static ru.auto.tests.desktop.consts.Pages.BUS;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CONSTRUCTION;
import static ru.auto.tests.desktop.consts.Pages.CRANE;
import static ru.auto.tests.desktop.consts.Pages.DREDGE;
import static ru.auto.tests.desktop.consts.Pages.LCV;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.MUNICIPAL;
import static ru.auto.tests.desktop.consts.Pages.TRAILER;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Параметры - инпуты от/до")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ParamsInputsFromToTest {

    private String paramValue = valueOf(getRandomShortInt());

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String paramName;

    @Parameterized.Parameter(2)
    public String paramQueryName;

    @Parameterized.Parameters(name = "name = {index}: {0} {2}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, "Мощность, л.с.", "power"},
                {CARS, "Пробег, км", "km_age"},
                {CARS, "Разгон до 100 км/ч, с", "acceleration"},

                {LCV, "Цена, ₽", "price"},
                {LCV, "Число мест", "seats"},
                {LCV, "Мощность, л.с.", "power"},
                {LCV, "Пробег, км", "km_age"},

                {TRUCK, "Цена, ₽", "price"},
                {TRUCK, "Мощность, л.с.", "power"},
                {TRUCK, "Пробег, км", "km_age"},

                {ARTIC, "Цена, ₽", "price"},
                {ARTIC, "Мощность, л.с.", "power"},
                {ARTIC, "Пробег, км", "km_age"},

                {BUS, "Цена, ₽", "price"},
                {BUS, "Число мест", "seats"},
                {BUS, "Мощность, л.с.", "power"},
                {BUS, "Пробег, км", "km_age"},

                {TRAILER, "Цена, ₽", "price"},
                {TRAILER, "Количество осей", "axis"},
                {TRAILER, "Пробег, км", "km_age"},

                {AGRICULTURAL, "Цена, ₽", "price"},
                {AGRICULTURAL, "Моточасы", "operating_hours"},
                {AGRICULTURAL, "Мощность, л.с.", "power"},

                {CONSTRUCTION, "Цена, ₽", "price"},
                {CONSTRUCTION, "Моточасы", "operating_hours"},

                {CRANE, "Цена, ₽", "price"},
                {CRANE, "Моточасы", "operating_hours"},
                {CRANE, "Высота подъёма, м", "load_height"},
                {CRANE, "Вылет стрелы, м", "crane_radius"},
                {CRANE, "Пробег, км", "km_age"},

                {DREDGE, "Цена, ₽", "price"},
                {DREDGE, "Моточасы", "operating_hours"},
                {DREDGE, "Мощность, л.с.", "power"},
                {DREDGE, "Объём ковша, м³", "bucket_volume"},

                {BULLDOZERS, "Цена, ₽", "price"},
                {BULLDOZERS, "Моточасы", "operating_hours"},
                {BULLDOZERS, "Мощность, л.с.", "power"},

                {MUNICIPAL, "Цена, ₽", "price"},
                {MUNICIPAL, "Мощность, л.с.", "power"},

                {MOTORCYCLE, "Цена, ₽", "price"},
                {MOTORCYCLE, "Мощность, л.с.", "power"},
                {MOTORCYCLE, "Пробег, км", "km_age"},
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(category).path(ALL).open();
        basePageSteps.onListingPage().filters().paramsButton().click();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onListingPage().paramsPopup().param(paramName).hover().click();
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Инпуты от")
    public void shouldInputFrom() {
        basePageSteps.onListingPage().paramsPopup().inputFrom(paramName).waitUntil(isDisplayed()).sendKeys(paramValue);
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onListingPage().paramsPopup().applyFiltersButton().click();
        urlSteps.addParam(format("%s_from", paramQueryName), paramValue).shouldNotSeeDiff();
        urlSteps.refresh();
        urlSteps.shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Инпуты до")
    public void shouldInputTo() {
        basePageSteps.onListingPage().paramsPopup().inputTo(paramName).waitUntil(isDisplayed()).sendKeys(paramValue);
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onListingPage().paramsPopup().applyFiltersButton().click();
        urlSteps.addParam(format("%s_to", paramQueryName), paramValue).shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().should(isDisplayed());
        urlSteps.refresh();
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().should(isDisplayed());
    }
}
