#include "common.h"
#include "daemon_runner.h"

#include <infra/yp_service_discovery/libs/main/main.h>

#include <library/cpp/monlib/encode/json/json.h>
#include <library/cpp/monlib/encode/spack/spack_v1.h>
#include <library/cpp/neh/neh.h>
#include <library/cpp/protobuf/json/json2proto.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/tests_data.h>

namespace NYP::NServiceDiscovery {
    const NInfra::TRouterResponse TTestClient::HttpRequest(const TStringBuf handler) const {
        NNeh::TMessage request(TStringBuilder() << HttpAddress_ << handler, TString());
        NNeh::THandleRef handle = NNeh::Request(request, nullptr);
        NNeh::TResponseRef response = handle->Wait();
        if (!response->IsError()) {
            NInfra::TAttributes attrs;
            for (const auto& header : response->Headers) {
                attrs.emplace(header.Name(), header.Value());
            }
            return {response->Data, attrs};
        }
        return {response->GetErrorText(), {}};
    }

    void TTestClient::Ping(NInfra::TRequestPtr<NApi::TReqPing>, NInfra::TReplyPtr<NApi::TRspPing> resp) {
        const TString response = HttpRequest("/ping").Content;

        NProtobufJson::TJson2ProtoConfig json2ProtoConfig = NProtobufJson::TJson2ProtoConfig();
        json2ProtoConfig.AllowUnknownFields = false;
        resp->Set(NProtobufJson::Json2Proto<NApi::TRspPing>(response, json2ProtoConfig));
    }

    void TTestClient::Shutdown(NInfra::TRequestPtr<NApi::TReqShutdown>, NInfra::TReplyPtr<NApi::TRspShutdown>) {
        HttpRequest("/shutdown");
    }

    void TTestClient::Sensors(NInfra::TRequestPtr<NApi::TReqSensors>, NInfra::TReplyPtr<NApi::TRspSensors> resp) {
        const NInfra::TRouterResponse routerResponse = HttpRequest("/sensors");
        NApi::TRspSensors response;
        response.SetData(routerResponse.Content);
        resp->Set(response);
        for (const auto& [name, value] : routerResponse.Attributes) {
            resp->SetAttribute(name, value);
        }
    }

    void TTestClient::DynamicSensors(NInfra::TRequestPtr<NApi::TReqSensors>, NInfra::TReplyPtr<NApi::TRspSensors> resp) {
        const NInfra::TRouterResponse routerResponse = HttpRequest("/dynamic_sensors");
        NApi::TRspSensors response;
        response.SetData(routerResponse.Content);
        resp->Set(response);
        for (const auto& [name, value] : routerResponse.Attributes) {
            resp->SetAttribute(name, value);
        }
    }

    TDaemonRunner::~TDaemonRunner() {
        Stop();
    }

    void TDaemonRunner::Start(NProtoConfig::TLoadConfigOptions options) {
        if (Running_) {
            Y_VERIFY(Running_ == false);
            return;
        }

        TPortManager portManager;

        Port_ = portManager.GetPort();

        if (options.Path.empty()) {
            options.Resource = "/yp_service_discovery/proto_config.json";
        }

        options.Overrides = {
            TStringBuilder() << "DiscoveryHttpServiceConfig.Port=" << Port_,
            TStringBuilder() << "GrpcServiceConfig.Port=" << Port_ + 1,
            TStringBuilder() << "AdminHttpServiceConfig.Port=" << Port_ + 2,
            TStringBuilder() << "LoggerConfig.Path=" << GetOutputPath() / "eventlog",
            TStringBuilder() << "EndpointsReplicaLoggerConfig.Path=" << GetOutputPath() / "replica-endpoints-eventlog",
            TStringBuilder() << "PodsReplicaLoggerConfig.Path=" << GetOutputPath() / "replica-pods-eventlog",
            TStringBuilder() << "NodeReplicaLoggerConfig.Path=" << GetOutputPath() / "replica-node-eventlog",
            TStringBuilder() << "ConfigUpdatesLoggerConfig.Path=" << GetOutputPath() / "config-updates-eventlog",
            TStringBuilder() << "AdminHttpServiceLoggerConfig.Path=" << GetOutputPath() / "http-admin-service-eventlog",
            TStringBuilder() << "DiscoveryHttpServiceLoggerConfig.Path=" << GetOutputPath() / "http-discovery-service-eventlog",
            TStringBuilder() << "YPEndpointReplicaConfig.StorageConfig.ValidationConfig.MaxAge=0",
            TStringBuilder() << "YPEndpointReplicaConfig.StorageConfig.AgeAlertThreshold=" << "1us",
            TStringBuilder() << "YPPodReplicaConfig.StorageConfig.ValidationConfig.MaxAge=0",
            TStringBuilder() << "YPPodReplicaConfig.StorageConfig.AgeAlertThreshold=" << "1us",
            TStringBuilder() << "YPNodeReplicaConfig.StorageConfig.ValidationConfig.MaxAge=0",
            TStringBuilder() << "YPNodeReplicaConfig.StorageConfig.AgeAlertThreshold=" << "1us",
            TStringBuilder() << "AbortWatchDogConfig.Timeout=" << "15m",
        };

        Pool_.Start(1);
        NThreading::Async([&options] {
            RunMain(options);
        },
                          Pool_);

        DiscoveryClient_ = MakeHolder<TTestClient>(Port_);
        DiscoveryRouter_ = CreateDiscoveryRouter(*DiscoveryClient_);
        AdminClient_ = MakeHolder<TTestClient>(Port_ + 2);
        AdminRouter_ = CreateAdminRouter(*AdminClient_, NProtoConfig::LoadWithOptions<TConfig>(options));

        while (Ping().Content != EXPECTED_PING_RESPONSE) {
            Sleep(TDuration::MilliSeconds(50));
        }

        Running_ = true;
    }

