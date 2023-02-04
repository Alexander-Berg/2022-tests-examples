package ru.auto.tests.cabinet.calls;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Epic;
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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.CALLS;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.QueryParams.PRICE_FROM;
import static ru.auto.tests.desktop.consts.QueryParams.PRICE_TO;
import static ru.auto.tests.desktop.consts.QueryParams.VIN;

@Epic(CABINET_DEALER)
@Feature(AutoruFeatures.CALLS)
@DisplayName("Фильтры-инпуты блока фильтрации по офферам")
@GuiceModules(CabinetTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CallsOfferFiltersInputTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String inputName;

    @Parameterized.Parameter(1)
    public String inputValue;

    @Parameterized.Parameter(2)
    public String queryParam;

    @Parameterized.Parameters(name = "{index}: {0} {1} {2}")
    public static String[][] getParameters() {
        return new String[][]{
                {"Цена от, ₽", "20000", PRICE_FROM},
                {"до", "500000", PRICE_TO},
                {"VIN", "XWEGN412BF0004265", VIN}
        };
    }


    @Before
    public void before() {
        mockRule.newMock().with("cabinet/Session/DirectDealerMoscow",
                "cabinet/DealerAccount",
                "cabinet/DealerTariff/AllTariffs",
                "cabinet/CommonCustomerGet",
                "cabinet/ClientsGet",
                "cabinet/DealerCampaigns",
                "cabinet/ApiAccessClient",
                "cabinet/Calltracking",
                "cabinet/CalltrackingSettings",
                "cabinet/CalltrackingAggregated").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(CALLS).open();
        basePageSteps.onCallsPage().filters().allParameters().click();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Фильтры-инпуты блока фильтрации по офферам")
    public void shouldSeeInputFilters() {
        basePageSteps.onCallsPage().filters().offerFilters().input(inputName, inputValue);

        urlSteps.addParam(queryParam, inputValue).shouldNotSeeDiff();
    }

}
