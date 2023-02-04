package ru.auto.tests.realty.vos2.adaptor;

import com.google.common.collect.Lists;
import com.google.gson.GsonBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import io.qameta.allure.Step;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.auto.tests.realty.vos2.ApiClient;
import ru.auto.tests.realty.vos2.anno.Prod;
import ru.auto.tests.realty.vos2.config.Vos2ApiConfig;
import ru.auto.tests.realty.vos2.model.CreateUserRequest;
import ru.auto.tests.realty.vos2.model.RequestInfo;
import ru.auto.tests.realty.vos2.model.WrappedRequest;
import ru.auto.tests.realty.vos2.objects.*;

import java.util.List;
import java.util.function.Function;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static io.restassured.http.ContentType.JSON;
import static io.restassured.mapper.ObjectMapperType.GSON;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.httpclient.HttpStatus.SC_OK;
import static org.awaitility.Awaitility.given;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.auto.tests.realty.vos2.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.realty.vos2.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.realty.vos2.enums.VosOfferStatus.ACTIVE;
import static ru.auto.tests.realty.vos2.enums.VosOfferStatus.TRUSTED;
import static ru.auto.tests.realty.vos2.matchers.OfferInfoMatchers.hasStatus;
import static ru.auto.tests.realty.vos2.matchers.OfferMatcher.offerShould;
import static ru.auto.tests.realty.vos2.ra.RequestSpecBuilders.jsonBody;
import static ru.auto.tests.realty.vos2.ra.ResponseSpecBuilders.shouldBeStatusOk;
import static ru.auto.tests.realty.vos2.utils.UtilsVos2Api.apiAwait;

/**
 * Created by vicdev on 28.02.17.
 */
public class Vos2ApiAdaptor extends AbstractModule {

    @Inject
    public Vos2ApiConfig config;

    @Inject
    @Prod
    private ApiClient vos2;

    @Step("Создаем драфт для пользователя {uid}")
    public CreateDraftResp createDraft(String uid, String body) {
        return vos2.draft().batchCreateDraftsRoute().userIDPath(uid).reqSpec(jsonBody(body))
                .execute(validatedWith(shouldBeStatusOk())).as(CreateDraftResp.class, GSON);
    }

    @Step("Создаем драфт для пользователя {uid} по умолчанию")
    public CreateDraftResp createDraft(String uid) {
        return createDraft(uid, getResourceAsString("testdata/draft_create_request_body.json"));
    }

    @Step("Создаем пользователя {uid} в VOS")
    public String createUser(String uid) {
        // Now user ID in VOS is his ID in Passport (uid in VOS == ID in Passport)
        CreateUserRequest user = getObjectFromJson(CreateUserRequest.class, "testdata/user_request_body.json");
        vos2.user().createRoute().body(user.login(uid).request(random(RequestInfo.class))).execute(validatedWith(shouldBeStatusOk()));
        return uid;
    }

    @Step("Получаем массив объявлений у пользователя {uid}")
    public UserOffersResp waitUserOffers(String uid) {
        waitOffers(uid);
        return getUserOffers(uid);
    }

    @Step("Получаем массив объявлений у пользователя {uid}")
    public UserOffersResp getUserOffers(String uid) {
        return vos2.userOffers().userOffersRoute().userIDPath(uid)
                .execute(validatedWith(shouldBeStatusOk())).as(UserOffersResp.class, GSON);
    }

    @Step("Получаем объявление с id {offerId} у пользователя {uid}")
    public GetOfferResp getUserOfferById(String uid, String offerId) {
        return vos2.offer().getOfferRoute().userIDPath(uid).offerIDPath(offerId)
                .execute(validatedWith(shouldBeStatusOk())).as(GetOfferResp.class, GSON);
    }

    @Step("Создаем два оффера для пользователя {uid}")
    public List<String> createTwoOffers(String uid) {
        List<String> offerIds = Lists.newArrayList();
        offerIds.add(createOffer(uid).getId().get(0));
        offerIds.add(createOffer(uid).getId().get(0));
        return offerIds;
    }

    @Step("Удаляем объявление с id {offerId} у пользователя {uid}")
    public void deleteOffer(String uid, String offerId) {
        vos2.offer().deleteRoute().userIDPath(uid).offerIDPath(offerId)
                .body(getRequest(WrappedRequest.class))
                .execute(validatedWith(shouldBe200OkJSON()));
    }

    @Step("Ждем объявления у пользователя с uid {uid}")
    private void waitOffers(String uid) {
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .pollInterval(3, SECONDS).atMost(30, SECONDS).until(
                () -> vos2.userOffers().userOffersRoute().userIDPath(uid)
                        .execute(validatedWith(shouldBeStatusOk()))
                        .as(UserOffersResp.class, GSON).getPager().getTotalItems(), greaterThanOrEqualTo((long) 1));
    }

    @Step("Создаем оффер для пользователя {uid}")
    public CreateOfferResp createOffer(String uid, String body) {
        return vos2.offer().batchCreateRoute().userIDPath(uid)
                .reqSpec(r -> r.setContentType(JSON)
                        .setBody(body))
                .execute(validatedWith(shouldBeStatusOk())).as(CreateOfferResp.class, GSON);
    }

    @Step("Создаем оффер для пользователя {uid} по умолчанию")
    public CreateOfferResp createOffer(String uid) {
        CreateOfferResp offer = createOffer(uid, getResourceAsString("testdata/offer_create_request_body.json"));
        waitActivateOffer(uid, offer.getId().get(0));
        return offer;
    }

//    public void waitActivateOffer(String uid, String offerId) {
//        assertThat("Оффер не был активирован", vos2,
//                withWaitFor(offerShould(or(hasStatus(ACTIVE.toString()),
//                        hasStatus(TRUSTED.toString()))).setUserId(uid).setOfferId(offerId),
//                        SECONDS.toMillis(60), SECONDS.toMillis(2)));
//    }

    @Step("Ждём активации офера «{offerId}»")
    public void waitActivateOffer(String uid, String offerId) {
        apiAwait().atMost(60, SECONDS)
                .untilAsserted(() -> offerShould(anyOf(hasStatus(ACTIVE.toString()),
                        hasStatus(TRUSTED.toString()))).setUserId(uid).setOfferId(offerId));
    }

    @Step("Проверяем, есть ли пользователь ({uid}) в VOS")
    public boolean isVosUser(String uid) {
        return vos2.user().getUserRoute().userIDPath(uid).execute(Function.identity()).statusCode() == SC_OK;
    }


    @Step("Отключаем оповещения пользователя {uid}")
    public void unsubscribeUserFromNotifications(String uid) {
        vos2.userSubscriptions().subscriptionsRoute().userIDPath(uid).allQuery(false)
                .execute(validatedWith(shouldBeCode(SC_OK)));
    }

    @Step("Получаем оповещения пользователя {uid}")
    public GetUserNotificationsResp getUserNotifications(String uid) {
        return vos2.userSubscriptions().getRoute().userIDPath(uid).execute(validatedWith(shouldBeCode(SC_OK)))
                .as(GetUserNotificationsResp.class, GSON);
    }

    public static <T> T getRequest(Class<T> tClass) {
        return getObjectFromJson(tClass, "request.json");
    }

    public static <T> T getObjectFromJson(Class<T> tClass, String path) {
        return new GsonBuilder().create().fromJson(getResourceAsString(path), tClass);
    }

    @Override
    protected void configure() {
    }
}
