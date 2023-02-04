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

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.AGRICULTURAL;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.AUTOLOADER;
import static ru.auto.tests.desktop.consts.Pages.BULLDOZERS;
import static ru.auto.tests.desktop.consts.Pages.CONSTRUCTION;
import static ru.auto.tests.desktop.consts.Pages.DREDGE;
import static ru.auto.tests.desktop.consts.Pages.LCV;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MUNICIPAL;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Фильтры - тип комТС")
@Feature(AutoruFeatures.FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ParamsCommerceTypeTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    public BasePageSteps basePageSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String type;

    @Parameterized.Parameter(2)
    public String commerceType;

    @Parameterized.Parameter(3)
    public String paramName;

    @Parameterized.Parameter(4)
    public String paramValue;

    @Parameterized.Parameters(name = "name = {index}: {0} {1} {2} {3} {4}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {LCV, "Тип кузова", "Борт-тент", "light_truck_type", "AWNING"},

                {AGRICULTURAL, "Тип техники", "Комбайн", "agricultural_type", "COMBAIN_HARVESTER"},

                {CONSTRUCTION, "Тип техники", "Бурильно-сваебойная машина", "construction_type", "DRILLING_PILING_MACHINE"},

                {AUTOLOADER, "Тип автопогрузчика", "Вилочный электропогрузчик", "autoloader_type", "FORKLIFTS_ELECTRO"},

                {DREDGE, "Тип экскаватора", "Планировщик", "dredge_type", "PLANNER_EXCAVATOR"},

                {BULLDOZERS, "Тип", "Колесный бульдозер", "bulldozer_type", "WHEELS_BULLDOZER"},

                {MUNICIPAL, "Тип техники", "Мусоровоз", "municipal_type", "GARBAGE_TRUCK"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(category).path(ALL).open();
        basePageSteps.onListingPage().filters().paramsButton().click();
        basePageSteps.hideApplyFiltersButton();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор типа кузова")
    public void shouldSelectBodyType() {
        basePageSteps.onListingPage().paramsPopup().popupButton(type).hover().click();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onListingPage().filtersPopup(type).item(commerceType).checkbox().hover().click();
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onListingPage().filtersPopup(type).applyFiltersButton().click();
        urlSteps.testing().path(MOSKVA).path(category).path(ALL).addParam(paramName, paramValue).shouldNotSeeDiff();
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onListingPage().paramsPopup().popupButton(type).should(hasText(format("%s\n%s", type, commerceType)));
        basePageSteps.onListingPage().paramsPopup().applyFiltersButton().click();
        urlSteps.shouldNotSeeDiff();
    }
}
