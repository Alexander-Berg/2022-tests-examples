#pragma once

#include <library/cpp/deprecated/atomic/atomic.h>

#include <util/datetime/base.h>

namespace NSrvKernel::NTesting {

template <typename F>
void WaitForConditionOrDeadline(F&& f) {
    TInstant deadline = TDuration::Seconds(5).ToDeadLine();
    while (Now() < deadline && !f()) {
        Sleep(TDuration::MilliSeconds(100));
    }
};

class TSimpleBarrier {
public:
    explicit TSimpleBarrier(size_t workerCount);

    void Pass();

    void WaitForAllWorkersToPass();

    void Reset();

private:
    int32_t WorkerCount_ = 0;
    TAtomic Counter_ = 0;
};

}  // NSrvKernel::NTesting
