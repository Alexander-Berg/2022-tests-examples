package ru.auto.tests.realtyapi.v1.search;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import net.javacrumbs.jsonunit.core.Option;
import org.apache.commons.compress.utils.Lists;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.yandex.qatools.allure.annotations.Parameter;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.parseParams;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.validatedWith;


@Title("GET /search/offerWithSiteSearch.json")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class OfferSiteSearchPositiveCaseCompareTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Parameter
    @Parameterized.Parameter
    public Map<String, String> query;


    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() throws IOException {
        return parseParams("positiveDiff", "search/offer_with_site_search_test_cases.txt");
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSearchHasSameResults() {
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.search().offerWithSiteSearchRoute()
                .reqSpec(authSpec())
                .reqSpec(req -> req.addQueryParams(query))
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON);


        JsonObject js1 = request.apply(api);
        JsonObject js2 = request.apply(api);
        Iterator<JsonElement> it2 = js2.getAsJsonObject("response")
                .getAsJsonObject("offers")
                .getAsJsonArray("items").iterator();

        Iterator<JsonElement> it1 = js2.getAsJsonObject("response")
                .getAsJsonObject("offers")
                .getAsJsonArray("items").iterator();
        while (it1.hasNext() && it2.hasNext()) {
            JsonElement elem1 = it1.next();
            JsonElement elem2 = it2.next();
            JsonObject author1 = elem1.getAsJsonObject().getAsJsonObject("author");
            JsonObject author2 = elem2.getAsJsonObject().getAsJsonObject("author");
            MatcherAssert.assertThat(author1, jsonEquals(author2).when(Option.IGNORING_ARRAY_ORDER));
        }

        MatcherAssert.assertThat(js1, jsonEquals(js2)
                .whenIgnoringPaths("response.searchQuery.logQueryId",
                        "response.timeStamp",
                        "response.offers.items[*].relevance",
                        "response.offers.items[*].author.allowedCommunicationChannels[*]"));
    }
}
