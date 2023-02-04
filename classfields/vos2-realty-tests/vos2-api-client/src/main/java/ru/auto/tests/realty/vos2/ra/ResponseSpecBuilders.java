package ru.auto.tests.realty.vos2.ra;

import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.specification.ResponseSpecification;

import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.startsWith;
import static ru.auto.tests.realty.vos2.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.realty.vos2.enums.ErrorStatusCode.ILLEGAL_ARGUMENT;
import static ru.auto.tests.realty.vos2.enums.ErrorStatusCode.INTERNAL_ERROR;
import static ru.auto.tests.realty.vos2.enums.ErrorStatusCode.NOT_FOUND;
import static ru.auto.tests.realty.vos2.enums.ErrorStatusCode.UNKNOWN_OFFER;
import static ru.auto.tests.realty.vos2.enums.ErrorStatusCode.UNKNOWN_USER;
import static ru.auto.tests.realty.vos2.enums.ErrorStatusCode.WRONG_REQUEST;

/**
 * Created by vicdev on 18.09.17.
 */
public class ResponseSpecBuilders {

    private static final String STATUS_PATH = "status";
    private static final String STATUS_CODE = "ERROR";
    private static final String ERRORS_SIZE = "errors.size";
    private static final String ERROR_CODE = "errors[0].code";
    private static final String ERROR_DESCRIPTION = "errors[0].description";

    public static ResponseSpecification shouldBeStatusOk() {
        return ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON()
                .expectBody(STATUS_PATH, equalTo("OK")).build();
    }

    public static ResponseSpecBuilder shouldBe404WithOfferNotDraft(String offerId) {
        return shouldBeCode(SC_BAD_REQUEST).expectContentType(JSON)
                .expectBody(STATUS_PATH, equalTo(STATUS_CODE))
                .expectBody(ERRORS_SIZE, greaterThanOrEqualTo(1))
                .expectBody(ERROR_CODE, equalTo(UNKNOWN_OFFER.value()))
                .expectBody(ERROR_DESCRIPTION, equalTo(format("Offer is not a draft: i_%s", offerId)));
    }

    public static ResponseSpecBuilder shouldBe404WithOfferNotFound(String offerId) {
        return shouldBeCode(SC_NOT_FOUND).expectContentType(JSON)
                .expectBody(STATUS_PATH, equalTo(STATUS_CODE))
                .expectBody(ERRORS_SIZE, greaterThanOrEqualTo(1))
                .expectBody(ERROR_CODE, equalTo(UNKNOWN_OFFER.value()))
                .expectBody(ERROR_DESCRIPTION, equalTo(format("Offer i_%s is not found", offerId)));
    }

    public static ResponseSpecBuilder shouldBe404WithOfferNotFound(String uid, String offerId) {
        return shouldBeCode(SC_NOT_FOUND).expectContentType(JSON)
                .expectBody(STATUS_PATH, equalTo(STATUS_CODE))
                .expectBody(ERRORS_SIZE, greaterThanOrEqualTo(1))
                .expectBody(ERROR_CODE, equalTo(UNKNOWN_OFFER.value()))
                .expectBody(ERROR_DESCRIPTION, equalTo(format("Offer not found: uid_%s:%s", uid, offerId)));
    }

    public static ResponseSpecBuilder shouldBe404WithRequestedHandlerNotBeFound() {
        return shouldBeCode(SC_NOT_FOUND).expectContentType(JSON)
                .expectBody(STATUS_PATH, equalTo(STATUS_CODE))
                .expectBody(ERRORS_SIZE, greaterThanOrEqualTo(1))
                .expectBody(ERROR_CODE, equalTo(NOT_FOUND.value()))
                .expectBody(ERROR_DESCRIPTION, startsWith("The requested handler could not be found." +
                        " Please check method and url of the request."));
    }

