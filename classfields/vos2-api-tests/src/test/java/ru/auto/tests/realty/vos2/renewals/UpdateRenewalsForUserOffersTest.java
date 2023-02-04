package ru.auto.tests.realty.vos2.renewals;


import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.qameta.allure.junit4.DisplayName;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.commons.runners.GuiceDataProviderRunner;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.realty.vos2.ApiClient;
import ru.auto.tests.realty.vos2.adaptor.Vos2ApiAdaptor;
import ru.auto.tests.realty.vos2.anno.Vos;
import ru.auto.tests.realty.vos2.module.Vos2ApiTestModule;

import java.time.Instant;

import static io.restassured.mapper.ObjectMapperType.GSON;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static ru.auto.tests.commons.util.Utils.getRandomShortInt;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realty.vos2.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.realty.vos2.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realty.vos2.ra.RequestSpecBuilders.jsonBody;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe400WithNoContentToMap;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe400WithNotValidJSON;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe404WithRequestedHandlerNotBeFound;
import static ru.auto.tests.realty.vos2.utils.UtilsVos2Api.getObjectFromJson;
import static ru.auto.tests.realty.vos2.utils.UtilsVos2Api.getRandomLogin;
import static ru.auto.tests.realty.vos2.testdata.TestData.defaultProductTypes;

@DisplayName("PUT /api/realty/renewals/{userID}")
@RunWith(GuiceDataProviderRunner.class)
@GuiceModules(Vos2ApiTestModule.class)
public class UpdateRenewalsForUserOffersTest {

//    private static final String RENEWALS_UPDATE_REQUEST_PATH = "testdata/renewals_update_request_body.json";
//
//    @Rule
//    @Inject
//    public RuleChain defaultRules;
//
//    @Inject
//    private ApiClient vos2;
//
//    @Inject
//    @Vos
//    private Account account;
//
//    @Inject
//    private Vos2ApiAdaptor adaptor;
//
//    private String id;
//
//    @DataProvider
//    public static Object[] productTypes() {
//        return defaultProductTypes();
//    }
//
//    @Test
//    @UseDataProvider("productTypes")
//    public void shouldSuccessUpdateRenewals(String productType) {
//        int cost = getRandomShortInt();
//        int duration = getRandomShortInt();
//        id = adaptor.createOffer(account.getId()).getId().get(0);
//
//        vos2.renewals().updatingRoute().userIDPath(account.getId())
//                .reqSpec(jsonBody(getBodyRequest(productType, cost, duration)))
//                .execute(validatedWith(shouldBeCode(SC_OK)));
//
//        JsonObject resp = vos2.renewals().getRoute().userIDPath(account.getId()).showStatusQuery("ALL")
//                .execute(validatedWith(shouldBeCode(SC_OK))).as(JsonObject.class, GSON);
//
//        assertThat(resp.entrySet().size(), greaterThanOrEqualTo(1));
//        JsonObject product = resp.getAsJsonArray(id).get(0).getAsJsonObject().getAsJsonObject("rule")
//                .getAsJsonObject("product");
//        assertThat(product.get("kind").getAsString(), equalTo(productType));
//        assertThat(product.get("cost").getAsInt(), equalTo(cost));
//        assertThat(product.get("duration").getAsInt(), equalTo(duration));
//    }
//
//    @Test
//    public void shouldSee404WithoutBody() {
//        vos2.renewals().updatingRoute().userIDPath(account.getId()).reqSpec(jsonBody(StringUtils.EMPTY))
//                .execute(validatedWith(shouldBe400WithNoContentToMap()));
//    }
//
//    @Test
//    public void shouldSee404ForEmptyBody() {
//        vos2.renewals().updatingRoute().userIDPath(account.getId()).reqSpec(jsonBody(new JsonObject()))
//                .execute(validatedWith(shouldBe400WithNotValidJSON()));
//    }
//
//    @Test
//    public void shouldSee404ForInvalidUser() {
//        vos2.renewals().updatingRoute().userIDPath(getRandomString()).reqSpec(jsonBody(new JsonObject()))
//                .execute(validatedWith(shouldBe404WithRequestedHandlerNotBeFound()));
//    }
//
//    @Test
//    public void shouldSee404ForNotExistUser() {
//        id = getRandomLogin();
//        JsonObject resp = vos2.renewals().updatingRoute().userIDPath(getRandomLogin())
//                .reqSpec(jsonBody(getBodyRequest("PREMIUM", getRandomShortInt(), getRandomShortInt())))
//                .execute(validatedWith(shouldBeCode(SC_OK))).as(JsonObject.class, GSON);
//
//        MatcherAssert.assertThat(resp, jsonEquals(new JsonObject()));
//    }
//
//    @Test
//    public void shouldSee404ForNotExistOffer() {
//        id = getRandomLogin();
//        JsonObject resp = vos2.renewals().updatingRoute().userIDPath(account.getId())
//                .reqSpec(jsonBody(getBodyRequest("PREMIUM", getRandomShortInt(), getRandomShortInt())))
//                .execute(validatedWith(shouldBeCode(SC_OK))).as(JsonObject.class, GSON);
//
//        MatcherAssert.assertThat(resp, jsonEquals(new JsonObject()));
//    }
//
//    private JsonObject getBodyRequest(String productType, int cost, int duration) {
//        JsonObject reqBody = getObjectFromJson(JsonObject.class, RENEWALS_UPDATE_REQUEST_PATH);
//        JsonArray reqArray = reqBody.getAsJsonObject("renewals").getAsJsonArray("offerId");
//        JsonObject product = reqArray.get(0).getAsJsonObject().getAsJsonObject("product");
//        product.addProperty("kind", productType);
//        product.addProperty("cost", cost);
//        product.addProperty("duration", duration);
//        reqArray.get(0).getAsJsonObject().addProperty("activation", Instant.now().toString());
//
//        reqBody.getAsJsonObject("renewals").remove("offerId");
//        reqBody.getAsJsonObject("renewals").add(id, reqArray);
//
//        return reqBody;
//    }
}
