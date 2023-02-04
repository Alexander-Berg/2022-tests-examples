#include <maps/wikimap/mapspro/services/editor/src/observers/sprav_poi_signal.h>

#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>

#include <yandex/maps/wiki/common/robot.h>

namespace maps::wiki::tests {

void createAgentSpravUser()
{
    auto work = cfg()->poolCore().masterWriteableTransaction();
    acl::ACLGateway aclGateway(*work);
    auto user = aclGateway.createUser(maps::wiki::common::WIKIMAPS_SPRAV_UID, "agent", "description", TESTS_USER);
    auto role = aclGateway.createRole("agent", "");
    aclGateway.createPolicy(user, role, aclGateway.aoi(0));
    role.add(aclGateway.rootPermission("mpro"));
    work->commit();
}

Y_UNIT_TEST_SUITE(sprav_poi)
{
WIKI_FIXTURE_TEST_CASE(should_create_sprav_signal_record, EditorTestFixture)
{
    cfg()->initAfterCommitThreadPools();
    createAgentSpravUser();
    auto observers = makeObservers<SpravPoiObserver>();
    performSaveObjectRequestJsonStr(
        loadFile("tests/data/create_poi_food.json"),
        observers,
        maps::wiki::common::WIKIMAPS_SPRAV_UID);
    cfg()->stopAfterCommitThreadPools();
    auto txnSocial = cfg()->poolSocial().masterReadOnlyTransaction();
    auto rows = txnSocial->exec("SELECT data_json FROM sprav.merge_poi_recent_patches");
    UNIT_ASSERT_EQUAL(rows.size(), 1);
    std::optional<poi_feed::FeedObjectData> signalData;
    UNIT_ASSERT_NO_EXCEPTION(signalData = poi_feed::FeedObjectData(rows[0][0].as<std::string>()));
    UNIT_ASSERT(signalData->ftTypeId());
    UNIT_ASSERT_EQUAL(*signalData->ftTypeId(), 180);
    UNIT_ASSERT_EQUAL(signalData->names().size(), 1);
    UNIT_ASSERT_EQUAL(signalData->shortNames().size(), 0);
    UNIT_ASSERT_EQUAL(signalData->permalink(), 89324234);
}
} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
