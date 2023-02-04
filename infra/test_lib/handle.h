#pragma once

#include "test_functions.h"

#include <infra/libs/service_iface/reply.h>
#include <infra/libs/service_iface/request.h>
#include <infra/libs/service_iface/router.h>
#include <infra/libs/service_iface/str_iface.h>

#include <infra/pod_agent/libs/client/client.h>
#include <infra/pod_agent/libs/multi_unistat/multi_unistat.h>

#include <library/cpp/protobuf/json/json2proto.h>

#include <yp/yp_proto/yp/client/api/proto/pod_agent.pb.h>

namespace NInfra::NPodAgent::NDaemonTest  {

class THandle {
public:
    THandle(const TString& address, const TDuration timeout):
        Client_(address, timeout)
    {
    }

    TRouterResponse Config() {
        TString response;
        TAttributes replyAttributes;
        Client_.Config(RequestPtr<TEmptyRequest<TReqConfig>>("/config", "", TAttributes()), ReplyPtr<TRawDataReply<TRspConfig>>(response, replyAttributes));
        return {response, replyAttributes};
    }

    void Shutdown() {
        TString response;
        TAttributes replyAttributes;
        Client_.Shutdown(RequestPtr<TEmptyRequest<TReqShutdown>>("/shutdown", "", TAttributes()), ReplyPtr<TEmptyReply<TRspShutdown>>(response, replyAttributes));
    }

    void SetLogLevel(const TReqSetLogLevel& logLevel) {
        TString response;
        TAttributes replyAttributes;
        Client_.SetLogLevel(RequestPtr<TProtoRequest<TReqSetLogLevel>>("/set_log_level", logLevel, TAttributes()), ReplyPtr<TEmptyReply<TRspSetLogLevel>>(response, replyAttributes));
    }

    API::TPodAgentStatus UpdatePodAgentRequest(const API::TPodAgentRequest& spec) {
        API::TPodAgentStatus response;
        TAttributes replyAttributes;
        Client_.UpdatePodAgentRequest(RequestPtr<TProtoRequest<API::TPodAgentRequest>>("/update_pod_request", spec, TAttributes()), ReplyPtr<TProtoReply<API::TPodAgentStatus>>(response, replyAttributes));

        return response;
    }

    API::TPodAgentStatus GetPodAgentStatus() {
        API::TPodAgentStatus response;
        TAttributes replyAttributes;
        Client_.GetPodAgentStatus(RequestPtr<TEmptyRequest<TReqGetPodAgentStatus>>("/get_pod_agent_status", "", TAttributes()), ReplyPtr<TProtoReply<API::TPodAgentStatus>>(response, replyAttributes));

        return response;
    }

    TString Ping() {
        TString response;
        TAttributes replyAttributes;
        Client_.Ping(RequestPtr<TEmptyRequest<TReqPing>>("/ping", "", TAttributes()), ReplyPtr<TRawDataReply<TRspPing>>(response, replyAttributes));

        return response;
    }

    TString Sensors(const TMultiUnistat::ESignalPriority signalPriorityLevel = TMultiUnistat::ESignalPriority::USER_INFO) {
        TString response;
        TAttributes replyAttributes;
        TReqSensors* reqSensors = new TReqSensors();
        reqSensors->SetPriorityLevel(ToString(signalPriorityLevel));
        Client_.Sensors(
            RequestPtr<TProtoRequest<TReqSensors>>("/sensors", *reqSensors, TAttributes())
            , ReplyPtr<TRawDataReply<TRspSensors>>(response, replyAttributes)
        );
        return response;
    }

    TString UserSensors(const TMultiUnistat::ESignalPriority signalPriorityLevel = TMultiUnistat::ESignalPriority::USER_INFO) {
        TString response;
        TAttributes replyAttributes;
        TReqSensors* reqUserSensors = new TReqSensors();
        reqUserSensors->SetPriorityLevel(ToString(signalPriorityLevel));
        Client_.UserSensors(
            RequestPtr<TProtoRequest<TReqSensors>>("/user_sensors", *reqUserSensors, TAttributes())
            , ReplyPtr<TRawDataReply<TRspSensors>>(response, replyAttributes)
        );
        return response;
    }

    TString Version() {
        TString response;
        TAttributes replyAttributes;
        Client_.Version(RequestPtr<TEmptyRequest<TReqVersion>>("/version", "", TAttributes()), ReplyPtr<TRawDataReply<TRspVersion>>(response, replyAttributes));

        return response;
    }

    void ReopenLog() {
        TString response;
        TAttributes replyAttributes;
        Client_.ReopenLog(RequestPtr<TEmptyRequest<TReqReopenLog>>("/reopen_log", "", TAttributes()), ReplyPtr<TEmptyReply<TRspReopenLog>>(response, replyAttributes));
    }

private:
    TClient Client_;
};

} // namespace NInfra::NPodAgent::NDaemonTest
