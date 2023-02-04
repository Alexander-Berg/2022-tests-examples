package ru.auto.tests.realtyapi.v2.cabinet;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
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
import ru.auto.tests.realtyapi.v2.model.RealtyApiCabinetClientsResponse;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static org.hamcrest.CoreMatchers.equalTo;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v2.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.v2.testdata.CabinetTestData.VALID_UID;
import static ru.auto.tests.realtyapi.v2.testdata.CabinetTestData.getBillingDomain;

@Title("GET /cabinet/{billing-domain}/clients")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GetClientsCompareTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Parameterized.Parameter(0)
    public String domain;

    @Parameterized.Parameters(name = "billing-domain={0}")
    public static Object[] getParameters() {
        return getBillingDomain();
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldHasNoDiffWithProduction() {
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.cabinet().clients()
                .reqSpec(authSpec())
                .billingDomainPath(domain)
                .xUidHeader(VALID_UID)
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(api), jsonEquals(request.apply(prodApi)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldHasOnlyAgencies() {
        RealtyApiCabinetClientsResponse response = api.cabinet().clients()
                .reqSpec(authSpec())
                .billingDomainPath(domain)
                .xUidHeader(VALID_UID)
                .queryTypeQuery("agencies")
                .executeAs(validatedWith(shouldBe200Ok()));

        response.getResponse().getClientsInfo()
                .forEach(c -> MatcherAssert.assertThat(c.getClient(), equalTo(null)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldHasOnlyClients() {
        RealtyApiCabinetClientsResponse response = api.cabinet().clients()
                .reqSpec(authSpec())
                .billingDomainPath(domain)
                .xUidHeader(VALID_UID)
                .queryTypeQuery("clients")
                .executeAs(validatedWith(shouldBe200Ok()));

        response.getResponse().getClientsInfo()
                .forEach(c -> MatcherAssert.assertThat(c.getAgency(), equalTo(null)));
    }
}
