package ru.auto.tests.realtyapi.adaptor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.restassured.path.json.JsonPath;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.auto.tests.commons.restassured.AllureLoggerFilter;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.realtyapi.anno.Prod;
import ru.auto.tests.realtyapi.bean.Promocode;
import ru.auto.tests.realtyapi.bean.promocode.Constraints;
import ru.auto.tests.realtyapi.bean.promocode.Feature;
import ru.auto.tests.realtyapi.responses.DraftSuccessResponse;
import ru.auto.tests.realtyapi.responses.FavoritesResponse;
import ru.auto.tests.realtyapi.responses.GetAllSubscriptions;
import ru.auto.tests.realtyapi.responses.SubscriptionResponse;
import ru.auto.tests.realtyapi.responses.UserOffersResp;
import ru.auto.tests.realtyapi.responses.UserResponse;
import ru.auto.tests.realtyapi.v1.ApiClient;
import ru.auto.tests.realtyapi.v1.ResponseSpecBuilders;
import ru.auto.tests.realtyapi.v1.model.ExamplePhoneModel;
import ru.auto.tests.realtyapi.v1.model.ExampleUserInfoModel;
import ru.auto.tests.realtyapi.v1.model.ExampleUserModel;
import ru.auto.tests.realtyapi.v1.model.FavoritesPatchRequest;
import ru.auto.tests.realtyapi.v1.model.RealtyResponseOfferResponse.OfferCategoryEnum;
import ru.auto.tests.realtyapi.v1.model.RealtyResponseOfferResponse.OfferTypeEnum;
import ru.auto.tests.realtyapi.v1.model.VosUserModel;

import java.util.Collections;
import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;
import static io.restassured.http.ContentType.JSON;
import static io.restassured.mapper.ObjectMapperType.GSON;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.function.Function.identity;
import static org.apache.http.HttpStatus.SC_OK;
import static org.awaitility.Awaitility.given;
import static org.awaitility.Duration.ZERO;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.commons.util.Utils.getRandomEmail;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.ra.ResponseSpecBuilders.shouldBeOK;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.apiAwait;
import static ru.auto.tests.realtyapi.utils.UtilsRealtyApi.getValidSubscriptionRequest;
import static ru.auto.tests.realtyapi.v1.model.ExampleUserInfoModel.TypeEnum.OWNER;
import static ru.auto.tests.realtyapi.v1.model.VosUserModel.PaymentTypeEnum.JURIDICAL_PERSON;

/**
 * Created by vicdev on 28.02.17.
 */
@Singleton
public class RealtyApiAdaptor {

    private static final String SEARCHER = "http://realty-searcher-api.vrts-slb.test.vertis.yandex.net:80";
    private static final String PROMOCODER = "http://promocoder-api-http-api.vrts-slb.test.vertis.yandex.net:80/api/1.x";
    private static final int POLL_INTERVAL = 3;
    private static final int TIMEOUT = 60;
    private static final String VALID_OGRN = "1127746509780";

    @Inject
    @Prod
    private ApiClient api;

    @Inject
    private FtlProcessor ftlProcessor;

    @Step("Получаем пользователя")
    public UserResponse getUser(String token) {
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger()).await().pollDelay(ZERO)
                .pollInterval(POLL_INTERVAL, SECONDS).atMost(TIMEOUT, SECONDS).ignoreExceptions()
                .until(() -> api.user().getUserRoute().reqSpec(authSpec())
                        .authorizationHeader(token).execute(Function.identity()).statusCode(), equalTo(HttpStatus.SC_OK));

