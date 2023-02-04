package ru.auto.tests.realtyapi.v2.rent.flats;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.junit4.DisplayName;
import lombok.extern.log4j.Log4j;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.realtyapi.RealtyTestApiModule;
import ru.auto.tests.realtyapi.adaptor.rent.RentApiAdaptor;
import ru.auto.tests.realtyapi.oauth.OAuth;
import ru.auto.tests.realtyapi.v2.ApiClient;
import ru.auto.tests.realtyapi.v2.model.RealtyApiApiSendSmsForFlatConfirmationErrorResponse;
import ru.auto.tests.realtyapi.v2.model.RealtyApiBasicSmsConfirmationValidationError;
import ru.auto.tests.realtyapi.v2.model.RealtyApiBasicSmsConfirmationValidationErrorDataValidationMessage;
import ru.auto.tests.realtyapi.v2.model.RealtyApiBasicSmsConfirmationValidationErrorResponse;
import ru.auto.tests.realtyapi.v2.model.RealtyApiError;
import ru.auto.tests.realtyapi.v2.model.RealtyApiErrorResponse;
import ru.auto.tests.realtyapi.v2.model.RealtyApiFlatValidationError;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiConfirmFlatRequest;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiConfirmSmsInfo;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiFlat;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiFlatValidationErrorDataFlatValidationMessage;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiRentUser;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiSendSmsForFlatRequest;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiSentSmsInfo;
import ru.auto.tests.realtyapi.v2.rent.AbstractHandlerTest;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v2.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.realtyapi.v2.ResponseSpecBuilders.validatedWith;

@Title("/rent/draft")
@RunWith(GuiceTestRunner.class)
@Log4j
@GuiceModules(RealtyTestApiModule.class)
public class DraftConfirmationTest extends AbstractHandlerTest {

    private RealtyRentApiFlat draft1;
    private RealtyRentApiRentUser rentUser1;

    private RealtyRentApiFlat draft2;
    private RealtyRentApiRentUser rentUser2;

    @Inject
    private ApiClient apiV2;

    @Inject
    private OAuth oAuth;

    @Inject
    private RentApiAdaptor rentApiAdaptor;

    @Inject
    private AccountManager am;

    @Before
    public void before() {
        createAccounts();
    }

    @After
    public void after() {
        flatToDelete.add(draft1);
        flatToDelete.add(draft2);
        usersToDelete.add(rentUser1);
        usersToDelete.add(rentUser2);
    }

    @AfterClass
    public static void cleaning() {
        deleteDrafts(token1);
        deleteUsers(token1);
        deleteAccounts();
    }

    @Test
    @DisplayName("Успешная отправка SMS")
    public void sendSmsForGoodDraftTest() {
        rentUser1 = rentApiAdaptor.getOrCreateRentUser(token1, uid1);

        String goodAddress = rentApiAdaptor.addressesInsidePolygon.get(0);

        draft1 = apiV2
                .rent()
                .upsertFlatDraft()
                .userPath(uid1)
                .reqSpec(authSpec()).authorizationHeader(token1)
                .xUidHeader(uid1)
                .body(rentApiAdaptor.buildRandomUpdateFlatDraftRequest(goodAddress))
                .executeAs(validatedWith(shouldBe200Ok()))
                .getResponse()
                .getFlat();

        Assertions.assertThat(draft1.getStatus())
                .describedAs("Статус черновика")
                .isEqualTo(RealtyRentApiFlat.StatusEnum.DRAFT);

        RealtyRentApiSendSmsForFlatRequest request =
                new RealtyRentApiSendSmsForFlatRequest()
                        .admitadUid(getRandomString())
                        .yandexUserUid(getRandomString());

        RealtyRentApiSentSmsInfo sentSmsInfo = apiV2
                .rent()
                .sendSmsForFlat()
                .userPath(uid1)
                .reqSpec(authSpec()).authorizationHeader(token1)
                .body(request)
                .executeAs(validatedWith(shouldBe200Ok()))
                .getResponse()
                .getSentSmsInfo();

        Assertions.assertThat(sentSmsInfo.getCodeLength())
                .describedAs("Длина кода авторизации")
                .isEqualTo(5);

        RealtyRentApiFlat flat = rentApiAdaptor.getFlatDraft(token1, uid1).getFlat();
        Assertions.assertThat(flat).describedAs("Черновик после запроса send-sms").isNotNull();
        Assertions.assertThat(flat.getFlatId())
                .describedAs("ID квартиры в черновике")
                .isEqualTo(draft1.getFlatId());
        Assertions.assertThat(flat.getStatus())
                .describedAs("Статус квартиры после запроса send-sms")
                .isEqualTo(RealtyRentApiFlat.StatusEnum.WAITING_FOR_CONFIRMATION);
    }

