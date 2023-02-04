package ru.auto.tests.realtyapi.v1.search;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import net.javacrumbs.jsonunit.core.Option;
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

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getQueryMapParams;


@Title("GET /siteWithOffersStat.json")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SiteWithOffersStatCompareTest {

    private static final String RESOURCE_FILE = "search/site_with_offers_stat_test_data.txt";

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
    public Map<String, String> queryParams;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() throws IOException {
        return getQueryMapParams(RESOURCE_FILE);
    }

    @Test
    public void shouldHasNoDiffWithProduction() {
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.search().siteWithOffersStatRoute()
                .reqSpec(authSpec())
                .reqSpec(req -> req.addQueryParams(queryParams))
                .execute(validatedWith(shouldBe200Ok())).as(JsonObject.class, GSON);
        JsonObject apiResponse = request.apply(api).getAsJsonObject("response");
        JsonObject prodApiResponse = request.apply(prodApi).getAsJsonObject("response");
        assertThat("There is no response in api", apiResponse != null);
        assertThat("There is no response in prod api", prodApiResponse != null);
        JsonElement site = apiResponse.get("site");
        JsonElement prodSite = prodApiResponse.get("site");
        assertThat("There is no site in api", site != null);
        assertThat("There is no site in prod api", prodSite != null);

        assertThat(site, jsonEquals(prodSite).whenIgnoringPaths("deliveryDates", "images", "buildingFeatures", "decorationImages", "transactionTerms", "construction", "siteSpecialProposals", "documents").when(Option.IGNORING_ARRAY_ORDER));
        assertThat(apiResponse, jsonEquals(prodApiResponse).whenIgnoringPaths("site").when(Option.IGNORING_ARRAY_ORDER));
    }
}
