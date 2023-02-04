#include "test_tts_data_provider.h"

#include <yandex_io/libs/logging/logging.h>

using namespace quasar::TestUtils;

void TestTTSDataProvider::init(Listener::WeakPtr listener)
{
    YIO_LOG_INFO("init");
    std::scoped_lock lock(mutex_);
    listener_ = std::move(listener);
    condVar_.notify_one();
}

void TestTTSDataProvider::start()
{
    YIO_LOG_INFO("start");
    if (auto slistener = listener_.lock()) {
        slistener->onStreamBegin();
    }
}

void TestTTSDataProvider::pause()
{
    YIO_LOG_INFO("pause");
    // nothing
}

void TestTTSDataProvider::setStreamEnd()
{
    YIO_LOG_INFO("setStreamEnd");
    if (auto slistener = listener_.lock()) {
        slistener->onStreamEnd();
    }
}

void TestTTSDataProvider::waitForListener()
{
    std::unique_lock lock(mutex_);
    condVar_.wait(lock, [this] { return listener_.lock() != nullptr; });
}