    void TDaemonRunner::Stop() {
        if (!Running_) {
            return;
        }

        Shutdown();

        Pool_.Stop();

        Running_ = false;
    }

    NInfra::TRouterResponse TDaemonRunner::Shutdown() {
        return AdminRouter_->Handle(TStringBuf("/shutdown"), {}, {});
    }

    NInfra::TRouterResponse TDaemonRunner::Ping() {
        try {
            return DiscoveryRouter_->Handle(TStringBuf("/ping"), {}, {});
        } catch (const yexception& ex) {
            return {ex.what(), {}};
        }
    }

    NInfra::TRouterResponse TDaemonRunner::Sensors() {
        return AdminRouter_->Handle(TStringBuf("/sensors"), {}, {});
    }

    NInfra::TRouterResponse TDaemonRunner::DynamicSensors() {
        return AdminRouter_->Handle(TStringBuf("/dynamic_sensors"), {}, {});
    }

    NInfra::TRouterResponse TDaemonRunner::ResolveEndpointsJson(const TString& data) {
        return DiscoveryRouter_->Handle(TStringBuf("/resolve_endpoints/json"), data, {});
    }

    NInfra::TRouterResponse TDaemonRunner::ResolveEndpoints(const TString& data) {
        return DiscoveryRouter_->Handle(TStringBuf("/resolve_endpoints"), data, {});
    }

    NInfra::TRouterResponse TDaemonRunner::ResolveNodeJson(const TString& data) {
        return DiscoveryRouter_->Handle(TStringBuf("/resolve_node/json"), data, {});
    }

    NInfra::TRouterResponse TDaemonRunner::ResolveNode(const TString& data) {
        return DiscoveryRouter_->Handle(TStringBuf("/resolve_node"), data, {});
    }

    NInfra::TRouterResponse TDaemonRunner::ResolvePodsJson(const TString& data) {
        return DiscoveryRouter_->Handle(TStringBuf("/resolve_pods/json"), data, {});
    }

    NInfra::TRouterResponse TDaemonRunner::ResolvePods(const TString& data) {
        return DiscoveryRouter_->Handle(TStringBuf("/resolve_pods"), data, {});
    }

    TDaemonRunner* DaemonRunnerFactory() {
        return SingletonWithPriority<TDaemonRunner, 1'000'000'000>();
    }

    void RunDaemon(NProtoConfig::TLoadConfigOptions options) {
        DaemonRunnerFactory()->Start(std::move(options));
    }

    void StopDaemon() {
        DaemonRunnerFactory()->Stop();
    }

    NInfra::TRouterResponse Ping() {
        return DaemonRunnerFactory()->Ping();
    }

    NInfra::TRouterResponse Sensors() {
        return DaemonRunnerFactory()->Sensors();
    }

    NInfra::TRouterResponse DynamicSensors() {
        return DaemonRunnerFactory()->DynamicSensors();
    }

    NInfra::TRouterResponse ResolveEndpointsJson(const TString& data) {
        return DaemonRunnerFactory()->ResolveEndpointsJson(data);
    }

    NInfra::TRouterResponse ResolveEndpoints(const TString& data) {
        return DaemonRunnerFactory()->ResolveEndpoints(data);
    }

    NInfra::TRouterResponse ResolveNodeJson(const TString& data) {
        return DaemonRunnerFactory()->ResolveNodeJson(data);
    }

    NInfra::TRouterResponse ResolveNode(const TString& data) {
        return DaemonRunnerFactory()->ResolveNode(data);
    }

    NInfra::TRouterResponse ResolvePodsJson(const TString& data) {
        return DaemonRunnerFactory()->ResolvePodsJson(data);
    }

    NInfra::TRouterResponse ResolvePods(const TString& data) {
        return DaemonRunnerFactory()->ResolvePods(data);
    }

    NJson::TJsonValue::TArray GetSensors(const NInfra::TRouterResponse sensorsResponse) {
        UNIT_ASSERT(AnyOf(sensorsResponse.Attributes, [](const auto& it){ return it.first == TStringBuf("Content-Type") && it.second == TStringBuf("application/x-solomon-spack"); }));
        TStringStream spackStream(sensorsResponse.Content);
        TStringStream jsonStream;
        auto encoder = NMonitoring::EncoderJson(&jsonStream);
        NMonitoring::DecodeSpackV1(&spackStream, encoder.Get());
        const NJson::TJsonValue response = NJson::ReadJsonTree(&jsonStream, /* throwOnError */ true);
        return response["sensors"].GetArray();
    }

    NJson::TJsonValue::TArray GetDynamicSensors() {
        return GetSensors(DynamicSensors());
    }
}
