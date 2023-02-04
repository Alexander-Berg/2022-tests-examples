package ru.auto.tests.realty.vos2.services;

import com.carlosbecker.guice.GuiceModules;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import org.apache.commons.lang3.StringUtils;
import org.assertj.Assertions;
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
import ru.auto.tests.realty.vos2.objects.GetOfferResp;

import static io.restassured.http.ContentType.JSON;
import static io.restassured.mapper.ObjectMapperType.GSON;
import static java.lang.String.format;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realty.vos2.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.realty.vos2.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realty.vos2.enums.ErrorStatusCode.UNKNOWN_OFFER;
import static ru.auto.tests.realty.vos2.ra.RequestSpecBuilders.jsonBody;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe400WithNoContentToMap;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBe500WithInternalNumberFormatException;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBeStatusOk;
import static ru.auto.tests.realty.vos2.utils.UtilsVos2Api.getRandomLogin;

@DisplayName("PUT /services/patch-offer/{offerID}")
@RunWith(GuiceDataProviderRunner.class)
@GuiceModules(Vos2ApiTestModule.class)
public class PartiallyUpdateOfferTest {

//    private static final String PARTIALLY_UPDATE_OFFER_REQUEST_PATH = "testdata/offer_update_request_body.json";
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
//    @Test
//    public void shouldSuccessPartiallyUpdateOffer() {
//        String address = "Россия, Приморский край, Владивосток, улица Державина, 21";
//        Long rgid = 585430L;
//        JsonObject body = new JsonObject();
//        body.add("location", getBodyRequest().getAsJsonObject("location"));
//        body.getAsJsonObject("location").addProperty("address", address);
//        body.getAsJsonObject("location").addProperty("rgid", rgid);
//
//        id = adaptor.createOffer(account.getId()).getId().get(0);
//
//        JsonObject beforeResp = vos2.offer().getOfferRoute().userIDPath(account.getId()).offerIDPath(id)
//                .execute(validatedWith(shouldBeStatusOk())).as(JsonObject.class, GSON);
//
//        vos2.services().patchOfferRoute().offerIDPath(id).reqSpec(jsonBody(body))
//                .execute(validatedWith(shouldBeStatusOk()));
//
//        GetOfferResp resp = adaptor.getUserOfferById(account.getId(), id);
//        Assertions.assertThat(resp.getSpecific()).hasAddress(address).hasRgid(rgid);
//
//        JsonObject afterResp = vos2.offer().getOfferRoute().userIDPath(account.getId()).offerIDPath(id)
//                .execute(validatedWith(shouldBeStatusOk())).as(JsonObject.class, GSON);
//
//        MatcherAssert.assertThat(beforeResp, jsonEquals(afterResp)
//                .whenIgnoringPaths("specific.address", "specific.rgid", "specific.unifiedLocation", "offer.redirectPhonesState"));
//    }
//
//    @Test
//    public void shouldSee400WithoutBody() {
//        id = adaptor.createOffer(account.getId()).getId().get(0);
//        vos2.services().patchOfferRoute().offerIDPath(id).reqSpec(jsonBody(StringUtils.EMPTY))
//                .execute(validatedWith(shouldBe400WithNoContentToMap()));
//    }
//
//    @Test
//    public void shouldSuccessPartiallyUpdateOfferWithEmptyBody() {
//        String emptyBody = "{}";
//        id = adaptor.createOffer(account.getId()).getId().get(0);
//
//        JsonObject beforeResp = vos2.offer().getOfferRoute().userIDPath(account.getId()).offerIDPath(id)
//                .execute(validatedWith(shouldBeStatusOk())).as(JsonObject.class, GSON);
//        vos2.services().patchOfferRoute().offerIDPath(id).reqSpec(jsonBody(emptyBody))
//                .execute(validatedWith(shouldBeStatusOk()));
//        JsonObject afterResp = vos2.offer().getOfferRoute().userIDPath(account.getId()).offerIDPath(id)
//                .execute(validatedWith(shouldBeStatusOk())).as(JsonObject.class, GSON);
//
//        MatcherAssert.assertThat(beforeResp, jsonEquals(afterResp));
//
//    }
//
//    @Test
//    public void shouldSee404ForNotExistOffer() {
//        String emptyBody = "{}";
//        String randomOfferId = getRandomLogin();
//        vos2.services().patchOfferRoute().offerIDPath(randomOfferId).reqSpec(jsonBody(emptyBody))
//                .execute(validatedWith(shouldBeCode(SC_NOT_FOUND).expectContentType(JSON)
//                        .expectBody("status", equalTo("ERROR"))
//                        .expectBody("errors.size", greaterThanOrEqualTo(1))
//                        .expectBody("errors[0].code", equalTo(UNKNOWN_OFFER.value()))
//                        .expectBody("errors[0].description", equalTo(format("Offer %s not found", randomOfferId)))));
//    }
//
//    @Test
//    public void shouldSee500ForInvalidOffer() {
//        String emptyBody = "{}";
//        String randomOfferId = getRandomString();
//        vos2.services().patchOfferRoute().offerIDPath(randomOfferId).reqSpec(jsonBody(emptyBody))
//                .execute(validatedWith(shouldBe500WithInternalNumberFormatException(randomOfferId)));
//    }
//
//    private JsonObject getBodyRequest() {
//        return Vos2ApiAdaptor.getObjectFromJson(JsonObject.class, PARTIALLY_UPDATE_OFFER_REQUEST_PATH);
//    }
}
