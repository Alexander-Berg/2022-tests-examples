#include "null_file_player_capability.h"

using namespace YandexIO;

void NullFilePlayerCapability::playSoundFile(const std::string& fileName,
                                             std::optional<quasar::proto::AudioChannel> channel,
                                             std::optional<PlayParams> params,
                                             std::shared_ptr<IPlaySoundFileListener> listener)
{
    Y_UNUSED(fileName);
    Y_UNUSED(channel);
    Y_UNUSED(params);
    Y_UNUSED(listener);
}

void NullFilePlayerCapability::stopSoundFile(const std::string& fileName)
{
    Y_UNUSED(fileName);
}
