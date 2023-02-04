#pragma once

#include <ads/bsyeti/libs/profile/profile.h>
#include <ads/bsyeti/libs/yt_storage/serializable_profile.h>

namespace NBSYeti {
    class IMutator {
    public:
        virtual void Mutate(TProfile& profile, ui64 timestamp, i64 seed) = 0;
        virtual TString GetProtoField() = 0;
        virtual void AssertEqual(const TProfile& left, const TProfile& right) = 0;
        virtual ~IMutator() = default;
    };

    using IMutatorPtr = TAtomicSharedPtr<IMutator>;

    TVector<IMutatorPtr> PrepareMutators(const NZstdFactory::TCodecFactory& factory, const THashSet<ui64>& excludedCodecs);
}
