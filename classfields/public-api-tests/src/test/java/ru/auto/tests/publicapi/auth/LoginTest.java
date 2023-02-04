package ru.auto.tests.publicapi.auth;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.model.VertisPassportLoginParameters;
import ru.auto.tests.publicapi.model.AutoApiLoginResponse;
import ru.auto.tests.publicapi.module.PublicApiModule;
import ru.auto.tests.publicapi.utils.UtilsPublicApi;

import java.util.function.Function;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static ru.auto.tests.publicapi.assertions.AutoruApiModelsAssertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.consts.DealerConsts.CLIENT_ID;
import static ru.auto.tests.publicapi.consts.DealerConsts.DEALER_LOGIN;
import static ru.auto.tests.publicapi.consts.DealerConsts.DEALER_PASS;
import static ru.auto.tests.publicapi.consts.Owners.DSKUZNETSOV;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBe401AuthError;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

/**
 * Created by vicdev on 15.09.17.
 */

@DisplayName("POST /auth/login")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class LoginTest {

    private static final int DEFAULT_TTL = 360;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private AccountManager am;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Test
    public void shouldSee403WhenNoAuth() {
        api.auth().login().execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee400WithEmptyBody() {
        api.auth().login().reqSpec(defaultSpec()).execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    public void shouldNotLoginWithNotExitAccount() {
        api.auth().login().body(random(VertisPassportLoginParameters.class)
                                .identityToken(null)
                                .login(Utils.getRandomEmail()).ttlSec(DEFAULT_TTL)).reqSpec(defaultSpec())
            .execute(validatedWith(shouldBe401AuthError()));
    }

    @Test
    public void shouldNotLoginWithWrongPass() {
        api.auth().login().body(new VertisPassportLoginParameters()
                .login(am.create().getLogin())
                .password(Utils.getRandomString()).ttlSec(DEFAULT_TTL)).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBe401AuthError()));
    }

    @Test
    public void shouldAuth() {
        Account account = am.create();
        AutoApiLoginResponse response = api.auth().login().body(new VertisPassportLoginParameters().login(account.getLogin())
                .password(account.getPassword()).ttlSec(DEFAULT_TTL)).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeCode(SC_OK)));
        assertThat(response.getUser()).hasId(account.getId());
    }

    @Test
    public void shouldHasNoDiffWithProduction() {
        Account account = am.create();
        String deviceId = UtilsPublicApi.getRandomDeviceId();

        Function<ApiClient, JsonObject> request = apiClient -> apiClient.auth().login().body(new VertisPassportLoginParameters().login(account.getLogin())
                .password(account.getPassword()).ttlSec(DEFAULT_TTL)).reqSpec(defaultSpec()).xDeviceUidHeader(deviceId)
                .execute(validatedWith(shouldBeCode(SC_OK))).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(api), jsonEquals(request.apply(prodApi)).whenIgnoringPaths("session.expire_timestamp", "session.creation_timestamp", "session.id"));
    }

    @Test
    public void shouldSessionIdChange() {
        Account account = am.create();
        Function<ApiClient, AutoApiLoginResponse> request = apiClient -> apiClient.auth().login().body(new VertisPassportLoginParameters().login(account.getLogin())
                .password(account.getPassword()).ttlSec(DEFAULT_TTL)).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeCode(SC_OK)));

        MatcherAssert.assertThat("<id> сессий должны меняться", request.apply(api).getSession().getId(), not(equalTo(request.apply(prodApi).getSession().getId())));
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldLoginAsDealer() {
        String clientId = api.auth().login().body(new VertisPassportLoginParameters().login(DEALER_LOGIN)
                .password(DEALER_PASS).ttlSec(DEFAULT_TTL)).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess())).getUser().getProfile().getAutoru().getClientId();
        MatcherAssert.assertThat(clientId, equalTo(CLIENT_ID));
    }
}
