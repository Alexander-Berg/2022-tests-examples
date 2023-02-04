#pragma once

#include <yandex_io/capabilities/playback_control/interfaces/i_playback_control_capability.h>

namespace YandexIO {

    class NullPlaybackControlCapability: public IPlaybackControlCapability {
    public:
        void play() override;
        void pause() override;
        void togglePlayPause(bool canRequestMusic) override;
        void rewind(std::uint32_t time) override;
        void like() override;
        void dislike() override;
        void next() override;
        void prev() override;
    };

} // namespace YandexIO
