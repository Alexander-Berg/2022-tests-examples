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
import static ru.auto.tests.desktop.consts.Pages.CONSTRUCTION;
import static ru.auto.tests.desktop.consts.Pages.CRANE;
import static ru.auto.tests.desktop.consts.Pages.DREDGE;
import static ru.auto.tests.desktop.consts.Pages.LCV;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.MUNICIPAL;
import static ru.auto.tests.desktop.consts.Pages.RUSSIA;
import static ru.auto.tests.desktop.consts.Pages.SCOOTERS;
import static ru.auto.tests.desktop.consts.Pages.SNOWMOBILE;
import static ru.auto.tests.desktop.consts.Pages.TRAILER;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

//import io.qameta.allure.Parameter;

@DisplayName("Листинг - расширенный фильтр - чекбоксы")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class AdvancedFiltersCheckboxesMotoCommerceTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String section;

    @Parameterized.Parameter(2)
    public String checkboxTitle;

    @Parameterized.Parameter(3)
    public String param;

    //@Parameter("Значение параметра")
    @Parameterized.Parameter(4)
    public String paramValue;

    @Parameterized.Parameters(name = "name = {index}: {0} {1} {2}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {LCV, ALL, "Обмен", "exchange_group", "POSSIBLE"},
                {LCV, ALL, "Торг", "haggle", "HAGGLE_POSSIBLE"},
                {LCV, ALL, "В наличии", "in_stock", "IN_STOCK"},
                {LCV, ALL, "Без доставки", "with_delivery", "NONE"},
                {LCV, ALL, "Антиблокировочная система (ABS)", "catalog_equipment", "abs"},

                {TRUCK, ALL, "Обмен", "exchange_group", "POSSIBLE"},
                {TRUCK, ALL, "Торг", "haggle", "HAGGLE_POSSIBLE"},
                {TRUCK, ALL, "В наличии", "in_stock", "IN_STOCK"},
                {TRUCK, ALL, "Без доставки", "with_delivery", "NONE"},
                {TRUCK, ALL, "Антиблокировочная система (ABS)", "catalog_equipment", "abs"},

                {ARTIC, ALL, "Обмен", "exchange_group", "POSSIBLE"},
                {ARTIC, ALL, "Торг", "haggle", "HAGGLE_POSSIBLE"},
                {ARTIC, ALL, "В наличии", "in_stock", "IN_STOCK"},
                {ARTIC, ALL, "Без доставки", "with_delivery", "NONE"},
                {ARTIC, ALL, "Антиблокировочная система (ABS)", "catalog_equipment", "abs"},

                {BUS, ALL, "Обмен", "exchange_group", "POSSIBLE"},
                {BUS, ALL, "Торг", "haggle", "HAGGLE_POSSIBLE"},
                {BUS, ALL, "В наличии", "in_stock", "IN_STOCK"},
                {BUS, ALL, "Без доставки", "with_delivery", "NONE"},
                {BUS, ALL, "Антиблокировочная система (ABS)", "catalog_equipment", "abs"},

                {TRAILER, ALL, "Обмен", "exchange_group", "POSSIBLE"},
                {TRAILER, ALL, "Торг", "haggle", "HAGGLE_POSSIBLE"},
                {TRAILER, ALL, "В наличии", "in_stock", "IN_STOCK"},
                {TRAILER, ALL, "Без доставки", "with_delivery", "NONE"},
                {TRAILER, ALL, "Антиблокировочная система (ABS)", "catalog_equipment", "abs"},

                {AGRICULTURAL, ALL, "Обмен", "exchange_group", "POSSIBLE"},
                {AGRICULTURAL, ALL, "Торг", "haggle", "HAGGLE_POSSIBLE"},
                {AGRICULTURAL, ALL, "В наличии", "in_stock", "IN_STOCK"},
                {AGRICULTURAL, ALL, "Без доставки", "with_delivery", "NONE"},

                {CONSTRUCTION, ALL, "Обмен", "exchange_group", "POSSIBLE"},
                {CONSTRUCTION, ALL, "Торг", "haggle", "HAGGLE_POSSIBLE"},
                {CONSTRUCTION, ALL, "В наличии", "in_stock", "IN_STOCK"},
                {CONSTRUCTION, ALL, "Без доставки", "with_delivery", "NONE"},

                {AUTOLOADER, ALL, "Обмен", "exchange_group", "POSSIBLE"},
                {AUTOLOADER, ALL, "Торг", "haggle", "HAGGLE_POSSIBLE"},
                {AUTOLOADER, ALL, "В наличии", "in_stock", "IN_STOCK"},
                {AUTOLOADER, ALL, "Без доставки", "with_delivery", "NONE"},

                {CRANE, ALL, "Обмен", "exchange_group", "POSSIBLE"},
                {CRANE, ALL, "Торг", "haggle", "HAGGLE_POSSIBLE"},
                {CRANE, ALL, "В наличии", "in_stock", "IN_STOCK"},
                {CRANE, ALL, "Без доставки", "with_delivery", "NONE"},

                {DREDGE, ALL, "Обмен", "exchange_group", "POSSIBLE"},
                {DREDGE, ALL, "Торг", "haggle", "HAGGLE_POSSIBLE"},
                {DREDGE, ALL, "В наличии", "in_stock", "IN_STOCK"},
                {DREDGE, ALL, "Без доставки", "with_delivery", "NONE"},

                {BULLDOZERS, ALL, "Обмен", "exchange_group", "POSSIBLE"},
                {BULLDOZERS, ALL, "Торг", "haggle", "HAGGLE_POSSIBLE"},
                {BULLDOZERS, ALL, "В наличии", "in_stock", "IN_STOCK"},
                {BULLDOZERS, ALL, "Без доставки", "with_delivery", "NONE"},

                {MUNICIPAL, ALL, "Обмен", "exchange_group", "POSSIBLE"},
                {MUNICIPAL, ALL, "Торг", "haggle", "HAGGLE_POSSIBLE"},
                {MUNICIPAL, ALL, "В наличии", "in_stock", "IN_STOCK"},
                {MUNICIPAL, ALL, "Без доставки", "with_delivery", "NONE"},

                {MOTORCYCLE, ALL, "В наличии", "in_stock", "IN_STOCK"},
                {MOTORCYCLE, ALL, "Обмен", "exchange_group", "POSSIBLE"},
                {MOTORCYCLE, ALL, "Антиблокировочная система (ABS)", "catalog_equipment", "abs"},

                {SCOOTERS, ALL, "В наличии", "in_stock", "IN_STOCK"},
                {SCOOTERS, ALL, "Обмен", "exchange_group", "POSSIBLE"},
                {SCOOTERS, ALL, "Электростартер", "catalog_equipment", "electric-starter"},

                {ATV, ALL, "В наличии", "in_stock", "IN_STOCK"},
                {ATV, ALL, "Обмен", "exchange_group", "POSSIBLE"},
                {ATV, ALL, "Антиблокировочная система (ABS)", "catalog_equipment", "abs"},

                {SNOWMOBILE, ALL, "В наличии", "in_stock", "IN_STOCK"},
                {SNOWMOBILE, ALL, "Обмен", "exchange_group", "POSSIBLE"},
                {SNOWMOBILE, ALL, "Электростартер", "catalog_equipment", "electric-starter"}
        });
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор параметра-чекбокса")
    public void shouldSeeCheckboxParamInUrl() {
        urlSteps.testing().path(RUSSIA).path(category).path(section).open();
        basePageSteps.onListingPage().filter().showAdvancedFilters();

        basePageSteps.onListingPage().getSale(0).hover();
        basePageSteps.onListingPage().filter().checkbox(checkboxTitle).should(isDisplayed()).click();
        urlSteps.addParam(param, paramValue).shouldNotSeeDiff();
        basePageSteps.onListingPage().filter().resultsButton().waitUntil(isDisplayed()).click();
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onListingPage().filter().showAdvancedFilters();
        basePageSteps.onListingPage().filter().checkboxChecked(checkboxTitle).waitUntil(isDisplayed());
        basePageSteps.onListingPage().salesList().waitUntil(hasSize(greaterThan(0)));
    }
}