    public static ResponseSpecBuilder shouldBe404WithUserNotFound(String accountId) {
        return shouldBeCode(SC_NOT_FOUND).expectContentType(JSON)
                .expectBody(STATUS_PATH, equalTo(STATUS_CODE))
                .expectBody(ERRORS_SIZE, greaterThanOrEqualTo(1))
                .expectBody(ERROR_CODE, equalTo(UNKNOWN_USER.value()))
                .expectBody(ERROR_DESCRIPTION, equalTo(format("User uid_%s not found", accountId)));
    }

    public static ResponseSpecBuilder shouldBe404WithUserIsNotFound(String accountId) {
        return shouldBeCode(SC_NOT_FOUND).expectContentType(JSON)
                .expectBody(STATUS_PATH, equalTo(STATUS_CODE))
                .expectBody(ERRORS_SIZE, greaterThanOrEqualTo(1))
                .expectBody(ERROR_CODE, equalTo(NOT_FOUND.value()))
                .expectBody(ERROR_DESCRIPTION, equalTo(format("UserNotFoundException: User uid_%s is not found", accountId)));
    }

    public static ResponseSpecBuilder shouldBe400WithNoContentToMap() {
        return shouldBeCode(SC_BAD_REQUEST).expectContentType(JSON)
                .expectBody(STATUS_PATH, equalTo(STATUS_CODE))
                .expectBody(ERRORS_SIZE, greaterThanOrEqualTo(1))
                .expectBody(ERROR_CODE, equalTo(WRONG_REQUEST.value()))
                .expectBody(ERROR_DESCRIPTION, startsWith("No content to map due to end-of-input"));
    }

   public static ResponseSpecBuilder shouldBe400WithEntityExpected() {
        return shouldBeCode(SC_BAD_REQUEST).expectContentType(JSON)
                .expectBody(STATUS_PATH, equalTo(STATUS_CODE))
                .expectBody(ERRORS_SIZE, greaterThanOrEqualTo(1))
                .expectBody(ERROR_CODE, equalTo(WRONG_REQUEST.value()))
                .expectBody(ERROR_DESCRIPTION, startsWith("Entity expected"));
    }
    public static ResponseSpecBuilder shouldBe400WithNotValidJSON() {
        return shouldBeCode(SC_BAD_REQUEST).expectContentType(JSON)
                .expectBody(STATUS_PATH, equalTo(STATUS_CODE))
                .expectBody(ERRORS_SIZE, greaterThanOrEqualTo(1))
                .expectBody(ERROR_CODE, equalTo(ILLEGAL_ARGUMENT.value()))
                .expectBody(ERROR_DESCRIPTION, startsWith("Received JSON is not valid."));
    }

    public static ResponseSpecBuilder shouldBe400WithNoEnumConstant() {
        return shouldBeCode(SC_BAD_REQUEST).expectContentType(JSON)
                .expectBody(STATUS_PATH, equalTo(STATUS_CODE))
                .expectBody(ERRORS_SIZE, greaterThanOrEqualTo(1))
                .expectBody(ERROR_CODE, equalTo(ILLEGAL_ARGUMENT.value()))
                .expectBody(ERROR_DESCRIPTION, startsWith("IllegalArgumentException: No enum constant"));
    }

    public static ResponseSpecBuilder shouldBe500WithNoSuchElement() {
        return shouldBeCode(SC_INTERNAL_SERVER_ERROR).expectContentType(JSON)
                .expectBody(STATUS_PATH, equalTo(STATUS_CODE))
                .expectBody(ERRORS_SIZE, greaterThanOrEqualTo(1))
                .expectBody(ERROR_CODE, equalTo(INTERNAL_ERROR.value()))
                .expectBody(ERROR_DESCRIPTION, startsWith("NoSuchElementException"));
    }

    public static ResponseSpecBuilder shouldBe400WithNumberFormatException(String offerId) {
        return shouldBeCode(SC_BAD_REQUEST).expectContentType(JSON)
                .expectBody(STATUS_PATH, equalTo(STATUS_CODE))
                .expectBody(ERRORS_SIZE, greaterThanOrEqualTo(1))
                .expectBody(ERROR_CODE, equalTo(ILLEGAL_ARGUMENT.value()))
                .expectBody(ERROR_DESCRIPTION,
                        equalTo(format("NumberFormatException: For input string: \"%s\"", offerId)));
    }

