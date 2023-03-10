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
        Assertions.assertThat(error).describedAs("?????????? ????????????").isNotNull();
        Assertions.assertThat(error.getCode())
                .describedAs("?????? ????????????")
                .isEqualTo(RealtyApiFlatValidationError.CodeEnum.VALIDATION_ERROR);
        Assertions.assertThat(error.getData())
                .describedAs("???????????? ?????????????????? ????????????????")
                .isNotNull();
        Assertions.assertThat(error.getData().getValidationErrors())
                .describedAs("???????????? ???????????? ?????????????????? ????????????????")
                .isNotNull();
        Assertions.assertThat(error.getData().getValidationErrors().size())
                .describedAs("???????????????????? ???????????? ?????????????????? ????????????????")
                .isEqualTo(1);
        Assertions.assertThat(error.getData().getValidationErrors().get(0))
                .describedAs("???????????? ??????????????????")
                .isNotNull();
        Assertions.assertThat(error.getData().getValidationErrors().get(0).getCode())
                .describedAs("?????? ???????????? ??????????????????")
                .isEqualTo(expectedCode);
        Assertions.assertThat(error.getData().getValidationErrors().get(0).getParameter())
                .describedAs("???????? ?? ?????????????? ??????????????????")
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
    @DisplayName("???????????? ??????????")
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

        Assertions.assertThat(draft1.getAddress().getAddress()).describedAs("?????????????????????? ??????????").isNull();

        assertSingleValidationError(
                RealtyRentApiFlatValidationErrorDataFlatValidationMessage.CodeEnum.EMPTY_ADDRESS,
                "/address/address"
        );
    }

    @Test
    @DisplayName("?????????? ?????? ????????")
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

        Assertions.assertThat(draft1.getAddress().getAddress()).describedAs("?????????????????????? ??????????").isEqualTo(address);

        assertSingleValidationError(
                RealtyRentApiFlatValidationErrorDataFlatValidationMessage.CodeEnum.ADDRESS_NOT_HOUSE,
                "/address/address"
        );
    }

    @Test
    @DisplayName("?????????? ?????? ???????? ???????????????? ??????????????")
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

        Assertions.assertThat(draft1.getAddress().getAddress()).describedAs("?????????????????????? ??????????").isEqualTo(address);

        assertSingleValidationError(
                RealtyRentApiFlatValidationErrorDataFlatValidationMessage.CodeEnum.ADDRESS_OUTSIDE_OF_WORKING_POLYGON,
                "/address/address"
        );
    }


    @Test
    @DisplayName("???????????? ?????????? ????????????????")
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

        Assertions.assertThat(draft1.getAddress().getFlatNumber()).describedAs("?????????????????????? ?????????? ????????????????").isNull();

        assertSingleValidationError(
                RealtyRentApiFlatValidationErrorDataFlatValidationMessage.CodeEnum.EMPTY_FLAT_NUMBER,
                "/address/flatNumber"
        );
    }

    @Test
    @DisplayName("???????????????????? ?????????? ????????????????")
    public void invalidFlatNumberTest() {
        rentUser1 = rentApiAdaptor.getOrCreateRentUser(token1, uid1);

        String address = rentApiAdaptor.addressesInsidePolygon.get(0);
        RealtyRentApiUpdateFlatDraftRequest request = rentApiAdaptor.buildRandomUpdateFlatDraftRequest(address);
        request.setFlatNumber("????12");

        draft1 = apiV2.rent().upsertFlatDraft()
                .userPath(uid1)
                .reqSpec(authSpec()).authorizationHeader(token1)
                .body(request)
                .executeAs(validatedWith(shouldBe200Ok()))
                .getResponse()
                .getFlat();

        Assertions.assertThat(draft1.getAddress().getFlatNumber()).describedAs("?????????????????????? ?????????? ????????????????").isEqualTo("????12");

        assertSingleValidationError(
                RealtyRentApiFlatValidationErrorDataFlatValidationMessage.CodeEnum.INVALID_FLAT_NUMBER,
                "/address/flatNumber"
        );
    }


    @Test
    @DisplayName("???????????? ??????????????")
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

        Assertions.assertThat(draft1.getPhone()).describedAs("?????????????????????? ??????????????").isNull();

        assertSingleValidationError(
                RealtyRentApiFlatValidationErrorDataFlatValidationMessage.CodeEnum.EMPTY_PHONE,
                "/phone"
        );
    }

    @Test
    @DisplayName("???????????? ?????????????? ??????????????????????, ???? ???? ????????????????????????")
    public void badPhoneTest() {
        rentUser1 = rentApiAdaptor.getOrCreateRentUser(token1, uid1);

        String address = rentApiAdaptor.addressesInsidePolygon.get(0);
        RealtyRentApiUpdateFlatDraftRequest request = rentApiAdaptor.buildRandomUpdateFlatDraftRequest(address);
        String badPhone = "???????????????? ??????????????";
        request.setPhone(badPhone);

        draft1 = apiV2.rent().upsertFlatDraft()
                .userPath(uid1)
                .reqSpec(authSpec()).authorizationHeader(token1)
                .body(request)
                .executeAs(validatedWith(shouldBe200Ok()))
                .getResponse()
                .getFlat();

        Assertions.assertThat(draft1.getPhone()).describedAs("?????????????????????? ??????????????").isEqualTo(badPhone);

        assertSingleValidationError(
                RealtyRentApiFlatValidationErrorDataFlatValidationMessage.CodeEnum.INVALID_PHONE,
                "/phone"
        );
    }


    @Test
    @DisplayName("???????????? ??????")
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

        Assertions.assertThat(draft1.getPerson().getName()).describedAs("?????????????????????? ??????").isNull();

        assertSingleValidationError(
                RealtyRentApiFlatValidationErrorDataFlatValidationMessage.CodeEnum.EMPTY_PERSON_NAME,
                "/person/name"
        );
    }

    @Test
    @DisplayName("???????????????????? ??????")
    public void invalidNameTest() {
        rentUser1 = rentApiAdaptor.getOrCreateRentUser(token1, uid1);

        String address = rentApiAdaptor.addressesInsidePolygon.get(0);
        RealtyRentApiUpdateFlatDraftRequest request = rentApiAdaptor.buildRandomUpdateFlatDraftRequest(address);
        request.getPerson().setName("????????86");

        draft1 = apiV2.rent().upsertFlatDraft()
                .userPath(uid1)
                .reqSpec(authSpec()).authorizationHeader(token1)
                .body(request)
                .executeAs(validatedWith(shouldBe200Ok()))
                .getResponse()
                .getFlat();

        Assertions.assertThat(draft1.getPerson().getName()).describedAs("?????????????????????? ??????").isEqualTo("????????86");

        assertSingleValidationError(
                RealtyRentApiFlatValidationErrorDataFlatValidationMessage.CodeEnum.INVALID_PERSON_NAME,
                "/person/name"
        );
    }

    @Test
    @DisplayName("???????????? ??????????????")
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

        Assertions.assertThat(draft1.getPerson().getSurname()).describedAs("?????????????????????? ??????????????").isNull();

        assertSingleValidationError(
                RealtyRentApiFlatValidationErrorDataFlatValidationMessage.CodeEnum.EMPTY_PERSON_NAME,
                "/person/surname"
        );
    }

    @Test
    @DisplayName("???????????????????? ??????????????")
    public void invalidSurnameTest() {
        rentUser1 = rentApiAdaptor.getOrCreateRentUser(token1, uid1);

        String address = rentApiAdaptor.addressesInsidePolygon.get(0);
        RealtyRentApiUpdateFlatDraftRequest request = rentApiAdaptor.buildRandomUpdateFlatDraftRequest(address);
        request.getPerson().setSurname("????????????86");

        draft1 = apiV2.rent().upsertFlatDraft()
                .userPath(uid1)
                .reqSpec(authSpec()).authorizationHeader(token1)
                .body(request)
                .executeAs(validatedWith(shouldBe200Ok()))
                .getResponse()
                .getFlat();

        Assertions.assertThat(draft1.getPerson().getSurname()).describedAs("?????????????????????? ??????????????").isEqualTo("????????????86");

        assertSingleValidationError(
                RealtyRentApiFlatValidationErrorDataFlatValidationMessage.CodeEnum.INVALID_PERSON_NAME,
                "/person/surname"
        );
    }

    @Test
    @DisplayName("???????????????????? ????????????????")
    public void invalidPatronymicTest() {
        rentUser1 = rentApiAdaptor.getOrCreateRentUser(token1, uid1);

        String address = rentApiAdaptor.addressesInsidePolygon.get(0);
        RealtyRentApiUpdateFlatDraftRequest request = rentApiAdaptor.buildRandomUpdateFlatDraftRequest(address);
        request.getPerson().setPatronymic("????????????????86");

        draft1 = apiV2.rent().upsertFlatDraft()
                .userPath(uid1)
                .reqSpec(authSpec()).authorizationHeader(token1)
                .body(request)
                .executeAs(validatedWith(shouldBe200Ok()))
                .getResponse()
                .getFlat();

        Assertions.assertThat(draft1.getPerson().getPatronymic()).describedAs("?????????????????????? ????????????????").isEqualTo("????????????????86");

        assertSingleValidationError(
                RealtyRentApiFlatValidationErrorDataFlatValidationMessage.CodeEnum.INVALID_PERSON_NAME,
                "/person/patronymic"
        );
    }

    @Test
    @DisplayName("???????????? ??????????")
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

        Assertions.assertThat(draft1.getEmail()).describedAs("?????????????????????? ??????????").isNull();

        assertSingleValidationError(
                RealtyRentApiFlatValidationErrorDataFlatValidationMessage.CodeEnum.EMPTY_EMAIL,
                "/email"
        );
    }

    @Test
    @DisplayName("???????????????????? ??????????")
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

        Assertions.assertThat(draft1.getEmail()).describedAs("?????????????????????? ??????????").isEqualTo("privet@medved");

        assertSingleValidationError(
                RealtyRentApiFlatValidationErrorDataFlatValidationMessage.CodeEnum.INVALID_EMAIL,
                "/email"
        );
    }
}
