package ru.yandex.arenda.steps;

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import io.qameta.allure.Step;
import io.qameta.allure.okhttp3.AllureOkHttp3;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import ru.yandex.arenda.account.FlatsKeeper;
import ru.yandex.arenda.api.service.Realty3RentApiService;
import ru.yandex.arenda.client.UnsafeOkHttpClient;
import ru.yandex.arenda.config.ArendaWebConfig;

import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.notNullValue;
import static ru.yandex.arenda.utils.UtilsWeb.getObjectFromJson;

public class RetrofitApiSteps {

    private static final String REALTY_3_API_HOST =
            "http://realty-gateway-api.vrts-slb.test.vertis.yandex.net";
    public static final String PATH_TO_POST_FLAT_DRAFT = "realty3api/put_flat_draft.json";
    public static final String PATH_TO_POST_FLAT_DRAFT_WITHOUT_PERSON = "realty3api/post_flat_draft_without_person.json";
    public static final String PATH_TO_ASSIGN_USER = "realty3api/assign_user.json";
    public static final String PATH_TO_UNASSIGN_USER = "realty3api/unassign_user.json";
    public static final String PATH_TO_POST_MODERATION_FLAT = "realty3api/post_moderation_draft.json";
    public static final String PATH_TO_POST_MODERATION_FLAT_CONTRACT = "realty3api/post_moderation_flat_contract.json";
    public static final String PATH_TO_POST_MODERATION_FLAT_QUESTIONNAIRE =
            "realty3api/post_moderation_flat_questionnaire.json";
    public static final String PATH_TO_OK_HOUSE_SERVICE = "realty3api/ok_house_service.json";
    public static final String PATH_TO_FLAT_CONTRACT_UPDATE_STATUS =
            "realty3api/post_moderation_flat_contract_update_status.json";
    public static final String PATH_TO_PATCH_USER = "realty3api/patch_user.json";
    public static final String PATH_TO_UPDATE_STATUS_OWNER = "realty3api/update_status_owner.json";
    public static final String ROLE_TENANT_CANDIDATE = "TENANT_CANDIDATE";

    private Retrofit.Builder retrofit;

    @Inject
    private ArendaWebConfig config;

    @Inject
    private FlatsKeeper flatsKeeper;

