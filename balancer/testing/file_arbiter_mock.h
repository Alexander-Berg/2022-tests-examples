#pragma once

#include <balancer/kernel/fs/file_arbiter.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace NSrvKernel::NTesting {

class TFileArbiterMock : public IFileArbiter {
public:
    MOCK_METHOD(TFileStat, GetStat, (const char*, bool), (const, override));
    MOCK_METHOD(TFileStat, GetStat, (FHANDLE), (const, override));

    MOCK_METHOD(THolder<IInputStream>, GetInput, (const TFile&), (const, override));
    MOCK_METHOD(THolder<IInputStream>, GetInput, (const TString&), (const, override));
};

}  // namespace NSrvKernel::NTesting
