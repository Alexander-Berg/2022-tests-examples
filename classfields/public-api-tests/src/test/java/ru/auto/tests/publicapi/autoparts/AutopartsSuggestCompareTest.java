package ru.auto.tests.publicapi.autoparts;

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
import static ru.auto.tests.publicapi.api.AutopartsApi.SuggestAutoPartsOldOper.CATEGORY_PATH;
import static ru.auto.tests.publicapi.api.AutopartsApi.SuggestAutoPartsOldOper.GEO_RADIUS_QUERY;
import static ru.auto.tests.publicapi.api.AutopartsApi.SuggestAutoPartsOldOper.ONLY_CATEGORIES_QUERY;
import static ru.auto.tests.publicapi.api.AutopartsApi.SuggestAutoPartsOldOper.RID_QUERY;
import static ru.auto.tests.publicapi.api.AutopartsApi.SuggestAutoPartsOldOper.SHIPPING_QUERY;
import static ru.auto.tests.publicapi.api.AutopartsApi.SuggestAutoPartsOldOper.TEXT_QUERY;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;


@DisplayName("GET /autoparts/{category}/suggest")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class AutopartsSuggestCompareTest {

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
                req -> req.addPathParam(CATEGORY_PATH, CARS).addQueryParam(TEXT_QUERY, "капот bmw"),
                req -> req.addPathParam(CATEGORY_PATH, CARS).addQueryParam(TEXT_QUERY, "капот bmw")
                        .addQueryParam(ONLY_CATEGORIES_QUERY, "false")
                        .addQueryParam(RID_QUERY, "213")
                        .addQueryParam(GEO_RADIUS_QUERY, "200")
                        .addQueryParam(SHIPPING_QUERY, "Pickup")
                //todo: добавить параметры
        );
    }

    @Test
    public void shouldHasNoDiffWithProduction() {
        Function<ApiClient, JsonObject> req = apiClient -> apiClient.autoparts().suggestAutoPartsOld().reqSpec(defaultSpec())
                .reqSpec(reqSpec)
                .execute(validatedWith(shouldBeSuccess()))
                .as(JsonObject.class);

        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)));
    }
}
