#include <yandex_io/libs/signals/signal_ready.h>

#include <yandex_io/libs/base/named_callback_queue.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/tests/testlib/test_utils.h>

#include <library/cpp/testing/unittest/registar.h>

#include <atomic>

using namespace quasar;
using namespace quasar::TestUtils;

Y_UNIT_TEST_SUITE(SignalReady) {
    Y_UNIT_TEST(testSyncBefore)
    {
        Lifetime lifetime;
        SignalReady signal;
        bool ready{false};

        signal.connect([&] { ready = true; }, lifetime);
        UNIT_ASSERT_EQUAL(ready, false);
        signal();
        UNIT_ASSERT_EQUAL(ready, true);
    }

    Y_UNIT_TEST(testSyncAfter)
    {
        Lifetime lifetime;
        SignalReady signal;
        bool ready{false};

        signal();
        UNIT_ASSERT_EQUAL(ready, false);

        signal.connect([&] { ready = true; }, lifetime);
        UNIT_ASSERT_EQUAL(ready, true);
    }

    Y_UNIT_TEST(testSyncManyTimes)
    {
        Lifetime lifetime;
        SignalReady signal;
        std::atomic<int> ready{false};

        signal();
        signal();
        signal();
        signal();
        UNIT_ASSERT_EQUAL(ready, 0);

        signal.connect([&] { ++ready; }, lifetime);
        UNIT_ASSERT_EQUAL(ready, 1);

        signal();
        signal();
        signal();
        UNIT_ASSERT_EQUAL(ready, 1);
    }

    Y_UNIT_TEST(testASyncBefore)
    {
        auto callbackQueue = std::make_shared<NamedCallbackQueue>("ASync");
        Lifetime lifetime;
        SignalReady signal;
        bool ready{false};

        signal.connect(
            [&] {
                Y_ENSURE_THREAD(callbackQueue);
                UNIT_ASSERT(!ready);
                ready = true;
            }, lifetime, callbackQueue);
        UNIT_ASSERT_EQUAL(ready, false);

        signal();
        flushCallbackQueue(callbackQueue);

        UNIT_ASSERT_EQUAL(ready, true);
    }

    Y_UNIT_TEST(testASyncAfter)
    {
        auto callbackQueue = std::make_shared<NamedCallbackQueue>("ASync");
        Lifetime lifetime;
        SignalReady signal;
        bool ready{false};

        signal();
        flushCallbackQueue(callbackQueue);
        UNIT_ASSERT_EQUAL(ready, false);

        signal.connect(
            [&] {
                Y_ENSURE_THREAD(callbackQueue);
                UNIT_ASSERT(!ready);
                ready = true;
            }, lifetime, callbackQueue);
        flushCallbackQueue(callbackQueue);
        UNIT_ASSERT_EQUAL(ready, true);
    }
}
