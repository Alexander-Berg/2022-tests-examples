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

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CALLS;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;

//import io.qameta.allure.Parameter;

@Epic(CABINET_DEALER)
@Feature(AutoruFeatures.CALLS)
@DisplayName("Кабинет дилера. Звонки. Фильтры-чекбоксы")
@GuiceModules(CabinetTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CallsFiltersCheckboxesTest {

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
    public String checkboxName;

    @Parameterized.Parameter(1)
    public String queryParamName;

    @Parameterized.Parameter(2)
    public String queryParamValue;

    @Parameterized.Parameters(name = "{index}: {0} {1} {2}")
    public static String[][] getParameters() {
        return new String[][]{
                {"Уникальные", "unique", "ONLY_UNIQUE"},
                {"Обратные", "callback", "CALLBACK_GROUP"}
        };
    }

    @Before
    public void before() {
        mockRule.newMock().with("cabinet/Session/DirectDealerMoscow",
                "cabinet/DealerAccount",
                "cabinet/CommonCustomerGet",
                "cabinet/ClientsGet",
                "cabinet/DealerCampaigns",
                "cabinet/ApiAccessClient",
                "cabinet/CalltrackingUnique",
                "cabinet/CalltrackingCallback",
                "cabinet/CalltrackingAggregatedUnique",
                "cabinet/CalltrackingAggregatedCallback",
                "cabinet/Calltracking",
                "cabinet/CalltrackingAggregated").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(CALLS).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Фильтры-селекты")
    public void shouldFilter() {
        basePageSteps.onCallsPage().filters().checkbox(checkboxName).click();
        urlSteps.addParam(queryParamName, queryParamValue).shouldNotSeeDiff();
        basePageSteps.onCallsPage().callsList().waitUntil(hasSize(2));
    }
}
