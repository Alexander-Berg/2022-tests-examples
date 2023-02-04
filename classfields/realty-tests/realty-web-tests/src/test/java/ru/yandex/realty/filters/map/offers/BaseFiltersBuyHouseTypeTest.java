package ru.yandex.realty.filters.map.offers;

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
import static org.hamcrest.Matchers.hasItem;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.DOM;
import static ru.yandex.realty.consts.Filters.KARTA;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.MAPFILTERS;
import static ru.yandex.realty.element.saleads.FiltersBlock.TYPE_BUTTON;
import static ru.yandex.realty.step.BasePageSteps.urlParam;

@DisplayName("Карта. Базовые фильтры поиска по объявлениям")
@Feature(MAPFILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class BaseFiltersBuyHouseTypeTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String label;

    @Parameterized.Parameter(1)
    public String expected;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> testParams() {
        return asList(new Object[][]{
                {"Отдельный дом", "HOUSE"},
                {"Часть дома", "PARTHOUSE"},
                {"Таунхаус", "TOWNHOUSE"},
                {"Дуплекс", "DUPLEX"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(DOM).path(KARTA).open();
        basePageSteps.onMapPage().filters().waitUntil(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Параметр «тип дома»")
    public void shouldSeeHouseTypeInUrl() {
        basePageSteps.onMapPage().filters().button(TYPE_BUTTON).click();
        basePageSteps.onMapPage().filters().selectPopup().item(label).click();
        basePageSteps.loaderWait();
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(DOM).queryParam("houseType", expected).path(KARTA)
                .shouldNotDiffWithWebDriverUrl();
        basePageSteps.shouldSeeRequestInBrowser(hasItem(urlParam("houseType", expected)));
    }
}
