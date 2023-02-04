package ru.auto.tests.cabinet.multiposting;

import com.carlosbecker.guice.GuiceModules;
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
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CabinetOffersPageSteps;

import javax.inject.Inject;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import static java.lang.String.format;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALES;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Мультипостинг")
@RunWith(Parameterized.class)
@GuiceModules(CabinetTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class MultipostingInputFiltersTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private CabinetOffersPageSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String inputName;

    @Parameterized.Parameter(1)
    public String inputText;

    @Parameterized.Parameter(2)
    public String urlParam;

    @Parameterized.Parameter(3)
    public String query;

    @Parameterized.Parameters(name = "{index}: {2} {1}")
    public static Object[][] getTestParameters() {
        long warehouseDaysTo = 3;
        long warehouseDaysFrom = 5;
        Instant now = Instant.now();
        Instant warehouseDateTo = now.minus(warehouseDaysTo - 1, ChronoUnit.DAYS);
        Instant warehouseDateFrom = now.minus(warehouseDaysFrom - 1, ChronoUnit.DAYS);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());

        return new Object[][]{
                {"Цена от, ₽", "100000", "price_from", "%s=100000&resetSales=true"},
                {"до", "500000", "price_to", "%s=500000&resetSales=true"},
                {"Год, c", "2010", "year_from", "%s=2010&resetSales=true"},
                {"На складе, от", String.valueOf(warehouseDaysTo), "create_date_to",
                        "%s=" + format("%s&resetSales=true", formatter.format(warehouseDateTo))},
                {"до, дней", String.valueOf(warehouseDaysFrom), "create_date_from",
                        "%s=" + format("%s&resetSales=true", formatter.format(warehouseDateFrom))},
                {"VIN номер или несколько через запятую", "RTY46788765444457",
                        "vin", "%s=RTY46788765444457&resetSales=true"}
        };
    }

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthDealer"),
                stub("cabinet/ApiAccessClient"),
                stub("cabinet/CommonCustomerGet"),
                stub("cabinet/ClientsGetMultipostingEnabled"),
                stub("cabinet/DealerInfoMultipostingEnabled"),
                stub("cabinet/UserOffersCarsCount"),
                stub("cabinet/UserOffersCarsUsedMultipostingActive"),
                stub("cabinet/UserOffersCarsUsedMultipostingPriceFrom"),
                stub("cabinet/UserOffersCarsUsedMultipostingPriceTo"),
                stub("cabinet/UserOffersCarsUsedMultipostingYearFrom"),
                stub("cabinet/UserOffersCarsUsedMultipostingWarehouseFrom"),
                stub("cabinet/UserOffersCarsUsedMultipostingWarehouseTo"),
                stub("cabinet/UserOffersCarsUsedMultipostingVinFilter")
        ).create();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(SALES).path(CARS).path(USED).open();
    }

    @Test
    @Category({Regression.class})
    @Owner(TIMONDL)
    @DisplayName("Фильтрация, инпуты")
    public void shouldFilter() {
        String title = steps.onCabinetOffersPage().snippet(0).title().getText();
        steps.onCabinetOffersPage().salesFiltersBlock().input(inputName, inputText);
        steps.onCabinetOffersPage().salesFiltersBlock().button("Показать 7\u00a0объявлений").click();

        urlSteps.replaceQuery(format(query, urlParam)).shouldNotSeeDiff();
        steps.onCabinetOffersPage().snippets().should(hasSize(greaterThan(0)));
        steps.onCabinetOffersPage().snippet(0).should(not(hasText(title)));
    }
}
