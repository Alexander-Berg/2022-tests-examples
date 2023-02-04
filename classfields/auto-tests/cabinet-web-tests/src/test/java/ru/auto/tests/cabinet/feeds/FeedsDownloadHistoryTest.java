package ru.auto.tests.cabinet.feeds;

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
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.BasePageSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.KRISKOLU;
import static ru.auto.tests.desktop.consts.Pages.FEEDS;
import static ru.auto.tests.desktop.consts.Pages.HISTORY;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Фиды. Переход на страницу конкретного фида по разным ссылкам")
@RunWith(Parameterized.class)
@GuiceModules(CabinetTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class FeedsDownloadHistoryTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String status;

    @Parameterized.Parameters(name = "name = {index}: {0} {1}")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {"Легковые с пробегом", "ERROR"},
                {"2", "ERROR"},
                {"1 016", "NOTICE"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthDealer",
                "cabinet/ApiAccessClient",
                "cabinet/CommonCustomerGet",
                "cabinet/DealerAccount",
                "cabinet/DealerTariff",
                "cabinet/ClientsGet",
                "cabinet/FeedsHistory",
                "cabinet/FeedsHistoryIdError",
                "cabinet/FeedsHistoryIdNotice",
                "cabinet/FeedsHistoryIdErrorNoneNotice").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(FEEDS).path(HISTORY).open();
    }

    @Test
    @Category({Regression.class})
    @Owner(KRISKOLU)
    @DisplayName("Переход на страницу конкретного фида")
    public void shouldClickFeed() {
        steps.onCabinetFeedsPage().downloadHistory().getFeed(0).button(category).click();
        urlSteps.subdomain(SUBDOMAIN_CABINET).path(FEEDS).path(HISTORY).path("/22719436/").addParam("error_type", status)
                .shouldNotSeeDiff();
    }
}
