#pragma once

#include <infra/pod_agent/libs/behaviour/bt/core/tree.h>

#include <infra/pod_agent/libs/behaviour/template_bt_storage/template_bt_storage.h>

#include <infra/pod_agent/libs/ip_client/client.h>

#include <infra/pod_agent/libs/network_client/client.h>

#include <infra/pod_agent/libs/path_util/path_holder.h>

#include <infra/pod_agent/libs/pod_agent/status_repository/box_status_repository.h>
#include <infra/pod_agent/libs/pod_agent/status_repository/layer_status_repository.h>
#include <infra/pod_agent/libs/pod_agent/status_repository/static_resource_status_repository.h>
#include <infra/pod_agent/libs/pod_agent/status_repository/volume_status_repository.h>
#include <infra/pod_agent/libs/pod_agent/status_repository/workload_status_repository.h>

#include <infra/pod_agent/libs/pod_agent/update_holder/update_holder.h>

#include <infra/pod_agent/libs/pod_agent/status_repository_internal/workload_status_repository_internal.h>

#include <infra/pod_agent/libs/porto_client/simple_client.h>

namespace NInfra::NPodAgent::NTreeTest {

const TString TEST_PREFIX = "BehaviourTreeTest_";

TPortoClientPtr GetTestPortoClient(const TString& testName);

TPortoClientPtr GetTestPortoClientWithRetries(const TString& testName);

TTemplateBTStoragePtr GetTemplateBTStorage();

TTreePtr GetBuildedTree(
    const TBehavior3& protoTree
    , TTemplateBTStoragePtr templateBTStorage
    , TUpdateHolderTargetPtr updateHolderTarget
    , TBoxStatusRepositoryPtr boxStatusRepository
    , TLayerStatusRepositoryPtr layerStatusRepository
    , TStaticResourceStatusRepositoryPtr staticResourceStatusRepository
    , TVolumeStatusRepositoryPtr volumeStatusRepository
    , TWorkloadStatusRepositoryPtr workloadStatusRepository
    , TWorkloadStatusRepositoryInternalPtr workloadStatusRepositoryInternal
    , TIpClientPtr ipClient
    , TNetworkClientPtr networkClient
    , TPortoClientPtr portoClient
    , TPathHolderPtr pathHolder
);

void TickTree(TTreePtr tree, size_t tickCount, std::function<bool()> breakHook = [](){return false;});

void AssertIsEqual(const google::protobuf::Message& messageFirst, const google::protobuf::Message& messageSecond, bool statement);

} // namespace NInfra::NPodAgent::NTreeTest
