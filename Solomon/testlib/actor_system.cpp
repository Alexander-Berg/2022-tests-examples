#include "actor_system.h"

#include <solomon/services/fetcher/lib/app_data.h>
#include <solomon/services/fetcher/lib/dns/continuous_resolver.h>

#include <library/cpp/actors/core/actor_bootstrapped.h>
#include <library/cpp/actors/core/executor_pool_basic.h>
#include <library/cpp/actors/core/hfunc.h>
#include <library/cpp/actors/core/scheduler_basic.h>
#include <library/cpp/ipv6_address/ipv6_address.h>

using namespace NActors;
using namespace NSolomon::NFetcher;

namespace NSolomon::NTesting {
    /// Mock DNS continuous resolver that will resolve all given addresses into localhost
    struct TMockResolver: TActor<TMockResolver> {
        TMockResolver()
            : TActor<TMockResolver>{&TThis::StateFunc}
        {
        }

        STFUNC(StateFunc) {
            switch (ev->GetTypeRewrite()) {
                HFunc(TEvStartResolving, Handle);
            }
        }

        void Handle(const TEvStartResolving::TPtr& ev, const TActorContext&) {
            Send(ev->Sender, new TEvHostResolveOk{ev->Get()->Hostname, Get1(), EDc::UNKNOWN});
        }
    };

    TFetcherActorRuntime::TFetcherActorRuntime(bool useRealThreads)
        : TTestActorRuntime{1, useRealThreads, /* enableScheduling = */ false}
    {
        AppData_ = std::make_shared<NFetcher::TAppData>();

        auto httpClient = CreateCurlClient({.WorkerThreads = 1, .HandlerThreads = 1}, *AppData_->Metrics);

        AppData_->SetDataHttpClient(httpClient);
        AppData_->SetResolverHttpClient(httpClient);

        AppData_->SetResolverFactory(CreateHostResolverFactory(THostResolverFactoryConfig{
            .Registry = *AppData_->Metrics,
            .ClusterInfo = ClusterInfo_,
            .DnsClient = AppData_->DnsClient,
            .HttpClient = AppData_->ResolverHttpClient(),
            .YpToken = "foo",
            .InstanceGroupClient = nullptr,
        }));

        NodeFactory.Reset(new TNodeFactory{AppData_});
    }

    TFetcherActorRuntime::~TFetcherActorRuntime() {
        if (AppData_) {
            AppData_->Deinit();
        }
    }

    IActor* CreateMockResolver() {
        return new TMockResolver;
    }
}
