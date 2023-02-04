package ru.auto.tests.realty.vos2.compare;


import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Issue;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import lombok.extern.log4j.Log4j;
import org.hamcrest.MatcherAssert;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.restassured.AllureLoggerFilter;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.realty.vos2.ApiClient;
import ru.auto.tests.realty.vos2.adaptor.Vos2ApiAdaptor;
import ru.auto.tests.realty.vos2.anno.CompareOffers;
import ru.auto.tests.realty.vos2.anno.Prod;
import ru.auto.tests.realty.vos2.config.Vos2ApiConfig;
import ru.auto.tests.realty.vos2.module.Vos2ApiCompareModule;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static io.restassured.mapper.ObjectMapperType.GSON;
import static java.lang.String.format;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBeStatusOk;

@DisplayName("Сравнение выдачи с различными типами офферов")
@RunWith(Parameterized.class)
@GuiceModules(Vos2ApiCompareModule.class)
@Issue("VERTISTEST-729")
@Log4j
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
@Ignore
public class CompareOffersTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Parameterized.Parameter
    public String offer;

    @Inject
    private ApiClient vos2;

    @Inject
    private Vos2ApiConfig config;

    @Inject
    @Prod
    private ApiClient prodVos2;

    @Inject
    @CompareOffers
    private Account account;

    @Inject
    private Vos2ApiAdaptor adaptor;

    private String id;

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object> getParameters() {
        return Arrays.asList(
                "offers/apartment_rent.json",
                "offers/apartment_sell.json",
                "offers/commercial_business_center_sell.json",
                "offers/commercial_retail_rent.json",
                "offers/commercial_retail_sell.json",
                "offers/garage_sell.json",
                "offers/house_rent.json",
                "offers/house_sell.json",
                "offers/lot_sell.json",
                "offers/room_rent.json",
                "offers/room_sell.json"
        );
    }

    @Before
    public void createOffer() {
        id = adaptor.createOffer(account.getId(), getResourceAsString(offer)).getId().get(0);
        adaptor.waitActivateOffer(account.getId(), id);
    }

    @Test
    @DisplayName("GET /api/realty/offer/{userID}/{offerID} ")
    public void shouldOfferByIdsHasNotDiffWithProduction() {
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.offer().getOfferRoute()
                .userIDPath(account.getId()).offerIDPath(id)
                .execute(validatedWith(shouldBeStatusOk())).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(vos2), jsonEquals(request.apply(prodVos2)));
    }

    @Test
    @DisplayName("GET /api/realty/offer/moderation/{offerID}")
    public void shouldOfferModerationHasNotDiffWithProduction() {
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.offer().getOfferByIdRoute().offerIDPath(id)
                .execute(validatedWith(shouldBeStatusOk())).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(vos2), jsonEquals(request.apply(prodVos2)));
    }

    @Step("Добавляем продукт для объявления: {offerId}")
    private void addProduct(String offerId, String uid) {
        given().filter(new AllureLoggerFilter()).baseUri(config.getVos2ApiProdURI().toString())
                .basePath("/services/billing/").contentType(JSON)
                .body(format(getResourceAsString("billing.json"), offerId, uid)).post();
    }

    @Test
    @DisplayName("GET /utils/raw-offer/{offerID}")
    public void shouldRawOfferHasNotDiffWithProduction() {
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.utils().utilsRawOffer().offerIDPath(id)
                .execute(validatedWith(shouldBe200OkJSON())).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(vos2), jsonEquals(request.apply(prodVos2)));
    }

    @Test
    @DisplayName("GET /api/realty/user_offers/by_ids/{userId}")
    public void shouldUserOffersByIdsHasNotDiffWithProduction() {
        Function<ApiClient, JsonObject> request = apiClient -> apiClient.userOffers().userOffersByIdsRoute()
                .userIDPath(account.getId()).offerIDsQuery(id)
                .execute(validatedWith(shouldBeStatusOk())).as(JsonObject.class, GSON);

        MatcherAssert.assertThat(request.apply(vos2), jsonEquals(request.apply(prodVos2)));
    }

    @After
    public void deleteOffers() {
        try {
            adaptor.deleteOffer(account.getId(), id);
        } catch (Exception e) {
            log.info(format("Can't delete offer %s for uid %s. Exception: %s", id, account.getId(), e.getMessage()));
        }
    }
}
