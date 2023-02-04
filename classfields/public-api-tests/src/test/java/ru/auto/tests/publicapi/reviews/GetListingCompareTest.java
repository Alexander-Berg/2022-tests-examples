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
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.api.ReviewsApi.ListingOper.CATEGORY_QUERY;
import static ru.auto.tests.publicapi.api.ReviewsApi.ListingOper.MARK_QUERY;
import static ru.auto.tests.publicapi.api.ReviewsApi.ListingOper.MODEL_QUERY;
import static ru.auto.tests.publicapi.api.ReviewsApi.ListingOper.SUBJECT_PATH;
import static ru.auto.tests.publicapi.api.ReviewsApi.ListingOper.SUB_CATEGORY_QUERY;
import static ru.auto.tests.publicapi.api.ReviewsApi.ListingOper.SUPER_GEN_QUERY;
import static ru.auto.tests.publicapi.model.AutoApiMotoCategories.CategoriesEnum.ATV;
import static ru.auto.tests.publicapi.model.AutoApiMotoCategories.CategoriesEnum.MOTORCYCLE;
import static ru.auto.tests.publicapi.model.AutoApiMotoCategories.CategoriesEnum.SCOOTERS;
import static ru.auto.tests.publicapi.model.AutoApiMotoCategories.CategoriesEnum.SNOWMOBILE;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.MOTO;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.TRUCKS;
import static ru.auto.tests.publicapi.model.AutoApiReview.SubjectEnum.AUTO;
import static ru.auto.tests.publicapi.model.AutoApiReview.SubjectEnum.STO;
import static ru.auto.tests.publicapi.model.AutoApiTruckCategories.CategoriesEnum.ARTIC;
import static ru.auto.tests.publicapi.model.AutoApiTruckCategories.CategoriesEnum.BUS;
import static ru.auto.tests.publicapi.model.AutoApiTruckCategories.CategoriesEnum.LCV;
import static ru.auto.tests.publicapi.model.AutoApiTruckCategories.CategoriesEnum.TRAILER;
import static ru.auto.tests.publicapi.model.AutoApiTruckCategories.CategoriesEnum.TRUCK;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;


