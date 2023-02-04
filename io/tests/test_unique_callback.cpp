#include <yandex_io/libs/threading/callback_queue.h>
#include <yandex_io/libs/threading/unique_callback.h>

#include <yandex_io/tests/testlib/test_utils.h>

#include <library/cpp/testing/unittest/registar.h>

#include <atomic>

using namespace quasar;
using namespace quasar::TestUtils;

Y_UNIT_TEST_SUITE(UniqueCallback) {
    Y_UNIT_TEST(testCtor)
    {
        UNIT_ASSERT_NO_EXCEPTION(UniqueCallback(nullptr));
        UNIT_ASSERT_NO_EXCEPTION(UniqueCallback(nullptr, UniqueCallback::ReplaceType::REPLACE_FIRST));
        UNIT_ASSERT_NO_EXCEPTION(UniqueCallback(std::shared_ptr<ICallbackQueue>{}));
    }

    Y_UNIT_TEST(testSimple)
    {
        Lifetime lifetime;
        auto callbackQueue = std::make_shared<CallbackQueue>();

        std::atomic<bool> suspendCallbackQueue{true};
        callbackQueue->add([&] { waitUntil([&]() { return !suspendCallbackQueue.load(); }); });

        std::atomic<int> flag = 0;
        UniqueCallback uc(callbackQueue);
        UNIT_ASSERT_VALUES_EQUAL(uc.isScheduled(), false);

        uc.execute([&] { flag = 1; }, lifetime);
        UNIT_ASSERT_VALUES_EQUAL(uc.isScheduled(), true);

        uc.execute([&] { flag = 2; }, lifetime);
        UNIT_ASSERT_VALUES_EQUAL(uc.isScheduled(), true);
        suspendCallbackQueue = false;

        waitUntil([&]() { return flag != 0; });

        UNIT_ASSERT_VALUES_EQUAL(flag.load(), 2);
        UNIT_ASSERT_VALUES_EQUAL(uc.isScheduled(), false);
    }

    Y_UNIT_TEST(testReset)
    {
        Lifetime lifetime;
        auto callbackQueue = std::make_shared<CallbackQueue>();

        std::atomic<bool> suspendCallbackQueue{true};
        callbackQueue->add([&] { waitUntil([&]() { return !suspendCallbackQueue.load(); }); });

        std::atomic<int> flag = 0;
        UniqueCallback uc(callbackQueue);
        UNIT_ASSERT_VALUES_EQUAL(uc.isScheduled(), false);

        uc.execute([&] { flag = 1; }, lifetime);
        UNIT_ASSERT_VALUES_EQUAL(uc.isScheduled(), true);

        uc.reset(); // Reset
        UNIT_ASSERT_VALUES_EQUAL(uc.isScheduled(), false);
        suspendCallbackQueue = false;

        std::atomic<int> ready = 0;
        callbackQueue->add([&] { ready = 1; });
        waitUntil([&]() { return ready.load(); });
        UNIT_ASSERT_VALUES_EQUAL(flag.load(), 0);
    }

    Y_UNIT_TEST(testReplaceFirst)
    {
        Lifetime lifetime;
        auto callbackQueue = std::make_shared<CallbackQueue>();

        std::atomic<bool> suspendCallbackQueue{true};
        callbackQueue->add([&] { waitUntil([&]() { return !suspendCallbackQueue.load(); }); });

        int flag = 0;
        std::atomic<bool> ready{false};
        UniqueCallback uc(callbackQueue, UniqueCallback::ReplaceType::REPLACE_FIRST);
        uc.execute([&] { flag *= 10; flag += 1; }, lifetime);
        callbackQueue->add([&] { flag *= 10; flag += 2; });
        uc.execute([&] { flag *= 10; flag += 3; }, lifetime);
        callbackQueue->add([&] { ready = true; });
        suspendCallbackQueue = false;

        waitUntil([&]() { return ready.load(); });

        UNIT_ASSERT_VALUES_EQUAL(flag, 32);
    }

    Y_UNIT_TEST(testInsertBack)
    {
        Lifetime lifetime;
        auto callbackQueue = std::make_shared<CallbackQueue>();

        std::atomic<bool> suspendCallbackQueue{true};
        callbackQueue->add([&] { waitUntil([&]() { return !suspendCallbackQueue.load(); }); });

        int flag = 0;
        std::atomic<bool> ready{false};
        UniqueCallback uc(callbackQueue, UniqueCallback::ReplaceType::INSERT_BACK);
        uc.execute([&] { flag *= 10; flag += 1; }, lifetime);
        callbackQueue->add([&] { flag *= 10; flag += 2; });
        uc.execute([&] { flag *= 10; flag += 3; }, lifetime);
        callbackQueue->add([&] { ready = true; });
        suspendCallbackQueue = false;

        waitUntil([&]() { return ready.load(); });

        UNIT_ASSERT_VALUES_EQUAL(flag, 23);
    }

    Y_UNIT_TEST(testDelayed1)
    {
        Lifetime lifetime;
        auto callbackQueue = std::make_shared<CallbackQueue>();

        std::atomic<bool> suspendCallbackQueue{true};
        callbackQueue->add([&] { waitUntil([&]() { return !suspendCallbackQueue.load(); }); });

        int flag = 0;
        std::atomic<bool> ready{false};
        UniqueCallback uc(callbackQueue);
        uc.executeDelayed([&] { flag *= 10; flag += 1; }, std::chrono::seconds{1}, lifetime);
        callbackQueue->add([&] { flag *= 10; flag += 2; });
        callbackQueue->addDelayed([&] { ready = true; }, std::chrono::seconds{2});
        suspendCallbackQueue = false;

        waitUntil([&]() { return ready.load(); });

        UNIT_ASSERT_VALUES_EQUAL(flag, 21);
    }

    Y_UNIT_TEST(testDelayed2)
    {
        Lifetime lifetime;
        auto callbackQueue = std::make_shared<CallbackQueue>();

        std::atomic<bool> suspendCallbackQueue{true};
        callbackQueue->add([&] { waitUntil([&]() { return !suspendCallbackQueue.load(); }); });

        int flag = 0;
        std::atomic<bool> ready{false};
        UniqueCallback uc(callbackQueue);
        uc.executeDelayed([&] { flag *= 10; flag += 1; }, std::chrono::seconds{1}, lifetime);
        callbackQueue->add([&] { flag *= 10; flag += 2; });
        uc.executeDelayed([&] { flag *= 10; flag += 3; }, std::chrono::seconds{1}, lifetime);
        callbackQueue->addDelayed([&] { ready = true; }, std::chrono::seconds{2});
        suspendCallbackQueue = false;

        waitUntil([&]() { return ready.load(); });

        UNIT_ASSERT_VALUES_EQUAL(flag, 23);
    }

    Y_UNIT_TEST(testDelayed3)
    {
        Lifetime lifetime;
        auto callbackQueue = std::make_shared<CallbackQueue>();

        std::atomic<bool> suspendCallbackQueue{true};
        callbackQueue->add([&] { waitUntil([&]() { return !suspendCallbackQueue.load(); }); });

        int flag = 0;
        UniqueCallback uc(callbackQueue, UniqueCallback::ReplaceType::REPLACE_FIRST);
        uc.executeDelayed([&] { flag = 100; }, std::chrono::seconds{10000}, lifetime); // Invoke never
        uc.execute([&] { flag = 1; }, lifetime);

        suspendCallbackQueue = false;

        flushCallbackQueue(callbackQueue);

        UNIT_ASSERT_VALUES_EQUAL(flag, 1);
    }
}
