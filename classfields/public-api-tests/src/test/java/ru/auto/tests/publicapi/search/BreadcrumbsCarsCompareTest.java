package ru.auto.tests.publicapi.search;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
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
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.api.SearchApi.BreadcrumbsCarsOper.BC_LOOKUP_QUERY;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;


@DisplayName("GET /search/cars/breadcrumbs")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class BreadcrumbsCarsCompareTest {


    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Parameter("Параметры")
    @Parameterized.Parameter
    public Consumer<RequestSpecBuilder> reqSpec;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters
    public static Collection<Consumer<RequestSpecBuilder>> getParameters() {
        return newArrayList(
                //mark#model#generation#configuration#techParam
                req -> req.addQueryParam(BC_LOOKUP_QUERY, "HONDA"),
                req -> req.addQueryParam(BC_LOOKUP_QUERY, "HONDA#CIVIC"),
                req -> req.addQueryParam(BC_LOOKUP_QUERY, "HONDA#CIVIC#3483480"),
                req -> req.addQueryParam(BC_LOOKUP_QUERY, "HONDA#CIVIC#3483480#8294766"),
                req -> req.addQueryParam(BC_LOOKUP_QUERY, "HONDA#CIVIC#3483480#8294766#20392452")
        );
    }

    @Test
    public void shouldBreadcrumbsCarsNoHasDifferenceWithProduction() {
        Function<ApiClient, JsonObject> req = apiClient -> apiClient.search().breadcrumbsCars().reqSpec(defaultSpec())
                .reqSpec(reqSpec)
                .execute(validatedWith(shouldBeSuccess()))
                .as(JsonObject.class);

        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)));
    }
}