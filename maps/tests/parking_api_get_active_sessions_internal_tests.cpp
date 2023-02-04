#include "parking_api_test_helper.h"

#include <library/cpp/testing/unittest/registar.h>

#include <maps/libs/http/include/http.h>
#include <maps/libs/http/include/test_utils.h>
#include <maps/libs/json/include/value.h>

#include <maps/automotive/parking/fastcgi/parking_api/lib/parking_api.h>

#include <string>

namespace maps::automotive::parking::tests {

namespace  {
const char* API_PATH_GET_ACTIVE_SESSIONS_INTERNAL = "/internal/get-active-sessions";
const std::string yaMoneyParkingApiHost = "http://test";
const UserId uid = 123;
}

Y_UNIT_TEST_SUITE(test_parking_api_get_active_session_internal) {

    Y_UNIT_TEST(check_no_sessions_case)
    {
        auto mockHandle = maps::http::addMock(
                    maps::http::URL(yaMoneyParkingApiHost + API_PATH_GET_ACTIVE_SESSIONS_INTERNAL),
                    [](const maps::http::MockRequest&) {
            return maps::http::MockResponse("{\"sessions\":[]}");
        });

        auto result = parkingSessionExists(yaMoneyParkingApiHost, uid);
        UNIT_ASSERT(result);
        UNIT_ASSERT(!(*result));
    }

    Y_UNIT_TEST(check_single_active_session_case)
    {
        auto mockHandle = maps::http::addMock(
                    maps::http::URL(yaMoneyParkingApiHost + API_PATH_GET_ACTIVE_SESSIONS_INTERNAL),
                    [](const maps::http::MockRequest&) {

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

            return maps::http::MockResponse(data);
        });

        auto result = parkingSessionExists(yaMoneyParkingApiHost, uid);
        UNIT_ASSERT(result);
        UNIT_ASSERT(*result);
    }

    Y_UNIT_TEST(check_single_inactive_session_case)
    {
        auto mockHandle = maps::http::addMock(
                    maps::http::URL(yaMoneyParkingApiHost + API_PATH_GET_ACTIVE_SESSIONS_INTERNAL),
                    [](const maps::http::MockRequest&) {

            std::string data = R"RAW(
                               {"sessions":[
                               {
                               "sessionReference":"135797fa2f7f47ee99b97a10ed957587",
                               "startTime":"2019-02-10T17:10:10.000+03:00",
                               "endTime":"2019-02-10T17:59:10.000+03:00",
                               "serverCurrentTime":"2017-07-01T18:44:35.463+03",
                               "sessionStatus":"expired",
                               "vehicle":{
                               "licensePlate":"a013mp777",
                               "name":"car 1"
                               }}]}
                               )RAW";

            return maps::http::MockResponse(data);
        });

        auto result = parkingSessionExists(yaMoneyParkingApiHost, uid);
        UNIT_ASSERT(result);
        UNIT_ASSERT(!(*result));
    }

