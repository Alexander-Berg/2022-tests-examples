#include "parking_api_test_helper.h"

#include <library/cpp/testing/unittest/registar.h>

#include <maps/libs/http/include/http.h>
#include <maps/libs/http/include/test_utils.h>
#include <maps/libs/json/include/value.h>

#include <maps/automotive/parking/fastcgi/parking_api/lib/parking_api.h>

#include <maps/infra/yacare/include/yacare.h>

#include <string>

namespace maps::automotive::parking::tests {

namespace  {
const char* API_PATH_STOP_PARKING = "/stop-parking";
const std::string yaMoneyParkingApiHost = "http://test.org";
const std::string testToken = "1234567890abc";
const std::string testSession = "1234567890";

http::MockHandle addYaMoneyParkingApiMock(int code, int httpCode, const std::string& message)
{
    return maps::http::addMock(
                maps::http::URL(yaMoneyParkingApiHost + API_PATH_STOP_PARKING),
                [code, httpCode, &message](const maps::http::MockRequest&) {
        maps::json::Builder builder;
        builder << errorJson(code, message);

        maps::http::MockResponse r(builder.str());
        r.status = httpCode;
        return r;
    });
}

}

Y_UNIT_TEST_SUITE(test_parking_api_stop_session) {

    Y_UNIT_TEST(stop_session_check_session_header)
    {
        auto mockHandle = maps::http::addMock(
                    maps::http::URL(yaMoneyParkingApiHost + API_PATH_STOP_PARKING),
                    [](const maps::http::MockRequest& request) {

            UNIT_ASSERT_EQUAL(request.header("Authorization"), "Bearer: " + testToken);

            std::string expectedData = R"RAW(
                                       {
                                       "sessionReference":"1234567890"
                                       }
                                       )RAW";

            auto expectedBody = json::Value::fromString(expectedData);
            auto body = json::Value::fromString(request.body);

            UNIT_ASSERT_EQUAL(expectedBody, body);

            std::string data = R"RAW(
                               {
                               "sessionDetails":{
                               "startTime":"2017-07-07T18:04:35.463+03:00",
                               "endTime":"2017-07-07T18:44:35.463+03:00",
                               "refund":{
                               "amount":12.01,
                               "currency":"RUB"
                               }
                               }
                               }
                               )RAW";

            return maps::http::MockResponse(data);
        });

        stopSessionProto(yaMoneyParkingApiHost, testToken, testSession);
    }

    Y_UNIT_TEST(stop_session_check_response)
    {
        auto mockHandle = maps::http::addMock(
                    maps::http::URL(yaMoneyParkingApiHost + API_PATH_STOP_PARKING),
                    [](const maps::http::MockRequest& request) {

            UNIT_ASSERT_EQUAL(request.header("Authorization"), "Bearer: " + testToken);

            std::string data = R"RAW(
                               {
                               "sessionDetails": {
                               "startTime" : "2019-02-10T17:10:00.000",
                               "endTime" : "2019-02-10T17:59:10.000",
                               "refund": {
                               "amount" : 40,
                               "currency": "RUB"
                               }
                               }
                               }
                               )RAW";

            return maps::http::MockResponse(data);
        });

        auto response = stopSessionProto(yaMoneyParkingApiHost, testToken, testSession);

        UNIT_ASSERT_EQUAL(response.start().value(), 1549818600);
        UNIT_ASSERT_EQUAL(response.end().value(), 1549821550);
        UNIT_ASSERT_EQUAL(response.refund().value(), 40.0);
    }

    Y_UNIT_TEST(stop_session_err_parking_api_500)
    {
        auto mockHandle = addYaMoneyParkingApiMock(0, 500, "");

        UNIT_ASSERT_EXCEPTION(stopSessionProto(yaMoneyParkingApiHost, testToken, testSession), maps::RuntimeError);
    }

    Y_UNIT_TEST(stop_session_err_parking_api_invalid_token)
    {
        auto mockHandle = addYaMoneyParkingApiMock(101, 401, "Token is invalid");

        UNIT_ASSERT_EXCEPTION(stopSessionProto(yaMoneyParkingApiHost, testToken, testSession), yacare::errors::Unauthorized);
    }

    Y_UNIT_TEST(stop_session_err_parking_api_invalid_session)
    {
        auto mockHandle = addYaMoneyParkingApiMock(215, 200, "Session is invalid");

        UNIT_ASSERT_EXCEPTION(stopSessionProto(yaMoneyParkingApiHost, testToken, testSession), yacare::errors::UnprocessableEntity);
    }


} //Y_UNIT_TEST_SUITE

} //maps::automotive::parking::tests
