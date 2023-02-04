package ru.yandex.realty.filters.mainpage;

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
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.DOM;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAINFILTERS;
import static ru.yandex.realty.element.saleads.FiltersBlock.KVARTIRU_BUTTON;
import static ru.yandex.realty.element.saleads.FiltersBlock.TYPE_BUTTON;

/**
 * @author kantemirov
 */
@DisplayName("Главная страница. Фильтры поиска для покупки дома. Тип")
@Feature(MAINFILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class BuyHouseTypeFiltersChpuTest {

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
                {"Часть дома", "/chast-doma/"},
                {"Таунхаус", "/townhouse/"},
                {"Дуплекс", "/duplex/"}
        });
    }

    @Before
    public void openSaleAdsPage() {
        urlSteps.testing().path(MOSKVA).open();
        user.onMainPage().filters().waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Главная страница. Параметр «тип дома»")
    public void shouldSeeHouseTypeInUrl() {
        user.onMainPage().filters().button(KVARTIRU_BUTTON).click();
        user.onMainPage().filters().selectPopup().item("Дом").click();
        user.onMainPage().filters().button(TYPE_BUTTON).click();
        user.onMainPage().filters().selectPopup().item(label).click();
        user.onMainPage().filters().submitButton().click();
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(DOM).path(expected).shouldNotDiffWithWebDriverUrl();
    }
}
