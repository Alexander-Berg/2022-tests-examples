#include "thread_time_arbiter_stub.h"

#include <util/generic/vector.h>

namespace NSrvKernel::NTesting {

void TThreadTimeArbiterStub::JumpImpl(TInstant tp) noexcept {
    FakeTime_.store(tp);

    TVector<TAtomicSharedPtr<TSleepContext>> toNotify;
    with_lock (MainMtx_) {
        auto it = SleepingThreads_.begin();
        while (it != SleepingThreads_.end() && it->first <= tp) {
            it->second->WakedUp = true;
            toNotify.emplace_back(std::move(it->second));
            SleepingThreads_.erase(it++);
        }
    }
    for (auto ctx : toNotify) {
        ctx->CondVar.Signal();
    }
}

void TThreadTimeArbiterStub::SleepUntil(TInstant tp) noexcept {
    with_lock (MainMtx_) {
        if (tp <= this->Now()) {
            return;
        }

        auto ctx = MakeAtomicShared<TSleepContext>();

        SleepingThreads_.emplace(tp, ctx);
        while (!ctx->WakedUp) {
            ctx->CondVar.WaitI(MainMtx_);
        }
    }
}

}  // namespace NSrvKernel::NTesting
