#include <maps/factory/services/sputnica_back/tests/common/common.h>
#include <maps/factory/services/sputnica_back/tests/api_backend_tests/fixture.h>
#include <maps/factory/services/sputnica_back/lib/yacare_helpers.h>

#include <yandex/maps/proto/factory/acl.sproto.h>
#include <maps/factory/libs/sproto_helpers/order.h>

#include <maps/infra/yacare/include/test_utils.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <boost/lexical_cast.hpp>

namespace maps::factory::sputnica::tests {

Y_UNIT_TEST_SUITE_F(users_api_tests, Fixture) {

Y_UNIT_TEST(test_listing_user_roles)
{
    http::MockRequest rq(http::GET, http::URL("http://localhost/users/list-roles"));

    //request without headers MUST return 401 Unauthorized
    http::MockResponse resp = yacare::performTestRequest(rq);
    EXPECT_EQ(resp.status, 401);

    //adding header containing customer user id
    setAuthHeaderFor(db::Role::Customer, rq);
    resp = yacare::performTestRequest(rq);

    EXPECT_EQ(resp.status, 200);
    auto sprotoCustomerRoles = boost::lexical_cast<sproto_helpers::sfactory::acl::Roles>(resp.body);
    ASSERT_EQ(sprotoCustomerRoles.roles().size(), 2u);
    ASSERT_EQ(sprotoCustomerRoles.roles()[0], sproto_helpers::sfactory::acl::Role::VIEWER);
    ASSERT_EQ(sprotoCustomerRoles.roles()[1], sproto_helpers::sfactory::acl::Role::CUSTOMER);

    //changing header to contain supplier user id
    setAuthHeaderFor(db::Role::Supplier, rq);
    resp = yacare::performTestRequest(rq);

    EXPECT_EQ(resp.status, 200);
    auto sprotoSupplierRoles = boost::lexical_cast<sproto_helpers::sfactory::acl::Roles>(resp.body);
    ASSERT_EQ(sprotoSupplierRoles.roles().size(), 2u);
    ASSERT_EQ(sprotoCustomerRoles.roles()[0], sproto_helpers::sfactory::acl::Role::VIEWER);
    ASSERT_EQ(sprotoSupplierRoles.roles()[1], sproto_helpers::sfactory::acl::Role::SUPPLIER);
}

} //Y_UNIT_TEST_SUITE

} //namespace maps::factory::sputnica::tests
