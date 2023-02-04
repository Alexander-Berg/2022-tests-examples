#pragma once

#include <solomon/libs/cpp/actors/test_runtime/actor_runtime.h>

#include <library/cpp/actors/interconnect/interconnect.h>

#include <library/cpp/testing/common/network.h>

namespace NSolomon::NTesting {
    struct TCoordinationActorRuntime: public TTestActorRuntime {
        explicit TCoordinationActorRuntime(ui32 nodeCount = 3, bool useThreads = true)
            : TTestActorRuntime{nodeCount, useThreads, /* enableScheduling = */ false}
        {
            InitIc();
        }

    private:
        void InitIc();
        void InitNodeImpl(TNodeDataBase* node, size_t num) override;

    private:
        TIntrusivePtr<NActors::TTableNameserverSetup> IcTable_;
        TVector<::NTesting::TPortHolder> Ports_;
    };
} // namespace NSolomon::NTesting
