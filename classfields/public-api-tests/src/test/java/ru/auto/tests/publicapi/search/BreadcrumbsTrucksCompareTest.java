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
import static ru.auto.tests.publicapi.api.SearchApi.BreadcrumbsTrucksOper.BC_LOOKUP_QUERY;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;


@DisplayName("GET /search/trucks/breadcrumbs")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class BreadcrumbsTrucksCompareTest {

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
                req -> req.addQueryParam(BC_LOOKUP_QUERY, "TRUCK"),
                req -> req.addQueryParam(BC_LOOKUP_QUERY, "TRUCK#BAW"),
                req -> req.addQueryParam(BC_LOOKUP_QUERY, "TRUCK#BAW#FENIX"),
                req -> req.addQueryParam(BC_LOOKUP_QUERY, "BUS"),
                req -> req.addQueryParam(BC_LOOKUP_QUERY, "BUS#JAC"),
                req -> req.addQueryParam(BC_LOOKUP_QUERY, "BUS#JAC#HK6120"),
                req -> req.addQueryParam(BC_LOOKUP_QUERY, "ARTIC"),
                req -> req.addQueryParam(BC_LOOKUP_QUERY, "ARTIC#DAF"),
                req -> req.addQueryParam(BC_LOOKUP_QUERY, "ARTIC#DAF#XF105"),
                req -> req.addQueryParam(BC_LOOKUP_QUERY, "LCV"),
                req -> req.addQueryParam(BC_LOOKUP_QUERY, "LCV#HYUNDAI"),
                req -> req.addQueryParam(BC_LOOKUP_QUERY, "LCV#HYUNDAI#PORTER"),
                req -> req.addQueryParam(BC_LOOKUP_QUERY, "TRAILER"),
                req -> req.addQueryParam(BC_LOOKUP_QUERY, "TRAILER#ABI"),
                req -> req.addQueryParam(BC_LOOKUP_QUERY, "TRAILER#ABI#JUBILEE")
        );
    }

    @Test
    public void shouldBreadcrumbsTrucksNoHasDifferenceWithProduction() {
        Function<ApiClient, JsonObject> req = apiClient -> apiClient.search().breadcrumbsTrucks().reqSpec(defaultSpec())
                .reqSpec(reqSpec)
                .execute(validatedWith(shouldBeSuccess()))
                .as(JsonObject.class);

        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)));
    }
}