#include <ads/bsyeti/big_rt/lib/serializable_profile/mutable_row.h>

#include <library/cpp/testing/gtest/gtest.h>

using TCodecID = NBigRT::TCodecID<NBigRT::TCaesarCodecsGetter>;

TEST(TCodecIDTest, EmptyCodecIDString) {
    EXPECT_THROW(TCodecID codecId(""), yexception);
}

TEST(TCodecIDTest, CompressionOnly) {
    TCodecID codecId("zstd_6");
    EXPECT_EQ("zstd_6", codecId.GetBaseCompressionCodec());
    EXPECT_EQ("zstd_6", codecId.GetPatchCompressionCodec());
}

TEST(TCodecIDTest, DefaultDeltaIsVcDiff) {
    TCodecID codecId("zstd_6");
    EXPECT_EQ(NBigRT::EDeltaAlgorithm::VCDiff, codecId.GetDeltaAlgorithm());
}

TEST(TCodecIDTest, DefaultCodecId) {
    TCodecID codecId;
    EXPECT_EQ(NBigRT::EDeltaAlgorithm::VCDiff, codecId.GetDeltaAlgorithm());
    EXPECT_EQ("zstd_6", codecId.GetBaseCompressionCodec());
    EXPECT_EQ("zstd_6", codecId.GetPatchCompressionCodec());
}

TEST(TCodecIDTest, CompressionAndDeltaBsDiff) {
    TCodecID codecId("zstd_6,bsdiff");
    EXPECT_EQ("zstd_6", codecId.GetBaseCompressionCodec());
    EXPECT_EQ("zstd_6", codecId.GetPatchCompressionCodec());
    EXPECT_EQ(NBigRT::EDeltaAlgorithm::BSDiff, codecId.GetDeltaAlgorithm());
}

TEST(TCodecIDTest, CompressionAndDeltaVcDiff) {
    TCodecID codecId("zstd_6,vcdiff");
    EXPECT_EQ("zstd_6", codecId.GetBaseCompressionCodec());
    EXPECT_EQ("zstd_6", codecId.GetPatchCompressionCodec());
    EXPECT_EQ(NBigRT::EDeltaAlgorithm::VCDiff, codecId.GetDeltaAlgorithm());
}

TEST(TCodecIDTest, CompressionAndDeltaXDelta) {
    TCodecID codecId("zstd_6,xdelta");
    EXPECT_EQ("zstd_6", codecId.GetBaseCompressionCodec());
    EXPECT_EQ("zstd_6", codecId.GetPatchCompressionCodec());
    EXPECT_EQ(NBigRT::EDeltaAlgorithm::XDelta, codecId.GetDeltaAlgorithm());
}

TEST(TCodecIDTest, DifferentCompressionCodecs) {
    TCodecID codecId("zstd_6,zstd_1,vcdiff");
    EXPECT_EQ("zstd_6", codecId.GetBaseCompressionCodec());
    EXPECT_EQ("zstd_1", codecId.GetPatchCompressionCodec());
    EXPECT_EQ(NBigRT::EDeltaAlgorithm::VCDiff, codecId.GetDeltaAlgorithm());
}

TEST(TCodecIDTest, SpacedCodecIdString) {
    EXPECT_THROW(TCodecID codecId("zstd_6, vcdiff"), yexception);
}

TEST(TCodecIDTest, TooManyCodecs) {
    EXPECT_THROW(TCodecID codecId("zstd_6,null,vcdiff,bsdiff,null"), yexception);
}

TEST(TCodecIDTest, EmptyCompressionCodec) {
    EXPECT_THROW(TCodecID codecId(",null,vcdiff"), yexception);
}

TEST(TCodecIDTest, CodecIdStringEmptyDelta1) {
    EXPECT_THROW(TCodecID codecId("zstd_6,"), yexception);
}

TEST(TCodecIDTest, CodecIdStringEmptyDelta2) {
    EXPECT_THROW(TCodecID codecId("zstd_6,null,"), yexception);
}

TEST(TCodecIDTest, CodecIdStringWrongDelta) {
    EXPECT_THROW(TCodecID codecId("zstd_6,diffdiff"), yexception);
}

TEST(TCodecIDTest, CodecIdStringWrongDelimiter) {
    EXPECT_NO_THROW(TCodecID codecId("zstd_6+diffdiff"));
}

TEST(TCodecIDTest, CodecIdStringEmptyCompression) {
    EXPECT_THROW(TCodecID codecId(",vcdiff"), yexception);
}

TEST(TCodecIDTest, SettingEmptyBaseCompression) {
    TCodecID codecId;
    EXPECT_THROW(codecId.SetBaseCompressionCodec(""), yexception);
}

TEST(TCodecIDTest, SettingEmptyPatchCompression) {
    TCodecID codecId;
    EXPECT_THROW(codecId.SetPatchCompressionCodec(""), yexception);
}

TEST(TCodecIDTest, ToStringBsDiff) {
    const TString str{"zstd_6,null,bsdiff"};
    TCodecID codecId(str);
    EXPECT_EQ(str, codecId.ToString());
}

TEST(TCodecIDTest, ToStringVcDiff) {
    const TString str{"zstd_6,zstd_6,vcdiff"};
    TCodecID codecId(str);
    EXPECT_EQ(str, codecId.ToString());
}

TEST(TCodecIDTest, ToStringXDelta) {
    const TString str{"zstd_6,null,xdelta"};
    TCodecID codecId(str);
    EXPECT_EQ(str, codecId.ToString());
}

TEST(TCodecIDTest, ToStringVcDiffDefault) {
    const TString str{"zstd_6"};
    TCodecID codecId(str);
    EXPECT_EQ("zstd_6,zstd_6,vcdiff", codecId.ToString());
}

TEST(TCodecIDTest, EqualsTrue) {
    const TString str{"zstd_6,vcdiff"};
    TCodecID lhs(str);
    TCodecID rhs(str);
    EXPECT_TRUE(lhs.Equals(rhs));
}

TEST(TCodecIDTest, EqualsFalseByCompression) {
    TCodecID lhs("zstd_6,vcdiff");
    TCodecID rhs("zstd_7,vcdiff");
    EXPECT_FALSE(lhs.Equals(rhs));
}

TEST(TCodecIDTest, EqualsFalseByDelta) {
    TCodecID lhs("zstd_6,vcdiff");
    TCodecID rhs("zstd_6,bsdiff");
    EXPECT_FALSE(lhs.Equals(rhs));
}

TEST(TCodecIDTest, EqualsTrueDefault) {
    TCodecID lhs("zstd_6,vcdiff");
    TCodecID rhs("zstd_6");
    EXPECT_TRUE(lhs.Equals(rhs));
}

TEST(TCodecIDTest, EqualsFalseDefault) {
    TCodecID lhs("zstd_6,bsdiff");
    TCodecID rhs("zstd_6");
    EXPECT_FALSE(lhs.Equals(rhs));
}

TEST(TCodecIDTest, EqualsTrueCompressionAndDefault) {
    TCodecID lhs("zstd_6");
    TCodecID rhs("zstd_6");
    EXPECT_TRUE(lhs.Equals(rhs));
}


