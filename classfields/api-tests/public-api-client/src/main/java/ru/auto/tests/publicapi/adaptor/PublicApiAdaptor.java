package ru.auto.tests.publicapi.adaptor;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.http.ContentType;
import org.apache.commons.codec.digest.DigestUtils;
import org.awaitility.Duration;
import retrofit2.Response;
import ru.auto.test.passport.api.ModerationApi;
import ru.auto.test.passport.api.UserApi;
import ru.auto.test.passport.model.UserAuthTokenCreateParameters;
import ru.auto.test.salesman.user.model.AutoSalesmanUserCreateTransactionResult;
import ru.auto.tests.commons.awaitility.AllureConditionEvaluationLogger;
import ru.auto.tests.commons.restassured.AllureLoggerFilter;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.JSON;
import ru.auto.tests.publicapi.ResponseSpecBuilders;
import ru.auto.tests.publicapi.adaptor.offer.OfferTemplate;
import ru.auto.tests.publicapi.adaptor.offer.OfferTemplateData;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.api.*;
import ru.auto.tests.publicapi.config.PublicApiConfig;
import ru.auto.tests.publicapi.model.*;
import ru.auto.tests.publicapi.model.AutoApiOffer.SectionEnum;
import ru.auto.tests.publicapi.objects.NoteRequest;
import ru.auto.tests.publicapi.utils.DeviceUidKeeper;
import ru.auto.tests.publicapi.utils.GrouppingIdParams;
import ru.auto.tests.salesman.user.adaptor.SalesmanUserApiAdaptor;

import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static io.restassured.config.EncoderConfig.encoderConfig;
import static io.restassured.mapper.ObjectMapperType.GSON;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.apache.http.HttpStatus.SC_OK;
import static org.awaitility.Awaitility.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.validatedWith;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.auto.tests.passport.adaptor.PassportApiAdaptor.SERVICE;
import static ru.auto.tests.publicapi.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum;
import static ru.auto.tests.publicapi.model.AutoApiOffer.CategoryEnum.CARS;
import static ru.auto.tests.publicapi.model.AutoApiOffer.SectionEnum.USED;
import static ru.auto.tests.publicapi.model.AutoApiOfferHideRequest.ReasonEnum.SOLD_ON_AUTORU;
import static ru.auto.tests.publicapi.model.AutoApiReview.SubjectEnum.AUTO;
import static ru.auto.tests.publicapi.model.AutoApiSearchCarsSearchRequestParameters.EngineGroupEnum.DIESEL;
import static ru.auto.tests.publicapi.model.AutoApiSearchCarsSearchRequestParameters.GearTypeEnum.ALL_WHEEL_DRIVE;
import static ru.auto.tests.publicapi.model.AutoApiSearchCarsSearchRequestParameters.TransmissionEnum.AUTOMATIC;
import static ru.auto.tests.publicapi.model.VertisChatMessagePayload.ContentTypeEnum.PLAIN;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.defaultSpec;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.xUserCMExpertAliases;
import static ru.auto.tests.publicapi.ra.RequestSpecBuilders.withJsonBody;
import static ru.auto.tests.publicapi.ra.ResponseSpecBuilders.shouldBeSuccess;
import static ru.auto.tests.publicapi.utils.UtilsPublicApi.getRandomCreditSecretWord;
import static ru.auto.tests.publicapi.utils.UtilsPublicApi.getRandomLicensePlate;
import static ru.auto.tests.publicapi.utils.UtilsPublicApi.getRandomVin;

/**
 * Created by vicdev on 28.02.17.
 */
@Singleton
public class PublicApiAdaptor extends AbstractModule {

    private static final int DEFAULT_TTL = 360;
    private static final int POLL_INTERVAL = 3;
    private static final int POLL_DELAY = 20;
    private static final int TIMEOUT = 60;
    private static final String VOS2_PHOTO_UPLOAD_URL = "http://vos2-autoru-api.vrts-slb.test.vertis.yandex.net/api/v1/draft/{category}/user:{userId}/{draftId}/photo";
    private static final String IMAGE_URL = "https://avatars.mds.yandex.net/get-autoru-vos/2177079/5bb605acdba0d5c8adfd246f9a366606/1200x900n";

    private static final String CONTROL_UPLOAD_FILE_NAME = "file";
    private static final String ID = "id";

    private static final String App2AppHeader = "APP2APP_SUPPORT";

    @Inject
    @Prod
    private ApiClient api;

    @Inject
    private PublicApiConfig config;

    @Inject
    private ru.auto.test.passport.ApiClient passport;

    @Inject
    private SalesmanUserApiAdaptor salesmanUserAdaptor;

    @Inject
    private DeviceUidKeeper deviceUidKeeper;

    @Step("Получаем смс код отправленный пользователю {uid}")
    public String getSmsCode(String uid, Integer indexOfSms) {
        final Pattern codePattern = Pattern.compile("\\d+");
        String text = given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .pollInterval(POLL_INTERVAL, SECONDS)
                .atMost(TIMEOUT, SECONDS)
                .ignoreExceptions()
                .until(() -> passport.createService(ModerationApi.class)
                        .getEvents(SERVICE, uid, "SMS_SENT", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)
                        .execute().body().getEvents().get(indexOfSms)
                        .getPayload().getSmsSent().getText(), notNullValue());
        Matcher m = codePattern.matcher(text);
        if (m.find()) {
            return m.group();
        } else return null;
    }

    @Step("Получаем код отправленный на email юзера {uid}")
    public String getEmailCode(String uid, Integer indexOfEmail) {
        return given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .pollInterval(POLL_INTERVAL, SECONDS)
                .atMost(TIMEOUT, SECONDS)
                .ignoreExceptions()
                .until(() -> passport.createService(ModerationApi.class)
                        .getEvents(SERVICE, uid, "EMAIL_SENT", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)
                        .execute().body().getEvents().get(indexOfEmail)
                        .getPayload().getEmailSent().getArguments().get("code"), notNullValue());
    }

