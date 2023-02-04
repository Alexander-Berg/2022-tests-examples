package ru.auto.tests.cabinet.crm.call;

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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CALLS;
import static ru.auto.tests.desktop.consts.Pages.NUMBERS;
import static ru.auto.tests.desktop.consts.Pages.SETTINGS;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_MANAGER;
import static ru.auto.tests.desktop.consts.QueryParams.CLIENT_ID;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Менеджер. Звонки. Вкладки")
@GuiceModules(CabinetTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CallsTabsTest {

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
    public String tabName;

    @Parameterized.Parameter(1)
    public String path;

    @Parameterized.Parameters(name = "{index}: {0} {1}")
    public static String[][] getParameters() {
        return new String[][]{
                {"Подменные номера", NUMBERS},
                {"Настройки", SETTINGS}
        };
    }

    @Before
    public void before() {
        mockRule.newMock().with("cabinet/Session/Manager",
                "cabinet/ApiAccessClientManager",
                "cabinet/CommonCustomerGetManager",
                "cabinet/DealerPhonesRedirect",
                "cabinet/DealerTariff/AllTariffs",
                "cabinet/Calltracking",
                "cabinet/CalltrackingAggregated",
                "cabinet/CalltrackingSettings").post();

        urlSteps.subdomain(SUBDOMAIN_MANAGER).path(CALLS).addParam(CLIENT_ID, "16453").open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по вкладке")
    public void shouldClickTab() {
        basePageSteps.onCallsPage().tabs().button(tabName).click();
        urlSteps.path(path).shouldNotSeeDiff();
    }
}