#pragma once

#include "test_functions.h"

#include <infra/pod_agent/libs/path_util/path_holder.h>

namespace NInfra::NPodAgent::NTreeTest {

static TLogger logger({});

class ITestBehaviourTreeCanon {
public:
    ITestBehaviourTreeCanon(
        const TString& testName
        , const TString& testSuiteName
        , const TString& treeName
    );

    virtual ~ITestBehaviourTreeCanon() = default;

    void DoTest();

protected:
    virtual void Test() = 0;

    virtual TMap<TString, TString> GetReplace() const = 0;

    virtual TMap<TString, TString> GetSpecificReplace() const { // override for specific replace
        TMap<TString, TString> specificReplace;
        return specificReplace;
    }

    virtual TIpClientPtr GetSpecificIpClient(TIpClientPtr ipClient) const { // override for specific ip client in tree
        return ipClient;
    }

    virtual TPortoClientPtr GetSpecificPorto(TPortoClientPtr porto) const { // override for specific porto in tree
        return porto;
    }

    virtual TNetworkClientPtr GetSpecificNetworkClient(TNetworkClientPtr networkClient) const { // override for specific network client in tree
        return networkClient;
    }

    TString GetRootContainerProperty(EPortoContainerProperty property) const {
        return Porto_->GetProperty(TPortoContainerName(""), property).Success();
    }

    virtual void SetupTest() { // override for specific prepare for test
        // do nothing
    }

    void PrepareSpecificPlace(); // call before test with SpecificPlace

private:
    TBehavior3 GetResolvedTree() const;

    void SetupTree();

protected:
    const TString TestName_;
    const TString TestSuiteName_;

    const TString LayerPrefix_;
    const TString PersistentStoragePrefix_;

    const TString SpecificPlacePath_;
    const TString SpecificPlace_;
    const TString SpecificPlaceResourceStorage_;
    const TString VolumeStorage_;
    const TString ResourceStorage_;
    const TString RbindVolumeStorageDir_;

    TUpdateHolderPtr UpdateHolder_;

    TBoxStatusRepositoryPtr BoxStatusRepository_;
    TLayerStatusRepositoryPtr LayerStatusRepository_;
    TStaticResourceStatusRepositoryPtr StaticResourceStatusRepository_;
    TVolumeStatusRepositoryPtr VolumeStatusRepository_;
    TWorkloadStatusRepositoryPtr WorkloadStatusRepository_;

    TWorkloadStatusRepositoryInternalPtr WorkloadStatusRepositoryInternal_;

    TIpClientPtr IpClient_;
    TIpClientPtr TreeIpClient_;

    TPathHolderPtr PathHolder_;
    TPortoClientPtr SafePorto_;
    TPortoClientPtr TreePorto_;
    TTreePtr Tree_;

    TNetworkClientPtr NetworkClient_;
    TNetworkClientPtr TreeNetworkClient_;

private:
    const TString TreeName_;
    TTemplateBTStoragePtr TemplateBTStorage_;

    TPortoClientPtr Porto_;
    TLocalContainer RootContainer_;

    TAtomicSharedPtr<IThreadPool> NetworkClientMtpQueue_;
};

} // namespace NInfra::NPodAgent::NTreeTest
