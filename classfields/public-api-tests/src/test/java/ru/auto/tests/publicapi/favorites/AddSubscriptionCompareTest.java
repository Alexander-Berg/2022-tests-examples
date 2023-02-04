package ru.auto.tests.publicapi.favorites;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import edu.emory.mathcs.backport.java.util.Arrays;
import io.qameta.allure.Description;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.model.AutoApiSavedSearchCreateParams;
import ru.auto.tests.publicapi.model.AutoApiSearchCatalogFilter;
import ru.auto.tests.publicapi.model.AutoApiSearchMotoSearchRequestParameters;
import static ru.auto.tests.publicapi.model.AutoApiSearchMotoSearchRequestParameters.MotoCategoryEnum.ATV;
import ru.auto.tests.publicapi.model.AutoApiSearchSearchRequestParameters;
import ru.auto.tests.publicapi.model.AutoApiSearchTrucksSearchRequestParameters;
import static ru.auto.tests.publicapi.model.AutoApiSearchTrucksSearchRequestParameters.TrucksCategoryEnum.LCV;
import ru.auto.tests.publicapi.model.VertisPassportSession;
import ru.auto.tests.publicapi.module.PublicApiSearchesModule;
import ru.auto.tests.publicapi.utils.DeviceUidKeeper;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.Collection;

import static com.google.common.collect.Lists.newArrayList;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.consts.Owners.DSKUZNETSOV;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.MOTO;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.TRUCKS;
import static ru.auto.tests.publicapi.model.AutoApiSearchMotoSearchRequestParameters.MotoCategoryEnum.ATV;
import static ru.auto.tests.publicapi.model.AutoApiSearchMotoSearchRequestParameters.MotoCategoryEnum.MOTORCYCLE;
import static ru.auto.tests.publicapi.model.AutoApiSearchMotoSearchRequestParameters.MotoCategoryEnum.SCOOTERS;
import static ru.auto.tests.publicapi.model.AutoApiSearchMotoSearchRequestParameters.MotoCategoryEnum.SNOWMOBILE;
import static ru.auto.tests.publicapi.model.AutoApiSearchTrucksSearchRequestParameters.TrucksCategoryEnum.ARTIC;
import static ru.auto.tests.publicapi.model.AutoApiSearchTrucksSearchRequestParameters.TrucksCategoryEnum.BULLDOZERS;
import static ru.auto.tests.publicapi.model.AutoApiSearchTrucksSearchRequestParameters.TrucksCategoryEnum.BUS;
import static ru.auto.tests.publicapi.model.AutoApiSearchTrucksSearchRequestParameters.TrucksCategoryEnum.LCV;
import static ru.auto.tests.publicapi.model.AutoApiSearchTrucksSearchRequestParameters.TrucksCategoryEnum.TRAILER;
import static ru.auto.tests.publicapi.model.AutoApiSearchTrucksSearchRequestParameters.TrucksCategoryEnum.TRUCK;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;

