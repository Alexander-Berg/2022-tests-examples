package ru.auto.tests.realtyapi.v1.suggest;

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
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.yandex.qatools.allure.annotations.Parameter;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static java.util.Arrays.stream;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.v1.testdata.TestData.defaultOfferCategory;
import static ru.auto.tests.realtyapi.v1.testdata.TestData.defaultOfferType;

@Title("GET /suggest/tags")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GetSuggestTagsCompareTest {

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
    public String text;

    @Parameter
    @Parameterized.Parameter(1)
    public String type;

    @Parameter
    @Parameterized.Parameter(2)
    public String category;

    @Parameterized.Parameters(name = "text={0} type={1} category={2}")
    public static List<Object[]> getParameters() {
        List<Object[]> parameters = new ArrayList<>();
        Object[] textQuery = new Object[]{"окна", "а", "1234", EMPTY};

        stream(textQuery).forEach(text ->
                defaultOfferCategory().forEach(category ->
                        defaultOfferType().forEach(type ->
                                parameters.add(new Object[]{text, type.getValue(), category.getValue()}))));
        return parameters;
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldDeveloperSuggestHasNoDiffWithProduction() {
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.suggest().suggestTagRoute()
                .reqSpec(authSpec())
                .textQuery(text)
                .typeQuery(type)
                .categoryQuery(category)
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(api), jsonEquals(request.apply(prodApi)));
    }
}
