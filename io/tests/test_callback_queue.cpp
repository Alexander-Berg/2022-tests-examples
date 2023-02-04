#include <yandex_io/libs/threading/callback_queue.h>

#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

#include <chrono>
#include <future>

Y_UNIT_TEST_SUITE_F(TestCallbackQueue, QuasarUnitTestFixture) {
    Y_UNIT_TEST(testCallbackQueueAdd) {
        quasar::CallbackQueue worker;

        std::promise<void> p;
        auto f = p.get_future();

        worker.add([&p]() { p.set_value(); });

        UNIT_ASSERT(f.wait_for(std::chrono::seconds(10)) == std::future_status::ready);
    }

    Y_UNIT_TEST(testCallbackQueueAddDelayed) {
        quasar::CallbackQueue worker;

        std::promise<void> p;
        auto f = p.get_future();

        worker.addDelayed([&p]() { p.set_value(); }, std::chrono::seconds(1));

        UNIT_ASSERT(f.wait_for(std::chrono::seconds(10)) == std::future_status::ready);
    }

    Y_UNIT_TEST(testCallbackQueueAddDelayedOrder1) {
        quasar::CallbackQueue worker;

        std::promise<void> p1;
        auto f1 = p1.get_future();

        std::promise<bool> p2;
        auto f2 = p2.get_future();

        worker.addDelayed([&p1]() {
            p1.set_value();
        }, std::chrono::seconds(1));

        worker.addDelayed([&p2, &f1]() {
            p2.set_value(f1.wait_for(std::chrono::seconds(0)) == std::future_status::ready);
        }, std::chrono::seconds(2));

        UNIT_ASSERT(f2.wait_for(std::chrono::seconds(10)) == std::future_status::ready);
        UNIT_ASSERT(f2.get());
    }

    Y_UNIT_TEST(testCallbackQueueAddDelayedOrder2) {
        quasar::CallbackQueue worker;

        std::promise<void> p1;
        auto f1 = p1.get_future();

        std::promise<bool> p2;
        auto f2 = p2.get_future();

        worker.addDelayed([&p2, &f1]() {
            p2.set_value(f1.wait_for(std::chrono::seconds(0)) == std::future_status::ready);
        }, std::chrono::seconds(2));

        worker.addDelayed([&p1]() {
            p1.set_value();
        }, std::chrono::seconds(1));

        UNIT_ASSERT(f2.wait_for(std::chrono::seconds(10)) == std::future_status::ready);
        UNIT_ASSERT(f2.get());
    }

    Y_UNIT_TEST(testWaitEgoist) {
        quasar::CallbackQueue worker;

        std::atomic<bool> p1{false};
        std::atomic<bool> p2{false};
        worker.add([&] {
            p1 = true;
            std::this_thread::sleep_for(std::chrono::seconds{1});
        });
        worker.add([&] {
            p2 = true;
            std::this_thread::sleep_for(std::chrono::seconds{1});
        });

        worker.wait(quasar::CallbackQueue::AwatingType::EGOIST);

        UNIT_ASSERT(p1.load());
        UNIT_ASSERT(p2.load());
    }

    Y_UNIT_TEST(testWaitAltruist) {
        quasar::CallbackQueue worker;

        std::atomic<bool> p1{false};
        std::atomic<bool> p2{false};
        std::atomic<bool> p3{false};
        std::atomic<bool> p4{false};
        std::atomic<bool> p5{false};
        worker.add([&] {
            p1 = true;
            std::this_thread::sleep_for(std::chrono::seconds{1});
            worker.add([&] {
                p3 = true;
            });
        });
        worker.add([&] {
            p2 = true;
            worker.add([&] {
                p4 = true;
                std::this_thread::sleep_for(std::chrono::seconds{1});
                worker.add([&] {
                    p5 = true;
                });
            });
        });

        worker.wait(quasar::CallbackQueue::AwatingType::ALTRUIST);

        UNIT_ASSERT(p1.load());
        UNIT_ASSERT(p2.load());
        UNIT_ASSERT(p3.load());
        UNIT_ASSERT(p4.load());
        UNIT_ASSERT(p5.load());
    }
}
