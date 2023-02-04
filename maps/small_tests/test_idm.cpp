#include <maps/wikimap/feedback/api/src/yacare/lib/idm.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::feedback::api::tests {

using namespace idm;

Y_UNIT_TEST_SUITE(test_idm)
{

Y_UNIT_TEST(parse_invalid_path)
{
    UNIT_ASSERT_EXCEPTION(pathToRoleKey(""), BadParameter);
    UNIT_ASSERT_EXCEPTION(pathToRoleKey("/"), BadParameter);
    UNIT_ASSERT_EXCEPTION(pathToRoleKey("//"), BadParameter);
    UNIT_ASSERT_EXCEPTION(pathToRoleKey("///"), BadParameter);
    UNIT_ASSERT_EXCEPTION(pathToRoleKey("/unknown"), BadParameter);
    UNIT_ASSERT_EXCEPTION(pathToRoleKey("/unknown/"), BadParameter);
    UNIT_ASSERT_EXCEPTION(pathToRoleKey("/role/unknown"), BadParameter);
    UNIT_ASSERT_EXCEPTION(pathToRoleKey("/role/unknown/"), BadParameter);
}

Y_UNIT_TEST(parse_valid_path)
{
    UNIT_ASSERT_VALUES_EQUAL(pathToRoleKey("/role/read-write"), RoleKey::ReadWrite);
    UNIT_ASSERT_VALUES_EQUAL(pathToRoleKey("/role/read-write/"), RoleKey::ReadWrite);

    UNIT_ASSERT_VALUES_EQUAL(pathToRoleKey("/role/read-only"), RoleKey::ReadOnly);
    UNIT_ASSERT_VALUES_EQUAL(pathToRoleKey("/role/read-only/"), RoleKey::ReadOnly);
}

Y_UNIT_TEST(json_empty_role_keys)
{
    RoleKeys roleKeys;

    std::ostringstream os;
    json::Builder builder(os);
    builder << [&](json::ObjectBuilder b) {
        toJson(b, roleKeys);
    };
    UNIT_ASSERT_VALUES_EQUAL(
        os.str(),
        "{\"roles\":[]}");
}

Y_UNIT_TEST(json_role_keys)
{
    RoleKeys roleKeys { RoleKey::ReadWrite, RoleKey::ReadOnly };

    std::ostringstream os;
    json::Builder builder(os);
    builder << [&](json::ObjectBuilder b) {
        toJson(b, roleKeys);
    };
    UNIT_ASSERT_VALUES_EQUAL(
        os.str(),
        "{\"roles\":[\"read-only\",\"read-write\"]}");
}

} // test_idm suite

} // namespace maps::wiki::feedback::api::tests
