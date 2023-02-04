#include <maps/libs/concurrent/include/queue.h>
#include <maps/libs/concurrent/include/latch.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <array>
#include <atomic>
#include <memory>
#include <string>
#include <thread>
#include <vector>

namespace maps::concurrent::tests {

namespace {

constexpr size_t NUM_THREADS = 4;

using Threads = std::vector<std::thread>;
void joinAll(Threads& threads) {
    for (auto&& thread : threads) {
        thread.join();
    }
}

} // anonymous namespace

Y_UNIT_TEST_SUITE(test_unbounded_blocking_queue) {

Y_UNIT_TEST(queue_must_preserve_content) {
    constexpr size_t SIZE = 500;

    std::array<std::atomic<size_t>, SIZE> counts{};

    UnboundedBlockingQueue<size_t> queue;

    EXPECT_TRUE(queue.empty());

    Latch latch(1);

    Threads consumers;
    Threads producers;

    for (size_t i = 0; i < NUM_THREADS; ++i) {
        producers.emplace_back([&] {
            latch.wait();
            for (size_t i = 0; i < SIZE; ++i) {
                queue.push(i);
            }
        });
    }
    for (size_t i = 0; i < NUM_THREADS; ++i) {
        consumers.emplace_back([&] {
            latch.wait();
            for (size_t i = 0; i < SIZE; ++i) {
                auto maybeE = queue.pop();
                ++counts[maybeE.value()];
            }
        });
    }

    latch.arrive();
    joinAll(consumers);
    joinAll(producers);

    EXPECT_TRUE(queue.empty());

    for (const auto& count : counts) {
        EXPECT_EQ(count, NUM_THREADS);
    }
}

Y_UNIT_TEST(queue_must_preserve_order) {
    UnboundedBlockingQueue<std::string> queue;

    EXPECT_TRUE(queue.empty());

    const std::vector<std::string> values{
        "one", "two", "three", "four", "five", "six", "seven"};
    Latch latch(1);
    std::thread producer([&] {
        latch.wait();
        for (auto&& value : values) {
            queue.push(value);
        }
    });

    std::vector<std::string> out;
    out.reserve(values.size());

    latch.arrive();
    while (out.size() < values.size()) {
        queue.popInto(std::back_inserter(out));
    }
    producer.join();
    EXPECT_EQ(values, out);

    EXPECT_TRUE(queue.empty());
}

Y_UNIT_TEST(queue_provides_no_wait_operations) {
    UnboundedBlockingQueue<std::string> queue;

    const std::string VALUE = "test";
    EXPECT_TRUE(queue.empty());
    EXPECT_FALSE(queue.tryPop());
    queue.push(VALUE);
    EXPECT_FALSE(queue.empty());
    auto maybeValue = queue.tryPop();
    ASSERT_TRUE(maybeValue);
    EXPECT_EQ(VALUE, *maybeValue);
    EXPECT_TRUE(queue.empty());
}

Y_UNIT_TEST(queue_supports_batch_operations) {
    constexpr size_t NUM_ITEMS = 100;

    UnboundedBlockingQueue<size_t> queue;
    std::vector<size_t> items;
    std::atomic<bool> failed(false);
    items.reserve(NUM_ITEMS);
    for (size_t i = 0; i < NUM_ITEMS; ++i) {
        items.push_back(i);
    }
    std::thread t([&] {
        auto i = items.begin(), e = items.end();
        while (i != e) {
            auto next = queue.pushRange(i, e);
            if (next <= i) {
                failed = true;
            }
            i = next;
        }
    });
    EXPECT_FALSE(failed);
    std::vector<size_t> out;
    while (out.size() < NUM_ITEMS) {
        const auto sizeBefore = out.size();
        queue.popInto(std::back_inserter(out));
        EXPECT_GT(out.size(), sizeBefore);
    }
    EXPECT_EQ(items, out);
    t.join();
}

Y_UNIT_TEST(queue_supports_closing) {
    constexpr size_t NUM_ITEMS = 100;
    constexpr size_t NUM_THREADS = 5;

    UnboundedBlockingQueue<size_t> queue;
    std::vector<std::thread> consumers;
    consumers.reserve(NUM_THREADS);
    std::atomic<size_t> processedItems(0);
    std::atomic<size_t> joinableThreads(0);

    for (size_t i = 0; i < NUM_THREADS; ++i) {
        consumers.emplace_back([&] {
            while (auto maybeResult = queue.pop()) {
                ++processedItems;
            }
            ++joinableThreads;
        });
    }

    for (size_t i = 0; i < NUM_ITEMS; ++i) {
        queue.push(i);
    }
    queue.close();
    joinAll(consumers);
    EXPECT_EQ(NUM_ITEMS, processedItems);
    EXPECT_EQ(NUM_THREADS, joinableThreads);
}

Y_UNIT_TEST(queue_throws_on_push_into_closed_queue) {
    UnboundedBlockingQueue<size_t> queue;
    queue.push(1);
    queue.close();
    EXPECT_THROW(queue.push(2), QueueClosed);
    auto out = queue.pop();
    EXPECT_EQ(out, 1u);
    EXPECT_EQ(queue.pop(), std::nullopt);
    EXPECT_THROW(queue.tryPop(), QueueClosed);
}

Y_UNIT_TEST(queue_allows_to_pop_movable_elements_by_value) {
    using MovableType = std::unique_ptr<size_t>;
    UnboundedBlockingQueue<MovableType> queue;
    constexpr size_t NUM_ITEMS = 100;
    std::thread producer([&] {
        for (size_t i = 0; i < NUM_ITEMS; ++i) {
            queue.push(std::make_unique<size_t>(i));
        }
    });
    for (size_t i = 0; i < NUM_ITEMS; ++i) {
        auto maybePtr = queue.pop();
        EXPECT_EQ(i, *maybePtr.value());
    }
    producer.join();
    EXPECT_TRUE(queue.empty());
}

Y_UNIT_TEST(queue_allows_pop_into_after_close) {
    UnboundedBlockingQueue<size_t> queue;
    queue.push(1);
    queue.close();
    EXPECT_FALSE(queue.empty());
    std::vector<size_t> out;
    queue.popInto(std::back_inserter(out));
    EXPECT_EQ(out.size(), 1u);
}

} //Y_UNIT_TEST_SUITE(test_bounded_blocking_queue)

} //namespace maps::concurrent::tests
