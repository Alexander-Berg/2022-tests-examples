#include "library/cpp/testing/gtest_extensions/assertions.h"
#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

#include <maps/analyzer/libs/event_loop/include/event_loop.h>
#include <maps/analyzer/libs/event_loop/include/queue.h>
#include <maps/analyzer/libs/requests/include/multi_socket.h>

#include <chrono>
#include <future>
#include <thread>


using namespace maps::analyzer::event_loop;
using namespace maps::analyzer::requests;

const std::string URL = "ya.ru";


TEST(MultiSocketTests, CheckThread) {
    EventLoop eloop;
    eloop.start();
    Request req{URL};
    MultiSocket multi{eloop};
    EXPECT_THROW(multi.perform(std::move(req), {}, ResponseHandlers{}), maps::Exception);

    std::promise<void> donePromise;
    std::future<void> done = donePromise.get_future();
    Queue<Request> q{eloop, [&](auto&& r) {
        EXPECT_NO_THROW(multi.perform(std::move(r), {}, ResponseHandlers{
            .onResponse = [&](auto&&) { donePromise.set_value(); }
        }));
    }};
    Request req2{URL};
    q.push(std::move(req2));

    q.join();
    EXPECT_EQ(done.wait_for(std::chrono::seconds(1)), std::future_status::ready);
    eloop.stop();
}
