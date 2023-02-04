#include <ads/bsyeti/libs/yt_storage/serializable_profile.h>

#include <library/cpp/testing/gtest/gtest.h>

using namespace NBSYeti;

class TUserProfileTest : public ::testing::Test {
protected:
    void SetUp() override {
        KeywordInfo = BuildKeywordInfoFromProto();
        CounterPacker = TCounterPackerBuilder::Build(BuildCounterInfoFromProto());
    }

    TKeywordInfo KeywordInfo;
    TCounterPacker CounterPacker;
};

TEST_F(TUserProfileTest, EmptyUserProfile) {
    TSerializableProfile::TRaw raw;
    raw.UserProfileState.emplace();
    TSerializableProfile profile("y1", KeywordInfo, CounterPacker, std::move(raw));

    EXPECT_NE(nullptr, profile.UserProfileData.Get());
}

TEST_F(TUserProfileTest, EmptySerializableProfile) {
    TSerializableProfile::TRaw raw;
    TSerializableProfile profile("y1", KeywordInfo, CounterPacker, std::move(raw));

    EXPECT_NE(nullptr, profile.UserProfileData.Get());
}

TEST_F(TUserProfileTest, DestroyedSerializableProfile) {
    TSerializableProfile::TRaw raw;
    TSerializableProfile profile("y1", KeywordInfo, CounterPacker, std::move(raw));

    Y_UNUSED(profile.DestroyAndDisown());

    EXPECT_THROW(profile.GetEncodingCodec(), yexception);
    EXPECT_THROW(profile.ComputeMutationAndApply(), yexception);
    EXPECT_THROW(profile.SetEncodingCodec(9), yexception);
    EXPECT_THROW(profile.SetMergePatchImmediately(), yexception);
    EXPECT_THROW(profile.DestroyAndDisown(), yexception);
}
