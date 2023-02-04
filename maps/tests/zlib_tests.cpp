#include <maps/libs/common/include/zlib.h>

#include <library/cpp/testing/gtest/gtest.h>

namespace maps::tests {

TEST(zlib_tests, somedata) {
    std::vector<char> data(100, 'Z');
    auto compressed = zlibCompress(data);
    EXPECT_LT(compressed.size(), data.size());
    EXPECT_EQ(zlibDecompress(compressed), data);
}

TEST(zlib_tests, bigdata) {
    std::vector<char> data(100000);
    for (size_t i = 0; i < data.size(); ++i) {
        data[i] = static_cast<char>(i + i / 211);
    }
    auto compressed1 = zlibCompress(data, 1);
    auto compressed6 = zlibCompress(data);
    auto compressed9 = zlibCompress(data, ZLIB_BEST_COMPRESSION);
    EXPECT_LT(compressed1.size(), data.size());
    EXPECT_LT(compressed6.size(), compressed1.size());
    EXPECT_LT(compressed9.size(), compressed6.size());
    EXPECT_EQ(zlibDecompress(compressed1), data);
    EXPECT_EQ(zlibDecompress(compressed6), data);
    EXPECT_EQ(zlibDecompress(compressed9), data);
}

TEST(zlib_tests, throw_when_decompress_invalid_data)
{
    EXPECT_THROW(zlibDecompress("!!!"), std::exception);
}

TEST(zlib_tests, empty)
{
    EXPECT_EQ(zlibCompress({}), std::vector<char>{});
    EXPECT_EQ(zlibDecompress({}), std::vector<char>{});
}

} // namespace maps::tests
