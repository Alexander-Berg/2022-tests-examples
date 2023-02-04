package ru.auto.tests.publicapi.magazin;

import com.carlosbecker.guice.GuiceModules;
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
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.api.MagazineApi.ArticlesListingOper.PAGE_QUERY;
import static ru.auto.tests.publicapi.api.MagazineApi.ArticlesListingOper.PAGE_SIZE_QUERY;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

/**
* Generated compare test for ArticlesListing
*/
@DisplayName("GET /magazine/articles/snippets")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ArticlesListingCompareTest {

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
    return newArrayList(req -> req
        //   .addQueryParam(ArticlesListingOper.CATEGORY_QUERY, Arrays.asList())
        //   .addQueryParam(ArticlesListingOper.MARK_QUERY, "mark_example")
        //   .addQueryParam(ArticlesListingOper.MODEL_QUERY, "model_example")
        //   .addQueryParam(ArticlesListingOper.SUPER_GEN_ID_QUERY, Arrays.asList())
           .addQueryParam(PAGE_QUERY, 1)
           .addQueryParam(PAGE_SIZE_QUERY, 10)
        //   .addQueryParam(ArticlesListingOper.SORT_QUERY, "sort_example")
);
    }

    @Test
    @Owner("generated")
    @Description("Compare json response for GET /magazine/articles/snippets")
    public void shouldArticlesListingHasNoDiffWithProduction() {
        Function<ApiClient, JsonObject> req = apiClient -> apiClient.magazine().articlesListing()
            .reqSpec(defaultSpec())
            .reqSpec(reqSpec).execute(validatedWith(shouldBeSuccess())).as(JsonObject.class);
        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)));
    }
}
