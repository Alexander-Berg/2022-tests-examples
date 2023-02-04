package ru.auto.tests.realtyapi.adaptor.rent;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.qameta.allure.Step;
import org.apache.commons.lang3.RandomStringUtils;
import ru.auto.tests.commons.util.Utils;
import ru.auto.tests.realtyapi.utils.UtilsRealtyApi;
import ru.auto.tests.realtyapi.v2.ApiClient;
import ru.auto.tests.realtyapi.v2.model.RealtyPersonFullName;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiAddUserToRoommateGroupRequest;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiAssignUserToFlatRequest;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiAssignUserToFlatResponse;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiContractModeratedFields;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiCreateFlatShowingsRequest;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiCreateFlatShowingsRequest.ShowingTypeEnum;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiFlatQuestionnaire;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiFlatQuestionnaireFlat;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiFlatQuestionnaireFlat.FlatTypeEnum;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiFlatQuestionnairePayments;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiFlatQuestionnairePayments.CommissionValueEnum;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiFlatShowing;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiGetFlatDraftResponse;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiGetFlatResponse;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiPassportData;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiPatchRentUserRequest;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiPostModerationContractRequest;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiPostModerationContractSuccessResponse;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiRentContractOwnerInfo;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiRentContractTenantInfo;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiRentUser;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiSendSmsForFlatRequest;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiSetFlatShowingStatusRequest;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiSetFlatShowingStatusRequestStatus;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiUpdateContractStatusRequest;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiUpdateContractStatusSuccessResponse;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiUpdateFlatDraftRequest;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiUpdateFlatDraftResponse;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiUpdateHouseServiceSettingsStatusRequest;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.util.Utils.getRandomShortInt;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v1.ResponseSpecBuilders.validatedWith;

@Singleton
public class RentApiAdaptor {
    @Inject
    private ApiClient api;

    @Step("Получение (создание, если не существует) пользователя")
    public RealtyRentApiRentUser getOrCreateRentUser(String token, String uid) {
        return api
                .rent()
                .getUser()
                .userPath(uid)
                .reqSpec(authSpec()).authorizationHeader(token)
                .executeAs(validatedWith(shouldBe200Ok()))
                .getResponse()
                .getUser();
    }

    @Step("Обновление телефона пользователя")
    public void updateUserPhone(String token, String uid, String phone) {
        RealtyRentApiPatchRentUserRequest request = new RealtyRentApiPatchRentUserRequest()
                .phone(phone);
        api.rent()
                .patchUser()
                .userPath(uid)
                .body(request)
                .reqSpec(authSpec())
                .authorizationHeader(token)
                .executeAs(validatedWith(shouldBe200Ok()));
    }

    @Step("Создание пользователя в Пальме")
    public RealtyRentApiRentUser createRentPalmaUser(String token, String uid) {
        RealtyRentApiPassportData passportData = new RealtyRentApiPassportData()
                .passportNumber("001122")
                .passportSeries("3344")
                .passportIssueDate(OffsetDateTime.now())
                .birthday(OffsetDateTime.now())
                .passportIssuedBy("Never")
                .departmentCode("555")
                .birthPlace("Internet")
                .registrationAddress("Address");
        RealtyRentApiPatchRentUserRequest request = new RealtyRentApiPatchRentUserRequest()
                .passportData(passportData);
        return api
                .rent()
                .patchUser()
                .userPath(uid)
                .reqSpec(authSpec())
                .authorizationHeader(token)
                .body(request)
                .executeAs(validatedWith(shouldBe200Ok()))
                .getResponse()
                .getUser();
    }

    @Step("Удаление пользователя")
    public Object deleteRentUser(String token, String userId) {
        return api
                .rentModeration()
                .deleteUser()
                .userIdPath(userId)
                .reqSpec(authSpec()).authorizationHeader(token)
                .xUidHeader(token)
                .executeAs(validatedWith(shouldBe200Ok()))
                .getResponse();
    }


    @Step("Получение черновика квартиры")
    public RealtyRentApiGetFlatDraftResponse getFlatDraft(String token, String uid) {
        return api
                .rent()
                .getFlatDraft()
                .userPath(uid)
                .reqSpec(authSpec()).authorizationHeader(token)
                .executeAs(validatedWith(shouldBe200Ok()))
                .getResponse();
    }

