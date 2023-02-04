#include <maps/factory/services/backend/tests/test_utils.h>

#include <maps/factory/libs/idm/db/user_role_gateway.h>

#include <library/cpp/testing/gtest/gtest.h>
#include <maps/infra/tvm2lua/ticket_parser2_lua/lib/lua_api.h>
#include <maps/libs/http/include/urlencode.h>
#include <maps/libs/json/include/value.h>

namespace maps::factory::backend::tests {

namespace {

const std::string LOGIN = "john";

class IdmFixture : public BackendFixture {
public:
    IdmFixture()
    {
        truncateTables();
        insertData();
    }

private:
    void insertData()
    {
        auto txn = txnHandle();
        txn->exec(maps::common::readFileToString(SRC_("idm_data.sql")));
        txn->commit();
    }
};

}

TEST_F(IdmFixture, test_idm_info)
{
    http::MockRequest request(http::GET,
        http::URL("http://localhost/v1/idm/info/"));
    auto response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 200);

    auto jsonBody = json::Value::fromString(response.body);
    EXPECT_EQ(jsonBody["code"].as<int>(), 0);
    EXPECT_EQ(jsonBody["roles"],
        json::Value::fromFile(SRC_("idm_roles_info.json"))["roles"]);
}

namespace {

enum class Op { Add, Remove };

http::MockResponse requestChangeUserRole(
    Op operation,
    const std::string& login,
    const std::string& rolePath)
{
    http::MockRequest request(http::POST,
        http::URL(std::string("http://localhost/v1/idm/") +
                  (operation == Op::Add ? "add-role/" : "remove-role/")));
    request.body = "login=" + http::urlEncode(login)
                   + "&path=" + http::urlEncode(rolePath);
    return yacare::performTestRequest(request);
}

auto requestAddUserRole(const std::string& login, const std::string& rolePath)
{
    return requestChangeUserRole(Op::Add, login, rolePath);
}

auto requestRemoveUserRole(const std::string& login, const std::string& rolePath)
{
    return requestChangeUserRole(Op::Remove, login, rolePath);
}

} // anonymous namespace

TEST_F(IdmFixture, test_idm_add_remove_role)
{
    {
        auto response = requestAddUserRole(LOGIN, "project/one/subproject/one-A/role/developer");
        EXPECT_EQ(response.status, 200);
        auto body = json::Value::fromString(response.body);
        EXPECT_EQ(body["code"].as<int>(), 0);

        auto txn = txnHandle();
        idm::db::UserRoleGateway gtw(*txn);
        auto userRoles = gtw.load(idm::db::table::UserRole::login == LOGIN);
        ASSERT_EQ(userRoles.size(), 1u);
        EXPECT_EQ(userRoles[0].projectRoleId(), 12);
    }
    {
        // Add existing role
        auto response = requestAddUserRole(LOGIN, "project/one/subproject/one-A/role/developer");
        EXPECT_EQ(response.status, 200);
        auto body = json::Value::fromString(response.body);
        EXPECT_EQ(body["code"].as<int>(), 0);
    }
    {
        auto response = requestRemoveUserRole(LOGIN, "project/one/subproject/one-A/role/developer");
        EXPECT_EQ(response.status, 200);
        auto body = json::Value::fromString(response.body);
        EXPECT_EQ(body["code"].as<int>(), 0);

        auto txn = txnHandle();
        EXPECT_TRUE(idm::db::UserRoleGateway{*txn}.load().empty());
    }
    {
        auto response = requestRemoveUserRole(LOGIN, "project/one/subproject/one-A/role/developer");
        EXPECT_EQ(response.status, 200);
        auto body = json::Value::fromString(response.body);
        EXPECT_EQ(body["code"].as<int>(), 0);
    }
}

TEST_F(IdmFixture, test_idm_invalid_parameters)
{
    {
        // Invalid slug path
        auto response = requestAddUserRole(LOGIN, "odd/elements/number");
        EXPECT_EQ(response.status, 400);
        auto body = json::Value::fromString(response.body);
        EXPECT_EQ(body["code"].as<int>(), 1);
    }
    {
        // Wrong slug
        auto response = requestAddUserRole(LOGIN, "project/two/wrong/developer");
        EXPECT_EQ(response.status, 400);
        auto body = json::Value::fromString(response.body);
        EXPECT_EQ(body["code"].as<int>(), 1);
    }
    {
        // Wrong project
        auto response = requestAddUserRole(LOGIN, "project/wrong/role/developer");
        EXPECT_EQ(response.status, 400);
        auto body = json::Value::fromString(response.body);
        EXPECT_EQ(body["code"].as<int>(), 1);
    }
    {
        // Wrong role
        auto response = requestAddUserRole(LOGIN, "project/two/role/wrong");
        EXPECT_EQ(response.status, 400);
        auto body = json::Value::fromString(response.body);
        EXPECT_EQ(body["code"].as<int>(), 1);
    }
}

TEST_F(IdmFixture, test_idm_get_all_roles)
{
    const std::string JOHN = "john";
    const std::string PAUL = "paul";

    requestAddUserRole(JOHN, "project/one/subproject/one-A/role/developer");
    requestAddUserRole(JOHN, "project/one/subproject/one-B/role/developer");
    requestAddUserRole(JOHN, "project/two/role/manager");
    requestAddUserRole(PAUL, "project/one/subproject/one-A/role/developer");
    requestAddUserRole(PAUL, "project/two/role/developer");

    http::MockRequest request(http::GET,
        http::URL("http://localhost/v1/idm/get-all-roles/"));
    auto response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 200);

    auto jsonBody = json::Value::fromString(response.body);
    EXPECT_EQ(jsonBody, json::Value::fromFile(SRC_("idm_users_info.json")));
}

} // maps::factory::backend::tests
