package ru.yandex.realty.filters.mainpage;

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
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.GARAZH;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAINFILTERS;
import static ru.yandex.realty.element.saleads.FiltersBlock.KVARTIRU_BUTTON;
import static ru.yandex.realty.element.saleads.FiltersBlock.TYPE_BUTTON;

/**
 * @author kantemirov
 */
@DisplayName("Главная страница. Фильтры поиска для покупки гаража")
@Feature(MAINFILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class BuyGarageFiltersTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps user;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String label;

    @Parameterized.Parameter(1)
    public String expected;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {"Бокс", "BOX"},
                {"Гараж", "GARAGE"},
                {"Машиноместо", "PARKING_PLACE"}
        });
    }

    @Before
    public void openSaleAdsPage() {
        urlSteps.testing().path(SPB_I_LO).open();
        user.onMainPage().filters().waitUntil(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Главная страница. Параметр типа гаража")
    public void shouldSeeBuyTypeOfGarageUrl() {
        user.onMainPage().filters().button(KVARTIRU_BUTTON).click();
        user.onMainPage().filters().selectPopup().item("Гараж").click();
        user.onMainPage().filters().button(TYPE_BUTTON).click();
        user.onMainPage().filters().selectPopup().item(label).click();
        user.onMainPage().filters().submitButton().click();
        urlSteps.path(KUPIT).path(GARAZH).queryParam("garageType", expected)
                .shouldNotDiffWithWebDriverUrl();
    }
}
