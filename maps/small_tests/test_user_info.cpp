#include <maps/wikimap/feedback/userapi/src/yacare/lib/user_info.h>

#include <maps/libs/auth/include/user_info.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::feedback::userapi::tests {

Y_UNIT_TEST_SUITE(test_user_info)
{

Y_UNIT_TEST(get_email_address)
{
    maps::auth::UserInfo userInfo;
    UNIT_ASSERT(!getEmailAddress(userInfo));

    userInfo.setAddressList({});
    UNIT_ASSERT(!getEmailAddress(userInfo));

    maps::auth::AddressListItem addressListItem;
    addressListItem.address = "sample@yandex.ru";
    userInfo.setAddressList({addressListItem});

    auto address = getEmailAddress(userInfo);
    UNIT_ASSERT(address);
    UNIT_ASSERT_VALUES_EQUAL(*address, "sample@yandex.ru");
}

}

} // namespace maps::wiki::feedback::userapi::tests
