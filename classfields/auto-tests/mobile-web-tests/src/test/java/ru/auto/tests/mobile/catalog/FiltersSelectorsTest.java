package ru.auto.tests.mobile.catalog;

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
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Каталог - расширенные фильтры, селекторы")
@Feature(AutoruFeatures.FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class FiltersSelectorsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(CATALOG).open();
        basePageSteps.onCatalogMainPage().filter().allParamsButton().should(isDisplayed()).click();
    }

    @Parameterized.Parameter
    public String filterName;

    @Parameterized.Parameter(1)
    public String option;

    @Parameterized.Parameter(2)
    public String paramName;

    @Parameterized.Parameter(3)
    public String paramValue;

    @Parameterized.Parameters(name = "name = {index}: {0} {1} {2} {3}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"Тип кузова", "Седан", "autoru_body_type", "SEDAN"},
                {"Тип кузова", "Все хэтчбеки", "autoru_body_type", "HATCHBACK"},
                {"Тип кузова", "– Хэтчбек 3 дв.", "autoru_body_type", "HATCHBACK_3_DOORS"},
                {"Тип кузова", "– Хэтчбек 5 дв.", "autoru_body_type", "HATCHBACK_5_DOORS"},
                {"Тип кузова", "– Лифтбек", "autoru_body_type", "HATCHBACK_LIFTBACK"},
                {"Тип кузова", "Все внедорожники", "autoru_body_type", "ALLROAD"},
                {"Тип кузова", "– Внедорожник 3 дв.", "autoru_body_type", "ALLROAD_3_DOORS"},
                {"Тип кузова", "– Внедорожник 5 дв.", "autoru_body_type", "ALLROAD_5_DOORS"},
                {"Тип кузова", "Универсал", "autoru_body_type", "WAGON"},
                {"Тип кузова", "Купе", "autoru_body_type", "COUPE"},
                {"Тип кузова", "Минивэн", "autoru_body_type", "MINIVAN"},
                {"Тип кузова", "Пикап", "autoru_body_type", "PICKUP"},
                {"Тип кузова", "Лимузин", "autoru_body_type", "LIMOUSINE"},
                {"Тип кузова", "Фургон", "autoru_body_type", "VAN"},
                {"Тип кузова", "Кабриолет", "autoru_body_type", "CABRIO"},

                {"Коробка", "Все автоматы", "transmission_full", "AUTO"},
                {"Коробка", "– Автоматическая", "transmission_full", "AUTO_AUTOMATIC"},
                {"Коробка", "– Робот", "transmission_full", "AUTO_ROBOT"},
                {"Коробка", "– Вариатор", "transmission_full", "AUTO_VARIATOR"},
                {"Коробка", "Механическая", "transmission_full", "MECHANICAL"},

                {"Двигатель", "Бензин", "engine_type", "GASOLINE"},
                {"Двигатель", "Дизель", "engine_type", "DIESEL"},
                {"Двигатель", "Гибрид", "engine_type", "HYBRID"},
                {"Двигатель", "Электро", "engine_type", "ELECTRO"},
                {"Двигатель", "Турбированный", "engine_type", "ENGINE_TURBO"},
                {"Двигатель", "Атмосферный", "engine_type", "ENGINE_NONE"},
                {"Двигатель", "Газобаллонное оборудование", "engine_type", "LPG"},

                {"Привод", "Передний", "gear_type", "FORWARD_CONTROL"},
                {"Привод", "Задний", "gear_type", "REAR_DRIVE"},
                {"Привод", "Полный", "gear_type", "ALL_WHEEL_DRIVE"},

                {"Руль", "Левый", "steering_wheel", "LEFT"},
                {"Руль", "Правый", "steering_wheel", "RIGHT"},

                {"К-во мест", "2 места", "seats", "2"},
                {"К-во мест", "4-5 мест", "seats", "4_5"},
                {"К-во мест", "6-8 мест", "seats", "6_7_8"}
        });
    }

    @Test
    @DisplayName("Селекторы")
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    public void shouldSeeSelectorFiltersInUrl() throws InterruptedException {
        basePageSteps.selectOption(basePageSteps.onFiltersPage().selector(filterName), option);
        waitSomething(500, TimeUnit.MILLISECONDS);
        basePageSteps.onFiltersPage().applyFiltersButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCatalogMainPage().filter().waitUntil(isDisplayed(), 3);

        urlSteps.testing().path(CATALOG).path(CARS).path(ALL).addParam(paramName, paramValue).shouldNotSeeDiff();
    }
}
