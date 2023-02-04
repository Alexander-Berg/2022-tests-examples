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
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

//import io.qameta.allure.Parameter;

@Epic(CABINET_DEALER)
@Feature(AutoruFeatures.CALLS)
@DisplayName("Кабинет дилера. Звонки. Поиск по тегу")
@GuiceModules(CabinetTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CallsFiltersTagsTest {

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
    public String tag;

    @Parameterized.Parameters(name = "{index}: {0}")
    public static String[][] getParameters() {
        return new String[][]{
                {"111"}
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
                "cabinet/CalltrackingAggregatedTags",
                "cabinet/CalltrackingAggregated",
                "cabinet/CalltrackingTags",
                "cabinet/Calltracking",
                "cabinet/CalltrackingCallTagSuggest").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(CALLS).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Поиск по тегу")
    public void shouldFilter() {
        basePageSteps.onCallsPage().filters().input("Поиск по тегам", tag);
        basePageSteps.onCallsPage().filters().selectPopup().item(tag).click();
        urlSteps.addParam("tag", tag).shouldNotSeeDiff();
        basePageSteps.onCallsPage().filters().tagsList().waitUntil(hasSize(1));
        basePageSteps.onCallsPage().filters().getTag(0).should(hasText(tag));
        basePageSteps.onCallsPage().callsList().waitUntil(hasSize(2));
    }
}
