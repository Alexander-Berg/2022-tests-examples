package ru.auto.tests.publicapi.favorites;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.mapper.ObjectMapperType;
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
import ru.auto.tests.publicapi.model.AutoApiSearchSearchRequestParameters;
import ru.auto.tests.publicapi.module.PublicApiSearchesModule;
import ru.yandex.qatools.allure.annotations.Parameter;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.publicapi.consts.Owners.DSKUZNETSOV;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

@DisplayName("GET /user/favorites/all/subscriptions")
@GuiceModules(PublicApiSearchesModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GetSubscriptionListCompareTest {
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    private PublicApiAdaptor adaptor;

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
                req -> req.setBody(new AutoApiSavedSearchCreateParams().params(new AutoApiSearchSearchRequestParameters().addMarkModelNameplateItem("BMW"))),
                req -> req.setBody(new AutoApiSavedSearchCreateParams().params(new AutoApiSearchSearchRequestParameters().addMarkModelNameplateItem("BMW#3ER"))),
                req -> req.setBody(new AutoApiSavedSearchCreateParams().params(new AutoApiSearchSearchRequestParameters().addMarkModelNameplateItem("BMW#3ER#10202930"))),
                req -> req.setBody(new AutoApiSavedSearchCreateParams().params(new AutoApiSearchSearchRequestParameters().addMarkModelNameplateItem("BMW#3ER#10202930#20548423"))),
                req -> req.setBody(new AutoApiSavedSearchCreateParams().params(new AutoApiSearchSearchRequestParameters().addMarkModelNameplateItem("BMW#3ER##20548423"))),
                req -> req.setBody(new AutoApiSavedSearchCreateParams().params(new AutoApiSearchSearchRequestParameters().addMarkModelNameplateItem("BMW#3ER#10202930#20548423").addMarkModelNameplateItem("VENDOR1"))),
                req -> req.setBody(new AutoApiSavedSearchCreateParams().params(new AutoApiSearchSearchRequestParameters().groupingId("tech_param_id=21398903,complectation_id=21402379")))
        );
    }

    @Test
    @Owner(DSKUZNETSOV)
    public void shouldSubscriptionsListHasNoDifferenceWithProduction() {
        String deviceUid = adaptor.getDeviceUidByAddingSubscription(CARS, reqSpec);

        Function<ApiClient, JsonObject> req = apiClient -> apiClient.userFavorites().getSavedSearches()
                .reqSpec(defaultSpec()).xDeviceUidHeader(deviceUid)
                .execute(validatedWith(shouldBeSuccess())).as(JsonObject.class, ObjectMapperType.GSON);

        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi)));
    }
}
