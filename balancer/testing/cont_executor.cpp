#include "cont_executor.h"
#include <balancer/kernel/process_common/main_options.h>


namespace NSrvKernel::NTesting {
    NSrvKernel::TMainOptions defaultOptions;


    TTestContExecutor::TTestContExecutor()
        : TOwnExecutor::TImpl(
            32000, defaultOptions.Poller, defaultOptions.CoroStackGuard, NCoro::NStack::TPoolAllocatorSettings{},
            nullptr, nullptr
        )
    {}
}
