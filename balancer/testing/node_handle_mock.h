#pragma once

#include <balancer/kernel/module/module_face.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace NSrvKernel::NTesting {

template <class I>
class TNodeHandleMock : public INodeHandle<I> {
public:
    MOCK_CONST_METHOD0_T(DoName, const TString&());
    MOCK_CONST_METHOD1_T(DoConstruct, I*(const TModuleParams&));
};

}  // namespace NSrvKernel::NTesting
