package ru.auto.tests.publicapi.ra;

import io.restassured.builder.ResponseSpecBuilder;

import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.AUTH_ERROR;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.BAD_REQUEST;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.BAD_PARAMS_DETAILS;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.DEVICE_NOT_FOUND;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.DRAFT_NOT_FOUND;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.NO_AUTH;
import static ru.auto.tests.publicapi.model.AutoApiErrorResponse.ErrorEnum.SEARCH_QUERY_NOT_VAILD;
import static ru.auto.tests.publicapi.model.AutoApiLoginForbiddenErrorResponse.ErrorEnum.CLIENT_LOGIN_NOT_ALLOWED;
import static ru.auto.tests.publicapi.model.AutoApiPasswordValidationErrorResponse.ErrorEnum.PASSWORD_AUTH_REQUIRED;
import static ru.auto.tests.publicapi.model.AutoApiSuccessResponse.StatusEnum.ERROR;
import static ru.auto.tests.publicapi.model.AutoApiSuccessResponse.StatusEnum.SUCCESS;

/**
 * Created by vicdev on 18.09.17.
 */
public class ResponseSpecBuilders {

    private static final String STATUS_PATH = "status";
    private static final String ERROR_PATH = "error";
    private static final String DETAILED_ERROR_PATH = "detailed_error";

    private ResponseSpecBuilders() {
    }

    public static ResponseSpecBuilder shouldBeSuccess() {
        return shouldBe200OkJSON().expectBody(STATUS_PATH,
                equalTo(SUCCESS.name()));
    }

    public static ResponseSpecBuilder shouldBe401NeedAuthentication() {
        return shouldBeCode(SC_UNAUTHORIZED).expectContentType(JSON)
                .expectBody(ERROR_PATH, equalTo(NO_AUTH.name()))
                .expectBody(STATUS_PATH, equalTo(ERROR.name()))
                .expectBody(DETAILED_ERROR_PATH, equalTo("Need authentication"));
    }

    public static ResponseSpecBuilder shouldBe401AuthError() {
        return shouldBeCode(SC_UNAUTHORIZED).expectContentType(JSON)
                .expectBody(ERROR_PATH, equalTo(AUTH_ERROR.name()))
                .expectBody(STATUS_PATH, equalTo(ERROR.name()))
                .expectBody(DETAILED_ERROR_PATH, equalTo(AUTH_ERROR.name()));
    }

    public static ResponseSpecBuilder shouldBe401NoAuth() {
        return shouldBeCode(SC_UNAUTHORIZED).expectContentType(JSON)
                .expectBody(ERROR_PATH, equalTo(NO_AUTH.name()))
                .expectBody(STATUS_PATH, equalTo(ERROR.name()))
                .expectBody(DETAILED_ERROR_PATH, equalTo("Expected private user. But AnonymousUser. Provide valid session_id"));
    }

    public static ResponseSpecBuilder shouldBe400UnknownCategoryError(String category) {
        return shouldBeCode(SC_BAD_REQUEST).expectContentType(JSON)
                .expectBody(ERROR_PATH, equalTo(BAD_REQUEST.name()))
                .expectBody(STATUS_PATH, equalTo(ERROR.name()))
                .expectBody(DETAILED_ERROR_PATH, containsString(String.format("Unknown category selector: [%s]. Known values: cars, moto, trucks, all", category)));
    }

    public static ResponseSpecBuilder shouldBe400IncorrectOfferIdError(String offerId) {
        return shouldBeCode(SC_BAD_REQUEST).expectContentType(JSON)
                .expectBody(ERROR_PATH, equalTo(BAD_REQUEST.name()))
                .expectBody(STATUS_PATH, equalTo(ERROR.name()))
                .expectBody(DETAILED_ERROR_PATH, containsString(String.format("Incorrect offer id: [%s]", offerId)));
    }

