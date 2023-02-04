package ru.auto.tests.realtyapi.v1.search;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import org.assertj.core.api.Assertions;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
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
import static net.javacrumbs.jsonunit.JsonMatchers.jsonPartMatches;
import static org.hamcrest.Matchers.equalTo;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.parseParams;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.validatedWith;


@Title("GET /search/offerWithSiteSearch.json")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class OfferSiteSearchGarageTest {

    private JsonArray offers;

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
        return parseParams("garage", "search/offer_with_site_search_test_cases.txt");
    }

    @Before
    public void getResponse() {
        offers = api.search().offerWithSiteSearchRoute()
                .reqSpec(authSpec())
                .reqSpec(req -> req.addQueryParams(query))
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON)
                .getAsJsonObject("response")
                .getAsJsonObject("offers")
                .getAsJsonArray("items");
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldCategorySameWithRequested() {
        String category = query.get("category");

        Assertions.assertThat(offers).describedAs("Все офферы должны иметь категорию = " + category)
                .allSatisfy(offer -> MatcherAssert.assertThat(offer, jsonPartMatches("offerCategory",
                        equalTo(category))));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldGarageTypeSameWithRequested() {
        String category = query.get("garageType");

        Assertions.assertThat(offers).describedAs("Все офферы должны иметь категорию " + category)
                .allSatisfy(offer -> MatcherAssert.assertThat(offer, jsonPartMatches("garage.garageType",
                        equalTo(category))));
    }
}
