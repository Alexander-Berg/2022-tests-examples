package ru.auto.tests.publicapi.carfax;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.module.PublicApiModule;

import java.time.Duration;
import java.util.function.Function;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;


@DisplayName("GET /carfax/offer/{category}/{offerID}/raw")
@RunWith(GuiceTestRunner.class)
@GuiceModules(PublicApiModule.class)
public class GetOfferRawReportTest {
    static long TIME_DAY_AGO = System.currentTimeMillis() - Duration.ofDays(1).toMillis();

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Test
    public void shouldCarsWithHistoryHasNoDiffWithProduction() {
        String offerId = prodApi.search().searchCars().pageQuery(1).pageSizeQuery(1).reqSpec(defaultSpec())
                .searchTagQuery("vin_offers_history").creationDateToQuery(TIME_DAY_AGO)
                .executeAs(validatedWith(shouldBe200OkJSON())).getOffers().get(0).getId();

        Function<ApiClient, JsonObject> req = apiClient -> apiClient.carfax().offerRawReport()
                .categoryPath(CARS).offerIDPath(offerId).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeSuccess())).as(JsonObject.class);

        MatcherAssert.assertThat(req.apply(api), jsonEquals(req.apply(prodApi))
                .whenIgnoringPaths(RawReportUtils.IGNORED_PATHS));
    }
}
