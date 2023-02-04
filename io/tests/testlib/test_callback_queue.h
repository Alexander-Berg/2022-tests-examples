#pragma once

#include <yandex_io/libs/threading/i_callback_queue.h>

#include <functional>
#include <queue>
#include <thread>

namespace quasar {

    class TestCallbackQueue: public ICallbackQueue {
    public:
        TestCallbackQueue();
        /** ICallbackQueue Interface start **/
        std::string name() const override;

        void add(std::function<void()> callback) override;
        void add(std::function<void()> callback, Lifetime::Tracker /*tracker*/) override;

        bool tryAdd(std::function<void()> callback) override;
        bool tryAdd(std::function<void()> callback, Lifetime::Tracker /*tracker*/) override;

        void addDelayed(std::function<void()> callback, std::chrono::milliseconds /*delay*/) override;
        void addDelayed(std::function<void()> callback, std::chrono::milliseconds delay, Lifetime::Tracker /*tracker*/) override;

        void wait(AwatingType /*awaitingType*/) override;

        bool isWorkingThread() const noexcept override;

        size_t size() const override;
        void setMaxSize(size_t /*size*/) override;
        /** ICallbackQueue Interface end **/

        void pumpDelayedQueueUntilEmpty();
        void pumpDelayedCallback();

        size_t delayedCallbackCount() const;
        // callbacks are not sorted by timeouts
        // returns timeout for first added callback in queue
        std::chrono::milliseconds firstDelayedCallbackTimeout() const;

    private:
        struct DelayedCallback {
            std::function<void()> callback;
            std::chrono::milliseconds delay;
        };
        std::queue<DelayedCallback> delayedQueue_;
        const std::thread::id tid_;
    };

} // namespace quasar
