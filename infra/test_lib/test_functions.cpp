#include <google/protobuf/timestamp.pb.h>
#include "test_functions.h"

#include <util/generic/vector.h>

namespace NInfra::NPodAgent::NDaemonTest  {

NProtobufJson::TJson2ProtoConfig GetSpecHolderJson2ProtoConfig() {
    NProtobufJson::TJson2ProtoConfig json2ProtoConfig;
    json2ProtoConfig.AllowUnknownFields = false;
    return json2ProtoConfig;
}

void CorrectTimestamp(google::protobuf::Timestamp* timestamp) {
    timestamp->set_seconds(42);
    timestamp->set_nanos(42);
}

template<typename T>
void CorrectObjectConditionTimestamp(T& object) {
    CorrectTimestamp(object.mutable_ready()->mutable_last_transition_time());
    CorrectTimestamp(object.mutable_in_progress()->mutable_last_transition_time());
    CorrectTimestamp(object.mutable_failed()->mutable_last_transition_time());
}

template<bool start, bool death, bool send, typename T>
void CorrectObjectAttemptTimestamp(T& attempt) {
    // "or" for flaky attempts
    if (attempt.has_current() || attempt.has_last() || attempt.has_last_failed()) {
        if constexpr (start) {
            CorrectTimestamp(attempt.mutable_current()->mutable_start_time());
            CorrectTimestamp(attempt.mutable_last()->mutable_start_time());
            CorrectTimestamp(attempt.mutable_last_failed()->mutable_start_time());
        }

        if constexpr (death) {
            CorrectTimestamp(attempt.mutable_current()->mutable_death_time());
            CorrectTimestamp(attempt.mutable_last()->mutable_death_time());
            CorrectTimestamp(attempt.mutable_last_failed()->mutable_death_time());
        }

        if constexpr (send) {
            CorrectTimestamp(attempt.mutable_current()->mutable_send_time());
            CorrectTimestamp(attempt.mutable_last()->mutable_send_time());
            CorrectTimestamp(attempt.mutable_last_failed()->mutable_send_time());
        }
    }
}

template<typename T>
void CorrectContainerName(T& status) {
    const TString containerName = status.container_name();
    if (!containerName.empty()) {
        status.set_container_name(containerName.substr(containerName.find('_', containerName.find('_') + 1) + 1)); // from 2nd '_'
    }
}

template<bool container, bool http, bool tcp, bool unixSignal, typename T>
void CorrectObjectHookStatus(T& hook) {
    if constexpr (container) {
        if (hook.has_container_status()) {
            CorrectContainerName(*hook.mutable_container_status());
            CorrectObjectAttemptTimestamp<true, true, false>(*hook.mutable_container_status());
        }
    }

    if constexpr (http) {
        if (hook.has_http_get_status()) {
            CorrectObjectAttemptTimestamp<true, true, false>(*hook.mutable_http_get_status());
        }
    }

    if constexpr (tcp) {
        if (hook.has_tcp_check_status()) {
            CorrectObjectAttemptTimestamp<true, true, false>(*hook.mutable_tcp_check_status());
        }
    }

    if constexpr (unixSignal) {
        if (hook.has_unix_signal_status()) {
            CorrectObjectAttemptTimestamp<false, false, true>(*hook.mutable_unix_signal_status());
        }
    }
}

void CorrectObjectFailedMessage(API::TCondition* condition) {
    const TVector<TString> specialFailedMessages = {
        "Busy(15):RemoveLayer"
        , "LayerAlreadyExists:(Layer already exists)"
        , "VolumeAlreadyExists(12):CreateVolume"
        , "VolumeNotReady:(Volume"
        , "SocketTimeout:(recv: Resource temporarily unavailable)"
    };

    for (const TString& specialFailedMessage : specialFailedMessages) {
        if (condition->message().find(specialFailedMessage) != TString::npos) {
            condition->set_message("");
            break;
        }
    }
}

template<typename T>
void CorrectFailCounter(T& object) {
    object.set_fail_counter(42);
}

API::TPodAgentStatus CorrectPodAgentStatus(const API::TPodAgentStatus& status) {
    if (!status.IsInitialized()) {
        return status;
    }

    API::TPodAgentStatus result = status;

    result.set_host_timestamp(42);
    CorrectObjectConditionTimestamp(result);
    CorrectObjectFailedMessage(result.mutable_failed());

    for (auto& obj: *result.mutable_resource_cache()->mutable_static_resources()) {
        CorrectObjectConditionTimestamp(obj);
        CorrectFailCounter(obj);
        CorrectObjectFailedMessage(obj.mutable_failed());
    }
    for (auto& obj: *result.mutable_resource_gang()->mutable_static_resources()) {
        CorrectObjectConditionTimestamp(obj);
        CorrectFailCounter(obj);
        CorrectObjectFailedMessage(obj.mutable_failed());
    }

    for (auto& obj: *result.mutable_resource_cache()->mutable_layers()) {
        CorrectObjectConditionTimestamp(obj);
        CorrectFailCounter(obj);
        CorrectObjectFailedMessage(obj.mutable_failed());
    }
    for (auto& obj: *result.mutable_resource_gang()->mutable_layers()) {
        CorrectObjectConditionTimestamp(obj);
        CorrectFailCounter(obj);
        CorrectObjectFailedMessage(obj.mutable_failed());
    }

    for (auto& obj: *result.mutable_boxes()) {
        CorrectObjectConditionTimestamp(obj);
        for (auto& container: *obj.mutable_inits()) {
            CorrectContainerName(container);
            CorrectObjectAttemptTimestamp<true, true, false>(container);
        }

        CorrectContainerName(obj);
        CorrectFailCounter(obj);
        CorrectObjectFailedMessage(obj.mutable_failed());
    }

    for (auto& obj: *result.mutable_volumes()) {
        CorrectObjectConditionTimestamp(obj);
        CorrectFailCounter(obj);
        CorrectObjectFailedMessage(obj.mutable_failed());
    }

    for (auto& obj: *result.mutable_workloads()) {
        CorrectObjectConditionTimestamp(obj);
        for (auto& container: *obj.mutable_init()) {
            CorrectContainerName(container);
            CorrectObjectAttemptTimestamp<true, true, false>(container);
        }

        CorrectContainerName(*obj.mutable_start());
        CorrectObjectAttemptTimestamp<true, true, false>(*obj.mutable_start());

        CorrectObjectHookStatus<true, true, true, false>(*obj.mutable_readiness_status());
        CorrectObjectHookStatus<true, true, true, false>(*obj.mutable_liveness_status());

        CorrectObjectHookStatus<true, true, false, true>(*obj.mutable_stop_status());
        CorrectObjectHookStatus<true, true, false, false>(*obj.mutable_destroy_status());
    }

    return result;
}

bool CheckAllObjectsReady(const API::TPodAgentStatus& status) {
    bool ready = true;

    for (const auto& object : status.resource_gang().layers()) {
        ready &= object.ready().status() == API::EConditionStatus_TRUE;
    }
    for (const auto& object : status.resource_gang().static_resources()) {
        ready &= object.ready().status() == API::EConditionStatus_TRUE;
    }

    for (const auto& object : status.resource_cache().layers()) {
        ready &= object.ready().status() == API::EConditionStatus_TRUE;
    }
    for (const auto& object : status.resource_cache().static_resources()) {
        ready &= object.ready().status() == API::EConditionStatus_TRUE;
    }

    for (const auto& object : status.volumes()) {
        ready &= object.ready().status() == API::EConditionStatus_TRUE;
    }

    for (const auto& object : status.boxes()) {
        ready &= object.ready().status() == API::EConditionStatus_TRUE;
    }

    for (const auto& object : status.workloads()) {
        ready &= object.ready().status() == API::EConditionStatus_TRUE;
    }

    return ready;
}

void DestroyContainersWithPrefix(TPortoClientPtr porto, const TString& prefix) {
    auto containers = porto->List(prefix + "*").Success();
    for (const auto& container : containers) {
        porto->Destroy(container).Success();
    }
}

} // namespace NInfra::NPodAgent::NDaemonTest
