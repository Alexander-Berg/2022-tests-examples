package ru.auto.tests.realtyapi.v2.flatPlans;

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
import ru.yandex.qatools.allure.annotations.Parameter;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.List;
import java.util.function.Function;

import static io.qameta.allure.jsonunit.JsonPatchMatcher.jsonEquals;
import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v2.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.v2.testdata.FlatPlansTestData.addressesForFlatPlans;

@Title("GET /flatPlans/suggest")
@GuiceModules(RealtyApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GetSuggestCompareTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Parameter
    @Parameterized.Parameter
    public String address;

    @Parameterized.Parameters(name = "address={0}")
    public static List<String> getParameters() {
        return addressesForFlatPlans();
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldPlanSuggestHasNoDiffWithProduction() {
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.flatPlans().search()
                .reqSpec(authSpec())
                .addressQuery(address)
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(api), jsonEquals(request.apply(prodApi)));
    }
}
