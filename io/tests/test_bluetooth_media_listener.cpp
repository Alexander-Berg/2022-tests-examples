#include "bluetooth_media_listener.h"

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

        MOCK_METHOD(int, asSinkPlayPause, (const BtNetwork&), (override));
        MOCK_METHOD(int, asSinkPlayStart, (const BtNetwork&), (override));
        MOCK_METHOD(int, asSinkPlayNext, (const BtNetwork&), (override));
        MOCK_METHOD(int, asSinkPlayPrev, (const BtNetwork&), (override));
        MOCK_METHOD(void, takeAudioFocus, (), (override));
        MOCK_METHOD(void, freeAudioFocus, (), (override));
        MOCK_METHOD(int, disconnectAll, (BtRole), (override));
    };

    void callMethods(const std::shared_ptr<BluetoothMediaListener>& bluetoothMediaListener) {
        bluetoothMediaListener->onBtPause();
        bluetoothMediaListener->onBtResume();
        bluetoothMediaListener->onBtNext();
        bluetoothMediaListener->onBtPrev(false);
        bluetoothMediaListener->onBtTakeAudioFocus();
        bluetoothMediaListener->onBtFreeAudioFocus();
        bluetoothMediaListener->onBtDisconnectAll();
    }

} // namespace

Y_UNIT_TEST_SUITE(BluetoothMediaListenerTest) {

    Y_UNIT_TEST(testBluetoothMediaListener) {
        auto bluetoothMock = std::make_shared<MockBluetooth>("Test");
        {
            testing::InSequence seq;
            EXPECT_CALL(*bluetoothMock, asSinkPlayPause(Bluetooth::BtNetwork{}));
            EXPECT_CALL(*bluetoothMock, asSinkPlayStart(Bluetooth::BtNetwork{}));
            EXPECT_CALL(*bluetoothMock, asSinkPlayNext(Bluetooth::BtNetwork{}));
            EXPECT_CALL(*bluetoothMock, asSinkPlayPrev(Bluetooth::BtNetwork{}));
            EXPECT_CALL(*bluetoothMock, takeAudioFocus());
            EXPECT_CALL(*bluetoothMock, freeAudioFocus());
            EXPECT_CALL(*bluetoothMock, disconnectAll(Bluetooth::BtRole::ALL));
        }
        auto bluetoothMediaListener = std::make_shared<BluetoothMediaListener>(bluetoothMock);
        callMethods(bluetoothMediaListener);
    }

    Y_UNIT_TEST(testBluetoothMediaListenerBricked) {
        auto bluetoothMock = std::make_shared<testing::StrictMock<MockBluetooth>>("Test");
        auto bluetoothMediaListener = std::make_shared<BluetoothMediaListener>(bluetoothMock);
        auto bricker = bluetoothMediaListener->getBrickStatusListener().lock();
        bricker->onBrickStatusChanged(true);
        callMethods(bluetoothMediaListener);
    }

};
