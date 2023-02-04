package ru.yandex.realty.newfilters;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
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
import static ru.yandex.realty.consts.Filters.DOM;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.mobile.element.main.FiltersBlock.FIELD_HOUSE_TYPE;
import static ru.yandex.realty.mobile.page.MainPage.DOM_OPTION;
import static ru.yandex.realty.mobile.page.MainPage.KVARTIRU_OPTION;

@DisplayName("Фильтры Дома. Тип дома")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebMobileModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class BaseHouseTypeTest {

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

    @Parameterized.Parameters(name = "выбираем тип дома «{0}»")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {"Отдельный дом", "HOUSE"},
//                {"Дуплекс", "DUPLEX"},
        });
    }


    @Before
    public void before() {
        urlSteps.testing().path(SPB_I_LO).open();
        user.onMobileMainPage().searchFilters().waitUntil(isDisplayed());

        user.selectOption(user.onMobileMainPage().searchFilters().selector(KVARTIRU_OPTION), DOM_OPTION);
    }

    @Test
    @Owner(KANTEMIROV)
    public void shouldSelectHouseType() {
        user.onMobileMainPage().searchFilters().byName(FIELD_HOUSE_TYPE).click();
        user.onMobileMainPage().searchFilters().filterPopup().tumbler(type).click();
        user.onMobileMainPage().searchFilters().filterPopup().showButton().click();
        user.onMobileMainPage().searchFilters().applyFiltersButton().click();

        urlSteps.path(KUPIT).path(DOM).queryParam("houseType", value).shouldNotDiffWithWebDriverUrl();
    }
}
