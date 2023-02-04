#include <ads/bsyeti/libs/yt_storage/serializable_profile.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <contrib/libs/protobuf-mutator/src/mutator.h>

template<typename T>
std::optional<bool> CheckPatch(const T&, bool) {
    return std::nullopt;
}

std::optional<bool> CheckPatch(const NBigRT::NPrivate::TDualMutation& mutation, bool hasValue) {
    if (hasValue) {
        return mutation.Patch.has_value() && mutation.Patch->Size() > 0;
    }
    return !mutation.Patch.has_value() || (mutation.Patch.has_value() && mutation.Patch->Empty());
}

template<std::size_t I = 0, typename... Tp>
bool CheckPatches(const std::tuple<Tp...>& t, bool andOption, bool hasValue) {
    if constexpr (I != sizeof...(Tp)) {
        auto current = CheckPatch(std::get<I>(t), hasValue);
        auto others = CheckPatches<I + 1, Tp...>(t, andOption, hasValue);
        return andOption ? (current.value_or(true) && others) : (current.value_or(false) || others);
    }
    return true;
}

using namespace NBSYeti;

void Mutate(TUserProfile& userProfile) {
    protobuf_mutator::Mutator mutator;
    auto allPatched = false;
    while (!allPatched) {
        auto profile = userProfile.Mutable();
        mutator.Mutate(profile->MutableMain(), 1024);
        mutator.Mutate(profile->MutableUserItems(), 1024);
        mutator.Mutate(profile->MutableCounters(), 1024);
        mutator.Mutate(profile->MutableApplications(), 1024);
        mutator.Mutate(profile->MutableBanners(), 1024);
        mutator.Mutate(profile->MutableDmps(), 1024);
        mutator.Mutate(profile->MutableQueries(), 1024);
        mutator.Mutate(profile->MutableAura(), 1024);
        mutator.Mutate(profile->MutableDjProfiles(), 1024);
        auto mutation = userProfile.Flush();
        allPatched = CheckPatches(mutation.Value, true, true); // all patches non-empty
    }
}

TEST(TProfileTest, MergePatchImmediately) {
    TUserProfile profile("y1");
    auto mutation = profile.Flush();
    auto allEmpty = CheckPatches(mutation.Value, true, false); // all patches empty
    EXPECT_TRUE(allEmpty);
    Mutate(profile);
    profile.SetMergePatchImmediately(true);
    profile.MarkDirty();
    EXPECT_TRUE(profile.GetMergePatchImmediately());
    mutation = profile.Flush();
    EXPECT_FALSE(profile.GetMergePatchImmediately());
    EXPECT_FALSE(mutation.Empty());
    allEmpty = CheckPatches(mutation.Value, true, false);
    EXPECT_TRUE(allEmpty);
}

TEST(TProfileTest, MergePatchImmediatelySerializableProfile) {
    auto keywordInfo = BuildKeywordInfoFromProto();
    auto counterPacker = TCounterPackerBuilder::Build(BuildCounterInfoFromProto());

    TUserProfile userProfile("y1");
    auto mutation = userProfile.Flush();
    auto allEmpty = CheckPatches(mutation.Value, true, false);
    EXPECT_TRUE(allEmpty);
    Mutate(userProfile);
    TSerializableProfile::TRaw original = {.UserProfileState = userProfile.GetRemoteState()};
    TSerializableProfile profile("y1", keywordInfo, counterPacker, std::move(original));
    profile.SetMergePatchImmediately();
    profile.MarkDirty();
    mutation = profile.ComputeMutationAndApply();
    allEmpty = CheckPatches(mutation.Value, true, false);
    EXPECT_TRUE(allEmpty);
}
