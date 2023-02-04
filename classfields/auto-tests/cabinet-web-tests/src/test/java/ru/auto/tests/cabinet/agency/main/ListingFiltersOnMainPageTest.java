package ru.auto.tests.cabinet.agency.main;

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
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.AgencyCabinetPagesSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.containsString;
import static ru.auto.tests.desktop.consts.AutoruFeatures.AGENCY_CABINET;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.DASHBOARD;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_AGENCY;
import static ru.auto.tests.desktop.element.cabinet.agency.ListingFilters.ACTIVE;
import static ru.auto.tests.desktop.element.cabinet.agency.ListingFilters.ALL;
import static ru.auto.tests.desktop.element.cabinet.agency.ListingFilters.FREEZED;
import static ru.auto.tests.desktop.element.cabinet.agency.ListingFilters.STOPPED;
import static ru.auto.tests.desktop.mock.MockAgencyDealersList.agencyDealersListRequest;
import static ru.auto.tests.desktop.mock.MockAgencyDealersList.agencyDealersListResponseBody;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.CABINET_AGENCY_DEALERS;
import static ru.auto.tests.desktop.step.cabinet.AgencyCabinetPagesSteps.ACTIVE_FILTER_CLASS_NAME;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Feature(AGENCY_CABINET)
@DisplayName("Кабинет агента. Главная страница. Фильтры")
@GuiceModules(CabinetTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ListingFiltersOnMainPageTest {

    public static final String FILTER_PARAM_VALUE = "1";

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

    @Parameterized.Parameter
    public String statusFilter;

    @Parameterized.Parameter(1)
    public String statusQuery;

    @Parameterized.Parameter(2)
    public String statusSnippet;

    @Parameterized.Parameters(name = "{index}: {0}")
    public static String[][] getParameters() {
        return new String[][]{
                {ACTIVE, "ACTIVE", "активный"},
                {STOPPED, "STOPPED", "остановлен"},
                {FREEZED, "FREEZED", "замороженный"}
        };
    }

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

        urlSteps.subdomain(SUBDOMAIN_AGENCY).path(DASHBOARD).open();
    }

    @Test
    @Category({Regression.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Переключение фильтров на дашборде")
    public void shouldSeeActiveFilterAndParamInUrl() {
        mockRule.overwriteStub(5,
                stub().withPostDeepEquals(CABINET_AGENCY_DEALERS)
                        .withRequestBody(
                                agencyDealersListRequest()
                                        .changeStatus(statusQuery).getBody())
                        .withResponseBody(
                                agencyDealersListResponseBody()
                                        .changeStatusResponse(0, statusQuery).getBody())
        );

        steps.onAgencyCabinetMainPage().listingFilters().filter(ALL).should(hasClass(
                containsString(ACTIVE_FILTER_CLASS_NAME)));
        steps.onAgencyCabinetMainPage().clientsList().get(0).status().should(hasText("неактивный"));
        steps.onAgencyCabinetMainPage().listingFilters().filter(statusFilter).click();

        steps.onAgencyCabinetMainPage().listingFilters().filter(statusFilter).should(
                hasClass(containsString(ACTIVE_FILTER_CLASS_NAME)));
        urlSteps.subdomain(SUBDOMAIN_AGENCY).path(DASHBOARD)
                .addParam(statusQuery.toLowerCase(), FILTER_PARAM_VALUE).shouldNotSeeDiff();
        steps.onAgencyCabinetClientsPage().clientsList().get(0).status().should(hasText(statusSnippet));
    }
}
