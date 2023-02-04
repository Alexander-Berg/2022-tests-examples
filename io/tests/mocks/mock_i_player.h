#pragma once

#include <yandex_io/services/aliced/capabilities/alice_capability/i_player.h>

#include <yandex_io/protos/quasar_proto.pb.h>
#include <yandex_io/sdk/interfaces/directive.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace YandexIO {

    class MockIPlayer: public IPlayer {
    public:
        MOCK_METHOD(void, play, (const quasar::proto::AudioAnalyticsContext&), (override));
        MOCK_METHOD(void, pause, (), (override));
        MOCK_METHOD(void, resume, (), (override));
        MOCK_METHOD(void, cancel, (), (override));
        MOCK_METHOD(void, handleAudioClientEvent, (const quasar::proto::AudioClientEvent& event), (override));
        MOCK_METHOD(void, handleAudioClientConnectionStatus, (bool connected), (override));
        MOCK_METHOD(void, setListener, (std::weak_ptr<IPlayer::IListener>), (override));
    };

} // namespace YandexIO
