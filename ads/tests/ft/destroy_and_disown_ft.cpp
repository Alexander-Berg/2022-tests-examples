#include "serializable_profile_test.h"

#include <ads/bsyeti/libs/yt_storage/load_profiles.h>
#include <ads/bsyeti/libs/yt_storage/save_profiles.h>
#include <ads/bsyeti/libs/yt_storage/serializable_profile.h>

using namespace NBSYeti;

TEST_F(TSerializableProfileTests, RestoreUserProfileFromProtosFixedCodec) {
    for (auto i = 0u; i < 1; ++i) {
        TUserProfile empty("y1");
        auto row = empty.Release();
        NBigRT::TCodecID<TBuzzardCodecsGetter> codecId(TBuzzardCodecsGetter::ToStringCodecID(2));
        row.SetCodecID(codecId.ToString());

        TUserProfile userProfile("y1", std::move(row));
        auto codecId1 = userProfile.GetRemoteState().CodecID;
        auto codecId2 = userProfile.GetCurrentCodecID();
        EXPECT_EQ(codecId1.ToString(), codecId2);
        EXPECT_TRUE(codecId.Equals(codecId1));

        TSerializableProfile::TRaw original = {.UserProfileState = userProfile.GetRemoteState()};
        TSerializableProfile profile("y1", KeywordInfo, CounterPacker, std::move(original));
        profile.TrySetCreationTime(Seed);
        auto encodingCodec = profile.GetEncodingCodec();

        Mutate(profile, {"", "CodecID"});

        profile.ComputeMutationAndApply();

        auto proto = profile.GetFullPublicProto();

        auto state = profile.DestroyAndDisown();
        EXPECT_EQ(false, state.UserProfileState.has_value());
        EXPECT_EQ(nullptr, state.UserProfile.Get());
        EXPECT_NE(nullptr, state.Protos.Get());

        TSerializableProfile recreated("y1", KeywordInfo, CounterPacker, std::move(state));
        auto recreatedCodec = recreated.GetEncodingCodec();
        EXPECT_EQ(encodingCodec, recreatedCodec);
        EXPECT_NE(nullptr, recreated.UserProfileData.Get());

        EXPECT_THAT(proto, NGTest::EqualsProto(recreated.GetFullPublicProto()));
    }
}

TEST_F(TSerializableProfileTests, RestoreUserProfileFromRowFixedCodec) {
    for (auto i = 0u; i < 1000; ++i) {
        TUserProfile empty("y1");
        auto row = empty.Release();
        NBigRT::TCodecID<TBuzzardCodecsGetter> codecId(TBuzzardCodecsGetter::ToStringCodecID(2));
        row.SetCodecID(codecId.ToString());

        TUserProfile userProfile("y1", std::move(row));
        auto codecId1 = userProfile.GetRemoteState().CodecID;
        auto codecId2 = userProfile.GetCurrentCodecID();
        EXPECT_EQ(codecId1.ToString(), codecId2);
        EXPECT_TRUE(codecId.Equals(codecId1));

        TSerializableProfile::TRaw original = {.UserProfileState = userProfile.GetRemoteState()};
        TSerializableProfile profile("y1", KeywordInfo, CounterPacker, std::move(original));
        profile.TrySetCreationTime(Seed);
        auto encodingCodec = profile.GetEncodingCodec();

        Mutate(profile, {"", "CodecID"});

        profile.ComputeMutationAndApply();

        auto proto = profile.GetFullPublicProto();

        auto state = profile.DestroyAndDisown();
        EXPECT_EQ(false, state.UserProfileState.has_value());
        EXPECT_EQ(nullptr, state.UserProfile.Get());
        EXPECT_NE(nullptr, state.Protos.Get());

        state.ResetProtos();
        EXPECT_EQ(true, state.UserProfileState.has_value());
        EXPECT_EQ(nullptr, state.Protos.Get());
        TSerializableProfile recreated("y1", KeywordInfo, CounterPacker, std::move(state));
        auto recreatedCodec = recreated.GetEncodingCodec();
        EXPECT_EQ(encodingCodec, recreatedCodec);
        EXPECT_NE(nullptr, recreated.UserProfileData.Get());

        EXPECT_THAT(proto, NGTest::EqualsProto(recreated.GetFullPublicProto()));
    }
}

TEST_F(TSerializableProfileTests, RestoreUserProfileFromProtos) {
    for (auto i = 0u; i < 1000; ++i) {
        TUserProfile userProfile("y1");

        TSerializableProfile::TRaw original = {.UserProfileState = userProfile.GetRemoteState()};
        TSerializableProfile profile("y1", KeywordInfo, CounterPacker, std::move(original));
        profile.TrySetCreationTime(Seed);

        Mutate(profile, {""});

        profile.ComputeMutationAndApply();

        auto codec = profile.GetEncodingCodec();

        auto proto = profile.GetFullPublicProto();

        auto state = profile.DestroyAndDisown();
        EXPECT_EQ(false, state.UserProfileState.has_value());
        EXPECT_EQ(nullptr, state.UserProfile.Get());
        EXPECT_NE(nullptr, state.Protos.Get());

        TSerializableProfile recreated("y1", KeywordInfo, CounterPacker, std::move(state));
        EXPECT_NE(nullptr, recreated.UserProfileData.Get());
        EXPECT_EQ(codec, recreated.GetEncodingCodec());

        EXPECT_THAT(proto, NGTest::EqualsProto(recreated.GetFullPublicProto()));

        {
            TSerializableProfile::TRaw state = {.UserProfileState = recreated.UserProfileData->GetRemoteState()};
            TSerializableProfile profile("y1", KeywordInfo, CounterPacker, std::move(state));
            EXPECT_THAT(proto, NGTest::EqualsProto(profile.GetFullPublicProto()));
        }
    }
}

TEST_F(TSerializableProfileTests, RestoreUserProfileFromRow) {
    for (auto i = 0u; i < 1000; ++i) {
        TUserProfile userProfile("y1");

        TSerializableProfile::TRaw original = {.UserProfileState = userProfile.GetRemoteState()};
        TSerializableProfile profile("y1", KeywordInfo, CounterPacker, std::move(original));
        profile.TrySetCreationTime(Seed);

        Mutate(profile, {""});

        profile.ComputeMutationAndApply();

        auto codec = profile.GetEncodingCodec();

        auto proto = profile.GetFullPublicProto();

        auto state = profile.DestroyAndDisown();
        EXPECT_EQ(false, state.UserProfileState.has_value());
        EXPECT_EQ(nullptr, state.UserProfile.Get());
        EXPECT_NE(nullptr, state.Protos.Get());

        state.ResetProtos();
        EXPECT_EQ(true, state.UserProfileState.has_value());
        EXPECT_EQ(nullptr, state.Protos.Get());
        TSerializableProfile recreated("y1", KeywordInfo, CounterPacker, std::move(state));
        EXPECT_NE(nullptr, recreated.UserProfileData.Get());
        EXPECT_EQ(codec, recreated.GetEncodingCodec());

        EXPECT_THAT(proto, NGTest::EqualsProto(recreated.GetFullPublicProto()));

        {
            TSerializableProfile::TRaw state = {.UserProfileState = recreated.UserProfileData->GetRemoteState()};
            TSerializableProfile profile("y1", KeywordInfo, CounterPacker, std::move(state));
            EXPECT_THAT(proto, NGTest::EqualsProto(profile.GetFullPublicProto()));
        }
    }
}
