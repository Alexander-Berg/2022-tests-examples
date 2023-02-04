package ru.auto.tests.realtyapi.v1.search;


import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
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
import java.util.Collection;
import java.util.Map;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.parseParams;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.validatedWith;


@Title("GET /search/offerWithSiteSearch.json")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class OfferSiteSearchNegativeCaseCompareTest {

    private static final int RGID_A = 587795;
    private static final int RGID_B = 417899;

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
        return parseParams("negativeDiff", "search/offer_with_site_search_test_cases.txt");
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSearchHasDifferentResults() {
        MatcherAssert.assertThat(getRequest(RGID_A), not(jsonEquals(getRequest(RGID_B))
                .whenIgnoringPaths("response.searchQuery.logQueryId",
                        "response.timeStamp",
                        "response.offers.items[*].relevance")));
    }

    private JsonObject getRequest(int rgid) {
        return api.search().offerWithSiteSearchRoute()
                .reqSpec(authSpec())
                .rgidQuery(rgid)
                .reqSpec(req -> req.addQueryParams(query))
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON);
    }
}
