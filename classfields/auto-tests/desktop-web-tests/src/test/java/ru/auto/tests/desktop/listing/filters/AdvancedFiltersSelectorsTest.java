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
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FILTERS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.ARTIC;
import static ru.auto.tests.desktop.consts.Pages.ATV;
import static ru.auto.tests.desktop.consts.Pages.BUS;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.LCV;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.RUSSIA;
import static ru.auto.tests.desktop.consts.Pages.SCOOTERS;
import static ru.auto.tests.desktop.consts.Pages.SNOWMOBILE;
import static ru.auto.tests.desktop.consts.Pages.TRAILER;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

//import io.qameta.allure.Parameter;

@DisplayName("Листинг - расширенный фильтр - селекторы")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class AdvancedFiltersSelectorsTest {

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

    @Parameterized.Parameters(name = "name = {index}: {0} {1} {2}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, ALL, "Владение", "До года", "owning_time_group", "LESS_THAN_YEAR"},
                {CARS, USED, "Руль", "Правый", "steering_wheel", "RIGHT"},
                {CARS, ALL, "Кроме битых", "Битые / не на ходу", "damage_group", "BEATEN"},
                {CARS, USED, "Растаможен", "Неважно", "customs_state_group", "DOESNT_MATTER"},

                {LCV, ALL, "Кроме битых", "Битые / не на ходу", "damage_group", "BEATEN"},
                {LCV, ALL, "Неважно", "Не растаможен", "customs_state_group", "NOT_CLEARED"},
                {LCV, ALL, "Подушки безопасности", "Водителя", "catalog_equipment", "airbag1"},

                {TRUCK, ALL, "Подвеска кабины", "Механическая", "suspension_cabin", "MECHANICAL"},
                {TRUCK, ALL, "Класс выхлопа", "0", "euro_class", "EURO_0"},
                {TRUCK, ALL, "Руль", "Левый", "steering_wheel", "LEFT"},
                {TRUCK, ALL, "Кроме битых", "Битые / не на ходу", "damage_group", "BEATEN"},
                {TRUCK, ALL, "Неважно", "Не растаможен", "customs_state_group", "NOT_CLEARED"},

                {ARTIC, ALL, "Кроме битых", "Битые / не на ходу", "damage_group", "BEATEN"},
                {ARTIC, ALL, "Неважно", "Не растаможен", "customs_state_group", "NOT_CLEARED"},
                {ARTIC, ALL, "Руль", "Левый", "steering_wheel", "LEFT"},

                {BUS, ALL, "Двигатель", "Бензин", "engine_type", "GASOLINE"},
                {BUS, ALL, "Коробка", "Автоматическая ", "transmission", "AUTOMATIC"},
                {BUS, ALL, "Колёсн. ф-ла", "4x2", "wheel_drive", "WD_4x2"},
                {BUS, ALL, "Кроме битых", "Битые / не на ходу", "damage_group", "BEATEN"},
                {BUS, ALL, "Неважно", "Не растаможен", "customs_state_group", "NOT_CLEARED"},

                {TRAILER, ALL, "Кроме битых", "Битые / не на ходу", "damage_group", "BEATEN"},
                {TRAILER, ALL, "Неважно", "Не растаможен", "customs_state_group", "NOT_CLEARED"},

                {MOTORCYCLE, ALL, "Двигатель", "Дизель", "engine_type", "DIESEL"},
                {MOTORCYCLE, ALL, "Цилиндров", "1", "cylinders", "CYLINDERS_1"},
                {MOTORCYCLE, ALL, "Привод", "Кардан", "gear_type", "CARDAN"},
                {MOTORCYCLE, ALL, "Коробка", "1 передача ", "transmission", "TRANSMISSION_1"},
                {MOTORCYCLE, ALL, "Расположение цилиндров", "V-образное", "cylinders_type", "V_TYPE"},
                {MOTORCYCLE, ALL, "Число тактов", "2", "strokes", "STROKES_2"},
                {MOTORCYCLE, ALL, "Кроме битых", "Битые / не на ходу", "damage_group", "BEATEN"},
                {MOTORCYCLE, ALL, "Неважно", "Не растаможен", "customs_state_group", "NOT_CLEARED"},

                {SCOOTERS, ALL, "Кроме битых", "Битые / не на ходу", "damage_group", "BEATEN"},

                {ATV, ALL, "Двигатель", "Инжектор", "engine_type", "GASOLINE_INJECTOR"},
                {ATV, ALL, "Цилиндров", "1", "cylinders", "CYLINDERS_1"},
                {ATV, ALL, "Привод", "Полный", "gear_type", "FULL"},
                {ATV, ALL, "Коробка", "АКПП ", "transmission", "AUTOMATIC"},
                {ATV, ALL, "Расположение цилиндров", "V-образное", "cylinders_type", "V_TYPE"},
                {ATV, ALL, "Число тактов", "2", "strokes", "STROKES_2"},
                {ATV, ALL, "Кроме битых", "Битые / не на ходу", "damage_group", "BEATEN"},
                {ATV, ALL, "Неважно", "Не растаможен", "customs_state_group", "NOT_CLEARED"},

                {SNOWMOBILE, ALL, "Двигатель", "Инжектор", "engine_type", "GASOLINE_INJECTOR"},
                {SNOWMOBILE, ALL, "Цилиндров", "1", "cylinders", "CYLINDERS_1"},
                {SNOWMOBILE, ALL, "Число тактов", "2", "strokes", "STROKES_2"},
                {SNOWMOBILE, ALL, "Расположение цилиндров", "V-образное", "cylinders_type", "V_TYPE"}
        });
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор опции в селекте")
    public void shouldSelectOption() {
        urlSteps.testing().path(RUSSIA).path(category).path(section).open();
        basePageSteps.onListingPage().filter().showAdvancedFilters();
        basePageSteps.hideElement(basePageSteps.onListingPage().filter().stickyPanel());
        basePageSteps.onListingPage().filter().selectItem(selectName, selectItem);

        urlSteps.addParam(param, paramValue).shouldNotSeeDiff();

        basePageSteps.showElement(basePageSteps.onListingPage().filter().stickyPanel());
        basePageSteps.onListingPage().filter().resultsButton().waitUntil(isDisplayed()).click();

        urlSteps.shouldNotSeeDiff();
        basePageSteps.onListingPage().salesList().waitUntil(hasSize(greaterThan(0)));

        basePageSteps.onListingPage().filter().showAdvancedFilters();
        basePageSteps.onListingPage().filter().select(selectItem.trim()).should(isDisplayed());
    }
}
