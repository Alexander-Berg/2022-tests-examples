#pragma once

#include <infra/pod_agent/libs/behaviour/trees/base/test/test_canon.h>

#include <infra/libs/http_service/service.h>

#include <util/network/sock.h>

namespace NInfra::NPodAgent::NTreeTest {

class ITestWorkloadCanon: public ITestBehaviourTreeCanon {
public:
    ITestWorkloadCanon(
        const TString& testName
        , const TString& testSuiteName
        , const TString& treeName
    )
        : ITestBehaviourTreeCanon(testName, testSuiteName, treeName)
        , BoxId_("MyBoxId")
        , WorkloadId_("MyWorkloadId")
    {
    }

    virtual ~ITestWorkloadCanon() {
        if (BoxPrepared_) {
            RemoveStorage();
        }
    }

protected:
    virtual void SetupTest() override {
        PrepareWorkload(GetWorkloadInitSize());
    }

    virtual ui32 GetWorkloadInitSize() const {
        return 0;
    }

    void PrepareBox();

    TString GetRootContainerProperty(EPortoContainerProperty property) const {
        return SafePorto_->GetProperty(TPortoContainerName(""), property).Success();
    }

    TPortoContainerName GetFullWorkloadContainerName(const TString& name) const {
        return PathHolder_->GetWorkloadContainerWithName(BoxId_, WorkloadId_, name);
    }

    void PrepareWorkload(ui32 initSize);

    TInet6StreamSocket CreateBindedSocket(ui16 port);
    THttpService CreateAndStartHttpService(const THttpServiceConfig& config, TRequestRouterPtr router);

private:
    void RemoveStorage();

protected:
    const TString BoxId_;
    const TString WorkloadId_;

private:
    bool BoxPrepared_{false};
};

} // namespace NInfra::NPodAgent::NTreeTest
