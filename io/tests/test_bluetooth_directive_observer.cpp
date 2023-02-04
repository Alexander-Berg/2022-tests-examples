#include "bluetooth_directive_observer.h"

#include <yandex_io/libs/base/directives.h>
#include <yandex_io/tests/testlib/null_bluetooth/null_bluetooth.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

#include <memory>

using namespace YandexIO;
using namespace quasar;

namespace {

    class MockBluetooth: public NullBluetooth {
    public:
        explicit MockBluetooth(const std::string& name)
            : NullBluetooth(name)
        {
        }

        MOCK_METHOD(int, setVisibility, (bool, bool), (override));
        MOCK_METHOD(int, powerOff, (), (override));
        MOCK_METHOD(int, powerOn, (), (override));
        MOCK_METHOD(int, disconnectAll, (Bluetooth::BtRole), (override));
    };

} // namespace

Y_UNIT_TEST_SUITE(BluetoothDirectiveObserverTests) {

    Y_UNIT_TEST(testBluetoothDirective) {
        auto bluetoothMock = std::make_shared<MockBluetooth>("Test");
        {
            testing::InSequence seq;
            EXPECT_CALL(*bluetoothMock, powerOn());
            EXPECT_CALL(*bluetoothMock, setVisibility(true, true));
            EXPECT_CALL(*bluetoothMock, disconnectAll(Bluetooth::BtRole::ALL));
            EXPECT_CALL(*bluetoothMock, setVisibility(false, false));
            EXPECT_CALL(*bluetoothMock, powerOff());
        }
        auto bluetoothDirectiveObserver = std::make_shared<BluetoothDirectiveObserver>(bluetoothMock, true);
        bluetoothDirectiveObserver->onDirective(Directives::START_BLUETOOTH, std::string(), std::string());
        bluetoothDirectiveObserver->onDirective(Directives::STOP_BLUETOOTH, std::string(), std::string());
    }

};
