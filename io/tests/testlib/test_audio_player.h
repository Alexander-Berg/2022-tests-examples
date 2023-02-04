#pragma once

#include "concurrent_queue.h"

#include <speechkit/AudioPlayer.h>

#include <memory>
#include <mutex>

namespace quasar {

    namespace TestUtils {

        class TestAudioPlayer
            : public SpeechKit::AudioPlayer,
              public std::enable_shared_from_this<TestAudioPlayer> {
        public:
            void play() override;
            void pause() override;
            void cancel() override;
            void playData(SpeechKit::SoundBuffer::SharedPtr buffer) override;
            void setDataEnd() override;
            void setVolume(float gain) override;
            float getVolume() const override;

            void subscribe(SpeechKit::AudioPlayer::AudioPlayerListener::WeakPtr listener) override;
            void unsubscribe(SpeechKit::AudioPlayer::AudioPlayerListener::WeakPtr listener) override;

            void waitForListener();
            bool waitForPlayStarted();
            bool waitForPlayFinished();
            bool waitForPlayCancelled();

        private:
            std::mutex mutex_;
            std::condition_variable condVar_;
            SpeechKit::AudioPlayer::AudioPlayerListener::WeakPtr listener_;

            std::shared_ptr<ConcurrentEvent> playStartedEvent_ = std::make_shared<TestUtils::ConcurrentEvent>();
            std::shared_ptr<ConcurrentEvent> playFinishedEvent_ = std::make_shared<TestUtils::ConcurrentEvent>();
            std::shared_ptr<ConcurrentEvent> playCancelledEvent_ = std::make_shared<TestUtils::ConcurrentEvent>();
        };

    } // namespace TestUtils

} // namespace quasar
