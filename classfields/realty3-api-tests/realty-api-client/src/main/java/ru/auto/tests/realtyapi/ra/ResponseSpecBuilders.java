package ru.auto.tests.realtyapi.ra;

import io.restassured.builder.ResponseSpecBuilder;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBe200OkJSON;
import static ru.auto.tests.commons.restassured.ResponseSpecBuilders.shouldBeCode;
import static ru.auto.tests.realtyapi.enums.PhoneError.PHONE_BADCODE;
import static ru.auto.tests.realtyapi.enums.PhoneError.PHONE_BADNUMFORMAT;
import static ru.auto.tests.realtyapi.enums.Status.OK;

/**
 * Created by vicdev on 18.09.17.
 */
public class ResponseSpecBuilders {

    private static final String STATUS_PATH = "response.status";
    public static final String ERROR_CODENAME = "error.codename";
    private static final String ERROR_CODE = "error.code";
    private static final String ERROR_DESCRIPTION = "error.message";
    public static final String ERROR_DATA_DESCRIPTION = "error.data.description";

    private ResponseSpecBuilders() {
    }

    public static ResponseSpecBuilder shouldBeOK() {
        return shouldBe200OkJSON().expectBody(STATUS_PATH,
                equalTo(OK.value()));
    }

    public static ResponseSpecBuilder shouldBeAcceptableCodeForMissingPathElement() {
        return new ResponseSpecBuilder().expectStatusCode(MissingPathElementErrorCodeMatcher.INSTANCE);
    }

    private static class MissingPathElementErrorCodeMatcher extends TypeSafeMatcher<Integer> {
        static final MissingPathElementErrorCodeMatcher INSTANCE = new MissingPathElementErrorCodeMatcher();

        @Override
        protected boolean matchesSafely(Integer item) {
            return item != null && (item == 400 || item == 404);
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("a client error status code");
        }
    }

    public static ResponseSpecBuilder shouldBe400PhoneBadFormat(String number) {
        return shouldBeCode(SC_BAD_REQUEST)
                .expectBody(ERROR_CODENAME, equalTo(PHONE_BADNUMFORMAT.name()))
                .expectBody("error.data.number", equalTo(number));
    }

    public static ResponseSpecBuilder shouldBe400PhoneBadNumFormat() {
        return shouldBeCode(SC_BAD_REQUEST)
                .expectBody(ERROR_CODE, equalTo("PHONE_BAD_NUM_FORMAT"));
    }

    public static ResponseSpecBuilder shouldBe400PhoneBadCode(String code) {
        return shouldBeCode(SC_BAD_REQUEST)
                .expectBody(ERROR_CODENAME, equalTo(PHONE_BADCODE.name()))
                .expectBody("error.data.phone_code", equalTo(code));
    }

    public static ResponseSpecBuilder shouldBe400PhoneBadConfiramtionCode() {
        return shouldBeCode(SC_BAD_REQUEST)
                .expectBody(ERROR_CODE, equalTo("PHONE_BAD_CONFIRMATION_CODE"));
    }

    public static ResponseSpecBuilder shouldBeValid() {
        return shouldBe200OkJSON()
                .expectBody("response.valid", equalTo(true));
    }

    public static ResponseSpecBuilder shouldBe404UnknownVosUser(String uid) {
        return shouldBeCode(SC_NOT_FOUND)
                .expectBody(ERROR_CODENAME, equalTo("UNKNOWN_VOS_USER"))
                .expectBody(ERROR_DATA_DESCRIPTION, equalTo(String.format("User uid_%s not found", uid)));
    }

    public static ResponseSpecBuilder shouldBe404RequestedHandlerNotBeFound() {
        return shouldBeCode(SC_NOT_FOUND)
                .expectBody(ERROR_CODENAME, equalTo("NOT_FOUND"))
                .expectBody(ERROR_DATA_DESCRIPTION, equalTo("The requested handler could not be found." +
                        " Please check method and url of the request."));
    }

    public static ResponseSpecBuilder shouldBe404OfferNotFound(String uid, String offerId) {
        return shouldBeCode(SC_NOT_FOUND)
                .expectBody(ERROR_CODENAME, equalTo("OFFER_NOT_FOUND"))
                .expectBody(ERROR_DATA_DESCRIPTION, equalTo(String.format("Offer not found: uid_%s:%s"
                        , uid, offerId)));
    }

    public static ResponseSpecBuilder shouldBe404ChatNotFound() {
        return shouldBeCode(SC_NOT_FOUND)
                .expectBody(ERROR_CODE, equalTo("CHAT_NOT_FOUND"));
    }

    public static ResponseSpecBuilder shouldBe404SiteNonFound(String siteId) {
        return shouldBeCode(SC_NOT_FOUND)
                .expectBody(ERROR_CODE, equalTo("NOT_FOUND"))
                .expectBody(ERROR_DESCRIPTION, equalTo(String.format("Site %s not found", siteId)));
    }

