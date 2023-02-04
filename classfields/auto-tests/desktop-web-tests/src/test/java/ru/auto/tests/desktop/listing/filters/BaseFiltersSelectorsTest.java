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

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FILTERS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.AGRICULTURAL;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.ARTIC;
import static ru.auto.tests.desktop.consts.Pages.ATV;
import static ru.auto.tests.desktop.consts.Pages.AUTOLOADER;
import static ru.auto.tests.desktop.consts.Pages.BULLDOZERS;
import static ru.auto.tests.desktop.consts.Pages.BUS;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CONSTRUCTION;
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

@DisplayName("Базовые фильтры поиска - селекторы")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class BaseFiltersSelectorsTest {

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

    @Parameterized.Parameters(name = "{0} {1} {2} {3}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, ALL, "Коробка", "\u00a0\u00a0Автоматическая ", "transmission", "%1$s=AUTOMATIC"},

                {LCV, ALL, "Тип кузова", "Бортовой грузовик", "light_truck_type", "%s=ONBOARD_TRUCK"},
                {LCV, ALL, "Привод", "Постоянный привод на все колеса", "gear_type", "%s=FULL_ALL_WHEEL"},
                {LCV, ALL, "Коробка", "Автоматическая ", "transmission", "%s=AUTOMATIC"},
                {LCV, ALL, "Двигатель", "Бензин", "engine_type", "%s=GASOLINE"},

                {TRUCK, ALL, "Тип кузова", "Автовоз", "truck_type", "%s=AUTOTRANSPORTER"},
                {TRUCK, ALL, "Тип кабины", "2-х местная без спального", "cabin_key", "%s=SEAT_2_WO_SLEEP"},
                {TRUCK, ALL, "Двигатель", "Бензин", "engine_type", "%s=GASOLINE"},
                {TRUCK, ALL, "Коробка", "Автоматическая ", "transmission", "%s=AUTOMATIC"},
                {TRUCK, ALL, "Колёсн. ф-ла", "10x10", "wheel_drive", "%s=WD_10x10"},
                {TRUCK, ALL, "Шасси", "Рессора-рессора", "suspension_chassis", "%s=SPRING_SPRING"},

                {ARTIC, ALL, "Колёсн. ф-ла", "4x2", "wheel_drive", "%s=WD_4x2"},
                {ARTIC, ALL, "Тип кабины", "2-х местная без спального", "cabin_key", "%s=SEAT_2_WO_SLEEP"},
                {ARTIC, ALL, "Высота седельного устройства", "128", "saddle_height", "%s=SH_128"},
                {ARTIC, ALL, "Подвеска кабины", "Механическая", "suspension_cabin", "%s=MECHANICAL"},

                {BUS, ALL, "Тип автобуса", "Вахтовый", "bus_type", "%s=CREW"},

                {TRAILER, ALL, "Тип подвески", "Рессорная", "suspension_type", "%s=SPRING"},
                {TRAILER, ALL, "Тормоза", "Барабанные", "brake_type", "%s=DRUM"},
                {TRAILER, ALL, "Тип прицепа", "Все съёмные кузова", "trailer_type", "%1$s=SWAP_BODY_ALL&%1$s=ISOTHERMAL&%1$s=BULK_CARGO&%1$s=SB_TARPAULIN&%1$s=SB_PLATFORM&%1$s=SB_REFRIGERATOR&%1$s=SB_VAN&%1$s=SPECIAL&%1$s=CONTAINER_TANK"},

                {AGRICULTURAL, ALL, "Тип техники", "Комбайн", "agricultural_type", "%s=COMBAIN_HARVESTER"},

                {CONSTRUCTION, ALL, "Тип техники", "Каток", "construction_type", "%s=RINK"},

                {AUTOLOADER, ALL, "Тип автопогрузчика", "Мини-погрузчик", "autoloader_type", "%s=MINI_FORKLIFTS"},

                {DREDGE, ALL, "Тип экскаватора", "Планировщик", "dredge_type", "%s=PLANNER_EXCAVATOR"},

                {BULLDOZERS, ALL, "Тип", "Колесный бульдозер", "bulldozer_type", "%s=WHEELS_BULLDOZER"},
                {BULLDOZERS, ALL, "Тяговый класс", "3", "traction_class", "%s=TRACTION_3"},

                {MUNICIPAL, ALL, "Тип техники", "Вакуумная машина", "municipal_type", "%s=VACUUM_MACHINE"},
                {MUNICIPAL, ALL, "Двигатель", "Бензин", "engine_type", "%s=GASOLINE"},

                {MOTORCYCLE, ALL, "Тип мотоцикла", "Все внедорожные ", "moto_type", "%1$s=OFF_ROAD_GROUP&%1$s=ALLROUND&%1$s=OFFROAD_ENDURO&%1$s=CROSS_COUNTRY&%1$s=SPORTENDURO&%1$s=TOURIST_ENDURO"},
                {MOTORCYCLE, ALL, "Тип мотоцикла", "\u00a0\u00a0Allround ", "moto_type", "%1$s=ALLROUND"},

                {SCOOTERS, ALL, "Двигатель", "Инжектор", "engine_type", "%s=GASOLINE_INJECTOR"},
                {SCOOTERS, ALL, "Число тактов", "2", "strokes", "%s=STROKES_2"},

                {ATV, ALL, "Тип вездехода", "Детский", "atv_type", "%s=CHILDISH"},

                {SNOWMOBILE, ALL, "Тип снегохода", "Детский", "snowmobile_type", "%s=CHILDISH"},
        });
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор опции в селекте")
    public void shouldSelectItem() {
        urlSteps.testing().path(MOSKVA).path(category).path(section).open();
        basePageSteps.onListingPage().filter().selectItem(selectName, selectItem);
        urlSteps.replaceQuery(format(paramValue, param)).shouldNotSeeDiff();
        basePageSteps.onListingPage().filter().select(selectItem.replaceAll("\u00a0", "").trim())
                .click();
        basePageSteps.onListingPage().filter().resultsButton().waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().waitForListingReload();
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onListingPage().salesList().waitUntil(hasSize(greaterThan(0)));
        basePageSteps.onListingPage().filter().select(selectItem.replaceAll("\u00a0", "").trim())
                .should(isDisplayed());
    }
}