    public static ResponseSpecBuilder shouldBe400WithInternalNumberFormatException(String offerId) {
        return shouldBeCode(SC_BAD_REQUEST).expectContentType(JSON)
                .expectBody(STATUS_PATH, equalTo(STATUS_CODE))
                .expectBody(ERRORS_SIZE, greaterThanOrEqualTo(1))
                .expectBody(ERROR_CODE, equalTo(INTERNAL_ERROR.value()))
                .expectBody(ERROR_DESCRIPTION,
                        equalTo(format("NumberFormatException: For input string: \"%s\"", offerId)));
    }

    public static ResponseSpecBuilder shouldBe500WithInternalNumberFormatException(String offerId) {
        return shouldBeCode(SC_INTERNAL_SERVER_ERROR).expectContentType(JSON)
                .expectBody(STATUS_PATH, equalTo(STATUS_CODE))
                .expectBody(ERRORS_SIZE, greaterThanOrEqualTo(1))
                .expectBody(ERROR_CODE, equalTo(INTERNAL_ERROR.value()))
                .expectBody(ERROR_DESCRIPTION,
                        equalTo(format("NumberFormatException: For input string: \"%s\"", offerId)));
    }

    public static ResponseSpecBuilder shouldBe404WithFailedIds(String uid,  String offerId) {
        return shouldBeCode(SC_NOT_FOUND).expectContentType(JSON)
                .expectBody(STATUS_PATH, equalTo(STATUS_CODE))
                .expectBody("failedIds.size", greaterThanOrEqualTo(1))
                .expectBody("failedIds[0]", equalTo(offerId))
                .expectBody("message",
                        equalTo(format("Check that provided offers exist and belong to user uid_%s", uid)));
    }
    public static ResponseSpecBuilder shouldBe404WithFailedIds(String uid,  java.util.List<String> offerIds) {
        return shouldBeCode(SC_NOT_FOUND).expectContentType(JSON)
                .expectBody(STATUS_PATH, equalTo(STATUS_CODE))
                .expectBody("failedIds.size", greaterThanOrEqualTo(1))
                .expectBody("failedIds[0]", equalTo(offerIds.get(0)))
                .expectBody("failedIds[1]", equalTo(offerIds.get(1)))
                .expectBody("message",
                        equalTo(format("Check that provided offers exist and belong to user uid_%s", uid)));
    }
    public static ResponseSpecBuilder shouldBe400WithNotDraftInOffer() {
        return shouldBeCode(SC_BAD_REQUEST).expectContentType(JSON)
                .expectBody(STATUS_PATH, equalTo(STATUS_CODE))
                .expectBody(ERRORS_SIZE, greaterThanOrEqualTo(1))
                .expectBody(ERROR_CODE, equalTo(ILLEGAL_ARGUMENT.value()))
                .expectBody(ERROR_DESCRIPTION, startsWith("Expected an offer, not a draft in offer"));
    }

    public static ResponseSpecBuilder shouldBe400WithMissingCommon() {
        return shouldBeCode(SC_BAD_REQUEST).expectContentType(JSON)
                .expectBody(STATUS_PATH, equalTo(STATUS_CODE))
                .expectBody(ERRORS_SIZE, greaterThanOrEqualTo(1))
                .expectBody(ERROR_CODE, equalTo(ILLEGAL_ARGUMENT.value()))
                .expectBody(ERROR_DESCRIPTION, startsWith("missing common "));
    }

    public static ResponseSpecBuilder shouldBe400WithUnableToParseParam(String param, String paramValue) {
        return shouldBeCode(SC_BAD_REQUEST).expectContentType(JSON)
                .expectBody(STATUS_PATH, equalTo(STATUS_CODE))
                .expectBody(ERRORS_SIZE, greaterThanOrEqualTo(1))
                .expectBody(ERROR_CODE, equalTo(ILLEGAL_ARGUMENT.value()))
                .expectBody(ERROR_DESCRIPTION,
                        equalTo(format("Unable to parse param '%s' with value '%s'", param, paramValue)));
    }
}
