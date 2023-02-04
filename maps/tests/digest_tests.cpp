#include <maps/libs/common/include/digest.h>

#include <library/cpp/testing/gtest/gtest.h>

namespace maps::tests {

namespace {

constexpr std::string_view EMPTY_TEXT_DATA{};
constexpr TArrayRef<const uint8_t> EMPTY_BINARY_DATA{};

constexpr std::string_view SOME_TEXT_DATA = "some data";
const TArrayRef<const uint8_t> SOME_BINARY_DATA(
    reinterpret_cast<const uint8_t*>(SOME_TEXT_DATA.data()),
    SOME_TEXT_DATA.size()
);

} //anonymous namespace

TEST(digest_tests, test_md5_hex) {
    EXPECT_EQ(computeMD5Hex(EMPTY_BINARY_DATA), "d41d8cd98f00b204e9800998ecf8427e");
    EXPECT_EQ(computeMD5Hex(EMPTY_TEXT_DATA), "d41d8cd98f00b204e9800998ecf8427e");

    EXPECT_EQ(computeMD5Hex(SOME_TEXT_DATA), "1e50210a0202497fb79bc38b6ade6c34");
    EXPECT_EQ(computeMD5Hex(SOME_BINARY_DATA), "1e50210a0202497fb79bc38b6ade6c34");
}

TEST(digest_tests, test_sha1_hex) {
    EXPECT_EQ(computeSha1Hex(EMPTY_BINARY_DATA), "da39a3ee5e6b4b0d3255bfef95601890afd80709");
    EXPECT_EQ(computeSha1Hex(EMPTY_TEXT_DATA), "da39a3ee5e6b4b0d3255bfef95601890afd80709");

    EXPECT_EQ(computeSha1Hex(SOME_TEXT_DATA), "baf34551fecb48acc3da868eb85e1b6dac9de356");
    EXPECT_EQ(computeSha1Hex(SOME_BINARY_DATA), "baf34551fecb48acc3da868eb85e1b6dac9de356");
}

TEST(digest_tests, test_sha256_hex) {
    EXPECT_EQ(computeSha256Hex(EMPTY_BINARY_DATA), "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    EXPECT_EQ(computeSha256Hex(EMPTY_TEXT_DATA), "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");

    EXPECT_EQ(computeSha256Hex(SOME_TEXT_DATA), "1307990e6ba5ca145eb35e99182a9bec46531bc54ddf656a602c780fa0240dee");
    EXPECT_EQ(computeSha256Hex(SOME_BINARY_DATA), "1307990e6ba5ca145eb35e99182a9bec46531bc54ddf656a602c780fa0240dee");
}

} //namespace maps::tests
