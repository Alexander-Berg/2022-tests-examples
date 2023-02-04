#include "resource_cache_controller.h"

#include <google/protobuf/timestamp.pb.h>
#include <google/protobuf/util/message_differencer.h>
#include <library/cpp/json/writer/json.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/yson/node/node.h>

#include <util/random/mersenne.h>
#include <util/random/shuffle.h>

namespace NInfra::NResourceCacheController::NResourceCacheControllerTest {

ui64 TimestampToMicros(const google::protobuf::Timestamp& timestamp) {
    return timestamp.seconds() * (ui64)1000000 + timestamp.nanos() / 1000;
}

TInstant ToInstant(const google::protobuf::Timestamp& timestamp) {
    return TInstant::MicroSeconds(TimestampToMicros(timestamp));
}

bool ChechIsConditionsLastTransitionTimeInGap(
    const NYP::NClient::NApi::NProto::TAggregatedCondition& condition1
    , const NYP::NClient::NApi::NProto::TAggregatedCondition& condition2
    , const TDuration& maxTimeGap
) {
    const TInstant time1 = ToInstant(condition1.condition().last_transition_time());
    const TInstant time2 = ToInstant(condition2.condition().last_transition_time());

    return time2 - time1 <= maxTimeGap;
}

bool ChechIsConditionsLastTransitionTimeExceedsGap(
    const NYP::NClient::NApi::NProto::TAggregatedCondition& condition1
    , const NYP::NClient::NApi::NProto::TAggregatedCondition& condition2
    , const TDuration& minTimeGap
) {
    const TInstant time1 = ToInstant(condition1.condition().last_transition_time());
    const TInstant time2 = ToInstant(condition2.condition().last_transition_time());

    return time2 - time1 >= minTimeGap;
}

bool ChechIsConditionsLastTransitionTimeEqual(
    const NYP::NClient::NApi::NProto::TAggregatedCondition& condition1
    , const NYP::NClient::NApi::NProto::TAggregatedCondition& condition2
) {
    return google::protobuf::util::MessageDifferencer::Equals(condition1.condition().last_transition_time(), condition2.condition().last_transition_time());
}

void CorrectTimestamp(google::protobuf::Timestamp* timestamp) {
    timestamp->set_seconds(1337);
    timestamp->set_nanos(1337);
}

NYP::NClient::NApi::NProto::TResourceCacheStatus CorrectResourceCacheStatus(
    const NYP::NClient::NApi::NProto::TResourceCacheStatus& resourceCacheStatus
) {
    NYP::NClient::NApi::NProto::TResourceCacheStatus updatedResourceCacheStatus = resourceCacheStatus;

    CorrectTimestamp(updatedResourceCacheStatus.mutable_all_ready()->mutable_condition()->mutable_last_transition_time());
    CorrectTimestamp(updatedResourceCacheStatus.mutable_all_in_progress()->mutable_condition()->mutable_last_transition_time());
    CorrectTimestamp(updatedResourceCacheStatus.mutable_all_failed()->mutable_condition()->mutable_last_transition_time());

    CorrectTimestamp(updatedResourceCacheStatus.mutable_latest_ready()->mutable_condition()->mutable_last_transition_time());
    CorrectTimestamp(updatedResourceCacheStatus.mutable_latest_in_progress()->mutable_condition()->mutable_last_transition_time());
    CorrectTimestamp(updatedResourceCacheStatus.mutable_latest_failed()->mutable_condition()->mutable_last_transition_time());

    for (auto& cachedResourceStatus : *updatedResourceCacheStatus.mutable_cached_resource_status()) {
        for (auto& cachedResourceRevisionStatus : *cachedResourceStatus.mutable_revisions()) {
                CorrectTimestamp(cachedResourceRevisionStatus.mutable_ready()->mutable_condition()->mutable_last_transition_time());
                CorrectTimestamp(cachedResourceRevisionStatus.mutable_in_progress()->mutable_condition()->mutable_last_transition_time());
                CorrectTimestamp(cachedResourceRevisionStatus.mutable_failed()->mutable_condition()->mutable_last_transition_time());
        }
    }

    return updatedResourceCacheStatus;
}

NYP::NClient::TResourceCache GetSimpleResourceCache(
    const ui32 updateWindow = 0
) {
    NYP::NClient::TResourceCache resourceCache;
    resourceCache.MutableMeta()->set_pod_set_id("pod_set");
    resourceCache.MutableSpec()->set_revision(1);
    resourceCache.MutableSpec()->set_update_window(updateWindow);

    return resourceCache;
}

NYP::NClient::NApi::NProto::TResourceCacheSpec::TCachedResource GetSimpleCachedLayer(
    const TString& id
    , ui32 maxLatestRevisions
    , const TString& url
    , const TString& checksum
) {
    NYP::NClient::NApi::NProto::TResourceCacheSpec::TCachedResource cachedLayer;
    cachedLayer.set_id(id);
    cachedLayer.mutable_basic_strategy()->set_max_latest_revisions(maxLatestRevisions);
    cachedLayer.mutable_layer()->set_url(url);
    cachedLayer.mutable_layer()->set_checksum(checksum);

    return cachedLayer;
}

NYP::NClient::NApi::NProto::TResourceCacheSpec::TCachedResource GetSimpleCachedStaticResource(
    const TString& id
    , ui32 maxLatestRevisions
    , const TString& url
    , const TString& checksum
    , ui32 checkPeriodMs
) {
    NYP::NClient::NApi::NProto::TResourceCacheSpec::TCachedResource cachedStaticResource;
    cachedStaticResource.set_id(id);
    cachedStaticResource.mutable_basic_strategy()->set_max_latest_revisions(maxLatestRevisions);
    cachedStaticResource.mutable_static_resource()->set_url(url);
    cachedStaticResource.mutable_static_resource()->mutable_verification()->set_checksum(checksum);
    cachedStaticResource.mutable_static_resource()->mutable_verification()->set_check_period_ms(checkPeriodMs);

    return cachedStaticResource;
}

NYP::NClient::TPod GetSimplePod(
    const TString& id
    , const NYP::NClient::NApi::NProto::TPodSpec::TPodAgentResourceCache& podAgentResourceCache = NYP::NClient::NApi::NProto::TPodSpec::TPodAgentResourceCache()
    , const bool generateReadyStatus = true
) {
    NYP::NClient::TPod pod;
    pod.MutableMeta()->set_id(id);

    if (generateReadyStatus) {
        for (const auto& layerInSpec : podAgentResourceCache.spec().layers()) {
            auto* layerInStatus = pod.MutableStatus()->mutable_agent()->mutable_pod_agent_payload()->mutable_status()->mutable_resource_cache()->mutable_layers()->Add();
            layerInStatus->set_id(layerInSpec.layer().id());
            layerInStatus->set_revision(layerInSpec.revision());
            layerInStatus->set_state(NInfra::NPodAgent::API::ELayerState_READY);
            layerInStatus->mutable_ready()->set_status(NInfra::NPodAgent::API::EConditionStatus_TRUE);
        }
        for (const auto& staticResourceInSpec : podAgentResourceCache.spec().static_resources()) {
            auto* staticResourceInStatus = pod.MutableStatus()->mutable_agent()->mutable_pod_agent_payload()->mutable_status()->mutable_resource_cache()->mutable_static_resources()->Add();
            staticResourceInStatus->set_id(staticResourceInSpec.resource().id());
            staticResourceInStatus->set_revision(staticResourceInSpec.revision());
            staticResourceInStatus->set_state(NInfra::NPodAgent::API::EStaticResourceState_READY);
            staticResourceInStatus->mutable_ready()->set_status(NInfra::NPodAgent::API::EConditionStatus_TRUE);
        }
    } else {
        auto* layer = pod.MutableStatus()->mutable_agent()->mutable_pod_agent_payload()->mutable_status()->mutable_resource_cache()->mutable_layers()->Add();
        layer->set_id("layer_id");
        layer->set_revision(1);
        layer->set_state(NInfra::NPodAgent::API::ELayerState_INVALID);
        layer->mutable_ready()->set_status(NInfra::NPodAgent::API::EConditionStatus_FALSE);
    }

    pod.MutableSpec()->mutable_resource_cache()->CopyFrom(podAgentResourceCache);

    return pod;
}

NInfra::NPodAgent::API::TLayerStatus GetSimpleLayerStatus(
    const TString& id
    , ui32 revision
    , bool ready
    , bool inProgress
    , bool failed
    , ui64 seconds = 0
    , ui32 nanos = 0
) {
    NInfra::NPodAgent::API::TLayerStatus status;
    status.set_id(id);
    status.set_revision(revision);

    status.mutable_ready()->set_status(ready ? NInfra::NPodAgent::API::EConditionStatus_TRUE : NInfra::NPodAgent::API::EConditionStatus_FALSE);
    status.mutable_ready()->set_reason(TStringBuilder() << "layer with id " << id << " reason");
    status.mutable_ready()->set_message(TStringBuilder() << "layer with id " << id << " message");
    status.mutable_ready()->mutable_last_transition_time()->set_seconds(seconds);
    status.mutable_ready()->mutable_last_transition_time()->set_nanos(nanos);

    status.mutable_in_progress()->set_status(inProgress ? NInfra::NPodAgent::API::EConditionStatus_TRUE : NInfra::NPodAgent::API::EConditionStatus_FALSE);
    status.mutable_in_progress()->set_reason(TStringBuilder() << "layer with id " << id << " reason");
    status.mutable_in_progress()->set_message(TStringBuilder() << "layer with id " << id << " message");
    status.mutable_in_progress()->mutable_last_transition_time()->set_seconds(seconds);
    status.mutable_in_progress()->mutable_last_transition_time()->set_nanos(nanos);

    status.mutable_failed()->set_status(failed ? NInfra::NPodAgent::API::EConditionStatus_TRUE : NInfra::NPodAgent::API::EConditionStatus_FALSE);
    status.mutable_failed()->set_reason(TStringBuilder() << "layer with id " << id << " reason");
    status.mutable_failed()->set_message(TStringBuilder() << "layer with id " << id << " message");
    status.mutable_failed()->mutable_last_transition_time()->set_seconds(seconds);
    status.mutable_failed()->mutable_last_transition_time()->set_nanos(nanos);

    return status;
}

NInfra::NPodAgent::API::TStaticResourceStatus GetSimpleStaticResourceStatus(
    const TString& id
    , ui32 revision
    , bool ready
    , bool inProgress
    , bool failed
    , ui64 seconds = 0
    , ui32 nanos = 0
) {
    NInfra::NPodAgent::API::TStaticResourceStatus status;
    status.set_id(id);
    status.set_revision(revision);

    status.mutable_ready()->set_status(ready ? NInfra::NPodAgent::API::EConditionStatus_TRUE : NInfra::NPodAgent::API::EConditionStatus_FALSE);
    status.mutable_ready()->set_reason(TStringBuilder() << "static_resource with id " << id << " reason");
    status.mutable_ready()->set_message(TStringBuilder() << "static_resouce with id " << id << " message");
    status.mutable_ready()->mutable_last_transition_time()->set_seconds(seconds);
    status.mutable_ready()->mutable_last_transition_time()->set_nanos(nanos);

    status.mutable_in_progress()->set_status(inProgress ? NInfra::NPodAgent::API::EConditionStatus_TRUE : NInfra::NPodAgent::API::EConditionStatus_FALSE);
    status.mutable_in_progress()->set_reason(TStringBuilder() << "static_resouce with id " << id << " reason");
    status.mutable_in_progress()->set_message(TStringBuilder() << "static_resource with id " << id << " message");
    status.mutable_in_progress()->mutable_last_transition_time()->set_seconds(seconds);
    status.mutable_in_progress()->mutable_last_transition_time()->set_nanos(nanos);

    status.mutable_failed()->set_status(failed ? NInfra::NPodAgent::API::EConditionStatus_TRUE : NInfra::NPodAgent::API::EConditionStatus_FALSE);
    status.mutable_failed()->set_reason(TStringBuilder() << "static_resouce with id " << id << " reason");
    status.mutable_failed()->set_message(TStringBuilder() << "static_resource with id " << id << " message");
    status.mutable_failed()->mutable_last_transition_time()->set_seconds(seconds);
    status.mutable_failed()->mutable_last_transition_time()->set_nanos(nanos);

    return status;
}

void AddLayerStatusToPodStatus(
    NYP::NClient::TPod& pod
    , const NInfra::NPodAgent::API::TLayerStatus layerStatus
) {
    pod.MutableStatus()
        ->mutable_agent()
        ->mutable_pod_agent_payload()
        ->mutable_status()
        ->mutable_resource_cache()
        ->mutable_layers()
        ->Add()
        ->CopyFrom(layerStatus);
}

void AddStaticResourceStatusToPodStatus(
    NYP::NClient::TPod& pod
    , const NInfra::NPodAgent::API::TStaticResourceStatus staticResourceStatus
) {
    pod.MutableStatus()
        ->mutable_agent()
        ->mutable_pod_agent_payload()
        ->mutable_status()
        ->mutable_resource_cache()
        ->mutable_static_resources()
        ->Add()
        ->CopyFrom(staticResourceStatus);
}

void GenerateSpecificPods(
    const ui32 sizeOfIdsCurrentReady
    , const ui32 sizeOfIdsOldReady
    , const ui32 sizeOfIdsCurrentNotReady
    , const ui32 sizeOfIdsOldNotReady
    , TVector<NYP::NClient::TPod>& pods
    , TVector<TString>& idsCurrentReady
    , TVector<TString>& idsOldReady
    , TVector<TString>& idsCurrentNotReady
    , TVector<TString>& idsOldNotReady
    , const NYP::NClient::NApi::NProto::TPodSpec::TPodAgentResourceCache& podAgentResourceCache
) {
    for (ui32 i = 0; i < sizeOfIdsCurrentReady; ++i) {
        pods.push_back(
            GetSimplePod(
                "pod_current_spec_ready_" + ToString(i)
                , podAgentResourceCache
                , true
            )
        );
        idsCurrentReady.push_back(pods.back().Meta().id());
    }
    for (ui32 i = 0; i < sizeOfIdsOldReady; ++i) {
        pods.push_back(
            GetSimplePod(
                "pod_old_spec_ready_" + ToString(i)
                , NYP::NClient::NApi::NProto::TPodSpec::TPodAgentResourceCache()
                , true
            )
        );
        idsOldReady.push_back(pods.back().Meta().id());
    }
    for (ui32 i = 0; i < sizeOfIdsCurrentNotReady; ++i) {
        pods.push_back(
            GetSimplePod(
                "pod_current_spec_not_ready_" + ToString(i)
                , podAgentResourceCache
                , false
            )
        );
        idsCurrentNotReady.push_back(pods.back().Meta().id());
    }
    for (ui32 i = 0; i < sizeOfIdsOldNotReady; ++i) {
        pods.push_back(
            GetSimplePod(
                "pod_old_spec_not_ready_" + ToString(i)
                , NYP::NClient::NApi::NProto::TPodSpec::TPodAgentResourceCache()
                , false
            )
        );
        idsOldNotReady.push_back(pods.back().Meta().id());
    }
}

TVector<TString> GetUpdatesIds(const TVector<NYP::NClient::TUpdateRequest>& updates) {
    TVector<TString> updatesIds;
    for (const auto& update : updates) {
        updatesIds.push_back(update.GetObjectId());
    }

    return updatesIds;
}

void CheckGenerateUpdatesWithSpecificUpdateWindow(
    const ui32 updateWindow
    , const ui32 sizeOfIdsCurrentReady
    , const ui32 sizeOfIdsOldReady
    , const ui32 sizeOfIdsCurrentNotReady
    , const ui32 sizeOfIdsOldNotReady
    , const ui32 neededUpdateSize
) {
    NYP::NClient::TResourceCache resourceCache = GetSimpleResourceCache(updateWindow);
    TResourceCacheController controller(resourceCache);

    TVector<NYP::NClient::TPod> pods;
    TVector<TString> idsCurrentReady;
    TVector<TString> idsOldReady;
    TVector<TString> idsCurrentNotReady;
    TVector<TString> idsOldNotReady;
    NYP::NClient::NApi::NProto::TPodSpec::TPodAgentResourceCache podAgentResourceCache;
    podAgentResourceCache.mutable_spec()->CopyFrom(controller.GeneratePodAgentResourceCacheSpec());
    GenerateSpecificPods(
        sizeOfIdsCurrentReady
        , sizeOfIdsOldReady
        , sizeOfIdsCurrentNotReady
        , sizeOfIdsOldNotReady
        , pods
        , idsCurrentReady
        , idsOldReady
        , idsCurrentNotReady
        , idsOldNotReady
        , podAgentResourceCache
    );

    auto updates = controller.GenerateUpdates(pods);
    TVector<TString> neededIds = idsOldNotReady;
    Sort(idsOldReady.begin(), idsOldReady.end());
    for (ui32 i = 0; i < idsOldReady.size() && neededIds.size() < neededUpdateSize; ++i) {
        neededIds.push_back(idsOldReady[i]);
    }
    Sort(neededIds.begin(), neededIds.end());
    UNIT_ASSERT_EQUAL(updates.size(), neededUpdateSize);
    UNIT_ASSERT_EQUAL(neededIds.size(), neededUpdateSize);
    for (ui32 i = 0; i < neededUpdateSize; ++i) {
        UNIT_ASSERT_EQUAL(updates[i].GetObjectId(), neededIds[i]);
        UNIT_ASSERT_EQUAL(updates[i].GetObjectType(), NYP::NClient::NApi::NProto::EObjectType::OT_POD);
    }

    auto updatesIds = GetUpdatesIds(updates);
    TVector<ui32> seeds = {42, 1337, 777, 1, 1024};
    for (const auto seed : seeds) {
        TMersenne<ui32> generator(seed);
        Shuffle(pods.begin(), pods.end(), generator);
        auto newUpdatesIds = GetUpdatesIds(controller.GenerateUpdates(pods));
        UNIT_ASSERT_EQUAL(updatesIds, newUpdatesIds);
    }
}

Y_UNIT_TEST_SUITE(ResourceCacheControllerTest) {

Y_UNIT_TEST(TestAddNewResourcesToStatus) {
    NYP::NClient::TResourceCache resourceCache = GetSimpleResourceCache();
    resourceCache.MutableSpec()->add_cached_resources()->CopyFrom(GetSimpleCachedLayer("my_layer", 5, "layer_url", "layer_checksum"));
    resourceCache.MutableSpec()->add_cached_resources()->CopyFrom(GetSimpleCachedStaticResource("my_static_resource", 5, "static_resource_url", "static_resource_checksum", 10));

    TResourceCacheController controller(resourceCache);
    controller.ApplySpecToCurrentStatus();

    NYP::NClient::NApi::NProto::TResourceCacheStatus currentStatus = CorrectResourceCacheStatus(controller.CurrentResourceCache().Status());
    UNIT_ASSERT_EQUAL(currentStatus.cached_resource_status_size(), 2);
    UNIT_ASSERT_EQUAL(currentStatus.cached_resource_status(0).revisions_size(), 1);
    UNIT_ASSERT_EQUAL(currentStatus.cached_resource_status(1).revisions_size(), 1);

    NYP::NClient::NApi::NProto::TResourceCacheStatus loadedStatus = CorrectResourceCacheStatus(controller.LastLoadedResourceCache().Status());
    UNIT_ASSERT_EQUAL(loadedStatus.cached_resource_status_size(), 0);

    Cout << currentStatus.Utf8DebugString() << Endl;
    Cout << loadedStatus.Utf8DebugString() << Endl;
}

Y_UNIT_TEST(TestRemoveResourcesFromSpecAndStatus) {
    NYP::NClient::TResourceCache resourceCache1 = GetSimpleResourceCache();
    resourceCache1.MutableSpec()->add_cached_resources()->CopyFrom(GetSimpleCachedLayer("my_layer1", 5, "layer_url1", "layer_checksum1"));
    resourceCache1.MutableSpec()->add_cached_resources()->CopyFrom(GetSimpleCachedStaticResource("my_static_resource1", 5, "static_resource_url1", "static_resource_checksum1", 10));

    TResourceCacheController controller(resourceCache1);
    controller.ApplySpecToCurrentStatus();

    NYP::NClient::TResourceCache resourceCache2 = controller.CurrentResourceCache();
    resourceCache2.MutableSpec()->mutable_cached_resources()->Clear();
    resourceCache2.MutableSpec()->add_cached_resources()->CopyFrom(GetSimpleCachedLayer("my_layer2", 5, "layer_url2", "layer_checksum2"));
    resourceCache2.MutableSpec()->add_cached_resources()->CopyFrom(GetSimpleCachedStaticResource("my_static_resource2", 5, "static_resource_url2", "static_resource_checksum2", 10));

    controller = TResourceCacheController(resourceCache2);
    NYP::NClient::NApi::NProto::TResourceCacheStatus status1 = CorrectResourceCacheStatus(controller.CurrentResourceCache().Status());
    controller.ApplySpecToCurrentStatus();
    NYP::NClient::NApi::NProto::TResourceCacheStatus status2 = CorrectResourceCacheStatus(controller.CurrentResourceCache().Status());

    resourceCache1.MutableStatus()->CopyFrom(status2);
    controller = TResourceCacheController(resourceCache1);
    NYP::NClient::NApi::NProto::TResourceCacheStatus status3 = CorrectResourceCacheStatus(controller.CurrentResourceCache().Status());
    controller.ApplySpecToCurrentStatus();
    NYP::NClient::NApi::NProto::TResourceCacheStatus status4 = CorrectResourceCacheStatus(controller.CurrentResourceCache().Status());

    UNIT_ASSERT(google::protobuf::util::MessageDifferencer::Equals(status1, status4));
    UNIT_ASSERT(google::protobuf::util::MessageDifferencer::Equals(status2, status3));
    UNIT_ASSERT_EQUAL(status3.cached_resource_status_size(), 2);
    UNIT_ASSERT_EQUAL(status3.cached_resource_status(0).revisions_size(), 1);
    UNIT_ASSERT_EQUAL(status3.cached_resource_status(1).revisions_size(), 1);
    UNIT_ASSERT_EQUAL(status4.cached_resource_status_size(), 2);
    UNIT_ASSERT_EQUAL(status4.cached_resource_status(0).revisions_size(), 1);
    UNIT_ASSERT_EQUAL(status4.cached_resource_status(1).revisions_size(), 1);

    // status1 == status4
    // status2 == status3
    Cout << status3.Utf8DebugString() << Endl;
    Cout << status4.Utf8DebugString() << Endl;
}

Y_UNIT_TEST(TestAddNewRevisionWithoutChange) {
    NYP::NClient::TResourceCache resourceCache = GetSimpleResourceCache();
    resourceCache.MutableSpec()->add_cached_resources()->CopyFrom(GetSimpleCachedLayer("my_layer", 5, "layer_url", "layer_checksum"));
    resourceCache.MutableSpec()->add_cached_resources()->CopyFrom(GetSimpleCachedStaticResource("my_static_resource", 5, "static_resource_url", "static_resource_checksum", 10));

    TResourceCacheController controller(resourceCache);
    controller.ApplySpecToCurrentStatus();

    NYP::NClient::NApi::NProto::TResourceCacheStatus status1 = CorrectResourceCacheStatus(controller.CurrentResourceCache().Status());
    resourceCache = controller.CurrentResourceCache();
    resourceCache.MutableSpec()->set_revision(2);
    controller = TResourceCacheController(resourceCache);
    controller.ApplySpecToCurrentStatus();

    NYP::NClient::NApi::NProto::TResourceCacheStatus status2 = CorrectResourceCacheStatus(controller.CurrentResourceCache().Status());

    UNIT_ASSERT_EQUAL(status1.cached_resource_status_size(), 2);
    UNIT_ASSERT_EQUAL(status1.cached_resource_status(0).revisions_size(), 1);
    UNIT_ASSERT_EQUAL(status1.cached_resource_status(1).revisions_size(), 1);
    UNIT_ASSERT_EQUAL(status1.cached_resource_status(0).revisions(0).revision(), 1);
    UNIT_ASSERT_EQUAL(status1.cached_resource_status(1).revisions(0).revision(), 1);
    UNIT_ASSERT_EQUAL(status2.cached_resource_status_size(), 2);
    UNIT_ASSERT_EQUAL(status2.cached_resource_status(0).revisions_size(), 1);
    UNIT_ASSERT_EQUAL(status2.cached_resource_status(1).revisions_size(), 1);
    UNIT_ASSERT_EQUAL(status2.cached_resource_status(0).revisions(0).revision(), 2);
    UNIT_ASSERT_EQUAL(status2.cached_resource_status(1).revisions(0).revision(), 2);

    Cout << status1.Utf8DebugString() << Endl;
    Cout << status2.Utf8DebugString() << Endl;
}

Y_UNIT_TEST(TestAddNewRevisionWithChange) {
    NYP::NClient::TResourceCache resourceCache = GetSimpleResourceCache();
    resourceCache.MutableSpec()->add_cached_resources()->CopyFrom(GetSimpleCachedLayer("my_layer", 5, "layer_url", "layer_checksum"));
    resourceCache.MutableSpec()->add_cached_resources()->CopyFrom(GetSimpleCachedStaticResource("my_static_resource", 5, "static_resource_url", "static_resource_checksum", 10));

    TResourceCacheController controller(resourceCache);
    controller.ApplySpecToCurrentStatus();

    NYP::NClient::NApi::NProto::TResourceCacheStatus status1 = CorrectResourceCacheStatus(controller.CurrentResourceCache().Status());
    resourceCache = controller.CurrentResourceCache();
    resourceCache.MutableSpec()->set_revision(2);
    *resourceCache.MutableSpec()->mutable_cached_resources(0)->mutable_layer()->mutable_url() += "_other";
    *resourceCache.MutableSpec()->mutable_cached_resources(1)->mutable_static_resource()->mutable_url() += "_other";
    controller = TResourceCacheController(resourceCache);
    controller.ApplySpecToCurrentStatus();

    NYP::NClient::NApi::NProto::TResourceCacheStatus status2 = CorrectResourceCacheStatus(controller.CurrentResourceCache().Status());

    UNIT_ASSERT_EQUAL(status1.cached_resource_status_size(), 2);
    UNIT_ASSERT_EQUAL(status1.cached_resource_status(0).revisions_size(), 1);
    UNIT_ASSERT_EQUAL(status1.cached_resource_status(1).revisions_size(), 1);
    UNIT_ASSERT_EQUAL(status1.cached_resource_status(0).revisions(0).revision(), 1);
    UNIT_ASSERT_EQUAL(status1.cached_resource_status(1).revisions(0).revision(), 1);
    UNIT_ASSERT_EQUAL(status2.cached_resource_status_size(), 2);
    UNIT_ASSERT_EQUAL(status2.cached_resource_status(0).revisions_size(), 2);
    UNIT_ASSERT_EQUAL(status2.cached_resource_status(1).revisions_size(), 2);
    UNIT_ASSERT_EQUAL(status2.cached_resource_status(0).revisions(0).revision(), 2);
    UNIT_ASSERT_EQUAL(status2.cached_resource_status(1).revisions(0).revision(), 2);
    UNIT_ASSERT_EQUAL(status2.cached_resource_status(0).revisions(1).revision(), 1);
    UNIT_ASSERT_EQUAL(status2.cached_resource_status(1).revisions(1).revision(), 1);

    Cout << status1.Utf8DebugString() << Endl;
    Cout << status2.Utf8DebugString() << Endl;
}

Y_UNIT_TEST(TestMoveOldRevisionToFront) {
    const size_t revisions = 5;
    const size_t maxLatestRevisions = 10;
    const size_t latestRevision = 14;
    const size_t specialRevision = 3;
    NYP::NClient::TResourceCache resourceCache = GetSimpleResourceCache();
    resourceCache.MutableSpec()->add_cached_resources()->CopyFrom(GetSimpleCachedLayer("my_layer", maxLatestRevisions, "layer_url", "layer_checksum"));
    resourceCache.MutableSpec()->add_cached_resources()->CopyFrom(GetSimpleCachedStaticResource("my_static_resource", maxLatestRevisions, "static_resource_url", "static_resource_checksum", 10));

    TResourceCacheController controller(resourceCache);
    controller.ApplySpecToCurrentStatus();

    NYP::NClient::NApi::NProto::TResourceCacheStatus status1 = CorrectResourceCacheStatus(controller.CurrentResourceCache().Status());
    resourceCache = controller.CurrentResourceCache();
    resourceCache.MutableSpec()->set_revision(latestRevision);

    for (size_t i = 0; i < 2; ++i) {
        NYP::NClient::NApi::NProto::TResourceCacheStatus::TCachedResourceRevisionStatus revisionTemplate;
        revisionTemplate.CopyFrom(resourceCache.Status().cached_resource_status(i).revisions(0));
        resourceCache.MutableStatus()->mutable_cached_resource_status(i)->mutable_revisions()->Clear();
        for (size_t j = 0; j < revisions; ++j) {
            auto* currentRevision = resourceCache.MutableStatus()->mutable_cached_resource_status(i)->mutable_revisions()->Add();
            currentRevision->CopyFrom(revisionTemplate);
            currentRevision->set_revision(revisions - j);
            if (currentRevision->has_layer()) {
                *currentRevision->mutable_layer()->mutable_url() += "_other_" + ToString(revisions - j);
            } else if (currentRevision->has_static_resource()) {
                *currentRevision->mutable_static_resource()->mutable_url() += "_other_" + ToString(revisions - j);
            }
        }
        if (resourceCache.Spec().cached_resources(i).has_layer()) {
            *resourceCache.MutableSpec()->mutable_cached_resources(i)->mutable_layer()->mutable_url() += "_other_" + ToString(specialRevision);
        } else if (resourceCache.Spec().cached_resources(i).has_static_resource()) {
            *resourceCache.MutableSpec()->mutable_cached_resources(i)->mutable_static_resource()->mutable_url() += "_other_" + ToString(specialRevision);
        }
    }

    controller = TResourceCacheController(resourceCache);
    controller.ApplySpecToCurrentStatus();

    NYP::NClient::NApi::NProto::TResourceCacheStatus status2 = CorrectResourceCacheStatus(controller.CurrentResourceCache().Status());

    UNIT_ASSERT_EQUAL(status2.cached_resource_status_size(), 2);
    UNIT_ASSERT_EQUAL(status2.cached_resource_status(0).revisions_size(), revisions);
    UNIT_ASSERT_EQUAL(status2.cached_resource_status(1).revisions_size(), revisions);
    for (size_t i = 0; i < revisions; ++i) {
        size_t revisionNow = 0;
        if (i == 0) {
            revisionNow = latestRevision;
        } else if (i < specialRevision) {
            revisionNow = revisions - i + 1;
        } else {
            revisionNow = revisions - i;
        }
        UNIT_ASSERT_EQUAL(status2.cached_resource_status(0).revisions(i).revision(), revisionNow);
        UNIT_ASSERT_EQUAL(status2.cached_resource_status(1).revisions(i).revision(), revisionNow);
    }

    Cout << status1.Utf8DebugString() << Endl;
    Cout << status2.Utf8DebugString() << Endl;
}

Y_UNIT_TEST(TestRemoveExtraRevisions) {
    const size_t revisions = 10;
    const size_t maxLatestRevisions = 5;
    const size_t latestRevision = 14;
    const size_t specialRevision = 3;
    NYP::NClient::TResourceCache resourceCache = GetSimpleResourceCache();
    resourceCache.MutableSpec()->add_cached_resources()->CopyFrom(GetSimpleCachedLayer("my_layer", maxLatestRevisions, "layer_url", "layer_checksum"));
    resourceCache.MutableSpec()->add_cached_resources()->CopyFrom(GetSimpleCachedStaticResource("my_static_resource", maxLatestRevisions, "static_resource_url", "static_resource_checksum", 10));

    TResourceCacheController controller(resourceCache);
    controller.ApplySpecToCurrentStatus();

    NYP::NClient::NApi::NProto::TResourceCacheStatus status1 = CorrectResourceCacheStatus(controller.CurrentResourceCache().Status());
    resourceCache = controller.CurrentResourceCache();
    resourceCache.MutableSpec()->set_revision(latestRevision);

    for (size_t i = 0; i < 2; ++i) {
        NYP::NClient::NApi::NProto::TResourceCacheStatus::TCachedResourceRevisionStatus revisionTemplate;
        revisionTemplate.CopyFrom(resourceCache.Status().cached_resource_status(i).revisions(0));
        resourceCache.MutableStatus()->mutable_cached_resource_status(i)->mutable_revisions()->Clear();
        for (size_t j = 0; j < revisions; ++j) {
            auto* currentRevision = resourceCache.MutableStatus()->mutable_cached_resource_status(i)->mutable_revisions()->Add();
            currentRevision->CopyFrom(revisionTemplate);
            currentRevision->set_revision(revisions - j);
            if (currentRevision->has_layer()) {
                *currentRevision->mutable_layer()->mutable_url() += "_other_" + ToString(revisions - j);
            } else if (currentRevision->has_static_resource()) {
                *currentRevision->mutable_static_resource()->mutable_url() += "_other_" + ToString(revisions - j);
            }
        }
        if (resourceCache.Spec().cached_resources(i).has_layer()) {
            *resourceCache.MutableSpec()->mutable_cached_resources(i)->mutable_layer()->mutable_url() += "_other_" + ToString(specialRevision);
        } else if (resourceCache.Spec().cached_resources(i).has_static_resource()) {
            *resourceCache.MutableSpec()->mutable_cached_resources(i)->mutable_static_resource()->mutable_url() += "_other_" + ToString(specialRevision);
        }
    }

    controller = TResourceCacheController(resourceCache);
    controller.ApplySpecToCurrentStatus();

    NYP::NClient::NApi::NProto::TResourceCacheStatus status2 = CorrectResourceCacheStatus(controller.CurrentResourceCache().Status());

    UNIT_ASSERT_EQUAL(status2.cached_resource_status_size(), 2);
    UNIT_ASSERT_EQUAL(status2.cached_resource_status(0).revisions_size(), maxLatestRevisions);
    UNIT_ASSERT_EQUAL(status2.cached_resource_status(1).revisions_size(), maxLatestRevisions);
    for (size_t i = 0; i < maxLatestRevisions; ++i) {
        size_t revisionNow = 0;
        if (i == 0) {
            revisionNow = latestRevision;
        } else {
            revisionNow = revisions - i + 1;
        }
        UNIT_ASSERT_EQUAL(status2.cached_resource_status(0).revisions(i).revision(), revisionNow);
        UNIT_ASSERT_EQUAL(status2.cached_resource_status(1).revisions(i).revision(), revisionNow);
    }

    Cout << status1.Utf8DebugString() << Endl;
    Cout << status2.Utf8DebugString() << Endl;
}

Y_UNIT_TEST(TestRemoveSameRevisions) {
    const size_t revisions = 10;
    const size_t maxLatestRevisions = 10;
    const size_t latestRevision = 14;
    NYP::NClient::TResourceCache resourceCache = GetSimpleResourceCache();
    resourceCache.MutableSpec()->add_cached_resources()->CopyFrom(GetSimpleCachedLayer("my_layer", maxLatestRevisions, "layer_url", "layer_checksum"));
    resourceCache.MutableSpec()->add_cached_resources()->CopyFrom(GetSimpleCachedStaticResource("my_static_resource", maxLatestRevisions, "static_resource_url", "static_resource_checksum", 10));

    TResourceCacheController controller(resourceCache);
    controller.ApplySpecToCurrentStatus();

    NYP::NClient::NApi::NProto::TResourceCacheStatus status1 = CorrectResourceCacheStatus(controller.CurrentResourceCache().Status());
    resourceCache = controller.CurrentResourceCache();
    resourceCache.MutableSpec()->set_revision(latestRevision);

    for (size_t i = 0; i < 2; ++i) {
        NYP::NClient::NApi::NProto::TResourceCacheStatus::TCachedResourceRevisionStatus revisionTemplate;
        revisionTemplate.CopyFrom(resourceCache.Status().cached_resource_status(i).revisions(0));
        resourceCache.MutableStatus()->mutable_cached_resource_status(i)->mutable_revisions()->Clear();
        for (size_t j = 0; j < revisions; ++j) {
            auto* currentRevision = resourceCache.MutableStatus()->mutable_cached_resource_status(i)->mutable_revisions()->Add();
            currentRevision->CopyFrom(revisionTemplate);
            currentRevision->set_revision(revisions - j);
            if (currentRevision->has_layer()) {
                *currentRevision->mutable_layer()->mutable_url() += "_all_same";
            } else if (currentRevision->has_static_resource()) {
                *currentRevision->mutable_static_resource()->mutable_url() += "_all_same";
            }
        }
        if (resourceCache.Spec().cached_resources(i).has_layer()) {
            *resourceCache.MutableSpec()->mutable_cached_resources(i)->mutable_layer()->mutable_url() += "_other_" + ToString(latestRevision);
        } else if (resourceCache.Spec().cached_resources(i).has_static_resource()) {
            *resourceCache.MutableSpec()->mutable_cached_resources(i)->mutable_static_resource()->mutable_url() += "_other_" + ToString(latestRevision);
        }
    }

    controller = TResourceCacheController(resourceCache);
    controller.ApplySpecToCurrentStatus();

    NYP::NClient::NApi::NProto::TResourceCacheStatus status2 = CorrectResourceCacheStatus(controller.CurrentResourceCache().Status());

    UNIT_ASSERT_EQUAL(status2.cached_resource_status_size(), 2);
    UNIT_ASSERT_EQUAL(status2.cached_resource_status(0).revisions_size(), 2);
    UNIT_ASSERT_EQUAL(status2.cached_resource_status(1).revisions_size(), 2);
    UNIT_ASSERT_EQUAL(status2.cached_resource_status(0).revisions(0).revision(), latestRevision);
    UNIT_ASSERT_EQUAL(status2.cached_resource_status(1).revisions(0).revision(), latestRevision);

    Cout << status1.Utf8DebugString() << Endl;
    Cout << status2.Utf8DebugString() << Endl;
}

Y_UNIT_TEST(TestCondition) {
    const size_t numberOfPods = 5;
    const size_t revisions = 3;
    const TString layerId = "my_layer";
    const TString staticResourceId = "my_static_resource";
    NYP::NClient::TResourceCache resourceCache = GetSimpleResourceCache();
    resourceCache.MutableSpec()->add_cached_resources()->CopyFrom(GetSimpleCachedLayer(layerId, 5, "layer_url", "layer_checksum"));
    resourceCache.MutableSpec()->add_cached_resources()->CopyFrom(GetSimpleCachedStaticResource(staticResourceId, 5, "static_resource_url", "static_resource_checksum", 10));

    TVector<NYP::NClient::TPod> pods;
    for (size_t i = 0; i < numberOfPods; ++i) {
        pods.push_back(GetSimplePod("pod_id"));
        for (size_t j = 1; j <= revisions; ++j) {
            AddLayerStatusToPodStatus(pods.back(), GetSimpleLayerStatus(layerId, j, (j == 1), (j == 2), (j == 3), i, i + 1));
            AddLayerStatusToPodStatus(pods.back(), GetSimpleLayerStatus(layerId + "_other", j, true, true, true, i, i + 1));
            AddStaticResourceStatusToPodStatus(pods.back(), GetSimpleStaticResourceStatus(staticResourceId, j, (j == 1), (j == 2), (j == 3), i, i + 1));
            AddStaticResourceStatusToPodStatus(pods.back(), GetSimpleStaticResourceStatus(staticResourceId + "_other", j, true, true, true, i, i + 1));
        }
    }

    TResourceCacheController controller(resourceCache);
    controller.ApplySpecToCurrentStatus();
    for (size_t revision = 2; revision <= revisions; ++revision) {
        resourceCache = controller.CurrentResourceCache();
        resourceCache.MutableSpec()->set_revision(revision);
        resourceCache.MutableSpec()->mutable_cached_resources(0)->mutable_layer()->set_url("layer_url" + ToString(revision));
        resourceCache.MutableSpec()->mutable_cached_resources(1)->mutable_static_resource()->set_url("static_resource_url" + ToString(revision));
        controller = TResourceCacheController(resourceCache);
        controller.ApplySpecToCurrentStatus();
    }
    const size_t statusCalculatingIterations = 3;
    for (size_t i = 1; i <= statusCalculatingIterations; ++i) {
        controller.AddPodsStatusToCurrentStatus(pods);
    }

    NYP::NClient::NApi::NProto::TResourceCacheStatus status = CorrectResourceCacheStatus(controller.CurrentResourceCache().Status());
    status = CorrectResourceCacheStatus(controller.CurrentResourceCache().Status());

    UNIT_ASSERT_EQUAL(status.cached_resource_status_size(), 2);
    UNIT_ASSERT_EQUAL(status.cached_resource_status(0).revisions_size(), 3);
    UNIT_ASSERT_EQUAL(status.cached_resource_status(1).revisions_size(), 3);

    UNIT_ASSERT_EQUAL(status.all_in_progress().pod_count(), numberOfPods);
    UNIT_ASSERT_EQUAL(status.all_in_progress().condition().status(), NYP::NClient::NApi::NProto::EConditionStatus::CS_TRUE);
    UNIT_ASSERT_EQUAL(status.all_ready().pod_count(), 0);
    UNIT_ASSERT_EQUAL(status.all_ready().condition().status(), NYP::NClient::NApi::NProto::EConditionStatus::CS_FALSE);
    UNIT_ASSERT_EQUAL(status.all_failed().pod_count(), numberOfPods);
    UNIT_ASSERT_EQUAL(status.all_failed().condition().status(), NYP::NClient::NApi::NProto::EConditionStatus::CS_TRUE);

    UNIT_ASSERT_EQUAL(status.latest_in_progress().pod_count(), 0);
    UNIT_ASSERT_EQUAL(status.latest_in_progress().condition().status(), NYP::NClient::NApi::NProto::EConditionStatus::CS_FALSE);
    UNIT_ASSERT_EQUAL(status.latest_ready().pod_count(), 0);
    UNIT_ASSERT_EQUAL(status.latest_ready().condition().status(), NYP::NClient::NApi::NProto::EConditionStatus::CS_FALSE);
    UNIT_ASSERT_EQUAL(status.latest_failed().pod_count(), numberOfPods);
    UNIT_ASSERT_EQUAL(status.latest_failed().condition().status(), NYP::NClient::NApi::NProto::EConditionStatus::CS_TRUE);

    for (size_t i = 0; i < 2; ++i) {
        UNIT_ASSERT_EQUAL(status.cached_resource_status(i).revisions(0).in_progress().pod_count(), 0);
        UNIT_ASSERT_EQUAL(status.cached_resource_status(i).revisions(0).in_progress().condition().status(), NYP::NClient::NApi::NProto::EConditionStatus::CS_FALSE);
        UNIT_ASSERT_EQUAL(status.cached_resource_status(i).revisions(0).ready().pod_count(), 0);
        UNIT_ASSERT_EQUAL(status.cached_resource_status(i).revisions(0).ready().condition().status(), NYP::NClient::NApi::NProto::EConditionStatus::CS_FALSE);
        UNIT_ASSERT_EQUAL(status.cached_resource_status(i).revisions(0).failed().pod_count(), numberOfPods);
        UNIT_ASSERT_EQUAL(status.cached_resource_status(i).revisions(0).failed().condition().status(), NYP::NClient::NApi::NProto::EConditionStatus::CS_TRUE);

        UNIT_ASSERT_EQUAL(status.cached_resource_status(i).revisions(1).in_progress().pod_count(), numberOfPods);
        UNIT_ASSERT_EQUAL(status.cached_resource_status(i).revisions(1).in_progress().condition().status(), NYP::NClient::NApi::NProto::EConditionStatus::CS_TRUE);
        UNIT_ASSERT_EQUAL(status.cached_resource_status(i).revisions(1).ready().pod_count(), 0);
        UNIT_ASSERT_EQUAL(status.cached_resource_status(i).revisions(1).ready().condition().status(), NYP::NClient::NApi::NProto::EConditionStatus::CS_FALSE);
        UNIT_ASSERT_EQUAL(status.cached_resource_status(i).revisions(1).failed().pod_count(), 0);
        UNIT_ASSERT_EQUAL(status.cached_resource_status(i).revisions(1).failed().condition().status(), NYP::NClient::NApi::NProto::EConditionStatus::CS_FALSE);

        UNIT_ASSERT_EQUAL(status.cached_resource_status(i).revisions(2).in_progress().pod_count(), 0);
        UNIT_ASSERT_EQUAL(status.cached_resource_status(i).revisions(2).in_progress().condition().status(), NYP::NClient::NApi::NProto::EConditionStatus::CS_FALSE);
        UNIT_ASSERT_EQUAL(status.cached_resource_status(i).revisions(2).ready().pod_count(), numberOfPods);
        UNIT_ASSERT_EQUAL(status.cached_resource_status(i).revisions(2).ready().condition().status(), NYP::NClient::NApi::NProto::EConditionStatus::CS_TRUE);
        UNIT_ASSERT_EQUAL(status.cached_resource_status(i).revisions(2).failed().pod_count(), 0);
        UNIT_ASSERT_EQUAL(status.cached_resource_status(i).revisions(2).failed().condition().status(), NYP::NClient::NApi::NProto::EConditionStatus::CS_FALSE);
    }

    Cout << status.Utf8DebugString() << Endl;
}

Y_UNIT_TEST(TestConditionAllReady) {
    const size_t numberOfPods = 5;
    const size_t revisions = 3;
    const TString layerId = "my_layer";
    const TString staticResourceId = "my_static_resource";
    NYP::NClient::TResourceCache resourceCache = GetSimpleResourceCache();
    resourceCache.MutableSpec()->add_cached_resources()->CopyFrom(GetSimpleCachedLayer(layerId, 5, "layer_url", "layer_checksum"));
    resourceCache.MutableSpec()->add_cached_resources()->CopyFrom(GetSimpleCachedStaticResource(staticResourceId, 5, "static_resource_url", "static_resource_checksum", 10));

    TVector<NYP::NClient::TPod> pods;
    for (size_t i = 0; i < numberOfPods; ++i) {
        pods.push_back(GetSimplePod("pod_id"));
        for (size_t j = 1; j <= revisions; ++j) {
            AddLayerStatusToPodStatus(pods.back(), GetSimpleLayerStatus(layerId, j, true, false, false, i, i + 1));
            AddLayerStatusToPodStatus(pods.back(), GetSimpleLayerStatus(layerId + "_other", j, false, true, true, i, i + 1));
            AddStaticResourceStatusToPodStatus(pods.back(), GetSimpleStaticResourceStatus(staticResourceId, j, true, false, false, i, i + 1));
            AddStaticResourceStatusToPodStatus(pods.back(), GetSimpleStaticResourceStatus(staticResourceId + "_other", j, false, true, true, i, i + 1));
        }
    }

    TResourceCacheController controller(resourceCache);
    controller.ApplySpecToCurrentStatus();
    for (size_t revision = 2; revision <= revisions; ++revision) {
        resourceCache = controller.CurrentResourceCache();
        resourceCache.MutableSpec()->set_revision(revision);
        resourceCache.MutableSpec()->mutable_cached_resources(0)->mutable_layer()->set_url("layer_url" + ToString(revision));
        resourceCache.MutableSpec()->mutable_cached_resources(1)->mutable_static_resource()->set_url("static_resource_url" + ToString(revision));
        controller = TResourceCacheController(resourceCache);
        controller.ApplySpecToCurrentStatus();
    }
    const size_t statusCalculatingIterations = 3;
    for (size_t i = 1; i <= statusCalculatingIterations; ++i) {
        controller.AddPodsStatusToCurrentStatus(pods);
    }

    NYP::NClient::NApi::NProto::TResourceCacheStatus status = CorrectResourceCacheStatus(controller.CurrentResourceCache().Status());

    UNIT_ASSERT_EQUAL(status.cached_resource_status_size(), 2);
    UNIT_ASSERT_EQUAL(status.cached_resource_status(0).revisions_size(), 3);
    UNIT_ASSERT_EQUAL(status.cached_resource_status(1).revisions_size(), 3);

    UNIT_ASSERT_EQUAL(status.all_in_progress().pod_count(), 0);
    UNIT_ASSERT_EQUAL(status.all_in_progress().condition().status(), NYP::NClient::NApi::NProto::EConditionStatus::CS_FALSE);
    UNIT_ASSERT_EQUAL(status.all_ready().pod_count(), numberOfPods);
    UNIT_ASSERT_EQUAL(status.all_ready().condition().status(), NYP::NClient::NApi::NProto::EConditionStatus::CS_TRUE);
    UNIT_ASSERT_EQUAL(status.all_failed().pod_count(), 0);
    UNIT_ASSERT_EQUAL(status.all_failed().condition().status(), NYP::NClient::NApi::NProto::EConditionStatus::CS_FALSE);

    UNIT_ASSERT_EQUAL(status.latest_in_progress().pod_count(), 0);
    UNIT_ASSERT_EQUAL(status.latest_in_progress().condition().status(), NYP::NClient::NApi::NProto::EConditionStatus::CS_FALSE);
    UNIT_ASSERT_EQUAL(status.latest_ready().pod_count(), numberOfPods);
    UNIT_ASSERT_EQUAL(status.latest_ready().condition().status(), NYP::NClient::NApi::NProto::EConditionStatus::CS_TRUE);
    UNIT_ASSERT_EQUAL(status.latest_failed().pod_count(), 0);
    UNIT_ASSERT_EQUAL(status.latest_failed().condition().status(), NYP::NClient::NApi::NProto::EConditionStatus::CS_FALSE);

    for (size_t i = 0; i < 2; ++i) {
        UNIT_ASSERT_EQUAL(status.cached_resource_status(i).revisions(0).in_progress().pod_count(), 0);
        UNIT_ASSERT_EQUAL(status.cached_resource_status(i).revisions(0).in_progress().condition().status(), NYP::NClient::NApi::NProto::EConditionStatus::CS_FALSE);
        UNIT_ASSERT_EQUAL(status.cached_resource_status(i).revisions(0).ready().pod_count(), pods.size());
        UNIT_ASSERT_EQUAL(status.cached_resource_status(i).revisions(0).ready().condition().status(), NYP::NClient::NApi::NProto::EConditionStatus::CS_TRUE);
        UNIT_ASSERT_EQUAL(status.cached_resource_status(i).revisions(0).failed().pod_count(), 0);
        UNIT_ASSERT_EQUAL(status.cached_resource_status(i).revisions(0).failed().condition().status(), NYP::NClient::NApi::NProto::EConditionStatus::CS_FALSE);

        UNIT_ASSERT_EQUAL(status.cached_resource_status(i).revisions(1).in_progress().pod_count(), 0);
        UNIT_ASSERT_EQUAL(status.cached_resource_status(i).revisions(1).in_progress().condition().status(), NYP::NClient::NApi::NProto::EConditionStatus::CS_FALSE);
        UNIT_ASSERT_EQUAL(status.cached_resource_status(i).revisions(1).ready().pod_count(), pods.size());
        UNIT_ASSERT_EQUAL(status.cached_resource_status(i).revisions(1).ready().condition().status(), NYP::NClient::NApi::NProto::EConditionStatus::CS_TRUE);
        UNIT_ASSERT_EQUAL(status.cached_resource_status(i).revisions(1).failed().pod_count(), 0);
        UNIT_ASSERT_EQUAL(status.cached_resource_status(i).revisions(1).failed().condition().status(), NYP::NClient::NApi::NProto::EConditionStatus::CS_FALSE);

        UNIT_ASSERT_EQUAL(status.cached_resource_status(i).revisions(2).in_progress().pod_count(), 0);
        UNIT_ASSERT_EQUAL(status.cached_resource_status(i).revisions(2).in_progress().condition().status(), NYP::NClient::NApi::NProto::EConditionStatus::CS_FALSE);
        UNIT_ASSERT_EQUAL(status.cached_resource_status(i).revisions(2).ready().pod_count(), pods.size());
        UNIT_ASSERT_EQUAL(status.cached_resource_status(i).revisions(2).ready().condition().status(), NYP::NClient::NApi::NProto::EConditionStatus::CS_TRUE);
        UNIT_ASSERT_EQUAL(status.cached_resource_status(i).revisions(2).failed().pod_count(), 0);
        UNIT_ASSERT_EQUAL(status.cached_resource_status(i).revisions(2).failed().condition().status(), NYP::NClient::NApi::NProto::EConditionStatus::CS_FALSE);
    }

    Cout << status.Utf8DebugString() << Endl;
}

Y_UNIT_TEST(TestGenerateUpdates) {
    const size_t numberOfPods = 5;
    const size_t numberOfUpdatedPods = 2;
    const size_t revisions = 2;
    const TString layerId = "my_layer";
    const TString staticResourceId = "my_static_resource";
    NYP::NClient::TResourceCache resourceCache = GetSimpleResourceCache();
    resourceCache.MutableSpec()->add_cached_resources()->CopyFrom(GetSimpleCachedLayer(layerId, 5, "layer_url", "layer_checksum"));
    resourceCache.MutableSpec()->add_cached_resources()->CopyFrom(GetSimpleCachedStaticResource(staticResourceId, 5, "static_resource_url", "static_resource_checksum", 10));

    TVector<NYP::NClient::TPod> pods;
    for (size_t i = 0; i < numberOfPods; ++i) {
        pods.push_back(GetSimplePod("pod_id"));
        for (size_t j = 1; j <= revisions; ++j) {
            AddLayerStatusToPodStatus(pods.back(), GetSimpleLayerStatus(layerId, j, true, false, false, i, i + 1));
            AddStaticResourceStatusToPodStatus(pods.back(), GetSimpleStaticResourceStatus(staticResourceId, j, true, false, false, i, i + 1));
        }
    }

    TResourceCacheController controller(resourceCache);
    UNIT_ASSERT_EQUAL(controller.GenerateUpdates(pods).size(), numberOfPods);
    controller.ApplySpecToCurrentStatus();
    UNIT_ASSERT_EQUAL(controller.GenerateUpdates(pods).size(), 1 + numberOfPods);
    controller = TResourceCacheController(controller.CurrentResourceCache());
    UNIT_ASSERT_EQUAL(controller.GenerateUpdates(pods).size(), numberOfPods);

    for (size_t i = 0; i < numberOfUpdatedPods; ++i) {
        pods[i].MutableSpec()->mutable_resource_cache()->mutable_spec()->CopyFrom(controller.GeneratePodAgentResourceCacheSpec());
    }
    UNIT_ASSERT_EQUAL(controller.GenerateUpdates(pods).size(), numberOfPods - numberOfUpdatedPods);

    NYP::NClient::NApi::NProto::TResourceCacheStatus currentStatus = CorrectResourceCacheStatus(controller.CurrentResourceCache().Status());
    NYP::NClient::NApi::NProto::TResourceCacheStatus loadedStatus = CorrectResourceCacheStatus(controller.LastLoadedResourceCache().Status());

    Cout << currentStatus.Utf8DebugString() << Endl;
    Cout << loadedStatus.Utf8DebugString() << Endl;
}

Y_UNIT_TEST(TestGenerateUpdatesWithSpecificUpdateWindow) {
    CheckGenerateUpdatesWithSpecificUpdateWindow(12, 1, 5, 3, 7, 9);
    CheckGenerateUpdatesWithSpecificUpdateWindow(11, 1, 5, 3, 7, 8);
    CheckGenerateUpdatesWithSpecificUpdateWindow(10, 1, 5, 3, 7, 7);
    CheckGenerateUpdatesWithSpecificUpdateWindow(999, 1, 5, 3, 7, 12);
    CheckGenerateUpdatesWithSpecificUpdateWindow(0, 5, 5, 5, 5, 10);
    CheckGenerateUpdatesWithSpecificUpdateWindow(3, 5, 0, 5, 0, 0);
}

Y_UNIT_TEST(TestGeneratePodAgentResourceCacheSpec) {
    const TString layerId = "my_layer";
    const TString layerUrl = "my_layer_url";
    const TString layerChecksum = "my_layer_checksum";
    const TString staticResourceId = "my_static_resource";
    const TString staticResourceUrl = "my_static_resource_url";
    const TString staticResourceChecksum = "my_static_resource_checksum";
    const ui32 staticResourceCheckPeriodMs = 10;
    NYP::NClient::TResourceCache resourceCache = GetSimpleResourceCache();
    resourceCache.MutableSpec()->add_cached_resources()->CopyFrom(GetSimpleCachedLayer(layerId, 5, layerUrl, layerChecksum));
    resourceCache.MutableSpec()->add_cached_resources()->CopyFrom(GetSimpleCachedStaticResource(staticResourceId, 5, staticResourceUrl, staticResourceChecksum, staticResourceCheckPeriodMs));

    TResourceCacheController controller(resourceCache);

    NInfra::NPodAgent::API::TPodAgentResourceCacheSpec spec1 = controller.GeneratePodAgentResourceCacheSpec();

    UNIT_ASSERT_EQUAL(spec1.layers_size(), 0);
    UNIT_ASSERT_EQUAL(spec1.static_resources_size(), 0);

    controller.ApplySpecToCurrentStatus();

    NInfra::NPodAgent::API::TPodAgentResourceCacheSpec spec2 = controller.GeneratePodAgentResourceCacheSpec();
    UNIT_ASSERT_EQUAL(spec2.layers_size(), 1);
    UNIT_ASSERT_EQUAL(spec2.layers(0).revision(), 1);
    UNIT_ASSERT_EQUAL(spec2.layers(0).layer().id(), layerId);
    UNIT_ASSERT_EQUAL(spec2.layers(0).layer().url(), layerUrl);
    UNIT_ASSERT_EQUAL(spec2.static_resources_size(), 1);
    UNIT_ASSERT_EQUAL(spec2.static_resources(0).revision(), 1);
    UNIT_ASSERT_EQUAL(spec2.static_resources(0).resource().id(), staticResourceId);
    UNIT_ASSERT_EQUAL(spec2.static_resources(0).resource().url(), staticResourceUrl);
    UNIT_ASSERT_EQUAL(spec2.static_resources(0).resource().verification().checksum(), staticResourceChecksum);
    UNIT_ASSERT_EQUAL(spec2.static_resources(0).resource().verification().check_period_ms(), staticResourceCheckPeriodMs);

    resourceCache = controller.CurrentResourceCache();
    resourceCache.MutableSpec()->set_revision(2);
    *resourceCache.MutableSpec()->mutable_cached_resources(0)->mutable_layer()->mutable_url() += "_other";
    *resourceCache.MutableSpec()->mutable_cached_resources(1)->mutable_static_resource()->mutable_url() += "_other";
    controller = TResourceCacheController(resourceCache);
    controller.ApplySpecToCurrentStatus();

    NInfra::NPodAgent::API::TPodAgentResourceCacheSpec spec3 = controller.GeneratePodAgentResourceCacheSpec();
    UNIT_ASSERT_EQUAL(spec3.layers_size(), 2);
    UNIT_ASSERT_EQUAL(spec3.static_resources_size(), 2);
    for (size_t i = 0; i < 2; ++i) {
        UNIT_ASSERT_EQUAL(spec3.layers(i).revision(), i + 1);
        UNIT_ASSERT_EQUAL(spec3.layers(i).layer().id(), layerId);
        UNIT_ASSERT_EQUAL(spec3.layers(i).layer().url(), layerUrl + (i == 1 ? "_other" : ""));
        UNIT_ASSERT_EQUAL(spec3.static_resources(i).revision(), i + 1);
        UNIT_ASSERT_EQUAL(spec3.static_resources(i).resource().id(), staticResourceId);
        UNIT_ASSERT_EQUAL(spec3.static_resources(i).resource().url(), staticResourceUrl + (i == 1 ? "_other" : ""));
        UNIT_ASSERT_EQUAL(spec3.static_resources(i).resource().verification().checksum(), staticResourceChecksum);
        UNIT_ASSERT_EQUAL(spec3.static_resources(i).resource().verification().check_period_ms(), staticResourceCheckPeriodMs);
    }

    Cout << spec1.Utf8DebugString() << Endl;
    Cout << spec2.Utf8DebugString() << Endl;
    Cout << spec3.Utf8DebugString() << Endl;
}

Y_UNIT_TEST(TestConditionLastTransitionTime) {
    const size_t numberOfPods = 3;
    const TString layerId = "my_layer";
    const TString staticResourceId = "my_static_resource";
    NYP::NClient::TResourceCache resourceCache = GetSimpleResourceCache();
    resourceCache.MutableSpec()->add_cached_resources()->CopyFrom(GetSimpleCachedLayer(layerId, 3, "layer_url", "layer_checksum"));
    resourceCache.MutableSpec()->add_cached_resources()->CopyFrom(GetSimpleCachedStaticResource(staticResourceId, 3, "static_resource_url", "static_resource_checksum", 10));

    TVector<NYP::NClient::TPod> pods;
    for (size_t i = 0; i < numberOfPods; ++i) {
        pods.push_back(GetSimplePod("pod_id"));
        AddLayerStatusToPodStatus(pods.back(), GetSimpleLayerStatus(layerId, 1, true, false, false));
        AddStaticResourceStatusToPodStatus(pods.back(), GetSimpleStaticResourceStatus(staticResourceId, 1, true, false, false));
    }


    TResourceCacheController controller(resourceCache);
    controller.ApplySpecToCurrentStatus();
    controller.AddPodsStatusToCurrentStatus(pods);
    NYP::NClient::NApi::NProto::TResourceCacheStatus status1 = controller.CurrentResourceCache().Status();

    constexpr auto conditionTrue = NInfra::NPodAgent::API::EConditionStatus::EConditionStatus_TRUE;
    constexpr auto conditionFalse = NInfra::NPodAgent::API::EConditionStatus::EConditionStatus_FALSE;

    for (size_t i = 0; i < 2; ++i) {
        NYP::NClient::TPod& pod = pods[i];
        if (i == 0) {
            pod.MutableStatus()->mutable_agent()->mutable_pod_agent_payload()->mutable_status()->mutable_resource_cache()->mutable_layers(0)->mutable_ready()->set_status(conditionFalse);
            pod.MutableStatus()->mutable_agent()->mutable_pod_agent_payload()->mutable_status()->mutable_resource_cache()->mutable_layers(0)->mutable_failed()->set_status(conditionTrue);
        } else {
            pod.MutableStatus()->mutable_agent()->mutable_pod_agent_payload()->mutable_status()->mutable_resource_cache()->mutable_static_resources(0)->mutable_ready()->set_status(conditionFalse);
            pod.MutableStatus()->mutable_agent()->mutable_pod_agent_payload()->mutable_status()->mutable_resource_cache()->mutable_static_resources(0)->mutable_failed()->set_status(conditionTrue);
        }
    }

    // Sleep for checking that last_transition_time in TCondition calculated correctly (passed at least sleepTime)
    const TDuration sleepTime = TDuration::MilliSeconds(500);
    Sleep(sleepTime);

    controller.AddPodsStatusToCurrentStatus(pods);
    NYP::NClient::NApi::NProto::TResourceCacheStatus status2 = controller.CurrentResourceCache().Status();

    const TDuration maxTimeGap = TDuration::Minutes(30);
    const TDuration guaranteedTimeSpent = sleepTime * 0.9;

    UNIT_ASSERT(ChechIsConditionsLastTransitionTimeInGap(status1.all_ready(), status2.all_ready(), maxTimeGap));
    UNIT_ASSERT(ChechIsConditionsLastTransitionTimeExceedsGap(status1.all_ready(), status2.all_ready(), guaranteedTimeSpent));
    UNIT_ASSERT(ChechIsConditionsLastTransitionTimeEqual(status1.all_in_progress(), status2.all_in_progress()));
    UNIT_ASSERT(!ChechIsConditionsLastTransitionTimeExceedsGap(status1.all_in_progress(), status2.all_in_progress(), guaranteedTimeSpent));
    UNIT_ASSERT(ChechIsConditionsLastTransitionTimeInGap(status1.all_failed(), status2.all_failed(), maxTimeGap));
    UNIT_ASSERT(ChechIsConditionsLastTransitionTimeExceedsGap(status1.all_failed(), status2.all_failed(), guaranteedTimeSpent));

    UNIT_ASSERT(ChechIsConditionsLastTransitionTimeInGap(status1.latest_ready(), status2.latest_ready(), maxTimeGap));
    UNIT_ASSERT(ChechIsConditionsLastTransitionTimeExceedsGap(status1.latest_ready(), status2.latest_ready(), guaranteedTimeSpent));
    UNIT_ASSERT(ChechIsConditionsLastTransitionTimeEqual(status1.latest_in_progress(), status2.latest_in_progress()));
    UNIT_ASSERT(!ChechIsConditionsLastTransitionTimeExceedsGap(status1.latest_in_progress(), status2.latest_in_progress(), guaranteedTimeSpent));
    UNIT_ASSERT(ChechIsConditionsLastTransitionTimeInGap(status1.latest_failed(), status2.latest_failed(), maxTimeGap));
    UNIT_ASSERT(ChechIsConditionsLastTransitionTimeExceedsGap(status1.latest_failed(), status2.latest_failed(), guaranteedTimeSpent));

    for (size_t i = 0; i < 2; ++i) {
        UNIT_ASSERT(ChechIsConditionsLastTransitionTimeInGap(status1.cached_resource_status(i).revisions(0).ready(), status2.cached_resource_status(i).revisions(0).ready(), maxTimeGap));
        UNIT_ASSERT(ChechIsConditionsLastTransitionTimeExceedsGap(status1.cached_resource_status(i).revisions(0).ready(), status2.cached_resource_status(i).revisions(0).ready(), guaranteedTimeSpent));
        UNIT_ASSERT(ChechIsConditionsLastTransitionTimeEqual(status1.cached_resource_status(i).revisions(0).in_progress(), status2.cached_resource_status(i).revisions(0).in_progress()));
        UNIT_ASSERT(!ChechIsConditionsLastTransitionTimeExceedsGap(status1.cached_resource_status(i).revisions(0).in_progress(), status2.cached_resource_status(i).revisions(0).in_progress(), guaranteedTimeSpent));
        UNIT_ASSERT(ChechIsConditionsLastTransitionTimeInGap(status1.cached_resource_status(i).revisions(0).failed(), status2.cached_resource_status(i).revisions(0).failed(), maxTimeGap));
        UNIT_ASSERT(ChechIsConditionsLastTransitionTimeExceedsGap(status1.cached_resource_status(i).revisions(0).failed(), status2.cached_resource_status(i).revisions(0).failed(), guaranteedTimeSpent));
    }

    status1 = CorrectResourceCacheStatus(status1);
    status2 = CorrectResourceCacheStatus(status2);

    Cout << status1.Utf8DebugString() << Endl;
    Cout << status2.Utf8DebugString() << Endl;
}

}

} // namespace NInfra::NResourceCacheController::NResourceCacheControllerTest
