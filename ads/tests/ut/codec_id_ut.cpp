#include <ads/bsyeti/big_rt/lib/serializable_profile/mutable_row.h>
#include <ads/bsyeti/libs/profile/codecs_getter.h>

#include <library/cpp/testing/gtest/gtest.h>

using TCodecID = NBigRT::TCodecID<NBSYeti::TBuzzardCodecsGetter>;

TEST(TCodecIDTest, DefaultCodecIdEmpty) {
    TCodecID codecId;
    EXPECT_TRUE(codecId.IsNull());
    EXPECT_TRUE(codecId.IsDefault());
    EXPECT_EQ(NBigRT::EDeltaAlgorithm::VCDiff, codecId.GetDeltaAlgorithm());
    EXPECT_EQ("zlib6", codecId.GetBaseCompressionCodec());
    EXPECT_EQ("zlib6", codecId.GetPatchCompressionCodec());
}

TEST(TCodecIDTest, DefaultCodecIdNonEmpty) {
    TCodecID codecId(NBSYeti::TBuzzardCodecsGetter::GetDefaultCodecID());
    EXPECT_TRUE(codecId.IsDefault());
    EXPECT_EQ(NBigRT::EDeltaAlgorithm::VCDiff, codecId.GetDeltaAlgorithm());
    EXPECT_EQ("zlib6", codecId.GetBaseCompressionCodec());
    EXPECT_EQ("zlib6", codecId.GetPatchCompressionCodec());
}

TEST(TCodecIDTest, SetDeltaCodecId) {
    TCodecID codecId;
    EXPECT_TRUE(codecId.IsNull());
    EXPECT_TRUE(codecId.IsDefault());
    codecId.SetDeltaAlgorithm(NBigRT::EDeltaAlgorithm::XDelta);
    EXPECT_EQ(NBigRT::EDeltaAlgorithm::XDelta, codecId.GetDeltaAlgorithm());
    EXPECT_EQ("zlib6", codecId.GetBaseCompressionCodec());
    EXPECT_EQ("zlib6", codecId.GetPatchCompressionCodec());
    EXPECT_FALSE(codecId.IsDefault());
}

TEST(TCodecIDTest, SetCompressionCodecId) {
    TCodecID codecId;
    EXPECT_TRUE(codecId.IsNull());
    EXPECT_TRUE(codecId.IsDefault());
    codecId.SetBaseCompressionCodec("zstd_6");
    codecId.SetPatchCompressionCodec("zstd_6");
    EXPECT_EQ(NBigRT::EDeltaAlgorithm::VCDiff, codecId.GetDeltaAlgorithm());
    EXPECT_EQ("zstd_6", codecId.GetBaseCompressionCodec());
    EXPECT_EQ("zstd_6", codecId.GetPatchCompressionCodec());
    EXPECT_FALSE(codecId.IsDefault());
}
