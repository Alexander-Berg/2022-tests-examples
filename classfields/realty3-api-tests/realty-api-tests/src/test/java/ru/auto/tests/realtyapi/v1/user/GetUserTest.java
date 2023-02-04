package ru.auto.tests.realtyapi.v1.user;

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
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;


@Title("GET /user")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class GetUserTest {

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
        api.user().getUserRoute().execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldUserHasNoDiffWithProduction() {
        Account account = am.create();
        String token = oAuth.getToken(account);

        Function<ApiClient, JsonObject> request = apiClient -> apiClient.user().getUserRoute().reqSpec(authSpec())
                .authorizationHeader(token)
                .execute(validatedWith(shouldBe200OkJSON())).as(JsonObject.class, GSON);

        assertThat(request.apply(api), jsonEquals(request.apply(prodApi)));
    }

    @Test
    public void shouldVosUserHasNoDiffWithProduction() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        adaptor.vosUser(token);

        Function<ApiClient, JsonObject> request = apiClient -> apiClient.user().getUserRoute().reqSpec(authSpec())
                .authorizationHeader(token)
                .execute(validatedWith(shouldBe200OkJSON())).as(JsonObject.class, GSON);

        assertThat(request.apply(api), jsonEquals(request.apply(prodApi)));
    }
}
