#include "test_canon.h"

#include <infra/pod_agent/libs/behaviour/bt/nodes/base/private_util.h>
#include <infra/pod_agent/libs/behaviour/loaders/behavior3_editor_json_reader.h>
#include <infra/pod_agent/libs/behaviour/loaders/behavior3_template_resolver.h>
#include <infra/pod_agent/libs/ip_client/mock_client.h>
#include <infra/pod_agent/libs/network_client/simple_client.h>
#include <infra/pod_agent/libs/porto_client/nested_client.h>
#include <infra/pod_agent/libs/porto_client/porto_test_lib/client_with_retries.h>

#include <library/cpp/testing/unittest/tests_data.h>

#include <util/folder/dirut.h>
#include <util/system/fs.h>
#include <util/system/thread.h>

namespace NInfra::NPodAgent::NTreeTest {

ITestBehaviourTreeCanon::ITestBehaviourTreeCanon(
    const TString& testName
    , const TString& testSuiteName
    , const TString& treeName
)
    : TestName_(testName)
    , TestSuiteName_(testSuiteName)
    , LayerPrefix_(TEST_PREFIX + ToString(TThread::CurrentThreadId()) + "_" + testName + "_" + testSuiteName + "_")
    , PersistentStoragePrefix_(LayerPrefix_)
    , SpecificPlacePath_(RealPath(GetWorkPath()) + "/" + testName + "/" + testSuiteName + "/place")
    , SpecificPlace_("//" + SpecificPlacePath_)
    , SpecificPlaceResourceStorage_(RealPath(GetWorkPath()) + "/" + testName + "/" + testSuiteName + "/specific_place/resources/")
    , VolumeStorage_(RealPath(GetWorkPath()) + "/" + testName + "/" + testSuiteName + "/volumes/")
    , ResourceStorage_(RealPath(GetWorkPath()) + "/" + testName + "/" + testSuiteName + "/resources/")
    , RbindVolumeStorageDir_(RealPath(GetWorkPath()) + "/" + testName + "/" + testSuiteName + "/rbind_volumes")
    , UpdateHolder_(new TUpdateHolder())
    , BoxStatusRepository_(new TBoxStatusRepository())
    , LayerStatusRepository_(new TLayerStatusRepository())
    , StaticResourceStatusRepository_(new TStaticResourceStatusRepository())
    , VolumeStatusRepository_(new TVolumeStatusRepository())
    , WorkloadStatusRepository_(new TWorkloadStatusRepository())
    , WorkloadStatusRepositoryInternal_(new TWorkloadStatusRepositoryInternal())
    , IpClient_(new TMockIpClient())
    , PathHolder_(
        new TPathHolder(
            "/"
            , {
                {"", ""}
                , {"specific_virtual_disk", SpecificPlace_}
            }
            , {
                {"", ResourceStorage_}
                , {SpecificPlace_, SpecificPlaceResourceStorage_}
            }
            , VolumeStorage_
            , PersistentStoragePrefix_
            , LayerPrefix_
            , ""
            , ""
            , RbindVolumeStorageDir_
        )
    )
    , TreeName_(treeName)
    , TemplateBTStorage_(GetTemplateBTStorage())
    , RootContainer_(GetTestPortoClientWithRetries(testName), TPortoContainerName("RootContainer"))
    , NetworkClientMtpQueue_(new TThreadPool())
{
    GetTestPortoClientWithRetries(testName)->Start(RootContainer_).Success(); // start 'meta' container;
    SafePorto_ = new TPortoClientWithRetries(new TNestedPortoClient(GetTestPortoClient(testName), RootContainer_));
    Porto_ = new TNestedPortoClient(GetTestPortoClient(testName), RootContainer_);

    NetworkClientMtpQueue_->Start(1);
    NetworkClient_ = new TSimpleNetworkClient(NetworkClientMtpQueue_); // work only with real queue, deadlock otherwise
}

void ITestBehaviourTreeCanon::DoTest() {
    SetupTest(); // must call in derived
    SetupTree(); // must call in derived
    Test();
}

void ITestBehaviourTreeCanon::PrepareSpecificPlace() {
    if (NFs::Exists(SpecificPlaceResourceStorage_)) {
        NFs::RemoveRecursive(SpecificPlaceResourceStorage_);
    }
    NFs::MakeDirectoryRecursive(SpecificPlaceResourceStorage_);

    if (NFs::Exists(SpecificPlacePath_)) {
        NFs::RemoveRecursive(SpecificPlacePath_);
    }
    NFs::MakeDirectoryRecursive(SpecificPlacePath_);
    Porto_->CreateVolume(SpecificPlacePath_, "", "", {}, 0, "", EPortoVolumeBackend::Auto, {""}, {}, false).Success();

    const TVector<TString> dirs = {
        SpecificPlacePath_ + "/porto_volumes/"
        , SpecificPlacePath_ + "/porto_storage/"
        , SpecificPlacePath_ + "/porto_layers/"
    };

    for (const TString& dir : dirs) {
        if (NFs::Exists(dir)) {
            NFs::RemoveRecursive(dir);
        }
        NFs::MakeDirectoryRecursive(dir);
    }
}

TBehavior3 ITestBehaviourTreeCanon::GetResolvedTree() const {
    TBehavior3 resolvedTree = TemplateBTStorage_->Get(TreeName_);
    TMap<TString, TString> replace = GetReplace();

    TMap<TString, TString> specificReplace = GetSpecificReplace();
    for (auto& item : specificReplace) {
        replace[item.first] = item.second;
    }

    TBehavior3TemplateResolver().Resolve(resolvedTree, replace);

    return resolvedTree;
}

void ITestBehaviourTreeCanon::SetupTree() {
    TreePorto_ = GetSpecificPorto(Porto_);
    TreeIpClient_ = GetSpecificIpClient(IpClient_);
    TreeNetworkClient_ = GetSpecificNetworkClient(NetworkClient_);

    Tree_ = GetBuildedTree(
        GetResolvedTree()
        , TemplateBTStorage_
        , UpdateHolder_->GetUpdateHolderTarget()
        , BoxStatusRepository_
        , LayerStatusRepository_
        , StaticResourceStatusRepository_
        , VolumeStatusRepository_
        , WorkloadStatusRepository_
        , WorkloadStatusRepositoryInternal_
        , TreeIpClient_
        , TreeNetworkClient_
        , TreePorto_
        , PathHolder_
    );
}

} // namespace NInfra::NPodAgent::NTreeTest