        return api.user().getUserRoute().reqSpec(authSpec())
                .authorizationHeader(token).execute(validatedWith(shouldBe200Ok())).as(UserResponse.class, GSON);
    }

    @Step("Создаем пользователя в VOS")
    public UserResponse vosUser(String token) {
        UserResponse userResp = getUser(token);
        String phoneId = userResp.getResponse().getUser().getPhones().get(0).getId();
        String phone = userResp.getResponse().getUser().getPhones().get(0).getPhone();
        return api.user().patchUserRoute().reqSpec(authSpec()).body(new ExampleUserModel().user(
                new ExampleUserInfoModel().type(OWNER)
                        .name(Utils.getRandomString()).email(getRandomEmail())
                        .phones(newArrayList(new ExamplePhoneModel()
                                .phone(phone).select(true).id(phoneId))))
                .locale("ru_RU"))
                .authorizationHeader(token)
                .execute(validatedWith(shouldBe200Ok()))
                .as(UserResponse.class, GSON);
    }

    @Step("Создаем юридическое лицо в VOS")
    public void juridicalUser(Account account, String token) {
        String uid = account.getId();

        api.user().updateUserRoute().reqSpec(authSpec()).uidPath(uid)
                .body(new VosUserModel()
                        .login(account.getId())
                        .name("Сергей Иванов")
                        .type(1)
                        .redirectPhones(false)
                        .email(getRandomEmail())
                        .ogrn(VALID_OGRN)
                        .telephones(Collections.singletonList(getFullPhoneNumber(uid)))
                        .paymentType(JURIDICAL_PERSON))
                .authorizationHeader(token)
                .execute(validatedWith(shouldBe200Ok()));
    }

    @Step("Создаем пустой черновик")
    public DraftSuccessResponse createEmptyDraft(String token) {
        vosUser(token);
        return api.draft().createDraftRoute().reqSpec(authSpec()).authorizationHeader(token).execute(validatedWith(shouldBe200Ok())).as(DraftSuccessResponse.class, GSON);
    }

    @Step("Создаем объявление по умолчанию")
    public DraftSuccessResponse createOffer(String token) {
        return createOffer(token, "offers/apartment_sell.ftl");
    }


    @Step("Получаем оффер из серчера")
    public String getOfferIdFromSearcher() {
        JsonPath jsonPath = RestAssured.given().filter(new AllureLoggerFilter())
                .get(format("%s/offerSearchV2.json?rgid=741964&type=SELL&category=APARTMENT", SEARCHER))
                .getBody()
                .jsonPath();

        try {
            return jsonPath.getString("data[0].offers[0].offerId");
        } catch (Exception e) {
            throw new RuntimeException("Не удалось получить id оффера:", e);
        }
    }

    @Step("Создаем объявление")
    public DraftSuccessResponse createOffer(String token, String path) {
        String offerId = createEmptyDraft(token).getResponse().getId();

        return api.draft().updateDraftRoute().reqSpec(authSpec()).offerIdPath(offerId)
                .authorizationHeader(token)
                .reqSpec(req -> req.setBody(ftlProcessor.processOffer(path)))
                .publishQuery(true)
                .execute(validatedWith(shouldBeOK())).as(DraftSuccessResponse.class, GSON);
    }

    @Step("Получаем объявления у пользователя")
    public UserOffersResp getUserOffers(String token) {
        return api.userOffers().getUserOffersRoute()
                .reqSpec(authSpec())
                .authorizationHeader(token)
                .execute(Function.identity())
                .as(UserOffersResp.class, GSON);
    }

    @Step("Добавляем объявление в избранное {offerIds}")
    public FavoritesResponse addFavorites(String token, String... offerIds) {
        return api.favorites().patchFavoritesRoute().reqSpec(authSpec()).authorizationHeader(token)
                .body(new FavoritesPatchRequest()
                        .add(newArrayList(offerIds))
                        .remove(newArrayList()))
                .execute(validatedWith(shouldBe200Ok())).as(FavoritesResponse.class, GSON);
    }

    @Step("Добавляем объявление в избранное {offerIds}, с типом {type} и категорией {category}")
    public FavoritesResponse addFavorites(String token, OfferTypeEnum type, OfferCategoryEnum category, String... offerIds) {
        return api.favorites().patchFavoritesRoute().reqSpec(authSpec()).authorizationHeader(token)
                .categoryQuery(category.name()).typeQuery(type.name())
                .body(new FavoritesPatchRequest()
                        .add(newArrayList(offerIds))
                        .remove(newArrayList()))
                .execute(validatedWith(shouldBe200Ok())).as(FavoritesResponse.class, GSON);
    }

    @Step("Получаем все подписки приложения")
    public GetAllSubscriptions getApplicationSubscriptions() {
        // All subscriptions depend on application uuid
        return api.subscriptions().getUserSubscriptionsRoute().reqSpec(authSpec())
                .execute(validatedWith(shouldBeCode(SC_OK))).as(GetAllSubscriptions.class, GSON);
    }

    @Step("Удаляем подписку {subscriptionId}")
    public void deleteSubscription(String subscriptionId) {
        if (isSubscriptionExist(subscriptionId)) {
            api.subscriptions().deleteSubscriptionRoute().idPath(subscriptionId).reqSpec(authSpec())
                    .execute(validatedWith(shouldBeCode(SC_OK)));
        }
    }

    @Step("Проверяем, существует ли подписка {subscriptionId}")
    public boolean isSubscriptionExist(String subscriptionId) {
        return api.subscriptions().getSubscriptionRoute().idPath(subscriptionId).reqSpec(authSpec())
                .execute(identity()).statusCode() == SC_OK;
    }

    @Step("Получаем подписку {subscriptionId}")
    public SubscriptionResponse getSubscriptionById(String subscriptionId) {
        return api.subscriptions().getSubscriptionRoute().reqSpec(authSpec()).idPath(subscriptionId)
                .execute(validatedWith(shouldBe200OkJSON())).as(SubscriptionResponse.class, GSON);
    }

    @Step("Создаем подписку c заданным телом")
    public SubscriptionResponse createSubscriptionWithRandomBody() {
        return api.subscriptions().createSubscriptionRoute().reqSpec(authSpec())
                .body(getValidSubscriptionRequest())
                .execute(validatedWith(shouldBe200OkJSON())).as(SubscriptionResponse.class, GSON);
    }

    @Step("Получаем полный номер телефона")
    public String getFullPhoneNumber(String uid) {
        return api.user().getUserRoute().reqSpec(authSpec())
                .xUidHeader(uid)
                .execute(validatedWith(shouldBe200Ok()))
                .as(JsonObject.class)
                .getAsJsonObject("response")
                .getAsJsonObject("user")
                .getAsJsonArray("phones")
                .get(0).getAsJsonObject()
                .get("fullPhone").getAsString();
    }

    @Step("Создаем обычный промокод")
    public Promocode createPromocode(String tag, int count, int totalAct, int userAct) {
        Promocode promocode = new Promocode()
                .setOneFeature(new Feature(tag, count))
                .setConstraints(new Constraints(totalAct, userAct));
        return postPromocode(promocode);
    }

    @Step("Создаем просроченный промокод")
    public Promocode createOutdatedPromocode(String tag, int count, int activations) {
        Promocode promocode = new Promocode()
                .setOneFeature(new Feature(tag, count))
                .setConstraints(new Constraints(activations, activations).setOutdatedDeadline());
        return postPromocode(promocode);
    }

    @Step("Создаем промокод с заблокированным пользователем")
    public Promocode createPromocodeWithBlacklist(String tag, int count, int activations, String uid) {
        Promocode promocode = new Promocode()
                .setOneFeature(new Feature(tag, count))
                .setConstraints(new Constraints(activations, activations).addOneUserToBlacklist(uid));
        return postPromocode(promocode);
    }

    @Step("Публикуем промокод")
    public Promocode postPromocode(Promocode promocode) {
        apiAwait().atMost(15, SECONDS)
                .until(() -> RestAssured.given().body(promocode).filter(new AllureLoggerFilter())
                        .contentType(JSON)
                        .header(new Header("X-Promocoder-User", "promocoder"))
                        .post(format("%s/service/realty/promocode", PROMOCODER))
                        .getStatusCode(), is(SC_OK));
        return promocode;
    }

    @Step("Получаем промокоды пользователя")
    public JsonArray getUsersPromocode(String uid) {
        return RestAssured.given().filter(new AllureLoggerFilter())
                .contentType(JSON)
                .get(format("%s/service/realty/feature/user/%s", PROMOCODER, uid))
                .getBody().as(JsonArray.class, GSON);
    }

    @Step("Получаем информацию об оффере")
    public JsonObject getOfferCard(String offerId) {
       return api.search().cardWithViewsRoute().reqSpec(authSpec())
                .idQuery(offerId)
                .execute(ResponseSpecBuilders.validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON)
                .getAsJsonObject("response");
    }

    @Step("Получить оффер")
    public JsonArray getOffer(String offerId) {
        return api.search().offerWithSiteSearchRoute()
                .reqSpec(authSpec())
                .offerIdQuery(offerId)
                .execute(ResponseSpecBuilders.validatedWith(shouldBe200Ok()))
                .as(JsonObject.class, GSON)
                .getAsJsonObject("response")
                .getAsJsonObject("offers")
                .getAsJsonArray("items");
    }

    @Step("Ожидаем пока offer прорастёт в поиске")
    public void waitOfferIsInSearcher(String offerId) {
        try {
            apiAwait().atMost(2, MINUTES)
                    .until(() -> getOffer(offerId), Matchers.is(not(emptyIterable())));
        } catch (Exception e) {
            throw new AssertionError("Оффер не появился в списке за две минуты");
        }
    }

}