    @Step("Получение квартиры по flatId")
    public RealtyRentApiGetFlatResponse getFlat(String token, String uid, String flatId) {
        return api
                .rent()
                .getFlat()
                .userPath(uid)
                .flatIdPath(flatId)
                .reqSpec(authSpec()).authorizationHeader(token)
                .executeAs(validatedWith(shouldBe200Ok()))
                .getResponse();
    }

    @Step("Удаление квартиры по flatId")
    public Object deleteFlat(String token, String flatId) {
        return api
                .rentModeration()
                .deleteFlat()
                .flatIdPath(flatId)
                .reqSpec(authSpec()).authorizationHeader(token)
                .xUidHeader(token)
                .executeAs(validatedWith(shouldBe200Ok()))
                .getResponse();
    }


    @Step("Создание рандомного черновика квартиры")
    public RealtyRentApiUpdateFlatDraftResponse createFlatDraft(String token, String uid) {
        getOrCreateRentUser(token, uid);

        String insideAddress = addressesInsidePolygon.get(0);
        RealtyRentApiUpdateFlatDraftRequest request = buildRandomUpdateFlatDraftRequest(insideAddress);

        return api
                .rent()
                .upsertFlatDraft()
                .userPath(uid)
                .reqSpec(authSpec()).authorizationHeader(token)
                .xUidHeader(token)
                .body(request)
                .executeAs(validatedWith(shouldBe200Ok()))
                .getResponse();
    }

    @Step("Создание анкеты квартиры")
    public RealtyRentApiFlatQuestionnaire createFlatQuestionnaire(String token, String flatId) {
        RealtyRentApiFlatQuestionnaireFlat flat = new RealtyRentApiFlatQuestionnaireFlat()
                .flatType(FlatTypeEnum.FLAT);
        RealtyRentApiFlatQuestionnairePayments payments = new RealtyRentApiFlatQuestionnairePayments()
                .commissionValue(CommissionValueEnum.FIVE)
                .rentalValue(35000L * 100);
        RealtyRentApiFlatQuestionnaire request = new RealtyRentApiFlatQuestionnaire()
                .flat(flat)
                .payments(payments);

        return api.rentModeration()
                .createFlatQuestionnaire()
                .flatIdPath(flatId)
                .reqSpec(authSpec())
                .authorizationHeader(token)
                .xUidHeader(token)
                .body(request)
                .executeAs(validatedWith(shouldBe200Ok()))
                .getResponse();
    }

    @Step("Создание показа квартиры")
    public RealtyRentApiFlatShowing createFlatShowing(String token, String flatId, String phone) {
        RealtyRentApiCreateFlatShowingsRequest request = new RealtyRentApiCreateFlatShowingsRequest()
                .tenantName("Тестовый Пользователь")
                .tenantPhone(phone)
                .showingType(ShowingTypeEnum.ONLINE);
        return api.rentModeration()
                .createFlatShowings()
                .flatIdPath(flatId)
                .reqSpec(authSpec())
                .authorizationHeader(token)
                .xUidHeader(token)
                .body(request)
                .executeAs(validatedWith(shouldBe200Ok()))
                .getResponse()
                .getFlatShowing();
    }

    @Step("Добавление жильца в показ")
    public void addRoommate(String token, String showingId, String userId) {
        RealtyRentApiAddUserToRoommateGroupRequest request = new RealtyRentApiAddUserToRoommateGroupRequest()
                .userId(userId);
        api.rentRoommatesModeration()
                .addUserToRoommateGroup()
                .showingIdPath(showingId)
                .reqSpec(authSpec())
                .authorizationHeader(token)
                .xUidHeader(token)
                .body(request)
                .executeAs(validatedWith(shouldBe200Ok()));
    }

    @Step("Смена статуса показа <На подписании>")
    public void setFlatShowingStatusToSigningAppointed(String token, String flatId, String showingId) {
        RealtyRentApiSetFlatShowingStatusRequest request = new RealtyRentApiSetFlatShowingStatusRequest()
                .signingAppointed(new RealtyRentApiSetFlatShowingStatusRequestStatus());
        api.rentModeration()
                .setFlatShowingStatus()
                .flatIdPath(flatId)
                .showingIdPath(showingId)
                .reqSpec(authSpec())
                .authorizationHeader(token)
                .xUidHeader(token)
                .body(request)
                .executeAs(validatedWith(shouldBe200Ok()));
    }

