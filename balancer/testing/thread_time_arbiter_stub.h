#pragma once

#include <balancer/kernel/thread/time_arbiter.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <util/system/condvar.h>
#include <util/system/mutex.h>

#include <util/generic/map.h>

namespace NSrvKernel::NTesting {

class TThreadTimeArbiterStub final : public IThreadTimeArbiter {
public:
    TThreadTimeArbiterStub(TInstant now = {})
        : FakeTime_(now)
    {}

    // Single threaded
    void Advance(TDuration dur) noexcept {
        JumpImpl(this->Now() + dur);
    }

    // Single threaded
    void Jump(TInstant tp) noexcept {
        if (tp <= this->Now()) {
            return;
        }
        JumpImpl(tp);
    }

private:
    void JumpImpl(TInstant tp) noexcept;

    // Multithreaded
    void SleepUntil(TInstant tp) noexcept override;

    // Multithreaded
    TInstant Now() const noexcept override {
        return FakeTime_.load();
    }

private:
    std::atomic<TInstant> FakeTime_;
    TMutex MainMtx_;

    struct TSleepContext {
        TCondVar CondVar;
        bool WakedUp = false;
    };
    TMultiMap<TInstant, TAtomicSharedPtr<TSleepContext>> SleepingThreads_;
};

}  // namespace NSrvKernel::NTesting