    @Test
    @DisplayName("Превышение лимита по отправке SMS в ед. времени")
    public void limitSendSmsTest() {
        rentUser1 = rentApiAdaptor.getOrCreateRentUser(token1, uid1);

        String insideAddress = rentApiAdaptor.addressesInsidePolygon.get(0);
        draft1 = apiV2
                .rent()
                .upsertFlatDraft()
                .userPath(uid1)
                .reqSpec(authSpec()).authorizationHeader(token1)
                .xUidHeader(uid1)
                .body(rentApiAdaptor.buildRandomUpdateFlatDraftRequest(insideAddress))
                .executeAs(validatedWith(shouldBe200Ok()))
                .getResponse()
                .getFlat();

        Assertions.assertThat(draft1.getStatus())
                .describedAs("Статус черновика")
                .isEqualTo(RealtyRentApiFlat.StatusEnum.DRAFT);

        RealtyRentApiSendSmsForFlatRequest request = rentApiAdaptor.buildRealtyRentApiSendSmsForFlatRequest();

        apiV2
                .rent()
                .sendSmsForFlat()
                .userPath(uid1)
                .reqSpec(authSpec()).authorizationHeader(token1)
                .body(request)
                .execute(validatedWith(shouldBe200Ok()));

        int count = 3;
        for (int i = 0; i < count; i++) {
            RealtyApiError response = apiV2
                    .rent()
                    .sendSmsForFlat()
                    .userPath(uid1)
                    .reqSpec(authSpec()).authorizationHeader(token1)
                    .body(request)
                    .execute(validatedWith(shouldBeCode(429)))
                    .as(RealtyApiErrorResponse.class)
                    .getError();

            Assertions.assertThat(response)
                    .describedAs("Ответ ошибки")
                    .isNotNull();
            Assertions.assertThat(response.getCode())
                    .describedAs("Код ошибки")
                    .isEqualTo(RealtyApiError.CodeEnum.TOO_MANY_REQUESTS);
        }
    }

    @Test
    @DisplayName("Попытка подтвердить несуществующий черновик")
    public void sendSmsForNonexistentDraftTest() {
        rentUser1 = rentApiAdaptor.getOrCreateRentUser(token1, uid1);

        RealtyRentApiSendSmsForFlatRequest request = rentApiAdaptor.buildRealtyRentApiSendSmsForFlatRequest();
        RealtyApiError response = apiV2
                .rent()
                .sendSmsForFlat()
                .userPath(uid1)
                .reqSpec(authSpec()).authorizationHeader(token1)
                .body(request)
                .execute(validatedWith(shouldBeCode(409)))
                .as(RealtyApiErrorResponse.class)
                .getError();

        Assertions.assertThat(response).describedAs("Ответ ошибки").isNotNull();
        Assertions.assertThat(response.getCode())
                .describedAs("Код ошибки")
                .isEqualTo(RealtyApiError.CodeEnum.CONFLICT);
        Assertions.assertThat(response.getMessage())
                .describedAs("Текст ошибки")
                .isEqualTo("Чтобы подтвердить заявку, нужно её заполнить");
    }