    @Step("Привязывание пользователя к квартире как кандидата в жильцы")
    public RealtyRentApiAssignUserToFlatResponse assignTenantCandidateToFlat(String token, String flatId,
                                                                             String userId) {
        RealtyRentApiAssignUserToFlatRequest request = new RealtyRentApiAssignUserToFlatRequest()
                .userId(userId)
                .userRole(RealtyRentApiAssignUserToFlatRequest.UserRoleEnum.TENANT_CANDIDATE);
        return api
                .rentModeration()
                .assignFlatToUser()
                .flatIdPath(flatId)
                .reqSpec(authSpec()).authorizationHeader(token)
                .xUidHeader(token)
                .body(request)
                .executeAs(validatedWith(shouldBe200Ok()))
                .getResponse();
    }

    @Step("Подтверждение квартиры по смс для создания заявления собственника")
    public void sendSmsConfirmation(String token, String uid) {
        getOrCreateRentUser(token, uid);

        RealtyRentApiSendSmsForFlatRequest request = buildRealtyRentApiSendSmsForFlatRequest();
        api
                .rent()
                .sendSmsForFlat()
                .userPath(uid)
                .reqSpec(authSpec()).authorizationHeader(token)
                .body(request)
                .execute(validatedWith(shouldBe200Ok()));
    }

    @Step("Создание контракта для квартиры")
    public RealtyRentApiPostModerationContractSuccessResponse createContract(
            String token,
            String flatId,
            RealtyRentApiPostModerationContractRequest request
    ) {
        return api
                .rentModeration()
                .createContract()
                .flatIdPath(flatId)
                .reqSpec(authSpec()).authorizationHeader(token)
                .xUidHeader(token)
                .body(request)
                .executeAs(validatedWith(shouldBe200Ok()))
                .getResponse();
    }

    public RealtyRentApiUpdateContractStatusSuccessResponse updateContractStatus(
            String token,
            String flatId,
            String contractId,
            RealtyRentApiUpdateContractStatusRequest request) {
        return api
                .rentModeration()
                .updateContractStatus()
                .flatIdPath(flatId)
                .contractIdPath(contractId)
                .reqSpec(authSpec()).authorizationHeader(token)
                .xUidHeader(token)
                .body(request)
                .executeAs(validatedWith(shouldBe200Ok()))
                .getResponse();
    }

    @Step("Отправка договора собственнику")
    public RealtyRentApiUpdateContractStatusSuccessResponse sendContractToOwner(String token, String flatId, String contractId) {
        return updateContractStatus(token, flatId, contractId, buildSendContractToOwnerRequest());
    }

    @Step("Подписание договора собственником")
    public RealtyRentApiUpdateContractStatusSuccessResponse signContractByOwner(String token, String flatId, String contractId) {
        return updateContractStatus(token, flatId, contractId, buildSignContractByOwnerRequest());
    }

    @Step("Подписание договора жильцом")
    public RealtyRentApiUpdateContractStatusSuccessResponse signContractByTenant(String token, String flatId, String contractId) {
        return updateContractStatus(token, flatId, contractId, buildSignContractByTenantRequest());
    }

    @Step("Подтверждение настроек ЖКХ собственником")
    public void fillHouseServiceSettingsByOwner(String token, String uid, String flatId) {
        RealtyRentApiUpdateHouseServiceSettingsStatusRequest request = buildFilledByOwnerRequest();
        api
                .rentHouseService()
                .updateFlatHouseServiceStatus()
                .userPath(uid)
                .flatIdPath(flatId)
                .reqSpec(authSpec()).authorizationHeader(token)
                .xUidHeader(token)
                .body(request)
                .execute(validatedWith(shouldBe200Ok()));
    }

    @Step("Подтверждение настроек ЖКХ жильцом")
    public void confirmHouseServiceSettingsByTenant(String token, String uid, String flatId) {
        RealtyRentApiUpdateHouseServiceSettingsStatusRequest request = buildConfirmedByTenantRequest();
        api
                .rentHouseService()
                .updateFlatHouseServiceStatus()
                .userPath(uid)
                .flatIdPath(flatId)
                .reqSpec(authSpec()).authorizationHeader(token)
                .xUidHeader(token)
                .body(request)
                .execute(validatedWith(shouldBe200Ok()));
    }

