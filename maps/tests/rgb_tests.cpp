#include <maps/libs/color/include/rgb.h>
#include <maps/libs/color/include/exception.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace maps::color::tests {

Y_UNIT_TEST_SUITE(rgb_should) {

Y_UNIT_TEST(be_dumped_as_hex_notation)
{
    Rgba red{0xff, 0x00, 0x00, 0xff};
    EXPECT_EQ(
        red.hexNotation(HashPrefix::Yes, AppendAlpha::No),
        "#ff0000"
    );
    EXPECT_EQ(
        red.hexNotation(HashPrefix::Yes, AppendAlpha::Yes),
        "#ff0000ff"
    );
    EXPECT_EQ(
        red.hexNotation(HashPrefix::Yes, AppendAlpha::Auto),
        "#ff0000"
    );
    EXPECT_EQ(
        red.hexNotation(),
        "#ff0000"
    );

    Rgba deadBeaf{0xde, 0xad, 0xbe, 0xaf};
    EXPECT_EQ(
        deadBeaf.hexNotation(HashPrefix::No, AppendAlpha::No),
        "deadbe"
    );
    EXPECT_EQ(
        deadBeaf.hexNotation(HashPrefix::No, AppendAlpha::Yes),
        "deadbeaf"
    );
    EXPECT_EQ(
        deadBeaf.hexNotation(HashPrefix::No, AppendAlpha::Auto),
        "deadbeaf"
    );
    EXPECT_EQ(
        deadBeaf.hexNotation(),
        "#deadbeaf"
    );
}

Y_UNIT_TEST(be_parsed_from_hex_notation)
{
    constexpr Rgba ABC{0xAA, 0xBB, 0xCC};
    const std::vector<std::pair<std::string, Rgba>> data = {
        {"aabbcc", ABC},
        {"aabbccff", ABC},
        {"AABBCC", ABC},
        {"AABBCCFF", ABC},
        {"#aabbcc", ABC},
        {"#aabbccff", ABC},
        {"#AaBbCcFf", ABC},
        {"0xaabbcc", ABC},
        {"0XAABBCCFF", ABC},
        {"abc", ABC},
        {"abcf", ABC},
        {"ABC", ABC},
        {"ABCF", ABC},
        {"#abc", ABC},
        {"#abcf", ABC},
        {"0xabc", ABC},
        {"0XABCF", ABC},
        {"#12345678", {0x12, 0x34, 0x56, 0x78}},
        {"1234", {0x11, 0x22, 0x33, 0x44}},
    };
    for (const auto& [input, expected] : data) {
        auto p = Rgba::fromHexNotation(input);
        EXPECT_EQ(p.red, expected.red) << "When parsing rgba hex notation '" << input << "'";
        EXPECT_EQ(p.green, expected.green) << "When parsing rgba hex notation '" << input << "'";
        EXPECT_EQ(p.blue, expected.blue) << "When parsing rgba hex notation '" << input << "'";
        EXPECT_EQ(p.alpha, expected.alpha) << "When parsing rgba hex notation '" << input << "'";
    }

    const std::vector<std::string> invalidData = {
        "",
        "QQQ",
        "#",
        "#12",
        "0x",
        "#0xFFFFFF",
        "0x#FFFFFF",
        "FFFFFz",
        "FFFFzF",
        "F0x",
        "12345678a",
        "1",
        "12",
        "12345",
        "1234567",
        "123456789",
    };
    for (std::string_view invalidInput : invalidData) {
        EXPECT_THROW_MESSAGE_HAS_SUBSTR(
            Rgba::fromHexNotation(invalidInput),
            maps::color::MalformedColor,
            invalidInput);
    }
}

Y_UNIT_TEST(check_predicates)
{
    Rgba red{0xff, 0x00, 0x00, 0xff};
    EXPECT_FALSE(red.isGrayscale());
    EXPECT_TRUE(red.isOpaque());
    EXPECT_FALSE(red.isTransparent());
    EXPECT_FALSE(red.isTranslucent());

    Rgba whiteTranslucent{0xff, 0xff, 0xff, 0x88};
    EXPECT_TRUE(whiteTranslucent.isGrayscale());
    EXPECT_FALSE(whiteTranslucent.isOpaque());
    EXPECT_FALSE(whiteTranslucent.isTransparent());
    EXPECT_TRUE(whiteTranslucent.isTranslucent());

    Rgba blackTransparent{0x00, 0x00, 0x00, 0x00};
    EXPECT_TRUE(blackTransparent.isGrayscale());
    EXPECT_FALSE(blackTransparent.isOpaque());
    EXPECT_TRUE(blackTransparent.isTransparent());
    EXPECT_FALSE(blackTransparent.isTranslucent());
}

} //Y_UNIT_TEST_SUITE

} //namespace maps::color::tests
