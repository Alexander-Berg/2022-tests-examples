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

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CALLS;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_MANAGER;
import static ru.auto.tests.desktop.consts.QueryParams.CLIENT_ID;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

//import io.qameta.allure.Parameter;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Менеджер. Звонки. Фильтры-селекты")
@GuiceModules(CabinetTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CallsFiltersSelectsTest {

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
    public String query;

    @Parameterized.Parameters(name = "{index}: {0} {1} {2}")
    public static String[][] getParameters() {
        return new String[][]{
                {"Все звонки", "Целевые", "call_target=TARGET_GROUP"},
                {"Все звонки", "Нецелевые", "call_target=NONTARGET_GROUP"},

                {"Все категории", "Легковые с пробегом", "category=CARS&section=USED"},
                {"Все категории", "Легковые новые", "category=CARS&section=NEW"},
                {"Все категории", "Мото", "category=MOTO"},
                {"Все категории", "Коммерческие", "category=TRUCKS"}
        };
    }

    @Before
    public void before() {
        mockRule.newMock().with("cabinet/Session/Manager",
                "cabinet/ApiAccessClientManager",
                "cabinet/CommonCustomerGetManager",
                "cabinet/CalltrackingCarsUsed",
                "cabinet/CalltrackingAggregatedCarsUsed",
                "cabinet/CalltrackingCarsNew",
                "cabinet/CalltrackingAggregatedCarsNew",
                "cabinet/CalltrackingMoto",
                "cabinet/CalltrackingAggregatedMoto",
                "cabinet/CalltrackingTrucks",
                "cabinet/CalltrackingAggregatedTrucks",
                "cabinet/CalltrackingTarget",
                "cabinet/CalltrackingNonTarget",
                "cabinet/CalltrackingAggregatedTarget",
                "cabinet/CalltrackingAggregatedNonTarget",
                "cabinet/Calltracking",
                "cabinet/CalltrackingAggregated").post();

        urlSteps.subdomain(SUBDOMAIN_MANAGER).path(CALLS).addParam(CLIENT_ID, "16453").open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Фильтры-селекты")
    public void shouldFilter() {
        basePageSteps.onCallsPage().filters().selectItem(selectName, selectItem);
        basePageSteps.onCallsPage().filters().select(selectItem).waitUntil(isDisplayed());
        urlSteps.subdomain(SUBDOMAIN_MANAGER).path(CALLS).replaceQuery(query)
                .addParam(CLIENT_ID, "16453").shouldNotSeeDiff();
        basePageSteps.onCallsPage().callsList().waitUntil(hasSize(2));
    }
}