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
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.ATV;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Фильтры - тип кузова комТС")
@Feature(AutoruFeatures.FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ParamsMotoTypeTest {

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
    public String motoType;

    @Parameterized.Parameter(3)
    public String paramName;

    @Parameterized.Parameter(4)
    public String paramValue;

    @Parameterized.Parameters(name = "name = {index}: {0} {1} {2}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {MOTORCYCLE, "Тип мотоцикла", "Все внедорожные", "moto_type", "OFF_ROAD_GROUP"},
                {MOTORCYCLE, "Тип мотоцикла", "Туристические", "moto_type", "TOURISM"},

                {ATV, "Тип вездехода", "Детский", "atv_type", "CHILDISH"}
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
    @DisplayName("Выбор типа мотоцикла")
    public void shouldSelectMotoType() {
        basePageSteps.onListingPage().paramsPopup().popupButton(type).hover().click();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onListingPage().filtersPopup(type).item(motoType).checkbox().hover().click();
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onListingPage().filtersPopup(type).applyFiltersButton().click();
        urlSteps.testing().path(MOSKVA).path(category).path(ALL).addParam(paramName, paramValue).shouldNotSeeDiff();
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onListingPage().paramsPopup().popupButton(type).should(hasText(format("%s\n%s", type, motoType)));
        basePageSteps.onListingPage().paramsPopup().applyFiltersButton().click();
        urlSteps.shouldNotSeeDiff();
    }
}
