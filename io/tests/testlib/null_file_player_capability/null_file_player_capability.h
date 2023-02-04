#pragma once

#include <yandex_io/capabilities/file_player/interfaces/i_file_player_capability.h>

namespace YandexIO {

    class NullFilePlayerCapability: public IFilePlayerCapability {
    public:
        void playSoundFile(const std::string& fileName,
                           std::optional<quasar::proto::AudioChannel> channel,
                           std::optional<PlayParams> params,
                           std::shared_ptr<IPlaySoundFileListener> listener) override;

        void stopSoundFile(const std::string& fileName) override;
    };

} // namespace YandexIO
