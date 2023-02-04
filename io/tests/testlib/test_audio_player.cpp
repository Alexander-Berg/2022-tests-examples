#include "test_audio_player.h"

#include <yandex_io/libs/logging/logging.h>

using namespace quasar::TestUtils;

void TestAudioPlayer::play()
{
    {
        std::lock_guard<std::mutex> lock(mutex_);

        if (auto slistener = listener_.lock()) {
            slistener->onPlayingBegin(shared_from_this());
        }
    }

    playStartedEvent_->set();
}

void TestAudioPlayer::pause()
{
    std::lock_guard<std::mutex> lock(mutex_);
    YIO_LOG_INFO("pause");

    if (auto slistener = listener_.lock()) {
        slistener->onPlayingPaused(shared_from_this());
    }
}

void TestAudioPlayer::cancel()
{
    YIO_LOG_INFO("cancel");
    playCancelledEvent_->set();
}

void TestAudioPlayer::playData(SpeechKit::SoundBuffer::SharedPtr /* buffer */)
{
}

void TestAudioPlayer::setDataEnd()
{
    {
        std::lock_guard<std::mutex> lock(mutex_);
        YIO_LOG_INFO("setDataEnd");

        if (auto slistener = listener_.lock()) {
            slistener->onPlayingDone(shared_from_this());
        }
    }

    playFinishedEvent_->set();
}

void TestAudioPlayer::setVolume(float /* gain */)
{
}

float TestAudioPlayer::getVolume() const {
    return 1.f;
}

void TestAudioPlayer::subscribe(SpeechKit::AudioPlayer::AudioPlayerListener::WeakPtr listener)
{
    std::lock_guard<std::mutex> lock(mutex_);
    listener_ = listener;
    condVar_.notify_one();
}

void TestAudioPlayer::unsubscribe(SpeechKit::AudioPlayer::AudioPlayerListener::WeakPtr /* listener */)
{
    std::lock_guard<std::mutex> lock(mutex_);
    listener_.reset();
}

void TestAudioPlayer::waitForListener()
{
    std::unique_lock<std::mutex> lock(mutex_);

    condVar_.wait_for(lock, std::chrono::seconds(1), [=]() {
        return listener_.lock() != nullptr;
    });
}

bool TestAudioPlayer::waitForPlayStarted()
{
    auto result = playStartedEvent_->wait();
    playStartedEvent_ = std::make_shared<TestUtils::ConcurrentEvent>();
    return result;
}

bool TestAudioPlayer::waitForPlayFinished()
{
    auto result = playFinishedEvent_->wait();
    playFinishedEvent_ = std::make_shared<TestUtils::ConcurrentEvent>();
    return result;
}

bool TestAudioPlayer::waitForPlayCancelled()
{
    auto result = playCancelledEvent_->wait();
    playCancelledEvent_ = std::make_shared<TestUtils::ConcurrentEvent>();
    return result;
}
