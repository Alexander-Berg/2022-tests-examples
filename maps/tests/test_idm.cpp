#include <library/cpp/testing/gtest/gtest.h>
#include <maps/automotive/libs/test_helpers/helpers.h>
#include <maps/automotive/store_internal/lib/dao/idm.h>
#include <maps/automotive/store_internal/tests/helpers.h>
#include <maps/automotive/store_internal/tests/postgres.h>
#include <maps/infra/tvm2lua/ticket_parser2_lua/lib/lua_api.h>
#include <maps/libs/json/include/value.h>

namespace maps::automotive::store_internal {

using IdmApi = AppContextPostgresFixture;

namespace {

const std::string IDM_PREFIX = "/idm/";

const std::string JOE = "joe";

void checkUserHasRole(const std::string& login, const std::string& role) {
    auto txn = dao::makeReadOnlyTransaction();
    IdmDao(*txn).checkUserHasRole(login, role);
}

void checkUserHasNoRole(const std::string& login, const std::string& role) {
    auto txn = dao::makeReadOnlyTransaction();
    EXPECT_THROW(IdmDao(*txn).checkUserHasRole(login, role), yacare::errors::Unauthorized);
}

void checkResponseStatus(const http::MockResponse& rsp)
{
    ASSERT_EQ(200, rsp.status);
    auto body = json::Value::fromString(rsp.body);
    ASSERT_EQ(0, body["code"].as<int>());
}

http::MockResponse idmGet(const std::string& request)
{
    http::URL url("http://localhost" + IDM_PREFIX + request);
    http::MockRequest req(http::GET, url);
    req.headers[tvm2::SRC_TVM_ID_HEADER] = std::to_string(idm::IDM_TVM_ID);
    return yacare::performTestRequest(req);
}

void idmPost(
    const std::string& request,
    const std::string& role,
    const std::string& login)
{
    http::URL url("http://localhost" + IDM_PREFIX + request);
    url.addParam("login", login);
    url.addParam("role", R"({"role": ")" + role + R"("})");
    http::MockRequest req(http::POST, url);
    req.headers[tvm2::SRC_TVM_ID_HEADER] = std::to_string(idm::IDM_TVM_ID);

    checkResponseStatus(yacare::performTestRequest(req));
}

} // namespace

using namespace role;

TEST_F(IdmApi, forbiddenWithoutIdmServiceTicket)
{
    http::HeaderMap badTvmId = {{tvm2::SRC_TVM_ID_HEADER, "2001602"}};

    EXPECT_EQ(401, mockGet(IDM_PREFIX + "info/").status);
    EXPECT_EQ(401, mockGet(IDM_PREFIX + "info/", badTvmId).status);
    EXPECT_EQ(401, mockGet(IDM_PREFIX + "get-all-roles/", badTvmId).status);
    EXPECT_EQ(401, mockPost(IDM_PREFIX + "add-role/", "", badTvmId).status);
    EXPECT_EQ(401, mockPost(IDM_PREFIX + "remove-role/", "", badTvmId).status);
}

TEST_F(IdmApi, info)
{
    auto rsp = idmGet("info/");
    ASSERT_EQ(200, rsp.status);
    auto body = json::Value::fromString(rsp.body);
    ASSERT_EQ(0, body["code"].as<int>());
    ASSERT_EQ("role", body["roles"]["slug"].as<std::string>());
    const auto& roles = body["roles"]["values"].fields();
    std::set<std::string> expectedRoles = {
        VIEWER,
        RELEASE_MANAGER_INTERNAL,
        RELEASE_MANAGER_PRODUCTION,
        DEVICE_KEY_MANAGER_VIRTUAL,
        DEVICE_KEY_MANAGER_PRODUCTION,
        ADMIN};
    std::set<std::string> actualRoles {roles.cbegin(), roles.cend()};
    EXPECT_EQ(expectedRoles, actualRoles);
}

TEST_F(IdmApi, getAllRoles)
{
    idmPost("add-role/", VIEWER, JOE);
    idmPost("add-role/", ADMIN, JOE);

    auto rsp = idmGet("get-all-roles/");
    checkResponseStatus(rsp);
    auto body = json::Value::fromString(rsp.body);
    ASSERT_EQ(7u, body["users"].size());

    std::unordered_map<std::string, std::set<std::string>> expectedRoles = {
        {JOE, {VIEWER, ADMIN}},
        {"admin-arnold", {ADMIN}},
        {"manager-prod", {RELEASE_MANAGER_PRODUCTION}},
        {"manager", {RELEASE_MANAGER_INTERNAL}},
        {"key-manager-prod", {DEVICE_KEY_MANAGER_PRODUCTION}},
        {"key-manager", {DEVICE_KEY_MANAGER_VIRTUAL}},
        {"viewer-victor", {VIEWER}}};
    for (const auto& user: body["users"]) {
        std::set<std::string> actualRoles;
        for (const auto& role: user["roles"]) {
            actualRoles.insert(role["role"].as<std::string>());
        }
        EXPECT_EQ(expectedRoles.at(user["login"].as<std::string>()), actualRoles);
    }
}

TEST_F(IdmApi, addRole)
{
    checkUserHasNoRole(JOE, VIEWER);

    idmPost("add-role/", VIEWER, JOE);

    checkUserHasRole(JOE, VIEWER);
    checkUserHasNoRole(JOE, RELEASE_MANAGER_PRODUCTION);
    checkUserHasNoRole(JOE, DEVICE_KEY_MANAGER_PRODUCTION);
    checkUserHasNoRole(JOE, ADMIN);

    // operation in idempotent
    idmPost("add-role/", VIEWER, JOE);
    checkUserHasRole(JOE, VIEWER);
}

TEST_F(IdmApi, addRoleInherited)
{
    checkUserHasNoRole(JOE, VIEWER);
    checkUserHasNoRole(JOE, DEVICE_KEY_MANAGER_PRODUCTION);

    idmPost("add-role/", RELEASE_MANAGER_PRODUCTION, JOE);

    checkUserHasRole(JOE, VIEWER);
    checkUserHasRole(JOE, RELEASE_MANAGER_PRODUCTION);
    checkUserHasRole(JOE, RELEASE_MANAGER_INTERNAL);
    checkUserHasNoRole(JOE, DEVICE_KEY_MANAGER_PRODUCTION);
    checkUserHasNoRole(JOE, ADMIN);

    // removing other role has no effect
    idmPost("remove-role/", DEVICE_KEY_MANAGER_PRODUCTION, JOE);
    checkUserHasRole(JOE, VIEWER);
    checkUserHasRole(JOE, RELEASE_MANAGER_PRODUCTION);
    checkUserHasRole(JOE, RELEASE_MANAGER_INTERNAL);
    checkUserHasNoRole(JOE, DEVICE_KEY_MANAGER_PRODUCTION);
    checkUserHasNoRole(JOE, ADMIN);

    // removing inherited role has no effect
    idmPost("remove-role/", RELEASE_MANAGER_INTERNAL, JOE);
    checkUserHasRole(JOE, VIEWER);
    checkUserHasRole(JOE, RELEASE_MANAGER_PRODUCTION);
    checkUserHasRole(JOE, RELEASE_MANAGER_INTERNAL);
    checkUserHasNoRole(JOE, DEVICE_KEY_MANAGER_PRODUCTION);
    checkUserHasNoRole(JOE, ADMIN);

    // add other role
    idmPost("add-role/", DEVICE_KEY_MANAGER_PRODUCTION, JOE);
    checkUserHasRole(JOE, VIEWER);
    checkUserHasRole(JOE, RELEASE_MANAGER_PRODUCTION);
    checkUserHasRole(JOE, RELEASE_MANAGER_INTERNAL);
    checkUserHasRole(JOE, DEVICE_KEY_MANAGER_PRODUCTION);
    checkUserHasRole(JOE, DEVICE_KEY_MANAGER_VIRTUAL);
    checkUserHasNoRole(JOE, ADMIN);

    // removing a role does not affect other roles
    idmPost("remove-role/", RELEASE_MANAGER_PRODUCTION, JOE);
    checkUserHasRole(JOE, VIEWER);
    checkUserHasNoRole(JOE, RELEASE_MANAGER_PRODUCTION);
    checkUserHasNoRole(JOE, RELEASE_MANAGER_INTERNAL);
    checkUserHasRole(JOE, DEVICE_KEY_MANAGER_PRODUCTION);
    checkUserHasRole(JOE, DEVICE_KEY_MANAGER_VIRTUAL);
    checkUserHasNoRole(JOE, ADMIN);
}

TEST_F(IdmApi, removeRole)
{
    idmPost("add-role/", VIEWER, JOE);
    checkUserHasRole(JOE, VIEWER);

    idmPost("remove-role/", VIEWER, JOE);
    checkUserHasNoRole(JOE, VIEWER);

    // operation is idempotent
    idmPost("remove-role/", VIEWER, JOE);
    checkUserHasNoRole(JOE, VIEWER);
}

TEST_F(IdmApi, userRoles)
{
    // no user info
    ASSERT_EQ(401, mockGet("/store/1.x/role/joe").status);

    idmPost("add-role/", VIEWER, JOE);
    idmPost("add-role/", RELEASE_MANAGER_PRODUCTION, JOE);

    { // not yandex-team login
        auth::UserInfo notYaTeam{};
        notYaTeam.setLogin("donald");
        yacare::tests::UserInfoFixture userInfoFixture(notYaTeam);
        ASSERT_EQ(401, mockGet("/store/1.x/role/joe").status);
    }

    yacare::tests::UserInfoFixture userInfoFixture(makeUserInfo("donald"));

    // "donald" does not have ADMIN role
    ASSERT_EQ(401, mockGet("/store/1.x/role/joe").status);

    idmPost("add-role/", ADMIN, "donald");

    // require user login in path
    ASSERT_EQ(400, mockGet("/store/1.x/role/").status);

    auto rsp = mockGet("/store/1.x/role/joe");
    ASSERT_EQ(200, rsp.status);
    auto body = json::Value::fromString(rsp.body);
    ASSERT_EQ(2u, body.size());
    std::set<std::string> expectedRoles = {VIEWER, RELEASE_MANAGER_PRODUCTION};
    std::set<std::string> actualRoles;
    for (const auto& role: body) {
        actualRoles.insert(role.as<std::string>());
    }
    EXPECT_EQ(expectedRoles, actualRoles);
}

}
