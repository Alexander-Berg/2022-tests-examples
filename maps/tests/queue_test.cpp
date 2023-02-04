#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

#include <maps/analyzer/libs/event_loop/include/event_loop.h>
#include <maps/analyzer/libs/event_loop/include/queue.h>
#include <maps/analyzer/libs/event_loop/include/batch_queue.h>
#include <maps/analyzer/libs/event_loop/include/batch_pull_queue.h>
#include <maps/analyzer/libs/event_loop/include/pull_queue.h>

#include <chrono>
#include <string>
#include <thread>


using namespace maps::analyzer::event_loop;


TEST(QueueTests, Sum) {
    EventLoop eloop;
    eloop.start();

    std::size_t result = 0;
    Queue<std::size_t> q(eloop, [&](std::size_t v) { result += v; });

    static constexpr std::size_t THREADS = 8;
    static constexpr std::size_t COUNT_TO = 10000;
    static constexpr std::size_t EXPECTED_SUM = 400040000;

    std::vector<std::thread> ths;

    for (std::size_t i = 0; i < THREADS; ++i) {
        ths.emplace_back([&]() {
            for (std::size_t j = 0; j <= COUNT_TO; ++j) {
                q.push(j);
            }
        });
    }

    for (auto& th: ths) {
        th.join();
    }

    q.join();
    eloop.stop();

    EXPECT_EQ(result, EXPECTED_SUM);
}

TEST(QueueTests, ComplexType) {
    EventLoop eloop;
    eloop.start();

    std::vector<std::string> items;
    Queue<std::string> q(eloop, [&](std::string&& s) {
        items.push_back(std::move(s));
    });

    std::vector<std::thread> ths;
    for (std::size_t i = 0; i < 4; ++i) {
        ths.emplace_back([&]() {
            for (std::size_t j = 0; j < 100; ++j) {
                q.push(std::to_string(j));
            }
        });
    }

    for (auto& th: ths) {
        th.join();
    }

    q.join();
    eloop.stop();

    EXPECT_EQ(items.size(), 400u);
}

TEST(BatchQueueTests, Sum) {
    EventLoop eloop;
    eloop.start();

    std::size_t result = 0;
    BatchQueue<std::size_t> q(eloop, [&](std::size_t v) { result += v; });

    static constexpr std::size_t THREADS = 8;
    static constexpr std::size_t COUNT_TO = 10000;
    static constexpr std::size_t EXPECTED_SUM = 400040000;

    std::vector<std::thread> ths;

    for (std::size_t i = 0; i < THREADS; ++i) {
        ths.emplace_back([&]() {
            auto w = q.writer();
            for (std::size_t j = 0; j <= COUNT_TO; ++j) {
                w.push(j);
            }
            w.flush();
        });
    }

    for (auto& th: ths) {
        th.join();
    }

    q.join();
    eloop.stop();

    EXPECT_EQ(result, EXPECTED_SUM);
}

TEST(PullQueueTests, Sum) {
    std::size_t result = 0;
    PullQueue<std::size_t> q;

    static constexpr std::size_t THREADS = 8;
    static constexpr std::size_t COUNT_TO = 1000;
    static constexpr std::size_t EXPECTED_SUM = 4004000;

    std::vector<std::thread> ths;

    for (std::size_t i = 0; i < THREADS; ++i) {
        ths.emplace_back([&]() {
            for (std::size_t j = 0; j <= COUNT_TO; ++j) {
                q.push(j);
            }
        });
    }

    for (auto& th: ths) {
        th.join();
    }

    q.close();

    for (auto v = q.pop(); v.has_value(); v = q.pop()) {
        result += v.value();
    }

    EXPECT_EQ(result, EXPECTED_SUM);
}

TEST(PullQueueTests, ComplexType) {
    std::vector<std::string> items;
    PullQueue<std::string> q;

    std::vector<std::thread> ths;
    for (std::size_t i = 0; i < 4; ++i) {
        ths.emplace_back([&]() {
            for (std::size_t j = 0; j < 100; ++j) {
                q.push(std::to_string(j));
            }
        });
    }

    for (auto& th: ths) {
        th.join();
    }

    q.close();

    for (auto s = q.pop(); s.has_value(); s = q.pop()) {
        items.emplace_back(std::move(s.value()));
    }

    EXPECT_EQ(items.size(), 400u);
}

TEST(PullQueueTests, Join) {
    PullQueue<int> q;
    int result = 0;
    std::thread ([&]() {
        q.push(1);
        q.push(2);
        q.join();
    }).detach();

    for (auto v = q.pop(); v.has_value(); v = q.pop()) {
       result += v.value();
    }

    EXPECT_EQ(result, 3);
}

TEST(EventLoopTests, Empty) {
    std::atomic<bool> s{true};
    std::thread ([&](){
        EventLoop loop;
        s.store(loop.step());
    }).detach();

    std::this_thread::sleep_for(std::chrono::seconds(1));

    EXPECT_FALSE(s.load());
}

TEST(BatchPullQueueTests, Sum) {
    std::size_t result = 0;
    BatchPullQueue<std::size_t> q;

    static constexpr std::size_t THREADS = 8;
    static constexpr std::size_t COUNT_TO = 10000;
    static constexpr std::size_t EXPECTED_SUM = 400040000;

    std::vector<std::thread> ths;

    for (std::size_t i = 0; i < THREADS; ++i) {
        ths.emplace_back([&]() {
            auto w = q.writer();
            for (std::size_t j = 0; j <= COUNT_TO; ++j) {
                w.push(j);
            }
            w.flush();
        });
    }

    for (auto& th: ths) {
        th.join();
    }

    q.close();

    for (auto v = q.pop(); v.has_value(); v = q.pop()) {
        result += v.value();
    }

    EXPECT_FALSE(q.size());
    EXPECT_EQ(result, EXPECTED_SUM);
}

TEST(BatchPullQueueTests, Join) {
    BatchPullQueue<int> q;
    int result = 0;
    std::thread ([&]() {
        auto w = q.writer();
        w.push(1);
        w.push(2);
        w.flush();
        q.join();
    }).detach();

    for (auto v = q.pop(); v.has_value(); v = q.pop()) {
       result += v.value();
    }

    EXPECT_FALSE(q.size());
    EXPECT_EQ(result, 3);
}