@DisplayName("GET /reviews/{subject}/listing")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GetListingCompareTest {

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
                req -> req.addPathParam(SUBJECT_PATH, AUTO),
                req -> req.addPathParam(SUBJECT_PATH, AUTO).addQueryParam(CATEGORY_QUERY, CARS),
                req -> req.addPathParam(SUBJECT_PATH, AUTO).addQueryParam(CATEGORY_QUERY, CARS).addQueryParam(MARK_QUERY, "AUDI").addQueryParam(MODEL_QUERY, "A6").addQueryParam(SUPER_GEN_QUERY, "20246005"),

                req -> req.addPathParam(SUBJECT_PATH, AUTO).addQueryParam(CATEGORY_QUERY, MOTO),
                req -> req.addPathParam(SUBJECT_PATH, AUTO).addQueryParam(CATEGORY_QUERY, MOTO).addQueryParam(SUB_CATEGORY_QUERY, MOTORCYCLE),
                req -> req.addPathParam(SUBJECT_PATH, AUTO).addQueryParam(CATEGORY_QUERY, MOTO).addQueryParam(SUB_CATEGORY_QUERY, MOTORCYCLE).addQueryParam(MARK_QUERY, "BMW").addQueryParam(MODEL_QUERY, "K_1200_LT"),
                req -> req.addPathParam(SUBJECT_PATH, AUTO).addQueryParam(CATEGORY_QUERY, MOTO).addQueryParam(SUB_CATEGORY_QUERY, SCOOTERS),
                req -> req.addPathParam(SUBJECT_PATH, AUTO).addQueryParam(CATEGORY_QUERY, MOTO).addQueryParam(SUB_CATEGORY_QUERY, SCOOTERS).addQueryParam(MARK_QUERY, "HONDA").addQueryParam(MODEL_QUERY, "FORZA"),
                req -> req.addPathParam(SUBJECT_PATH, AUTO).addQueryParam(CATEGORY_QUERY, MOTO).addQueryParam(SUB_CATEGORY_QUERY, ATV),
                req -> req.addPathParam(SUBJECT_PATH, AUTO).addQueryParam(CATEGORY_QUERY, MOTO).addQueryParam(SUB_CATEGORY_QUERY, ATV).addQueryParam(MARK_QUERY, "CF_MOTO").addQueryParam(MODEL_QUERY, "CF800_2__X8_"),
                req -> req.addPathParam(SUBJECT_PATH, AUTO).addQueryParam(CATEGORY_QUERY, MOTO).addQueryParam(SUB_CATEGORY_QUERY, SNOWMOBILE),
                req -> req.addPathParam(SUBJECT_PATH, AUTO).addQueryParam(CATEGORY_QUERY, MOTO).addQueryParam(SUB_CATEGORY_QUERY, SNOWMOBILE).addQueryParam(MARK_QUERY, "BRP").addQueryParam(MODEL_QUERY, "LYNX_YETI"),

                req -> req.addPathParam(SUBJECT_PATH, AUTO).addQueryParam(CATEGORY_QUERY, TRUCKS),
                req -> req.addPathParam(SUBJECT_PATH, AUTO).addQueryParam(CATEGORY_QUERY, TRUCKS).addQueryParam(SUB_CATEGORY_QUERY, TRUCK),
                req -> req.addPathParam(SUBJECT_PATH, AUTO).addQueryParam(CATEGORY_QUERY, TRUCKS).addQueryParam(SUB_CATEGORY_QUERY, TRUCK).addQueryParam(MARK_QUERY, "BAW").addQueryParam(MODEL_QUERY, "FENIX"),
                req -> req.addPathParam(SUBJECT_PATH, AUTO).addQueryParam(CATEGORY_QUERY, TRUCKS).addQueryParam(SUB_CATEGORY_QUERY, BUS),
                req -> req.addPathParam(SUBJECT_PATH, AUTO).addQueryParam(CATEGORY_QUERY, TRUCKS).addQueryParam(SUB_CATEGORY_QUERY, BUS).addQueryParam(MARK_QUERY, "JAC").addQueryParam(MODEL_QUERY, "HK6120"),
                req -> req.addPathParam(SUBJECT_PATH, AUTO).addQueryParam(CATEGORY_QUERY, TRUCKS).addQueryParam(SUB_CATEGORY_QUERY, ARTIC),
                req -> req.addPathParam(SUBJECT_PATH, AUTO).addQueryParam(CATEGORY_QUERY, TRUCKS).addQueryParam(SUB_CATEGORY_QUERY, ARTIC).addQueryParam(MARK_QUERY, "MZSA").addQueryParam(MODEL_QUERY, "817701"),
                req -> req.addPathParam(SUBJECT_PATH, AUTO).addQueryParam(CATEGORY_QUERY, TRUCKS).addQueryParam(SUB_CATEGORY_QUERY, LCV),
                req -> req.addPathParam(SUBJECT_PATH, AUTO).addQueryParam(CATEGORY_QUERY, TRUCKS).addQueryParam(SUB_CATEGORY_QUERY, LCV).addQueryParam(MARK_QUERY, "HYUNDAI").addQueryParam(MODEL_QUERY, "PORTER"),
                req -> req.addPathParam(SUBJECT_PATH, AUTO).addQueryParam(CATEGORY_QUERY, TRUCKS).addQueryParam(SUB_CATEGORY_QUERY, TRAILER),
                req -> req.addPathParam(SUBJECT_PATH, AUTO).addQueryParam(CATEGORY_QUERY, TRUCKS).addQueryParam(SUB_CATEGORY_QUERY, TRAILER).addQueryParam(MARK_QUERY, "MERCEDES").addQueryParam(MODEL_QUERY, "ACTROS_T"),

                req -> req.addPathParam(SUBJECT_PATH, STO)
        );
    }

    @Test
    public void shouldGeoNoHasDifferenceWithProduction() {
        Function<ApiClient, JsonObject> req = apiClient -> apiClient.reviews().listing().reqSpec(defaultSpec())
                .reqSpec(reqSpec)
                .execute(validatedWith(shouldBeSuccess()))
                .as(JsonObject.class);

        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)).whenIgnoringPaths("search_query_id"));
    }
}
