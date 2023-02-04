#pragma once

#include <condition_variable>
#include <mutex>
#include <optional>
#include <queue>

namespace quasar {

    namespace TestUtils {

        class ConcurrentEvent {
        public:
            void set() {
                std::lock_guard<std::mutex> lock(mutex_);
                set_ = true;
                cv_.notify_one();
            }

            bool wait() {
                std::unique_lock<std::mutex> lock(mutex_);
                return cv_.wait_for(lock, std::chrono::seconds(5), [=]() {
                    return set_ == true;
                });
            }

            void waitForever() {
                std::unique_lock<std::mutex> lock(mutex_);
                cv_.wait(lock, [=]() {
                    return set_ == true;
                });
            }

        private:
            bool set_ = false;
            std::mutex mutex_;
            std::condition_variable cv_;
        };

        template <class Item>
        class ConcurrentQueue {
        public:
            using SharedPtr = std::shared_ptr<ConcurrentQueue<Item>>;

        public:
            void push(Item item) {
                std::lock_guard<std::mutex> lock(mutex);

                queue.push(item);
                queueNotEmptyCondition.notify_one();
            }

            Item waitForNextItem() {
                std::unique_lock<std::mutex> lock(mutex);
                queueNotEmptyCondition.wait(lock, [this] { return !queue.empty(); });

                Item item = queue.front();
                queue.pop();

                return item;
            }

            std::optional<Item> waitForNextItem(std::chrono::seconds timeout) {
                std::unique_lock<std::mutex> lock(mutex);
                if (queueNotEmptyCondition.wait_for(lock, timeout, [this] { return !queue.empty(); })) {
                    Item item = queue.front();
                    queue.pop();

                    return item;
                }

                return {};
            }

        private:
            std::mutex mutex;
            std::queue<Item> queue;
            std::condition_variable queueNotEmptyCondition;
        };

    } // namespace TestUtils

} // namespace quasar
