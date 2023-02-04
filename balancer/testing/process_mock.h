#pragma once

#include <balancer/kernel/ctl/ctl.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace NSrvKernel::NTesting {

    class TProcessMock : public IWorkerCtl {
    public:
        TProcessMock() : IWorkerCtl(0) {}
        MOCK_METHOD(void, GetAcceptedStatistics, (IExceptionlessOutputStream&, const TAddrDescrs&, const TString&), (const));
        MOCK_METHOD(NSrvKernel::TSharedFiles*, SharedFiles, (), (override));
        MOCK_METHOD(TThreadedQueue*, ThreadedQueue, (const TString&), (override));
        MOCK_METHOD(NSrvKernel::IPingerManager&, SharedPingerManager, (), (override));
        MOCK_METHOD(size_t, WorkerId, (), (const));
        MOCK_METHOD(bool, IsShuttingDown, (), (const, override));
        MOCK_METHOD(NDns::IResolver&, Resolver, (), (override));
        MOCK_METHOD(TLog*, GetLog, (const TString&), (override));
        MOCK_METHOD(TLog*, GetDynamicBalancingLog, (), (override));
        MOCK_METHOD(NProcessCore::TChildProcessType, WorkerType, (), (const, override));
        MOCK_METHOD(TContExecutor&, Executor, (), (const, override));
        MOCK_METHOD(void, DumpSharedFiles, (NJson::TJsonWriter&), (override));
        MOCK_METHOD(void, AddGracefulShutdownHandler, (IGracefulShutdownHandler*, bool), (override));
        MOCK_METHOD(const NSrvKernel::TBackendCheckOverridenParameters&, BackendCheckOverrides, (), (override));
        MOCK_METHOD(bool, BlockNewRequests, (), (const, override));
        MOCK_METHOD(TEventHandler*, EventHandler, (), (const, noexcept, override));
    };

}  // namespace NSrvKernel::NTesting
