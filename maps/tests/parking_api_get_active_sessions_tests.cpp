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
const char* API_PATH_GET_ACTIVE_SESSIONS = "/get-active-sessions";
const std::string yaMoneyParkingApiHost = "http://test";
const std::string testToken = "1234567890abc";

http::MockHandle addYaMoneyParkingApiMock(int code, int httpCode, const std::string& message)
{
    return maps::http::addMock(
                maps::http::URL(yaMoneyParkingApiHost + API_PATH_GET_ACTIVE_SESSIONS),
                [code, httpCode, &message](const maps::http::MockRequest&) {
        maps::json::Builder builder;
        builder << errorJson(code, message);

        maps::http::MockResponse r(builder.str());
        r.status = httpCode;
        return r;
    });
}

}

Y_UNIT_TEST_SUITE(test_parking_api_get_active_session) {

    Y_UNIT_TEST(get_active_sessions_empty_list)
    {
        auto mockHandle = maps::http::addMock(
                    maps::http::URL(yaMoneyParkingApiHost + API_PATH_GET_ACTIVE_SESSIONS),
                    [](const maps::http::MockRequest& request) {
            UNIT_ASSERT_EQUAL(request.header("Authorization"), "Bearer: " + testToken);

            return maps::http::MockResponse("{\"sessions\":[]}");
        });

        auto response = activeSessionsProto(yaMoneyParkingApiHost, testToken);

        UNIT_ASSERT_EQUAL(response.sessionSize(), 0);
    }

    Y_UNIT_TEST(get_active_sessions_single_session)
    {
        auto mockHandle = maps::http::addMock(
                    maps::http::URL(yaMoneyParkingApiHost + API_PATH_GET_ACTIVE_SESSIONS),
                    [](const maps::http::MockRequest& request) {

            std::string data = R"RAW(
                               {"sessions":[
                               {
                               "sessionReference":"135797fa2f7f47ee99b97a10ed957587",
                               "startTime":"2019-02-10T17:10:10.000+03:00",
                               "endTime":"2019-02-10T17:59:10.000+03:00",
                               "serverCurrentTime":"2017-07-01T18:44:35.463+03",
                               "sessionStatus":"active",
                               "vehicle":{
                               "licensePlate":"a013mp777",
                               "name":"car 1"
                               }}]}
                               )RAW";

            UNIT_ASSERT_EQUAL(request.header("Authorization"), "Bearer: " + testToken);

            return maps::http::MockResponse(data);
        });

        auto response = activeSessionsProto(yaMoneyParkingApiHost, testToken);

        UNIT_ASSERT_EQUAL(response.sessionSize(), 1);
        auto& session = response.Getsession(0);
        UNIT_ASSERT_EQUAL(session.GetexternalId(), "135797fa2f7f47ee99b97a10ed957587");
        UNIT_ASSERT_EQUAL(session.start().value(), 1549807810);
        UNIT_ASSERT_EQUAL(session.start().tz_offset(), 10800);
        UNIT_ASSERT_EQUAL(session.start().text(), "17:10");
        UNIT_ASSERT_EQUAL(session.end().value(), 1549810750);
        UNIT_ASSERT_EQUAL(session.end().tz_offset(), 10800);
        UNIT_ASSERT_EQUAL(session.end().text(), "17:59");
    }

    Y_UNIT_TEST(get_active_sessions_multi_sessions)
    {
        auto mockHandle = maps::http::addMock(
                    maps::http::URL(yaMoneyParkingApiHost + API_PATH_GET_ACTIVE_SESSIONS),
                    [&](const maps::http::MockRequest& request) {
            UNIT_ASSERT_EQUAL(request.header("Authorization"), "Bearer: " + testToken);

            std::string data = R"RAW(
                               {"sessions":[
                               {
                               "sessionReference":"135797fa2f7f47ee99b97a10ed957587",
                               "startTime":"2017-07-01T18:04:35.463+03:00",
                               "endTime":"2017-07-01T19:04:35.463+03:00",
                               "serverCurrentTime":"2017-07-01T18:44:35.463+03:00",
                               "sessionStatus":"active",
                               "vehicle":{
                               "licensePlate":"a013mp777",
                               "name":"car 1"
                               }},
                               {
                               "sessionReference":"135797fa2f7f47ee99b97a10ed951234",
                               "startTime":"2017-07-01T18:14:35.463+03:00",
                               "endTime":"2017-07-01T19:14:35.463+03:00",
                               "serverCurrentTime":"2017-07-01T18:44:35.463+03:00",
                               "sessionStatus":"active",
                               "vehicle":{
                               "licensePlate":"a100mp777",
                               "name":"car 2"
                               }}]}
                               )RAW";

            return maps::http::MockResponse(data);
        });

        auto response = activeSessionsProto(yaMoneyParkingApiHost, testToken);
        UNIT_ASSERT_EQUAL(response.sessionSize(), 2);
        auto& session = response.Getsession(1);
        UNIT_ASSERT_EQUAL(session.GetexternalId(), "135797fa2f7f47ee99b97a10ed951234");
        UNIT_ASSERT_EQUAL(session.start().value(), 1498922075);
        UNIT_ASSERT_EQUAL(session.start().tz_offset(), 10800);
        UNIT_ASSERT_EQUAL(session.start().text(), "18:14");
        UNIT_ASSERT_EQUAL(session.end().value(), 1498925675);
        UNIT_ASSERT_EQUAL(session.end().tz_offset(), 10800);
        UNIT_ASSERT_EQUAL(session.end().text(), "19:14");
    }

    Y_UNIT_TEST(get_active_sessions_multi_sessions_processing_status)
    {
        auto mockHandle = maps::http::addMock(
                    maps::http::URL(yaMoneyParkingApiHost + API_PATH_GET_ACTIVE_SESSIONS),
                    [&](const maps::http::MockRequest& request) {
            UNIT_ASSERT_EQUAL(request.header("Authorization"), "Bearer: " + testToken);

            std::string data = R"RAW(
                               {"sessions":[
                               {
                               "sessionReference":"135797fa2f7f47ee99b97a10ed957587",
                               "startTime":"2019-02-10T17:10:10.000+03:00",
                               "endTime":"2019-02-10T17:59:10.000+03:00",
                               "serverCurrentTime":"2017-07-01T18:44:35.463+03:00",
                               "sessionStatus":"active",
                               "vehicle":{
                               "licensePlate":"a013mp777",
                               "name":"car 1"
                               }},
                               {
                               "sessionReference":"135797fa2f7f47ee99b97a10bb000000",
                               "startTime":"2017-07-01T18:14:35.463+03:00",
                               "endTime":"2017-07-01T19:14:35.463+03:00",
                               "serverCurrentTime":"2017-07-01T18:44:35.463+03:00",
                               "sessionStatus":"processing",
                               "vehicle":{
                               "licensePlate":"a100mp777",
                               "name":"car 2"
                               }}]}
                               )RAW";

            return maps::http::MockResponse(data);
        });

        auto response = activeSessionsProto(yaMoneyParkingApiHost, testToken);
        UNIT_ASSERT_EQUAL(response.sessionSize(), 1);
        auto& session = response.Getsession(0);
        UNIT_ASSERT_EQUAL(session.GetexternalId(), "135797fa2f7f47ee99b97a10ed957587");
        UNIT_ASSERT_EQUAL(session.start().value(), 1549807810);
        UNIT_ASSERT_EQUAL(session.start().tz_offset(), 10800);
        UNIT_ASSERT_EQUAL(session.start().text(), "17:10");
        UNIT_ASSERT_EQUAL(session.end().value(), 1549810750);
        UNIT_ASSERT_EQUAL(session.end().tz_offset(), 10800);
        UNIT_ASSERT_EQUAL(session.end().text(), "17:59");
    }

    Y_UNIT_TEST(get_active_sessions_err_invalid_token)
    {
        auto mockHandle = addYaMoneyParkingApiMock(101, 401, "Token is invalid");

        UNIT_ASSERT_EXCEPTION(activeSessionsProto(yaMoneyParkingApiHost, testToken), yacare::errors::Unauthorized);
    }

    Y_UNIT_TEST(get_active_sessions_err_500)
    {
        auto mockHandle = addYaMoneyParkingApiMock(0, 500, "");

        UNIT_ASSERT_EXCEPTION(activeSessionsProto(yaMoneyParkingApiHost, testToken), maps::RuntimeError);
    }

    Y_UNIT_TEST(get_active_sessions_err_data_no_reference)
    {
        auto mockHandle = maps::http::addMock(
                    maps::http::URL(yaMoneyParkingApiHost + API_PATH_GET_ACTIVE_SESSIONS),
                    [](const maps::http::MockRequest&) {
            std::string data = R"RAW(
                               {"sessions":[
                               {
                               "startTime":"2017-07-01T18:04:35.463+03:00",
                               "endTime":"2017-07-01T19:04:35.463+03:00",
                               "serverCurrentTime":"2017-07-01T18:44:35.463+03:00",
                               "sessionStatus":"active",
                               "vehicle":{
                               "licensePlate":"a013mp777",
                               "name":"car 1"
                               }}]}
                               )RAW";

            return maps::http::MockResponse(data);
        });

        UNIT_ASSERT_EXCEPTION(activeSessionsProto(yaMoneyParkingApiHost, testToken), maps::RuntimeError);
    }

    Y_UNIT_TEST(get_active_sessions_err_data_no_start_time)
    {
        auto mockHandle = maps::http::addMock(
                    maps::http::URL(yaMoneyParkingApiHost + API_PATH_GET_ACTIVE_SESSIONS),
                    [](const maps::http::MockRequest&) {
            std::string data = R"RAW(
                               {"sessions":[
                               {
                               "sessionReference":"135797fa2f7f47ee99b97a10ed957587",
                               "endTime":"2017-07-01T19:04:35.463+03:00",
                               "serverCurrentTime":"2017-07-01T18:44:35.463+03:00",
                               "sessionStatus":"active",
                               "vehicle":{
                               "licensePlate":"a013mp777",
                               "name":"car 1"
                               }}]}
                               )RAW";

            return maps::http::MockResponse(data);
        });

        UNIT_ASSERT_EXCEPTION(activeSessionsProto(yaMoneyParkingApiHost, testToken), maps::RuntimeError);
    }

    Y_UNIT_TEST(get_active_sessions_err_data_no_end_time)
    {
        auto mockHandle = maps::http::addMock(
                    maps::http::URL(yaMoneyParkingApiHost + API_PATH_GET_ACTIVE_SESSIONS),
                    [](const maps::http::MockRequest&) {
            std::string data = R"RAW(
                               {"sessions":[
                               {
                               "sessionReference":"135797fa2f7f47ee99b97a10ed957587",
                               "startTime":"2017-07-01T18:04:35.463+03:00",
                               "serverCurrentTime":"2017-07-01T18:44:35.463+03:00",
                               "sessionStatus":"active",
                               "vehicle":{
                               "licensePlate":"a013mp777",
                               "name":"car 1"
                               }}]}
                               )RAW";

            return maps::http::MockResponse(data);
        });

        UNIT_ASSERT_EXCEPTION(activeSessionsProto(yaMoneyParkingApiHost, testToken), maps::RuntimeError);
    }

    Y_UNIT_TEST(get_active_sessions_err_data_no_status)
    {
        auto mockHandle = maps::http::addMock(
                    maps::http::URL(yaMoneyParkingApiHost + API_PATH_GET_ACTIVE_SESSIONS),
                    [](const maps::http::MockRequest&) {
            std::string data = R"RAW(
                               {"sessions":[
                               {
                               "sessionReference":"135797fa2f7f47ee99b97a10ed957587",
                               "startTime":"2017-07-01T18:04:35.463+03:00",
                               "endTime":"2017-07-01T18:44:35.463+03:00",
                               "serverCurrentTime":"2017-07-01T18:44:35.463+03:00",
                               "vehicle":{
                               "licensePlate":"a013mp777",
                               "name":"car 1"
                               }}]}
                               )RAW";

            return maps::http::MockResponse(data);
        });

        UNIT_ASSERT_EXCEPTION(activeSessionsProto(yaMoneyParkingApiHost, testToken), maps::RuntimeError);
    }

} //Y_UNIT_TEST_SUITE

} //maps::automotive::parking::tests
