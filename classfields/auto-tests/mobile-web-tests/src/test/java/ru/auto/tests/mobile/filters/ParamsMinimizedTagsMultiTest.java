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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.LCV;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Расширенные фильтры - тэги")
@Feature(AutoruFeatures.FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ParamsMinimizedTagsMultiTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    //@Parameter("Категория ТС")
    @Parameterized.Parameter
    public String category;

    //@Parameter("Название фильтра")
    @Parameterized.Parameter(1)
    public String paramName;

    //@Parameter("Название параметра")
    @Parameterized.Parameter(2)
    public String paramQueryName;

    //@Parameter("Первое значение селектора")
    @Parameterized.Parameter(3)
    public String paramValue1;

    //@Parameter("Второе значение селектора")
    @Parameterized.Parameter(4)
    public String paramValue2;

    //@Parameter("Первое значение параметра")
    @Parameterized.Parameter(5)
    public String paramQueryValue1;

    //@Parameter("Второе значение параметра")
    @Parameterized.Parameter(6)
    public String paramQueryValue2;

    @Parameterized.Parameters(name = "name = {index}: {0} {1}")
    public static Collection<Object> getParameters() {
        return asList(new Object[][]{
                {CARS, "Кузов", "body_type_group", "Седан", "Хэтчбек 3 дв.", "SEDAN", "HATCHBACK_3_DOORS"},
                {CARS, "Двигатель", "engine_group", "Бензин", "Дизель", "GASOLINE", "DIESEL"},
                {CARS, "Привод", "gear_type", "Передний", "Задний", "FORWARD_CONTROL", "REAR_DRIVE"},
                {CARS, "Цвет", "color", "Серебристый", "Красный", "CACECB", "EE1D19"},

                {LCV, "Двигатель", "engine_type", "Гибрид", "Дизель+газ", "HYBRID", "DIESEL_GAS"},

                {MOTORCYCLE, "Коробка", "transmission", "1 передача", "2 передачи", "TRANSMISSION_1", "TRANSMISSION_2"}
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
    @DisplayName("Тэги")
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    public void shouldClickTag() {
        basePageSteps.onListingPage().paramsPopup().tags(paramName).button(paramValue1).click();
        basePageSteps.onListingPage().paramsPopup().tags(paramName).button(paramValue2).click();
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onListingPage().paramsPopup().applyFiltersButton().click();
        urlSteps.testing().path(MOSKVA).path(category).path(ALL).addParam(paramQueryName, paramQueryValue1)
                .addParam(paramQueryName, paramQueryValue2).shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().should(isDisplayed());
        urlSteps.refresh();
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().should(isDisplayed());
    }
}
