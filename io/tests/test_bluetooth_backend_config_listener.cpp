#include "bluetooth_backend_config_listener.h"

#include <yandex_io/tests/testlib/null_bluetooth/null_bluetooth.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

#include <iostream>
#include <sstream>

using namespace YandexIO;

namespace {

    class MockBluetooth: public NullBluetooth {
    public:
        explicit MockBluetooth(const std::string& name)
            : NullBluetooth(name)
        {
        }

        MOCK_METHOD(int, setName, (const std::string&), (override));
    };

    std::string repeatString(const std::string& str, size_t repeat) {
        std::string res;
        for (size_t i = 0; i != repeat; ++i) {
            res += str;
        }
        return res;
    }

} // namespace

Y_UNIT_TEST_SUITE(BluetoothBackendConfigListenerTests) {

    Y_UNIT_TEST(testBluetoothDirective) {
        auto bluetoothMock = std::make_shared<MockBluetooth>("Test");
        auto bluetoothBackendConfigListener = std::make_shared<BluetoothBackendConfigListener>(bluetoothMock);
        {
            testing::InSequence seq;
            EXPECT_CALL(*bluetoothMock, setName("Yandex Station 1234"));
            EXPECT_CALL(*bluetoothMock, setName("Яндекс Станция 1234"));
            EXPECT_CALL(*bluetoothMock, setName(std::string(63, 'y')));
            // UTF8 string must truncate to last symbol, not byte. We will have 62 byte string (31 symbol)
            EXPECT_CALL(*bluetoothMock, setName(repeatString("Ϣ", 31)));
        }
        bluetoothBackendConfigListener->onDeviceConfig("name", "\"Yandex Station 1234\"");
        bluetoothBackendConfigListener->onDeviceConfig("name", "\"Яндекс Станция 1234\"");
        std::stringstream ss;
        // Check truncate string
        ss << '"' << std::string(65, 'y') << '"';
        bluetoothBackendConfigListener->onDeviceConfig("name", ss.str());
        // Clear stream
        ss.str(std::string());
        // Create long UTF8 string with 2 byte synbols
        ss << '"' << repeatString("Ϣ", 65) << '"';
        bluetoothBackendConfigListener->onDeviceConfig("name", ss.str());
    }

};
