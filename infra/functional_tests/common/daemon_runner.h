#pragma once

#include <infra/yp_service_discovery/libs/client/client.h>
#include <infra/yp_service_discovery/libs/config/config.pb.h>
#include <infra/yp_service_discovery/libs/router_api/router_api.h>

#include <library/cpp/json/json_value.h>
#include <library/cpp/proto_config/load.h>
#include <library/cpp/threading/future/async.h>

#include <util/generic/string.h>

#include <util/stream/file.h>
#include <util/string/builder.h>
#include <util/string/cast.h>
#include <util/thread/pool.h>

namespace NYP::NServiceDiscovery {
    class TTestClient final: public TClient {
    public:
        TTestClient(const ui16 port)
            : TClient(TStringBuilder() << "0.0.0.0:" << ToString(port + 1), TDuration::Seconds(10))
            , HttpAddress_(TStringBuilder() << "http://" << "localhost:" << ToString(port))
        {
        }

        const NInfra::TRouterResponse HttpRequest(const TStringBuf handler) const;

        void Ping(NInfra::TRequestPtr<NApi::TReqPing>, NInfra::TReplyPtr<NApi::TRspPing>) override;
        void Shutdown(NInfra::TRequestPtr<NApi::TReqShutdown>, NInfra::TReplyPtr<NApi::TRspShutdown>) override;
        void Sensors(NInfra::TRequestPtr<NApi::TReqSensors>, NInfra::TReplyPtr<NApi::TRspSensors>) override;
        void DynamicSensors(NInfra::TRequestPtr<NApi::TReqSensors>, NInfra::TReplyPtr<NApi::TRspSensors> resp) override;

    private:
        const TString HttpAddress_;
    };

    class TDaemonRunner {
    public:
        TDaemonRunner() = default;
        ~TDaemonRunner();

        void Start(NProtoConfig::TLoadConfigOptions options);

        void Stop();

        NInfra::TRouterResponse Shutdown();

        NInfra::TRouterResponse Ping();

        NInfra::TRouterResponse Sensors();

        NInfra::TRouterResponse DynamicSensors();

        NInfra::TRouterResponse ResolveEndpointsJson(const TString& data);

        NInfra::TRouterResponse ResolveEndpoints(const TString& data);

        NInfra::TRouterResponse ResolveNodeJson(const TString& data);

        NInfra::TRouterResponse ResolveNode(const TString& data);

        NInfra::TRouterResponse ResolvePodsJson(const TString& data);

        NInfra::TRouterResponse ResolvePods(const TString& data);

    private:
        bool Running_ = false;
        ui16 Port_ = 0;
        TThreadPool Pool_;

        THolder<IApi> AdminClient_;
        THolder<IApi> DiscoveryClient_;
        NInfra::TRequestRouterPtr AdminRouter_;
        NInfra::TRequestRouterPtr DiscoveryRouter_;
    };

    void RunDaemon(NProtoConfig::TLoadConfigOptions options = NProtoConfig::TLoadConfigOptions());

    void StopDaemon();

    NInfra::TRouterResponse Ping();

    NInfra::TRouterResponse Sensors();

    NInfra::TRouterResponse DynamicSensors();

    NInfra::TRouterResponse ResolveEndpointsJson(const TString& data);

    NInfra::TRouterResponse ResolveEndpoints(const TString& data);

    NInfra::TRouterResponse ResolveNodeJson(const TString& data);

    NInfra::TRouterResponse ResolveNode(const TString& data);

    NInfra::TRouterResponse ResolvePodsJson(const TString& data);

    NInfra::TRouterResponse ResolvePods(const TString& data);

    NJson::TJsonValue::TArray GetSensors(const NInfra::TRouterResponse sensorsResponse = Sensors());

    NJson::TJsonValue::TArray GetDynamicSensors();
}
