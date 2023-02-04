package ru.auto.tests.cabinet.agency.header;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.AgencyAccountProvider;
import ru.auto.tests.desktop.DesktopConfig;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.LoginSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.AgencyCabinetPagesSteps;
import ru.auto.tests.passport.account.Account;

import javax.inject.Inject;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.AutoruFeatures.AGENCY_CABINET;
import static ru.auto.tests.desktop.consts.AutoruFeatures.HEADER;
import static ru.auto.tests.desktop.consts.Owners.JENKL;
import static ru.auto.tests.desktop.consts.Pages.DASHBOARD;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_AGENCY;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 19.09.18
 */
@Feature(AGENCY_CABINET)
@Story(HEADER)
@DisplayName("Кабинет агента. Шапка")
@GuiceModules(CabinetTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CabinetPagesFromHeaderTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private LoginSteps loginSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private DesktopConfig config;

    @Inject
    private AgencyCabinetPagesSteps steps;

    @Parameterized.Parameter
    public String page;

    @Parameterized.Parameter(1)
    public String expectedUrl;

    @Parameterized.Parameters(name = "{index}: Переход на страницу «{0}»")
    public static Object[][] getParameters() {
        return new Object[][]{
                {"Клиенты", "https://agency.%s/clients/"},
                {"Пользователи", "https://office7.%s/v2/agent/users/"}
        };
    }

    @Before
    public void before() {
        Account account = AgencyAccountProvider.MAIN_TEST_AGENT.get();
        loginSteps.loginAs(account);

        urlSteps.subdomain(SUBDOMAIN_AGENCY).path(DASHBOARD).open();
    }

    @Test
    @Category({Regression.class})
    @Owner(JENKL)
    @DisplayName("Переход на страницу кабинета")
    public void shouldSeeCabinetPage() {
        steps.onAgencyCabinetMainPage().header().cabinetPage(page).click();
        urlSteps.fromUri(format(expectedUrl, config.getBaseDomain())).shouldNotSeeDiff();
    }
}
