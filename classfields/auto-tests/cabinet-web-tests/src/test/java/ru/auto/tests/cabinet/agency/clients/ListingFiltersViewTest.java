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

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.AGENCY_CABINET;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FILTERS;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.CLIENTS;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_AGENCY;
import static ru.auto.tests.desktop.mock.MockAgencyDealersList.agencyDealersListRequest;
import static ru.auto.tests.desktop.mock.MockAgencyDealersList.agencyDealersListResponseBody;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.CABINET_AGENCY_DEALERS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Feature(AGENCY_CABINET)
@Story(FILTERS)
@DisplayName("Кабинет агента. Клиенты. Фильтры")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class ListingFiltersViewTest {

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
        mockRule.setStubs(stub("cabinet/SessionAgency"),
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
    @Owner(NATAGOLOVKINA)
    @DisplayName("Отображение фильтра на странице clients")
    public void shouldSeeFilters() {
        steps.onAgencyCabinetClientsPage().listingFilters().should(hasText(
                "Все\n938\nАктивные\n864\nОстановленные\n43\nЗамороженные\n31"));
        steps.onAgencyCabinetClientsPage().clientsList().should(hasSize(15));
    }

}
