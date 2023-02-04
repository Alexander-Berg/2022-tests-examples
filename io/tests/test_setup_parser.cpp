#include <yandex_io/libs/setup_parser/credentials.h>
#include <yandex_io/libs/setup_parser/setup_parser.h>
#include <yandex_io/libs/setup_parser/wifi_type.h>

#include <yandex_io/libs/base/utils.h>

#include <yandex_io/protos/quasar_proto.pb.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <vector>

namespace {

    using namespace quasar;
    using namespace quasar::proto;
    using namespace quasar::SetupParser;

    using byte = unsigned char;

    proto::WifiInfo createWifiInfo(const TString& ssid) {
        WifiInfo wifiInfo;
        wifiInfo.set_ssid(ssid);
        return wifiInfo;
    }

    Y_UNIT_TEST_SUITE(testSetupParser) {
        Y_UNIT_TEST(testParseWithSpecifiedWifiType) {
            std::vector<byte> ssid = stringToBytes("ssid");
            std::vector<byte> password = stringToBytes("password");
            std::vector<byte> xToken = stringToBytes("xToken");
            std::vector<byte> testMessage;
            testMessage.push_back(quasar::WifiType::WIFI_TYPE_WPA);
            testMessage.push_back(ssid.size());
            testMessage.insert(testMessage.end(), ssid.begin(), ssid.end());
            testMessage.push_back(password.size());
            testMessage.insert(testMessage.end(), password.begin(), password.end());
            testMessage.push_back(xToken.size());
            testMessage.insert(testMessage.end(), xToken.begin(), xToken.end());

            const Credentials credentials = parseInitData(testMessage);

            UNIT_ASSERT_VALUES_EQUAL("password", credentials.password);
            UNIT_ASSERT_VALUES_EQUAL("xToken", credentials.tokenCode);
            UNIT_ASSERT_VALUES_EQUAL(quasar::WifiType::WIFI_TYPE_WPA, credentials.wifiType);
            UNIT_ASSERT_VALUES_EQUAL(1, credentials.SSIDs.size());
            UNIT_ASSERT_VALUES_EQUAL("ssid", credentials.SSIDs[0]);
        }

        Y_UNIT_TEST(testParseWithUnknownWifiType) {
            int16_t ssidHash = javaStyleStringHash("ssid");
            std::vector<byte> ssid;
            ssid.push_back(ssidHash % (1 << 8));
            ssid.push_back(ssidHash >> 8);
            std::vector<byte> password = stringToBytes("password");
            std::vector<byte> xToken = stringToBytes("xToken");
            std::vector<byte> testMessage;
            testMessage.push_back(quasar::WifiType::WIFI_TYPE_UNKNOWN);
            testMessage.insert(testMessage.end(), ssid.begin(), ssid.end());
            testMessage.push_back(password.size());
            testMessage.insert(testMessage.end(), password.begin(), password.end());
            testMessage.push_back(xToken.size());
            testMessage.insert(testMessage.end(), xToken.begin(), xToken.end());

            Credentials credentials = parseInitData(testMessage);

            UNIT_ASSERT_VALUES_EQUAL("password", credentials.password);
            UNIT_ASSERT_VALUES_EQUAL("xToken", credentials.tokenCode);
            UNIT_ASSERT_VALUES_EQUAL(ssidHash, credentials.SSIDHashCode);
            UNIT_ASSERT_VALUES_EQUAL(quasar::WifiType::WIFI_TYPE_UNKNOWN, credentials.wifiType);

            google::protobuf::RepeatedPtrField<WifiInfo> hotspotList;
            hotspotList.Add(createWifiInfo("ssid_fake1"));
            hotspotList.Add(createWifiInfo("ssid"));
            hotspotList.Add(createWifiInfo("ssid_fake2"));
            credentials.fillSSIDs(hotspotList);

            UNIT_ASSERT_VALUES_EQUAL(1, credentials.SSIDs.size());
            UNIT_ASSERT_VALUES_EQUAL("ssid", credentials.SSIDs[0]);
        }
    }
} // namespace
