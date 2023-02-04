#include <maps/factory/libs/db/acl.h>
#include <maps/factory/libs/db/acl_gateway.h>

#include <maps/factory/libs/unittest/fixture.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace maps::factory::db::tests {

Y_UNIT_TEST_SUITE(test_acl_gateway) {

Y_UNIT_TEST(test_querying_user_roles)
{
    unittest::Fixture fixture;
    pqxx::connection conn(fixture.postgres().connectionString());
    pqxx::work txn(conn);

    EXPECT_TRUE(UserRoleGateway(txn).hasRole(unittest::TEST_CUSTOMER_USER_ID, Role::Customer));
    EXPECT_TRUE(UserRoleGateway(txn).hasRole(unittest::TEST_SUPPLIER_USER_ID, Role::Supplier));
}

} // suite

} // namespace maps::factory::db::tests
