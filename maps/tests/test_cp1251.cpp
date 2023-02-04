#include <maps/libs/codepage/include/codepage.h>
#include <maps/libs/codepage/tests/test_data.h>

#include <maps/libs/common/include/exception.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace maps::codepage::tests {

Y_UNIT_TEST_SUITE(test_cp1251_to_u32) {

Y_UNIT_TEST(test_roza) {
	std::u32string converted;
	convert(ROZA_CP1251, converted);
	EXPECT_EQ(converted.size(), ROZA_U32.size());
	EXPECT_EQ(asVector(converted), asVector(ROZA_U32));
    EXPECT_EQ(converted, ROZA_U32);
}

Y_UNIT_TEST(test_converting_0x98) {
    cp1251string_view UNSUPPORTED_CHAR("\x98");
	std::u32string converted;
    UNIT_ASSERT_EXCEPTION_CONTAINS(
        convert(UNSUPPORTED_CHAR, converted),
        maps::Exception,
        "Symbol with code 152 is not CP-1251 and can not be converted"
    );
}

} //Y_UNIT_TEST_SUITE(test_cp1251_to_u32)

Y_UNIT_TEST_SUITE(test_u32_to_cp1251) {

Y_UNIT_TEST(test_roza) {
    cp1251string converted;
    convert(ROZA_U32, converted);
    EXPECT_EQ(converted.value().size(), ROZA_CP1251.value().size());
    EXPECT_EQ(asVector(converted), asVector(ROZA_CP1251));
    EXPECT_EQ(converted, ROZA_CP1251);
}

Y_UNIT_TEST(test_thousand_words) {
    cp1251string converted;
    convert(THOUSAND_WORDS_U32, converted);
    EXPECT_TRUE(converted.value().empty());
}

} //Y_UNIT_TEST_SUITE(test_u32_to_cp1251)

} //namespace maps::codepage::tests

