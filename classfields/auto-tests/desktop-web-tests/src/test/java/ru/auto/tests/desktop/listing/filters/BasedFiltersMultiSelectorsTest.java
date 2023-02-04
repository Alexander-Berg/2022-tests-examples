package ru.auto.tests.desktop.listing.filters;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.LCV;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Расширенные фильтры - тэги")
@Feature(AutoruFeatures.FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class BasedFiltersMultiSelectorsTest {

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
    public String select;

    //@Parameter("Название параметра")
    @Parameterized.Parameter(2)
    public String paramName;

    //@Parameter("Первое значение селектора")
    @Parameterized.Parameter(3)
    public String selectValue_1;

    //@Parameter("Второе значение селектора")
    @Parameterized.Parameter(4)
    public String selectValue_2;

    //@Parameter("Первое значение параметра")
    @Parameterized.Parameter(5)
    public String paramQueryValue1;

    //@Parameter("Второе значение параметра")
    @Parameterized.Parameter(6)
    public String paramQueryValue2;

    @Parameterized.Parameters(name = "name = {index}: {0} {1}")
    public static Collection<Object> getParameters() {
        return asList(new Object[][]{
                {CARS, "Кузов", "body_type_group", "Седан ", "\u00a0\u00a0Хэтчбек 3 дв. ", "SEDAN", "HATCHBACK_3_DOORS"},
                {CARS, "Двигатель", "engine_group", "Бензин", "Дизель", "GASOLINE", "DIESEL"},
                {CARS, "Привод", "gear_type", "Передний", "Задний", "FORWARD_CONTROL", "REAR_DRIVE"},
                {CARS, "Коробка", "transmission", "\u00a0\u00a0Автоматическая ", "Механическая ", "AUTOMATIC", "MECHANICAL"},

                {LCV, "Двигатель", "engine_type", "Гибрид", "Дизель+газ", "HYBRID", "DIESEL_GAS"},

                {MOTORCYCLE, "Тип мотоцикла", "moto_type", "\u00a0\u00a0Allround ", "\u00a0\u00a0Внедорожный Эндуро ",
                        "ALLROUND", "OFFROAD_ENDURO"},
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(category).path(ALL).open();
    }

    @Test
    @DisplayName("Мультивыбор параметров в фильтре (селекты)")
    @Category({Regression.class})
    @Owner(NATAGOLOVKINA)
    public void shouldMultiChangeSelect() {
        basePageSteps.onListingPage().filter().select(select).hover();
        basePageSteps.scrollDown(basePageSteps.onListingPage().filter()
                .select(select).getSize().getHeight() * 3);
        basePageSteps.onListingPage().filter().selectItem(select, selectValue_1, selectValue_2);
        urlSteps.addParam(paramName, paramQueryValue1).addParam(paramName, paramQueryValue2).shouldNotSeeDiff();
        basePageSteps.onListingPage().filter().resultsButton().waitUntil(isDisplayed()).click();

        urlSteps.shouldNotSeeDiff();
        basePageSteps.onListingPage().filter().should(isDisplayed());

        basePageSteps.refresh();

        urlSteps.shouldNotSeeDiff();
        basePageSteps.onListingPage().filter().should(isDisplayed());
    }
}
