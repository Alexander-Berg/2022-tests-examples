package ru.auto.tests.realtyapi.v1.user.offer;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.yandex.qatools.allure.annotations.Parameter;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v1.testdata.TestData.defaultInvalidOffers;



@Title("POST /user/offers/validation")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ValidationOfferInvalidOffersTest {
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private ApiClient prodApi;

    @Inject
    private AccountManager am;

    @Inject
    private OAuth oAuth;

    @Parameter
    @Parameterized.Parameter(0)
    public String path;

    @Parameterized.Parameters(name = "path={0}")
    public static Object[] getParameters() {
        return defaultInvalidOffers();
    }

    @Test
    public void shouldValidationHasNoDiffWithProduction() {
        Account account = am.create();
        String token = oAuth.getToken(account);

        Function<ApiClient, JsonObject> request = apiClient -> apiClient.userOffers().validateOfferRoute()
                .reqSpec(authSpec()).authorizationHeader(token)
                .reqSpec(req -> req.setBody(getResourceAsString(path)))
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST))).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(api), jsonEquals(request.apply(prodApi)));
    }
}
