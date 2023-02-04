#include <maps/libs/codepage/include/codepage.h>
#include <maps/libs/codepage/tests/test_data.h>

#include <maps/libs/common/include/exception.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace maps::codepage::tests {

Y_UNIT_TEST_SUITE(test_u32_to_u8) {

Y_UNIT_TEST(test_lorem_ipsum) {
    u8string converted;
    convert(LOREM_IPSUM_U32, converted);
    EXPECT_EQ(converted.value().size(), LOREM_IPSUM_U8.value().size());
    EXPECT_EQ(converted, LOREM_IPSUM_U8);
}

Y_UNIT_TEST(test_onegin) {
    u8string converted;
    convert(ONEGIN_U32, converted);
    EXPECT_EQ(converted.value().size(), ONEGIN_U8.value().size());
    EXPECT_EQ(converted, ONEGIN_U8);
}

Y_UNIT_TEST(test_thousand_words) {
    u8string converted;
    convert(THOUSAND_WORDS_U32, converted);
    EXPECT_EQ(converted.value().size(), THOUSAND_WORDS_U8.value().size());
    EXPECT_EQ(asVector(converted), asVector(THOUSAND_WORDS_U8));
    EXPECT_EQ(converted, THOUSAND_WORDS_U8);
}

Y_UNIT_TEST(test_hwair) {
    u8string converted;
    convert(HWAIR_U32, converted);
    EXPECT_EQ(converted.value().size(), HWAIR_U8.value().size());
    EXPECT_EQ(asVector(converted), asVector(HWAIR_U8));
    EXPECT_EQ(converted, HWAIR_U8);
}

Y_UNIT_TEST(test_invalid_codepoint) {
    u8string converted;
    std::u32string MAX_CORRECT_CODE_POINT(1, static_cast<char32_t>(0x001f'ffff));
    EXPECT_NO_THROW(convert(MAX_CORRECT_CODE_POINT, converted));
}

Y_UNIT_TEST(wrong_code_points) {
    u8string converted;

    std::u32string WRONG_CODE_POINT_UNSIGNED(1, static_cast<char32_t>(0x7000'0000));
    UNIT_ASSERT_EXCEPTION_CONTAINS(
        convert(WRONG_CODE_POINT_UNSIGNED, converted),
        maps::Exception,
        "Symbol with code 1879048192 is not Unicode and can not be represented in UTF-8"
    );

    std::u32string WRONG_CODE_POINT_SIGNED(1, static_cast<char32_t>(0xf000'0000));
    UNIT_ASSERT_EXCEPTION_CONTAINS(
        convert(WRONG_CODE_POINT_SIGNED, converted),
        maps::Exception,
        "Symbol with code 4026531840 is not Unicode and can not be represented in UTF-8"
    );
}

} //Y_UNIT_TEST_SUITE(test_u32_to_u8)

} //namespace maps::codepage::tests
