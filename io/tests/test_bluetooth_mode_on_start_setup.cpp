#include "bluetooth_mode_on_start_setup.h"

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
        MOCK_METHOD(int, powerOn, (), (override));
    };

} // namespace

Y_UNIT_TEST_SUITE(BluetoothDeviceModeObserver) {

    Y_UNIT_TEST(testBluetoothModeOnStartSetup) {
        auto bluetoothMock = std::make_shared<MockBluetooth>("Test");
        {
            testing::InSequence seq;
            EXPECT_CALL(*bluetoothMock, powerOn());
            EXPECT_CALL(*bluetoothMock, setVisibility(true, true));
            EXPECT_CALL(*bluetoothMock, setVisibility(false, false));
        }
        auto bluetoothModeOnStartSetup = std::make_shared<BluetoothModeOnStartSetup>(bluetoothMock);
        bluetoothModeOnStartSetup->onStartSetup(false);
        bluetoothModeOnStartSetup->onFinishSetup();
    }

};
