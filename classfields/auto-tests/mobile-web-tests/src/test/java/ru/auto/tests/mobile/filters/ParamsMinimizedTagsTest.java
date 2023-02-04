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
import static ru.auto.tests.desktop.consts.Pages.AGRICULTURAL;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.ARTIC;
import static ru.auto.tests.desktop.consts.Pages.ATV;
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
import static ru.auto.tests.desktop.consts.Pages.SCOOTERS;
import static ru.auto.tests.desktop.consts.Pages.SNOWMOBILE;
import static ru.auto.tests.desktop.consts.Pages.TRAILER;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Расширенные фильтры - тэги")
@Feature(AutoruFeatures.FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ParamsMinimizedTagsTest {

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
    public String section;

    @Parameterized.Parameter(2)
    public String paramName;

    @Parameterized.Parameter(3)
    public String paramValue;

    @Parameterized.Parameter(4)
    public String paramQueryName;

    @Parameterized.Parameter(5)
    public String paramQueryValue;

    @Parameterized.Parameters(name = "name = {index}: {0} {1} {2} {3}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, ALL, "Коробка", "Автомат", "transmission", "AUTO"},
                {CARS, ALL, "Коробка", "Автоматическая", "transmission", "AUTOMATIC"},
                {CARS, ALL, "Расположение руля", "Левый", "steering_wheel", "LEFT"},

                {LCV, ALL, "Грузоподъёмность, тонн", "до 1 т.", "loading_to", "1000"},
                {LCV, ALL, "Коробка", "Автоматическая", "transmission", "AUTOMATIC"},
                {LCV, ALL, "Двигатель", "Бензин", "engine_type", "GASOLINE"},
                {LCV, ALL, "Привод", "Передний", "gear_type", "FRONT"},
                {LCV, ALL, "Цвет", "Белый", "color", "FAFBFB"},

                {TRUCK, ALL, "Грузоподъёмность, тонн", "до 3,5 т.", "loading_to", "3500"},
                {TRUCK, ALL, "Тип кабины", "2-х местная без спального", "cabin_key", "SEAT_2_WO_SLEEP"},
                {TRUCK, ALL, "Подвеска кабины", "Механическая", "suspension_cabin", "MECHANICAL"},
                {TRUCK, ALL, "Колёсная формула", "4x2", "wheel_drive", "WD_4x2"},
                {TRUCK, ALL, "Подвеска шасси", "Рессора-пневмо", "suspension_chassis", "SPRING_PNEUMO"},
                {TRUCK, ALL, "Коробка", "Автоматическая", "transmission", "AUTOMATIC"},
                {TRUCK, ALL, "Двигатель", "Бензин", "engine_type", "GASOLINE"},
                {TRUCK, ALL, "Класс выхлопа EURO", "0", "euro_class", "EURO_0"},

                {ARTIC, ALL, "Коробка", "Автоматическая", "transmission", "AUTOMATIC"},

                {BUS, ALL, "Коробка", "Автоматическая", "transmission", "AUTOMATIC"},

                {TRAILER, ALL, "Тормоза", "Барабанные", "brake_type", "DRUM"},

                {AGRICULTURAL, ALL, "Цвет", "Белый", "color", "FAFBFB"},

                {CONSTRUCTION, ALL, "Цвет", "Белый", "color", "FAFBFB"},

                {CRANE, ALL, "Цвет", "Белый", "color", "FAFBFB"},

                {DREDGE, ALL, "Цвет", "Белый", "color", "FAFBFB"},

                {BULLDOZERS, ALL, "Цвет", "Белый", "color", "FAFBFB"},

                {CRANE, ALL, "Цвет", "Белый", "color", "FAFBFB"},

                {MUNICIPAL, ALL, "Цвет", "Белый", "color", "FAFBFB"},

                {MOTORCYCLE, ALL, "Коробка", "1 передача", "transmission", "TRANSMISSION_1"},
                {MOTORCYCLE, ALL, "Двигатель", "Дизель", "engine_type", "DIESEL"},
                {MOTORCYCLE, ALL, "Привод", "Кардан", "gear_type", "CARDAN"},
                {MOTORCYCLE, ALL, "Количество цилиндров", "1", "cylinders", "CYLINDERS_1"},
                {MOTORCYCLE, ALL, "Расположение цилиндров", "V-образное", "cylinders_type", "V_TYPE"},
                {MOTORCYCLE, ALL, "Число тактов", "2", "strokes", "STROKES_2"},
                {MOTORCYCLE, ALL, "Цвет", "Белый", "color", "FAFBFB"},

                {SCOOTERS, ALL, "Цвет", "Белый", "color", "FAFBFB"},

                {ATV, ALL, "Цвет", "Белый", "color", "FAFBFB"},

                {SNOWMOBILE, ALL, "Цвет", "Белый", "color", "FAFBFB"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(category).path(section).open();
        basePageSteps.onListingPage().filters().paramsButton().click();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onListingPage().paramsPopup().param(paramName).hover().click();
    }

    @Test
    @DisplayName("Тэги")
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    public void shouldClickTag() {
        basePageSteps.onListingPage().paramsPopup().tags(paramName).button(paramValue).click();
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onListingPage().paramsPopup().applyFiltersButton().click();
        urlSteps.addParam(paramQueryName, paramQueryValue).shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().waitUntil(isDisplayed());
        basePageSteps.onListingPage().filters().should(isDisplayed());
        urlSteps.refresh();
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().should(isDisplayed());
    }
}
