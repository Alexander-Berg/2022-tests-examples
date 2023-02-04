#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <maps/automotive/remote_access/libs/data_types/notifications_phone.h>

Y_UNIT_TEST_SUITE(test_phone_mask) {

using namespace maps::automotive;

Y_UNIT_TEST(test_phone_mask_for_various_length)
{
    ASSERT_EQ(getFilledByStars("+79876543210"), "+7987*****10");
    ASSERT_EQ(getFilledByStars("+7123459876543210"), "+712345987*****10");
    ASSERT_EQ(getFilledByStars("43210"), "***10");
    ASSERT_EQ(getFilledByStars("0"), "0");
}

Y_UNIT_TEST(test_phone_format_validation)
{
    ASSERT_TRUE(isPhoneFormatValid("+79876543210"));
    ASSERT_TRUE(isPhoneFormatValid("+798765432100"));
    ASSERT_TRUE(isPhoneFormatValid("+7987654321"));
    ASSERT_TRUE(isPhoneFormatValid("+39876543210"));
    ASSERT_FALSE(isPhoneFormatValid("79876543210"));
    ASSERT_FALSE(isPhoneFormatValid("798765432100"));
    ASSERT_FALSE(isPhoneFormatValid("89876543210"));
}

}