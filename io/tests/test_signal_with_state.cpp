#include <yandex_io/libs/signals/signal_with_state.h>

#include <yandex_io/libs/base/named_callback_queue.h>

#include <yandex_io/tests/testlib/test_utils.h>

#include <library/cpp/testing/unittest/registar.h>

#include <atomic>

using namespace quasar;
using namespace quasar::TestUtils;

Y_UNIT_TEST_SUITE(SignalWithState) {
    Y_UNIT_TEST(testSyncEmptyState)
    {
        Lifetime lifetime;
        SignalWithState<ISignal<int, double>> signal;
        int iresult = 0;
        double dresult = 0.;

        signal.connect([&](int i, double d) { iresult = i; dresult = d; }, lifetime);
        UNIT_ASSERT_EQUAL(iresult, 0);
        UNIT_ASSERT_EQUAL(dresult, 0.);

        signal(5, 7.);
        UNIT_ASSERT_EQUAL(iresult, 5);
        UNIT_ASSERT_EQUAL(dresult, 7.);
    }

    Y_UNIT_TEST(testSyncReadyState)
    {
        Lifetime lifetime;
        SignalWithState<ISignal<int, double>> signal;
        int iresult = 0;
        double dresult = 0.;

        signal(5, 7.);
        signal.connect([&](int i, double d) { iresult = i; dresult = d; }, lifetime);

        UNIT_ASSERT_EQUAL(iresult, 5);
        UNIT_ASSERT_EQUAL(dresult, 7.);
    }

    Y_UNIT_TEST(testSyncConnectInSignal)
    {
        Lifetime lifetime;
        SignalWithState<ISignal<int>> signal;
        int resultA;
        int resultB;

        signal(5);
        signal.connect(
            [&](int value)
            {
                resultA = value;
                signal.connect([&](int value) { resultB = value * 2; }, lifetime);
            }, lifetime);
        UNIT_ASSERT_EQUAL(resultA, 5);
        UNIT_ASSERT_EQUAL(resultB, 2 * resultA);
    }

    Y_UNIT_TEST(testASyncEmptyState)
    {
        auto callbackQueue = std::make_shared<NamedCallbackQueue>("ASyncEmptyState");
        Lifetime lifetime;
        SignalWithState<ISignal<int>> signal;
        int result = 0;
        std::atomic<bool> awaitingFlag{false};
        std::atomic<bool> awaitingSignal{false};

        signal.connect([&](int i) { result = i; awaitingSignal = false; }, lifetime, callbackQueue);
        awaitingFlag = true;
        callbackQueue->add([&] { awaitingFlag = false; });
        while (awaitingFlag) {
            std::this_thread::yield();
        }
        UNIT_ASSERT_EQUAL(result, 0);

        awaitingSignal = true;
        signal(5);
        while (awaitingSignal) {
            std::this_thread::yield();
        }
        UNIT_ASSERT_EQUAL(result, 5);
    }

    Y_UNIT_TEST(testASyncReadyStateCallback)
    {
        auto callbackQueue = std::make_shared<NamedCallbackQueue>("ASyncReadyStateCallback");
        Lifetime lifetime;
        SignalWithState<ISignal<int>> signal;
        int result = 0;
        std::atomic<bool> awatingSignal{false};

        awatingSignal = true;
        signal(5);
        signal.connect([&](int i) { result = i; awatingSignal = false; }, lifetime, callbackQueue);
        while (awatingSignal) {
            std::this_thread::yield();
        }
        UNIT_ASSERT_EQUAL(result, 5);
    }

    Y_UNIT_TEST(testWaitSignalAfter) {
        Lifetime lifetime;
        SignalWithState<ISignal<int>> signal;
        std::atomic<bool> ok{false};
        std::atomic<bool> success{false};

        auto callbackQueue = std::make_shared<NamedCallbackQueue>("ASync");
        callbackQueue->add(
            [&] {
                success = waitSignal(signal, std::chrono::seconds{5});
                ok = true;
            }, Lifetime::immortal);

        signal(1);
        waitUntil([&] { return ok.load(); });

        UNIT_ASSERT(success.load());
    }

    Y_UNIT_TEST(testWaitSignalBefore) {
        Lifetime lifetime;
        SignalWithState<ISignal<int>> signal;
        std::atomic<bool> ok{false};
        std::atomic<bool> success{false};

        signal(1);

        auto callbackQueue = std::make_shared<NamedCallbackQueue>("ASync");
        callbackQueue->add(
            [&] {
                success = waitSignal(signal, std::chrono::seconds{5});
                ok = true;
            }, Lifetime::immortal);

        waitUntil([&] { return ok.load(); });

        UNIT_ASSERT(success.load());
    }

    Y_UNIT_TEST(testWaitSignalLongWorker) {
        Lifetime lifetime;
        SignalWithState<ISignal<int>> signal;
        std::atomic<int> result{0};
        std::atomic<bool> ok{false};
        std::atomic<bool> success{false};
        std::atomic<bool> startProcess1stSignal{false};
        std::atomic<bool> waitSecondSignalEmited{false};

        auto callbackQueue = std::make_shared<NamedCallbackQueue>("ASync");
        callbackQueue->add(
            [&] {
                success = waitSignal(signal, std::chrono::seconds{5},
                                     [&](int v) {
                                         startProcess1stSignal = true;
                                         waitUntil([&] { return waitSecondSignalEmited.load(); });
                                         result = v;
                                     });
                ok = true;
            }, Lifetime::immortal);

        signal(500);
        waitUntil([&] { return startProcess1stSignal.load(); });
        signal(333);
        waitSecondSignalEmited = true;

        waitUntil([&] { return ok.load(); });

        UNIT_ASSERT(success.load());
        UNIT_ASSERT_VALUES_EQUAL(result.load(), 500);
    }

    Y_UNIT_TEST(testWaitSignalTimeout) {
        Lifetime lifetime;
        SignalWithState<ISignal<int>> signal;
        std::atomic<bool> ok{false};
        std::atomic<bool> success{false};

        auto callbackQueue = std::make_shared<NamedCallbackQueue>("ASync");
        callbackQueue->add(
            [&] {
                success = waitSignal(signal, std::chrono::milliseconds{500});
                ok = true;
            }, Lifetime::immortal);

        std::this_thread::sleep_for(std::chrono::milliseconds{600});
        signal(1);

        waitUntil([&] { return ok.load(); });

        UNIT_ASSERT(!success.load());
    }
}
