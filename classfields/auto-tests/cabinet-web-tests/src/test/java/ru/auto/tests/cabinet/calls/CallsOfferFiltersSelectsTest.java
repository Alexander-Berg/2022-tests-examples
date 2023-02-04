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
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CALLS;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.QueryParams.BODY_TYPE;
import static ru.auto.tests.desktop.consts.QueryParams.TRANSMISSION;
import static ru.auto.tests.desktop.consts.QueryParams.YEAR_FROM;
import static ru.auto.tests.desktop.consts.QueryParams.YEAR_TO;

@Epic(CABINET_DEALER)
@Feature(AutoruFeatures.CALLS)
@DisplayName("Фильтры-селекты блока фильтрации по офферам")
@GuiceModules(CabinetTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CallsOfferFiltersSelectsTest {

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
    public String selectName;

    @Parameterized.Parameter(1)
    public String selectItem;

    @Parameterized.Parameter(2)
    public String paramName;

    @Parameterized.Parameter(3)
    public String paramValue;

    @Parameterized.Parameters(name = "{index}: {0} {1} {2}")
    public static String[][] getParameters() {
        return new String[][]{
                {"Кузов", "Седан ", BODY_TYPE, "SEDAN"},
                {"Коробка", "Механическая ", TRANSMISSION, "MECHANICAL"},
                {"Год от", "2018", YEAR_FROM, "2018"},
                {"до", "2020", YEAR_TO, "2020"}
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
    @DisplayName("Фильтры-селекты блока фильтрации по офферам")
    public void shouldSeeSelectFilters() {
        basePageSteps.onCallsPage().filters().offerFilters().selectItem(selectName, selectItem);

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(CALLS).addParam(paramName, paramValue).shouldNotSeeDiff();
    }

}
