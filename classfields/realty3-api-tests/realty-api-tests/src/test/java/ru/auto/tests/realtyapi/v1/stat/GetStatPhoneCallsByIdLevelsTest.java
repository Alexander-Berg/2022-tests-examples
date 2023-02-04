package ru.auto.tests.realtyapi.v1.stat;

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
import ru.auto.tests.realtyapi.adaptor.RealtyApiAdaptor;
import ru.auto.tests.realtyapi.anno.Prod;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.List;
import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.commons.util.Utils.getRandomShortInt;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v1.testdata.TestData.defaultStatAggregation;


@Title("GET /stat/phoneCalls/{offerId}")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GetStatPhoneCallsByIdLevelsTest {

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

    @Parameterized.Parameter(0)
    public String level;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "{0}")
    public static List<String> getParameters() {
        return defaultStatAggregation();
    }

    @Test
    public void shouldStatByOfferIdLevelsHasNoDiffWithProduction() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        String offerId = adaptor.createOffer(token).getResponse().getId();
        int span = getRandomShortInt();

        Function<ApiClient, JsonObject> request = apiClient -> apiClient.stat().thisUserOfferPhoneCalls()
                .reqSpec(authSpec()).xAuthorizationHeader(token)
                .offerIdPath(offerId)
                .spanQuery(span)
                .levelQuery(level)
                .execute(validatedWith(shouldBe200OkJSON())).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(api), jsonEquals(request.apply(prodApi))
                .whenIgnoringPaths("response.values[*].date"));
    }
}
