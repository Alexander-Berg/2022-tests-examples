#include <maps/factory/libs/idm/common.h>
#include <maps/factory/libs/idm/slug_path.h>
#include <maps/factory/libs/idm/idm_service.h>
#include <maps/factory/libs/idm/tests/fixture.h>

#include <library/cpp/testing/gtest/gtest.h>

namespace maps::factory::idm {
using introspection::operator==;
using introspection::operator<;
} // namespace maps::factory::idm

namespace maps::factory::idm::tests {

namespace {

const SlugPath ONE_A_DEVELOPER = parseSlugPath("project/one/subproject/one-A/role/developer");
const SlugPath ONE_A_MANAGER = parseSlugPath("project/one/subproject/one-A/role/manager");
const SlugPath ONE_B_DEVELOPER = parseSlugPath("project/one/subproject/one-B/role/developer");
const SlugPath TWO_DEVELOPER = parseSlugPath("project/two/role/developer");
const SlugPath TWO_MANAGER = parseSlugPath("project/two/role/manager");

const Login JOHN = "john";
const Login PAUL = "paul";
const Login GEORGE = "george";

} // namespace

TEST_F(Fixture, test_user_roles)
{
    replaceData();
    {
        auto txn = txnHandle();
        auto slugPaths = IdmService(*txn, JOHN).getRoles();
        EXPECT_TRUE(slugPaths.empty());
    }

    {
        // Add some role
        auto txn = txnHandle();
        IdmService(*txn, JOHN).addRole(ONE_A_DEVELOPER);
        txn->commit();

        txn = txnHandle();
        auto slugPaths = IdmService(*txn, JOHN).getRoles();
        ASSERT_EQ(slugPaths.size(), 1u);
        EXPECT_EQ(slugPaths[0], ONE_A_DEVELOPER);
    }

    {
        // Add some more roles
        auto txn = txnHandle();
        IdmService(*txn, JOHN)
            .addRole(ONE_A_MANAGER)
            .addRole(TWO_DEVELOPER);
        txn->commit();

        txn = txnHandle();
        auto slugPaths = IdmService(*txn, JOHN).getRoles();
        std::set<SlugPath> received(slugPaths.begin(), slugPaths.end());
        std::set<SlugPath> expected{ONE_A_DEVELOPER, ONE_A_MANAGER, TWO_DEVELOPER};
        EXPECT_EQ(received, expected);
    }

    {
        // Remove role
        auto txn = txnHandle();
        IdmService(*txn, JOHN).removeRole(ONE_A_MANAGER);
        txn->commit();

        txn = txnHandle();
        auto slugPaths = IdmService(*txn, JOHN).getRoles();
        std::set<SlugPath> received(slugPaths.begin(), slugPaths.end());
        std::set<SlugPath> expected{ONE_A_DEVELOPER, TWO_DEVELOPER};
        EXPECT_EQ(received, expected);
    }

    {
        // Add already existing role
        auto txn = txnHandle();
        EXPECT_NO_THROW(IdmService(*txn, JOHN).addRole(ONE_A_DEVELOPER));

        txn = txnHandle();
        EXPECT_EQ(IdmService(*txn, JOHN).getRoles().size(), 2u);
    }

    {
        // Remove non-existing role
        auto txn = txnHandle();
        EXPECT_NO_THROW(IdmService(*txn, JOHN).removeRole(ONE_A_MANAGER));
        txn->commit();

        txn = txnHandle();
        EXPECT_EQ(IdmService(*txn, JOHN).getRoles().size(), 2u);
    }
}

TEST_F(Fixture, test_all_user_roles)
{
    replaceData();
    auto txn = txnHandle();
    IdmService(*txn, JOHN)
        .addRole(ONE_A_DEVELOPER)
        .addRole(ONE_B_DEVELOPER);
    IdmService(*txn, PAUL)
        .addRole(ONE_A_DEVELOPER)
        .addRole(ONE_A_MANAGER);
    IdmService(*txn, GEORGE)
        .addRole(TWO_DEVELOPER);
    txn->commit();

    txn = txnHandle();
    auto allRoles = IdmService::getAllRoles(*txn);
    ASSERT_EQ(allRoles.size(), 3u);

    std::set<SlugPath> receivedJohn(allRoles[JOHN].begin(), allRoles[JOHN].end());
    std::set<SlugPath> expectedJohn{ONE_A_DEVELOPER, ONE_B_DEVELOPER};
    EXPECT_EQ(receivedJohn, expectedJohn);

    std::set<SlugPath> receivedPaul(allRoles[PAUL].begin(), allRoles[PAUL].end());
    std::set<SlugPath> expectedPaul{ONE_A_DEVELOPER, ONE_A_MANAGER};
    EXPECT_EQ(receivedPaul, expectedPaul);

    ASSERT_EQ(allRoles[GEORGE].size(), 1u);
    EXPECT_EQ(allRoles[GEORGE][0], TWO_DEVELOPER);
}

TEST_F(Fixture, test_check_user_role)
{
    replaceData();
    const Id PROJECT_ID_ONE_A = 4;
    const Id PROJECT_ID_ONE_B = 5;
    const std::string DEVELOPER = "developer";
    const std::string MANAGER = "manager";

    auto txn = txnHandle();
    EXPECT_FALSE(IdmService(*txn, JOHN).hasRole(DEVELOPER, PROJECT_ID_ONE_A));
    EXPECT_FALSE(IdmService(*txn, JOHN).hasRole(DEVELOPER, PROJECT_ID_ONE_B));

    IdmService(*txn, JOHN).addRole(ONE_A_DEVELOPER);
    txn->commit();

    txn = txnHandle();
    EXPECT_TRUE(IdmService(*txn, JOHN).hasRole(DEVELOPER, PROJECT_ID_ONE_A));
    EXPECT_FALSE(IdmService(*txn, JOHN).hasRole(MANAGER, PROJECT_ID_ONE_A));
    EXPECT_FALSE(IdmService(*txn, JOHN).hasRole(DEVELOPER, PROJECT_ID_ONE_B));
    EXPECT_FALSE(IdmService(*txn, PAUL).hasRole(DEVELOPER, PROJECT_ID_ONE_A));

    EXPECT_FALSE(IdmService(*txn, JOHN).hasRole("unknown", PROJECT_ID_ONE_A));

    EXPECT_TRUE(IdmService(*txn, JOHN).hasOneOfRoles({DEVELOPER, MANAGER}, PROJECT_ID_ONE_A));
    EXPECT_FALSE(IdmService(*txn, JOHN).hasOneOfRoles({MANAGER}, PROJECT_ID_ONE_A));
    EXPECT_FALSE(IdmService(*txn, JOHN).hasOneOfRoles({}, PROJECT_ID_ONE_A));

    IdmService(*txn, JOHN).removeRole(ONE_A_MANAGER);
    txn->commit();

    EXPECT_TRUE(IdmService(*txnHandle(), JOHN).hasRole(DEVELOPER, PROJECT_ID_ONE_A));
}

} // namespace maps::factory::idm::tests
