package ru.auto.tests.realtyapi.v1.search;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import org.assertj.core.api.Assertions;
import org.hamcrest.MatcherAssert;
import org.hamcrest.number.OrderingComparison;
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

import static io.restassured.mapper.ObjectMapperType.GSON;

import static net.javacrumbs.jsonunit.JsonMatchers.jsonPartMatches;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.realtyapi.consts.Owners.SCROOGE;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.v1.model.RealtyResponseOfferResponse.OfferTypeEnum.SELL;

@Title("GET /search/offerWithSiteSearch.json")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class FlatAreaTest {

    private JsonArray offers;
    private static final String SQUARE_METER = "SQUARE_METER";
    private static final String FLAT_AREA = "80";
    private static final String APARTMENT = "APARTMENT";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Test
    @Owner(SCROOGE)
    public void shouldApartmentAreaInUpperBound() {
        offers = api.search().offerWithSiteSearchRoute()
                .reqSpec(authSpec())
                .typeQuery(SELL)
                .categoryQuery(APARTMENT)
                .areaMaxQuery(FLAT_AREA)
                .showSimilarQuery("NO")
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON)
                .getAsJsonObject("response")
                .getAsJsonObject("offers")
                .getAsJsonArray("items");

        BigDecimal value = new BigDecimal(FLAT_AREA);
        Assertions.assertThat(offers).describedAs("Все офферы должны иметь площадь <= " + value)
                .allSatisfy(offer -> MatcherAssert.assertThat(offer, jsonPartMatches("area.value",
                        OrderingComparison.lessThanOrEqualTo(value))));
    }

    @Test
    @Owner(SCROOGE)
    public void shouldApartmentAreaInLowerBound() {
        offers = api.search().offerWithSiteSearchRoute()
                .reqSpec(authSpec())
                .typeQuery(SELL)
                .categoryQuery(APARTMENT)
                .areaMinQuery(FLAT_AREA)
                .showSimilarQuery("NO")
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON)
                .getAsJsonObject("response")
                .getAsJsonObject("offers")
                .getAsJsonArray("items");

        BigDecimal value = new BigDecimal(FLAT_AREA);
        Assertions.assertThat(offers).describedAs("Все офферы должны иметь цену => " + value)
                .allSatisfy(offer -> MatcherAssert.assertThat(offer, jsonPartMatches("area.value",
                        greaterThanOrEqualTo(value))));
    }

    @Test
    @Owner(SCROOGE)
    public void shouldAreaUnitIsCorrect() {
        offers = api.search().offerWithSiteSearchRoute()
                .reqSpec(authSpec())
                .typeQuery(SELL)
                .categoryQuery(APARTMENT)
                .areaMaxQuery(FLAT_AREA)
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON)
                .getAsJsonObject("response")
                .getAsJsonObject("offers")
                .getAsJsonArray("items");

        Assertions.assertThat(offers).describedAs("Площадь должна быть в квадратных метрах")
                .allSatisfy(offer -> MatcherAssert.assertThat(offer, jsonPartMatches("area.unit",
                        equalTo(SQUARE_METER))));
    }
}
