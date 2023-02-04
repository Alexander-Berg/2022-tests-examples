#include <yandex_io/libs/signals/signal_external.h>

#include <yandex_io/libs/base/named_callback_queue.h>

#include <library/cpp/testing/unittest/registar.h>

#include <atomic>

using namespace quasar;

Y_UNIT_TEST_SUITE(SignalExternal) {
    Y_UNIT_TEST(testSyncConnectThenEmit)
    {
        Lifetime lifetime;
        SignalExternal<ISignal<int, double>> signal(
            [&](bool onConnect)
            {
                if (onConnect) {
                    return std::make_tuple(1, 1.);
                } else {
                    return std::make_tuple(5, 7.);
                }
            }, Lifetime::immortal);
        int iresult = 0;
        double dresult = 0.;

        signal.connect([&](int i, double d) { iresult = i; dresult = d; }, lifetime);
        UNIT_ASSERT_EQUAL(iresult, 1);
        UNIT_ASSERT_EQUAL(dresult, 1.);

        signal.emit();
        UNIT_ASSERT_EQUAL(iresult, 5);
        UNIT_ASSERT_EQUAL(dresult, 7.);
    }

    Y_UNIT_TEST(testSyncSameValue)
    {
        int externalValue = 0;
        Lifetime lifetime;
        SignalExternal<ISignal<int>> signal(
            [&](bool /*onConnect*/)
            {
                return std::make_tuple(++externalValue);
            }, Lifetime::immortal);
        int resultA = 0;
        int resultB = 0;
        int resultC = 0;

        signal.connect([&](int i) { resultA = i; }, lifetime);
        signal.connect([&](int i) { resultB = i; }, lifetime);
        signal.connect([&](int i) { resultC = i; }, lifetime);
        UNIT_ASSERT_EQUAL(resultA, 1); // externalValue increases every time when signal lambda is called
        UNIT_ASSERT_EQUAL(resultB, 2);
        UNIT_ASSERT_EQUAL(resultC, 3);

        signal.emit();
        UNIT_ASSERT_EQUAL(resultA, 4);
        UNIT_ASSERT_EQUAL(resultB, 4);
        UNIT_ASSERT_EQUAL(resultC, 4);
    }

    Y_UNIT_TEST(testSyncEmitFewtimes)
    {
        std::atomic<int> signaled{0};
        Lifetime lifetime;
        SignalExternal<ISignal<int, double>> signal(
            [&](bool onConnect)
            {
                if (!onConnect) {
                    ++signaled;
                }
                if (signaled == 1) {
                    return std::make_tuple(5, 7.);
                } else if (signaled == 2) {
                    return std::make_tuple(13, 17.);
                } else if (signaled) {
                    UNIT_ASSERT(!"Too much");
                }
                return std::make_tuple(0, 0.);
            }, Lifetime::immortal);
        int iresult = 0;
        double dresult = 0.;

        signal.connect([&](int i, double d) { iresult = i; dresult = d; }, lifetime);
        UNIT_ASSERT_EQUAL(iresult, 0);
        UNIT_ASSERT_EQUAL(dresult, 0.);

        signal.emit();
        UNIT_ASSERT_EQUAL(iresult, 5);
        UNIT_ASSERT_EQUAL(dresult, 7.);

        signal.emit();
        UNIT_ASSERT_EQUAL(iresult, 13);
        UNIT_ASSERT_EQUAL(dresult, 17.);
    }

    Y_UNIT_TEST(testASyncReadyStateCallback)
    {
        auto callbackQueue = std::make_shared<NamedCallbackQueue>("ASyncReadyStateCallback");
        Lifetime lifetime;
        std::atomic<int> externalState = 0;
        SignalExternal<ISignal<int>> signal(
            [&](bool /*onConnect*/)
            {
                return std::make_tuple((int)externalState);
            }, Lifetime::immortal);
        int result = 0;
        std::atomic<bool> awaitingSignal{false};

        UNIT_ASSERT_EQUAL(result, 0);

        awaitingSignal = true;
        externalState = 1;
        signal.emit();
        signal.connect([&](int i) { result = i; awaitingSignal = false; }, lifetime, callbackQueue);
        while (awaitingSignal) {
            std::this_thread::yield();
        }
        UNIT_ASSERT_EQUAL(result, externalState);

        externalState = 2;
        awaitingSignal = true;
        signal.emit();
        while (awaitingSignal) {
            std::this_thread::yield();
        }
        UNIT_ASSERT_EQUAL(result, externalState);
    }
}
