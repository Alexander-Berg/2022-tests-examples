#include "simple_barrier.h"

namespace NSrvKernel::NTesting {

TSimpleBarrier::TSimpleBarrier(size_t workerCount)
    : WorkerCount_(workerCount)
{}

void TSimpleBarrier::Pass() {
    AtomicIncrement(Counter_);
}

void TSimpleBarrier::WaitForAllWorkersToPass() {
    WaitForConditionOrDeadline([&]() { return AtomicGet(Counter_) == WorkerCount_; });
    Reset();
}

void TSimpleBarrier::Reset() {
    AtomicSet(Counter_, 0);
}

}  // namespace NSrvKernel::NTesting
