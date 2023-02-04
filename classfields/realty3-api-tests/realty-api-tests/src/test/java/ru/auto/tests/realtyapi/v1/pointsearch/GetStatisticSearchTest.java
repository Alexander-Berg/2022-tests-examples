package ru.auto.tests.realtyapi.v1.pointsearch;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import org.assertj.core.api.Assertions;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.yandex.qatools.allure.annotations.Title;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static java.lang.String.format;
import static net.javacrumbs.jsonunit.JsonMatchers.jsonNodeAbsent;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realtyapi.v1.model.RealtyResponseOfferResponse.OfferCategoryEnum.APARTMENT;
import static ru.auto.tests.realtyapi.v1.model.RealtyResponseOfferResponse.OfferTypeEnum.SELL;

@Title("GET /pointStatisticSearch.json")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class GetStatisticSearchTest {

    private static final int OFFERS_SIZE = 150;
    private static final int ZERO_OFFERS = 0;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Test
    @Owner(ARTEAMO)
    public void shouldSee403WithNoAuth() {
        api.pointSearch().pointStatisticSearchRoute()
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee400WithNoParams() {
        api.pointSearch().pointStatisticSearchRoute().reqSpec(authSpec())
                .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldNotSeeOffers() {
        JsonObject response = api.pointSearch().pointStatisticSearchRoute().reqSpec(authSpec())
                .typeQuery(SELL)
                .categoryQuery(APARTMENT)
                .maxCoordinatesQuery(ZERO_OFFERS)
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON)
                .getAsJsonObject("response");

        Assertions.assertThat(response).describedAs("В ответе не должно быть офферов")
                .satisfies(r -> MatcherAssert.assertThat(r, jsonNodeAbsent("points")));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSeeOffersInResponse() {
        JsonObject response = api.pointSearch().pointStatisticSearchRoute().reqSpec(authSpec())
                .typeQuery(SELL)
                .categoryQuery(APARTMENT)
                .maxCoordinatesQuery(OFFERS_SIZE)
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON)
                .getAsJsonObject("response");

        Assertions.assertThat(response.getAsJsonArray("points"))
                .describedAs(format("В ответе должно быть %s офферов", OFFERS_SIZE))
                .hasSize(OFFERS_SIZE);
    }
}
