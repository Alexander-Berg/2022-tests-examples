#include "controller_tests_common_includes.h"

#include <maps/wikimap/mapspro/services/editor/src/actions/social/comments/clear.h>
#include <maps/wikimap/mapspro/services/editor/src/configs/config.h>
#include <maps/wikimap/mapspro/services/editor/src/moderation.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/formatter.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/save_object_parser.h>
#include <maps/wikimap/mapspro/services/editor/src/utils.h>

#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>

#include <maps/libs/pgpool/include/pgpool3.h>
#include <maps/wikimap/mapspro/libs/acl/include/aclgateway.h>
#include <maps/wikimap/mapspro/libs/acl/include/exception.h>
#include <yandex/maps/wiki/revision/revisionsgateway.h>
#include <yandex/maps/wiki/social/gateway.h>

#include <tuple>

namespace maps::wiki::tests {
namespace {

const std::string DATA = "data";
const TId TEST_COMMIT_ID = 0;
const TId OBJECT_ID = 0;

} // namespace

Y_UNIT_TEST_SUITE(clear_user_comments)
{

WIKI_FIXTURE_TEST_CASE(clear_user_comments, EditorTestFixture)
{
    using namespace social;

    auto txnSocialWrite = cfg()->poolSocial().masterWriteableTransaction();
    Gateway socialGateway(*txnSocialWrite);

    populateACLPermissionsTree();
    TUid user = createRandomUser(); // user with no permissions

    auto commentCreatedByUser = socialGateway.createComment(
        user, CommentType::Info, DATA,
        TEST_COMMIT_ID, OBJECT_ID,
        std::nullopt,
        {});

    auto commentCreatedByTestsUser = socialGateway.createComment(
        TESTS_USER, CommentType::Info, DATA,
        TEST_COMMIT_ID, OBJECT_ID,
        std::nullopt,
        {});
    txnSocialWrite->commit();

    auto json = performAndValidateJson<ClearUserComments>(
        UserContext(TESTS_USER, {}),
        user
    );

    UNIT_ASSERT_EQUAL(json["count"].toString(), "1");
    {
        auto txnSocial = cfg()->poolSocial().masterWriteableTransaction();
        UNIT_ASSERT(!commentCreatedByUser.deleteBy(*txnSocial, TESTS_USER));
    }

    UNIT_CHECK_GENERATED_EXCEPTION(
        performAndValidateJson<ClearUserComments>(
            UserContext(user, {}),
            TESTS_USER
        ),
        acl::AccessDenied
    );
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