    Y_UNIT_TEST(check_multiple_active_sessions_case)
    {
        auto mockHandle = maps::http::addMock(
                    maps::http::URL(yaMoneyParkingApiHost + API_PATH_GET_ACTIVE_SESSIONS_INTERNAL),
                    [&](const maps::http::MockRequest&) {

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

        auto result = parkingSessionExists(yaMoneyParkingApiHost, uid);
        UNIT_ASSERT(result);
        UNIT_ASSERT(*result);
    }

    Y_UNIT_TEST(check_multiple_sessions_single_active_case)
    {
        auto mockHandle = maps::http::addMock(
                    maps::http::URL(yaMoneyParkingApiHost + API_PATH_GET_ACTIVE_SESSIONS_INTERNAL),
                    [&](const maps::http::MockRequest&) {

            std::string data = R"RAW(
                               {"sessions":[
                               {
                               "sessionReference":"135797fa2f7f47ee99b97a10ed957587",
                               "startTime":"2019-02-10T17:10:10.000+03:00",
                               "endTime":"2019-02-10T17:59:10.000+03:00",
                               "serverCurrentTime":"2017-07-01T18:44:35.463+03:00",
                               "sessionStatus":"expired",
                               "vehicle":{
                               "licensePlate":"a013mp777",
                               "name":"car 1"
                               }},
                               {
                               "sessionReference":"135797fa2f7f47ee99b97a10bb000000",
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

        auto result = parkingSessionExists(yaMoneyParkingApiHost, uid);
        UNIT_ASSERT(result);
        UNIT_ASSERT(*result);
    }

    Y_UNIT_TEST(check_multiple_inactive_sessions_case)
    {
        auto mockHandle = maps::http::addMock(
                    maps::http::URL(yaMoneyParkingApiHost + API_PATH_GET_ACTIVE_SESSIONS_INTERNAL),
                    [&](const maps::http::MockRequest&) {

            std::string data = R"RAW(
                               {"sessions":[
                               {
                               "sessionReference":"135797fa2f7f47ee99b97a10ed957587",
                               "startTime":"2019-02-10T17:10:10.000+03:00",
                               "endTime":"2019-02-10T17:59:10.000+03:00",
                               "serverCurrentTime":"2017-07-01T18:44:35.463+03:00",
                               "sessionStatus":"expired",
                               "vehicle":{
                               "licensePlate":"a013mp777",
                               "name":"car 1"
                               }},
                               {
                               "sessionReference":"135797fa2f7f47ee99b97a10bb000000",
                               "startTime":"2017-07-01T18:14:35.463+03:00",
                               "endTime":"2017-07-01T19:14:35.463+03:00",
                               "serverCurrentTime":"2017-07-01T18:44:35.463+03:00",
                               "sessionStatus":"expired",
                               "vehicle":{
                               "licensePlate":"a100mp777",
                               "name":"car 2"
                               }}]}
                               )RAW";

            return maps::http::MockResponse(data);
        });

        auto result = parkingSessionExists(yaMoneyParkingApiHost, uid);
        UNIT_ASSERT(result);
        UNIT_ASSERT(!(*result));
    }

    Y_UNIT_TEST(check_no_status_case)
    {
        auto mockHandle = maps::http::addMock(
                    maps::http::URL(yaMoneyParkingApiHost + API_PATH_GET_ACTIVE_SESSIONS_INTERNAL),
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

        auto result = parkingSessionExists(yaMoneyParkingApiHost, uid);
        UNIT_ASSERT(!result);
    }

    Y_UNIT_TEST(check_no_sessions_field_case)
    {
        auto mockHandle = maps::http::addMock(
                    maps::http::URL(yaMoneyParkingApiHost + API_PATH_GET_ACTIVE_SESSIONS_INTERNAL),
                    [](const maps::http::MockRequest&) {
            std::string data = "{}";

            return maps::http::MockResponse(data);
        });

        auto result = parkingSessionExists(yaMoneyParkingApiHost, uid);
        UNIT_ASSERT(!result);
    }

    Y_UNIT_TEST(check_json_error_response_case)
    {
        auto mockHandle = maps::http::addMock(
                    maps::http::URL(yaMoneyParkingApiHost + API_PATH_GET_ACTIVE_SESSIONS_INTERNAL),
                    [](const maps::http::MockRequest&) {
            maps::json::Builder builder;
            builder << errorJson(0, "");
            return maps::http::MockResponse(builder.str());
        });

        auto result = parkingSessionExists(yaMoneyParkingApiHost, uid);
        UNIT_ASSERT(!result);
    }

    Y_UNIT_TEST(check_http_error_response_case)
    {
        auto mockHandle = maps::http::addMock(
                    maps::http::URL(yaMoneyParkingApiHost + API_PATH_GET_ACTIVE_SESSIONS_INTERNAL),
                    [](const maps::http::MockRequest&) {
            maps::http::MockResponse r;
            r.status = 500;
            return r;
        });

        auto result = parkingSessionExists(yaMoneyParkingApiHost, uid);
        UNIT_ASSERT(!result);
    }

} //Y_UNIT_TEST_SUITE

} //maps::automotive::parking::tests