    public static ResponseSpecBuilder shouldBe400InvalidIdFormatError(String id) {
        return shouldBeCode(SC_BAD_REQUEST).expectContentType(JSON)
                .expectBody(ERROR_PATH, equalTo(BAD_PARAMS_DETAILS.name()))
                .expectBody(STATUS_PATH, equalTo(ERROR.name()))
                .expectBody(DETAILED_ERROR_PATH, containsString(String.format("invalid id format: %s", id)));
    }

    public static ResponseSpecBuilder shouldBe403ClientLoginNotAllowedError() {
        return shouldBeCode(SC_FORBIDDEN).expectContentType(JSON)
                .expectBody(ERROR_PATH, equalTo(CLIENT_LOGIN_NOT_ALLOWED.name()))
                .expectBody(STATUS_PATH, equalTo(ERROR.name()))
                .expectBody(DETAILED_ERROR_PATH, equalTo(CLIENT_LOGIN_NOT_ALLOWED.name()));

    }

    public static ResponseSpecBuilder shouldBe403PasswordRequiredError() {
        return shouldBeCode(SC_FORBIDDEN).expectContentType(JSON)
                .expectBody(ERROR_PATH, equalTo(PASSWORD_AUTH_REQUIRED.name()))
                .expectBody(STATUS_PATH, equalTo(ERROR.name()))
                .expectBody(DETAILED_ERROR_PATH, equalTo(PASSWORD_AUTH_REQUIRED.name()));

    }

    public static ResponseSpecBuilder shouldBe404DraftNotFound() {
        return shouldBeCode(SC_NOT_FOUND).expectContentType(JSON)
                .expectBody(ERROR_PATH, equalTo(DRAFT_NOT_FOUND.name()))
                .expectBody(STATUS_PATH, equalTo(ERROR.name()))
                .expectBody(DETAILED_ERROR_PATH, equalTo(DRAFT_NOT_FOUND.name()));
    }

    public static ResponseSpecBuilder shouldBe404DeviceNotFound() {
        return shouldBeCode(SC_NOT_FOUND).expectContentType(JSON)
                .expectBody(ERROR_PATH, equalTo(DEVICE_NOT_FOUND.name()))
                .expectBody(STATUS_PATH, equalTo(ERROR.name()))
                .expectBody(DETAILED_ERROR_PATH, equalTo(DEVICE_NOT_FOUND.name()));
    }

    public static ResponseSpecBuilder shouldBe400SearchQueryNotValidError(String firstInvalidParameterName, String invalidValue) {
        return shouldBeCode(SC_BAD_REQUEST).expectContentType(JSON)
                .expectBody(ERROR_PATH, equalTo(SEARCH_QUERY_NOT_VAILD.name()))
                .expectBody(STATUS_PATH, equalTo(ERROR.name()))
                .expectBody(DETAILED_ERROR_PATH,
                        equalTo(String.format("Invalid value: %s for parameter: %s", invalidValue, firstInvalidParameterName)));
    }

    public static ResponseSpecBuilder shouldBe400MalformedParameterError(String invalidParameterName) {
        return shouldBeCode(SC_BAD_REQUEST).expectContentType(JSON)
                .expectBody(ERROR_PATH, equalTo(BAD_REQUEST.name()))
                .expectBody(STATUS_PATH, equalTo(ERROR.name()))
                .expectBody(DETAILED_ERROR_PATH,
                        containsString(invalidParameterName))
                .expectBody(DETAILED_ERROR_PATH,
                        containsString("malformed"));
    }

    public static ResponseSpecBuilder shouldBe400NonPositiveValueError(String invalidParameterName) {
        return shouldBeCode(SC_BAD_REQUEST).expectContentType(JSON)
                .expectBody(ERROR_PATH, equalTo(BAD_REQUEST.name()))
                .expectBody(STATUS_PATH, equalTo(ERROR.name()))
                .expectBody(DETAILED_ERROR_PATH,
                        containsString(invalidParameterName))
                .expectBody(DETAILED_ERROR_PATH,
                        containsString("should be positive"));

    }

    public static ResponseSpecBuilder shouldBeEmptyJson() {
        return shouldBe200OkJSON().expectBody(equalTo("{}"));
    }
}