    public RealtyPersonFullName buildRandomPerson() {
        return new RealtyPersonFullName()
                .name(NAMES.get(ThreadLocalRandom.current().nextInt(NAMES.size())))
                .surname(SURNAMES.get(ThreadLocalRandom.current().nextInt(SURNAMES.size())))
                .patronymic("ибн " + NAMES.get(ThreadLocalRandom.current().nextInt(NAMES.size())));
    }

    public RealtyRentApiUpdateFlatDraftRequest buildRandomUpdateFlatDraftRequest(String address) {
        return new RealtyRentApiUpdateFlatDraftRequest()
                .address(address)
                .flatNumber(String.valueOf(getRandomShortInt()))
                .phone(UtilsRealtyApi.getAutoTestsPhone())
                .email("qwerty@yandex.ru") //TODO HzREALTYBACK-5901 generate random email
                .person(buildRandomPerson());
    }

    public RealtyRentApiSendSmsForFlatRequest buildRealtyRentApiSendSmsForFlatRequest() {
        return new RealtyRentApiSendSmsForFlatRequest()
                .admitadUid(getRandomString())
                .yandexUserUid(getRandomString());
    }

    public RealtyRentApiRentContractOwnerInfo buildOwnerInfo() {
        return new RealtyRentApiRentContractOwnerInfo()
                .inn(RandomStringUtils.randomNumeric(12))
                .person(buildRandomPerson())
                .phone(Utils.getRandomPhone())
                .email(Utils.getRandomEmail());
    }

    public RealtyRentApiRentContractTenantInfo buildTenantInfo(String tenantId) {
        return new RealtyRentApiRentContractTenantInfo()
                .person(buildRandomPerson())
                .phone(Utils.getRandomPhone())
                .email(Utils.getRandomEmail())
                .tenantUserId(tenantId);
    }

    public RealtyRentApiPostModerationContractRequest buildContract(String tenantId) {
        return buildContract(buildOwnerInfo(), buildTenantInfo(tenantId));
    }

    public RealtyRentApiPostModerationContractRequest buildContract(RealtyRentApiRentContractOwnerInfo ownerInfo,
                                                                    RealtyRentApiRentContractTenantInfo tenantInfo) {
        OffsetDateTime yesterday = OffsetDateTime.now().minusDays(1);
        RealtyRentApiContractModeratedFields contract = new RealtyRentApiContractModeratedFields()
                .rentStartDate(yesterday)
                .tenantCheckInDate(yesterday)
                .ownerInfo(ownerInfo)
                .tenantInfo(tenantInfo)
                .paymentDayOfMonth(10)
                .contractClass(RealtyRentApiContractModeratedFields.ContractClassEnum.OWNERSHIP)
                .ownerStatus(RealtyRentApiContractModeratedFields.OwnerStatusEnum.NATURAL);
        return new RealtyRentApiPostModerationContractRequest().contract(contract);
    }

    public RealtyRentApiUpdateContractStatusRequest buildContractActivateRequest() {
        RealtyRentApiUpdateContractStatusRequest request = new RealtyRentApiUpdateContractStatusRequest();
        request.setActivate(new Object());
        return request;
    }

    public RealtyRentApiUpdateContractStatusRequest buildSendContractToOwnerRequest() {
        RealtyRentApiUpdateContractStatusRequest request = new RealtyRentApiUpdateContractStatusRequest();
        request.sendToOwner(new Object());
        return request;
    }

    public RealtyRentApiUpdateContractStatusRequest buildSignContractByOwnerRequest() {
        RealtyRentApiUpdateContractStatusRequest request = new RealtyRentApiUpdateContractStatusRequest();
        request.signByOwner(new Object());
        return request;
    }

    public RealtyRentApiUpdateContractStatusRequest buildSignContractByTenantRequest() {
        RealtyRentApiUpdateContractStatusRequest request = new RealtyRentApiUpdateContractStatusRequest();
        request.signByTenant(new Object());
        return request;
    }

    public RealtyRentApiUpdateContractStatusRequest buildInsurancePolicyRequest() {
        return new RealtyRentApiUpdateContractStatusRequest().setInsurancePolicy(new Object());
    }

