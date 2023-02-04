#include <maps/factory/libs/idm/db/project_role_gateway.h>
#include <maps/factory/libs/idm/db/project_gateway.h>
#include <maps/factory/libs/idm/db/role_gateway.h>
#include <maps/factory/libs/idm/db/user_role_gateway.h>
#include <maps/factory/libs/idm/tests/fixture.h>

#include <library/cpp/testing/gtest/gtest.h>

namespace maps::factory::idm::db {
using introspection::operator==;
} // namespace maps::factory::idm::db

namespace maps::factory::idm::db {
using introspection::operator==;
} // namespace maps::factory::idm::db

namespace maps::factory::idm::tests {

using namespace db::table::alias;

TEST_F(Fixture, test_db_projects)
{
    replaceData();
    db::Project root = db::ProjectGateway(*txnHandle()).loadOne(_Project::key == "_root");

    EXPECT_EQ(root.id(), 1);
    EXPECT_EQ(root.nameRu(), "Система");
    EXPECT_EQ(root.nameEn(), "System");
    EXPECT_EQ(root.slug(), "project");
    EXPECT_EQ(root.slugNameRu(), "Проект");
    EXPECT_EQ(root.slugNameEn(), "Project");
}

TEST_F(Fixture, test_idm_roles)
{
    replaceData();
    auto txn = txnHandle();
    auto project = db::ProjectGateway(*txn)
        .loadOne(_Project::key == "two");

    auto projectRoles = db::ProjectRoleGateway(*txn)
        .load(_ProjectRole::projectId == project.id());
    EXPECT_EQ(projectRoles.size(), 2u);

    std::vector<int64_t> ids;
    for (auto projectRole: projectRoles) {
        ids.push_back(projectRole.roleId());
    }

    auto roles = db::RoleGateway(*txn).load(
        _Role::id.in(ids),
        sql_chemistry::orderBy(_Role::id));
    EXPECT_EQ(roles.size(), 2u);

    const auto& viewer = roles.at(0);
    EXPECT_EQ(viewer.key(), "developer");
    EXPECT_EQ(viewer.roleSet(), "developer");

    const auto& editor = roles.at(1);
    EXPECT_EQ(editor.key(), "manager");
    EXPECT_EQ(editor.roleSet(), std::nullopt);
}

TEST_F(Fixture, test_db_user_roles)
{
    replaceData();
    auto txn = txnHandle();
    auto testProject = db::ProjectGateway(*txn).loadOne(_Project::key == "two");
    auto testProjectRoles = db::ProjectRoleGateway(*txn).load(
        _ProjectRole::projectId == testProject.id());
    auto testRoles = db::RoleGateway(*txn).load();

    std::map<std::string, int64_t> projectRoleByKey;
    for (const auto& role: testRoles) {
        auto itr = std::find_if(
            testProjectRoles.begin(),
            testProjectRoles.end(),
            [&](const db::ProjectRole& pr) { return pr.roleId() == role.id(); });
        if (itr != testProjectRoles.end()) {
            projectRoleByKey.emplace(role.key(), itr->id());
        }
    }

    EXPECT_TRUE(projectRoleByKey.find("developer") != projectRoleByKey.end());
    EXPECT_TRUE(projectRoleByKey.find("manager") != projectRoleByKey.end());

    db::UserRoles testUserRoles{
        {"morpheus", projectRoleByKey["developer"]},
        {"neo", projectRoleByKey["manager"]}
    };
    db::UserRoleGateway{*txn}.insert(testUserRoles);
    txn->commit();

    {
        // Load roles in test project
        auto txn = txnHandle();
        auto userRoles = db::UserRoleGateway{*txn}.load(
            _UserRole::projectRoleId == _ProjectRole::id &&
            _ProjectRole::projectId == testProject.id(),
            sql_chemistry::orderBy(_UserRole::login));
        ASSERT_EQ(userRoles.size(), 2u);
        EXPECT_EQ(userRoles, testUserRoles);
    }

    // Load developer ProjectRole id
    int64_t developerRoleId{0};
    {
        auto txn = txnHandle();
        developerRoleId = db::ProjectRoleGateway{*txn}.loadOne(
            _ProjectRole::projectId == testProject.id() &&
            _ProjectRole::roleId == _Role::id &&
            _Role::key == "developer").id();
    }

    {
        // Revoke developer role
        auto txn = txnHandle();
        db::UserRoleGateway{*txn}.remove(
            _UserRole::projectRoleId == developerRoleId &&
            _UserRole::login == "morpheus");
        txn->commit();
    }

    {
        // Load roles in test project
        auto txn = txnHandle();
        auto userRoles = db::UserRoleGateway{*txn}.load(
            _UserRole::projectRoleId == _ProjectRole::id &&
            _ProjectRole::projectId == testProject.id());
        ASSERT_EQ(userRoles.size(), 1u);
        EXPECT_EQ(userRoles[0], testUserRoles[1]);
    }

    db::UserRole newUserRole{"trinity", developerRoleId};
    testUserRoles.push_back(newUserRole);

    {
        // Add developer role in test project
        auto txn = txnHandle();
        db::UserRoleGateway{*txn}.insert(newUserRole);
        txn->commit();
    }

    {
        // Check roles
        auto txn = txnHandle();

        auto userRoles = db::UserRoleGateway{*txn}.load(
            _UserRole::projectRoleId == _ProjectRole::id &&
            _ProjectRole::projectId == testProject.id(),
            sql_chemistry::orderBy(_UserRole::login));
        ASSERT_EQ(userRoles.size(), 2u);
        EXPECT_EQ(userRoles[0], testUserRoles[1]);
        EXPECT_EQ(userRoles[1], testUserRoles[2]);
    }
}

} // namespace maps::factory::idm::tests
