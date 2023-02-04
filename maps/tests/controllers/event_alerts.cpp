#include <maps/wikimap/mapspro/services/editor/src/observers/social.h>
#include <maps/wikimap/mapspro/services/editor/src/observers/view_syncronizer.h>

#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>

namespace maps::wiki::tests {

Y_UNIT_TEST_SUITE(event_alerts)
{
WIKI_FIXTURE_TEST_CASE(test_event_alerts, EditorTestFixture)
{
    auto observers = makeObservers<ViewSyncronizer>();
    performSaveObjectRequest("tests/data/create_aoi.xml", observers);

    observers.add(ObserverPtr(new SocialObserver()));
    performSaveObjectRequest("tests/data/create_test_country.json", observers);

    TId aoiId = getAoiRevisionId().objectId();
    auto mode = social::ModerationMode::Moderator;
    setModerationRole(TESTS_USER2, mode, aoiId);

    auto tasksJson = performAndValidateJson<SocialModerationTasksAcquire>(
            TESTS_USER2, aoiId, mode,
            10u, social::TasksOrder::NewestFirst);

    WIKI_TEST_REQUIRE_EQUAL(tasksJson["tasks"].size(), 1);
    // check that at least one alert is generated
    UNIT_ASSERT(tasksJson["tasks"][0]["alerts"].size());
}


WIKI_FIXTURE_TEST_CASE(should_add_attrs_to_commit_event, EditorTestFixture)
{
    auto observers = makeObservers<ViewSyncronizer>();
    performSaveObjectRequest("tests/data/create_aoi.xml", observers);

    observers.add(ObserverPtr(new SocialObserver()));
    performSaveObjectRequest("tests/data/create_poi_food.json", observers);

    auto txn = cfg()->poolSocial().masterReadOnlyTransaction();
    social::Gateway gateway(*txn);
    auto events = gateway.loadEditEventsWithExtraDataByCommitIds(
        {getObjRevisionId("poi_food").commitId()}
    );
    UNIT_ASSERT_EQUAL(events.size(), 1);

    auto extraData = events.front().extraData();
    WIKI_TEST_REQUIRE(extraData);
    WIKI_TEST_REQUIRE(extraData->ftTypeId);
    UNIT_ASSERT_EQUAL(*extraData->ftTypeId, 180);
    WIKI_TEST_REQUIRE(extraData->businessRubricId);
    UNIT_ASSERT_EQUAL(*extraData->businessRubricId, 123456789);
}
} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
