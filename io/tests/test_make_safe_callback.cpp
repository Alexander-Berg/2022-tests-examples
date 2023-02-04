#include <yandex_io/libs/threading/callback_queue.h>

#include <yandex_io/tests/testlib/test_utils.h>

#include <library/cpp/testing/unittest/registar.h>

#include <atomic>
#include <chrono>
#include <future>

using namespace quasar;
using namespace quasar::TestUtils;

Y_UNIT_TEST_SUITE(TestMakeSafeCallback) {
    Y_UNIT_TEST(test1) {
        Lifetime lifetime;

        int flag = 0;
        auto job = makeSafeCallback([&](int v) { flag = v; }, lifetime);

        UNIT_ASSERT_VALUES_EQUAL(flag, 0);
        job(1);
        UNIT_ASSERT_VALUES_EQUAL(flag, 1);
        job(2);
        UNIT_ASSERT_VALUES_EQUAL(flag, 2);
        lifetime.die();
        job(5);
        UNIT_ASSERT_VALUES_EQUAL(flag, 2);
    }

    Y_UNIT_TEST(test2) {
        Lifetime lifetime;
        auto callbackQueue = std::make_shared<CallbackQueue>();
        std::atomic<bool> suspend{true};
        callbackQueue->add([&] { waitUntil([&]() { return !suspend; }); });

        std::atomic<int> flag{0};
        auto job = makeSafeCallback([&](int v) { flag = v * 10; }, lifetime, callbackQueue);

        UNIT_ASSERT_VALUES_EQUAL(flag.load(), 0);

        job(1);
        UNIT_ASSERT_VALUES_EQUAL(flag.load(), 0);
        suspend = false;

        flushCallbackQueue(callbackQueue);
        UNIT_ASSERT_VALUES_EQUAL(flag.load(), 10);

        job(2);
        flushCallbackQueue(callbackQueue);
        UNIT_ASSERT_VALUES_EQUAL(flag.load(), 20);

        lifetime.die();
        UNIT_ASSERT_VALUES_EQUAL(lifetime.expired(), true);

        job(3);
        flushCallbackQueue(callbackQueue);

        UNIT_ASSERT_VALUES_EQUAL(flag.load(), 20);
    }
}
