#include <maps/wikimap/mapspro/services/editor/src/configs/config.h>
#include <maps/wikimap/mapspro/services/editor/src/observers/social.h>
#include <maps/wikimap/mapspro/services/editor/src/observers/view_syncronizer.h>
#include <maps/wikimap/mapspro/services/editor/src/revisions_facade.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/formatter.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/save_object_parser.h>
#include <maps/wikimap/mapspro/services/editor/src/utils.h>

#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>

#include <maps/libs/json/include/value.h>

namespace maps::wiki::tests {

namespace {

void checkModerationTasksForVegetationChangedByModerator(
    const std::string& createJsonFilepath,
    bool isInImportantRegion,
    unsigned int tasksSize)
{
    auto observers = makeObservers<ViewSyncronizer>();

    // create aoi for moderation
    performSaveObjectRequest("tests/data/create_aoi.xml", observers);

    // create test objects
    performSaveObjectRequest(createJsonFilepath, observers);

    // create important region for important changes
    if (isInImportantRegion) {
        performSaveObjectRequest("tests/data/create_diffalert_region.xml", observers);
    }

    // get vegetation id
    auto branchCtx = BranchContextFacade::acquireRead(0, "");
    ObjectsCache cache(branchCtx, boost::none);
    auto revsVeg = cache.revisionsFacade().snapshot().revisionIdsByFilter(
        revision::filters::Attr("cat:vegetation").defined());
    WIKI_TEST_REQUIRE_EQUAL(revsVeg.size(), 1);
    TOid vegId = revsVeg.begin()->objectId();

    // set moderation role
    auto aoiRevisionId = getAoiRevisionId();
    auto aoiId = aoiRevisionId.objectId();
    setModerationRole(TESTS_USER, social::ModerationMode::Moderator, aoiId);

    // delete vegetation
    observers.add(std::make_shared<SocialObserver>());
    ObjectsUpdateState::Request deleteRequest(
        {TESTS_USER, {}}, vegId, "deleted", 0, "", common::FormatType::JSON);
    UNIT_ASSERT_NO_EXCEPTION(
        ObjectsUpdateState(observers, deleteRequest)());
    adjustTasksResolveTimeBySupervisorDelay();

    // check moderation tasks
    setModerationRole(TESTS_USER2, social::ModerationMode::Supervisor, aoiId);

    auto tasksJson = performAndValidateJson<SocialModerationTasksAcquire>(
        TESTS_USER2, aoiId, social::ModerationMode::Supervisor,
        10u, social::TasksOrder::NewestFirst);

    const auto& tasks = tasksJson["tasks"];
    WIKI_TEST_REQUIRE(tasks.isArray());
    WIKI_TEST_REQUIRE_EQUAL(tasks.size(), tasksSize);
}

std::vector<TId>
getTasksIds(const json::Value& json)
{
    WIKI_TEST_REQUIRE(json.isObject());
    const auto& token = json["token"];
    WIKI_TEST_REQUIRE(token.isString());
    UNIT_ASSERT(!token.toString().empty());
    const auto& tasks = json["tasks"];
    WIKI_TEST_REQUIRE(tasks.isArray());

    std::vector<TId> taskIds;
    for (const auto& task : tasks) {
        taskIds.push_back(boost::lexical_cast<TId>(task["id"].toString()));
    }
    return taskIds;
}

void
checkTodayProcessedCount(TUid uid, size_t todayProcessedCount)
{
    auto result = performJsonGetRequest<SocialModerationTasksStat>(uid, -180, "");
    validateJsonResponse(result, "SocialModerationTasksStat");
    auto json = json::Value::fromString(result);
    WIKI_TEST_REQUIRE(json.isObject());
    const auto& count = json["taskCounters"]["today"];
    WIKI_TEST_REQUIRE(count.isInteger());
    WIKI_TEST_REQUIRE_EQUAL(count.as<size_t>(), todayProcessedCount);
}

void checkModerationTasksForVegetationChangedByUser(
    const std::string& createJsonFilepath,
    const std::string& editJsonFilepath,
    bool isInImportantRegion,
    unsigned int supervisorTasksSize)
{
    auto observers = makeObservers<ViewSyncronizer>();

    // create aoi for moderation
    performSaveObjectRequest("tests/data/create_aoi.xml", observers);

    // create test objects
    performSaveObjectRequest(createJsonFilepath, observers);

    // create important region for important changes
    if (isInImportantRegion) {
        performSaveObjectRequest("tests/data/create_diffalert_region.xml", observers);
    }

    // change test objects as ordinary user
    observers.add(std::make_shared<SocialObserver>());
    performSaveObjectRequest(editJsonFilepath, observers);

    //--------------------------------

    // set moderation role
    auto aoiRevisionId = getAoiRevisionId();
    auto aoiId = aoiRevisionId.objectId();
    setModerationRole(TESTS_USER2, social::ModerationMode::Moderator, aoiId);

    checkTodayProcessedCount(TESTS_USER2, 0);

    // get moderation tasks
    auto taskIds = getTasksIds(performAndValidateJson<SocialModerationTasksAcquire>(
        TESTS_USER2, aoiId, social::ModerationMode::Moderator,
        10u, social::TasksOrder::NewestFirst));
    WIKI_TEST_REQUIRE(!taskIds.empty());

    // resolve moderation task
    auto taskId = taskIds[0];
    performAndValidateJson<SocialModerationTasksResolve>(
        UserContext(TESTS_USER2, {}),
        aoiId, social::ResolveResolution::Accept, social::TIds{taskId});
    adjustTasksResolveTimeBySupervisorDelay();

    // check that there is no more moderation tasks
    taskIds = getTasksIds(performAndValidateJson<SocialModerationTasksAcquire>(
        TESTS_USER2, aoiId, social::ModerationMode::Moderator,
        10u, social::TasksOrder::NewestFirst));
    WIKI_TEST_REQUIRE(taskIds.empty());

    checkTodayProcessedCount(TESTS_USER2, 1);

    //--------------------------------

    // set supervisor role
    setModerationRole(TESTS_USER2, social::ModerationMode::Supervisor, aoiId);

    checkTodayProcessedCount(TESTS_USER2, 1);

    // get moderation tasks
    taskIds = getTasksIds(performAndValidateJson<SocialModerationTasksAcquire>(
        TESTS_USER2, aoiId, social::ModerationMode::Supervisor,
        10u, social::TasksOrder::NewestFirst));
    WIKI_TEST_REQUIRE(taskIds.size() == supervisorTasksSize);

    // close moderation tasks if necessary
    if (supervisorTasksSize > 0) {
        performAndValidateJson<SocialModerationTasksClose>(
            UserContext(TESTS_USER2, {}), aoiId, social::CloseResolution::Approve, social::TIds{taskId});

        taskIds = getTasksIds(performAndValidateJson<SocialModerationTasksAcquire>(
            TESTS_USER2, aoiId, social::ModerationMode::Supervisor,
            10u, social::TasksOrder::NewestFirst));
        WIKI_TEST_REQUIRE(taskIds.empty());
    }

    checkTodayProcessedCount(TESTS_USER2, 1);
}

} // namespace

Y_UNIT_TEST_SUITE(change_vegetation_and_moderate_tasks)
{
WIKI_FIXTURE_TEST_CASE(test_delete_small_object_to_moderate, EditorTestFixture)
{
    checkModerationTasksForVegetationChangedByModerator("tests/data/create_small_vegetation.json", false, 1);
}

WIKI_FIXTURE_TEST_CASE(test_delete_small_object_to_moderate_in_important_region, EditorTestFixture)
{
    checkModerationTasksForVegetationChangedByModerator("tests/data/create_small_vegetation.json", true, 1);
}

WIKI_FIXTURE_TEST_CASE(test_delete_big_object_to_moderate, EditorTestFixture)
{
    checkModerationTasksForVegetationChangedByModerator("tests/data/create_big_vegetation.json", false, 1);
}

WIKI_FIXTURE_TEST_CASE(test_delete_big_object_to_moderate_in_important_region, EditorTestFixture)
{
    checkModerationTasksForVegetationChangedByModerator("tests/data/create_big_vegetation.json", true, 1);
}

WIKI_FIXTURE_TEST_CASE(test_change_small_object_to_moderate, EditorTestFixture)
{
    checkModerationTasksForVegetationChangedByUser(
        "tests/data/create_small_vegetation.json",
        "tests/data/edit_small_vegetation.json",
        false,
        1);
}

WIKI_FIXTURE_TEST_CASE(test_change_small_object_to_moderate_in_important_region, EditorTestFixture)
{
    checkModerationTasksForVegetationChangedByUser(
        "tests/data/create_small_vegetation.json",
        "tests/data/edit_small_vegetation.json",
        true,
        1);
}

WIKI_FIXTURE_TEST_CASE(test_change_big_object_to_moderate, EditorTestFixture)
{
    checkModerationTasksForVegetationChangedByUser(
        "tests/data/create_big_vegetation.json",
        "tests/data/edit_big_vegetation.json",
        false,
        1);
}

WIKI_FIXTURE_TEST_CASE(test_change_big_object_to_moderate_in_important_region, EditorTestFixture)
{
    checkModerationTasksForVegetationChangedByUser(
        "tests/data/create_big_vegetation.json",
        "tests/data/edit_big_vegetation.json",
        true,
        1);
}
} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