    public RetrofitApiSteps() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);


        OkHttpClient.Builder httpClient = UnsafeOkHttpClient.getUnsafeOkHttpClient().newBuilder();
        httpClient.addInterceptor(logging);
        httpClient.addInterceptor(new AllureOkHttp3());

        retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient.build());
    }

    @Step("Получаем инфо юзера по uid «{uid}»")
    public JsonObject getUserByUid(String uid) {
        return await().pollInterval(4, TimeUnit.SECONDS)
                .atMost(21, TimeUnit.SECONDS)
                .until(() -> retrofit.baseUrl(REALTY_3_API_HOST).build()
                        .create(Realty3RentApiService.class)
                        .getUserByUid(uid)
                        .execute().body(), notNullValue()).getAsJsonObject("response").getAsJsonObject("user");
    }

    @Step("Заполняем юзера «{uid}» информацией")
    public void patchUserByUid(String uid, JsonObject body) {
        await().pollInterval(4, TimeUnit.SECONDS)
                .atMost(21, TimeUnit.SECONDS)
                .until(() -> retrofit.baseUrl(REALTY_3_API_HOST).build()
                        .create(Realty3RentApiService.class)
                        .patchUserByUid(uid, body)
                        .execute().body(), notNullValue());
    }

    @Step("Создаем заполненного юзера")
    public void createUser(String uid) {
        getUserByUid(uid);
        patchUserByUid(uid, getObjectFromJson(JsonObject.class, PATH_TO_PATCH_USER));
    }

    @Step("Создаем заполненного юзера")
    public void createUserWithName(String uid, String name, String surname, String patronymic) {
        JsonObject person = new JsonObject();
        person.addProperty("name", name);
        person.addProperty("surname", surname);
        person.addProperty("patronymic", patronymic);
        getUserByUid(uid);
        JsonObject body = getObjectFromJson(JsonObject.class, PATH_TO_PATCH_USER);
        body.add("person", person);
        patchUserByUid(uid, body);
    }

    @Step("Получаем квартиры юзера по uid «{uid}»")
    public JsonObject getUserFlats(String uid) {
        return await().pollInterval(4, TimeUnit.SECONDS)
                .atMost(21, TimeUnit.SECONDS)
                .until(() -> retrofit.baseUrl(REALTY_3_API_HOST).build()
                        .create(Realty3RentApiService.class)
                        .getUserFlats(uid)
                        .execute().body(), notNullValue()).getAsJsonObject("response");
    }

    @Step("Получаем userUid")
    public String getUserId(String uid) {
        return getUserByUid(uid).getAsJsonPrimitive("userId").getAsString();
    }

    @Step("Получаем первую квартиру юзера по uid «{uid}»")
    public String getFirstUserFlatId(String uid) {
        return getUserFlats(uid).getAsJsonArray("flats").get(0).getAsJsonObject()
                .getAsJsonPrimitive("flatId").getAsString();
    }

    @Step("Создаем черновик квартиры для юзера с uid «{uid}», возвращаем flatId")
    public String postFlatDraft(String uid, JsonObject body) {
        String flatId = await().pollInterval(4, TimeUnit.SECONDS)
                .atMost(21, TimeUnit.SECONDS)
                .until(() -> retrofit.baseUrl(REALTY_3_API_HOST).build()
                        .create(Realty3RentApiService.class)
                        .putFlatDraft(uid, body)
                        .execute().body(), notNullValue()).getAsJsonObject("response").getAsJsonObject("flat")
                .getAsJsonPrimitive("flatId").getAsString();
        flatsKeeper.add(flatId);
        return flatId;
    }

    //не работает 401 Unauthorized
    @Step("Создаем квартиру через админку")
    public String postModerationFlat(JsonObject body) {
        return await().pollInterval(4, TimeUnit.SECONDS)
                .atMost(21, TimeUnit.SECONDS)
                .until(() -> retrofit.baseUrl(REALTY_3_API_HOST).build()
                        .create(Realty3RentApiService.class)
                        .postModerationFlats(body)
                        .execute().body(), notNullValue()).getAsJsonObject("response").getAsJsonObject("flat")
                .getAsJsonPrimitive("flatId").getAsString();
    }

    @Step("Добавляем договор в квартиру через админку")
    public String postModerationFlatContract(String flatId) {
        JsonObject body = getObjectFromJson(JsonObject.class, PATH_TO_POST_MODERATION_FLAT_CONTRACT);
        return await().pollInterval(4, TimeUnit.SECONDS)
                .atMost(21, TimeUnit.SECONDS)
                .until(() -> retrofit.baseUrl(REALTY_3_API_HOST).build()
                        .create(Realty3RentApiService.class)
                        .postModerationFlatsContract(flatId, body)
                        .execute().body(), notNullValue()).getAsJsonObject("response").getAsJsonObject("contract")
                .getAsJsonPrimitive("contractId").getAsString();
    }

    @Step("Принимаем условия ЖКХ через админку")
    public void okHouseService(String flatId) {
        await().pollInterval(4, TimeUnit.SECONDS)
                .atMost(21, TimeUnit.SECONDS)
                .until(() -> retrofit.baseUrl(REALTY_3_API_HOST).build()
                        .create(Realty3RentApiService.class)
                        .postHouseServices(flatId,
                                getObjectFromJson(JsonObject.class, PATH_TO_OK_HOUSE_SERVICE))
                        .execute().body(), notNullValue());
    }

    @Step("Обновляем статус договора в квартире через админку")
    public void flatContractUpdateStatus(String flatId, String contractId) {
        await().pollInterval(4, TimeUnit.SECONDS)
                .atMost(21, TimeUnit.SECONDS)
                .until(() -> retrofit.baseUrl(REALTY_3_API_HOST).build()
                        .create(Realty3RentApiService.class)
                        .flatContractUpdateStatus(flatId, contractId,
                                getObjectFromJson(JsonObject.class, PATH_TO_FLAT_CONTRACT_UPDATE_STATUS))
                        .execute().body(), notNullValue());
    }

    @Step("Отправляем смс для подтверждения квартиры «{flatId}» юзера «{uid}», возвращаем requestId для конфирмации")
    public String sendSms(String uid, String flatId) {
        return await().pollInterval(4, TimeUnit.SECONDS)
                .atMost(21, TimeUnit.SECONDS)
                .until(() -> retrofit.baseUrl(REALTY_3_API_HOST).build()
                        .create(Realty3RentApiService.class)
                        .confirmationCodeRequest(uid)
                        .execute().body(), notNullValue()).getAsJsonObject("response")
                .getAsJsonObject("sentSmsInfo").getAsJsonPrimitive("requestId").getAsString();
    }

    @Step("Подтверждаем квартиру «{flatId}» юзера «{uid}»")
    public void confirmFlat(String uid, JsonObject body) {
        await().pollInterval(4, TimeUnit.SECONDS)
                .atMost(21, TimeUnit.SECONDS)
                .until(() -> retrofit.baseUrl(REALTY_3_API_HOST).build()
                        .create(Realty3RentApiService.class)
                        .confirmationCodeSubmit(uid, body)
                        .execute().body(), notNullValue());
    }

    @Step("Заполняем анкету для квартиры «{flatId}»")
    public void postModerationFlatsQuestionnaire(String flatId) {
        await().pollInterval(4, TimeUnit.SECONDS)
                .atMost(21, TimeUnit.SECONDS)
                .until(() -> {
                    JsonObject body = retrofit.baseUrl(REALTY_3_API_HOST).build()
                            .create(Realty3RentApiService.class)
                            .postModerationFlatsQuestionnaire(flatId,
                                    getObjectFromJson(JsonObject.class, PATH_TO_POST_MODERATION_FLAT_QUESTIONNAIRE))
                            .execute().body();
                    return body;
                }, notNullValue());
    }

    @Step("Отправляем смс и подтверждаем")
    public String sendSmsAndConfirm(String uid, String flatId) {
        String requestId = sendSms(uid, flatId);
        JsonObject confirmBody = getObjectFromJson(JsonObject.class, "realty3api/confirm_sms_info.json");
        confirmBody.getAsJsonObject("confirmSmsInfo").addProperty("requestId", requestId);
        confirmFlat(uid, confirmBody);
        flatsKeeper.add(flatId);
        return flatId;
    }

    @Step("Создаем подтвержденную квартиру для юзера «{uid}»")
    public String createConfirmedFlat(String uid) {
        String flatId = postFlatDraft(uid, getObjectFromJson(JsonObject.class, PATH_TO_POST_FLAT_DRAFT));
        return sendSmsAndConfirm(uid, flatId);
    }

    @Step("Создаем подтвержденную квартиру для юзера «{uid}» без заполненных данных")
    public String createConfirmedFlatWithoutPerson(String uid) {
        String flatId = postFlatDraft(uid, getObjectFromJson(JsonObject.class, PATH_TO_POST_FLAT_DRAFT_WITHOUT_PERSON));
        return sendSmsAndConfirm(uid, flatId);
    }

    @Step("Привязываем юзера с userId=«{userId}» как {userRole}, к квартире «{flatId}»")
    public void assignToUser(String flatId, String userId, String userRole) {
        JsonObject body = getObjectFromJson(JsonObject.class, PATH_TO_ASSIGN_USER);
        body.addProperty("userId", userId);
        body.addProperty("userRole", userRole);
        await().pollInterval(4, TimeUnit.SECONDS)
                .atMost(21, TimeUnit.SECONDS)
                .until(() -> retrofit.baseUrl(REALTY_3_API_HOST).build()
                        .create(Realty3RentApiService.class)
                        .postModerationFlatsAssignToUser(flatId, body)
                        .execute().body(), notNullValue());
    }

    @Step("Отвязываем юзера с userId=«{userId}», от квартиры «{flatId}»")
    public void unassignUser(String flatId, String userId) {
        JsonObject body = getObjectFromJson(JsonObject.class, PATH_TO_UNASSIGN_USER);
        body.addProperty("userId", userId);
        await().pollInterval(4, TimeUnit.SECONDS)
                .atMost(21, TimeUnit.SECONDS)
                .until(() -> retrofit.baseUrl(REALTY_3_API_HOST).build()
                        .create(Realty3RentApiService.class)
                        .postModerationFlatsUnAssignUser(flatId, body)
                        .execute().body(), notNullValue());
    }

    @Step("Удаляем юзера с userId=«{userId}»")
    public void deleteUser(String userId) {
        await().pollInterval(4, TimeUnit.SECONDS)
                .atMost(21, TimeUnit.SECONDS)
                .until(() -> retrofit.baseUrl(REALTY_3_API_HOST).build()
                        .create(Realty3RentApiService.class)
                        .deleteUser(userId)
                        .execute().body(), notNullValue());
    }

    @Step("Удаляем квартиру с flatId=«{flatId}»")
    public void deleteFlat(String flatId) {
        await().pollInterval(4, TimeUnit.SECONDS)
                .atMost(21, TimeUnit.SECONDS)
                .until(() -> retrofit.baseUrl(REALTY_3_API_HOST).build()
                        .create(Realty3RentApiService.class)
                        .deleteFlat(flatId)
                        .execute().body(), notNullValue());
    }

    @Step("Получаем OwnerRequestID из «owner-requests» для flatId=«{flatId}»")
    public String getOwnerRequestID(String flatId) {
        return await().pollInterval(4, TimeUnit.SECONDS)
                .atMost(21, TimeUnit.SECONDS)
                .until(() -> retrofit.baseUrl(REALTY_3_API_HOST).build()
                        .create(Realty3RentApiService.class)
                        .getFlatOwnerRequests(flatId)
                        .execute().body(), notNullValue()).getAsJsonArray("response").get(0).getAsJsonObject()
                .getAsJsonPrimitive("ownerRequestId").getAsString();
    }

    @Step("Получаем OwnerRequestID из «owner-requests» для flatId=«{flatId}»")
    public void updateStatusInProgress(String flatId, String ownerRequestId) {
        await().pollInterval(4, TimeUnit.SECONDS)
                .atMost(21, TimeUnit.SECONDS)
                .until(() -> retrofit.baseUrl(REALTY_3_API_HOST).build()
                        .create(Realty3RentApiService.class)
                        .postFlatOwnerRequestIdUpdateStatus(flatId, ownerRequestId,
                                getObjectFromJson(JsonObject.class, PATH_TO_UPDATE_STATUS_OWNER))
                        .execute().body(), notNullValue());
    }

    public String getNameFormatted(String uid) {
        JsonObject person = getUserByUid(uid).getAsJsonObject("person");
        return format("%s %s %s",
                person.getAsJsonPrimitive("surname").getAsString(),
                person.getAsJsonPrimitive("name").getAsString(),
                person.getAsJsonPrimitive("patronymic").getAsString());
    }
}
