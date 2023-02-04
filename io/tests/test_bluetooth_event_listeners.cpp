#include "bluetooth_factory_reset_event_listener.h"
#include "bluetooth_media_event_listener.h"
#include "bluetooth_visibility_event_listener.h"
#include "bluetooth_volume_event_listener.h"

#include <yandex_io/modules/bluetooth/bluetooth_observer/bluetooth_observer.h>
#include <yandex_io/modules/bluetooth/bluetooth_state_listener/callback_bluetooth_state_listener.h>
#include <yandex_io/modules/bluetooth/util/util.h>

#include <yandex_io/protos/quasar_proto.pb.h>
#include <yandex_io/tests/testlib/null_bluetooth/null_bluetooth.h>
#include <yandex_io/tests/testlib/null_sdk/null_sdk_interface.h>
#include <yandex_io/tests/testlib/null_playback_control_capability/null_playback_control_capability.h>
#include <yandex_io/capabilities/playback_control/interfaces/mocks/mock_playback_control_capability.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

#include <future>
#include <iostream>
#include <sstream>
#include <utility>

using namespace YandexIO;
using namespace quasar;

namespace {

    class MockBluetooth: public NullBluetooth {
    public:
        explicit MockBluetooth(const std::string& name)
            : NullBluetooth(name)
        {
        }

        int asSinkSetVolumeAbs(int vol) override {
            Bluetooth::EventResult res;
            res.avrcpEvent = Bluetooth::AVRCP::CHANGE_VOLUME_ABS;
            res.volumeAbs = vol;
            sinkEventCallbackLocked(Bluetooth::SinkEvent::AVRCP_IN, res);
            return 0;
        }

        int setVisibility(bool disc, bool conn) override {
            sinkEventCallbackLocked(eventFromVisibility(disc, conn), EventResult{});
            return 0;
        }

        void factoryReset() override {
            baseEventCallbackLocked(BaseEvent::FACTORY_RESET, EventResult{});
        };

        void connect() {
            sinkEventCallbackLocked(Bluetooth::SinkEvent::CONNECTED, Bluetooth::EventResult{});
        }

        void disconnect() {
            sinkEventCallbackLocked(Bluetooth::SinkEvent::DISCONNECTED, Bluetooth::EventResult{});
        }

        void resume() {
            Bluetooth::EventResult res;
            res.avrcpEvent = Bluetooth::AVRCP::PLAY_START;
            sinkEventCallbackLocked(Bluetooth::SinkEvent::AVRCP_IN, res);
        }

        void pause() {
            Bluetooth::EventResult res;
            res.avrcpEvent = Bluetooth::AVRCP::PLAY_PAUSE;
            sinkEventCallbackLocked(Bluetooth::SinkEvent::AVRCP_IN, res);
        }

        void setTrackInfo() {
            Bluetooth::EventResult res;
            res.avrcpEvent = Bluetooth::AVRCP::TRACK_META_INFO;
            sinkEventCallbackLocked(Bluetooth::SinkEvent::AVRCP_IN, res);
        }

        void callSourceAVRCPEvent(Bluetooth::AVRCP avrcp) {
            Bluetooth::EventResult res;
            res.avrcpEvent = avrcp;
            sourceEventCallbackLocked(Bluetooth::SourceEvent::AVRCP_IN, res);
        }
    };

    class ConnectionEventAwaiter {
    public:
        void connected() {
            std::scoped_lock guard(m_);
            counter_++;
            cv_.notify_all();
        }

        void disconnected() {
            std::scoped_lock guard(m_);
            counter_--;
            cv_.notify_all();
        }

        void waitEqual(size_t expected) {
            std::unique_lock lock(m_);
            cv_.wait(lock, [this, expected]() {
                return counter_ == expected;
            });
        }

    private:
        std::condition_variable cv_;
        std::mutex m_;
        size_t counter_ = 0;
    };

    class TestBluetoothObserver: public BluetoothObserver {
    public:
        TestBluetoothObserver() = default;

        void onDiscoveryStart() override {
            visibilityPromise_.set_value(true);
        }

        void onDiscoveryStop() override {
            visibilityPromise_.set_value(false);
        }

        void onSourceConnected(const std::string& /*btAddr*/) override {
            awaiter_->connected();
        }

