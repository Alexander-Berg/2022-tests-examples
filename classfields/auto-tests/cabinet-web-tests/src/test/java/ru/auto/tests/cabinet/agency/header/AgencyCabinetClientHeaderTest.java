package ru.auto.tests.cabinet.agency.header;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.AgencyCabinetPagesSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.AGENCY_CABINET;
import static ru.auto.tests.desktop.consts.AutoruFeatures.HEADER;
import static ru.auto.tests.desktop.consts.Owners.JENKL;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.CLIENTS;
import static ru.auto.tests.desktop.consts.Pages.DASHBOARD;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_AGENCY;
import static ru.auto.tests.desktop.mock.MockAgencyDealersList.agencyDealersListRequest;
import static ru.auto.tests.desktop.mock.MockAgencyDealersList.agencyDealersListResponseBody;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.CABINET_AGENCY_DEALERS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 19.09.18
 */
@Feature(AGENCY_CABINET)
@Story(HEADER)
@DisplayName("Кабинет агента. Шапка")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class AgencyCabinetClientHeaderTest {

    private static final String CLIENT_CODE = "nsk0393";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private AgencyCabinetPagesSteps steps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("cabinet/SessionAgency"),
                stub("cabinet/DealerAccountAgency"),
                stub("cabinet/CommonCustomerGetAgency"),
                stub("cabinet/UserOffersAllCount"),
                stub("cabinet/AgencyClientsPresetsGet"),
                stub().withPostDeepEquals(CABINET_AGENCY_DEALERS)
                        .withRequestBody(
                                agencyDealersListRequest().getBody())
                        .withResponseBody(
                                agencyDealersListResponseBody().getBody())
        ).create();

        urlSteps.subdomain(SUBDOMAIN_AGENCY).path(CLIENTS).open();
    }

    @Test
    @Category({Regression.class})
    @Owner(JENKL)
    @DisplayName("Клик по логотипу в шапке")
    public void shouldClickLogo() {
        steps.onAgencyCabinetMainPage().header().logo().click();

        urlSteps.subdomain(SUBDOMAIN_AGENCY).path(DASHBOARD).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Поиск по клиенту в шапке")
    public void shouldSeeFoundClient() {
        mockRule.overwriteStub(5,
                stub().withPostDeepEquals(CABINET_AGENCY_DEALERS)
                        .withRequestBody(
                                agencyDealersListRequest()
                                        .setClientId(CLIENT_CODE).getBody())
                        .withResponseBody(
                                agencyDealersListResponseBody()
                                        .changeClientsListToClientId(CLIENT_CODE)
                                        .getBody())
        );

        steps.onAgencyCabinetMainPage().header().findClients().click();
        steps.onAgencyCabinetMainPage().header().searchClientPopup().input().click();
        steps.onAgencyCabinetMainPage().header().searchClientPopup().input().sendKeys(CLIENT_CODE);
        steps.onAgencyCabinetMainPage().header().searchClientPopup().find().click();

        urlSteps.subdomain(SUBDOMAIN_AGENCY).path(CLIENTS)
                .addParam("origin", CLIENT_CODE).shouldNotSeeDiff();

        steps.onAgencyCabinetMainPage().listingList().should(hasSize(1))
                .get(0).code().should(hasText(CLIENT_CODE));
    }
}
