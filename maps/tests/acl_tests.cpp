#include <maps/factory/libs/idm/common.h>
#include <maps/factory/libs/idm/idm_acl.h>
#include <maps/factory/libs/idm/slug_path.h>
#include <maps/factory/libs/idm/idm_service.h>
#include <maps/factory/libs/idm/tests/fixture.h>

#include <maps/factory/libs/idm/db/project_gateway.h>

#include <library/cpp/testing/gtest/gtest.h>

namespace maps::factory::idm::tests {

using namespace db::table::alias;

namespace {

const Login JOHN = "john";
const Login PAUL = "paul";
const Login GEORGE = "george";

const SlugPath VIEWER = parseSlugPath("project/mapsfactory/role/viewer");
const SlugPath EDITOR = parseSlugPath("project/mapsfactory/role/editor");

} // namespace

TEST_F(Fixture, test_factory_acl)
{
    const auto addRoles = [&]() {
        auto txn = txnHandle();
        IdmService(*txn, JOHN).addRole(EDITOR);
        IdmService(*txn, PAUL).addRole(VIEWER);
        txn->commit();
    };
    ASSERT_NO_THROW(addRoles());

    auto txn = txnHandle();
    const Id factoryProjectId = db::ProjectGateway(*txn)
        .loadOne(_Project::key == "mapsfactory").id();

    EXPECT_NO_THROW(checkAcl(*txn, JOHN, IdmRole::Editor, factoryProjectId));
    EXPECT_NO_THROW(checkAcl(*txn, JOHN, IdmRole::Viewer, factoryProjectId));
    EXPECT_THROW(checkAcl(*txn, PAUL, IdmRole::Editor, factoryProjectId), Forbidden);
    EXPECT_NO_THROW(checkAcl(*txn, PAUL, IdmRole::Viewer, factoryProjectId));
    EXPECT_THROW(checkAcl(*txn, GEORGE, IdmRole::Editor, factoryProjectId), Forbidden);
    EXPECT_THROW(checkAcl(*txn, GEORGE, IdmRole::Viewer, factoryProjectId), Forbidden);
}

} // namespace maps::factory::idm::tests
