package ru.auto.tests.publicapi.features;

import com.carlosbecker.guice.GuiceModules;
import com.google.common.base.Functions;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

/**
 * Generated compare test for FeaturesList
 */
@DisplayName("GET /features")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class FeaturesListCompareTest {

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
        return newArrayList(req -> Functions.identity());
    }

    @Test
    @Owner("generated")
    @Description("Compare json response for GET /features")
    public void shouldFeaturesListHasNoDiffWithProduction() {
        Function<ApiClient, JsonArray> req = apiClient -> apiClient.features().getFeatureList()
                .reqSpec(defaultSpec())
                .reqSpec(reqSpec).execute(validatedWith(shouldBe200OkJSON())).as(JsonArray.class);
        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)));
    }
}