        void onSourceDisconnected(const std::string& /*btAddr*/) override {
            awaiter_->disconnected();
        }

        void onChangeVolumeAVRCP(int volume) override {
            volumePromise_.set_value(volume);
        }

        void onFactoryResetComplete() override {
            factoryResetPromise_.set_value();
        }

        void setVolumePromise(std::promise<int> p) {
            volumePromise_ = std::move(p);
        }

        void setFactoryResetPromise(std::promise<void> p) {
            factoryResetPromise_ = std::move(p);
        }

        void setVisibilityPromise(std::promise<bool> p) {
            visibilityPromise_ = std::move(p);
        }

        void setConnectionEventAwaiter(std::shared_ptr<ConnectionEventAwaiter> awaiter) {
            awaiter_ = std::move(awaiter);
        }

    private:
        std::shared_ptr<ConnectionEventAwaiter> awaiter_;
        std::promise<int> volumePromise_;
        std::promise<void> factoryResetPromise_;
        std::promise<bool> visibilityPromise_;
    };

    template <typename T>
    std::pair<std::promise<T>, std::future<T>> makePromiseWithFuture() {
        std::promise<T> p;
        std::future<T> f = p.get_future();
        return std::make_pair(std::move(p), std::move(f));
    }

    class SDKMock: public NullSDKInterface {
    public:
        MOCK_METHOD(void, bluetoothMediaSinkPause, (), (override));
        MOCK_METHOD(void, bluetoothMediaSinkStart, (), (override));
        MOCK_METHOD(void, bluetoothMediaSinkTrackInfo, (const std::string&, const std::string&, const std::string&, const std::string&, int, int), (override));
        MOCK_METHOD(void, bluetoothSinkDisconnected, (const std::string&, const std::string&), (override));
        MOCK_METHOD(void, bluetoothSinkConnected, (const std::string&, const std::string&), (override));
        MOCK_METHOD(std::shared_ptr<IPlaybackControlCapability>, getPlaybackControlCapability, (), (const, override));
    };

} // namespace

