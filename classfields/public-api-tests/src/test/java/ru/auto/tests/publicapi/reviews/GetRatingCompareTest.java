package ru.auto.tests.publicapi.reviews;

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
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.api.ReviewsApi.RatingOper.CATEGORY_PATH;
import static ru.auto.tests.publicapi.api.ReviewsApi.RatingOper.MARK_QUERY;
import static ru.auto.tests.publicapi.api.ReviewsApi.RatingOper.MODEL_QUERY;
import static ru.auto.tests.publicapi.api.ReviewsApi.RatingOper.SUBJECT_PATH;
import static ru.auto.tests.publicapi.api.ReviewsApi.RatingOper.SUPER_GEN_QUERY;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.MOTO;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.TRUCKS;
import static ru.auto.tests.publicapi.model.AutoApiReview.SubjectEnum.AUTO;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;


@DisplayName("GET /reviews/{subject}/{category}/rating")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GetRatingCompareTest {

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
                req -> req.addPathParam(SUBJECT_PATH, AUTO).addPathParam(CATEGORY_PATH, CARS).addQueryParam(MARK_QUERY, "AUDI").addQueryParam(MODEL_QUERY, "A6").addQueryParam(SUPER_GEN_QUERY, "20246005"),
                req -> req.addPathParam(SUBJECT_PATH, AUTO).addPathParam(CATEGORY_PATH, MOTO).addQueryParam(MARK_QUERY, "BMW").addQueryParam(MODEL_QUERY, "K_1200_LT"),
                req -> req.addPathParam(SUBJECT_PATH, AUTO).addPathParam(CATEGORY_PATH, TRUCKS).addQueryParam(MARK_QUERY, "BAW").addQueryParam(MODEL_QUERY, "FENIX")
        );
    }

    @Test
    public void shouldGetRatingNoHasDifferenceWithProduction() {
        Function<ApiClient, JsonObject> req = apiClient -> apiClient.reviews().rating().reqSpec(defaultSpec())
                .reqSpec(reqSpec)
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class);

        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)));
    }
}
