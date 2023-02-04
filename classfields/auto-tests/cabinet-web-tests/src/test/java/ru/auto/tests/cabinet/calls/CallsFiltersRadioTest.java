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
@DisplayName("Кабинет дилера. Звонки. Фильтры-радио")
@GuiceModules(CabinetTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CallsFiltersRadioTest {

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
    public String radioName;

    @Parameterized.Parameter(1)
    public String query;

    @Parameterized.Parameters(name = "{index}: {0} {1}")
    public static String[][] getParameters() {
        return new String[][]{
                {"Принятые", "ANSWERED_GROUP"},
                {"Пропущенные", "MISSED_GROUP"}
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
                "cabinet/CalltrackingAnswered",
                "cabinet/CalltrackingMissed",
                "cabinet/Calltracking",
                "cabinet/CalltrackingAggregatedAnswered",
                "cabinet/CalltrackingAggregatedMissed",
                "cabinet/CalltrackingAggregated").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(CALLS).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Фильтры-радио")
    public void shouldFilter() {
        basePageSteps.onCallsPage().filters().radioButton(radioName).click();
        urlSteps.addParam("call_result", query).shouldNotSeeDiff();
        basePageSteps.onCallsPage().callsList().waitUntil(hasSize(2));
    }
}
