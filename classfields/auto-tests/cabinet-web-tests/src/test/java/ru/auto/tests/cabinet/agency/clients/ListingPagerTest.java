package ru.auto.tests.cabinet.agency.clients;

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

import static ru.auto.tests.desktop.consts.AutoruFeatures.AGENCY_CABINET;
import static ru.auto.tests.desktop.consts.AutoruFeatures.PAGER;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.CLIENTS;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_AGENCY;
import static ru.auto.tests.desktop.consts.QueryParams.PAGE;
import static ru.auto.tests.desktop.mock.MockAgencyDealersList.agencyDealersListRequest;
import static ru.auto.tests.desktop.mock.MockAgencyDealersList.agencyDealersListResponseBody;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.CABINET_AGENCY_DEALERS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 26.03.18
 */

@Feature(AGENCY_CABINET)
@Story(PAGER)
@DisplayName("Кабинет агента. Клиенты. Пагинация")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetTestsModule.class)
public class ListingPagerTest {

    private static final String SECOND_PAGE_NUM = "2";
    private static final String FIRST_PAGE_NUM = "1";
    private static final String CLIENT_FIRST_PAGE = "nsk0393";
    private static final String CLIENT_SECOND_PAGE = "nsk0394";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private AgencyCabinetPagesSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Before
    public void before() {
        mockRule.setStubs(stub("cabinet/SessionAgency"),
                stub("cabinet/DealerAccountAgency"),
                stub("cabinet/CommonCustomerGetAgency"),
                stub("cabinet/UserOffersAllCount"),
                stub("cabinet/AgencyClientsPresetsGet"),
                stub().withPostDeepEquals(CABINET_AGENCY_DEALERS)
                        .withRequestBody(
                                agencyDealersListRequest().getBody())
                        .withResponseBody(agencyDealersListResponseBody().getBody()),
                stub().withPostDeepEquals(CABINET_AGENCY_DEALERS)
                        .withRequestBody(
                                agencyDealersListRequest()
                                        .setPage(2).getBody())
                        .withResponseBody(
                                agencyDealersListResponseBody()
                                        .setClientIdResponse(0, CLIENT_SECOND_PAGE)
                                        .setPageResponse(2).getBody())
        ).create();

        urlSteps.subdomain(SUBDOMAIN_AGENCY).path(CLIENTS).open();
    }

    @Test
    @Category({Regression.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Переход на страницу «2»")
    public void shouldBeOfSecondPage() {
        steps.onAgencyCabinetClientsPage().pager().page(SECOND_PAGE_NUM).click();

        steps.onAgencyCabinetClientsPage().clientsList().get(0).code().waitUntil(hasText(CLIENT_SECOND_PAGE), 5);
        urlSteps.subdomain(SUBDOMAIN_AGENCY).path(CLIENTS)
                .addParam(PAGE, SECOND_PAGE_NUM).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Переход на страницу «1» со страницы «2»")
    public void shouldBeReturnedToFirstPageFromSecondPage() {
        urlSteps.subdomain(SUBDOMAIN_AGENCY).path(CLIENTS).addParam(PAGE, SECOND_PAGE_NUM).open();
        steps.onAgencyCabinetClientsPage().pager().page(FIRST_PAGE_NUM).click();

        steps.onAgencyCabinetClientsPage().clientsList().get(0).code().waitUntil(hasText(CLIENT_FIRST_PAGE), 5);
        urlSteps.subdomain(SUBDOMAIN_AGENCY).path(CLIENTS)
                .replaceParam(PAGE, FIRST_PAGE_NUM).shouldNotSeeDiff();
    }
}
