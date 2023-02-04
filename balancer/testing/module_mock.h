#pragma once

#include <balancer/kernel/module/module_face.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace NSrvKernel::NTesting {

class TModulePartMock : public IModule {
public:
    MOCK_METHOD(IModuleHandle*, DoHandle, (), (const, override));
    MOCK_METHOD(TError, DoRun, (const TConnDescr&), (const, override));
};

class TModuleMock : public TModulePartMock {
public:
    MOCK_METHOD(void, DoInit, (IWorkerCtl*), (override));
    MOCK_METHOD(void, DoCheckConstraints, (), (const, override));
    MOCK_METHOD(void, DoFinalize, (), (override));
    MOCK_METHOD(void, DoDispose, (IWorkerCtl*), (override));
    MOCK_METHOD(bool, DoExtraAccessLog, (), (const, override));
    MOCK_METHOD(const TEventHandler*, DoEventHandler, (), (const, override));
};

}  // namespace NSrvKernel::NTesting
