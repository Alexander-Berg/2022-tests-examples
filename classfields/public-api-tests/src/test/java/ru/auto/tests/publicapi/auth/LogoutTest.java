package ru.auto.tests.publicapi.auth;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.model.AutoApiErrorResponse;
import ru.auto.tests.publicapi.model.AutoApiLoginResponse;
import ru.auto.tests.publicapi.model.AutoApiLogoutResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.function.Function;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.NO_AUTH;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.StatusEnum.ERROR;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;


/**
 * Created by vicdev on 15.09.17.
 */

@DisplayName("POST /auth/logout")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class LogoutTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Inject
    private PublicApiAdaptor adaptor;

    @Inject
    private AccountManager am;

    @Test
    public void shouldSee403WhenNoAuth() {
        api.auth().logout().execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldNotLogoutWithoutSession() {
        AutoApiErrorResponse response = api.auth().logout().reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_FORBIDDEN)))
                .as(AutoApiErrorResponse.class);
        assertThat(response).hasStatus(ERROR)
                .hasError(NO_AUTH)
                .hasDetailedError("session_id is required");
    }

    @Test
    public void shouldLogout() {
        AutoApiLoginResponse loginResult = adaptor.login(am.create());

        AutoApiLogoutResponse response = api.auth().logout().reqSpec(defaultSpec()).xSessionIdHeader(loginResult.getSession().getId())
                .xDeviceUidHeader(loginResult.getSession().getDeviceUid())
                .xUidHeader(loginResult.getSession().getUserId())
                .executeAs(validatedWith(shouldBe200OkJSON()));

        assertThat(response.getSession()).hasDeviceUid(loginResult.getSession().getDeviceUid());
    }

    @Test
    public void shouldHasNoDiffWithProduction() {
        AutoApiLoginResponse loginResult = adaptor.login(am.create());

        Function<ApiClient, AutoApiLogoutResponse> request = apiClient -> apiClient.auth().logout().reqSpec(defaultSpec()).xSessionIdHeader(loginResult.getSession().getId())
                .xDeviceUidHeader(loginResult.getSession().getDeviceUid())
                .xUidHeader(loginResult.getSession().getUserId())
                .executeAs(validatedWith(shouldBe200OkJSON()));

        MatcherAssert.assertThat(request.apply(api), jsonEquals(request.apply(prodApi))
                .whenIgnoringPaths("session.expire_timestamp", "session.creation_timestamp", "session.id",
                        "session.creationTimestamp", "session.expireTimestamp"));
    }

}