    public static ResponseSpecBuilder shouldBe400NoDeveloperWithChat(String siteId) {
        return shouldBeCode(SC_BAD_REQUEST)
                .expectBody(ERROR_CODE, equalTo("BAD_REQUEST"))
                .expectBody(ERROR_DESCRIPTION, equalTo(String.format("Site %s has no developer with chat", siteId)));
    }

    public static ResponseSpecBuilder shouldBe403ForbiddenForOfferWithoutChat(String offerId) {
        return shouldBeCode(SC_FORBIDDEN)
                .expectBody(ERROR_CODE, equalTo("FORBIDDEN"))
                .expectBody(ERROR_DESCRIPTION,
                        equalTo(String.format("It's not allowed to create chat room for the offer %s", offerId)));
    }

    //REALTY-13891
    public static ResponseSpecBuilder shouldBe404OfferNotFound(String offerId) {
        return shouldBeCode(SC_NOT_FOUND)
                .expectBody(ERROR_CODENAME, equalTo("OFFER_NOT_FOUND"))
                .expectBody(ERROR_DATA_DESCRIPTION, equalTo(String.format("Offer i_%s is not found", offerId)));
    }

    public static ResponseSpecBuilder shouldBe401WithPrivateUserExpected() {
        return shouldBeCode(SC_UNAUTHORIZED)
                .expectBody(ERROR_CODENAME, equalTo("INVALID_PARAMS"))
                .expectBody(ERROR_DATA_DESCRIPTION, equalTo("Private user expected after login"));
    }

    public static ResponseSpecBuilder shouldBe404OfferNotFoundInVos(String offerId) {
        return shouldBeCode(SC_NOT_FOUND)
                .expectBody("error.codename", equalTo("OFFER_NOT_FOUND"))
                .expectBody("error.data.description", equalTo(String.format("%s not found in VOS"
                        , offerId)));
    }

    public static ResponseSpecBuilder shouldSubscriptionNotFound() {
        return shouldBeCode(SC_NOT_FOUND)
                .expectBody("error.codename", equalTo("SUBSCRIPTION_NOT_FOUND"))
                .expectBody("error.code", equalTo("UNKNOWN_ERROR"));
    }

    public static ResponseSpecBuilder shouldBe404WithRequestedResourceCouldNotBeFound() {
        return shouldBeCode(SC_NOT_FOUND)
                .expectBody("error.codename", equalTo("SUBSCRIPTION_NOT_FOUND"))
                .expectBody("error.code", equalTo("UNKNOWN_ERROR"))
                .expectBody("error.data.description", equalTo("The requested resource could " +
                        "not be found but may be available again in the future."));
    }

    public static ResponseSpecBuilder shouldBe500WithInternalError() {
        return shouldBeCode(SC_INTERNAL_SERVER_ERROR)
                .expectBody("error.codename", equalTo("INTERNAL_ERROR"))
                .expectBody("error.code", equalTo("UNKNOWN_ERROR"))
                .expectBody("error.data.description", equalTo("None.get"));
    }

    public static ResponseSpecBuilder shouldBe400WithInvalidParams() {
        return shouldBeCode(SC_BAD_REQUEST)
                .expectBody("error.codename", equalTo("INVALID_PARAMS"))
                .expectBody("error.data.message",
                        equalTo("{\"obj.title\":[{\"msg\":[\"error.path.missing\"],\"args\":[]}]," +
                                "\"obj.token\":[{\"msg\":[\"error.path.missing\"],\"args\":[]}]," +
                                "\"obj.period\":[{\"msg\":[\"error.path.missing\"],\"args\":[]}]}"));
    }

    public static ResponseSpecBuilder shouldBe500WithUnrecognizedToken() {
        return shouldBeCode(SC_INTERNAL_SERVER_ERROR)
                .expectBody("error.codename", equalTo("INTERNAL_ERROR"))
                .expectBody("error.code", equalTo("UNKNOWN_ERROR"))
                .expectBody("error.data.description", startsWith("Unrecognized token"));
    }

    public static ResponseSpecBuilder shouldBe400WithNotValid64bitInteger(String uid) {
        return shouldBeCode(SC_BAD_REQUEST)
                .expectBody(ERROR_CODENAME, equalTo("Wrong status code"))
                .expectBody(ERROR_DATA_DESCRIPTION, equalTo(String.format("Unexpected response: HTTP/1.1 " +
                        "400 Bad Request, body = The query parameter 'user' was malformed:\n'%s' " +
                        "is not a valid 64-bit integer value", uid)));
    }

    public static ResponseSpecBuilder shouldBe400WithInvalidEmptyParams() {
        return shouldBeCode(SC_BAD_REQUEST)
                .expectBody("error.codename", equalTo("INVALID_PARAMS"));
    }

    public static ResponseSpecBuilder shouldBe500WithArchiveError() {
        return shouldBeCode(SC_INTERNAL_SERVER_ERROR)
                .expectBody(ERROR_CODENAME, equalTo("ARCHIVE_ERROR"));
    }
}
