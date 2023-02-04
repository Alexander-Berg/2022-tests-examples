#include <balancer/kernel/testing/thread_time_arbiter_stub.h>
#include <balancer/kernel/thread/time_manager.h>

#include <library/cpp/testing/unittest/registar.h>

#include <util/thread/factory.h>

#include <util/system/event.h>
#include <util/system/thread.h>

#include <thread>

using namespace NSrvKernel;

Y_UNIT_TEST_SUITE(TThreadTimeArbiterStubTest) {
    constexpr TInstant INIT_TIME = {};
    constexpr auto DURATION_SAMPLE = TDuration::Seconds(5);

    Y_UNIT_TEST(SmokingTest) {
        TThreadTimeManager::Instance().EnableTesting();

        auto timeArbiterStubPtr = MakeHolder<NSrvKernel::NTesting::TThreadTimeArbiterStub>(INIT_TIME);
        auto* timeArbiterStub = timeArbiterStubPtr.Get();

        TThreadTimeManager::TMockGuard guard{std::move(timeArbiterStubPtr)};

        TInstant now = TThreadTimeManager::Instance().Now();

        // Initialized TThreadTimeArbiterStub's Now() equals to INIT_TIME
        UNIT_ASSERT_EQUAL(now, INIT_TIME);

        // Advance test
        timeArbiterStub->Advance(DURATION_SAMPLE);
        UNIT_ASSERT_EQUAL(TThreadTimeManager::Instance().Now(), now + DURATION_SAMPLE);

        // Jump test
        auto tp = now + DURATION_SAMPLE * 2;
        timeArbiterStub->Jump(tp);
        UNIT_ASSERT_EQUAL(TThreadTimeManager::Instance().Now(), tp);
    }

    Y_UNIT_TEST(OtherThreadSleepTest) {
        TThreadTimeManager::Instance().EnableTesting();

        // Creating timeArbiter mock with guard
        auto timeArbiterStubPtr = MakeHolder<NSrvKernel::NTesting::TThreadTimeArbiterStub>(INIT_TIME);
        auto* timeArbiterStub = timeArbiterStubPtr.Get();

        TThreadTimeManager::TMockGuard guard{std::move(timeArbiterStubPtr)};

        TManualEvent threadStart, threadEnd;

        TInstant now = TThreadTimeManager::Instance().Now();

        // Situation when other thread sleeps until some timestamp.
        // Master thread calls Advance().
        // --------------------------------------------------------------------------------
        auto* threadPool = SystemThreadFactory();
        THolder<IThreadFactory::IThread> task = threadPool->Run([&] {
            threadStart.Signal();
            TThreadTimeManager::Instance().SleepUntil(now + DURATION_SAMPLE);
            threadEnd.Signal();
        });

        threadStart.Wait();
        timeArbiterStub->Advance(DURATION_SAMPLE);
        threadEnd.Wait();
        // --------------------------------------------------------------------------------

        threadStart.Reset();
        threadEnd.Reset();

        // We mustn't sleep for zero time.
        // --------------------------------------------------------------------------------
        task.Reset(threadPool->Run([&] {
            threadStart.Signal();
            TThreadTimeManager::Instance().Sleep(TDuration{});
            threadEnd.Signal();
        }));
        // --------------------------------------------------------------------------------

        threadStart.Wait();
        threadEnd.Wait();

        task->Join();
    }
}
