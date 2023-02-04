package ru.auto.tests.cabinet.agency.header;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.AgencyCabinetPagesSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static ru.auto.tests.desktop.consts.AutoruFeatures.AGENCY_CABINET;
import static ru.auto.tests.desktop.consts.AutoruFeatures.HEADER;
import static ru.auto.tests.desktop.consts.Owners.JENKL;
import static ru.auto.tests.desktop.consts.Pages.DASHBOARD;
import static ru.auto.tests.desktop.consts.Pages.LOGIN;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.PROFILE;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_AGENCY;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_AUTH;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 26.03.18
 */

@Feature(AGENCY_CABINET)
@Story(HEADER)
@DisplayName("Кабинет агента. Шапка. Персональное меню")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetTestsModule.class)
public class PersonalMenuOfAgencyDealerTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private AgencyCabinetPagesSteps steps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("cabinet/SessionAgency"),
                stub("cabinet/DealerAccountAgency"),
                stub("cabinet/CommonCustomerGetAgency"),
                stub("cabinet/AgencyClientsPresetsGet"),
                stub("cabinet/AuthLogout")
        ).create();

        urlSteps.subdomain(SUBDOMAIN_AGENCY).path(DASHBOARD).open();
        steps.waitUntilPageIsFullyLoaded();
    }

    @Test
    @Category({Regression.class})
    @Owner(JENKL)
    @DisplayName("Блок «Персональное меню агента»")
    public void shouldSeePersonalMenuBlock() {
        steps.moveCursor(steps.onAgencyCabinetMainPage().header().personalMenuOfDealer());
        steps.onCabinetOffersPage().header().personalMenuOfDealer().waitUntil(isDisplayed())
                .should(hasText("Настройки учётной записи\nВыход"));
    }

    @Test
    @Category({Regression.class})
    @Owner(JENKL)
    @DisplayName("Кнопка «Настройки»")
    public void shouldSeePersonalSettingsPage() {
        steps.moveCursor(steps.onCabinetOffersPage().header().personalMenuOfDealer());
        steps.onCabinetOffersPage().header().personalMenuOfDealer()
                .menu("Настройки").waitUntil(isDisplayed()).click();
        urlSteps.testing().path(MY).path(PROFILE).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class})
    @Owner(JENKL)
    @DisplayName("Кнопка «Выход»")
    public void shouldSeeAuthorizationPage() {
        steps.moveCursor(steps.onCabinetOffersPage().header().personalMenuOfDealer());

        mockRule.overwriteStub(0, stub("desktop/SessionUnauth"));

        steps.onCabinetOffersPage().header().personalMenuOfDealer()
                .menu("Выход").waitUntil(isDisplayed()).click();
        shouldRedirectToAuthorizationPage();
    }

    @Step("Должен произойти редирект на страницу авторизации")
    private void shouldRedirectToAuthorizationPage() {
        assertThat("Должны быть на странице авторизации",
                urlSteps.getCurrentUrl(), containsString(urlSteps.subdomain(SUBDOMAIN_AUTH).path(LOGIN).toString()));
    }
}
