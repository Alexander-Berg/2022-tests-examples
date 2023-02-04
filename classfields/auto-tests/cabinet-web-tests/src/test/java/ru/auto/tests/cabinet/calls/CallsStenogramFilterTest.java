package ru.auto.tests.cabinet.calls;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import io.qameta.allure.Epic;
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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static io.restassured.http.Method.POST;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.commons.mountebank.http.predicates.PredicateType.MATCHES;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.CALLS;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.QueryParams.TEXT_QUERY;
import static ru.auto.tests.desktop.element.cabinet.calls.Filters.CHOOSE_WHO_SHOULD_SAY_PHRASE;
import static ru.auto.tests.desktop.element.cabinet.calls.Filters.SEARCH_IN_CLIENT_TEXT;
import static ru.auto.tests.desktop.element.cabinet.calls.Filters.SEARCH_IN_OPERATOR_TEXT;
import static ru.auto.tests.desktop.element.cabinet.calls.Filters.TYPE_WORD_OR_PHRASE_FOR_SEARCH;
import static ru.auto.tests.desktop.mock.MockCalltracking.DOMAIN;
import static ru.auto.tests.desktop.mock.MockCalltracking.SOURCE;
import static ru.auto.tests.desktop.mock.MockCalltracking.TARGET;
import static ru.auto.tests.desktop.mock.MockCalltracking.TEXT_FILTER;
import static ru.auto.tests.desktop.mock.MockCalltracking.WEBSEARCH_QUERY;
import static ru.auto.tests.desktop.mock.MockCalltracking.calltrackingExample;
import static ru.auto.tests.desktop.mock.MockCalltracking.calltrackingFiltered;
import static ru.auto.tests.desktop.mock.MockCalltracking.getCalltrackingRequest;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.CALLTRACKING;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(CABINET_DEALER)
@Feature(AutoruFeatures.CALLS)
@Story("Поиск по стенограмме")
@DisplayName("Поиск по стенограмме")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class CallsStenogramFilterTest {

    private static final String PHRASE_FOR_SEARCH = "Мейджер авто";
    private static final int NOT_FILTERED_COUNT = 12;
    private static final int FILTERED_COUNT = 2;

    private JsonObject requestBody = getCalltrackingRequest();

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("cabinet/Session/DirectDealerMoscow"),
                stub("cabinet/DealerAccount"),
                stub("cabinet/DealerTariff"),
                stub("cabinet/CommonCustomerGet"),
                stub("cabinet/ClientsGet"),
                stub("cabinet/DealerCampaigns"),
                stub("cabinet/ApiAccessClient"),
                stub("cabinet/CalltrackingAggregated"),
                stub("cabinet/CalltrackingSettings"),
                stub().withPredicateType(MATCHES)
                        .withPath(CALLTRACKING)
                        .withMethod(POST)
                        .withRequestBody(requestBody)
                        .withResponseBody(calltrackingExample().getBody())
        ).create();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(CALLS).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение звонков, после фильтрации по фразе")
    public void shouldSeeCallFiltrationByText() {
        basePageSteps.onCallsPage().callsList().waitUntil(hasSize(NOT_FILTERED_COUNT));
        basePageSteps.onCallsPage().filters().input(TYPE_WORD_OR_PHRASE_FOR_SEARCH, PHRASE_FOR_SEARCH);

        requestBody.getAsJsonObject(TEXT_FILTER).addProperty(WEBSEARCH_QUERY, PHRASE_FOR_SEARCH);

        mockRule.overwriteStub(9,
                stub().withPredicateType(MATCHES)
                        .withPath(CALLTRACKING)
                        .withMethod(POST)
                        .withRequestBody(requestBody)
                        .withResponseBody(calltrackingFiltered().getBody())
        );

        basePageSteps.onCallsPage().filters().search().click();

        basePageSteps.onCallsPage().callsList().should(hasSize(FILTERED_COUNT));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение звонков, после фильтрации по фразе + «Искать в тексте оператора»")
    public void shouldSeeCallFiltrationByTextAndOnlyOperator() {
        basePageSteps.onCallsPage().callsList().waitUntil(hasSize(NOT_FILTERED_COUNT));
        basePageSteps.onCallsPage().filters().input(TYPE_WORD_OR_PHRASE_FOR_SEARCH, PHRASE_FOR_SEARCH);

        requestBody.getAsJsonObject(TEXT_FILTER).addProperty(WEBSEARCH_QUERY, PHRASE_FOR_SEARCH);
        requestBody.getAsJsonObject(TEXT_FILTER).addProperty(DOMAIN, TARGET);

        mockRule.overwriteStub(9,
                stub().withPredicateType(MATCHES)
                        .withPath(CALLTRACKING)
                        .withMethod(POST)
                        .withRequestBody(requestBody)
                        .withResponseBody(calltrackingFiltered().getBody())
        );

        basePageSteps.onCallsPage().filters().selectItem(CHOOSE_WHO_SHOULD_SAY_PHRASE, SEARCH_IN_OPERATOR_TEXT);
        basePageSteps.onCallsPage().filters().select(SEARCH_IN_OPERATOR_TEXT).waitUntil(isDisplayed());

        basePageSteps.onCallsPage().callsList().should(hasSize(FILTERED_COUNT));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение звонков, после фильтрации по фразе + «Искать в тексте клиента»")
    public void shouldSeeCallFiltrationByTextAndOnlyClient() {
        basePageSteps.onCallsPage().callsList().waitUntil(hasSize(NOT_FILTERED_COUNT));
        basePageSteps.onCallsPage().filters().input(TYPE_WORD_OR_PHRASE_FOR_SEARCH, PHRASE_FOR_SEARCH);

        requestBody.getAsJsonObject(TEXT_FILTER).addProperty(WEBSEARCH_QUERY, PHRASE_FOR_SEARCH);
        requestBody.getAsJsonObject(TEXT_FILTER).addProperty(DOMAIN, SOURCE);

        mockRule.overwriteStub(9,
                stub().withPredicateType(MATCHES)
                        .withPath(CALLTRACKING)
                        .withMethod(POST)
                        .withRequestBody(requestBody)
                        .withResponseBody(calltrackingFiltered().getBody())
        );

        basePageSteps.onCallsPage().filters().selectItem(CHOOSE_WHO_SHOULD_SAY_PHRASE, SEARCH_IN_CLIENT_TEXT);
        basePageSteps.onCallsPage().filters().select(SEARCH_IN_CLIENT_TEXT).waitUntil(isDisplayed());

        basePageSteps.onCallsPage().callsList().should(hasSize(FILTERED_COUNT));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сбрасываем фильтрацию по фразе")
    public void shouldResetCallFiltrationByText() {
        requestBody.getAsJsonObject(TEXT_FILTER).addProperty(WEBSEARCH_QUERY, PHRASE_FOR_SEARCH);

        mockRule.overwriteStub(9,
                stub().withPredicateType(MATCHES)
                        .withPath(CALLTRACKING)
                        .withMethod(POST)
                        .withRequestBody(requestBody)
                        .withResponseBody(calltrackingFiltered().getBody())
        );

        urlSteps.addParam(TEXT_QUERY, PHRASE_FOR_SEARCH).open();

        basePageSteps.onCallsPage().callsList().waitUntil(hasSize(FILTERED_COUNT));

        mockRule.overwriteStub(9,
                stub().withPredicateType(MATCHES)
                        .withPath(CALLTRACKING)
                        .withMethod(POST)
                        .withRequestBody(getCalltrackingRequest())
                        .withResponseBody(calltrackingExample().getBody())
        );

        basePageSteps.onCallsPage().filters().clearInputButton(TYPE_WORD_OR_PHRASE_FOR_SEARCH).click();

        basePageSteps.onCallsPage().callsList().should(hasSize(NOT_FILTERED_COUNT));
        urlSteps.subdomain(SUBDOMAIN_CABINET).path(CALLS).shouldNotSeeDiff();
    }

}
