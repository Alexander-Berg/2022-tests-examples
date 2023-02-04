package ru.auto.tests.realty.vos2.compare;


import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.tngtech.java.junit.dataprovider.DataProvider;
import io.qameta.allure.Issue;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.MatcherAssert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.runners.GuiceDataProviderRunner;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.realty.vos2.ApiClient;
import ru.auto.tests.realty.vos2.anno.Compare;
import ru.auto.tests.realty.vos2.anno.Prod;
import ru.auto.tests.realty.vos2.module.Vos2ApiCompareModule;

import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.realty.vos2.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBeStatusOk;

@DisplayName("Сравнение выдачи с заранее заготовленным пользователем в VOS2")
@RunWith(GuiceDataProviderRunner.class)
@GuiceModules(Vos2ApiCompareModule.class)
@Issue("VERTISTEST-729")
@Ignore
public class CompareTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    @Compare
    private Account account;

    @Inject
    private ApiClient vos2;

    @Inject
    @Prod
    private ApiClient prodVos2;

    @Test
    @DisplayName("GET /api/realty/user/{userID}/redirect_phones")
    public void shouldRedirectPhonesHasNotDiffWithProduction() {
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.redirectPhones().statusRoute()
                .userIDPath(account.getId()).execute(validatedWith(shouldBeStatusOk())).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(vos2), jsonEquals(request.apply(prodVos2)));
    }

//    @Test
//    @DisplayName("GET /api/realty/renewals/{userID}")
//    public void shouldRenewalsHasNotDiffWithProduction() {
//        String all = "ALL";
//        Function<ApiClient, JsonObject> request = apiClient -> apiClient.renewals().getRoute()
//                .userIDPath(account.getId()).showStatusQuery(all)
//                .execute(validatedWith(shouldBe200OkJSON())).as(JsonObject.class, GSON);
//
//        MatcherAssert.assertThat(request.apply(vos2), jsonEquals(request.apply(prodVos2)));
//    }

    @Test
    @DisplayName("GET /api/realty/user/{userID}")
    public void shouldUserHasNotDiffWithProduction() {
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.user().getUserRoute()
                .userIDPath(account.getId()).execute(validatedWith(shouldBeStatusOk())).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(vos2), jsonEquals(request.apply(prodVos2)));
    }

    @Test
    @DisplayName("GET /user/subscriptions/get/{userID}")
    public void shouldSubscriptionsHasNotDiffWithProduction() {
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.userSubscriptions().getRoute()
                .userIDPath(account.getId()).execute(validatedWith(shouldBe200Ok())).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(vos2), jsonEquals(request.apply(prodVos2)));
    }

    @Test
    @Issue("VOS-2557")
    @DisplayName("GET /utils/features")
    public void shouldAllFeaturesHasNotDiffWithProduction() {
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.utils().utilsAllFeatures()
                .execute(validatedWith(shouldBe200OkJSON())).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(vos2), jsonEquals(request.apply(prodVos2)));
    }

    @Test
    @DataProvider({"watching"})
    @DisplayName("GET /utils/features/{feature}")
    public void shouldFeatureHasNotDiffWithProduction(String feature) {
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.utils().utilsGetFeature().featurePath(feature)
                .execute(validatedWith(shouldBe200OkJSON())).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(vos2), jsonEquals(request.apply(prodVos2)));
    }

    @Test
    @DisplayName("GET /utils/raw-user/{userRef}")
    public void shouldRawUserHasNotDiffWithProduction() {
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.utils().utilsRawUser()
                .userRefPath(getUserRef(account.getId()))
                .execute(validatedWith(shouldBe200OkJSON())).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(vos2), jsonEquals(request.apply(prodVos2)));
    }

    @Test
    @DisplayName("GET /api/realty/user_offers/{userID}/statistics")
    public void shouldStatisticsHasNotDiffWithProduction() {
        Function<ApiClient, JsonArray> request = apiClient -> apiClient.userOffers().getStatisticsV2Route()
                .userIDPath(account.getId()).execute(validatedWith(shouldBe200OkJSON())).as(JsonArray.class, GSON);

        MatcherAssert.assertThat(request.apply(vos2), jsonEquals(request.apply(prodVos2)));
    }

    @Test
    @DisplayName("GET /api/realty/user_offers/{userID}/statisticsV2")
    public void shouldStatisticsV2HasNotDiffWithProduction() {
        Function<ApiClient, JsonArray> request = apiClient -> apiClient.userOffers().getStatisticsV2Route()
                .userIDPath(account.getId()).execute(validatedWith(shouldBe200OkJSON())).as(JsonArray.class, GSON);

        MatcherAssert.assertThat(request.apply(vos2), jsonEquals(request.apply(prodVos2)));
    }


    @Test
    @DisplayName("GET /api/realty/user_offers/{userID}")
    public void shouldUserOffersHasNotDiffWithProduction() {
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.userOffers().userOffersRoute()
                .userIDPath(account.getId()).execute(validatedWith(shouldBeStatusOk())).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(vos2), jsonEquals(request.apply(prodVos2)));
    }

    private static String getUserRef(String uid) {
        return String.format("uid_%s", uid);
    }
}
