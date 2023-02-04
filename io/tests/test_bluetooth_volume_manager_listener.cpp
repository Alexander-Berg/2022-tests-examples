#include "bluetooth_volume_manager_listener.h"

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

        MOCK_METHOD(int, asSinkSetVolumeAbs, (int), (override));
    };

} // namespace

Y_UNIT_TEST_SUITE(BluetoothVolumeManagerListenerTests) {

    Y_UNIT_TEST(testBluetoothVolumeManager) {
        auto bluetoothMock = std::make_shared<MockBluetooth>("Test");
        {
            testing::InSequence seq;
            EXPECT_CALL(*bluetoothMock, asSinkSetVolumeAbs(64)).Times(1);
        }
        std::shared_ptr<IVolumeManagerListener> bluetoothVolumeManagerListener = std::make_shared<BluetoothVolumeManagerListener>(bluetoothMock, 10);
        constexpr bool muted = false;
        bluetoothVolumeManagerListener->onVolumeChange(5, 0, muted, "source", true);
        bluetoothVolumeManagerListener->onVolumeChange(5, 0, muted, "source", false);
    }

};
