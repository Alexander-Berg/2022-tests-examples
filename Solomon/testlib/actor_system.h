#pragma once

#include <solomon/services/fetcher/lib/app_data.h>
#include <solomon/services/fetcher/lib/cluster/cluster.h>

#include <solomon/libs/cpp/actors/fwd.h>
#include <solomon/libs/cpp/actors/test_runtime/actor_runtime.h>

#include <util/generic/ptr.h>

namespace NSolomon::NTesting {
    struct TFetcherActorRuntime: public TTestActorRuntime {
        struct TNodeFactory final: TTestActorRuntimeBase::INodeFactory {
            struct TNode: TNodeDataBase {
                TNode(std::shared_ptr<void> appData)
                {
                    AppData0 = std::move(appData);
                }
            };

            TNodeFactory(std::shared_ptr<void> appData)
                : AppData{std::move(appData)}
            {
            }

            TIntrusivePtr<TNodeDataBase> CreateNode() override {
                return new TNode{AppData};
            }

            std::shared_ptr<void> AppData;
        };

        explicit TFetcherActorRuntime(bool useRealThreads = false);
        ~TFetcherActorRuntime();

        NFetcher::TAppData& AppData() {
            return *AppData_;
        }

    private:
        std::shared_ptr<NFetcher::TAppData> AppData_;
        NFetcher::TClusterInfo ClusterInfo_{"tests"};
    };

    inline THolder<TFetcherActorRuntime> MakeActorSystem(bool useRealThreads = false) {
        return MakeHolder<TFetcherActorRuntime>(useRealThreads);
    }

    NActors::IActor* CreateMockResolver();
}
