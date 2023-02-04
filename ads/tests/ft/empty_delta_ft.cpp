#include "serializable_profile_test.h"

#include <ads/bsyeti/libs/yt_storage/load_profiles.h>
#include <ads/bsyeti/libs/yt_storage/save_profiles.h>
#include <ads/bsyeti/libs/yt_storage/serializable_profile.h>

using namespace NBSYeti;

TEST_F(TSerializableProfileTests, Mutation) {
    for (auto i = 0u; i < 1000; ++i) {
        TUserProfile userProfile("y1");

        TSerializableProfile::TRaw original = {.UserProfileState = userProfile.GetRemoteState()};
        TSerializableProfile profile("y1", KeywordInfo, CounterPacker, std::move(original));
        profile.TrySetCreationTime(Seed);

        Mutate(profile, {"", "CodecID", "Queries"}); // NB! Excluded Queries!

        auto delta = profile.ComputeMutationAndApply();
        profile.MarkClean();
        EXPECT_FALSE(delta.Empty());

        delta = profile.ComputeMutationAndApply();
        EXPECT_TRUE(delta.Empty());

        profile.MarkDirty();
        delta = profile.ComputeMutationAndApply();
        EXPECT_TRUE(delta.Empty());
    }
}
