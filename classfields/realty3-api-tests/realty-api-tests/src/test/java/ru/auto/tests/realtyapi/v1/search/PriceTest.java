package ru.auto.tests.realtyapi.v1.search;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import org.assertj.core.api.Assertions;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.yandex.qatools.allure.annotations.Title;

import java.math.BigDecimal;
import java.util.Collection;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static java.util.Arrays.asList;
import static net.javacrumbs.jsonunit.JsonMatchers.jsonPartMatches;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.realtyapi.consts.Owners.SCROOGE;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.v1.model.RealtyResponseOfferResponse.OfferTypeEnum.SELL;

@Title("GET /search/offerWithSiteSearch.json")
@RunWith(Parameterized.class)
@GuiceModules(RealtyApiModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class PriceTest {

    private JsonArray offers;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Parameterized.Parameter()
    public String category;

    @Parameterized.Parameter(1)
    public String price;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"APARTMENT", "7000000"},
                {"ROOMS", "1500000"},
                {"HOUSE", "25000000"},
                {"LOT", "5000000"},
                {"COMMERCIAL", "10000000"},
                {"GARAGE", "3000000"}
        });
    }

    @Test
    @Owner(SCROOGE)
    public void shouldPriceInUpperBound() {
        offers = api.search().offerWithSiteSearchRoute()
                .reqSpec(authSpec())
                .typeQuery(SELL)
                .categoryQuery(category)
                .priceMaxQuery(price)
                .showSimilarQuery("NO")
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON)
                .getAsJsonObject("response")
                .getAsJsonObject("offers")
                .getAsJsonArray("items");

        BigDecimal value = new BigDecimal(price);
        Assertions.assertThat(offers).describedAs("Все офферы должны иметь цену <= " + value)
                .allSatisfy(offer -> MatcherAssert.assertThat(offer, jsonPartMatches("price.priceForWhole.value",
                        lessThanOrEqualTo(value))));
    }

    @Test
    @Owner(SCROOGE)
    public void shouldPriceInLowerBound() {
        offers = api.search().offerWithSiteSearchRoute()
                .reqSpec(authSpec())
                .typeQuery(SELL)
                .categoryQuery(category)
                .priceMinQuery(price)
                .showSimilarQuery("NO")
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON)
                .getAsJsonObject("response")
                .getAsJsonObject("offers")
                .getAsJsonArray("items");

        BigDecimal value = new BigDecimal(price);
        Assertions.assertThat(offers).describedAs("Все офферы должны иметь цену => " + value)
                .allSatisfy(offer -> MatcherAssert.assertThat(offer, jsonPartMatches("price.priceForWhole.value",
                        greaterThanOrEqualTo(value))));
    }
}