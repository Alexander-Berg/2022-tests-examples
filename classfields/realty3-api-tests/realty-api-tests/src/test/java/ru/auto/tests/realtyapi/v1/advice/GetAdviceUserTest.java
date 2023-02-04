package ru.auto.tests.realtyapi.v1.advice;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.adaptor.RealtyApiAdaptor;
import ru.auto.tests.realtyapi.anno.Prod;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.ra.ResponseSpecBuilders.shouldBe404RequestedHandlerNotBeFound;
import static ru.auto.tests.realtyapi.ra.ResponseSpecBuilders.shouldBe404UnknownVosUser;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getRandomUID;


@Title("GET /advice/user/{uid}")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class GetAdviceUserTest {

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

    @Inject
    private RealtyApiAdaptor adaptor;

    @Test
    public void shouldSee403WithoutHeaders() {
        api.advice().getUserAdvice().uidPath(getRandomUID()).execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee404ForNotVosUser() {
        Account account = am.create();
        String token = oAuth.getToken(account);

        api.advice().getUserAdvice().reqSpec(authSpec()).authorizationHeader(token).uidPath(account.getId())
                .execute(validatedWith(shouldBe404UnknownVosUser(account.getId())));
    }

    @Test
    public void shouldSee400ForInvalidVosUser() {
        Account account = am.create();
        String token = oAuth.getToken(account);

        api.advice().getUserAdvice().reqSpec(authSpec()).authorizationHeader(token).uidPath(getRandomString())
                .execute(validatedWith(shouldBe404RequestedHandlerNotBeFound()));
    }

    @Test
    public void shouldUserAdvicesHasNoDiffWithProduction() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        adaptor.vosUser(token);

        Function<ApiClient, JsonObject> request = apiClient -> apiClient.advice().getUserAdvice()
                .reqSpec(authSpec()).authorizationHeader(token)
                .uidPath(account.getId())
                .execute(validatedWith(shouldBe200OkJSON())).as(JsonObject.class, GSON);

        assertThat(request.apply(api), jsonEquals(request.apply(prodApi)));
    }
}
