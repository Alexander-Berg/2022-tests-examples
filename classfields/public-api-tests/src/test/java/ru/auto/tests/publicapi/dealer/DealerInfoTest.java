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
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.model.AutoApiErrorResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.publicapi.consts.Owners.AVGRIBANOV;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.BAD_REQUEST;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.NO_AUTH;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.AGENT_ACCESS_FROBIDDEN;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.testdata.DealerAccounts.getAgencyMaxAccount;
import static ru.auto.tests.publicapi.testdata.DealerAccounts.getDemoAccount;
import static ru.auto.tests.publicapi.testdata.DealerAccounts.getManagerAccount;

@DisplayName("GET /dealer/info")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class DealerInfoTest {

    private static final String DEALER_ID = "20101";
    private static final String AGENCY_DEALER_ID = "9015";

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
    @Owner(AVGRIBANOV)
    public void shouldSee403WhenNoAuth() {
        api.dealer().getDealerInfo().execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(AVGRIBANOV)
    public void shouldSee401WithoutSession() {
        api.dealer().getDealerInfo().reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)));
    }

    @Test
    @Owner(AVGRIBANOV)
    public void shouldSee401WithUserSession() {
        Account account = accountManager.create();
        String sessionId = adaptor.login(account).getSession().getId();

        AutoApiErrorResponse response = api.dealer().getDealerInfo().reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)))
                .as(AutoApiErrorResponse.class, GSON);

        assertThat(response).hasStatus(ERROR)
                .hasError(NO_AUTH)
                .hasDetailedError("Expected dealer user. Provide valid session_id");
    }

    @Test
    @Owner(AVGRIBANOV)
    public void shouldSee403WithUserSession() {
        Account account = accountManager.create();
        String sessionId = adaptor.login(account).getSession().getId();

        AutoApiErrorResponse response = api.dealer().getDealerInfo().reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .xDealerIdHeader(DEALER_ID)
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
                .as(AutoApiErrorResponse.class, GSON);

        assertThat(response).hasStatus(ERROR)
                .hasError(AGENT_ACCESS_FROBIDDEN)
                .hasDetailedError(AGENT_ACCESS_FROBIDDEN.getValue());
    }

    @Test
    @Owner(AVGRIBANOV)
    public void shouldSee400WithManagerSession() {
        String sessionId = adaptor.login(getManagerAccount()).getSession().getId();

        AutoApiErrorResponse response = api.dealer().getDealerInfo().reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .xDealerIdHeader(Utils.getRandomString())
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
                .as(AutoApiErrorResponse.class, GSON);

        assertThat(response).hasStatus(ERROR)
                .hasError(BAD_REQUEST);
        Assertions.assertThat(response.getDetailedError())
                .contains("The value of HTTP header 'x-dealer-id' was malformed:\nExpected numeric x-dealer-id");
    }

    @Test
    @Owner(AVGRIBANOV)
    public void shouldSee403WithAgencySession() {
        String sessionId = adaptor.login(getAgencyMaxAccount()).getSession().getId();

        AutoApiErrorResponse response = api.dealer().getDealerInfo().reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .xDealerIdHeader(DEALER_ID)
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
                .as(AutoApiErrorResponse.class, GSON);

        assertThat(response).hasStatus(ERROR)
                .hasError(AGENT_ACCESS_FROBIDDEN)
                .hasDetailedError(AGENT_ACCESS_FROBIDDEN.getValue());
    }

    @Test
    @Owner(AVGRIBANOV)
    public void shouldHasNoDiffWithProduction() {
        String sessionId = adaptor.login(getDemoAccount()).getSession().getId();

        Function<ApiClient, JsonObject> req = apiClient -> apiClient.dealer().getDealerInfo().reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBe200OkJSON()))
                .as(JsonObject.class, GSON);

        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)));
    }

    @Test
    @Owner(AVGRIBANOV)
    public void shouldHasNoDiffWithProductionManager() {
        String sessionId = adaptor.login(getManagerAccount()).getSession().getId();

        Function<ApiClient, JsonObject> req = apiClient -> apiClient.dealer().getDealerInfo().reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .xDealerIdHeader(DEALER_ID)
                .execute(validatedWith(shouldBe200OkJSON()))
                .as(JsonObject.class, GSON);

        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)));
    }

    @Test
    @Owner(AVGRIBANOV)
    public void shouldHasNoDiffWithProductionAgency() {
        String sessionId = adaptor.login(getAgencyMaxAccount()).getSession().getId();

        Function<ApiClient, JsonObject> req = apiClient -> apiClient.dealer().getDealerInfo().reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .xDealerIdHeader(AGENCY_DEALER_ID)
                .execute(validatedWith(shouldBe200OkJSON()))
                .as(JsonObject.class, GSON);

        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)));
    }

}
