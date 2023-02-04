#include <yandex_io/libs/signals/signal.h>

#include <yandex_io/libs/base/named_callback_queue.h>

#include <library/cpp/testing/unittest/registar.h>

#include <atomic>

using namespace quasar;

Y_UNIT_TEST_SUITE(Signal) {
    Y_UNIT_TEST(testSync)
    {
        Lifetime lifetime;
        Signal<ISignal<int>> signal;
        int result = 0;

        signal.connect([&](int value) { result = value; }, lifetime);
        UNIT_ASSERT_EQUAL(result, 0);
        signal(5);
        UNIT_ASSERT_EQUAL(result, 5);
    }

    Y_UNIT_TEST(testLifetimeDie)
    {
        Lifetime lifetime;
        Signal<ISignal<int>> signal;
        int result = 0;

        signal.connect([&](int value) { result = value; }, lifetime);
        UNIT_ASSERT_EQUAL(result, 0);
        signal(5);
        UNIT_ASSERT_EQUAL(result, 5);
        lifetime.die();
        signal(10);
        UNIT_ASSERT_EQUAL(result, 5);
    }

    Y_UNIT_TEST(testASync)
    {
        auto callbackQueue = std::make_shared<NamedCallbackQueue>("ASync");
        Lifetime lifetime;
        Signal<ISignal<void>> signal;
        std::atomic<std::thread::id> threadId{std::this_thread::get_id()};

        signal.connect([&]() { threadId = std::this_thread::get_id(); }, lifetime, callbackQueue);
        UNIT_ASSERT_EQUAL(threadId, std::this_thread::get_id());
        signal();
        while (threadId == std::this_thread::get_id()) {
            std::this_thread::yield();
        }
        UNIT_ASSERT_UNEQUAL(threadId, std::this_thread::get_id());
    }

    Y_UNIT_TEST(testASyncLifetimeDieBeforeSignal)
    {
        auto callbackQueue = std::make_shared<NamedCallbackQueue>("ASyncLifetimeDieBeforeSignal");
        Lifetime lifetime;
        Signal<ISignal<void>> signal;
        std::atomic<int> value{0};
        std::atomic<std::thread::id> threadId{std::this_thread::get_id()};

        signal.connect([&]() { value = 1; }, lifetime, callbackQueue); // Try modify value
        UNIT_ASSERT_EQUAL(value, 0);
        UNIT_ASSERT_EQUAL(threadId, std::this_thread::get_id());
        lifetime.die();
        signal();                                                             // Emit signal after lifetime expired
        callbackQueue->add([&]() { threadId = std::this_thread::get_id(); }); // Mark to exit
        while (threadId == std::this_thread::get_id()) {
            std::this_thread::yield();
        }
        UNIT_ASSERT_EQUAL(value, 0);                               // value still zero
        UNIT_ASSERT_UNEQUAL(threadId, std::this_thread::get_id()); // but second callback executed
    }

    Y_UNIT_TEST(testASyncLifetimeDieAfterSignal)
    {
        auto callbackQueue = std::make_shared<NamedCallbackQueue>("ASyncLifetimeDieAfterSignal");
        Lifetime lifetime;
        Signal<ISignal<void>> signal;
        std::atomic<int> value{0};
        std::atomic<std::thread::id> threadId{std::this_thread::get_id()};

        signal.connect([&]() { value = 1; }, lifetime, callbackQueue); // Try modify value
        UNIT_ASSERT_EQUAL(value, 0);
        UNIT_ASSERT_EQUAL(threadId, std::this_thread::get_id());

        std::atomic<bool> blockCallbackQueue{true};
        callbackQueue->add(
            [&]() {
                while (blockCallbackQueue) {
                    std::this_thread::yield();
                }
            });
        signal(); // Signal before die.
        lifetime.die();

        blockCallbackQueue = false;                                           // unlock callback queue
        callbackQueue->add([&]() { threadId = std::this_thread::get_id(); }); // Mark to exit
        while (threadId == std::this_thread::get_id()) {
            std::this_thread::yield();
        }
        UNIT_ASSERT_EQUAL(value, 0);                               // value still zero
        UNIT_ASSERT_UNEQUAL(threadId, std::this_thread::get_id()); // but second callback executed
    }

    Y_UNIT_TEST(testSyncAndASync)
    {
        auto callbackQueue = std::make_shared<NamedCallbackQueue>("SyncAndASync");
        Lifetime lifetime;
        Signal<ISignal<void>> signal;
        std::atomic<int> value{0};
        std::atomic<std::thread::id> threadId{std::this_thread::get_id()};

        signal.connect([&]() { threadId = std::this_thread::get_id(); }, lifetime, callbackQueue);
        UNIT_ASSERT_EQUAL(value, 0);
        UNIT_ASSERT_EQUAL(threadId, std::this_thread::get_id());

        signal.connect([&]() { value = 5; }, lifetime);

        signal();
        UNIT_ASSERT_UNEQUAL(value, 0);

        while (threadId == std::this_thread::get_id()) {
            std::this_thread::yield();
        }
        UNIT_ASSERT_UNEQUAL(threadId, std::this_thread::get_id());
    }

    Y_UNIT_TEST(testDisconnect) {
        Lifetime lifetime;
        Signal<ISignal<int>> signal;
        int result = 0;

        auto id = signal.connect([&](int value) { result = value; }, lifetime);
        UNIT_ASSERT_EQUAL(result, 0);
        signal(5);
        UNIT_ASSERT_EQUAL(result, 5);

        auto ok = signal.disconnect(id);
        signal(10);
        UNIT_ASSERT_EQUAL(ok, true);
        UNIT_ASSERT_EQUAL(result, 5); // no changes

        auto fail = signal.disconnect(id);
        UNIT_ASSERT_EQUAL(fail, false);
    }
}
