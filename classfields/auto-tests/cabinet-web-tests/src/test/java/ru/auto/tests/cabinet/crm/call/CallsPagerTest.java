package ru.auto.tests.cabinet.crm.call;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CALLS;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_MANAGER;
import static ru.auto.tests.desktop.consts.QueryParams.CLIENT_ID;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Менеджер. Звонки. Пагинация")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class CallsPagerTest {

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

    @Before
    public void before() {
        mockRule.newMock().with("cabinet/Session/Manager",
                "cabinet/DealerTariff",
                "cabinet/ApiAccessClientManager",
                "cabinet/CommonCustomerGetManager",
                "cabinet/Calltracking",
                "cabinet/CalltrackingPage2",
                "cabinet/CalltrackingAggregated",
                "cabinet/CalltrackingSettings").post();

        urlSteps.subdomain(SUBDOMAIN_MANAGER).path(CALLS).addParam(CLIENT_ID, "16453").open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Переход на следующую страницу по кнопке «Следующая»")
    public void shouldClickNextButton() {
        String call = basePageSteps.onCallsPage().getCall(0).getText();
        basePageSteps.onCallsPage().pager().next().click();
        urlSteps.addParam("page", "2").shouldNotSeeDiff();
        basePageSteps.onCallsPage().callsList().waitUntil(hasSize(2));
        basePageSteps.onCallsPage().getCall(0).should(hasText(not(equalTo(call))));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Переход на предыдущую страницу по кнопке «Предыдущая»")
    public void shouldClickPrevButton() {
        String call = basePageSteps.onCallsPage().getCall(0).getText();
        basePageSteps.onCallsPage().pager().next().click();
        waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.onCallsPage().pager().prev().waitUntil(isEnabled()).click();
        urlSteps.replaceParam("page", "1").shouldNotSeeDiff();
        basePageSteps.onCallsPage().callsList().waitUntil(hasSize(20));
        basePageSteps.onCallsPage().getCall(0).waitUntil(hasText(equalTo(call)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Переход на страницу")
    public void shouldClickPage() {
        String call = basePageSteps.onCallsPage().getCall(0).getText();
        basePageSteps.onCallsPage().pager().page("2").click();
        urlSteps.addParam("page", "2").shouldNotSeeDiff();
        basePageSteps.onCallsPage().callsList().waitUntil(hasSize(2));
        basePageSteps.onCallsPage().getCall(0).should(hasText(not(equalTo(call))));
    }
}