package ru.auto.tests.realtyapi.v2.village;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Owner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.realtyapi.module.RealtyApiModule;
import ru.auto.tests.realtyapi.v2.ApiClient;
import ru.yandex.qatools.allure.annotations.Title;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.*;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realtyapi.consts.Owners.ARTEAMO;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;

@Title("GET /village/{id}/offers")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyApiModule.class)
public class GetVillageIdOffersTest {
    private static final long VALID_VILLAGE_ID = 1773615;
    private static final String[] INVALID_OFFER_TYPE = new String[]{"VILLAGE"};
    private static final float PRICE_MAX = 1410600;
    private static final float PRICE_MIN = 1410800;
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiClient api;

    @Test
    @Owner(ARTEAMO)
    public void shouldSee403WithNoAuth() {
        api.village().villageOffersRoute()
                .idPath(VALID_VILLAGE_ID)
                .execute(validatedWith(shouldBeCode(SC_FORBIDDEN)));
    }

    @Test
    public void shouldSee404WithInvalidId() {
        api.village().villageOffersRoute()
                .reqSpec(authSpec())
                .idPath(getRandomString())
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }

    @Test
    public void shouldSee404WithNoId() {
        api.village().villageOffersRoute()
                .reqSpec(authSpec())
                .idPath(EMPTY)
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }

    @Test
    public void shouldSee404WithInvalidOfferType() {
        api.village().villageOffersRoute()
                .reqSpec(authSpec())
                .idPath(EMPTY)
                .villageOfferTypeQuery(INVALID_OFFER_TYPE)
                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));
    }

    @Test
    @Owner(ARTEAMO)
    public void shouldSee200OkAWithMinPriceBiggerThanMaxPrice() {
        api.village().villageOffersRoute()
                .reqSpec(authSpec())
                .idPath(VALID_VILLAGE_ID)
                .priceMaxQuery(PRICE_MAX)
                .priceMinQuery(PRICE_MIN)
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON);
    }
}
