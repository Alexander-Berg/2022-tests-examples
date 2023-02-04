package ru.auto.tests.publicapi.dealer;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.assertj.core.api.Assertions;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.model.AutoApiErrorResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static java.lang.String.format;
import static org.apache.http.HttpStatus.*;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.TIMONDL;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.*;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.testdata.DealerAccounts.getDemoAccount;

@DisplayName("GET /dealer/trade-in")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class DealerTradeInTest {

    private static final String DATE_FROM_FIELD_VALUE = "2019-08-01";
    private static final String DATE_FROM_FIELD_ERROR = "Request is missing required query parameter 'from_date'";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Inject
    private AccountManager accountManager;

    @Inject
    private PublicApiAdaptor adaptor;

    @Test
    @Owner(TIMONDL)
    public void shouldSee403WhenNoAuth() {
        api.dealer().getTradeInRequests().execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(TIMONDL)
    public void shouldSee401WithoutSession() {
        api.dealer().getTradeInRequests().reqSpec(defaultSpec())
                .fromDateQuery(DATE_FROM_FIELD_VALUE)
                .execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)));
    }

    @Test
    @Owner(TIMONDL)
    public void shouldSee403WithUserSession() {
        Account account = accountManager.create();
        String sessionId = adaptor.login(account).getSession().getId();

        AutoApiErrorResponse response = api.dealer().getTradeInRequests().reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .fromDateQuery(DATE_FROM_FIELD_VALUE)
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
                .as(AutoApiErrorResponse.class, GSON);

        assertThat(response).hasStatus(ERROR)
                .hasError(CUSTOMER_ACCESS_FORBIDDEN)
                .hasDetailedError(format("Permission denied to TRADE_IN:Read for user:%s", account.getId()));
    }

    @Test
    @Owner(TIMONDL)
    public void shouldSee400WithoutDateFrom() {
        String sessionId = adaptor.login(getDemoAccount()).getSession().getId();

        AutoApiErrorResponse response = api.dealer().getTradeInRequests().reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
                .as(AutoApiErrorResponse.class, GSON);

        assertThat(response).hasStatus(ERROR).hasError(BAD_REQUEST);
        Assertions.assertThat(response.getDetailedError()).contains(DATE_FROM_FIELD_ERROR);
    }

    @Test
    @Owner(TIMONDL)
    public void shouldHasNoDiffWithProduction() {
        String sessionId = adaptor.login(getDemoAccount()).getSession().getId();

        Function<ApiClient, JsonObject> req = apiClient -> apiClient.dealer().getTradeInRequests().reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .fromDateQuery(DATE_FROM_FIELD_VALUE)
                .execute(validatedWith(shouldBe200OkJSON()))
                .as(JsonObject.class, GSON);

        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)));
    }

}
