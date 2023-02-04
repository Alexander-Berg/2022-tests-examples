package ru.auto.tests.publicapi.carfax;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.apache.commons.lang3.RandomUtils;
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

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.publicapi.carfax.GetOfferRawReportTest.TIME_DAY_AGO;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.model.AutoApiVinResolution.StatusEnum.ERROR;
import static ru.auto.tests.publicapi.model.AutoApiVinResolution.StatusEnum.INVALID;
import static ru.auto.tests.publicapi.model.AutoApiVinResolution.StatusEnum.OK;
import static ru.auto.tests.publicapi.model.AutoApiVinResolution.StatusEnum.UNTRUSTED;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;

@DisplayName("GET /carfax/offer/{category}/{offerID}/raw")
@GuiceModules(PublicApiModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class GetOfferRawReportCompareTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Inject
    @Prod
    private ApiClient prodApi;

    @Parameter("Таг")
    @Parameterized.Parameter(0)
    public String tag;

    @Parameter("Статус")
    @Parameterized.Parameter(1)
    public String resolution;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "{0} - {1}")
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(defaultTagToResolutions());
    }

    private static Object[][] defaultTagToResolutions() {
        return new Object[][]{
                {"vin_resolution_invalid", INVALID.name()},
                {"vin_resolution_error", ERROR.name()},
                {"vin_resolution_ok", OK.name()},
                {"vin_resolution_unknown", "UNKNOWN"},
                {"vin_resolution_untrusted", UNTRUSTED.name()},
        };
    }

    @Test
    public void shouldCarsVinHasNoDiffWithProduction() {
        String offerId = prodApi.search().searchCars().reqSpec(defaultSpec())
                .pageQuery(1)
                .searchTagQuery(tag)
                .creationDateToQuery(TIME_DAY_AGO)
                .executeAs(validatedWith(shouldBe200OkJSON()))
                .getOffers().get(RandomUtils.nextInt(0, 9)).getId();

        Function<ApiClient, JsonObject> req = apiClient -> apiClient.carfax().offerRawReport()
                .categoryPath(CARS).offerIDPath(offerId).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeSuccess()))
                .as(JsonObject.class);

        MatcherAssert.assertThat(
                "For offer id = " + offerId + ":",
                req.apply(api),
                jsonEquals(req.apply(prodApi)).whenIgnoringPaths(RawReportUtils.IGNORED_PATHS));
    }
}