Y_UNIT_TEST_SUITE(BluetoothEventListeners) {

    Y_UNIT_TEST(testBluetoothMediaEventListener) {
        auto bluetoothObserver = std::make_shared<TestBluetoothObserver>();
        auto awaiter = std::make_shared<ConnectionEventAwaiter>();
        bluetoothObserver->setConnectionEventAwaiter(awaiter);
        auto bluetoothImpl = std::make_shared<MockBluetooth>("Test");
        auto nullSDK = std::make_shared<NullSDKInterface>();
        auto bluetoothMediaEventListener = std::make_shared<BluetoothMediaEventListener>(nullSDK, false);
        bluetoothMediaEventListener->addBluetoothListener(bluetoothObserver);
        bluetoothImpl->registerEventListener(bluetoothMediaEventListener);

        std::mutex m;
        std::condition_variable cv;
        bool playingState = false;
        bool connectedState = false;
        auto bluetoothStateListener = std::make_shared<CallbackBluetoothStateListener>([&](bool state) {
        std::scoped_lock guard(m);
        playingState = state;
        cv.notify_all(); }, [&](bool state) {
        std::scoped_lock guard(m);
        connectedState = state;
        cv.notify_all(); });

        bluetoothMediaEventListener->addBluetoothStateListener(bluetoothStateListener);
        bluetoothImpl->connect();
        awaiter->waitEqual(1);
        std::unique_lock lock(m);
        cv.wait(lock, [&]() { return connectedState; });
        lock.unlock();
        bluetoothImpl->connect();
        awaiter->waitEqual(2);
        bluetoothImpl->disconnect();
        awaiter->waitEqual(1);
        // state should not change
        std::this_thread::sleep_for(std::chrono::seconds(1));
        lock.lock();
        cv.wait(lock, [&]() { return connectedState; });
        bluetoothImpl->resume();
        cv.wait(lock, [&]() { return playingState; });
        bluetoothImpl->pause();
        cv.wait(lock, [&]() { return !playingState; });
        bluetoothImpl->resume();
        cv.wait(lock, [&]() { return playingState; });
        lock.unlock();
        bluetoothImpl->disconnect();
        awaiter->waitEqual(0);
        lock.lock();
        cv.wait(lock, [&]() { return !connectedState; });
        cv.wait(lock, [&]() { return !playingState; });
    }

    Y_UNIT_TEST(testBluetoothVolumeEventListener) {
        auto [p, f] = makePromiseWithFuture<int>();
        auto observer = std::make_shared<TestBluetoothObserver>();
        observer->setVolumePromise(std::move(p));
        auto bluetoothImpl = std::make_shared<MockBluetooth>("Test");
        auto bluetoothVolumeEventListener = std::make_shared<BluetoothVolumeEventListerner>();
        bluetoothVolumeEventListener->addBluetoothListener(observer);
        bluetoothImpl->registerEventListener(bluetoothVolumeEventListener);
        // max BLuetooth Volume value is 127
        bluetoothImpl->asSinkSetVolumeAbs(63);
        auto volumePercent = f.get();
        UNIT_ASSERT_EQUAL(volumePercent, 50);
    }

    Y_UNIT_TEST(testBluetoothFactoryResetEventListener) {
        auto [p, f] = makePromiseWithFuture<void>();
        auto observer = std::make_shared<TestBluetoothObserver>();
        observer->setFactoryResetPromise(std::move(p));
        auto bluetoothImpl = std::make_shared<MockBluetooth>("Test");
        auto bluetoothFactoryResetEventListener = std::make_shared<BluetoothFactoryResetEventListener>();
        bluetoothFactoryResetEventListener->addListener(observer);
        bluetoothImpl->registerEventListener(bluetoothFactoryResetEventListener);
        bluetoothImpl->factoryReset();
        f.get();
    }

    Y_UNIT_TEST(testBluetoothVisibilityEventListener) {
        auto observer = std::make_shared<TestBluetoothObserver>();
        auto bluetoothImpl = std::make_shared<MockBluetooth>("Test");
        auto bluetoothVisibilityEventListener = std::make_shared<BluetoothVisibilityEventListener>(bluetoothImpl);
        bluetoothVisibilityEventListener->addListener(observer);
        bluetoothImpl->registerEventListener(bluetoothVisibilityEventListener);
        {
            auto [p, f] = makePromiseWithFuture<bool>();
            observer->setVisibilityPromise(std::move(p));
            bluetoothImpl->setVisibility(true, true);
            UNIT_ASSERT(f.get());
        }
        {
            auto [p, f] = makePromiseWithFuture<bool>();
            observer->setVisibilityPromise(std::move(p));
            bluetoothImpl->setVisibility(true, false);
            UNIT_ASSERT(f.get());
        }
        {
            auto [p, f] = makePromiseWithFuture<bool>();
            observer->setVisibilityPromise(std::move(p));
            bluetoothImpl->setVisibility(false, true);
            UNIT_ASSERT(!f.get());
        }
        {
            auto [p, f] = makePromiseWithFuture<bool>();
            observer->setVisibilityPromise(std::move(p));
            bluetoothImpl->setVisibility(false, true);
            UNIT_ASSERT(!f.get());
        }
    }

    Y_UNIT_TEST(testBluetoothVisibilityFallback) {
        auto observer = std::make_shared<TestBluetoothObserver>();
        auto bluetoothImpl = std::make_shared<MockBluetooth>("Test");
        auto bluetoothVisibilityEventListener = std::make_shared<BluetoothVisibilityEventListener>(bluetoothImpl, std::chrono::milliseconds{777});
        bluetoothVisibilityEventListener->addListener(observer);
        bluetoothImpl->registerEventListener(bluetoothVisibilityEventListener);
        {
            auto [p, f] = makePromiseWithFuture<bool>();
            observer->setVisibilityPromise(std::move(p));
            bluetoothImpl->setVisibility(true, true);
            UNIT_ASSERT(f.get());
        }
        {
            auto [p, f] = makePromiseWithFuture<bool>();
            observer->setVisibilityPromise(std::move(p));
            UNIT_ASSERT(!f.get());
        }
    }

    Y_UNIT_TEST(testBluetoothSinkMediaMethods) {
        auto bluetoothImpl = std::make_shared<MockBluetooth>("Test");
        auto sdk = std::make_shared<SDKMock>();
        auto bluetoothMediaEventListener = std::make_shared<BluetoothMediaEventListener>(sdk, false);
        bluetoothImpl->registerEventListener(bluetoothMediaEventListener);
        auto p = std::make_unique<std::promise<void>>();
        auto f = p->get_future();
        {
            testing::InSequence seq;
            EXPECT_CALL(*sdk, bluetoothMediaSinkStart());
            EXPECT_CALL(*sdk, bluetoothMediaSinkPause());
            EXPECT_CALL(*sdk, bluetoothMediaSinkTrackInfo(std::string(),
                                                          std::string(),
                                                          std::string(),
                                                          std::string(),
                                                          -1,
                                                          -1))
                .WillOnce(testing::Invoke([&]() {
                    p->set_value();
                }));
        }
        bluetoothImpl->resume();
        bluetoothImpl->pause();
        bluetoothImpl->setTrackInfo();
        // wait last callback in sequence;
        f.get();
    }

    Y_UNIT_TEST(testBluetoothSinkConnectionMethods) {
        auto bluetoothImpl = std::make_shared<MockBluetooth>("Test");
        auto sdk = std::make_shared<SDKMock>();
        auto bluetoothMediaEventListener = std::make_shared<BluetoothMediaEventListener>(sdk, false);
        bluetoothImpl->registerEventListener(bluetoothMediaEventListener);
        auto p = std::make_unique<std::promise<void>>();
        auto f = p->get_future();
        {
            testing::InSequence seq;
            EXPECT_CALL(*sdk, bluetoothSinkConnected(std::string(), std::string()));
            EXPECT_CALL(*sdk, bluetoothMediaSinkPause());
            EXPECT_CALL(*sdk, bluetoothSinkDisconnected(std::string(), std::string())).WillOnce(testing::Invoke([&]() {
                p->set_value();
            }));
        }
        bluetoothImpl->connect();
        bluetoothImpl->disconnect();
        f.get();
    }

    Y_UNIT_TEST(testBluetoothSourceMethods) {
        auto bluetoothImpl = std::make_shared<MockBluetooth>("Test");
        auto sdk = std::make_shared<SDKMock>();
        auto bluetoothMediaEventListener = std::make_shared<BluetoothMediaEventListener>(sdk, false);
        bluetoothImpl->registerEventListener(bluetoothMediaEventListener);
        auto p = std::make_unique<std::promise<void>>();
        auto f = p->get_future();
        auto mockPlaybackCapability = std::make_shared<MockPlaybackControlCapability>();
        {
            testing::InSequence seq;
            EXPECT_CALL(*sdk, getPlaybackControlCapability()).WillOnce(testing::Invoke([&]() {
                return mockPlaybackCapability;
            }));
            EXPECT_CALL(*mockPlaybackCapability, next());
            EXPECT_CALL(*sdk, getPlaybackControlCapability()).WillOnce(testing::Invoke([&]() {
                return mockPlaybackCapability;
            }));
            EXPECT_CALL(*mockPlaybackCapability, prev());
            EXPECT_CALL(*sdk, getPlaybackControlCapability()).WillOnce(testing::Invoke([&]() {
                return mockPlaybackCapability;
            }));
            EXPECT_CALL(*mockPlaybackCapability, pause());
            EXPECT_CALL(*sdk, getPlaybackControlCapability()).WillOnce(testing::Invoke([&]() {
                return mockPlaybackCapability;
            }));
            EXPECT_CALL(*mockPlaybackCapability, pause());
            EXPECT_CALL(*sdk, getPlaybackControlCapability()).WillOnce(testing::Invoke([&]() {
                return mockPlaybackCapability;
            }));
            EXPECT_CALL(*mockPlaybackCapability, play()).WillOnce(testing::Invoke([&]() {
                p->set_value();
            }));
        }
        bluetoothImpl->callSourceAVRCPEvent(Bluetooth::AVRCP::PLAY_NEXT);
        bluetoothImpl->callSourceAVRCPEvent(Bluetooth::AVRCP::PLAY_PREV);
        bluetoothImpl->callSourceAVRCPEvent(Bluetooth::AVRCP::PLAY_PAUSE);
        bluetoothImpl->callSourceAVRCPEvent(Bluetooth::AVRCP::PLAY_STOP);
        bluetoothImpl->callSourceAVRCPEvent(Bluetooth::AVRCP::PLAY_START);
        f.get();
    }

};