    @Test
    @DisplayName("Попытка подтвердить невалидный черновик (с адресом вне полигона)")
    public void sendSmsForDraftOutsidePolygonTest() {
        rentUser1 = rentApiAdaptor.getOrCreateRentUser(token1, uid1);

        String outsideAddress = rentApiAdaptor.addressesOutsidePolygon.get(0);
        draft1 = apiV2
                .rent()
                .upsertFlatDraft()
                .userPath(uid1)
                .reqSpec(authSpec()).authorizationHeader(token1)
                .xUidHeader(uid1)
                .body(rentApiAdaptor.buildRandomUpdateFlatDraftRequest(outsideAddress))
                .executeAs(validatedWith(shouldBe200Ok()))
                .getResponse()
                .getFlat();

        Assertions.assertThat(draft1.getStatus())
                .describedAs("Статус черновика")
                .isEqualTo(RealtyRentApiFlat.StatusEnum.DRAFT);

        RealtyRentApiSendSmsForFlatRequest request =
                new RealtyRentApiSendSmsForFlatRequest()
                        .admitadUid(getRandomString())
                        .yandexUserUid(getRandomString());

        RealtyApiFlatValidationError response = apiV2
                .rent()
                .sendSmsForFlat()
                .userPath(uid1)
                .reqSpec(authSpec()).authorizationHeader(token1)
                .body(request)
                .execute(validatedWith(shouldBeCode(400)))
                .as(RealtyApiApiSendSmsForFlatConfirmationErrorResponse.class)
                .getError();

        Assertions.assertThat(response).describedAs("Ответ ошибки").isNotNull();
        Assertions.assertThat(response.getCode())
                .describedAs("Код ошибки")
                .isEqualTo(RealtyApiFlatValidationError.CodeEnum.VALIDATION_ERROR);
        Assertions.assertThat(response.getData())
                .describedAs("Ошибки валидации квартиры")
                .isNotNull();
        Assertions.assertThat(response.getData().getValidationErrors())
                .describedAs("Список ошибок валидации квартиры")
                .isNotNull();
        Assertions.assertThat(response.getData().getValidationErrors().size())
                .describedAs("Количество ошибок валидации квартиры")
                .isEqualTo(1);
        Assertions.assertThat(response.getData().getValidationErrors().get(0))
                .describedAs("Ошибка валидации")
                .isNotNull();
        Assertions.assertThat(response.getData().getValidationErrors().get(0).getCode())
                .describedAs("Код ошибки валидации")
                .isEqualTo(RealtyRentApiFlatValidationErrorDataFlatValidationMessage.CodeEnum.ADDRESS_OUTSIDE_OF_WORKING_POLYGON);
        Assertions.assertThat(response.getData().getValidationErrors().get(0).getParameter())
                .describedAs("Поле с ошибкой валидации")
                .isEqualTo("/address/address");
    }

    @Test
    @DisplayName("Ошибка авторизации при запросе SMS-кода")
    public void sendSmsAuthorizationErrorTest() {
        rentUser1 = rentApiAdaptor.getOrCreateRentUser(token1, uid1);
        draft1 = rentApiAdaptor.createFlatDraft(token1, uid1).getFlat();
        RealtyRentApiSendSmsForFlatRequest request = rentApiAdaptor.buildRealtyRentApiSendSmsForFlatRequest();
        apiV2
                .rent()
                .sendSmsForFlat()
                .userPath(uid1)
                .body(request)
                .execute(validatedWith(shouldBeCode(403)));
    }

    @Test
    @DisplayName("Ручка проверки введенного кода идемпотентна")
    public void doubleCodeSubmitTest() {
        rentUser1 = rentApiAdaptor.getOrCreateRentUser(token1, uid1);

        String goodAddress = rentApiAdaptor.addressesInsidePolygon.get(0);

        draft1 = apiV2
                .rent()
                .upsertFlatDraft()
                .userPath(uid1)
                .reqSpec(authSpec()).authorizationHeader(token1)
                .xUidHeader(uid1)
                .body(rentApiAdaptor.buildRandomUpdateFlatDraftRequest(goodAddress))
                .executeAs(validatedWith(shouldBe200Ok()))
                .getResponse()
                .getFlat();

        Assertions.assertThat(draft1.getStatus())
                .describedAs("Статус черновика")
                .isEqualTo(RealtyRentApiFlat.StatusEnum.DRAFT);

        RealtyRentApiSentSmsInfo sentSmsInfo = apiV2
                .rent()
                .sendSmsForFlat()
                .userPath(uid1)
                .reqSpec(authSpec()).authorizationHeader(token1)
                .body(new RealtyRentApiSendSmsForFlatRequest())
                .executeAs(validatedWith(shouldBe200Ok()))
                .getResponse()
                .getSentSmsInfo();

        apiV2.rent().confirmFlat()
                .userPath(uid1)
                .reqSpec(authSpec()).authorizationHeader(token1)
                .body(
                        new RealtyRentApiConfirmFlatRequest().confirmSmsInfo(
                                new RealtyRentApiConfirmSmsInfo()
                                        .code("00000")
                                        .requestId(sentSmsInfo.getRequestId())
                        )
                )
                .execute(validatedWith(shouldBe200Ok()));

        apiV2.rent().confirmFlat()
                .userPath(uid1)
                .reqSpec(authSpec()).authorizationHeader(token1)
                .body(
                        new RealtyRentApiConfirmFlatRequest().confirmSmsInfo(
                                new RealtyRentApiConfirmSmsInfo()
                                        .code("00000")
                                        .requestId(sentSmsInfo.getRequestId())
                        )
                )
                .execute(validatedWith(shouldBe200Ok()));
    }

