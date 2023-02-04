#pragma once

#include <balancer/kernel/stats/manager.h>
#include <library/cpp/json/json_value.h>

namespace NSrvKernel::NTesting {

    struct TStatsFixtureOpts {
        ui32 Workers = 1;
        ui32 SpecWorkers = 0;
    };

    class TStatsFixture : public TMoveOnly {
    public:
        TStatsFixture(TStatsFixtureOpts opts = {});

        void FreezeAndInit() {
            Allocator_->Freeze();
        }

        TSharedAllocator& Allocator() {
            return *Allocator_;
        }

        TSharedStatsManager& Manager() {
            return Manager_;
        }

        THashMap<TString, NJson::TJsonValue> Unistat();

    private:
        THolder<TSharedAllocator> Allocator_{MakeHolder<TSharedAllocator>()};
        TSharedStatsManager Manager_{*Allocator_};
    };
}
