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
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static net.javacrumbs.jsonunit.JsonMatchers.jsonPartMatches;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.parseParams;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.validatedWith;


@Title("GET /search/offerWithSiteSearch.json")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class OfferSiteSearchPriceUnitTest {

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
        return parseParams("priceUnit", "search/offer_with_site_search_test_cases.txt");
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
    public void shouldPriceInUpperBound() {
        BigDecimal priceMax = new BigDecimal(query.get("priceMax"));

        Assertions.assertThat(offers).describedAs("Все офферы должны иметь цену <= " + priceMax)
                .allSatisfy(offer -> MatcherAssert.assertThat(offer, jsonPartMatches("price.value",
                        lessThanOrEqualTo(priceMax))));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldPriceInLowerBound() {
        BigDecimal priceMin = new BigDecimal(query.get("priceMin"));

        Assertions.assertThat(offers).describedAs("Все офферы должны иметь цену >= " + priceMin)
                .allSatisfy(offer -> MatcherAssert.assertThat(offer, jsonPartMatches("price.value",
                        greaterThanOrEqualTo(priceMin))));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldPeriodIsCorrect() {
        String perMonth = "PER_MONTH";

        Assertions.assertThat(offers).describedAs("Все офферы должны иметь период оплаты " + perMonth)
                .allSatisfy(offer -> MatcherAssert.assertThat(offer, jsonPartMatches("price.period",
                        equalTo(perMonth))));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldUnitIsCorrect() {
        String squareMeter = "SQUARE_METER";


        Assertions.assertThat(offers).describedAs("Все офферы должны иметь цену за квадратный метр")
                .allSatisfy(offer -> MatcherAssert.assertThat(offer, jsonPartMatches("price.unit",
                        equalTo(squareMeter))));
    }
}