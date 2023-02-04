#pragma once

#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/sdk/audio_source/i_audio_source_client.h>

namespace quasar {

    class MockAudioSourceClient: public YandexIO::IAudioSourceClient {
        using RequestChannelType = YandexIO::RequestChannelType;
        std::optional<RequestChannelType> subscription_;
        std::list<std::weak_ptr<Listener>> listeners_;

        void broadcast(const YandexIO::ChannelsData& data) {
            for (auto listener : listeners_) {
                if (auto strongListener = listener.lock(); strongListener) {
                    strongListener->onAudioData(data);
                }
            }
        }

        static bool isRequested(const YandexIO::ChannelData& channel, RequestChannelType reqType) {
            using RCT = RequestChannelType;
            using CT = YandexIO::ChannelData::Type;
            switch (reqType) {
                case RCT::MAIN:
                    return channel.isForRecognition;
                case RCT::RAW:
                    return channel.type == CT::RAW;
                case RCT::VQE:
                    return channel.type == CT::VQE;
                default:
                    break;
            };
            return false;
        }

    public:
        void subscribeToChannels(RequestChannelType type) override {
            subscription_ = type;
            YIO_LOG_INFO("Subscribed to " << int(type));
        }
        void unsubscribeFromChannels() override {
            subscription_.reset();
        }
        void start() override {
        }
        void addListener(std::weak_ptr<Listener> listener) override {
            listeners_.push_back(listener);
        };

        void pushChannelsData(const YandexIO::ChannelsData& data) {
            if (subscription_) {
                const auto type = subscription_.value();
                if (type == RequestChannelType::ALL) {
                    broadcast(data);
                } else {
                    for (auto& channel : data) {
                        if (isRequested(channel, type)) {
                            broadcast({channel});
                            break;
                        }
                    }
                }
            }
        }
    };

} // namespace quasar
