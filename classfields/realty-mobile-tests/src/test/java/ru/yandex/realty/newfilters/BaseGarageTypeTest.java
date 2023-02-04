package ru.yandex.realty.newfilters;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.GARAZH;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.mobile.element.main.FiltersBlock.FIELD_GARAGE_TYPE;
import static ru.yandex.realty.mobile.page.MainPage.KVARTIRU_OPTION;

@DisplayName("Фильтры Гаража. Тип гаража")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebMobileModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class BaseGarageTypeTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps user;

    @Inject
    private UrlSteps urlSteps;


    @Parameterized.Parameter
    public String type;

    @Parameterized.Parameter(1)
    public String value;

    @Parameterized.Parameters(name = "выбираем тип гаража «{0}»")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {"Бокс", "BOX"},
                {"Гараж", "GARAGE"},
                {"Машиноместо", "PARKING_PLACE"}
        });
    }


    @Before
    public void garageCategory() {
        urlSteps.testing().path(SPB_I_LO).open();
        user.onMobileMainPage().searchFilters().waitUntil(isDisplayed());

        user.selectOption(user.onMobileMainPage().searchFilters().selector(KVARTIRU_OPTION), "Гараж или машиноместо");
    }

    @Test
    public void shouldSelectGarageType() {
        user.onMobileMainPage().searchFilters().byName(FIELD_GARAGE_TYPE).click();
        user.onMobileMainPage().searchFilters().filterPopup().tumbler(type).click();
        user.onMobileMainPage().searchFilters().filterPopup().showButton().click();
        user.onMobileMainPage().searchFilters().applyFiltersButton().click();

        urlSteps.path(KUPIT).path(GARAZH).queryParam("garageType", value).shouldNotDiffWithWebDriverUrl();
    }
}
