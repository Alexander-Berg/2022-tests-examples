#include <maps/wikimap/feedback/api/src/yacare/lib/idm.h>

#include <maps/wikimap/feedback/api/src/libs/test_helpers/db_fixture.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::feedback::api::tests {

using namespace idm;

namespace {

const std::string ROBOT = "robot-wikimap";
const std::string UNKNOWN = "unknown";

auto& dbPool()
{
    static api::tests::DbFixture db;
    return db.pool();
}

} // namespace

Y_UNIT_TEST_SUITE(test_idm)
{

Y_UNIT_TEST(load_roles)
{
    auto roles = loadRoles(dbPool());

    UNIT_ASSERT_VALUES_EQUAL(roles.begin()->first, "read-only");
    UNIT_ASSERT_VALUES_EQUAL(roles.rbegin()->first, "read-write");
}

Y_UNIT_TEST(add_two_roles)
{
    UNIT_ASSERT_NO_EXCEPTION(addUserRole(dbPool(), ROBOT, RoleKey::ReadWrite));
    UNIT_ASSERT_NO_EXCEPTION(addUserRole(dbPool(), ROBOT, RoleKey::ReadOnly));

    auto login2keys = loadLoginToRoleKeys(dbPool());
    UNIT_ASSERT_VALUES_EQUAL(login2keys.size(), 1);

    const auto& [login, roleKeys] = *login2keys.begin();
    UNIT_ASSERT_VALUES_EQUAL(login, ROBOT);
    UNIT_ASSERT_VALUES_EQUAL(roleKeys.size(), 2);
    UNIT_ASSERT_VALUES_EQUAL(*roleKeys.begin(), RoleKey::ReadOnly);
    UNIT_ASSERT_VALUES_EQUAL(*roleKeys.rbegin(), RoleKey::ReadWrite);
}

Y_UNIT_TEST(add_remove_roles)
{
    UNIT_ASSERT_NO_EXCEPTION(removeUserRole(dbPool(), ROBOT, RoleKey::ReadOnly)); // remove non-existed

    UNIT_ASSERT_NO_EXCEPTION(addUserRole(dbPool(), ROBOT, RoleKey::ReadWrite));
    UNIT_ASSERT_NO_EXCEPTION(addUserRole(dbPool(), ROBOT, RoleKey::ReadOnly));
    UNIT_ASSERT_NO_EXCEPTION(removeUserRole(dbPool(), ROBOT, RoleKey::ReadOnly));
    UNIT_ASSERT_NO_EXCEPTION(removeUserRole(dbPool(), ROBOT, RoleKey::ReadOnly)); // remove already removed

    auto login2keys = loadLoginToRoleKeys(dbPool());
    UNIT_ASSERT_VALUES_EQUAL(login2keys.size(), 1);
    const auto& [login, roleKeys] = *login2keys.begin();
    UNIT_ASSERT_VALUES_EQUAL(login, ROBOT);
    UNIT_ASSERT_VALUES_EQUAL(roleKeys.size(), 1);
    UNIT_ASSERT_VALUES_EQUAL(*roleKeys.begin(), RoleKey::ReadWrite);
}

Y_UNIT_TEST(check_read_write_user)
{
    UNIT_ASSERT_NO_EXCEPTION(addUserRole(dbPool(), ROBOT, RoleKey::ReadWrite));
    UNIT_ASSERT_NO_EXCEPTION(addUserRole(dbPool(), ROBOT, RoleKey::ReadOnly));

    auto roles = getUserRoles(dbPool(), ROBOT);
    UNIT_ASSERT_VALUES_EQUAL(roles.size(), 2);
    UNIT_ASSERT_VALUES_EQUAL(*roles.begin(), RoleKey::ReadOnly);
    UNIT_ASSERT_VALUES_EQUAL(*roles.rbegin(), RoleKey::ReadWrite);
    UNIT_ASSERT_VALUES_EQUAL(checkAccess(dbPool(), ROBOT, Access::Read), true);
    UNIT_ASSERT_VALUES_EQUAL(checkAccess(dbPool(), ROBOT, Access::Write), true);
}

Y_UNIT_TEST(check_read_only_user)
{
    UNIT_ASSERT_NO_EXCEPTION(removeUserRole(dbPool(), ROBOT, RoleKey::ReadWrite));
    UNIT_ASSERT_NO_EXCEPTION(addUserRole(dbPool(), ROBOT, RoleKey::ReadOnly));

    auto roles = getUserRoles(dbPool(), ROBOT);
    UNIT_ASSERT_VALUES_EQUAL(roles.size(), 1);
    UNIT_ASSERT_VALUES_EQUAL(*roles.begin(), RoleKey::ReadOnly);
    UNIT_ASSERT_VALUES_EQUAL(checkAccess(dbPool(), ROBOT, Access::Read), true);
    UNIT_ASSERT_VALUES_EQUAL(checkAccess(dbPool(), ROBOT, Access::Write), false);
}

Y_UNIT_TEST(check_unknown_user)
{
    UNIT_ASSERT_VALUES_EQUAL(getUserRoles(dbPool(), UNKNOWN).size(), 0);
    UNIT_ASSERT_VALUES_EQUAL(checkAccess(dbPool(), UNKNOWN, Access::Read), false);
    UNIT_ASSERT_VALUES_EQUAL(checkAccess(dbPool(), UNKNOWN, Access::Write), false);
}

Y_UNIT_TEST(json_roles)
{
    auto roles = loadRoles(dbPool());

    std::ostringstream os;
    json::Builder builder(os);
    builder << [&](json::ObjectBuilder b) {
        toJson(b, roles);
    };
    UNIT_ASSERT_VALUES_EQUAL(
        os.str(),
        "{\"roles\":"
            "{\"slug\":\"role\","
             "\"name\":\"роль\","
             "\"values\":"
                "{\"read-only\":\"только чтение\","
                 "\"read-write\":\"чтение-запись\"}}}");
}

Y_UNIT_TEST(json_login_to_role_keys)
{
    addUserRole(dbPool(), ROBOT, RoleKey::ReadWrite);
    addUserRole(dbPool(), ROBOT, RoleKey::ReadOnly);

    auto loginToRoleKeys = loadLoginToRoleKeys(dbPool());

    std::ostringstream os;
    json::Builder builder(os);
    builder << [&](json::ObjectBuilder b) {
        toJson(b, loginToRoleKeys);
    };
    UNIT_ASSERT_VALUES_EQUAL(
        os.str(),
        "{\"users\":["
            "{\"login\":\"robot-wikimap\","
                "\"roles\":["
                    "{\"role\":\"read-only\"},"
                    "{\"role\":\"read-write\"}"
        "]}]}");
}

} // test_idm suite

} // namespace maps::wiki::feedback::api::tests
