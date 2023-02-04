#include <yandex_io/protos/enum_names/enum_names.h>

#include <yandex_io/protos/quasar_proto.pb.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace quasar;
using namespace quasar::proto;

Y_UNIT_TEST_SUITE(ProtobufUtilsTests) {

    Y_UNIT_TEST(testWifiConnectResponseStatusName) {
        static_assert(WifiConnectResponse_Status_Status_MAX == WifiConnectResponse_Status_TIMEOUT);

        UNIT_ASSERT_VALUES_EQUAL(wifiConnectResponseStatusName(WifiConnectResponse::OK), "OK");
        UNIT_ASSERT_VALUES_EQUAL(wifiConnectResponseStatusName(WifiConnectResponse::SSID_NOT_FOUND), "SSID_NOT_FOUND");
        UNIT_ASSERT_VALUES_EQUAL(wifiConnectResponseStatusName(WifiConnectResponse::AUTH_ERROR), "AUTH_ERROR");
        UNIT_ASSERT_VALUES_EQUAL(wifiConnectResponseStatusName(WifiConnectResponse::TIMEOUT), "TIMEOUT");
    }

    Y_UNIT_TEST(testAddUserResponseStatusName) {
        static_assert(AddUserResponse_Status_Status_MAX == AddUserResponse_Status_INVALID_TOKEN);

        UNIT_ASSERT_VALUES_EQUAL(addUserResponseStatusName(AddUserResponse::OK), "OK");
        UNIT_ASSERT_VALUES_EQUAL(addUserResponseStatusName(AddUserResponse::NO_INTERNET), "NO_INTERNET");
        UNIT_ASSERT_VALUES_EQUAL(addUserResponseStatusName(AddUserResponse::CODE_EXPIRED), "CODE_EXPIRED");
        UNIT_ASSERT_VALUES_EQUAL(addUserResponseStatusName(AddUserResponse::CRYPTO_ERROR), "CRYPTO_ERROR");
        UNIT_ASSERT_VALUES_EQUAL(addUserResponseStatusName(AddUserResponse::INVALID_TOKEN), "INVALID_TOKEN");
    }

    Y_UNIT_TEST(testWifiTypeName) {
        static_assert(WifiType_MAX == WifiType::NONE_WIFI_TYPE);
        UNIT_ASSERT_VALUES_EQUAL(wifiTypeName(WifiType::WEP), "WEP");
        UNIT_ASSERT_VALUES_EQUAL(wifiTypeName(WifiType::WPA), "WPA");
        UNIT_ASSERT_VALUES_EQUAL(wifiTypeName(WifiType::OPEN), "OPEN");
        UNIT_ASSERT_VALUES_EQUAL(wifiTypeName(WifiType::UNKNOWN_WIFI_TYPE), "UNKNOWN");
        UNIT_ASSERT_VALUES_EQUAL(wifiTypeName(WifiType::NONE_WIFI_TYPE), "NONE");
    }

    Y_UNIT_TEST(testWifiTypeParse) {
        static_assert(WifiType_MAX == WifiType::NONE_WIFI_TYPE);
        auto CHECK_EQUAL = [](auto a, auto b) {
            WifiType type;
            wifiTypeParse(a, &type);
            UNIT_ASSERT_EQUAL(type, b);
        };

        CHECK_EQUAL("WEP", WifiType::WEP);
        CHECK_EQUAL("WPA", WifiType::WPA);
        CHECK_EQUAL("OPEN", WifiType::OPEN);
        CHECK_EQUAL("UNKNOWN", WifiType::UNKNOWN_WIFI_TYPE);
        CHECK_EQUAL("NONE", WifiType::NONE_WIFI_TYPE);
    }

    Y_UNIT_TEST(testWifiStatusStatusName) {
        static_assert(WifiStatus_Status_Status_MAX == WifiStatus_Status_NOT_CHOSEN);

        UNIT_ASSERT_VALUES_EQUAL(wifiStatusStatusName(WifiStatus::NOT_CONNECTED), "NOT_CONNECTED");
        UNIT_ASSERT_VALUES_EQUAL(wifiStatusStatusName(WifiStatus::CONNECTING), "CONNECTING");
        UNIT_ASSERT_VALUES_EQUAL(wifiStatusStatusName(WifiStatus::CONNECTED_NO_INTERNET), "CONNECTED_NO_INTERNET");
        UNIT_ASSERT_VALUES_EQUAL(wifiStatusStatusName(WifiStatus::CONNECTED), "CONNECTED");
        UNIT_ASSERT_VALUES_EQUAL(wifiStatusStatusName(WifiStatus::NOT_CHOSEN), "NOT_CHOSEN");
    }

    Y_UNIT_TEST(testWifiStatusSpeedName) {
        static_assert(WifiStatus_Speed_Speed_MAX == WifiStatus_Speed_FAST);

        UNIT_ASSERT_VALUES_EQUAL(wifiStatusSpeedName(WifiStatus::UNKNOWN), "UNKNOWN");
        UNIT_ASSERT_VALUES_EQUAL(wifiStatusSpeedName(WifiStatus::LOW), "LOW");
        UNIT_ASSERT_VALUES_EQUAL(wifiStatusSpeedName(WifiStatus::MIDDLE), "MIDDLE");
        UNIT_ASSERT_VALUES_EQUAL(wifiStatusSpeedName(WifiStatus::FAST), "FAST");
    }

}