    public RealtyRentApiUpdateHouseServiceSettingsStatusRequest buildFilledByOwnerRequest() {
        return new RealtyRentApiUpdateHouseServiceSettingsStatusRequest().setFilledByOwner(new Object());
    }

    public RealtyRentApiUpdateHouseServiceSettingsStatusRequest buildConfirmedByTenantRequest() {
        return new RealtyRentApiUpdateHouseServiceSettingsStatusRequest().setConfirmedByTenant(new Object());
    }

    public List<String> addressesInsidePolygon = Arrays.asList(
            "Россия, Санкт-Петербург, Ленинский проспект, 140",
            "Россия, Москва, Салтыковская улица, 1"
    );

    public List<String> addressesOutsidePolygon = Arrays.asList(
            "Россия, Республика Коми, муниципальный район Сосногорск, посёлок городского типа Нижний Одес, улица Ленина, 16",
            "Россия, Республика Саха (Якутия), муниципальный район Булунский улус, посёлок городского типа Тикси, улица 50 лет Северного Морского Пути, 23"
    );

    private static List<String> SURNAMES = Arrays.asList(
            "Смирнов",
            "Иванов",
            "Кузнецов",
            "Соколов",
            "Попов",
            "Лебедев",
            "Козлов",
            "Новиков",
            "Морозов",
            "Петров",
            "Волков",
            "Соловьёв",
            "Васильев",
            "Зайцев",
            "Павлов",
            "Семёнов",
            "Голубев",
            "Виноградов",
            "Богданов",
            "Воробьёв",
            "Фёдоров",
            "Михайлов",
            "Беляев",
            "Тарасов",
            "Белов",
            "Комаров",
            "Орлов",
            "Киселёв",
            "Макаров",
            "Андреев",
            "Ковалёв",
            "Ильин",
            "Гусев",
            "Титов",
            "Кузьмин",
            "Кудрявцев",
            "Баранов",
            "Куликов",
            "Алексеев",
            "Степанов",
            "Яковлев",
            "Сорокин",
            "Сергеев",
            "Романов",
            "Захаров",
            "Борисов",
            "Королёв",
            "Герасимов",
            "Пономарёв",
            "Григорьев",
            "Лазарев",
            "Медведев",
            "Ершов",
            "Никитин",
            "Соболев",
            "Рябов",
            "Поляков",
            "Цветков",
            "Данилов",
            "Жуков",
            "Фролов",
            "Журавлёв",
            "Николаев",
            "Крылов",
            "Максимов",
            "Сидоров",
            "Осипов",
            "Белоусов",
            "Федотов",
            "Дорофеев",
            "Егоров",
            "Матвеев",
            "Бобров",
            "Дмитриев",
            "Калинин",
            "Анисимов",
            "Петухов",
            "Антонов",
            "Тимофеев",
            "Никифоров",
            "Веселов",
            "Филиппов",
            "Марков",
            "Большаков",
            "Суханов",
            "Миронов",
            "Ширяев",
            "Александров",
            "Коновалов",
            "Шестаков",
            "Казаков",
            "Ефимов",
            "Денисов",
            "Громов",
            "Фомин",
            "Давыдов",
            "Мельников",
            "Щербаков",
            "Блинов",
            "Колесников",
            "Карпов",
            "Афанасьев",
            "Власов",
            "Маслов",
            "Исаков",
            "Тихонов",
            "Аксёнов",
            "Гаврилов",
            "Родионов",
            "Котов",
            "Горбунов",
            "Кудряшов",
            "Быков",
            "Зуев",
            "Третьяков",
            "Савельев",
            "Панов",
            "Рыбаков",
            "Суворов",
            "Абрамов",
            "Воронов",
            "Мухин",
            "Архипов",
            "Трофимов",
            "Мартынов",
            "Емельянов",
            "Горшков",
            "Чернов",
            "Овчинников",
            "Селезнёв",
            "Панфилов",
            "Копылов",
            "Михеев",
            "Галкин",
            "Назаров",
            "Лобанов",
            "Лукин",
            "Беляков",
            "Потапов",
            "Некрасов",
            "Хохлов",
            "Жданов",
            "Наумов",
            "Шилов",
            "Воронцов",
            "Ермаков",
            "Дроздов",
            "Игнатьев",
            "Савин",
            "Логинов",
            "Сафонов",
            "Капустин",
            "Кириллов",
            "Моисеев",
            "Елисеев",
            "Кошелев",
            "Костин",
            "Горбачёв",
            "Орехов",
            "Ефремов",
            "Исаев",
            "Евдокимов",
            "Калашников",
            "Кабанов",
            "Носков",
            "Юдин",
            "Кулагин",
            "Лапин",
            "Прохоров",
            "Нестеров",
            "Харитонов",
            "Агафонов",
            "Муравьёв",
            "Ларионов",
            "Федосеев",
            "Зимин",
            "Пахомов",
            "Шубин",
            "Игнатов",
            "Филатов",
            "Крюков",
            "Рогов",
            "Кулаков",
            "Терентьев",
            "Молчанов",
            "Владимиров",
            "Артемьев",
            "Гурьев",
            "Зиновьев",
            "Гришин",
            "Кононов",
            "Дементьев",
            "Ситников",
            "Симонов",
            "Мишин",
            "Фадеев",
            "Комиссаров",
            "Мамонтов",
            "Носов",
            "Гуляев",
            "Шаров",
            "Устинов",
            "Вишняков",
            "Евсеев",
            "Лаврентьев",
            "Брагин",
            "Константинов",
            "Корнилов",
            "Авдеев",
            "Зыков",
            "Бирюков",
            "Шарапов",
            "Никонов",
            "Щукин",
            "Дьячков",
            "Одинцов",
            "Сазонов",
            "Якушев",
            "Красильников",
            "Гордеев",
            "Самойлов",
            "Князев",
            "Беспалов",
            "Уваров",
            "Шашков",
            "Бобылёв",
            "Доронин",
            "Белозёров",
            "Рожков",
            "Самсонов",
            "Мясников",
            "Лихачёв",
            "Буров",
            "Сысоев",
            "Фомичёв",
            "Русаков",
            "Стрелков",
            "Гущин",
            "Тетерин",
            "Колобов",
            "Субботин",
            "Фокин",
            "Блохин",
            "Селиверстов",
            "Пестов",
            "Кондратьев",
            "Силин",
            "Меркушев",
            "Лыткин",
            "Туров"
    );
    
