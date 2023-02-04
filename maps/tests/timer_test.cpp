#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

#include <maps/analyzer/libs/event_loop/include/event_loop.h>
#include <maps/analyzer/libs/event_loop/include/event.h>

#include <chrono>
#include <thread>


using namespace maps::analyzer::event_loop;


TEST(TimerTests, Timer) {
    EventLoop eloop;
    eloop.start();

    int v = 0;
    auto t = timer(eloop, [&](Event*, short) {
        v += 1;
    });

    auto f = t->on(std::chrono::milliseconds(1));
    f.wait();
    eloop.stop();

    EXPECT_EQ(v, 1);
}

TEST(TimerTests, Timer2) {
    EventLoop eloop;
    eloop.start();

    std::promise<void> done;

    int v = 0;
    auto t = timer(eloop, [&](Event* self, short) {
        v += 1;
        if (v < 10) {
            self->on(std::chrono::milliseconds(1));
        } else {
            done.set_value();
        }
    });
    t->on(std::chrono::milliseconds(1));

    auto f = done.get_future();
    f.wait();

    eloop.stop();
    EXPECT_EQ(v, 10);
}
