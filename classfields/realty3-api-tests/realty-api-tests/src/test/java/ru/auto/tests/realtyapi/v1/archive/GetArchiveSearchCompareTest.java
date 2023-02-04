package ru.auto.tests.realtyapi.v1.archive;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.restassured.builder.RequestSpecBuilder;
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

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;
import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.v1.api.ArchiveApi.ArchiveSearchRouteOper.ADDRESS_QUERY;
import static ru.auto.tests.realtyapi.v1.api.ArchiveApi.ArchiveSearchRouteOper.CATEGORY_QUERY;
import static ru.auto.tests.realtyapi.v1.api.ArchiveApi.ArchiveSearchRouteOper.ROOMS_TOTAL_QUERY;
import static ru.auto.tests.realtyapi.v1.api.ArchiveApi.ArchiveSearchRouteOper.TYPE_QUERY;
import static ru.auto.tests.realtyapi.v1.archive.GetArchiveSearchTest.ADDRESS;
import static ru.auto.tests.realtyapi.v1.model.RealtyResponseOfferResponse.OfferCategoryEnum.APARTMENT;
import static ru.auto.tests.realtyapi.v1.model.RealtyResponseOfferResponse.OfferCategoryEnum.ROOMS;
import static ru.auto.tests.realtyapi.v1.model.RealtyResponseOfferResponse.OfferTypeEnum.SELL;
import static ru.auto.tests.realtyapi.v1.testdata.TestData.defaultNumberOfRooms;
import static ru.auto.tests.realtyapi.v1.testdata.TestData.defaultOfferCategory;
import static ru.auto.tests.realtyapi.v1.testdata.TestData.defaultOfferType;
import static ru.auto.tests.realtyapi.v1.testdata.TestData.validAddresses;


@Title("GET /archive/search.json")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GetArchiveSearchCompareTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Parameter("Спецификация")
    @Parameterized.Parameter(0)
    public Consumer<RequestSpecBuilder> reqSpec;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters
    public static Collection<Consumer<RequestSpecBuilder>> getParameters() {
        List<Consumer<RequestSpecBuilder>> params = newArrayList();
        validAddresses().forEach(address ->
                params.add(req -> req.addParam(ADDRESS_QUERY, address).addParam(TYPE_QUERY, SELL)
                        .addParam(CATEGORY_QUERY, APARTMENT)));
        defaultOfferType().forEach(offerType ->
                params.add(req -> req.addParam(ADDRESS_QUERY, ADDRESS).addParam(TYPE_QUERY, offerType)
                        .addParam(CATEGORY_QUERY, APARTMENT)));
        defaultOfferCategory().forEach(offerCategory ->
                params.add(req -> req.addParam(ADDRESS_QUERY, ADDRESS).addParam(TYPE_QUERY, SELL)
                        .addParam(CATEGORY_QUERY, offerCategory)));
        defaultNumberOfRooms().forEach(numberOfRooms ->
                params.add(req -> req.addParam(ADDRESS_QUERY, ADDRESS).addParam(TYPE_QUERY, SELL)
                        .addParam(CATEGORY_QUERY, ROOMS).addParam(ROOMS_TOTAL_QUERY, numberOfRooms)));
        return params;
    }

    @Test
    public void shouldAddressHasNoDiffWithProduction() {
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.archive().archiveSearchRoute()
                .reqSpec(authSpec()).reqSpec(reqSpec)
                .execute(validatedWith(shouldBe200OkJSON())).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(api), jsonEquals(request.apply(prodApi)));
    }
}

