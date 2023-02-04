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
import ru.auto.tests.realtyapi.v2.model.RealtyApiFlatValidationError;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiFlat;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiFlatValidationErrorDataFlatValidationMessage;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiRentUser;
import ru.auto.tests.realtyapi.v2.model.RealtyRentApiUpdateFlatDraftRequest;
import ru.auto.tests.realtyapi.v2.rent.AbstractHandlerTest;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200Ok;
import static ru.auto.tests.realtyapi.ra.RequestSpecBuilders.authSpec;
import static ru.auto.tests.realtyapi.v2.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.realtyapi.v2.ResponseSpecBuilders.validatedWith;

@Title("/rent/draft")
@RunWith(GuiceTestRunner.class)
@Log4j
@GuiceModules(RealtyTestApiModule.class)
public class DraftValidationTest extends AbstractHandlerTest {

    private RealtyRentApiFlat draft1;
    private RealtyRentApiRentUser rentUser1;

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
        usersToDelete.add(rentUser1);
    }

    @AfterClass
    public static void cleaning() {
        deleteDrafts(token1);
        deleteUsers(token1);
        deleteAccounts();
    }


    private void assertSingleValidationErrorInResponse(
            RealtyApiFlatValidationError error,
            RealtyRentApiFlatValidationErrorDataFlatValidationMessage.CodeEnum expectedCode,
            String expectedField
    ) {
        Assertions.assertThat(error).describedAs("Ответ ошибки").isNotNull();
        Assertions.assertThat(error.getCode())
                .describedAs("Код ошибки")
                .isEqualTo(RealtyApiFlatValidationError.CodeEnum.VALIDATION_ERROR);
        Assertions.assertThat(error.getData())
                .describedAs("Ошибки валидации квартиры")
                .isNotNull();
        Assertions.assertThat(error.getData().getValidationErrors())
                .describedAs("Список ошибок валидации квартиры")
                .isNotNull();
        Assertions.assertThat(error.getData().getValidationErrors().size())
                .describedAs("Количество ошибок валидации квартиры")
                .isEqualTo(1);
        Assertions.assertThat(error.getData().getValidationErrors().get(0))
                .describedAs("Ошибка валидации")
                .isNotNull();
        Assertions.assertThat(error.getData().getValidationErrors().get(0).getCode())
                .describedAs("Код ошибки валидации")
                .isEqualTo(expectedCode);
        Assertions.assertThat(error.getData().getValidationErrors().get(0).getParameter())
                .describedAs("Поле с ошибкой валидации")
                .isEqualTo(expectedField);
    }

    private void assertSingleValidationError(
            RealtyRentApiFlatValidationErrorDataFlatValidationMessage.CodeEnum expectedCode,
            String expectedField
    ) {
        assertSingleValidationErrorInResponse(
                apiV2
                        .rent()
                        .sendSmsForFlat()
                        .userPath(uid1)
                        .reqSpec(authSpec()).authorizationHeader(token1)
                        .execute(validatedWith(shouldBeCode(400)))
                        .as(RealtyApiApiSendSmsForFlatConfirmationErrorResponse.class)
                        .getError(),
                expectedCode,
                expectedField
        );
    }

    @Test
    @DisplayName("Пустой адрес")
    public void emptyAddressTest() {
        rentUser1 = rentApiAdaptor.getOrCreateRentUser(token1, uid1);

        RealtyRentApiUpdateFlatDraftRequest request = rentApiAdaptor.buildRandomUpdateFlatDraftRequest("");

        draft1 = apiV2.rent().upsertFlatDraft()
                .userPath(uid1)
                .reqSpec(authSpec()).authorizationHeader(token1)
                .body(request)
                .executeAs(validatedWith(shouldBe200Ok()))
                .getResponse()
                .getFlat();

        Assertions.assertThat(draft1.getAddress().getAddress()).describedAs("Сохраненный адрес").isNull();

        assertSingleValidationError(
                RealtyRentApiFlatValidationErrorDataFlatValidationMessage.CodeEnum.EMPTY_ADDRESS,
                "/address/address"
        );
    }

    @Test
    @DisplayName("Адрес без дома")
    public void nonHouseAddressTest() {
        rentUser1 = rentApiAdaptor.getOrCreateRentUser(token1, uid1);

        String address = rentApiAdaptor.addressesOutsidePolygon.get(0);
        address = address.substring(0, address.lastIndexOf(','));
        RealtyRentApiUpdateFlatDraftRequest request = rentApiAdaptor.buildRandomUpdateFlatDraftRequest(address);

        draft1 = apiV2.rent().upsertFlatDraft()
                .userPath(uid1)
                .reqSpec(authSpec()).authorizationHeader(token1)
                .body(request)
                .executeAs(validatedWith(shouldBe200Ok()))
                .getResponse()
                .getFlat();

        Assertions.assertThat(draft1.getAddress().getAddress()).describedAs("Сохраненный адрес").isEqualTo(address);

        assertSingleValidationError(
                RealtyRentApiFlatValidationErrorDataFlatValidationMessage.CodeEnum.ADDRESS_NOT_HOUSE,
                "/address/address"
        );
    }

    @Test
    @DisplayName("Адрес вне зоны действия сервиса")
    public void addressOutsidePolygonsTest() {
        rentUser1 = rentApiAdaptor.getOrCreateRentUser(token1, uid1);

        String address = rentApiAdaptor.addressesOutsidePolygon.get(0);
        RealtyRentApiUpdateFlatDraftRequest request = rentApiAdaptor.buildRandomUpdateFlatDraftRequest(address);

        draft1 = apiV2.rent().upsertFlatDraft()
                .userPath(uid1)
                .reqSpec(authSpec()).authorizationHeader(token1)
                .body(request)
                .executeAs(validatedWith(shouldBe200Ok()))
                .getResponse()
                .getFlat();

        Assertions.assertThat(draft1.getAddress().getAddress()).describedAs("Сохраненный адрес").isEqualTo(address);

        assertSingleValidationError(
                RealtyRentApiFlatValidationErrorDataFlatValidationMessage.CodeEnum.ADDRESS_OUTSIDE_OF_WORKING_POLYGON,
                "/address/address"
        );
    }


    @Test
    @DisplayName("Пустой номер квартиры")
    public void emptyFlatNumberTest() {
        rentUser1 = rentApiAdaptor.getOrCreateRentUser(token1, uid1);

        String address = rentApiAdaptor.addressesInsidePolygon.get(0);
        RealtyRentApiUpdateFlatDraftRequest request = rentApiAdaptor.buildRandomUpdateFlatDraftRequest(address);
        request.setFlatNumber("");

        draft1 = apiV2.rent().upsertFlatDraft()
                .userPath(uid1)
                .reqSpec(authSpec()).authorizationHeader(token1)
                .body(request)
                .executeAs(validatedWith(shouldBe200Ok()))
                .getResponse()
                .getFlat();

        Assertions.assertThat(draft1.getAddress().getFlatNumber()).describedAs("Сохраненный номер квартиры").isNull();

        assertSingleValidationError(
                RealtyRentApiFlatValidationErrorDataFlatValidationMessage.CodeEnum.EMPTY_FLAT_NUMBER,
                "/address/flatNumber"
        );
    }

    @Test
    @DisplayName("Невалидный номер квартиры")
    public void invalidFlatNumberTest() {
        rentUser1 = rentApiAdaptor.getOrCreateRentUser(token1, uid1);

        String address = rentApiAdaptor.addressesInsidePolygon.get(0);
        RealtyRentApiUpdateFlatDraftRequest request = rentApiAdaptor.buildRandomUpdateFlatDraftRequest(address);
        request.setFlatNumber("кв12");

        draft1 = apiV2.rent().upsertFlatDraft()
                .userPath(uid1)
                .reqSpec(authSpec()).authorizationHeader(token1)
                .body(request)
                .executeAs(validatedWith(shouldBe200Ok()))
                .getResponse()
                .getFlat();

        Assertions.assertThat(draft1.getAddress().getFlatNumber()).describedAs("Сохраненный номер квартиры").isEqualTo("кв12");

        assertSingleValidationError(
                RealtyRentApiFlatValidationErrorDataFlatValidationMessage.CodeEnum.INVALID_FLAT_NUMBER,
                "/address/flatNumber"
        );
    }


    @Test
    @DisplayName("Пустой телефон")
    public void emptyPhoneTest() {
        rentUser1 = rentApiAdaptor.getOrCreateRentUser(token1, uid1);

        String address = rentApiAdaptor.addressesInsidePolygon.get(0);
        RealtyRentApiUpdateFlatDraftRequest request = rentApiAdaptor.buildRandomUpdateFlatDraftRequest(address);
        request.setPhone("");

        draft1 = apiV2.rent().upsertFlatDraft()
                .userPath(uid1)
                .reqSpec(authSpec()).authorizationHeader(token1)
                .body(request)
                .executeAs(validatedWith(shouldBe200Ok()))
                .getResponse()
                .getFlat();

        Assertions.assertThat(draft1.getPhone()).describedAs("Сохраненный телефон").isNull();

        assertSingleValidationError(
                RealtyRentApiFlatValidationErrorDataFlatValidationMessage.CodeEnum.EMPTY_PHONE,
                "/phone"
        );
    }

    @Test
    @DisplayName("Кривой телефон сохраняется, но не валидируется")
    public void badPhoneTest() {
        rentUser1 = rentApiAdaptor.getOrCreateRentUser(token1, uid1);

        String address = rentApiAdaptor.addressesInsidePolygon.get(0);
        RealtyRentApiUpdateFlatDraftRequest request = rentApiAdaptor.buildRandomUpdateFlatDraftRequest(address);
        String badPhone = "странный телефон";
        request.setPhone(badPhone);

        draft1 = apiV2.rent().upsertFlatDraft()
                .userPath(uid1)
                .reqSpec(authSpec()).authorizationHeader(token1)
                .body(request)
                .executeAs(validatedWith(shouldBe200Ok()))
                .getResponse()
                .getFlat();

        Assertions.assertThat(draft1.getPhone()).describedAs("Сохраненный телефон").isEqualTo(badPhone);

        assertSingleValidationError(
                RealtyRentApiFlatValidationErrorDataFlatValidationMessage.CodeEnum.INVALID_PHONE,
                "/phone"
        );
    }


    @Test
    @DisplayName("Пустое имя")
    public void emptyNameTest() {
        rentUser1 = rentApiAdaptor.getOrCreateRentUser(token1, uid1);

        String address = rentApiAdaptor.addressesInsidePolygon.get(0);
        RealtyRentApiUpdateFlatDraftRequest request = rentApiAdaptor.buildRandomUpdateFlatDraftRequest(address);
        request.getPerson().setName("");

        draft1 = apiV2.rent().upsertFlatDraft()
                .userPath(uid1)
                .reqSpec(authSpec()).authorizationHeader(token1)
                .body(request)
                .executeAs(validatedWith(shouldBe200Ok()))
                .getResponse()
                .getFlat();

        Assertions.assertThat(draft1.getPerson().getName()).describedAs("Сохраненное имя").isNull();

        assertSingleValidationError(
                RealtyRentApiFlatValidationErrorDataFlatValidationMessage.CodeEnum.EMPTY_PERSON_NAME,
                "/person/name"
        );
    }

    @Test
    @DisplayName("Невалидное имя")
    public void invalidNameTest() {
        rentUser1 = rentApiAdaptor.getOrCreateRentUser(token1, uid1);

        String address = rentApiAdaptor.addressesInsidePolygon.get(0);
        RealtyRentApiUpdateFlatDraftRequest request = rentApiAdaptor.buildRandomUpdateFlatDraftRequest(address);
        request.getPerson().setName("Иван86");

        draft1 = apiV2.rent().upsertFlatDraft()
                .userPath(uid1)
                .reqSpec(authSpec()).authorizationHeader(token1)
                .body(request)
                .executeAs(validatedWith(shouldBe200Ok()))
                .getResponse()
                .getFlat();

        Assertions.assertThat(draft1.getPerson().getName()).describedAs("Сохраненное имя").isEqualTo("Иван86");

        assertSingleValidationError(
                RealtyRentApiFlatValidationErrorDataFlatValidationMessage.CodeEnum.INVALID_PERSON_NAME,
                "/person/name"
        );
    }

    @Test
    @DisplayName("Пустая фамилия")
    public void emptySurnameTest() {
        rentUser1 = rentApiAdaptor.getOrCreateRentUser(token1, uid1);

        String address = rentApiAdaptor.addressesInsidePolygon.get(0);
        RealtyRentApiUpdateFlatDraftRequest request = rentApiAdaptor.buildRandomUpdateFlatDraftRequest(address);
        request.getPerson().setSurname("");

        draft1 = apiV2.rent().upsertFlatDraft()
                .userPath(uid1)
                .reqSpec(authSpec()).authorizationHeader(token1)
                .body(request)
                .executeAs(validatedWith(shouldBe200Ok()))
                .getResponse()
                .getFlat();

        Assertions.assertThat(draft1.getPerson().getSurname()).describedAs("Сохраненное фамилие").isNull();

        assertSingleValidationError(
                RealtyRentApiFlatValidationErrorDataFlatValidationMessage.CodeEnum.EMPTY_PERSON_NAME,
                "/person/surname"
        );
    }

    @Test
    @DisplayName("Невалидная фамилия")
    public void invalidSurnameTest() {
        rentUser1 = rentApiAdaptor.getOrCreateRentUser(token1, uid1);

        String address = rentApiAdaptor.addressesInsidePolygon.get(0);
        RealtyRentApiUpdateFlatDraftRequest request = rentApiAdaptor.buildRandomUpdateFlatDraftRequest(address);
        request.getPerson().setSurname("Иванов86");

        draft1 = apiV2.rent().upsertFlatDraft()
                .userPath(uid1)
                .reqSpec(authSpec()).authorizationHeader(token1)
                .body(request)
                .executeAs(validatedWith(shouldBe200Ok()))
                .getResponse()
                .getFlat();

        Assertions.assertThat(draft1.getPerson().getSurname()).describedAs("Сохраненное фамилие").isEqualTo("Иванов86");

        assertSingleValidationError(
                RealtyRentApiFlatValidationErrorDataFlatValidationMessage.CodeEnum.INVALID_PERSON_NAME,
                "/person/surname"
        );
    }

    @Test
    @DisplayName("Невалидное отчество")
    public void invalidPatronymicTest() {
        rentUser1 = rentApiAdaptor.getOrCreateRentUser(token1, uid1);

        String address = rentApiAdaptor.addressesInsidePolygon.get(0);
        RealtyRentApiUpdateFlatDraftRequest request = rentApiAdaptor.buildRandomUpdateFlatDraftRequest(address);
        request.getPerson().setPatronymic("Иванович86");

        draft1 = apiV2.rent().upsertFlatDraft()
                .userPath(uid1)
                .reqSpec(authSpec()).authorizationHeader(token1)
                .body(request)
                .executeAs(validatedWith(shouldBe200Ok()))
                .getResponse()
                .getFlat();

        Assertions.assertThat(draft1.getPerson().getPatronymic()).describedAs("Сохраненное отчество").isEqualTo("Иванович86");

        assertSingleValidationError(
                RealtyRentApiFlatValidationErrorDataFlatValidationMessage.CodeEnum.INVALID_PERSON_NAME,
                "/person/patronymic"
        );
    }

    @Test
    @DisplayName("Пустая почта")
    public void emptyEmailTest() {
        rentUser1 = rentApiAdaptor.getOrCreateRentUser(token1, uid1);

        String address = rentApiAdaptor.addressesInsidePolygon.get(0);
        RealtyRentApiUpdateFlatDraftRequest request = rentApiAdaptor.buildRandomUpdateFlatDraftRequest(address);
        request.setEmail("");

        draft1 = apiV2.rent().upsertFlatDraft()
                .userPath(uid1)
                .reqSpec(authSpec()).authorizationHeader(token1)
                .body(request)
                .executeAs(validatedWith(shouldBe200Ok()))
                .getResponse()
                .getFlat();

        Assertions.assertThat(draft1.getEmail()).describedAs("Сохраненная почта").isNull();

        assertSingleValidationError(
                RealtyRentApiFlatValidationErrorDataFlatValidationMessage.CodeEnum.EMPTY_EMAIL,
                "/email"
        );
    }

    @Test
    @DisplayName("Невалидная почта")
    public void invalidEmailTest() {
        rentUser1 = rentApiAdaptor.getOrCreateRentUser(token1, uid1);

        String address = rentApiAdaptor.addressesInsidePolygon.get(0);
        RealtyRentApiUpdateFlatDraftRequest request = rentApiAdaptor.buildRandomUpdateFlatDraftRequest(address);
        request.setEmail("privet@medved");

        draft1 = apiV2.rent().upsertFlatDraft()
                .userPath(uid1)
                .reqSpec(authSpec()).authorizationHeader(token1)
                .body(request)
                .executeAs(validatedWith(shouldBe200Ok()))
                .getResponse()
                .getFlat();

        Assertions.assertThat(draft1.getEmail()).describedAs("Сохраненная почта").isEqualTo("privet@medved");

        assertSingleValidationError(
                RealtyRentApiFlatValidationErrorDataFlatValidationMessage.CodeEnum.INVALID_EMAIL,
                "/email"
        );
    }
}
