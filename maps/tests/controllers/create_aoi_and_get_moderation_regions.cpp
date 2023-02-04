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

namespace maps::wiki::tests {

Y_UNIT_TEST_SUITE(create_aoi_and_get_moderation_regions)
{
WIKI_FIXTURE_TEST_CASE(test_create_aoi_and_get_moderation_regions, EditorTestFixture)
{
    performSaveObjectRequest("tests/data/create_aoi.xml");

    using namespace social;
    using namespace moderation;

    std::list<boost::optional<ModerationMode>> allModes;

    const auto moderationModeStr = toAclRoleName(ModerationMode::Moderator);
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
        auto aoi = aclGateway.aoi(aoiId);

        auto user = aclGateway.user(TESTS_USER);
        UNIT_ASSERT_NO_EXCEPTION(user.checkActiveStatus());

        for (const auto& pair : moderation::getModerationModeResolver()) {
            UNIT_ASSERT_NO_EXCEPTION(aclGateway.role(pair.first));
            allModes.push_back(pair.second);
        }

        auto role = aclGateway.role(moderationModeStr);
        UNIT_ASSERT_NO_EXCEPTION(aclGateway.createPolicy(user, role, aoi));
        work->commit();
    }

    allModes.push_back(boost::optional<ModerationMode>()); // non-filtered mode
    for (auto it = allModes.begin(); it != allModes.end(); ++it) {
        const auto& testModerationMode = *it;
        SocialModerationRegions::Request controllerRequest{
            TESTS_USER, "", testModerationMode, /*eventType=*/boost::none, /*createdBy=*/boost::none, /*primaryObjectId=*/boost::none, aoiId, /*countsByCategoryGroup=*/false};
        SocialModerationRegions controller(controllerRequest);
        auto formatter = Formatter::create(common::FormatType::JSON);

        if (testModerationMode &&
                moderationModeStr != toAclRoleName(*testModerationMode)) {

            catchLogicException([&] { (*formatter)(*controller()); }, ERR_FORBIDDEN);
            continue;
        }
        std::string result;
        UNIT_ASSERT_NO_EXCEPTION(result = (*formatter)(*controller()));
        validateJsonResponse(result, "SocialModerationRegions");

        auto json = json::Value::fromString(result);
        const auto& regions = json["regions"];
        WIKI_TEST_REQUIRE(regions.isArray());

        WIKI_TEST_REQUIRE_EQUAL(regions.size(), 1);
        const auto& region = regions[0];
        UNIT_ASSERT_EQUAL(region["mode"].toString(), moderationModeStr);
        const auto& geoObject = region["geoObject"];
        WIKI_TEST_REQUIRE(geoObject.isObject());
        UNIT_ASSERT_EQUAL(geoObject["id"].toString(), std::to_string(aoiId));
        UNIT_ASSERT_EQUAL(geoObject["categoryId"].toString(), CATEGORY_AOI);
        UNIT_ASSERT_EQUAL(geoObject["title"].toString(), "test aoi");
        const auto& geom = geoObject["geometry"];
        WIKI_TEST_REQUIRE(geom.isObject());
        UNIT_ASSERT_EQUAL(geom["type"].toString(), "Polygon");
        UNIT_ASSERT(geom["coordinates"].isArray());
    }

    auto getStat = [&]()
    {
        GetSocialModerationStat controller({TESTS_USER, ""});
        auto formatter = Formatter::create(common::FormatType::JSON);
        std::string result;
        UNIT_ASSERT_NO_EXCEPTION(result = (*formatter)(*controller()));
        validateJsonResponse(result, "SocialModerationStat");
        auto json = json::Value::fromString(result);
        UNIT_ASSERT_EQUAL(json["uid"].toString(), std::to_string(TESTS_USER));
        return json["hasTasks"].toString();
    };

    UNIT_ASSERT_EQUAL(getStat(), "false"); // current mode Moderator
    {
        auto work = cfg()->poolSocial().masterWriteableTransaction();
        social::Gateway gateway(*work);
        social::CommitData commitData(
                revision::TRUNK_BRANCH_ID, commitId, "action", "[0,0,1,1]");
        social::PrimaryObjectData objData(777, "bld", "label", "notes");
        auto event = gateway.createCommitEvent(
                TESTS_USER, commitData, objData, {aoiId});
        auto task = gateway.createAcceptedTask(event, USER_CREATED_OR_UNBANNED_TIME);
        work->commit();

        adjustTasksResolveTimeBySupervisorDelay();
    }
    UNIT_ASSERT_EQUAL(getStat(), "false");
    setModerationRole(TESTS_USER, social::ModerationMode::SuperModerator, aoiId);
    UNIT_ASSERT_EQUAL(getStat(), "true");
    setModerationRole(TESTS_USER, social::ModerationMode::Supervisor, aoiId);
    UNIT_ASSERT_EQUAL(getStat(), "true");

    {
        SocialModerationRegions::Request controllerRequest{
            TESTS_USER, "", social::ModerationMode::Supervisor,
            /*eventType=*/boost::none, /*createdBy=*/boost::none, /*primaryObjectId=*/boost::none, aoiId, /*countsByCategoryGroup=*/true};
        SocialModerationRegions controller(controllerRequest);
        auto formatter = Formatter::create(common::FormatType::JSON);
        std::string result = (*formatter)(*controller());
        validateJsonResponse(result, "SocialModerationRegions");
        auto json = json::Value::fromString(result);
        auto bldCounters =
            json["regions"][0]["taskCountersByCategoryGroup"]["bld_group"];
        UNIT_ASSERT_EQUAL(bldCounters["available"].as<size_t>(), 1);
        UNIT_ASSERT_EQUAL(bldCounters["acquired"].as<size_t>(), 0);
        UNIT_ASSERT_EQUAL(bldCounters["old"].as<size_t>(), 0);
        UNIT_ASSERT_EQUAL(bldCounters["total"].as<size_t>(), 1);
    };
}
} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