    //получение сессии, нужно для создания анонимной сессии с заголовками x-device-uid, x-session-id
    @Step("Получаем новую сессию под анонимным пользователем")
    public VertisPassportSessionResult session() {
        return api.session().getSession().reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBe200OkJSON()));
    }

    // TODO: AUTORUBACK-1258 Нужно для получения userTicket - пока workaround, потом выпилим
    @Step("Получаем сессию под авторизованным пользователем")
    public VertisPassportSessionResult session(String sessionId) {
        return api.session().getSession().reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .executeAs(validatedWith(shouldBe200OkJSON()));
    }

    @Step("Авторизируемся пользователем {account.login}:{account.password} ({account.id})")
    public AutoApiLoginResponse login(Account account) {
        VertisPassportLoginParameters loginParameters = new VertisPassportLoginParameters()
                .login(account.getLogin())
                .password(account.getPassword())
                .ttlSec(DEFAULT_TTL);

        return api.auth().login().reqSpec(defaultSpec())
                .body(loginParameters)
                .executeAs(validatedWith(shouldBe200OkJSON()));
    }

    @Step("Создаем пустой черновик с типом {category} для пользователя {sessionId}")
    public AutoApiDraftResponse createEmptyDraft(String sessionId, CategoryEnum category) {
        DraftApi.CreateDraftOper createDraftOper = api.draft().createDraft().categoryPath(category.name()).reqSpec(defaultSpec())
                .body(new AutoApiOffer())
                .xSessionIdHeader(sessionId);
        return given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .pollDelay(Duration.ZERO).pollInterval(POLL_INTERVAL, SECONDS).atMost(TIMEOUT, SECONDS).ignoreExceptions()
                .until(() -> createDraftOper.executeAs(validatedWith(shouldBe200OkJSON())), notNullValue());
    }

    public AutoApiDraftResponse createDraftForLuxuryCars(OfferTemplateData data, String sessionId) {
        String offerSource = new OfferTemplate().process("offers/cars_luxury.ftl", data);
        return createDraft(sessionId, CARS, offerSource);
    }

    public AutoApiDraftResponse createDraftForC2B(String login, String sessionId) {
        OfferTemplateData data = new OfferTemplateData().withPhone(login).withLicensePlate(getRandomLicensePlate()).withVin(getRandomVin());
        String offerSource = new OfferTemplate().process("offers/c2b_auction_offer.ftl", data);
        return createDraft(sessionId, CARS, offerSource);
    }

    public AutoApiC2BApplicationCreateResponse createC2BApplicationFromDraft(String draftId, String sessionId) {
        AutoApiC2bInspectPlace defaultPlace = new AutoApiC2bInspectPlace();
        defaultPlace.setLat(55.735523);
        defaultPlace.setLon(37.642475);
        defaultPlace.setComment("Создано из интеграционных тестов");


        return api.application().createC2BApplication().reqSpec(defaultSpec()).draftIdPath(draftId)
                .inspectTimeQuery("16:52")
                .inspectDateQuery("22-07-2021")
                .body(defaultPlace)
                .categoryPath(CategoryEnum.CARS)
                .xSessionIdHeader(sessionId)
                .executeAs(validatedWith(shouldBe200OkJSON()));
    }

    public AutoC2bReceptionGetApplicationResponse getC2BApplication(Long applicationId, String sessionId) {
        return api.application().getC2BApplication().reqSpec(defaultSpec()).applicationIdPath(applicationId)
                .xSessionIdHeader(sessionId)
                .executeAs(validatedWith(shouldBe200OkJSON()));
    }

    public AutoApiDraftResponse createDraft(OfferTemplateData data, String sessionId, CategoryEnum category, String templatePath) {
        String offerSource = new OfferTemplate().process(templatePath, data);
        return createDraft(sessionId, category, offerSource);
    }

    public AutoApiDraftResponse createDraft(OfferTemplateData data, String sessionId, CategoryEnum category) {
        String path = format("offers/%s.ftl", lowerCase(category.name()));
        String offerSource = new OfferTemplate().process(path, data);
        return createDraft(sessionId, category, offerSource);
    }

    public AutoApiDraftResponse createDraft(String login, String sessionId, CategoryEnum category) {
        return createDraft(new OfferTemplateData().withPhone(login).withLicensePlate(getRandomLicensePlate())
                .withVin(getRandomVin()), sessionId, category);
    }

    @Step("Создаем черновик с типом {category} для сессии {sessionId}")
    public AutoApiDraftResponse createDraft(String sessionId, CategoryEnum category, String offerSource) {
        DraftApi.CreateDraftOper createDraftOper = api.draft().createDraft().categoryPath(category.name()).reqSpec(defaultSpec())
                .reqSpec(withJsonBody(offerSource))
                .xSessionIdHeader(sessionId);

        return given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .pollDelay(Duration.ZERO).pollInterval(POLL_INTERVAL, SECONDS).atMost(TIMEOUT, SECONDS).ignoreExceptions()
                .until(() -> createDraftOper.executeAs(validatedWith(shouldBeSuccess())), notNullValue());
    }

    @Step("Публикуем черновик «{offerId}»")
    public AutoApiOffersSaveSuccessResponse publishDraft(String sessionId, CategoryEnum category, String offerId) {
        DraftApi.PublishDraftOper publishDraftOper = api.draft().publishDraft().categoryPath(category.name()).offerIdPath(offerId)
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec());

        AutoApiOffersSaveSuccessResponse response =
                given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                        .pollDelay(Duration.ZERO).pollInterval(POLL_INTERVAL, SECONDS).atMost(TIMEOUT, SECONDS).ignoreExceptions()
                        .until(() -> publishDraftOper.executeAs(validatedWith(shouldBe200OkJSON())), notNullValue());

        RestAssured.given().filter(new AllureLoggerFilter())
                .baseUri(config.getVosApiURI().toString())
                .post("/offer/{category}/{offerId}/skip-need-activation",
                        lowerCase(category.name()), response.getOfferId());
        return response;
    }

    @Step("Отправляем сообщение от лица службы поддержки с тэгом для инициации голосовалки")
    public void initPollBySupportMessageWithTag(String userId) {
        String chatMessage = format(getResourceAsString("chat/support_chat_message.json"), "user:" + userId, "###Разное:Закрытие_чата");
        RestAssured.given().filter(new AllureLoggerFilter())
                .baseUri(config.getChatApiURI().toString())
                .header("Accept", "application/json")
                .contentType("application/json")
                .body(chatMessage)
                .when()
                .post("/jivosite/auto");
    }

    @Step("Помечаем сообщение ${messageId} как спам")
    public void markChatMessageAsSpam(String messageId, Account account) {
        RestAssured.given().filter(new AllureLoggerFilter())
                .baseUri(config.getChatApiURI().toString())
                .header("Accept", "application/json")
                .header("X-User-ID", "user:" + account.getId())
                .header("X-Passport-User-ID", account.getId())
                .contentType("application/json")
                .when()
                .put("/auto/messages/spam?message_id=" + messageId + "&value=true");
    }

    @Step("Ждем пока в комнате чата будет {count} сообщений")
    public void waitUntilRoomHasCountOfMessages(int count, int pollDelay, int timeout, String roomId, String sessionId) {
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .pollDelay(pollDelay, SECONDS).pollInterval(POLL_INTERVAL, SECONDS).atMost(timeout, SECONDS).ignoreExceptions()
                .until(() -> api.chat().getMessages().roomIdQuery(roomId).xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                        .executeAs(ResponseSpecBuilders.validatedWith(shouldBeSuccess())).getMessages().size(), equalTo(count));
    }

    @Step("Создаем пустую комнату под пользователем {sessionId}")
    public AutoApiRoomResponse createEmptyRoom(String sessionId) {
        return api.chat().createRoom().reqSpec(defaultSpec()).body(new AutoApiCreateRoomRequest())
                .xSessionIdHeader(sessionId)
                .executeAs(validatedWith(shouldBeSuccess()));
    }

    @Step("Создаем комнату с пользователями {accounts} под пользователем {sessionId}")
    public AutoApiRoomResponse createRoom(String sessionId, List<Account> accounts) {
        return api.chat().createRoom().reqSpec(defaultSpec()).body(new AutoApiCreateRoomRequest()
                        .users(accounts.stream().map(r -> "user:" + r.getId()).collect(Collectors.toList())))
                .xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBeSuccess()))
                .as(AutoApiRoomResponse.class);
    }

    @Step("Создаем комнату с пользователями {accounts} под пользователем {sessionId}")
    public AutoApiRoomResponse createRoomWithOfferId(String sessionId, List<Account> accounts,
                                                     CategoryEnum category, String offerId) {
        AutoApiChatOfferSubjectSource offerSubject = new AutoApiChatOfferSubjectSource()
                .category(category.getValue()).id(offerId);

        return api.chat().createRoom().reqSpec(defaultSpec())
                .body(new AutoApiCreateRoomRequest()
                        .users(accounts.stream().map(r -> "user:" + r.getId()).collect(Collectors.toList()))
                        .subject(new AutoApiChatSubjectSource().offer(offerSubject)))
                .xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBeSuccess()))
                .as(AutoApiRoomResponse.class);
    }

    @Step("Отправляем сообщение в чатик {roomId} от пользователя {sessionId}")
    public AutoApiMessageResponse createMessage(String sessionId, String roomId) {
        return api.chat().sendMessage().reqSpec(defaultSpec())
                .body(new AutoApiSendMessageRequest()
                        .payload(new VertisChatMessagePayload().contentType(PLAIN)
                                .value(Utils.getRandomString())).roomId(roomId))
                .xSessionIdHeader(sessionId)
                .executeAs(validatedWith(shouldBeSuccess()));
    }

    @Step("Отправляем сообщение в чатик {roomId} от пользователя {userId}, видимое для {visibleFor}")
    public void createMessage(String userId, String roomId, String visibleFor) {
        String createMessageParameters = format(getResourceAsString("chat/create_message_parameters.json"), "user:" + userId, roomId, Utils.getRandomString(), "user:" + visibleFor);
        RestAssured.given().filter(new AllureLoggerFilter())
                .baseUri(config.getChatApiURI().toString())
                .header("Accept", "application/json")
                .header("X-User-ID", "user:" + userId)
                .header("X-Passport-User-ID", userId)
                .contentType("application/json")
                .body(createMessageParameters)
                .when()
                .post("/auto/messages");
    }

    @Step("Создаем объявление с типом {category} для пользователя {login} ({sessionId})")
    public AutoApiOffersSaveSuccessResponse createOffer(String login, String sessionId, CategoryEnum category) {
        String offerId = createDraft(login, sessionId, category).getOfferId();
        uploadImage(getUserIdFromSession(sessionId), category, offerId);
        return publishDraft(sessionId, category, offerId);
    }

    @Step("Создаем объявление с типом {category} для пользователя {login} ({sessionId})")
    public AutoApiOffersSaveSuccessResponse createOffer(String login, String sessionId, CategoryEnum category, String path) {
        String offerId = createDraft(sessionId, category, new OfferTemplate().process(path, login)).getOfferId();
        uploadImage(getUserIdFromSession(sessionId), category, offerId);
        return publishDraft(sessionId, category, offerId);
    }

    @Step("Ждем пока объявление {offerId} не станет активным")
    public void waitOfferActivation(String sessionId, CategoryEnum category, String offerId) {
        UserOffersApi.GetMyOfferOper getMyOfferOper = api.userOffers()
                .getMyOffer().categoryPath(category.name()).offerIDPath(offerId)
                .reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId);

        given().conditionEvaluationListener(
                        new AllureConditionEvaluationLogger()).pollDelay(Duration.ZERO).pollInterval(POLL_INTERVAL, SECONDS)
                .atMost(TIMEOUT, SECONDS).until(() -> getMyOfferOper.execute(validatedWith(shouldBeSuccess()))
                        .jsonPath().get("offer.status"), equalTo(AutoApiOffer.StatusEnum.ACTIVE.name()));
    }

    @Step("Ждем пока у пользователя появятся объявления с vin {vinCode}")
    public void waitUserOffersActive(String sessionId, CategoryEnum category, String vinCode) {
        UserOffersApi.CountOffersOper getMyActiveOffersCount = api.userOffers()
                .countOffers().categoryPath(category.name())
                .vinQuery(vinCode)
                .statusQuery(AutoApiOffer.StatusEnum.ACTIVE.name())
                .reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId);

        given().conditionEvaluationListener(
                new AllureConditionEvaluationLogger()).pollDelay(Duration.ZERO).pollInterval(POLL_INTERVAL, SECONDS)
                .atMost(TIMEOUT, SECONDS).until(() -> getMyActiveOffersCount.execute(validatedWith(shouldBeSuccess()))
                .jsonPath().get("count"), equalTo(1));
    }

    @Step("Ждем пока у пользователя появятся объявления с vin {vinCode}")
    public void waitUserOffersActiveCount(String sessionId, CategoryEnum category, int count) {
        UserOffersApi.CountOffersOper getMyActiveOffersCount = api.userOffers()
                .countOffers().categoryPath(category.name())
                .statusQuery(AutoApiOffer.StatusEnum.ACTIVE.name())
                .reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId);

        given().conditionEvaluationListener(
                new AllureConditionEvaluationLogger()).pollDelay(Duration.ZERO).pollInterval(POLL_INTERVAL, SECONDS)
                .atMost(TIMEOUT, SECONDS).until(() -> getMyActiveOffersCount.execute(validatedWith(shouldBeSuccess()))
                .jsonPath().get("count"), equalTo(count));
    }


    @Step("Снимаем с продажи «{offerId}»")
    public void hideOffer(String sessionId, CategoryEnum category, String offerId) {
        api.userOffers().hide().categoryPath(category.name().toLowerCase()).offerIDPath(offerId)
                .reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .body(new AutoApiOfferHideRequest()
                        .reason(SOLD_ON_AUTORU).manySpamCalls(true).soldPrice(40000))
                .execute(validatedWith(shouldBeSuccess()));
    }

    @Step("Удаляем оффер «{offerId}»")
    public void deleteOffer(String sessionId, CategoryEnum category, String offerId) {
        api.userOffers().hideOffer()
                .categoryPath(category)
                .offerIDPath(offerId)
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .execute(validatedWith(shouldBeSuccess()));
    }

    @Step("Добавляем объявление {offerId} в избранное с типом {category} для пользователя {sessionId}")
    public void addFavorites(String sessionId, CategoryEnum category, String offerId) {
        api.userFavorites().addFavorite().categoryPath(category.name()).offerIdPath(offerId)
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId).execute(validatedWith(shouldBeSuccess()));
    }

    @Step("Добавляем заметку {note} для оффера {offerId}")
    public void addNotes(String sessionId, CategoryEnum category, String offerId, String note) {
        api.userNotes().addNote().categoryPath(category.name()).offerIdPath(offerId)
                .reqSpec(r -> r.setBody(new NoteRequest().withNote(note)))
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId).execute(validatedWith(shouldBeSuccess()));
    }

    @Step("Получаем список сравнения пользователя ({sessionId})")
    public AutoApiCatalogCardListingResponse getCompareList(String sessionId) {
        return api.userCompare().cards().categoryPath(CARS.name()).reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId).executeAs(validatedWith(shouldBe200OkJSON()));
    }

    @Step("Добавляем карточку из каталога в список сравнения пользователя {login} ({sessionId})")
    public String addToCompare(String login, String sessionId) {
        AutoApiCarInfo carInfo = createOffer(login, sessionId, CARS).getOffer().getCarInfo();
        String catalogId = getCatalogId(carInfo);
        api.userCompare().addCard().categoryPath(CARS.name()).catalogCardIdPath(catalogId).reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBeSuccess()));
        return catalogId;
    }

    @Step("Добавляем оффер в список сравнения пользователя {login} ({sessionId})")
    public String addOfferToCompare(String login, String sessionId) {
        AutoApiOffer offer = createOffer(login, sessionId, CARS).getOffer();
        String favoriteId = "favorite-" + offer.getId();
        api.userCompare().addCard().categoryPath(CARS.name()).catalogCardIdPath(favoriteId).reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBeSuccess()));
        return offer.getId();
    }

    @Step("Добавляем подписку для пользователя {sessionId}")
    public AutoApiSearchInstance addSearch(CategoryEnum category, String sessionId) {
        return api.userFavorites().addSavedSearch()
                .categoryPath(category.name())
                .body(getDefaultSavedSearch())
                .reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .reqSpec(ridParam()).executeAs(validatedWith(shouldBeSuccess())).getSearch();
    }

    @Step("Получаем оффер по id «{offerId}»")
    public AutoApiOfferResponse getOffer(CategoryEnum category, String offerId) {
        return api.offerCard().getOfferCard().reqSpec(defaultSpec()).categoryPath(category.name())
                .offerIDPath(offerId)
                .executeAs(validatedWith(shouldBeSuccess()));
    }

    @Step("Получаем черновик по id «{draftId}» для пользователя {sessionId}")
    public AutoApiDraftResponse getDraft(String sessionId, CategoryEnum category, String draftId) {
        return api.draft().getDraft().reqSpec(defaultSpec())
                .categoryPath(category.name())
                .offerIdPath(draftId)
                .xSessionIdHeader(sessionId)
                .executeAs(validatedWith(shouldBeSuccess()));
    }

    @Step("Получаем оффер по id «{offerId}» с сессией")
    public AutoApiOfferResponse getOfferWithSession(CategoryEnum category, String offerId, String sessionId) {
        return api.offerCard().getOfferCard().reqSpec(defaultSpec())
                .categoryPath(category.name())
                .offerIDPath(offerId)
                .xSessionIdHeader(sessionId)
                .executeAs(validatedWith(shouldBeSuccess()));
    }

    @Step("Получаем рандомный оффер cars из поиска")
    public AutoApiOffer getRandomCarsOfferFromSearch() {
        AutoApiOfferListingResponse searchResponse = api.search().searchCars().reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBe200OkJSON()));

        return searchResponse.getOffers().stream().findAny().get();
    }

    @Step("Получаем оффер по id «{offerId}»")
    public JsonObject getUserOffer(String sessionId, CategoryEnum category, String offerId) {
        return api.userOffers().getMyOffer().reqSpec(defaultSpec())
                .categoryPath(category.name())
                .offerIDPath(offerId)
                .xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBeSuccess()))
                .as(JsonObject.class, GSON);
    }

    @Step("Создаем расписание применения услуги schedule_type==ONCE_AT_TIME")
    public void createSchedule(String sessionId, CategoryEnum category, String offerId, String product, String time,
                               AutoApiBillingSchedulesScheduleRequest.ScheduleTypeEnum scheduleType) {
        api.billingSchedules().upsertSchedule().categoryPath(category.name()).offerIdPath(offerId).productPath(product)
                .body(new AutoApiBillingSchedulesScheduleRequest()
                        .time(time).scheduleType(scheduleType))
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId).execute(validatedWith(shouldBeSuccess()));
    }

    private AutoApiSavedSearchCreateParams getDefaultSavedSearch() {
        return new AutoApiSavedSearchCreateParams()
                .title(Utils.getRandomString())
                .params(new AutoApiSearchSearchRequestParameters());
    }

    @Step("Создаем черновик отзыва c телом {review}")
    public AutoApiReviewSaveResponse saveReview(String login, String sessionId, String review) {
        String body = format(getResourceAsString(review), login);
        return api.reviews().createReview().subjectPath(AUTO)
                .reqSpec(defaultSpec())
                .reqSpec(withJsonBody(body))
                .xSessionIdHeader(sessionId).executeAs(validatedWith(shouldBeSuccess()));
    }

    @Step("Добавляем комментарий: '{message}' для отзыва {reviewId}")
    public AutoApiAddCommentResponse addComment(String sessionId, AutoApiReview.SubjectEnum path, String reviewId, String message) {
        return api.reviews().addComments().subjectPath(path).reviewIdPath(reviewId)
                .body(new AutoApiAddCommentRequest().message(message)).xSessionIdHeader(sessionId)
                .reqSpec(defaultSpec()).executeAs(validatedWith(shouldBeSuccess()));
    }

    @Step("Добавляем комментарий '{message}' к родительскому комментарию {parentCommentId} для отзыва {reviewId}")
    public AutoApiAddCommentResponse addChildComment(String sessionId, AutoApiReview.SubjectEnum path, String reviewId, String parentCommentId, String message) {
        ReviewsApi.AddCommentsOper addCommentsOper = api.reviews().addComments().subjectPath(path).reviewIdPath(reviewId)
                .body(new AutoApiAddCommentRequest().message(message).parentId(Integer.valueOf(parentCommentId)))
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec());

        return given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .pollDelay(POLL_DELAY, SECONDS).pollInterval(POLL_INTERVAL, SECONDS).atMost(TIMEOUT, SECONDS)
                .ignoreExceptions().until(() -> addCommentsOper
                        .executeAs(validatedWith(shouldBeSuccess())), notNullValue());
    }

    @Step("Удаляем комментарий {commentId} у отзыва {reviewId}")
    public void deleteComment(String sessionId, AutoApiReview.SubjectEnum path, String reviewId, String commentId) {
        api.reviews().deleteCommentReview().subjectPath(path).reviewIdPath(reviewId).commentIdPath(commentId)
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec()).executeAs(validatedWith(shouldBeSuccess()));
    }

    @Step("Получаем отзыв {reviewId} пользователя {sessionId}")
    public AutoApiReviewResponse getUserReview(String sessionId, String reviewId) {
        return api.userReviews().userReview().reviewIdPath(reviewId).xSessionIdHeader(sessionId)
                .reqSpec(defaultSpec()).execute(validatedWith(shouldBeSuccess())).as(AutoApiReviewResponse.class);
    }

    @Step("Ставим {opinionPath} отзыву {reviewId} от пользователя {sessionId}")
    public void setReviewOpinion(String sessionId, AutoApiReview.SubjectEnum subjectPath, String reviewId,
                                 AutoApiReview.UserOpinionEnum opinionPath) {
        api.reviews().setOpinion().subjectPath(subjectPath).reqSpec(defaultSpec()).xSessionIdHeader(sessionId)
                .reviewIdPath(reviewId).opinionPath(opinionPath).execute(validatedWith(shouldBeSuccess()));
    }


    @Step("Получаем подписку {id} для пользователя {sessionId}")
    public AutoApiSavedSearchResponse getSavedSearch(String sessionId, String id) {
        return api.userFavorites().getSavedSearch().idPath(id)
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId).executeAs(validatedWith(shouldBeSuccess()));
    }

    @Step("Добавляем почту пользователю {sessionId}")
    public void addEmailToUser(String sessionId, String uid, String email) {
        createEmailChangeRequest(sessionId, email);
        String code = getEmailCode(uid, 0);
        confirmEmailChangeRequest(sessionId, code, email, email);
        String secondCode = getEmailCode(uid, 1);

        api.user().confirmIdentity().body(new VertisPassportConfirmIdentityParameters().code(secondCode).email(email))
                .reqSpec(defaultSpec()).executeAs(ResponseSpecBuilders.validatedWith(shouldBe200OkJSON()));
    }

    @Step("Запрашиваем смену пароля для пользователя {sessionId}")
    public void createPasswordChangeRequest(String sessionId, String phone) {
        api.userPassword().requestPasswordReset().body(new VertisPassportRequestPasswordResetParameters().phone(phone))
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec()).execute(ResponseSpecBuilders.validatedWith(shouldBeSuccess()));
    }

    @Step("Запрашиваем смену email для пользователь {sessionId}")
    public void createEmailChangeRequest(String sessionId, String email) {
        api.userEmail().requestEmailChangeCode()
                .body(new VertisPassportRequestEmailChangeParameters().currentIdentity(new VertisPassportUserIdentity().email(email)))
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec()).executeAs(ResponseSpecBuilders.validatedWith(shouldBeCode(SC_OK)));
    }

    @Step("Подтверждаем смену имейла с {email} на {newEmail} для пользователя {sessionId}")
    public void confirmEmailChangeRequest(String sessionId, String code, String email, String newEmail) {
        api.userEmail().changeEmail().body(new VertisPassportChangeEmailParameters().email(newEmail)
                        .confirmationCode(new VertisPassportChangeEmailParametersConfirmationCode().code(code).identity(new VertisPassportUserIdentity().email(email))))
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec()).execute(ResponseSpecBuilders.validatedWith(shouldBeSuccess()));
    }

    @Step("Получаем объявления найденные по тегу «{searchTag}»")
    public AutoApiOfferListingResponse getOfferListingWithSearchTag(String searchTag) {
        return api.search().postSearchCars().body(new AutoApiSearchSearchRequestParameters().searchTag(Arrays.asList(searchTag)))
                .pageQuery(1).pageSizeQuery(1).reqSpec(defaultSpec()).executeAs(validatedWith(shouldBeSuccess()));
    }

    @Step("Создаем группу для дилера {sessionId}")
    public AutoCabinetGroup createDealerUsersGroup(String sessionId, AutoCabinetGroup group) {
        return api.dealer().createUsersGroup().reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .body(group)
                .executeAs(ResponseSpecBuilders.validatedWith(shouldBe200OkJSON()));
    }

    @Step("Удаляем группу {groupId} для дилера {sessionId}")
    public void deleteDealerUsersGroup(String sessionId, Long groupId) {
        api.dealer().deleteUsersGroup().reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .groupIdPath(groupId)
                .execute(validatedWith(shouldBe200OkJSON()));
    }

    @Step("Ждем пока подключится услуга {code} к офферу {offerId} категории {category}")
    public void applyProduct(String code, CategoryEnum category, String offerId, String sessionId) {
        api.userOffers().applyProducts().categoryPath(category).offerIDPath(offerId)
                .body(new AutoApiApplyAutoruProductsRequest().products(newArrayList(new AutoSalesmanAutoruProduct().code(code))))
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId).execute(validatedWith(shouldBeSuccess()));

        given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .pollDelay(POLL_DELAY, SECONDS).pollInterval(POLL_INTERVAL, SECONDS).atMost(TIMEOUT, SECONDS)
                .ignoreExceptions().until(() ->
                        api.userOffers().getMyOffer().categoryPath(category).offerIDPath(offerId)
                                .xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                                .executeAs(validatedWith(shouldBeSuccess())).getOffer().getServices()
                                .stream()
                                .filter(PaidService -> PaidService.getService().equals(code))
                                .collect(Collectors.toList())
                                .get(0).getService(), equalTo(code));
    }

    @Step("Ждем пока подключится услуга {product} к офферу {offerId} категории {category}")
    public void applyBadgeProduct(List<AutoSalesmanAutoruProduct> product, CategoryEnum category, String offerId, String sessionId) {
        api.userOffers().applyProducts().categoryPath(category).offerIDPath(offerId)
                .body(new AutoApiApplyAutoruProductsRequest().products(newArrayList(product)))
                .reqSpec(defaultSpec()).xSessionIdHeader(sessionId).execute(validatedWith(shouldBeSuccess()));

        given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .pollDelay(POLL_DELAY, SECONDS).pollInterval(POLL_INTERVAL, SECONDS).atMost(TIMEOUT, SECONDS)
                .ignoreExceptions().until(() ->
                        api.userOffers().getMyOffer().categoryPath(category).offerIDPath(offerId)
                                .xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                                .executeAs(validatedWith(shouldBeSuccess())).getOffer().getBadges()
                                .get(0), equalTo(product.get(0).getBadges().get(0)));
    }

    @Step("Покупаем отчет по VIN или ГосНомеру {vinOrLicensePlate} для пользователя {sessionId}")
    public void buyVinHistory(String sessionId, String userId, String vinOrLicensePlate) {
        buyVinHistory(userId);

        api.carfax().rawReport()
                .vinOrLicensePlateQuery(vinOrLicensePlate)
                .xSessionIdHeader(sessionId)
                .reqSpec(defaultSpec())
                .reqSpec(requestSpecBuilder -> requestSpecBuilder.addQueryParam("decrement_quota", "true"))
                .executeAs(validatedWith(shouldBe200OkJSON()));
    }

    @Step("Покупаем отчет по id объявления {offerId} для пользователя {sessionId}")
    public void buyVinOfferHistory(String sessionId, String userId, String offerId) {
        buyVinHistory(userId);

        api.carfax().offerRawReport()
                .reqSpec(defaultSpec())
                .offerIDPath(offerId)
                .categoryPath(CARS)
                .decrementQuotaQuery(true)
                .xSessionIdHeader(sessionId)
                .execute(ResponseSpecBuilders.validatedWith(shouldBe200OkJSON()));
    }

    @Step("Покупаем отчет по VIN или ГосНомеру {vinOrLicensePlate} для пользователя {sessionId}")
    public void buyVinHistory(String userId) {
        Response<AutoSalesmanUserCreateTransactionResult> response = salesmanUserAdaptor.createSubscriptionTransaction(userId, "offers-history-reports", 500L);
        salesmanUserAdaptor.activatePayment(response.body().getTransactionId());
    }

    @Step("Покупаем услугу {product} для оффера {offerId} пользователя {userId}")
    public void buyOfferService(String userId, String offerId, String product) {
        Response<AutoSalesmanUserCreateTransactionResult> response = salesmanUserAdaptor.createTransactionForOffer(userId, product, offerId);
        salesmanUserAdaptor.activatePayment(response.body().getTransactionId());
    }

    @Step("Добавляем сохраненный поиск анонимному пользователю и возвращаем полученный deviceUid")
    public String getDeviceUidByAddingSubscription(CategoryEnum category, Consumer<RequestSpecBuilder> reqSpec) {
        String deviceUid = api.userFavorites().addSavedSearch().categoryPath(category).reqSpec(reqSpec)
                .reqSpec(defaultSpec()).execute(ResponseSpecBuilders.validatedWith(shouldBeSuccess())).getHeader("x-device-uid");
        deviceUidKeeper.add(deviceUid);

        return deviceUid;
    }

    @Step("Инициируем платеж для оффера {offerId} у пользователя {sessionId}")
    public AutoApiBillingInitPaymentResponse initPayment(String sessionId, String offerId, String productName) {
        return api.billing().initPayment()
                .reqSpec(defaultSpec())
                .salesmanDomainPath("autoru")
                .xSessionIdHeader(sessionId)
                .body(new AutoApiBillingInitPaymentRequest()
                        .addProductItem(new AutoApiBillingInitPaymentRequestProduct().name(productName).count(1))
                        .autoruPurchase(new AutoApiBillingInitPaymentRequestAutoruPurchase().offerId(offerId).prolongable(false)))
                .executeAs(validatedWith(shouldBe200OkJSON()));
    }

    @Step("Удаляем алиас у дилера")
    public AutoDealerAliasesDropResponse deleteDealerAlias(AutoDealerAliasesV2PublicInput requestBody) {
        return api.dealer().deleteAlias().reqSpec(xUserCMExpertAliases())
                .body(requestBody)
                .executeAs(validatedWith(shouldBe200OkJSON()));
    }

    @Step("Добавлям алиас дилеру")
    public AutoDealerAliasesCreateResponse addDealerAlias(AutoDealerAliasesV2PublicInput requestBody) {
        return api.dealer().createAlias().reqSpec(xUserCMExpertAliases())
                .body(requestBody)
                .executeAs(validatedWith(shouldBe200OkJSON()));
    }

    @Step("Загружаем фото в черновик")
    public void uploadImage(String userId, CategoryEnum category, String draftId) {
        RestAssured.given().config(RestAssuredConfig.config()
                        .sslConfig(SSLConfig.sslConfig().relaxedHTTPSValidation())
                        .encoderConfig(encoderConfig().encodeContentTypeAs("multipart/form-data", ContentType.TEXT)))
                .filter(new AllureLoggerFilter())
                .contentType("multipart/form-data")
                .pathParam("userId", userId)
                .pathParam("category", category.getValue().toLowerCase())
                .pathParam("draftId", draftId)
                .multiPart("url", IMAGE_URL)
                .post(VOS2_PHOTO_UPLOAD_URL);
    }

    public UploadResponse uploadImageFile(String uploadUrl, String photoPath) {
        JsonElement result = uploadImageFromFile(uploadUrl, photoPath, CONTROL_UPLOAD_FILE_NAME);
        return new GsonBuilder().create().fromJson(result, UploadResponse.class);
    }

    public AutoApiPhoto uploadImageFileToDraft(String uploadUrlFromDraft, String photoPath) {
        JsonElement result = uploadImageFromFile(uploadUrlFromDraft, photoPath, CONTROL_UPLOAD_FILE_NAME).get("photo");
        return new GsonBuilder().create().fromJson(result, AutoApiPhoto.class);
    }

    public String uploadImageFileToSafeDeal(String uploadUrl, String photoPath) {
        JsonElement result = uploadImageFromFile(uploadUrl, photoPath, CONTROL_UPLOAD_FILE_NAME);
        return result.getAsJsonObject().get(ID).getAsString();
    }

    @Step("Загружаем фото из файла {photoPath} по ссылке {uploadUrl}")
    public JsonObject uploadImageFromFile(String uploadUrl, String photoPath, String controlName) {
        File file = new File(getClass().getClassLoader().getResource(photoPath).getFile());
        return RestAssured.given()
                .config(RestAssuredConfig.config().sslConfig(SSLConfig.sslConfig().relaxedHTTPSValidation()))
                .filter(new AllureLoggerFilter()).contentType("multipart/form-data")
                .multiPart(controlName, file, "image/jpeg")
                .post(uploadUrl).then().statusCode(SC_OK).extract()
                .as(JsonObject.class, GSON);
    }

    @Step("Вращаем картинку {photoId} в черновике {draftId} по часовой стрелки")
    public AutoApiPhoto rotateDraftImageCw(String sessionId, String draftId, String photoId) {
        AutoApiPhotoSaveSuccessResponse photoResponse = api.draftPhotos().rotateCWPhoto()
                .categoryPath(AutoApiOffer.CategoryEnum.CARS)
                .offerIDPath(draftId)
                .photoIDPath(photoId)
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess()));

        return photoResponse.getPhoto();
    }

    @Step("Вращаем картинку {photoId} в черновике {draftId} против часовой стрелки")
    public AutoApiPhoto rotateDraftImageCcw(String sessionId, String draftId, String photoId) {
        AutoApiPhotoSaveSuccessResponse photoResponse = api.draftPhotos().rotateCCWPhoto()
                .categoryPath(AutoApiOffer.CategoryEnum.CARS)
                .offerIDPath(draftId)
                .photoIDPath(photoId)
                .xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBeSuccess()));

        return photoResponse.getPhoto();
    }

    @Step("Создаем комментарий к отчету по VIN {vin} для блока {blockId}")
    public void createCarfaxComment(String sessionId, String vin, String blockId, String text) {
        AutoApiVinCommentsAddVinReportCommentRequest body = new AutoApiVinCommentsAddVinReportCommentRequest()
                .comment(new AutoApiVinCommentsVinReportComment().blockId(blockId).text(text));

        api.carfax().putComment().reqSpec(defaultSpec())
                .vinQuery(vin)
                .body(body)
                .xSessionIdHeader(sessionId)
                .executeAs(ResponseSpecBuilders.validatedWith(shouldBe200OkJSON()));
    }

    @Step("Создать задачку Feedprocessor для клиента")
    public AutoApiFeedprocessorFeedFeedTask createFeedprocessorTaskForDealer(String sessionId) {
        AutoApiFeedprocessorFeedFeedTaskTaskSettings body = new AutoApiFeedprocessorFeedFeedTaskTaskSettings()
                .internalUrl("https://vertis-feeds.s3.mds.yandex.net/9452beff61da74d35ef31d58e4c8cf7a")
                .settings(new AutoApiFeedprocessorFeedFeedSettings().source(getRandomString()).deleteSale(false)
                        .leaveAddedImages(true).leaveServices(true).isActive(true));

        return api.feeds().createTaskCars().reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .sectionPath(USED)
                .body(body)
                .executeAs(ResponseSpecBuilders.validatedWith(shouldBe200OkJSON()));
    }

    @Step("Получить токен для авторизации пользователя")
    public String getUserLoginToken(String userId) {
        UserAuthTokenCreateParameters body = new UserAuthTokenCreateParameters().ttlSec(3600);
        try {
            return passport.createService(UserApi.class)
                    .createToken1(userId, SERVICE, body, null, null, null, null, null, null, null, null, null, null, null)
                    .execute().body().getId();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Step("Создаем комментарий для {topicGroup}/{topicId}")
    public AutoApiAddCommentResponse createComment(String sessionId, String topicGroup, String topicId) {
        return api.comments().addComment().reqSpec(defaultSpec())
                .topicGroupPath(topicGroup)
                .topicIdPath(topicId)
                .body(new AutoApiAddCommentRequest().message(getRandomString()))
                .xSessionIdHeader(sessionId)
                .executeAs(validatedWith(shouldBe200OkJSON()));
    }

    @Step("Получаем ID рандомной истории")
    public String getRandomStoryId() {
        VertisStoryStoryResponse response = api.story().searchStory()
                .reqSpec(defaultSpec())
                .executeAs(validatedWith(shouldBe200OkJSON()));

        return response.getStories().stream().findAny().get().getId();
    }

    @Step("Создаем заявку на кредит для сессии {sessionId}")
    public VertisSharkApiCreditApplicationResponse createCreditApplication(String sessionId) {
        VertisSharkCreditApplicationInfo info = new VertisSharkCreditApplicationInfo()
                .controlWord(new VertisSharkBlockControlWordBlock().word(getRandomCreditSecretWord()))
                .advertStatementAgreement(new VertisSharkBlockAdvertStatementAgreementBlock().isAgree(true).timestamp(OffsetDateTime.now()))
                .okbStatementAgreement(new VertisSharkBlockOkbStatementAgreementBlock().isAgree(true).timestamp(OffsetDateTime.now()));
        VertisSharkCreditApplicationRequirements requirements = new VertisSharkCreditApplicationRequirements()
                .maxAmount((long) new Random().nextInt(1000000)).initialFee((long) new Random().nextInt(100000))
                .termMonths(new Random().nextInt(48)).geobaseIds(newArrayList(213L));
        VertisSharkPersonProfile personalProfile = new VertisSharkPersonProfile()
                .name(new VertisSharkBlockNameBlock().nameEntity(new VertisSharkEntityNameEntity().name("Иван").surname("Иванов").patronymic("Иванович")))
                .gender(new VertisSharkBlockGenderBlock().genderType(VertisSharkBlockGenderBlock.GenderTypeEnum.MALE))
                .passportRf(new VertisSharkBlockPassportRfBlock().passportRfEntity(
                        new VertisSharkEntityPassportRfEntity().series("0000").number("111222").departCode("111-222").departName("ОУФМС")
                                .issueDate(OffsetDateTime.now().minusYears(2L))))
                .phones(new VertisSharkBlockPhonesBlock().addPhoneEntitiesItem(new VertisSharkEntityPhoneEntity().phone("79097585110")));

        return api.shark().creditApplicationCreate().reqSpec(defaultSpec())
                .body(new VertisSharkCreditApplicationSource().info(info).requirements(requirements).borrowerPersonProfile(personalProfile))
                .xSessionIdHeader(sessionId)
                .executeAs(validatedWith(shouldBe200OkJSON()));
    }

    @Step("Создаем заявку на кредит для сессии {sessionId}")
    public VertisSharkApiCreditApplicationResponse createCreditApplication(String sessionId, String creditApplicationSource) {
        return api.shark().creditApplicationCreate().reqSpec(defaultSpec())
                .reqSpec(withJsonBody(getResourceAsString(creditApplicationSource)))
                .xSessionIdHeader(sessionId)
                .executeAs(validatedWith(shouldBe200OkJSON()));
    }

    @Step("Получаем заявку на кредит {applicationId} для сессии {sessionId}")
    public AutoApiSharkRichCreditApplicationResponse getCreditApplication(String sessionId, String applicationId) {
        return api.shark().creditApplicationGet().reqSpec(defaultSpec())
                .creditApplicationIdPath(applicationId)
                .xSessionIdHeader(sessionId)
                .executeAs(validatedWith(shouldBe200OkJSON()));
    }

    @Step("Получаем заявку на кредит {applicationId} для сессии {sessionId}")
    public VertisSharkApiCreditApplicationResponse cancelCreditApplication(String sessionId, String applicationId) {
        return api.shark().creditApplicationUpdate().reqSpec(defaultSpec())
                .creditApplicationIdPath(applicationId)
                .body(new VertisSharkCreditApplicationSource()
                        .state(VertisSharkCreditApplicationSource.StateEnum.CANCELED))
                .xSessionIdHeader(sessionId)
                .executeAs(validatedWith(shouldBe200OkJSON()));
    }

    @Step("Получаем cписок кредитных продуктов для сессии {sessionId}")
    public AutoApiSharkRichCreditProductsResponse getAllCreditProducts(String sessionId) {
        return api.shark().creditProductList()
                .reqSpec(defaultSpec())
                .body(new VertisSharkApiCreditProductsRequest().all(new Object()))
                .xSessionIdHeader(sessionId)
                .executeAs(validatedWith(shouldBe200OkJSON()));
    }

    @Step("Получаем сделку для сессии {sessionId} и сделки {dealId}")
    public VertisSafeDealDealView getDeal(String sessionId, String dealId) {
        return api.safeDeal().dealGet()
                .reqSpec(defaultSpec())
                .dealIdPath(dealId)
                .xSessionIdHeader(sessionId)
                .executeAs(validatedWith(shouldBe200OkJSON()))
                .getDeal();
    }

    @Step("Обновляем сделку для сессии {sessionId} и сделки {dealId}")
    private VertisSafeDealDealView updateDeal(String sessionId, String dealId, VertisSafeDealApiDealUpdateRequest request) {
        return api.safeDeal().dealUpdate()
                .reqSpec(defaultSpec())
                .dealIdPath(dealId)
                .body(request)
                .xSessionIdHeader(sessionId)
                .executeAs(validatedWith(shouldBe200OkJSON()))
                .getDeal();
    }

    public VertisSafeDealDealView updateDealByBuyer(String sessionId, String dealId, VertisSafeDealApiDealUpdateRequestByBuyer byBuyer) {
        return updateDeal(sessionId, dealId, new VertisSafeDealApiDealUpdateRequest().byBuyer(byBuyer));
    }

    public VertisSafeDealDealView updateDealBySeller(String sessionId, String dealId, VertisSafeDealApiDealUpdateRequestBySeller bySeller) {
        return updateDeal(sessionId, dealId, new VertisSafeDealApiDealUpdateRequest().bySeller(bySeller));
    }

    @Step("Формируем запрос на создание сделки по офферу {offerId}")
    public VertisSafeDealApiDealCreateRequest dealCreateRequest(String offerId) {
        return new VertisSafeDealApiDealCreateRequest()
                .domain(VertisSafeDealApiDealCreateRequest.DomainEnum.AUTO)
                .subject(
                        new VertisSafeDealApiDealCreateRequestSubject().autoru(
                                new VertisSafeDealApiDealCreateRequestSubjectAutoru().byOffer(
                                        new VertisSafeDealApiDealCreateRequestSubjectAutoruByOffer().offer(
                                                new VertisSafeDealDealSubjectAutoruOffer()
                                                    .id(offerId)
                                                    .category(VertisSafeDealDealSubjectAutoruOffer.CategoryEnum.CARS)
                                        )
                                )
                        )
                );
    }

    @Step("Создаем сделку покупателем для сессии {buyerSessionId} и оффера {offerId}")
    public VertisSafeDealDealView createDealByBuyer(String buyerSessionId, String offerId) {
        VertisSafeDealApiDealCreateRequest request = dealCreateRequest(offerId);
        return api.safeDeal().dealCreate()
                .reqSpec(defaultSpec())
                .body(request)
                .xSessionIdHeader(buyerSessionId)
                .executeAs(validatedWith(shouldBe200OkJSON()))
                .getDeal();
    }

    @Step("Принимаем запрос на сделку продавцом для сессии {sellerSessionId} и сделке {dealId}")
    public VertisSafeDealDealView acceptDealBySeller(String sellerSessionId, String dealId, Boolean approve) {
        VertisSafeDealApiDealUpdateRequestBySeller bySeller =
                new VertisSafeDealApiDealUpdateRequestBySeller().newDealApprove(
                        new VertisSafeDealApiDealUpdateRequestApproveUpdate().approve(approve)
                );
        return updateDealBySeller(sellerSessionId, dealId, bySeller);
    }

    @Step("Вводим паспортные данные покупателем для сессии {buyerSessionId} и сделке {dealId}")
    public VertisSafeDealDealView updatePassportByBuyer(String buyerSessionId, String dealId, VertisSafeDealEntityNameEntity nameEntity, VertisSafeDealEntityPassportRfEntity passportRfEntity, VertisSafeDealEntityPhoneEntity phoneEntity) {
        VertisSafeDealApiDealUpdateRequestByBuyer byBuyer =
                new VertisSafeDealApiDealUpdateRequestByBuyer().personProfileUpdate(
                        new VertisSafeDealApiDealUpdateRequestPersonProfileUpdate()
                                .nameEntity(nameEntity)
                                .passportRfEntity(passportRfEntity)
                                .phoneEntity(phoneEntity)
                );
        return updateDealByBuyer(buyerSessionId, dealId, byBuyer);
    }

    @Step("Вводим паспортные данные продавцом для сессии {sellerSessionId} и сделке {dealId}")
    public VertisSafeDealDealView updatePassportBySeller(String sellerSessionId, String dealId, VertisSafeDealEntityNameEntity nameEntity, VertisSafeDealEntityPassportRfEntity passportRfEntity, VertisSafeDealEntityPhoneEntity phoneEntity) {
        VertisSafeDealApiDealUpdateRequestBySeller bySeller =
                new VertisSafeDealApiDealUpdateRequestBySeller().personProfileUpdate(
                        new VertisSafeDealApiDealUpdateRequestPersonProfileUpdate()
                                .nameEntity(nameEntity)
                                .passportRfEntity(passportRfEntity)
                                .phoneEntity(phoneEntity)
                );
        return updateDealBySeller(sellerSessionId, dealId, bySeller);
    }

    @Step("Вводим данные авто продавцом для сессии {sellerSessionId} и сделке {dealId}")
    public VertisSafeDealDealView updateSubjectInfoBySeller(String sellerSessionId, String dealId, VertisSafeDealDealSubjectAutoruOfferCarInfo offerCarInfo, VertisSafeDealDealSubjectAutoruPtsCarInfo ptsCarInfo, VertisSafeDealDealSubjectAutoruStsCarInfo stsCarInfo) {
        VertisSafeDealApiDealUpdateRequestBySeller bySeller =
                new VertisSafeDealApiDealUpdateRequestBySeller().subjectInfoUpdate(
                        new VertisSafeDealApiDealUpdateRequestSubjectUpdate().autoru(
                                new VertisSafeDealApiDealUpdateRequestSubjectUpdateAutoru()
                                        .carInfo(offerCarInfo)
                                        .ptsInfo(ptsCarInfo)
                                        .stsInfo(stsCarInfo)
                        )
                );
        return updateDealBySeller(sellerSessionId, dealId, bySeller);
    }

    @Step("Предлагаем цену покупателем для сессии {buyerSessionId} и сделке {dealId}")
    public VertisSafeDealDealView updateSellingPriceByBuyer(String buyerSessionId, String dealId, long sellingPriceRub) {
        VertisSafeDealApiDealUpdateRequestByBuyer byBuyer =
                new VertisSafeDealApiDealUpdateRequestByBuyer().sellingPriceUpdate(
                        new VertisSafeDealApiDealUpdateRequestSellingPriceUpdate()
                                .sellingPriceRub(sellingPriceRub)
                );
        return updateDealByBuyer(buyerSessionId, dealId, byBuyer);
    }

    @Step("Подтверждаем цену продавцом для сессии {sellerSessionId} и сделке {dealId}")
    public VertisSafeDealDealView approveSellingPriceBySeller(String sellerSessionId, String dealId, long sellingPriceRub) {
        VertisSafeDealApiDealUpdateRequestBySeller bySeller =
                new VertisSafeDealApiDealUpdateRequestBySeller().priceApprove(
                        new VertisSafeDealApiDealUpdateRequestSellingPriceUpdate()
                                .sellingPriceRub(sellingPriceRub)
                );
        return updateDealBySeller(sellerSessionId, dealId, bySeller);
    }

    @Step("Вводим реквизиты продавцом для сессии {sellerSessionId} и сделке {dealId}")
    public VertisSafeDealDealView updateBankingDetailsBySeller(String sellerSessionId, String dealId, VertisSafeDealEntityBankingEntity bankingEntity) {
        VertisSafeDealApiDealUpdateRequestBySeller bySeller =
                new VertisSafeDealApiDealUpdateRequestBySeller().sellerBankAccountUpdate(
                        new VertisSafeDealApiDealUpdateRequestSellerBankAccountUpdate()
                                .bankingEntity(bankingEntity)
                );
        return updateDealBySeller(sellerSessionId, dealId, bySeller);
    }

    @Step("Вводим место и дату встречи покупателем для сессии {buyerSessionId} и сделке {dealId}")
    public VertisSafeDealDealView updateMeetingDetailsByBuyer(String buyerSessionId, String dealId, long geobaseId, String meetingDate) {
        VertisSafeDealApiDealUpdateRequestByBuyer byBuyer =
                new VertisSafeDealApiDealUpdateRequestByBuyer().personalMeetingUpdate(
                        new VertisSafeDealApiDealUpdateRequestPersonalMeetingUpdate()
                                .geobaseId(geobaseId)
                                .meetingDate(meetingDate)
                );
        return updateDealByBuyer(buyerSessionId, dealId, byBuyer);
    }

    @Step("Загружаем документы покупателем для сессии {buyerSessionId} и сделке {dealId}")
    public VertisSafeDealDealView uploadDocumentByBuyer(String buyerSessionId, String dealId, String documentName, String path) {
        String documentId = uploadImageFileToSafeDeal(getDeal(buyerSessionId, dealId).getParty().getBuyer().getDealContractUploadUrl(), path);
        VertisSafeDealApiDealUpdateRequestByBuyer byBuyer =
                new VertisSafeDealApiDealUpdateRequestByBuyer().documentApprove(
                        new VertisSafeDealApiDealUpdateRequestApproveDocumentUpdate()
                                .documentName(documentName)
                                .documentId(documentId)
                );
        return updateDealByBuyer(buyerSessionId, dealId, byBuyer);
    }

    @Step("Подтверждаем подписанные документы продавцом для сессии {sellerSessionId} и сделке {dealId}")
    public VertisSafeDealDealView approveDocumentsBySeller(String sellerSessionId, String dealId) {
        VertisSafeDealApiDealUpdateRequestBySeller bySeller =
                new VertisSafeDealApiDealUpdateRequestBySeller().documentsApprove(
                        new VertisSafeDealApiDealUpdateRequestApproveUpdate()
                                .approve(true)
                );
        return updateDealBySeller(sellerSessionId, dealId, bySeller);
    }

    @Step("Запрашиваем код подтвержединия сделки продавцом для сессии {sellerSessionId} и сделке {dealId}")
    public VertisSafeDealDealView requestCodeBySeller(String sellerSessionId, String dealId) {
        VertisSafeDealApiDealUpdateRequestBySeller bySeller =
                new VertisSafeDealApiDealUpdateRequestBySeller().dealRequestConfirmationCode(
                        new VertisSafeDealApiDealUpdateRequestApproveUpdate()
                                .approve(true)
                );
        return updateDealBySeller(sellerSessionId, dealId, bySeller);
    }

    @Step("Вводим код подтвержединия сделки продавцом для сессии {sellerSessionId} и сделке {dealId}")
    public VertisSafeDealDealView confirmCodeBySeller(String sellerSessionId, String dealId, String code) {
        VertisSafeDealApiDealUpdateRequestBySeller bySeller =
                new VertisSafeDealApiDealUpdateRequestBySeller().completeDealConfirm(
                        new VertisSafeDealApiDealUpdateRequestConfirmDealWithCode()
                                .code(code)
                );
        return updateDealBySeller(sellerSessionId, dealId, bySeller);
    }

    @Step("Запрашиваем код подтвержединия сделки покупателем сессии {buyerSessionId} и сделке {dealId}")
    public VertisSafeDealDealView requestCodeByBuyer(String buyerSessionId, String dealId) {
        VertisSafeDealApiDealUpdateRequestByBuyer byBuyer =
                new VertisSafeDealApiDealUpdateRequestByBuyer().dealRequestConfirmationCode(
                        new VertisSafeDealApiDealUpdateRequestApproveUpdate()
                                .approve(true)
                );
        return updateDealByBuyer(buyerSessionId, dealId, byBuyer);
    }

    @Step("Вводим код подтвержединия сделки покупателем для сессии {buyerSessionId} и сделке {dealId}")
    public VertisSafeDealDealView confirmCodeByBuyer(String buyerSessionId, String dealId, String code) {
        VertisSafeDealApiDealUpdateRequestByBuyer byBuyer =
                new VertisSafeDealApiDealUpdateRequestByBuyer().completeDealConfirm(
                        new VertisSafeDealApiDealUpdateRequestConfirmDealWithCode()
                                .code(code)
                );
        return updateDealByBuyer(buyerSessionId, dealId, byBuyer);
    }

    @Step("Получаем информацию про пользователя по сессии {sessionId}")
    public AutoApiUserResponse getUserInfo(String sessionId) {
        return api.user().getCurrentUser().reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .executeAs(validatedWith(shouldBe200OkJSON()));
    }

    @Step("Активируем РК для продукта {productAlias} у дилера {sessionId}")
    public void activateDealerCampaignProduct(String sessionId, String productAlias) {
        api.dealer().activateDealerProductCampaignAlias().reqSpec(defaultSpec())
                .productPath(productAlias)
                .xSessionIdHeader(sessionId)
                .executeAs(validatedWith(shouldBeSuccess()));
    }

    @Step("Деактивируем РК для продукта {productAlias} у дилера {sessionId}")
    public void deactivateDealerCampaignProduct(String sessionId, String productAlias) {
        api.dealer().deactivateDealerProductCampaign().reqSpec(defaultSpec())
                .productPath(productAlias)
                .xSessionIdHeader(sessionId)
                .executeAs(validatedWith(shouldBeSuccess()));
    }

    @Step("Получаем РК для продукта {productAlias} у дилера {sessionId}")
    public AutoApiBillingCampaignResponse getDealerCampaignProduct(String sessionId, String productAlias) {
        return api.dealer().getDealerProductCampaign().reqSpec(defaultSpec())
                .productPath(productAlias)
                .xSessionIdHeader(sessionId)
                .executeAs(validatedWith(shouldBe200OkJSON()));
    }

    @Step("Привязываем пользователя {userEmail} к дилеру {sessionId}")
    public void linkUserToDealer(String sessionId, Integer groupId, String userEmail) {
        api.dealer().linkUser().reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .groupQuery(groupId)
                .emailQuery(userEmail)
                .execute(validatedWith(shouldBe200OkJSON()));
    }

    @Step("Создаем карточку в гараже пользователю {sessionId}")
    public AutoApiVinGarageCreateCardResponse createGarageCard(String cardJson, String sessionId) {
        return api.garage().createCard().reqSpec(defaultSpec())
                .reqSpec(withJsonBody(cardJson))
                .xSessionIdHeader(sessionId)
                .executeAs(validatedWith(shouldBeSuccess()));
    }

    @Step("Создаем карточку в гараже пользователю {sessionId} по идентификатору {vinOrLp}")
    public AutoApiVinGarageCreateCardResponse createGarageCardByVinOrLp(String vinOrLp, String sessionId) {
        return api.garage().createCardFromIdentifier().reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .identifierPath(vinOrLp)
                .executeAs(validatedWith(shouldBeSuccess()));
    }

    @Step("Удаляем карточку гаража {cardId} пользователя {sessionId}")
    public void deleteGarageCard(String cardId, String sessionId) {
        api.garage().deleteCardGarage().reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .cardIdPath(cardId)
                .executeAs(validatedWith(shouldBeSuccess()));
    }

    @Step("Запрашиваем карточку гаража {cardId} пользователя {sessionId}")
    public AutoApiVinGarageGetCardResponse getGarageCard(String cardId, String sessionId) {
        return api.garage().getCard().reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .cardIdPath(cardId)
                .executeAs(validatedWith(shouldBeSuccess()));
    }

    @Step("Ждем пока карточка {recallsCardId} отзывных пользователя {sessionId} удалится")
    public void waitUntilRecallsCardDeleted(Long recallsCardId, String sessionId) {
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .pollDelay(3, SECONDS).pollInterval(POLL_INTERVAL, SECONDS).atMost(TIMEOUT, SECONDS).ignoreExceptions()
                .until(() -> {
                    AutoApiRecallsUserCardsResponse response = api.recalls().userCards().xSessionIdHeader(sessionId).reqSpec(defaultSpec())
                            .executeAs(validatedWith(shouldBeCode(200)));
                    return response.getCards() == null || response.getCards().stream().noneMatch(card -> Objects.equals(card.getCardId(), recallsCardId));
                }, equalTo(true));
    }

    @Step("Ждем пока карточка отзывных пользователя {sessionId} не создастся для карточки гаража {garageCardId}")
    public void waitUntilRecallsCardCreatedForGarageCard(String garageCardId, String sessionId) {
        given().conditionEvaluationListener(new AllureConditionEvaluationLogger())
                .pollDelay(3, SECONDS).pollInterval(POLL_INTERVAL, SECONDS).atMost(TIMEOUT, SECONDS).ignoreExceptions()
                .until(() -> api.garage().getCard().xSessionIdHeader(sessionId).cardIdPath(garageCardId).reqSpec(defaultSpec())
                        .executeAs(validatedWith(shouldBeCode(200))).getCard().getRecalls().getCard().getCardId(), notNullValue());
    }

    @Step("Удаляем карточку гаража {cardId} пользователя {sessionId} и ждём удаления карточки отзывных")
    public void deleteGarageCardAndWaitRecallsDeleted(String cardId, String sessionId) {
        waitUntilRecallsCardCreatedForGarageCard(cardId, sessionId);
        AutoApiVinGarageGetCardResponse cardWithRecalls = getGarageCard(cardId, sessionId);
        Long recallsCardId = cardWithRecalls.getCard().getRecalls().getCard().getCardId();
        assertNotNull("Id карточки отзывных не может быть null", recallsCardId);
        deleteGarageCard(cardId, sessionId);
        waitUntilRecallsCardDeleted(recallsCardId, sessionId);
    }

    @Step("Добавляем отзывную компанию пользователю {sessionId} по VIN {vin}")
    public AutoApiRecallsUserCardResponse addRecallUserCard(String sessionId, String vin) {
        return api.recalls().addUserCard()
                .reqSpec(defaultSpec())
                .vinOrLicensePlateQuery(vin)
                .xSessionIdHeader(sessionId)
                .executeAs(validatedWith(shouldBe200OkJSON()));
    }

    @Step("Создать заявку на подбор автомобиля для пользователя {sessionId}")
    public AutoApiMatchApplicationCreateResponse createMatchApplication(Account account, String sessionId) {
        RuAutoMatchMakerMatchApplication requestBody = new RuAutoMatchMakerMatchApplication()
                .userProposal(new RuAutoMatchMakerUserProposal().searchParams(
                        new AutoApiSearchSearchRequestParameters().addRidItem(213).geoRadius(0)
                                .addCatalogFilterItem(new AutoApiSearchCatalogFilter().mark("BMW").model("3ER"))))
                .userInfo(new RuAutoMatchMakerUserInfo().userId(Long.parseLong(account.getId()))
                        .phone(format("+%s", account.getLogin()))
                        .creditInfo(new RuAutoMatchMakerCreditInfo().isPossible(false)));

        return api.matchApplications().createMatchApplication().reqSpec(defaultSpec())
                .body(requestBody)
                .xSessionIdHeader(sessionId)
                .executeAs(validatedWith(shouldBe200OkJSON()));
    }

    @Step("Сохраняем настройки фида для {sessionId} в легковых {section}")
    public void saveFeedSettingsCars(String sessionId, SectionEnum section) {
        AutoApiFeedprocessorFeedFeedSettings requestBody = new AutoApiFeedprocessorFeedFeedSettings()
                .source("https://auto.ru/").deleteSale(false).leaveServices(false).leaveAddedImages(false).isActive(true);

        api.feeds().saveSettingsCars().reqSpec(defaultSpec())
                .sectionPath(section)
                .body(requestBody)
                .xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBeSuccess()));
    }

    @Step("Удалить настройки фида для {sessionId} в легковых {section}")
    public void deleteFeedSettingsCars(String sessionId, SectionEnum section) {
        api.feeds().deleteSettingsCars().reqSpec(defaultSpec())
                .sectionPath(section)
                .xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBeSuccess()));
    }

    @Step("Удалить настройки фида для {sessionId} в коммерческом {category} {section}")
    public void deleteFeedSettingsCommercial(String sessionId, String category, SectionEnum section) {
        api.feeds().deleteSettingsTrucks().reqSpec(defaultSpec())
                .trucksCategoryPath(category)
                .sectionPath(section)
                .xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBeSuccess()));
    }

    @Step("Получить настройки коллтрекинга для {sessionId}")
    public AutoApiCalltrackingSettingsResponse getCalltrackingSettings(String sessionId) {
        return api.calltracking().getSettingsCalltracking().reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .executeAs(validatedWith(shouldBe200OkJSON()));
    }

    @Step("Добавляем тэг {tag} от звонка {callId} под клиентом {sessionId}")
    public void addTagToCall(String sessionId, Long callId, String tag) {
        AutoApiCalltrackingAddTagRequest requestBody = new AutoApiCalltrackingAddTagRequest()
                .addTagsItem(new AutoCalltrackingCallTag().value(tag))
                .callId(callId);

        api.calltracking().addCallTag().reqSpec(defaultSpec())
                .body(requestBody)
                .xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBeSuccess()));
    }

    @Step("Отвязываем тэг {tag} от звонка {callId} под клиентом {sessionId}")
    public void removeTagFromCall(String sessionId, Long callId, String tag) {
        AutoApiCalltrackingUnbindTagRequest requestBody = new AutoApiCalltrackingUnbindTagRequest()
                .addTagsItem(new AutoCalltrackingCallTag().value(tag))
                .callId(callId);

        api.calltracking().unbindCallTag().reqSpec(defaultSpec())
                .body(requestBody)
                .xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBeSuccess()));
    }

    @Step("Добавляем текст поискового запроса в историю")
    public void addSearchTextToHistory(String sessionId) {
        AutoApiSearchCarsSearchRequestParameters carsParams = new AutoApiSearchCarsSearchRequestParameters()
                .addTransmissionItem(AUTOMATIC).addGearTypeItem(ALL_WHEEL_DRIVE).addEngineGroupItem(DIESEL);

        api.searchline().addHistorySearchline().reqSpec(defaultSpec())
                .categoryPath(CARS)
                .queryQuery("Дизель полный привод автомат")
                .body(new AutoApiSearchSearchRequestParameters().carsParams(carsParams))
                .xSessionIdHeader(sessionId)
                .execute(validatedWith(shouldBeSuccess()));
    }

    @Step("Получаем актуальные techparam_id и complectation_id из оффера в поиске")
    public GrouppingIdParams getActualTechParamAndConfiguration() {
        AutoApiOfferListingResponse resp = api.search().searchCars().reqSpec(defaultSpec())
                .stateQuery("NEW")
                .sortQuery("fresh_relevance_1-desc")
                .pageQuery(1)
                .pageSizeQuery(2)
                .executeAs(validatedWith(shouldBe200OkJSON()));

        String techParamId = resp.getOffers().get(0).getCarInfo().getTechParamId().toString();
        String complectationId = resp.getOffers().get(0).getCarInfo().getComplectationId().toString();

        return new GrouppingIdParams(techParamId, complectationId);
    }

    @Step("Авторизация пользователя в vox для app2app")
    public AutoApiAddVoxUserResponse voxSignUpUser(String sessionId, Boolean isApp2AppFeature) {
        VoxApi.SignUpUserOper request = api.vox().signUpUser().reqSpec(defaultSpec()).xSessionIdHeader(sessionId);
        VoxApi.SignUpUserOper requestWithFeature = isApp2AppFeature ? request.xFeaturesHeader(App2AppHeader) : request;
        return requestWithFeature.executeAs(validatedWith(shouldBe200OkJSON()));
    }

    @Step("Добавляем поиск в историю поисков пользователя")
    public void addSearchToSearchHistory(
            String sessionId,
            AutoApiSearchSearchRequestParameters searchParams,
            String category
    ) {
        api.search().addHistorySearch()
                .body(searchParams)
                .categoryPath(category)
                .reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .executeAs(validatedWith(shouldBe200OkJSON()));
    }

    @Step("Удаляем подписки пользователя {userId} сервиса Lenta")
    public void deleteLentaSubscriptions(String userId) {
        api.lenta().deleteSubscriptionLenta()
                .reqSpec(defaultSpec())
                .userIdQuery(userId)
                .executeAs(validatedWith(shouldBe200OkJSON()));
    }

    @Step("Получаем подписки пользователя {userId} сервиса Lenta, ожидаем ответ {expectedCode}")
    public AutoLentaSubscriptionResponse getLentaSubscriptions(String userId, int expectedCode) {
        return api.lenta().getSubscriptionLenta()
                .reqSpec(defaultSpec())
                .userIdQuery(userId)
                .executeAs(validatedWith(shouldBeCode(expectedCode)));
    }

    @Step("Получаем ленту пользователя {userId} в количестве {contentAmount}")
    public AutoLentaFeedResponse getFeed(String userId, int contentAmount) {
        return api
                .lenta()
                .getFeedLenta()
                .reqSpec(defaultSpec())
                .userIdQuery(userId)
                .contentAmountQuery(contentAmount)
                .executeAs(validatedWith(shouldBe200OkJSON()));
    }

    @Step("Помечаем ко {userId} в количестве {contentAmount}")
    public void markRead(String userId, AutoLentaReadRequest readRequest) {
        api.lenta().markReadLenta()
                .reqSpec(defaultSpec())
                .userIdQuery(userId)
                .body(readRequest)
                .executeAs(validatedWith(shouldBe200OkJSON()));
    }

    @Step("Создаем подписку пользователю {userId}")
    public void addLentaSubscription(String userId, AutoLentaSubscription subscription) {
        api.lenta().setSubscriptionLenta()
                .reqSpec(defaultSpec())
                .userIdQuery(userId)
                .body(subscription)
                .executeAs(validatedWith(shouldBe200OkJSON()));
    }

    @Step("Задаем настройку пользователю {userId}")
    public void setLentaSettings(String userId, AutoLentaSettings settings) {
        api.lenta().setSettingsLenta()
                .reqSpec(defaultSpec())
                .userIdQuery(userId)
                .body(settings)
                .executeAs(validatedWith(shouldBe200OkJSON()));
    }

    @Step("Добавляем теги к подписке пользователя {userId}")
    public AutoLentaSubscriptionResponse addLentaSubscriptionTags(String userId, AutoLentaSubscription subscription) {
        return api.lenta().addSubscriptionTagsLenta()
                .reqSpec(defaultSpec())
                .userIdQuery(userId)
                .body(subscription)
                .executeAs(validatedWith(shouldBe200OkJSON()));
    }

    @Step("Удаляем теги из подписки пользователя {userId}")
    public AutoLentaSubscriptionResponse deleteLentaSubscriptionTags(String userId, AutoLentaSubscription subscription) {
        return api.lenta().deleteSubscriptionTagsLenta()
                .reqSpec(defaultSpec())
                .userIdQuery(userId)
                .body(subscription)
                .executeAs(validatedWith(shouldBe200OkJSON()));
    }

    public String getUserIdFromSession(String sessionId) {
        return sessionId.split("\\|")[0];
    }

    private static Consumer<RequestSpecBuilder> ridParam() {
        return r -> r.addQueryParam("rid", "213");
    }

    private String getCatalogId(AutoApiCarInfo carInfo) {
        return String.format("%s__%s", carInfo.getConfigurationId(), carInfo.getTechParamId());
    }

    private File getFile(String path) {
        ClassLoader classLoader = getClass().getClassLoader();
        return new File(classLoader.getResource(path).getFile());
    }

    @Override
    protected void configure() {
    }


    public String getTamperForRequest(Object params, String deviceUid) {
        StringBuilder tamperStr
                = new StringBuilder();

        if (!deviceUid.isEmpty()) {
            tamperStr.append(deviceUid);
        }
        String salt = config.getTamperSalt();
        tamperStr.append(salt);

        if (params != null) {
            String serialized = new JSON().serialize(params);
            tamperStr.append(serialized.length());
        }
        return DigestUtils.md5Hex(tamperStr.toString());
    }

    @Step("Получаем все точки внешней панорамы {panoramaId}")
    public AutoApiExteriorPanoramaPoiResponse getAllExteriorPoi(String panoramaId) {
        return api.exteriorPanorama().getPoiExt()
                .reqSpec(defaultSpec())
                .panoramaIdPath(panoramaId)
                .executeAs(validatedWith(shouldBe200OkJSON()));
    }

    @Step("Добавляем точку на внешнюю панораму {panoramaId}")
    public AutoPanoramasExteriorPoi addExteriorPoi(String panoramaId, AutoPanoramasExteriorPoi poi) {
        return api.exteriorPanorama().createPoiExt()
                .reqSpec(defaultSpec())
                .body(poi)
                .panoramaIdPath(panoramaId)
                .executeAs(validatedWith(shouldBe200OkJSON()));
    }

    @Step("Удаляем точку {poiId} внешней панорамы {panoramaId}")
    public void deleteExteriorPoi(String poiId, String panoramaId) {
        api.exteriorPanorama().deletePoiExt()
                .reqSpec(defaultSpec())
                .panoramaIdPath(panoramaId)
                .poiIdPath(poiId)
                .executeAs(validatedWith(shouldBe200OkJSON()));
    }

    @Step("Удаляем все точки внешней панорамы {panoramaId}")
    public void deleteAllExteriorPoi(String panoramaId) {
        List<AutoPanoramasExteriorPoi> allPoi = getAllExteriorPoi(panoramaId).getPoi();

        if (allPoi != null && !allPoi.isEmpty()) {
            allPoi.stream().map(poi -> {
                assert poi.getPoint() != null;
                return poi.getPoint().getId();
            }).forEach(poiId -> deleteExteriorPoi(poiId, panoramaId));
        }
    }

    public AutoPanoramasExteriorPoi makeExteriorPoi(Double x, Double y, String text, Optional<String> imageLinkOpt) {

        AutoPanoramasExteriorPoiProperties properties = imageLinkOpt.map(link ->
                new AutoPanoramasExteriorPoiProperties()
                .text(new AutoPanoramasPoiTextProperty().text(text))
                .addImageItem(new AutoPanoramasPoiImageProperty().link(link))
        ).orElseGet(() -> new AutoPanoramasExteriorPoiProperties()
                .text(new AutoPanoramasPoiTextProperty().text(text)));

        return new AutoPanoramasExteriorPoi()
                .point(new AutoPanoramasPoiPoint()
                        .coordinates(new AutoPanoramasPoiRelativeCoordinates().x(x).y(y)))
                .frameInfo(new AutoPanoramasExteriorPoiFrameInfo()
                        .frame(0)
                        .aspectType(AutoPanoramasExteriorPoiFrameInfo.AspectTypeEnum.R4X3))
                .properties(properties);
    }

    @Step("Получаем все точки внутренней панорамы {panoramaId}")
    public AutoApiInteriorPanoramaPoiResponse getAllInteriorPoi(String panoramaId) {
        return api.interiorPanorama().getPoiInt()
                .reqSpec(defaultSpec())
                .panoramaIdPath(panoramaId)
                .executeAs(validatedWith(shouldBe200OkJSON()));
    }

    @Step("Добавляем точку на внутреннюю панораму {panoramaId}")
    public AutoPanoramasInteriorPoi addInteriorPoi(String panoramaId, AutoPanoramasInteriorPoi poi) {

        return api.interiorPanorama().createPoiInt()
                .reqSpec(defaultSpec())
                .body(poi)
                .panoramaIdPath(panoramaId)
                .executeAs(validatedWith(shouldBe200OkJSON()));
    }

    @Step("Удаляем точку {poiId} внутренней панорамы {panoramaId}")
    public void deleteInteriorPoi(String poiId, String panoramaId) {
        api.interiorPanorama().deletePoiInt()
                .reqSpec(defaultSpec())
                .panoramaIdPath(panoramaId)
                .poiIdPath(poiId)
                .executeAs(validatedWith(shouldBe200OkJSON()));
    }

    @Step("Удаляем все точки внутренней панорамы {panoramaId}")
    public void deleteAllInteriorPoi(String panoramaId) {
        List<AutoPanoramasInteriorPoi> allPoi = getAllInteriorPoi(panoramaId).getPoi();

        if (allPoi != null && !allPoi.isEmpty()) {
            allPoi.forEach(poi -> deleteInteriorPoi(poi.getPoint().getId(), panoramaId));
        }
    }

    @Step("Разрешаем просматривать объявления пользователя {sessionId}")
    public void setAllowOffersShow(String sessionId) {
        api.userProfile()
                .updateUserProfile()
                .body(new VertisPassportAutoruUserProfilePatch().allowOffersShow(true))
                .reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .executeAs(validatedWith(ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode(SC_OK)));
    }

    @Step("Возвращаем пользователя {sessionId}")
    public AutoApiUserResponse getUser(String sessionId, String userId) {
        return api.user()
                .getUser()
                .reqSpec(defaultSpec())
                .xSessionIdHeader(sessionId)
                .userIDPath(userId)
                .executeAs(validatedWith(ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode(SC_OK)));
    }

    public AutoPanoramasInteriorPoi makeInteriorPoi(Double x, Double y, String text, Optional<String> imageLinkOpt) {

        AutoPanoramasInteriorPoiProperties properties = imageLinkOpt.map(link ->
                new AutoPanoramasInteriorPoiProperties()
                        .text(new AutoPanoramasPoiTextProperty().text(text))
                        .addImageItem(new AutoPanoramasPoiImageProperty().link(link))
        ).orElseGet(() -> new AutoPanoramasInteriorPoiProperties()
                .text(new AutoPanoramasPoiTextProperty().text(text)));

        return new AutoPanoramasInteriorPoi()
                .point(new AutoPanoramasPoiPoint()
                        .coordinates(new AutoPanoramasPoiRelativeCoordinates().x(x).y(y)))
                .properties(properties);
    }
}
