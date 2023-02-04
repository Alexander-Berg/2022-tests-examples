package ru.auto.tests.example;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonArray;
import com.google.inject.Inject;
import io.qameta.allure.Description;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.builder.RequestSpecBuilder;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.example.anno.Prod;
import ru.auto.tests.example.module.ExampleApiModule;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.example.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.example.api.PetApi.FindPetsByStatusOper.STATUS_QUERY;
import static ru.auto.tests.example.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;

/**
 * Generated compare test for FindPetsByStatus
 */
@DisplayName("GET /pet/findByStatus")
@GuiceModules(ExampleApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class FindPetsByStatusCompareTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Parameterized.Parameter
    public Consumer<RequestSpecBuilder> reqSpec;

    @Parameterized.Parameters
    public static Collection<Consumer<RequestSpecBuilder>> getParameters() {
        return newArrayList(req -> req.and()
                .addQueryParam(STATUS_QUERY, Arrays.asList("available"))
        );
    }

    @Test
    @Owner("generated")
    @Description("Compare json response for GET /pet/findByStatus")
    public void shouldFindPetsByStatusHasNoDiffWithProduction() {
        Function<ApiClient, JsonArray> req = apiClient -> apiClient.pet().findPetsByStatus()
                .reqSpec(defaultSpec())
                .reqSpec(reqSpec).execute(validatedWith(shouldBe200OkJSON())).as(JsonArray.class);
        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)));
    }
}
