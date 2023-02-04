#pragma once

#include <ads/bsyeti/big_rt/lib/serializable_profile/serializable_profile.h>

#include <ads/bsyeti/big_rt/lib/serializable_profile/tests/profiles/proto/test.pb.h>
#include <ads/bsyeti/big_rt/lib/serializable_profile/tests/profiles/generated/test_traits.h>

namespace NBigRT::NTests {
    class TTestProfile : public TSerializableProfile<TTestProfileProtoTraits> {
        using TBase = TSerializableProfile<TTestProfileProtoTraits>;
    public:
        using TBase::TBase;

        TTestProfile(ui64 profileID) : TBase({profileID}) {}
    };
}
