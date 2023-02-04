package ru.auto.tests.realtyapi.v1.auth;


import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.yandex.qatools.allure.annotations.Title;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static java.lang.String.format;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.ra.ResponseSpecBuilders.shouldBe401WithPrivateUserExpected;


@Title("POST /auth/login")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class LoginTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private OAuth oAuth;

    @Inject
    private AccountManager am;

    @Test
    public void shouldSee403WithoutHeaders() {
        api.auth().loginRoute().execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee401WithInvalidAuthorization() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        api.auth().loginRoute().reqSpec(authSpec()).authorizationHeader(format("%s%s", token, getRandomString()))
                .execute(validatedWith(shouldBe401WithPrivateUserExpected()));
    }

    @Test
    public void shouldSee401WithoutAuthorization() {
        api.auth().loginRoute().reqSpec(authSpec()).execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)));
    }

    @Test
    public void shouldSee401WithEmptyAuthorization() {
        api.auth().loginRoute().reqSpec(authSpec()).authorizationHeader(StringUtils.EMPTY)
                .execute(validatedWith(shouldBeCode(SC_UNAUTHORIZED)));
    }

    @Test
    public void shouldSuccessLogin() {
        Account account = am.create();
        String token = oAuth.getToken(account);

        JsonObject resp = api.auth().loginRoute().reqSpec(authSpec()).authorizationHeader(token)
                .execute(validatedWith(shouldBe200OkJSON())).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(resp, jsonEquals(new JsonObject()));
    }
}
