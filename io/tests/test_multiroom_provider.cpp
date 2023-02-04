#include <yandex_io/interfaces/multiroom/connector/multiroom_provider.h>

#include <yandex_io/libs/base/named_callback_queue.h>
#include <yandex_io/libs/device/mock/simple_device.h>
#include <yandex_io/libs/ipc/mock/simple_connector.h>
#include <yandex_io/protos/quasar_proto.pb.h>
#include <yandex_io/tests/testlib/test_utils.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace quasar;
using namespace quasar::TestUtils;

namespace {
    const std::string deviceId = "XXXTESTDEVICEID";
    const std::string platform = "XXXTESTPLATFORM";
    const std::string ipAddress = "256.256.256.256";
} // namespace

Y_UNIT_TEST_SUITE(MultiroomProvider) {

    Y_UNIT_TEST(testMultiroomStateInitial)
    {
        auto device = std::make_shared<YandexIO::mock::SimpleDevice>(deviceId, platform);
        auto connector = std::make_shared<ipc::mock::SimpleConnector>(nullptr);
        auto lifecycle = std::make_shared<NamedCallbackQueue>("testMultiroomProviderCallbackThread");

        MultiroomProvider multiroomProvider(device, connector, lifecycle);
        auto multiroomState = multiroomProvider.multiroomState().value();

        UNIT_ASSERT(multiroomState != nullptr);
        UNIT_ASSERT_VALUES_EQUAL(multiroomState->deviceId, "");
        UNIT_ASSERT_VALUES_EQUAL(multiroomState->ipAddress, "");
        UNIT_ASSERT_VALUES_EQUAL((int)multiroomState->mode, (int)MultiroomState::Mode::UNDEFINED);
        UNIT_ASSERT_VALUES_EQUAL((int)multiroomState->slaveSyncLevel, (int)MultiroomState::SyncLevel::UNDEFINED);
        UNIT_ASSERT_VALUES_EQUAL(multiroomState->slaveClockSyncing, false);
        UNIT_ASSERT_VALUES_EQUAL(multiroomState->peers.size(), 0);
        UNIT_ASSERT(multiroomState->broadcast == nullptr);
    }

    Y_UNIT_TEST(testMultiroomState)
    {
        auto device = std::make_shared<YandexIO::mock::SimpleDevice>(deviceId, platform);
        auto connector = std::make_shared<ipc::mock::SimpleConnector>(nullptr);
        auto lifecycle = std::make_shared<NamedCallbackQueue>("testMultiroomProviderCallbackThread");

        MultiroomProvider multiroomProvider(device, connector, lifecycle);
        int stage = 0;

        bool fStage0 = false;
        bool fStage1 = false;
        bool fStage3 = false;
        multiroomProvider.multiroomState().connect(
            [&](auto multiroomState) {
                if (stage == 0) {
                    UNIT_ASSERT_VALUES_EQUAL(fStage0, false);
                    fStage0 = true;
                } else if (stage == 1) {
                    UNIT_ASSERT_VALUES_EQUAL(fStage1, false);
                    fStage1 = true;
                    UNIT_ASSERT_VALUES_EQUAL(multiroomState->deviceId, deviceId);
                    UNIT_ASSERT_VALUES_EQUAL(multiroomState->ipAddress, ipAddress);
                    UNIT_ASSERT_VALUES_EQUAL(multiroomState->peers.size(), 2);
                    UNIT_ASSERT_VALUES_EQUAL(multiroomState->peers[0].deviceId, "D1");
                    UNIT_ASSERT_VALUES_EQUAL(multiroomState->peers[1].deviceId, "D2");
                    UNIT_ASSERT(multiroomState->broadcast != nullptr);
                    UNIT_ASSERT_VALUES_EQUAL(multiroomState->broadcast->multiroomToken, "00000000-0000-0000");
                    UNIT_ASSERT(multiroomState->broadcast->music != nullptr);
                    UNIT_ASSERT_VALUES_EQUAL(multiroomState->broadcast->music->trackId, "TrackId");
                } else if (stage == 2) {
                    UNIT_ASSERT(false); // No changes, no enter!
                } else if (stage == 3) {
                    UNIT_ASSERT_VALUES_EQUAL(fStage3, false);
                    fStage3 = true;
                    UNIT_ASSERT_VALUES_EQUAL(multiroomState->broadcast->multiroomToken, "11111111-1111-1111");
                } else {
                    UNIT_ASSERT(false);
                }
            }, Lifetime::immortal);

        flushCallbackQueue(lifecycle);

        UNIT_ASSERT_VALUES_EQUAL(fStage0, true);

        proto::QuasarMessage message;
        auto protoState = message.mutable_multiroom_state();
        auto protoBroadcast = protoState->mutable_multiroom_broadcast();
        auto protoParams = protoBroadcast->mutable_multiroom_params();
        auto protoMusic = protoParams->mutable_music_params();

        protoState->set_device_id(TString(deviceId));
        protoState->set_ip_address(TString(ipAddress));
        protoState->set_mode(proto::MultiroomState::MASTER);
        protoState->set_slave_sync_level(proto::MultiroomState::STRONG);
        auto p1 = protoState->add_peers();
        p1->set_device_id("D1");
        p1->set_ip_address("IP1");
        auto p2 = protoState->add_peers();
        p2->set_device_id("D2");
        p2->set_ip_address("IP2");

        protoBroadcast->set_device_id(TString(deviceId));
        protoBroadcast->set_session_timestamp_ms(1);
        protoBroadcast->set_multiroom_token("00000000-0000-0000");
        protoBroadcast->add_room_device_ids("D1");
        protoBroadcast->set_vins_request_id("4444-4444-4444-4444");
        protoBroadcast->set_state(proto::MultiroomBroadcast::PLAYING);

        protoMusic->set_current_track_id("TrackId");
        protoMusic->set_json_track_info("{ \"field1\": 1}");
        protoMusic->set_uid("1234");
        protoMusic->set_session_id("MusicSessionId");
        protoMusic->set_timestamp_ms(1);
        protoMusic->set_is_paused(false);

        stage = 1;
        connector->pushMessage(message);
        flushCallbackQueue(lifecycle);

        stage = 2;
        connector->pushMessage(message);
        flushCallbackQueue(lifecycle);

        proto::QuasarMessage message3(message);
        message3.mutable_multiroom_state()->mutable_multiroom_broadcast()->set_multiroom_token("11111111-1111-1111");
        stage = 3;
        connector->pushMessage(message3);
        flushCallbackQueue(lifecycle);

        UNIT_ASSERT_VALUES_EQUAL(fStage0, true);
        UNIT_ASSERT_VALUES_EQUAL(fStage1, true);
        UNIT_ASSERT_VALUES_EQUAL(fStage3, true);
    }

} // Y_UNIT_TEST_SUITE(MultiroomProvider)
