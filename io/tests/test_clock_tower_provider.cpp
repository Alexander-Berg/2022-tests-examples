#include <yandex_io/interfaces/clock_tower/connector/clock_tower_provider.h>

#include <yandex_io/interfaces/clock_tower/mock/clock_tower_provider.h>

#include <yandex_io/libs/base/named_callback_queue.h>
#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/ipc/mock/connector.h>
#include <yandex_io/protos/quasar_proto.pb.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

#include <time.h>

using namespace quasar;
using namespace quasar::TestUtils;
using namespace testing;

namespace {
    struct Fixture: public QuasarUnitTestFixture {
        Fixture() {
            mockConnector = std::make_shared<ipc::mock::Connector>();
        }

        void setupDefaultExpectCalls()
        {
            EXPECT_CALL(*mockConnector, setMessageHandler(_)).WillOnce(Invoke([&](ipc::IConnector::MessageHandler arg) {
                mockMessageHandler = arg;
            }));

            EXPECT_CALL(*mockConnector, connectToService()).Times(1);
        }
        YandexIO::Configuration::TestGuard testGuard;

        std::shared_ptr<ipc::mock::Connector> mockConnector;
        ipc::IConnector::MessageHandler mockMessageHandler;
    };

} // namespace

Y_UNIT_TEST_SUITE_F(ClockTowerProvider, Fixture) {
    Y_UNIT_TEST(testClockTowerCtor)
    {
        setupDefaultExpectCalls();
        ClockTowerProvider clockTowerProvider(mockConnector);

        auto state = clockTowerProvider.clockTowerState().value();
        UNIT_ASSERT(state);
        UNIT_ASSERT(state->localClock == nullptr);
        UNIT_ASSERT(state->remoteClocks.empty());
    }

    Y_UNIT_TEST(testUpdateClockTowerState)
    {
        setupDefaultExpectCalls();
        ClockTowerProvider clockTowerProvider(mockConnector);
        UNIT_ASSERT(mockMessageHandler);

        constexpr int64_t precision = 1000000LL; // 1 ms;
        proto::QuasarMessage message;
        auto& clockSync = *message.mutable_clock_tower_sync();

        /*
         * Check I. Initial update
         */
        std::shared_ptr<const IClock> localClockFromCheck1;
        std::shared_ptr<const IClock> remoteClockAFromCheck1;
        {
            auto& localClock = *clockSync.mutable_local_clock();
            localClock.set_device_id("MY_DEVICE_ID");
            localClock.set_clock_host("localhost");
            localClock.set_clock_port(1111);
            localClock.set_clock_id("LOCAL_CLOCK_ID");
            localClock.set_diff_ns(0);

            auto& remoteClock1 = *clockSync.add_remote_clock();
            remoteClock1.set_device_id("A_DEVICE_ID");
            remoteClock1.set_clock_host("a_host");
            remoteClock1.set_clock_port(1111);
            remoteClock1.set_clock_id("A_CLOCK_ID");
            remoteClock1.set_diff_ns(0);

            auto& remoteClock2 = *clockSync.add_remote_clock();
            remoteClock2.set_device_id("B_DEVICE_ID");
            remoteClock2.set_clock_host("b_host");
            remoteClock2.set_clock_port(1111);
            remoteClock2.set_clock_id("B_CLOCK_ID");
            remoteClock2.set_diff_ns(0);

            mockMessageHandler(ipc::UniqueMessage{message});
            auto state = clockTowerProvider.clockTowerState().value();

            UNIT_ASSERT(state);
            UNIT_ASSERT(state->localClock);
            localClockFromCheck1 = state->localClock;
            UNIT_ASSERT_VALUES_EQUAL(state->localClock->deviceId(), "MY_DEVICE_ID");
            UNIT_ASSERT_VALUES_EQUAL(state->localClock->peer(), "localhost:1111");
            UNIT_ASSERT_VALUES_EQUAL(state->localClock->clockId(), "LOCAL_CLOCK_ID");

            UNIT_ASSERT_VALUES_EQUAL(state->remoteClocks.size(), 2);

            auto remoteClock = state->remoteClocks.begin()->second;
            remoteClockAFromCheck1 = remoteClock;
            UNIT_ASSERT(remoteClock);
            UNIT_ASSERT_VALUES_EQUAL(remoteClock->deviceId(), "A_DEVICE_ID");
            UNIT_ASSERT_VALUES_EQUAL(remoteClock->peer(), "a_host:1111");
            UNIT_ASSERT_VALUES_EQUAL(remoteClock->clockId(), "A_CLOCK_ID");
            auto delta01 = (state->localClock->now() - remoteClock->now()).count();
            UNIT_ASSERT(std::abs(delta01) < precision);

            remoteClock = (++state->remoteClocks.begin())->second;
            UNIT_ASSERT(remoteClock);
            UNIT_ASSERT_VALUES_EQUAL(remoteClock->deviceId(), "B_DEVICE_ID");
            UNIT_ASSERT_VALUES_EQUAL(remoteClock->peer(), "b_host:1111");
            UNIT_ASSERT_VALUES_EQUAL(remoteClock->clockId(), "B_CLOCK_ID");
            auto delta02 = (state->localClock->now() - remoteClock->now()).count();
            UNIT_ASSERT(std::abs(delta02) < precision);
        }

        /*
         * Check II. Check clock time drifting
         */
        std::shared_ptr<const IClock> remoteClockAFromCheck2;
        {
            int64_t localClockDiff = 2000000000LL; // 2 seconds
            auto& localClock = *clockSync.mutable_local_clock();
            localClock.set_diff_ns(localClockDiff);

            mockMessageHandler(ipc::UniqueMessage{message});
            auto state = clockTowerProvider.clockTowerState().value();

            UNIT_ASSERT(state->localClock);
            UNIT_ASSERT_VALUES_EQUAL(state->remoteClocks.size(), 2);

            UNIT_ASSERT(localClockFromCheck1 == state->localClock); //  same clock

            remoteClockAFromCheck2 = state->remoteClocks.begin()->second;
            UNIT_ASSERT_VALUES_EQUAL(remoteClockAFromCheck1->clockId(), "A_CLOCK_ID");
            UNIT_ASSERT_VALUES_EQUAL(remoteClockAFromCheck2->clockId(), "A_CLOCK_ID");
            UNIT_ASSERT(remoteClockAFromCheck1 == remoteClockAFromCheck2);

            int64_t delta = (state->localClock->now() - remoteClockAFromCheck2->now()).count();
            UNIT_ASSERT(std::abs(delta) < localClockDiff + precision);
        }

        /*
         * Check III. Check clock id changing
         */
        std::shared_ptr<const IClock> remoteClockAFromCheck3;
        {
            clockSync.mutable_remote_clock(0)->set_clock_id("AA_CLOCK_ID");
            mockMessageHandler(ipc::UniqueMessage{message});

            auto state = clockTowerProvider.clockTowerState().value();
            UNIT_ASSERT(state->localClock);
            UNIT_ASSERT_VALUES_EQUAL(state->remoteClocks.size(), 2);

            UNIT_ASSERT(localClockFromCheck1 == state->localClock); //  same clock

            remoteClockAFromCheck3 = state->remoteClocks.begin()->second;
            UNIT_ASSERT_VALUES_EQUAL(remoteClockAFromCheck3->clockId(), "AA_CLOCK_ID");
            UNIT_ASSERT(remoteClockAFromCheck1 != remoteClockAFromCheck3);

            UNIT_ASSERT(remoteClockAFromCheck1->expired());
            UNIT_ASSERT(!remoteClockAFromCheck3->expired());
        }
    }

    Y_UNIT_TEST(testDumpAllClocks)
    {
        setupDefaultExpectCalls();
        ClockTowerProvider clockTowerProvider(mockConnector);
        UNIT_ASSERT(mockMessageHandler);

        proto::QuasarMessage message;
        auto& clockSync = *message.mutable_clock_tower_sync();

        auto& localClock = *clockSync.mutable_local_clock();
        localClock.set_device_id("MY_DEVICE_ID");
        localClock.set_clock_host("localhost");
        localClock.set_clock_port(1111);
        localClock.set_clock_id("LOCAL_CLOCK_ID");
        localClock.set_diff_ns(0);

        auto& remoteClock1 = *clockSync.add_remote_clock();
        remoteClock1.set_device_id("A_DEVICE_ID");
        remoteClock1.set_clock_host("a_host");
        remoteClock1.set_clock_port(1111);
        remoteClock1.set_clock_id("A_CLOCK_ID");
        remoteClock1.set_diff_ns(0);

        auto& remoteClock2 = *clockSync.add_remote_clock();
        remoteClock2.set_device_id("B_DEVICE_ID");
        remoteClock2.set_clock_host("b_host");
        remoteClock2.set_clock_port(1111);
        remoteClock2.set_clock_id("B_CLOCK_ID");
        remoteClock2.set_diff_ns(0);

        mockMessageHandler(ipc::UniqueMessage{message});

        auto clks = clockTowerProvider.dumpAllClocks();

        UNIT_ASSERT_VALUES_EQUAL(clks.size(), 3);
        UNIT_ASSERT_VALUES_EQUAL(clks.count("LOCAL_CLOCK_ID"), 1);
        UNIT_ASSERT(clks["LOCAL_CLOCK_ID"].count() > 0);

        UNIT_ASSERT_VALUES_EQUAL(clks.count("A_CLOCK_ID"), 1);
        UNIT_ASSERT(clks["A_CLOCK_ID"].count() > 0);

        UNIT_ASSERT_VALUES_EQUAL(clks.count("B_CLOCK_ID"), 1);
        UNIT_ASSERT(clks["B_CLOCK_ID"].count() > 0);

        constexpr int64_t precision = 100000LL; // 100 us;
        UNIT_ASSERT(std::abs(clks["LOCAL_CLOCK_ID"].count() - clks["A_CLOCK_ID"].count()) < precision);
        UNIT_ASSERT(std::abs(clks["A_CLOCK_ID"].count() - clks["B_CLOCK_ID"].count()) < precision);
        UNIT_ASSERT(std::abs(clks["B_CLOCK_ID"].count() - clks["LOCAL_CLOCK_ID"].count()) < precision);
    }

    Y_UNIT_TEST(testCheckClock)
    {
        setupDefaultExpectCalls();
        ClockTowerProvider clockTowerProvider(mockConnector);
        UNIT_ASSERT(mockMessageHandler);

        constexpr int64_t precision = 1000000LL;         // 1 ms;
        constexpr int64_t localClockDiff = 2000000000LL; // 2 seconds
        proto::QuasarMessage message;
        auto& clockSync = *message.mutable_clock_tower_sync();
        auto& localClock = *clockSync.mutable_local_clock();
        localClock.set_device_id("MY_DEVICE_ID");
        localClock.set_clock_host("localhost");
        localClock.set_clock_port(1111);
        localClock.set_clock_id("LOCAL_CLOCK_ID");
        localClock.set_diff_ns(localClockDiff);
        mockMessageHandler(ipc::UniqueMessage{message});

        auto state = clockTowerProvider.clockTowerState().value();
        UNIT_ASSERT(state->localClock);

        struct timespec ts;
        clock_gettime(CLOCK_MONOTONIC_RAW, &ts);
        auto c1 = ts.tv_sec * 1000000000LL + ts.tv_nsec;
        auto c2 = state->localClock->now().count();
        UNIT_ASSERT(c1 > c2 && c1 - c2 < localClockDiff + precision);
    }
}