@DisplayName("POST /user/favorites/{category}/subscriptions")
@GuiceModules(PublicApiSearchesModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class AddSubscriptionCompareTest {
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Inject
    private PublicApiAdaptor adaptor;

    @Inject
    private DeviceUidKeeper deviceUidKeeper;

    @Parameter("Категория")
    @Parameterized.Parameter
    public CategoryEnum category;

    @Parameter("Параметры")
    @Parameterized.Parameter(1)
    public AutoApiSearchSearchRequestParameters params;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(new Object[][]{
                {CARS, new AutoApiSearchSearchRequestParameters()},
                {CARS, new AutoApiSearchSearchRequestParameters().catalogFilter(newArrayList(new AutoApiSearchCatalogFilter()._configuration(20368784L).generation(20368749L).model("Q7").mark("AUDI")))},
                {CARS, new AutoApiSearchSearchRequestParameters().catalogFilter(newArrayList(new AutoApiSearchCatalogFilter().vendor("VENDOR1")))},
                {CARS, new AutoApiSearchSearchRequestParameters().catalogFilter(newArrayList(new AutoApiSearchCatalogFilter().mark("AUDI")))},
                {CARS, new AutoApiSearchSearchRequestParameters().catalogFilter(newArrayList(new AutoApiSearchCatalogFilter().mark("AUDI").model("A4")))},
                {CARS, new AutoApiSearchSearchRequestParameters().catalogFilter(newArrayList(new AutoApiSearchCatalogFilter().mark("AUDI").model("A4").generation(20637504L)))},
                {CARS, new AutoApiSearchSearchRequestParameters().catalogFilter(newArrayList(new AutoApiSearchCatalogFilter().mark("BMW").model("3ER").nameplate(10202930L)))},
                {CARS, new AutoApiSearchSearchRequestParameters().catalogFilter(newArrayList(new AutoApiSearchCatalogFilter().techParam(21081173L)._configuration(21081114L).generation(21081076L).model("CONTINENTAL_GT").mark("BENTLEY")))},

                // @see AUTORUBACK-2689
                // @see AUTORUBACK-2683
                // Simplified example with mismatched generation and nameplate - should return an empty list.
                {CARS, new AutoApiSearchSearchRequestParameters()
                 .catalogFilter(
                     newArrayList(
                         new AutoApiSearchCatalogFilter().mark("BMW").model("X3").generation(20156797L).nameplateName("m40d")
                     )
                  )},
                // Full example with multiple generation and nameplate combinations, some of them mismatched - should return a few results.
                {CARS, new AutoApiSearchSearchRequestParameters()
                 .catalogFilter(
                     newArrayList(
                         new AutoApiSearchCatalogFilter().mark("BMW").model("X3").generation(20156797L).nameplateName("35d"),
                         new AutoApiSearchCatalogFilter().mark("BMW").model("X3").generation(20156797L).nameplateName("m40d"),
                         new AutoApiSearchCatalogFilter().mark("BMW").model("X3").generation(21029610L).nameplateName("35d"),
                         new AutoApiSearchCatalogFilter().mark("BMW").model("X3").generation(21029610L).nameplateName("m40d")
                     ))},
                {CARS, new AutoApiSearchSearchRequestParameters().markModelNameplate(newArrayList("VENDOR1"))},
                {CARS, new AutoApiSearchSearchRequestParameters().markModelNameplate(newArrayList("CHERY#INDIS", "VENDOR2"))},
                {CARS, new AutoApiSearchSearchRequestParameters().markModelNameplate(newArrayList("AUDI"))},
                {CARS, new AutoApiSearchSearchRequestParameters().markModelNameplate(newArrayList("AUDI#A4"))},
                {CARS, new AutoApiSearchSearchRequestParameters().markModelNameplate(newArrayList("AUDI#A4##20637504"))},
                {CARS, new AutoApiSearchSearchRequestParameters().markModelNameplate(newArrayList("BMW#3ER#10202930"))},
                {CARS, new AutoApiSearchSearchRequestParameters().groupingId("tech_param_id=21081173,complectation_id=0")},
                {CARS, new AutoApiSearchSearchRequestParameters().groupingId("tech_param_id=20972235,complectation_id=20972429").markModelNameplate(newArrayList("MITSUBISHI#PAJERO_SPORT##20663923"))},
                {MOTO, new AutoApiSearchSearchRequestParameters().motoParams(new AutoApiSearchMotoSearchRequestParameters().motoCategory(ATV))},
                {MOTO, new AutoApiSearchSearchRequestParameters().motoParams(new AutoApiSearchMotoSearchRequestParameters().motoCategory(SCOOTERS))},
                {MOTO, new AutoApiSearchSearchRequestParameters().motoParams(new AutoApiSearchMotoSearchRequestParameters().motoCategory(MOTORCYCLE))},
                {MOTO, new AutoApiSearchSearchRequestParameters().motoParams(new AutoApiSearchMotoSearchRequestParameters().motoCategory(SNOWMOBILE))},
                {MOTO, new AutoApiSearchSearchRequestParameters().motoParams(new AutoApiSearchMotoSearchRequestParameters().motoCategory(SNOWMOBILE))},
                {MOTO, new AutoApiSearchSearchRequestParameters().markModelNameplate(newArrayList("HONDA")).motoParams(new AutoApiSearchMotoSearchRequestParameters().motoCategory(ATV))},
                {MOTO, new AutoApiSearchSearchRequestParameters().markModelNameplate(newArrayList("HONDA#ATC_200X")).motoParams(new AutoApiSearchMotoSearchRequestParameters().motoCategory(ATV))},
                {TRUCKS, new AutoApiSearchSearchRequestParameters().trucksParams(new AutoApiSearchTrucksSearchRequestParameters().trucksCategory(LCV))},
                {TRUCKS, new AutoApiSearchSearchRequestParameters().trucksParams(new AutoApiSearchTrucksSearchRequestParameters().trucksCategory(TRUCK))},
                {TRUCKS, new AutoApiSearchSearchRequestParameters().trucksParams(new AutoApiSearchTrucksSearchRequestParameters().trucksCategory(ARTIC))},
                {TRUCKS, new AutoApiSearchSearchRequestParameters().trucksParams(new AutoApiSearchTrucksSearchRequestParameters().trucksCategory(BUS))},
                {TRUCKS, new AutoApiSearchSearchRequestParameters().trucksParams(new AutoApiSearchTrucksSearchRequestParameters().trucksCategory(TRAILER))},
                {TRUCKS, new AutoApiSearchSearchRequestParameters().trucksParams(new AutoApiSearchTrucksSearchRequestParameters().trucksCategory(BULLDOZERS))},
                {TRUCKS, new AutoApiSearchSearchRequestParameters().markModelNameplate(newArrayList("CITROEN")).trucksParams(new AutoApiSearchTrucksSearchRequestParameters().trucksCategory(LCV))},
                {TRUCKS, new AutoApiSearchSearchRequestParameters().markModelNameplate(newArrayList("CITROEN#NEMO")).trucksParams(new AutoApiSearchTrucksSearchRequestParameters().trucksCategory(LCV))}
        });
    }

    @Test
    @Owner(DSKUZNETSOV)
    @Issue("AUTORUAPI-6279")
    @Description("Сравниваем с продакшном ответы запросов на сохранение поисков от двух разных юзеров(сессий и девайсов)")
    public void shouldAddSubscriptionHasNoDiffWithProduction() {
        VertisPassportSession sessionResponse = adaptor.session().getSession();
        String sessionId = sessionResponse.getId();
        String deviceUid = sessionResponse.getDeviceUid();
        deviceUidKeeper.add(deviceUid);

        VertisPassportSession anotherSessionResponse = adaptor.session().getSession();
        String anotherSessionId = anotherSessionResponse.getId();
        String anotherDeviceUid = anotherSessionResponse.getDeviceUid();
        deviceUidKeeper.add(anotherDeviceUid);

        JsonObject response = api.userFavorites().addSavedSearch().categoryPath(category)
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId).xDeviceUidHeader(deviceUid)
                .body(new AutoApiSavedSearchCreateParams().params(params))
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class);


        JsonObject prodResponse = prodApi.userFavorites().addSavedSearch().categoryPath(category)
                .reqSpec(defaultSpec()).xSessionIdHeader(anotherSessionId).xDeviceUidHeader(anotherDeviceUid)
                .body(new AutoApiSavedSearchCreateParams().params(params))
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class);

        MatcherAssert.assertThat(response, jsonEquals(prodResponse));
    }
}
