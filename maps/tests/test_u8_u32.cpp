#include <maps/libs/codepage/include/codepage.h>
#include <maps/libs/codepage/tests/test_data.h>

#include <maps/libs/common/include/exception.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <string_view>

namespace maps::codepage::tests {

using namespace std::literals::string_view_literals;

Y_UNIT_TEST_SUITE(test_data) {

//TODO: this test should be moved to a standalone file
Y_UNIT_TEST(test_data_test) {
    EXPECT_EQ(sizeof(LOREM_IPSUM_U8.value().front()), 1u);
    EXPECT_EQ(sizeof(LOREM_IPSUM_U32.front()), 4u);
    //each letter occupies exactly 1 byte
    EXPECT_EQ(LOREM_IPSUM_U8.value().size(), LOREM_IPSUM_U32.size());

    EXPECT_EQ(sizeof(ONEGIN_U8.value().front()), 1u);
    EXPECT_EQ(sizeof(ONEGIN_U32.front()), 4u);
    //each letter occupies 2 bytes, yet punctuation and space occupies only one
    EXPECT_GT(ONEGIN_U8.value().size(), ONEGIN_U32.size() * 1);
    EXPECT_LT(ONEGIN_U8.value().size(), ONEGIN_U32.size() * 2);

    EXPECT_EQ(sizeof(THOUSAND_WORDS_U8.value().front()), 1u);
    EXPECT_EQ(sizeof(THOUSAND_WORDS_U32.front()), 4u);
    //each hieroglyph occupies 3 bytes
    EXPECT_EQ(THOUSAND_WORDS_U8.value().size(), THOUSAND_WORDS_U32.size() * 3);

    EXPECT_EQ(sizeof(HWAIR_U8.value().front()), 1u);
    EXPECT_EQ(sizeof(HWAIR_U32.front()), 4u);
    //Hwair letter occupies 4 bytes
    EXPECT_EQ(HWAIR_U8.value().size(), HWAIR_U32.size() * 4);

    EXPECT_EQ(asVector(ROZA_U32), asVector(U"а роза упала на лапу азора"));
    EXPECT_EQ(ROZA_U32, U"а роза упала на лапу азора");
}

} //Y_UNIT_TEST_SUITE(test_data)

Y_UNIT_TEST_SUITE(test_u8_to_u32) {

Y_UNIT_TEST(test_lorem_ipsum) {
    std::u32string converted;
    convert(LOREM_IPSUM_U8, converted);
    EXPECT_EQ(converted.size(), LOREM_IPSUM_U32.size());
    EXPECT_EQ(converted, LOREM_IPSUM_U32);
}

Y_UNIT_TEST(test_onegin) {
    std::u32string converted;
    convert(ONEGIN_U8, converted);
    EXPECT_EQ(converted.size(), ONEGIN_U32.size());
    EXPECT_EQ(converted, ONEGIN_U32);
}

Y_UNIT_TEST(test_thousand_words) {
    std::u32string converted;
    convert(THOUSAND_WORDS_U8, converted);
    EXPECT_EQ(converted.size(), THOUSAND_WORDS_U32.size());
    EXPECT_EQ(converted, THOUSAND_WORDS_U32);
}

Y_UNIT_TEST(test_hwair) {
    std::u32string converted;
    convert(HWAIR_U8, converted);
    EXPECT_EQ(converted.size(), HWAIR_U32.size());
    EXPECT_EQ(converted, HWAIR_U32);
}

Y_UNIT_TEST(overlong_encoding) {
    /*
     * These examples have been taken from
     * https://en.wikipedia.org/wiki/UTF-8#Overlong_encodings
     */
    constexpr u8string_view OVERLONG_EURO_U8(std::string_view("\xF0\x82\x82\xAC"));
    constexpr char32_t NORMAL_EURO_SIGN = U'€';

    std::u32string converted;
    convert(OVERLONG_EURO_U8, converted);
    EXPECT_EQ(converted.size(), 1u);
    EXPECT_EQ(converted.front(), NORMAL_EURO_SIGN);

    constexpr u8string_view OVERLONG_ASCII_ZERO_U8(std::string_view("\xC0\x80"));
    constexpr char32_t NORMAL_ASCII_ZERO = U'\0';

    convert(OVERLONG_ASCII_ZERO_U8, converted);
    EXPECT_EQ(converted.size(), 1u);
    EXPECT_EQ(converted.front(), NORMAL_ASCII_ZERO);
}

Y_UNIT_TEST(wrong_byte_streams_not_enough_data) {
    std::u32string converted;

    constexpr u8string_view TWO_BYTE_NOT_ENOUGH("\xC0"sv);
    UNIT_ASSERT_EXCEPTION_CONTAINS(
        convert(TWO_BYTE_NOT_ENOUGH, converted),
        maps::Exception,
        "Not enough bytes at position 0: 1 more byte is needed"
    );

    constexpr u8string_view THREE_BYTE_NOT_ENOUGH("\xE0\x80"sv);
    UNIT_ASSERT_EXCEPTION_CONTAINS(
        convert(THREE_BYTE_NOT_ENOUGH, converted),
        maps::Exception,
        "Not enough bytes at position 0: 2 more bytes are needed"
    );

    constexpr u8string_view FOUR_BYTE_NOT_ENOUGH("\xF0\x80\x80"sv);
    UNIT_ASSERT_EXCEPTION_CONTAINS(
        convert(FOUR_BYTE_NOT_ENOUGH, converted),
        maps::Exception,
        "Not enough bytes at position 0: 3 more bytes are needed"
    );
}

Y_UNIT_TEST(wrong_byte_streams_signatures) {
    std::u32string converted;

    constexpr u8string_view BAD_HEAD_SIGNATURE_MIDDLE("\x80\0\0\0"sv);
    UNIT_ASSERT_EXCEPTION_CONTAINS(
        convert(BAD_HEAD_SIGNATURE_MIDDLE, converted),
        maps::Exception,
        "Wrong heading byte signature"
    );

    constexpr u8string_view BAD_HEAD_SIGNATURE_END("\xff\0\0\0"sv);
    UNIT_ASSERT_EXCEPTION_CONTAINS(
        convert(BAD_HEAD_SIGNATURE_END, converted),
        maps::Exception,
        "Wrong heading byte signature"
    );

    constexpr u8string_view BAD_TRAILING_SIGNATURE_TWO("\xC0\xC0\0\0\0"sv);
    UNIT_ASSERT_EXCEPTION_CONTAINS(
        convert(BAD_TRAILING_SIGNATURE_TWO, converted),
        maps::Exception,
        "Wrong trailing byte signatures"
    );

    constexpr u8string_view BAD_TRAILING_SIGNATURE_THREE("\xE0\xC0\0\0\0"sv);
    UNIT_ASSERT_EXCEPTION_CONTAINS(
        convert(BAD_TRAILING_SIGNATURE_THREE, converted),
        maps::Exception,
        "Wrong trailing byte signatures"
    );

    constexpr u8string_view BAD_TRAILING_SIGNATURE_FOUR("\xF0\xC0\0\0\0"sv);
    UNIT_ASSERT_EXCEPTION_CONTAINS(
        convert(BAD_TRAILING_SIGNATURE_FOUR, converted),
        maps::Exception,
        "Wrong trailing byte signatures"
    );
}

} //Y_UNIT_TEST_SUITE(test_u8_to_u32)

} //namespace maps::codepage::tests
