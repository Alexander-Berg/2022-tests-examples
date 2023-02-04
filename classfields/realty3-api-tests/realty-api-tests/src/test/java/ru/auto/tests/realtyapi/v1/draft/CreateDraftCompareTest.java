package ru.auto.tests.realtyapi.v1.draft;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import org.assertj.core.api.Assertions;
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
import ru.yandex.qatools.allure.annotations.Parameter;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.List;
import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v1.testdata.TestData.ME;
import static ru.auto.tests.realtyapi.v1.testdata.TestData.getDrafts;

@Title("POST /user/{user}/offers/draft")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class CreateDraftCompareTest {

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

    @Parameter
    @Parameterized.Parameter(0)
    public String draft;

    @Parameterized.Parameters(name = "draft={0}")
    public static List<String> getParameters() {
        return getDrafts();
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldCreateDraftHasNoDiffWithProduction() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        adaptor.vosUser(token);

        Function<ApiClient, JsonObject> request = apiClient -> apiClient.draft().createDraftForUserRoute()
                .reqSpec(authSpec())
                .reqSpec(req -> req.setBody(getResourceAsString(draft)))
                .userPath(ME)
                .authorizationHeader(token)
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(api), jsonEquals(request.apply(prodApi))
                .whenIgnoringPaths("response.id", "response.details[0].id"));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldCreateDraft() {
        Account account = am.create();
        String token = oAuth.getToken(account);
        adaptor.vosUser(token);

        JsonObject response = api.draft().createDraftForUserRoute().reqSpec(authSpec())
                .reqSpec(req -> req.setBody(getResourceAsString(draft)))
                .userPath(ME)
                .authorizationHeader(token)
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON)
                .getAsJsonObject("response");

        JsonObject offer = api.userOffers().getUserOfferRoute().reqSpec(authSpec())
                .offerIdPath(response.get("id").getAsString())
                .authorizationHeader(token)
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON)
                .getAsJsonObject("response");

        Assertions.assertThat(offer.get("offerStatus").getAsString()).isEqualTo("draft");
    }
}
