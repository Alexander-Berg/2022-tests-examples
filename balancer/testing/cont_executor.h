#pragma once

#include <balancer/kernel/coro/own_executor.h>

namespace NSrvKernel::NTesting {
    class TTestContExecutor
        : public TOwnExecutor::TImpl
    {
    public:
        TTestContExecutor();
    };
}
