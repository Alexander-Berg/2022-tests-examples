#include <maps/wikimap/mapspro/services/editor/src/serialize/save_object_parser.h>

#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>

#include <maps/wikimap/mapspro/libs/acl/include/restricted_users.h>
#include <yandex/maps/wiki/social/gateway.h>

namespace maps::wiki::tests {

Y_UNIT_TEST_SUITE(restricted_users)
{
WIKI_FIXTURE_TEST_CASE(restricted_users, EditorTestFixture)
{
    using namespace acl;

    const std::string REASON = "comments.60";

    auto branchCtx = BranchContextFacade::acquireRead(0, "");

    UNIT_ASSERT(!isUserRestricted(branchCtx.txnCore(), TESTS_USER));
    restrictUser(branchCtx.txnCore(), TESTS_USER, REASON);
    UNIT_ASSERT(isUserRestricted(branchCtx.txnCore(), TESTS_USER));

    UNIT_CHECK_GENERATED_EXCEPTION(
        performSaveObjectRequest("tests/data/create_ad_el.xml"), // performed by TESTS_USER
        LogicException);

    unrestrictUser(branchCtx.txnCore(), TESTS_USER);
    UNIT_ASSERT(!isUserRestricted(branchCtx.txnCore(), TESTS_USER));

    UNIT_CHECK_GENERATED_NO_EXCEPTION(
        performSaveObjectRequest("tests/data/create_ad_el.xml"), // performed by TESTS_USER
        LogicException);
}
} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
