package ru.auto.tests.realtyapi.v1.archive;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.yandex.qatools.allure.annotations.Title;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.hamcrest.Matchers.equalTo;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.ra.ResponseSpecBuilders.shouldBe500WithArchiveError;
import static ru.auto.tests.realtyapi.v1.model.RealtyResponseOfferResponse.OfferCategoryEnum.APARTMENT;
import static ru.auto.tests.realtyapi.v1.model.RealtyResponseOfferResponse.OfferTypeEnum.SELL;


@Title("GET /archive/search.json")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class GetArchiveSearchTest {

    static final String ADDRESS = "Россия, Санкт-Петербург, Невский проспект, 128";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Test
    public void shouldSee403WithoutHeaders() {
        api.archive().archiveSearchRoute()
                .addressQuery(ADDRESS)
                .typeQuery(SELL)
                .categoryQuery(APARTMENT)
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee500WithEmptyAddress() {
        api.archive().archiveSearchRoute()
                .reqSpec(authSpec())
                .addressQuery(EMPTY)
                .typeQuery(SELL)
                .categoryQuery(APARTMENT)
                .execute(validatedWith(shouldBe500WithArchiveError()));
    }

    @Test
    public void shouldSee200WithInvalidAddress() {
        JsonObject resp = api.archive().archiveSearchRoute()
                .reqSpec(authSpec())
                .addressQuery(getRandomString())
                .typeQuery(SELL)
                .categoryQuery(APARTMENT)
                .execute(validatedWith(shouldBe200OkJSON())).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(resp.getAsJsonObject("response").getAsJsonArray("offers").size(),
                equalTo(0));
    }
}
