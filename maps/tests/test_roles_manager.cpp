#include "helpers.h"

#include <maps/automotive/libs/idm_roles_manager/roles_manager.h>
#include <maps/automotive/libs/interfaces/factory.h>

#include <maps/infra/yacare/include/request.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <library/cpp/testing/gtest/gtest.h>
#include <maps/libs/common/include/exception.h>

namespace maps::automotive::tests {

using namespace ::testing;
using namespace maps::automotive::idm;

const std::string GROUP_SLUG = "some-group";
const std::string ROLE_ID = "important-role";

class TestEnvironment : public NUnitTest::TBaseFixture {
public:
    TestEnvironment() {
        factory_ = std::make_shared<interfaces::Factory>();
        db_ = factory_->addSingleton<MockedDatabase>();

        groupSpec_ = {
                .name = "Test group",
                .slug = GROUP_SLUG,
                .roles = {
                    {ROLE_ID, {
                        .ru = {
                            .name = "Важный человек",
                            .help = "Может важничать"
                        },
                        .en = {
                            .name = "Important role",
                            .help = "Can do important job"
                        }
                    }}
                }
            };

        manager_ = factory_->addSingleton<RolesManager>(groupSpec_);
    }

    MockedDatabase& getDatabase() { return *db_; }
    IRolesManager& getManager() { return *manager_; }
    GroupSpec& getGroupSpec() { return groupSpec_; }

private:
    std::shared_ptr<interfaces::Factory> factory_;
    std::shared_ptr<MockedDatabase> db_;
    std::shared_ptr<IRolesManager> manager_;
    idm::GroupSpec groupSpec_;
};

Y_UNIT_TEST_SUITE_F(test_roles_manager, TestEnvironment) {

Y_UNIT_TEST(test_check_roles_info) {
    {
        yacare::Response response;
        getManager().getRolesInfo(response);

        const auto jsonResponse = maps::json::Value::fromString(response.bodyString());
        const auto spec = getGroupSpec();
        UNIT_ASSERT_VALUES_EQUAL(jsonResponse["code"].as<int>(), 0);
        UNIT_ASSERT_VALUES_EQUAL(jsonResponse["roles"]["slug"].toString(), spec.slug);
        UNIT_ASSERT_VALUES_EQUAL(jsonResponse["roles"]["name"].toString(), spec.name);

        for (const auto& [roleId, descr]: spec.roles) {
            const auto actualRole = jsonResponse["roles"]["values"][roleId];
            UNIT_ASSERT_VALUES_EQUAL(actualRole["name"]["ru"].toString(), descr.ru.name);
            UNIT_ASSERT_VALUES_EQUAL(actualRole["name"]["en"].toString(), descr.en.name);

            UNIT_ASSERT_VALUES_EQUAL(actualRole["help"]["ru"].toString(), descr.ru.help);
            UNIT_ASSERT_VALUES_EQUAL(actualRole["help"]["en"].toString(), descr.en.help);
        }
    }
}

Y_UNIT_TEST(test_add_and_remove_role) {
    const std::string& login = "test-user";

    {
        EXPECT_CALL(getDatabase(), addIdmUserRole(testing::Eq(login), testing::Eq(ROLE_ID)))
            .Times(testing::Exactly(1));

        yacare::RequestBuilder requestBuilder;
        formRequest(requestBuilder, login, GROUP_SLUG, ROLE_ID);

        yacare::Response response;
        getManager().addRole(requestBuilder.request(), response);
        checkResponse(response, 0, "status", "ok");
    }

    {
        EXPECT_CALL(getDatabase(), getAllIdmRoles())
            .Times(testing::Exactly(1))
            .WillOnce([&]() -> idm::UsersWithRoles {
                return {
                    {login, {ROLE_ID}}
                };
            });

        yacare::Response response;
        getManager().getAllRoles(response);

        const auto jsonResponse = maps::json::Value::fromString(response.bodyString());
        UNIT_ASSERT_VALUES_EQUAL(jsonResponse["code"].as<int>(), 0);
        for (const auto& user: jsonResponse["users"]) {
            UNIT_ASSERT_VALUES_EQUAL(user["login"].toString(), login);
            UNIT_ASSERT_VALUES_EQUAL(user["roles"].size(), 1);
            UNIT_ASSERT_VALUES_EQUAL(user["roles"][0][GROUP_SLUG].toString(), ROLE_ID);
        }
    }

    {
        EXPECT_CALL(getDatabase(), deleteIdmUserRole(testing::Eq(login), testing::Eq(ROLE_ID)))
            .Times(testing::Exactly(1));

        yacare::RequestBuilder requestBuilder;
        formRequest(requestBuilder, login, GROUP_SLUG, ROLE_ID);

        yacare::Response response;
        getManager().deleteRole(requestBuilder.request(), response);
        checkResponse(response, 0, "status", "ok");
    }

    {
        EXPECT_CALL(getDatabase(), getAllIdmRoles())
            .Times(testing::Exactly(1))
            .WillOnce([&]() -> idm::UsersWithRoles {
                return {};
            });

        yacare::Response response;
        getManager().getAllRoles(response);

        const auto jsonResponse = maps::json::Value::fromString(response.bodyString());
        UNIT_ASSERT_VALUES_EQUAL(jsonResponse["code"].as<int>(), 0);
        UNIT_ASSERT_VALUES_EQUAL(jsonResponse["users"].size(), 0);
    }
}

Y_UNIT_TEST(test_add_twice) {
    const std::string& login = "test-user";

    EXPECT_CALL(getDatabase(), addIdmUserRole(testing::Eq(login), testing::Eq(ROLE_ID)))
        .Times(testing::Exactly(1));

    yacare::RequestBuilder requestBuilder;
    formRequest(requestBuilder, login, GROUP_SLUG, ROLE_ID);

    {
        yacare::Response response;
        getManager().addRole(requestBuilder.request(), response);
        checkResponse(response, 0, "status", "ok");
    }

    EXPECT_CALL(getDatabase(), addIdmUserRole(testing::Eq(login), testing::Eq(ROLE_ID)))
        .Times(testing::Exactly(1))
        .WillOnce(testing::Throw(IRolesStorage::RoleAlreadyExists()));

    {
        yacare::Response response;
        getManager().addRole(requestBuilder.request(), response);
        checkResponse(response, 3, "warning");
    }
}

Y_UNIT_TEST(test_role_does_not_exits) {
    EXPECT_CALL(getDatabase(), addIdmUserRole(_, _))
        .Times(testing::Exactly(0));

    yacare::RequestBuilder requestBuilder;
    formRequest(requestBuilder, "some login", GROUP_SLUG, "not a role");

    yacare::Response response;
    getManager().addRole(requestBuilder.request(), response);
    checkResponse(response, 2, "error");
}

Y_UNIT_TEST(test_delete_absent_login) {
    const std::string& login = "test-user";

    EXPECT_CALL(getDatabase(), deleteIdmUserRole(testing::Eq(login), testing::Eq(ROLE_ID)))
        .Times(testing::Exactly(1))
        .WillOnce(testing::Throw(IRolesStorage::RoleDoesNotExist()));

    yacare::RequestBuilder requestBuilder;
    formRequest(requestBuilder, login, GROUP_SLUG, ROLE_ID);

    yacare::Response response;
    getManager().deleteRole(requestBuilder.request(), response);
    checkResponse(response, 2, "error");
}

} // Y_UNIT_TEST_SUITE_F

} // namespace maps::automotive::tests
