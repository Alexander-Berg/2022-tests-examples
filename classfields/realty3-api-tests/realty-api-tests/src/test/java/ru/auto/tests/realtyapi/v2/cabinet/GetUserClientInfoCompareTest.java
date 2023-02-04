package ru.auto.tests.realtyapi.v2.cabinet;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import org.assertj.Assertions;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.realtyapi.anno.Prod;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.v2.ApiClient;
import ru.auto.tests.realtyapi.v2.model.RealtyApiCabinetClientResponse;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v2.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.v2.testdata.CabinetTestData.ME;
import static ru.auto.tests.realtyapi.v2.testdata.CabinetTestData.getAgencies;

@Title("GET /cabinet/{billing-domain}/user/{user}/client")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GetUserClientInfoCompareTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Parameterized.Parameter(0)
    public String uid;

    @Parameterized.Parameter(1)
    public String clientId;

    @Parameterized.Parameter(2)
    public String domain;

    @Parameterized.Parameters(name = "uid={0} clientId={1} billing-domain={2}")
    public static Object[][] getParameters() {
        return getAgencies();
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSeeNoDiffWithProduction() {
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.cabinet().clientInfo()
                .reqSpec(authSpec())
                .billingDomainPath(domain)
                .clientQuery(clientId)
                .xUidHeader(uid)
                .userPath(ME)
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(api), jsonEquals(request.apply(prodApi)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSeeNoDiffWithProductionWithNoId() {
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.cabinet().clientInfo()
                .reqSpec(authSpec())
                .billingDomainPath(domain)
                .xUidHeader(uid)
                .userPath(ME)
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(api), jsonEquals(request.apply(prodApi)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldClientHasValidId() {
        RealtyApiCabinetClientResponse response = api.cabinet().clientInfo()
                .reqSpec(authSpec())
                .billingDomainPath(domain)
                .clientQuery(clientId)
                .xUidHeader(uid)
                .userPath(ME)
                .executeAs(validatedWith(shouldBe200Ok()));

        Assertions.assertThat(response.getResponse().getClient()).hasId(new Long(clientId));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSeeInfoAboutAgency() {
        RealtyApiCabinetClientResponse response = api.cabinet().clientInfo()
                .reqSpec(authSpec())
                .billingDomainPath(domain)
                .xUidHeader(uid)
                .userPath(ME)
                .executeAs(validatedWith(shouldBe200Ok()));

        Assertions.assertThat(response.getResponse().getAgency()).isNotNull();
    }
}
