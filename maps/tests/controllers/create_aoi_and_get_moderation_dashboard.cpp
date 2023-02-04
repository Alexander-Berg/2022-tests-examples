#include <maps/wikimap/mapspro/services/editor/src/configs/config.h>
#include <maps/wikimap/mapspro/services/editor/src/moderation.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/formatter.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/save_object_parser.h>
#include <maps/wikimap/mapspro/services/editor/src/utils.h>

#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>

#include <maps/libs/json/include/value.h>
#include <maps/libs/pgpool/include/pgpool3.h>
#include <maps/wikimap/mapspro/libs/acl/include/aclgateway.h>
#include <yandex/maps/wiki/revision/revisionsgateway.h>
#include <yandex/maps/wiki/social/gateway.h>

#include <tuple>

namespace maps::wiki::tests {

Y_UNIT_TEST_SUITE(create_aoi_and_get_moderation_dashboard)
{
WIKI_FIXTURE_TEST_CASE(test_create_aoi_and_get_moderation_dashboard, EditorTestFixture)
{
    performSaveObjectRequest("tests/data/create_aoi.xml");

    using namespace social;
    using namespace moderation;

    std::vector<ModerationMode> allModes;

    TOid aoiId = 0;
    TCommitId commitId = 0;
    {
        auto work = cfg()->poolCore().masterWriteableTransaction();
        revision::RevisionsGateway revGateway(*work);
        auto snapshot = revGateway.snapshot(revGateway.headCommitId());
        auto revIds = snapshot.revisionIdsByFilter(
            revision::filters::Attr("cat:aoi").defined());
        WIKI_TEST_REQUIRE_EQUAL(revIds.size(), 1);
        aoiId = revIds.front().objectId();
        commitId = revIds.front().commitId();

        acl::ACLGateway aclGateway(*work);
        auto user = aclGateway.user(TESTS_USER);
        UNIT_ASSERT_NO_EXCEPTION(user.checkActiveStatus());
    }

    auto checkCounters = [&](
            ModerationMode mode,
            const std::tuple<int, int, int, int>& counters)
    {
        SocialModerationDashboard controller(
            {TESTS_USER, mode, SocialModerationDashboard::OutputType::Flat, "", boost::none, boost::none});
        auto formatter = Formatter::create(common::FormatType::JSON);
        auto result = (*formatter)(*controller());
        validateJsonResponse(result, "SocialModerationDashboard");

        auto json = json::Value::fromString(result);
        UNIT_ASSERT_EQUAL(json["mode"].toString(), toAclRoleName(mode));
        const auto& regions = json["regions"];
        WIKI_TEST_REQUIRE(regions.isArray());
        if (!std::get<0>(counters)) {
            UNIT_ASSERT(regions.empty());
            return;
        }

        WIKI_TEST_REQUIRE_EQUAL(regions.size(), 1);
        const auto& region = regions[0];
        UNIT_ASSERT_EQUAL(region["id"].toString(), std::to_string(aoiId));
        UNIT_ASSERT_EQUAL(region["title"].toString(), "test aoi");
        UNIT_ASSERT_EQUAL(region["moderators"].as<int>(), std::get<0>(counters));
        UNIT_ASSERT_EQUAL(region["totalCount"].as<int>(), std::get<1>(counters));
        UNIT_ASSERT(
            (std::get<1>(counters) == 0) ==
                region["oldestTaskActiveSince"].toString().empty());
        UNIT_ASSERT_EQUAL(region["recentNew"].as<int>(), std::get<2>(counters));
        UNIT_ASSERT_EQUAL(region["recentProcessed"].as<int>(), std::get<3>(counters));
    };

    checkCounters(ModerationMode::Moderator, std::make_tuple(0, 0, 0, 0));
    checkCounters(ModerationMode::SuperModerator, std::make_tuple(0, 0, 0, 0));
    checkCounters(ModerationMode::Supervisor, std::make_tuple(0, 0, 0, 0));

    setModerationRole(TESTS_USER, ModerationMode::Moderator, aoiId);
    checkCounters(ModerationMode::Moderator, std::make_tuple(1, 0, 0, 0));
    checkCounters(ModerationMode::SuperModerator, std::make_tuple(0, 0, 0, 0));
    checkCounters(ModerationMode::Supervisor, std::make_tuple(0, 0, 0, 0));

    setModerationRole(TESTS_USER, ModerationMode::Supervisor, aoiId);
    checkCounters(ModerationMode::Moderator, std::make_tuple(1, 0, 0, 0));
    checkCounters(ModerationMode::SuperModerator, std::make_tuple(0, 0, 0, 0));
    checkCounters(ModerationMode::Supervisor, std::make_tuple(1, 0, 0, 0));

    {
        auto work = cfg()->poolSocial().masterWriteableTransaction();
        social::Gateway gateway(*work);
        social::CommitData commitData(
                revision::TRUNK_BRANCH_ID, commitId, "action", "[0, 0, 1, 1]");
        social::PrimaryObjectData objData(888, "urban_areal", "label", "notes");
        auto event = gateway.createCommitEvent(
                TESTS_USER, commitData, objData, {aoiId});
        auto task = gateway.createTask(event, USER_CREATED_OR_UNBANNED_TIME);
        work->commit();
    }
    checkCounters(ModerationMode::Moderator, std::make_tuple(1, 1, 1, 0));
    checkCounters(ModerationMode::SuperModerator, std::make_tuple(0, 0, 0, 0));
    checkCounters(ModerationMode::Supervisor, std::make_tuple(1, 0, 0, 0));

    {
        auto work = cfg()->poolSocial().masterWriteableTransaction();
        social::Gateway gateway(*work);
        auto console = gateway.superModerationConsole(TESTS_USER);
        console.resolveEditTasksByCommitIds(ResolveResolution::Edit, {commitId});
        work->commit();
        adjustTasksResolveTimeBySupervisorDelay();
    }
    checkCounters(ModerationMode::Moderator, std::make_tuple(1, 0, 1, 1));
    checkCounters(ModerationMode::SuperModerator, std::make_tuple(0, 0, 0, 0));
    checkCounters(ModerationMode::Supervisor, std::make_tuple(1, 1, 1, 0));
}
} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