    private static List<String> NAMES = Arrays.asList(
            "Август",
            "Адам",
            "Адриан",
            "Аким",
            "Александр",
            "Алексей",
            "Ананий",
            "Анатолий",
            "Андрей",
            "Антон",
            "Антонин",
            "Аполлон",
            "Аркадий",
            "Арсений",
            "Артём",
            "Богдан",
            "Болеслав",
            "Борис",
            "Бронислав",
            "Вадим",
            "Валентин",
            "Валериан",
            "Валерий",
            "Василий",
            "Вениамин",
            "Викентий",
            "Виктор",
            "Виль",
            "Виталий",
            "Витольд",
            "Владимир",
            "Владислав",
            "Владлен",
            "Всеволод",
            "Вячеслав",
            "Геннадий",
            "Георгий",
            "Герасим",
            "Герман",
            "Глеб",
            "Гордей",
            "Григорий",
            "Давид",
            "Дан",
            "Данила",
            "Денис",
            "Дмитрий",
            "Добрыня",
            "Донат",
            "Евгений",
            "Егор",
            "Ефим",
            "Захар",
            "Иван",
            "Игнат",
            "Игорь",
            "Илларион",
            "Илья",
            "Иннокентий",
            "Иосиф",
            "Ираклий",
            "Кирилл",
            "Клим",
            "Константин",
            "Кузьма",
            "Лаврентий",
            "Лев",
            "Леонид",
            "Макар",
            "Максим",
            "Марк",
            "Матвей",
            "Милан",
            "Мирослав",
            "Михаил",
            "Назар",
            "Никита",
            "Никодим",
            "Николай",
            "Олег",
            "Павел",
            "Платон",
            "Прохор",
            "Пётр",
            "Радислав",
            "Рафаил",
            "Родион",
            "Роман",
            "Ростислав",
            "Руслан",
            "Савва",
            "Святослав",
            "Семён",
            "Сергей",
            "Станислав",
            "Степан",
            "Тарас",
            "Тимофей",
            "Тит",
            "Феликс",
            "Филипп",
            "Фёдор",
            "Юлиан",
            "Юлий",
            "Юрий",
            "Яков",
            "Ян",
            "Ярослав"
    );

}
