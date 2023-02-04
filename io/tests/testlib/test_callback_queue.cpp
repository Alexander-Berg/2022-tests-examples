#include "test_callback_queue.h"

#include <yandex_io/libs/logging/logging.h>

#include <util/system/yassert.h>

using namespace quasar;

TestCallbackQueue::TestCallbackQueue()
    : tid_(std::this_thread::get_id())
{
}
/** ICallbackQueue Interface start **/
std::string TestCallbackQueue::name() const {
    return "test";
};

void TestCallbackQueue::add(std::function<void()> callback) {
    YIO_LOG_DEBUG("Add callback");
    Y_VERIFY(isWorkingThread()); // should always be called from main thread
    Y_VERIFY(callback);
    callback();
}
void TestCallbackQueue::add(std::function<void()> callback, Lifetime::Tracker /*tracker*/) {
    add(std::move(callback));
}

bool TestCallbackQueue::tryAdd(std::function<void()> /*callback*/) {
    throw std::runtime_error("Not implemented");
}

bool TestCallbackQueue::tryAdd(std::function<void()> callback, Lifetime::Tracker /*tracker*/) {
    return tryAdd(std::move(callback));
}

void TestCallbackQueue::addDelayed(std::function<void()> callback, std::chrono::milliseconds delay) {
    YIO_LOG_DEBUG("Add delayed callback");
    Y_VERIFY(isWorkingThread()); // should always be called from main thread
    // todo: order by delay
    Y_VERIFY(callback);
    DelayedCallback delayedCallback{.callback = std::move(callback), .delay = delay};
    delayedQueue_.push(std::move(delayedCallback));
}
void TestCallbackQueue::addDelayed(std::function<void()> callback, std::chrono::milliseconds delay, Lifetime::Tracker /*tracker*/) {
    addDelayed(std::move(callback), delay);
}

void TestCallbackQueue::wait(AwatingType /*awaitingType*/) {
    throw std::runtime_error("Not implemented");
}

bool TestCallbackQueue::isWorkingThread() const noexcept {
    return tid_ == std::this_thread::get_id();
}

size_t TestCallbackQueue::size() const {
    throw std::runtime_error("Not implemented");
};

void TestCallbackQueue::setMaxSize(size_t /*size*/) {
    throw std::runtime_error("Not implemented");
}
/** ICallbackQueue Interface end **/

void TestCallbackQueue::pumpDelayedQueueUntilEmpty() {
    Y_VERIFY(isWorkingThread()); // should always be called from main thread
    while (!delayedQueue_.empty()) {
        pumpDelayedCallback();
    }
}

void TestCallbackQueue::pumpDelayedCallback() {
    Y_VERIFY(isWorkingThread()); // should always be called from main thread
    Y_VERIFY(!delayedQueue_.empty());
    auto callback = std::move(delayedQueue_.front());
    delayedQueue_.pop();
    callback.callback();
}

size_t TestCallbackQueue::delayedCallbackCount() const {
    return delayedQueue_.size();
}

std::chrono::milliseconds TestCallbackQueue::firstDelayedCallbackTimeout() const {
    Y_VERIFY(!delayedQueue_.empty());
    return delayedQueue_.front().delay;
}
