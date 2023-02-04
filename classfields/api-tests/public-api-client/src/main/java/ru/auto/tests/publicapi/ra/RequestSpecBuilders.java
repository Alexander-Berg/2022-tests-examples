package ru.auto.tests.publicapi.ra;

import io.restassured.builder.RequestSpecBuilder;
import ru.auto.tests.publicapi.api.CatalogApi;
import ru.auto.tests.publicapi.api.SearchApi;
import ru.auto.tests.publicapi.api.VideoApi;
import ru.auto.tests.publicapi.model.AutoApiOffer;

import java.util.function.Consumer;

import static io.restassured.http.ContentType.JSON;
import static io.restassured.http.ContentType.XML;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.model.AutoApiSearchSearchRequestParameters.CurrencyEnum;
import static ru.auto.tests.publicapi.model.AutoApiSearchSearchRequestParameters.CustomsStateGroupEnum;
import static ru.auto.tests.publicapi.model.AutoApiSearchSearchRequestParameters.DamageGroupEnum;
import static ru.auto.tests.publicapi.model.AutoApiSearchSearchRequestParameters.StateGroupEnum;

public class RequestSpecBuilders {

    public static final String UBER_TRACE_ID = "uber-trace-id";


    private RequestSpecBuilders() {
    }

    public static Consumer<RequestSpecBuilder> defaultSpec() {
        return requestSpec -> requestSpec
                .addHeader("x-authorization", "Vertis swagger")
                .addHeader("x-real-ip", "0:0:0:0:0:0:0:1");
    }

    public static Consumer<RequestSpecBuilder> xUserCMExpertAliases() {
        return requestSpec -> requestSpec
                .addHeader("x-authorization", "cmexpert-9814ee29475f052718641dc9b1e3dd9732ca2672")
                .addHeader("x-real-ip", "0:0:0:0:0:0:0:1");
    }

    public static Consumer<RequestSpecBuilder> xUserLocationHeader(String value) {
        return requestSpec -> requestSpec
                .addHeader("X-User-Location", value);
    }

    public static Consumer<RequestSpecBuilder> withJsonBody(String json) {
        return requestSpec ->
                requestSpec.setContentType(JSON).setBody(json);
    }

    public static Consumer<RequestSpecBuilder> withXmlBody(String xml) {
        return requestSpec ->
                requestSpec.setContentType(XML.withCharset("UTF-8")).setBody(xml);
    }

    public static Consumer<RequestSpecBuilder> withDefaultSearchQuery() {

        //default for apps
        //TODO: добавить pagination & sort когда будут перезаписываться query-параметры
        return requestSpec -> requestSpec
                .addQueryParam(SearchApi.SearchCarsOper.CONTEXT_QUERY, "listing")
                .addQueryParam(SearchApi.SearchCarsOper.CURRENCY_QUERY, CurrencyEnum.RUR)
                .addQueryParam(SearchApi.SearchCarsOper.HAS_IMAGE_QUERY, true)
                .addQueryParam(SearchApi.SearchCarsOper.STATE_GROUP_QUERY, StateGroupEnum.ALL)
                .addQueryParam(SearchApi.SearchCarsOper.DAMAGE_GROUP_QUERY, DamageGroupEnum.NOT_BEATEN)
                .addQueryParam(SearchApi.SearchCarsOper.CUSTOMS_STATE_GROUP_QUERY, CustomsStateGroupEnum.CLEARED);
    }

    public static Consumer<RequestSpecBuilder> withDefaultVideoQuery() {
        return withVideoQuery(AutoApiOffer.CategoryEnum.CARS, "LADA (ВАЗ)", "Гранта", "Sport");
    }

    public static Consumer<RequestSpecBuilder> withVideoQuery(
            AutoApiOffer.CategoryEnum categoryPath,
            String markQuery,
            String modelQuery,
            String superGenQuery
    ) {
        return requestSpec -> {
            requestSpec.addPathParam(VideoApi.SearchVideoOper.CATEGORY_PATH, categoryPath.name());
            if (markQuery != null) requestSpec.addQueryParam(VideoApi.SearchVideoOper.MARK_QUERY, markQuery);
            if (modelQuery != null) requestSpec.addQueryParam(VideoApi.SearchVideoOper.MODEL_QUERY, modelQuery);
            if (superGenQuery != null) requestSpec.addQueryParam(VideoApi.SearchVideoOper.SUPER_GEN_QUERY, superGenQuery);
        };
    }

    public static Consumer<RequestSpecBuilder> withDefaultDictionaryFormatPath() {
        return requestSpec -> requestSpec
                .addPathParam(CatalogApi.DictionariesListOper.FORMAT_PATH, "v1");
    }

    public static Consumer<RequestSpecBuilder> withDefaultDictionaryPaths() {
        return withDefaultDictionaryFormatPath()
                .andThen(req -> req.addPathParam(CatalogApi.DictionaryOper.CATEGORY_PATH, CARS)
                .addPathParam(CatalogApi.DictionaryOper.DICTIONARY_PATH, "body_type"));
    }
}
