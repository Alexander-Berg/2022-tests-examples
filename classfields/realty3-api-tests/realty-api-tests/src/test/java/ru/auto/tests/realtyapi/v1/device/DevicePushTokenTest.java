package ru.auto.tests.realtyapi.v1.device;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.restassured.ResponseSpecBuilders;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.anno.Prod;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.auto.tests.realtyapi.v1.model.PushTokenRequest;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.realtyapi.consts.Owners.SCROOGE;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;


@Title("POST /device/pushToken")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class DevicePushTokenTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Inject
    private OAuth oAuth;

    @Inject
    private AccountManager am;

    @Test
    @Owner(SCROOGE)
    public void shouldHasNotDiffWithProduction() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        PushTokenRequest body = getPushRequestBody();

        Function<ApiClient, JsonObject> request = apiClient -> apiClient.device().pushTokenRoute()
                .reqSpec(authSpec()).authorizationHeader(token)
                .body(body)
                .execute(ResponseSpecBuilders.validatedWith(shouldBe200Ok())).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(api), jsonEquals(request.apply(prodApi)));
    }

    @Test
    @Owner(SCROOGE)
    public void shouldNoAuthHasNoDiffWithProduction() {
        PushTokenRequest body = getPushRequestBody();

        Function<ApiClient, JsonObject> request = apiClient -> apiClient.device().pushTokenRoute().reqSpec(authSpec())
                .body(body)
                .execute(ResponseSpecBuilders.validatedWith(shouldBe200Ok())).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(api), jsonEquals(request.apply(prodApi)));
    }

    @Test
    @Owner(SCROOGE)
    public void shouldSee403AfterPushToken() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        PushTokenRequest body = getPushRequestBody();

        api.device().pushTokenRoute().authorizationHeader(token)
                .body(body)
                .execute(ResponseSpecBuilders.validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(SCROOGE)
    public void shouldSee403WithoutOAuthPushToken() {
        String token = Utils.getRandomString();
        PushTokenRequest body = getPushRequestBody();

        api.device().pushTokenRoute().authorizationHeader(token)
                .body(body)
                .execute(ResponseSpecBuilders.validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    private PushTokenRequest getPushRequestBody() {
        return new PushTokenRequest().platform(PushTokenRequest.PlatformEnum.GCM).hidden(false)
                .token(Utils.getRandomString());
    }
}
