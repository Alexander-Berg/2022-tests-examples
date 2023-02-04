#include <maps/factory/libs/idm/common.h>
#include <maps/factory/libs/idm/slug_path.h>
#include <maps/factory/libs/idm/role_tree.h>
#include <maps/factory/libs/idm/tests/fixture.h>

#include <maps/factory/libs/idm/db/project_gateway.h>
#include <maps/factory/libs/idm/db/role_gateway.h>
#include <maps/factory/libs/idm/db/project_role_gateway.h>

#include <maps/libs/json/include/value.h>

#include <library/cpp/testing/gtest/gtest.h>

namespace maps::factory::idm {
using introspection::operator==;
} // namespace maps::factory::idm

namespace maps::factory::idm::db {
using introspection::operator==;
} // namespace maps::factory::idm::db

namespace maps::factory::idm::tests {

TEST(SlugPathTests, test_slug_conversion)
{
    std::vector<std::pair<std::string, SlugPath>> strAndPathPairs{
        {"", SlugPath{}},
        {"/role/president", SlugPath{{"role", "president"}}},
        {"/country/Russia/role/president",
            SlugPath{{"country", "Russia"}, {"role", "president"}}}
    };

    for (const auto&[str, path]: strAndPathPairs) {
        EXPECT_EQ(toString(path), str);
        const auto parsedPath = parseSlugPath(str);
        EXPECT_EQ(parsedPath, path);
    }

    std::string invalid = "/odd/tokens/number";
    EXPECT_THROW(parseSlugPath(invalid), BadParameter);
}

/**
 * The tests below use the following hierarchy of roles:
 *
 *                 _root
 *            ______/  \_________
 *           /                   \
 *       project 2           project 1
 *        /   \                 /   \___________
 *       /     \               /                \
 *      /       \         subProject 1A     subProject 1B
 *     /         \          /      \              \
 * developer  manager   developer  manager     developer
 */
TEST_F(Fixture, test_role_tree_basic)
{
    replaceData();
    auto txn = txnHandle();
    const auto roleTree = RoleTree::load(*txn);

    for (Id id: {2, 3, 4, 5}) {
        const auto& node = roleTree.getNode(id);
        auto project = db::ProjectGateway{*txn}.loadById(id);
        EXPECT_EQ(node.project(), project);
    }

    {
        const auto& root = roleTree.root();
        EXPECT_EQ(root.id(), 1);
        Ids subprojectIds{2, 3};
        EXPECT_EQ(root.childIds(), subprojectIds);
        EXPECT_TRUE(root.leafChildIds().empty());
    }

    {
        const auto& projectOne = roleTree.getNode(2);
        Ids subprojectIds{4, 5};
        EXPECT_EQ(projectOne.childIds(), subprojectIds);
        EXPECT_TRUE(projectOne.leafChildIds().empty());
    }

    {
        const auto& projectTwo = roleTree.getNode(3);
        Ids projectRoleIds{10, 11};
        EXPECT_TRUE(projectTwo.childIds().empty());
        EXPECT_EQ(projectTwo.leafChildIds(), projectRoleIds);
    }

    {
        const auto& subProjectA = roleTree.getNode(4);
        Ids projectRoleIds{12, 13};
        EXPECT_TRUE(subProjectA.childIds().empty());
        EXPECT_EQ(subProjectA.leafChildIds(), projectRoleIds);
    }

    {
        const auto& subProjectB = roleTree.getNode(5);
        Ids projectRoleIds{14};
        EXPECT_TRUE(subProjectB.childIds().empty());
        EXPECT_EQ(subProjectB.leafChildIds(), projectRoleIds);
    }

    auto projectRoles = db::ProjectRoleGateway{*txn}.load();
    for (const auto& projectRole: projectRoles) {
        Id id = projectRole.id();
        Id projectId = projectRole.projectId();
        Id roleId = projectRole.roleId();

        const auto& leafNode = roleTree.getLeafNode(id);
        EXPECT_EQ(leafNode.parentId(), projectId);
        auto role = db::RoleGateway{*txn}.loadById(roleId);
        EXPECT_EQ(leafNode.role(), role);
    }
}

TEST_F(Fixture, test_slug_path_search)
{
    replaceData();
    auto txn = txnHandle();
    const auto roleTree = RoleTree::load(*txn);

    std::vector<std::pair<Id, std::string>> roleTreeAndSlugPaths{
        {10, "/project/two/role/developer"},
        {11, "/project/two/role/manager"},
        {12, "/project/one/subproject/one-A/role/developer"},
        {13, "/project/one/subproject/one-A/role/manager"},
        {14, "/project/one/subproject/one-B/role/developer"}
    };

    for (const auto&[id, strPath]: roleTreeAndSlugPaths) {
        // Check id -> path
        auto path = roleTree.slugPathByLeafNodeId(id);
        EXPECT_EQ(toString(path), strPath);

        // Check path -> id
        auto optionalId = roleTree.leafNodeIdBySlugPath(parseSlugPath(strPath));
        ASSERT_TRUE(optionalId);
        EXPECT_EQ(*optionalId, id);
    }
}

TEST_F(Fixture, test_role_tree_json)
{
    replaceData();
    auto txn = txnHandle();

    const auto roleTree = RoleTree::load(*txn);
    json::Builder builder;
    builder << [&](json::ObjectBuilder builder) {
        roleTree.toJson(builder);
    };
    auto rolesInfo = builder.str();

    EXPECT_EQ(json::Value::fromString(rolesInfo),
        json::Value::fromFile(SRC_("roles_info.json")));
}

} // namespace maps::factory::idm::tests
