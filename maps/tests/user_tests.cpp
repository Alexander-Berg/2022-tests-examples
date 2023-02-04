#include <library/cpp/testing/unittest/registar.h>

#include <maps/wikimap/mapspro/libs/gdpr/include/user.h>

namespace maps::wiki::gdpr::tests {

Y_UNIT_TEST_SUITE(user_tests)
{

Y_UNIT_TEST(uid)
{
    UNIT_ASSERT_EQUAL(User(0).uid(), 0);
    UNIT_ASSERT_EQUAL(User(1).uid(), 1);
    UNIT_ASSERT_EQUAL(User(-1).uid(), 0);
}

Y_UNIT_TEST(real_uid)
{
    UNIT_ASSERT_EQUAL(User(0).realUid(), 0);
    UNIT_ASSERT_EQUAL(User(1).realUid(), 1);
    UNIT_ASSERT_EQUAL(User(-1).realUid(), 1);
}

Y_UNIT_TEST(hidden)
{
    UNIT_ASSERT_EQUAL(User(0).hidden(), true);
    UNIT_ASSERT_EQUAL(User(1).hidden(), false);
    UNIT_ASSERT_EQUAL(User(-1).hidden(), true);
}

} // Y_UNIT_TEST_SUITE(user_tests)

} // namespace maps::wiki::gdpr::tests
