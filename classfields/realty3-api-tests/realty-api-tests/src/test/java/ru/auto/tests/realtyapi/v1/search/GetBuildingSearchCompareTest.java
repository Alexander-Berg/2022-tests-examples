package ru.auto.tests.realtyapi.v1.search;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.realtyapi.anno.Prod;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.yandex.qatools.allure.annotations.Parameter;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;

@Title("GET /search/buildingSearch")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GetBuildingSearchCompareTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Parameter
    @Parameterized.Parameter(0)
    public String geocoderAddress;

    @Parameterized.Parameters(name = "address={0}")
    public static Collection<String> getParameters() {
        return Arrays.asList(
                "Россия, Санкт-Петербург, улица Верности, 48",
                getRandomString());
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldBuildingSearchHasNoDiffWithProduction() {
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.search().buildingSearchRoute()
                .reqSpec(authSpec())
                .addressQuery(geocoderAddress)
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON);

        assertThat(request.apply(api), jsonEquals(request.apply(prodApi)));
    }
}