    @Test
    @DisplayName("Введение неверного кода")
    public void invalidCodeSubmitTest() {
        rentUser1 = rentApiAdaptor.getOrCreateRentUser(token1, uid1);

        String goodAddress = rentApiAdaptor.addressesInsidePolygon.get(0);

        draft1 = apiV2
                .rent()
                .upsertFlatDraft()
                .userPath(uid1)
                .reqSpec(authSpec()).authorizationHeader(token1)
                .xUidHeader(uid1)
                .body(rentApiAdaptor.buildRandomUpdateFlatDraftRequest(goodAddress))
                .executeAs(validatedWith(shouldBe200Ok()))
                .getResponse()
                .getFlat();

        Assertions.assertThat(draft1.getStatus())
                .describedAs("Статус черновика")
                .isEqualTo(RealtyRentApiFlat.StatusEnum.DRAFT);

        RealtyRentApiSentSmsInfo sentSmsInfo = apiV2
                .rent()
                .sendSmsForFlat()
                .userPath(uid1)
                .reqSpec(authSpec()).authorizationHeader(token1)
                .body(new RealtyRentApiSendSmsForFlatRequest())
                .executeAs(validatedWith(shouldBe200Ok()))
                .getResponse()
                .getSentSmsInfo();

        RealtyApiBasicSmsConfirmationValidationError response = apiV2.rent().confirmFlat()
                .userPath(uid1)
                .reqSpec(authSpec()).authorizationHeader(token1)
                .body(
                        new RealtyRentApiConfirmFlatRequest().confirmSmsInfo(
                                new RealtyRentApiConfirmSmsInfo()
                                        .code("00001")
                                        .requestId(sentSmsInfo.getRequestId())
                        )
                )
                .execute(validatedWith(shouldBeCode(400)))
                .as(RealtyApiBasicSmsConfirmationValidationErrorResponse.class)
                .getError();
        Assertions.assertThat(response).describedAs("Ответ ошибки").isNotNull();
        Assertions.assertThat(response.getCode())
                .describedAs("Код ошибки")
                .isEqualTo(RealtyApiBasicSmsConfirmationValidationError.CodeEnum.VALIDATION_ERROR);
        Assertions.assertThat(response.getData())
                .describedAs("Ошибки валидации")
                .isNotNull();
        Assertions.assertThat(response.getData().getValidationErrors())
                .describedAs("Список ошибок валидации")
                .isNotNull();
        Assertions.assertThat(response.getData().getValidationErrors().size())
                .describedAs("Количество ошибок валидации")
                .isEqualTo(1);
        Assertions.assertThat(response.getData().getValidationErrors().get(0))
                .describedAs("Ошибка валидации")
                .isNotNull();
        Assertions.assertThat(response.getData().getValidationErrors().get(0).getCode())
                .describedAs("Код ошибки валидации")
                .isEqualTo(RealtyApiBasicSmsConfirmationValidationErrorDataValidationMessage.CodeEnum.INVALID_CODE);
        Assertions.assertThat(response.getData().getValidationErrors().get(0).getParameter())
                .describedAs("Поле с ошибкой валидации")
                .isEqualTo("/confirmSmsInfo/code");
    }
}
