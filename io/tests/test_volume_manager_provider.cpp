#include <yandex_io/interfaces/volume_manager/connector/volume_manager_provider.h>

#include <yandex_io/libs/base/named_callback_queue.h>
#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/ipc/mock/connector.h>
#include <yandex_io/protos/quasar_proto.pb.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

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

Y_UNIT_TEST_SUITE_F(VolumeManagerProvider, Fixture) {
    Y_UNIT_TEST(testVolumeManagerStateInitial)
    {
        setupDefaultExpectCalls();
        VolumeManagerProvider volumeManagerProvider(mockConnector);

        auto state = volumeManagerProvider.volumeManagerState().value();

        UNIT_ASSERT(state);
        UNIT_ASSERT(*state == VolumeManagerState{});
    }

    Y_UNIT_TEST(testVolumeManagerStateChanged)
    {
        setupDefaultExpectCalls();
        VolumeManagerProvider volumeManagerProvider(mockConnector);

        auto state = volumeManagerProvider.volumeManagerState().value();
        UNIT_ASSERT(mockMessageHandler);

        std::atomic<int> signalCount{0};
        volumeManagerProvider.volumeManagerState().connect(
            [&](const auto& /*state*/) {
                ++signalCount;
            }, Lifetime::immortal);
        UNIT_ASSERT_VALUES_EQUAL(signalCount.load(), 1); // initial connect

        auto message = ipc::buildMessage([](auto& msg) {
            auto& state = *msg.mutable_volume_manager_message()->mutable_state();
            state.set_platform_volume(1000);
            state.set_alice_volume(5);
            state.set_is_muted(false);
            state.set_source("test");
            state.set_set_bt_volume(true);
        });
        mockMessageHandler(message);

        UNIT_ASSERT_VALUES_EQUAL(signalCount.load(), 2); // new state
        state = volumeManagerProvider.volumeManagerState().value();

        UNIT_ASSERT_VALUES_EQUAL(state->platformVolume, 1000);
        UNIT_ASSERT_VALUES_EQUAL(state->aliceVolume, 5);
        UNIT_ASSERT_VALUES_EQUAL(state->isMuted, false);
        UNIT_ASSERT_VALUES_EQUAL(state->source, "test");
        UNIT_ASSERT_VALUES_EQUAL(state->setBtVolume, true);

        mockMessageHandler(message);                     // repeate same state
        UNIT_ASSERT_VALUES_EQUAL(signalCount.load(), 2); // no changes

        auto state2 = volumeManagerProvider.volumeManagerState().value();

        UNIT_ASSERT(*state == *state2);
    }
}
