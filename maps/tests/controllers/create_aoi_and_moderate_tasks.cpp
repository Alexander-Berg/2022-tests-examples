#include <maps/wikimap/mapspro/services/editor/src/configs/config.h>
#include <maps/wikimap/mapspro/services/editor/src/moderation.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/common.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/formatter.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/save_object_parser.h>
#include <maps/wikimap/mapspro/services/editor/src/utils.h>

#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>

#include <maps/libs/json/include/value.h>
#include <maps/wikimap/mapspro/libs/acl/include/aclgateway.h>
#include <yandex/maps/wiki/revision/branch_manager.h>
#include <yandex/maps/wiki/revision/commit_manager.h>
#include <yandex/maps/wiki/social/gateway.h>

#include <chrono>
#include <thread>

namespace maps::wiki::tests {

Y_UNIT_TEST_SUITE(create_aoi_and_moderate_tasks)
{
namespace {

json::Value
acquireTasks(
    social::ModerationMode mode,
    TOid aoiId,
    social::TasksOrder order = social::TasksOrder::NewestFirst,
    size_t limit = 10)
{
    return performAndValidateJson<SocialModerationTasksAcquire>(
        TESTS_USER, aoiId, mode, limit, order);
}

const json::Value&
checkFeedAndGetEvents(
    const json::Value& feed,
    social::FeedType feedType,
    TId subscriber,
    size_t count)
{
    WIKI_TEST_REQUIRE(feed.isObject());
    WIKI_TEST_REQUIRE_EQUAL(feed["feedType"].toString(), boost::lexical_cast<std::string>(feedType));
    WIKI_TEST_REQUIRE_EQUAL(feed["branchId"].toString(), "0");
    WIKI_TEST_REQUIRE_EQUAL(feed["subscriber"].toString(), std::to_string(subscriber));
    const auto& events = feed["events"];
    WIKI_TEST_REQUIRE(events.isArray());
    WIKI_TEST_REQUIRE_EQUAL(events.size(), count);
    return events;
}

void
checkAcquireEmpty(const json::Value& json)
{
    WIKI_TEST_REQUIRE(json.isObject());
    const auto& token = json["token"];
    WIKI_TEST_REQUIRE(token.isString());
    UNIT_ASSERT(!token.toString().empty());
    const auto& tasks = json["tasks"];
    WIKI_TEST_REQUIRE(tasks.isArray());
    WIKI_TEST_REQUIRE_EQUAL(tasks.size(), 0);
}

void
checkAcquireNonEmpty(const json::Value& json)
{
    WIKI_TEST_REQUIRE(json.isObject());
    const auto& token = json["token"];
    WIKI_TEST_REQUIRE(token.isString());
    UNIT_ASSERT(!token.toString().empty());
    const auto& tasks = json["tasks"];
    WIKI_TEST_REQUIRE(tasks.isArray());
    WIKI_TEST_REQUIRE(tasks.size() > 0);
}

void
checkNonEmptyToken(const json::Value& json)
{
    WIKI_TEST_REQUIRE(json.isObject());
    const auto& token = json["token"];
    WIKI_TEST_REQUIRE(token.isString());
    UNIT_ASSERT(!token.toString().empty());
}

json::Value
getFeed(
    TUid uid, social::FeedType feedType, TId subscriber,
    size_t page = 1, size_t perPage = 10,
    TId beforeEventId = 0, TId afterEventId = 0, std::string token = "")
{
    GetRegularSocialFeed::Request controllerRequest{
        uid, revision::TRUNK_BRANCH_ID,
        feedType, subscriber,
        boost::none, // NO CATEGORY GROUPS
        {}, // NO SKIP_CREATED_BY,
        GetRegularSocialFeed::PreApprovedOnly::No,
        page, perPage, beforeEventId, afterEventId, token,
        GetRegularSocialFeed::WithTotal::No,
        boost::optional<maps::chrono::TimePoint>{}, // since
        boost::optional<maps::chrono::TimePoint>{} // till
    };
    GetRegularSocialFeed controller(controllerRequest);
    auto formatter = Formatter::create(common::FormatType::JSON);
    auto result = (*formatter)(*controller());
    validateJsonResponse(result, "GetSocialFeed");
    return json::Value::fromString(result);
}

json::Value
resolveTasks(TOid aoiId, social::ResolveResolution resolution, TOIds ids)
{
    auto result = performAndValidateJson<SocialModerationTasksResolve>(
        UserContext(TESTS_USER, {}), aoiId, resolution, ids);
    adjustTasksResolveTimeBySupervisorDelay();
    return result;
}

json::Value
closeTasks(TOid aoiId, social::CloseResolution resolution, TOIds ids)
{
    return performAndValidateJson<SocialModerationTasksClose>(
        UserContext(TESTS_USER, {}), aoiId, resolution, ids);
}

json::Value
commitRevert(TCommitId commitId, TUid uid)
{
    return performAndValidateJson<CommitsRevert>(
        UserContext(uid, {}),
        commitId,
        /*revertReason*/ boost::none,
        /*feedbackTaskId*/ boost::none);
}

enum class Stage { Create, GetFeed, Acquire, Resolve, Close };

TId
createCommonTestData(const TRevisionId& aoiRevisionId, Stage stage, std::string objCategoryId = "ad_el")
{
    const TRevisionId objRevisionId = getObjRevisionId(objCategoryId);

    auto aoiId = aoiRevisionId.objectId();
    setModerationRole(TESTS_USER, social::ModerationMode::Moderator, aoiId);
    checkAcquireEmpty(acquireTasks(social::ModerationMode::Moderator, aoiId));
    checkAcquireEmpty(acquireTasks(social::ModerationMode::Moderator, aoiId, social::TasksOrder::OldestFirst));

    TId eventId = 0;
    const auto uidStr = std::to_string(TESTS_USER);
    const auto otherUidStr = std::to_string(TESTS_USER2);
    social::CommitData commitData(
        revision::TRUNK_BRANCH_ID, objRevisionId.commitId(),
        "object-created", "[0,0,1,1]");
    social::PrimaryObjectData objectData{
        aoiRevisionId.objectId(), objCategoryId, "test " + objCategoryId, "created"};

    {
        auto work = cfg()->poolSocial().masterWriteableTransaction();
        social::Gateway gw(*work);
        auto event = gw.createCommitEvent(TESTS_USER, commitData, objectData, {aoiId});
        eventId = event.id();
        gw.createTask(event, USER_CREATED_OR_UNBANNED_TIME);
        work->commit();
    }
    if (stage == Stage::Create) {
        return eventId;
    }

    auto checkEvent = [&](const json::Value& event)
    {
        WIKI_TEST_REQUIRE(event.isObject());
        WIKI_TEST_REQUIRE_EQUAL(event["id"].toString(), std::to_string(eventId));
        WIKI_TEST_REQUIRE_EQUAL(event["action"].toString(), commitData.action());
        WIKI_TEST_REQUIRE(!event["date"].toString().empty());

        const auto& data = event["data"];
        WIKI_TEST_REQUIRE(data.isObject());

        const auto& geoObjectCommit = data["commit"];
        WIKI_TEST_REQUIRE(geoObjectCommit.isObject());
        WIKI_TEST_REQUIRE_EQUAL(geoObjectCommit["id"].toString(), std::to_string(commitData.commitId()));
        WIKI_TEST_REQUIRE_EQUAL(geoObjectCommit["uid"].toString(), uidStr);
        WIKI_TEST_REQUIRE(!geoObjectCommit["date"].toString().empty());
        WIKI_TEST_REQUIRE_EQUAL(geoObjectCommit["action"].toString(), commitData.action());

        const auto& geoObject = data["geoObject"];
        WIKI_TEST_REQUIRE(geoObject.isObject());
        WIKI_TEST_REQUIRE_EQUAL(geoObject["id"].toString(), std::to_string(objectData.id()));
        WIKI_TEST_REQUIRE_EQUAL(geoObject["categoryId"].toString(), objectData.categoryId());
        WIKI_TEST_REQUIRE_EQUAL(geoObject["title"].toString(), objectData.screenLabel());
    };

    auto checkFeed = [&](const json::Value& feed)
    {
        const auto& events = checkFeedAndGetEvents(feed, social::FeedType::Aoi, aoiId, 1);
        checkEvent(events[0]);
        WIKI_TEST_REQUIRE_EQUAL(feed["totalCount"].as<size_t>(), 1);
    };

    auto checkFeedRelative = [&](const json::Value& feed)
    {
        const auto& events = checkFeedAndGetEvents(feed, social::FeedType::Aoi, aoiId, 1);
        checkEvent(events[0]);
        WIKI_TEST_REQUIRE_EQUAL(feed["hasMore"].toString(), "false");
    };

    auto checkFeedRelativeEmpty = [&](const json::Value& feed)
    {
        checkFeedAndGetEvents(feed, social::FeedType::Aoi, aoiId, 0);
        WIKI_TEST_REQUIRE_EQUAL(feed["hasMore"].toString(), "false");
    };

    if (stage == Stage::GetFeed) {
        checkFeed(getFeed(TESTS_USER, social::FeedType::Aoi, aoiId));
        checkFeedRelative(getFeed(TESTS_USER, social::FeedType::Aoi, aoiId, 0, 10));
        checkFeedRelative(getFeed(TESTS_USER, social::FeedType::Aoi, aoiId, 0, 10, eventId + 1));  // before
        checkFeedRelativeEmpty(getFeed(TESTS_USER, social::FeedType::Aoi, aoiId, 0, 10, eventId)); // before
        checkFeedRelativeEmpty(getFeed(TESTS_USER, social::FeedType::Aoi, aoiId, 0, 10, 0, eventId)); // after
        return eventId;
    }

    auto checkAcquireNonEmpty = [&](const json::Value& json)
    {
        WIKI_TEST_REQUIRE(json.isObject());
        const auto& token = json["token"];
        WIKI_TEST_REQUIRE(token.isString());
        UNIT_ASSERT(!token.toString().empty());
        const auto& tasks = json["tasks"];
        WIKI_TEST_REQUIRE(tasks.isArray());
        WIKI_TEST_REQUIRE_EQUAL(tasks.size(), 1);

        const auto& task = tasks[0];
        WIKI_TEST_REQUIRE_EQUAL(task["id"].toString(), std::to_string(eventId));

        const auto& locked = task["acquired"];
        WIKI_TEST_REQUIRE(locked.isObject());
        WIKI_TEST_REQUIRE_EQUAL(locked["uid"].toString(), uidStr);
        WIKI_TEST_REQUIRE(!locked["date"].toString().empty());

        checkEvent(task["event"]);
        return task;
    };
    checkAcquireNonEmpty(acquireTasks(social::ModerationMode::Moderator, aoiId));
    if (stage == Stage::Acquire) {
        return eventId;
    }
    checkNonEmptyToken(resolveTasks(aoiId, social::ResolveResolution::Accept, {eventId}));
    checkAcquireEmpty(acquireTasks(social::ModerationMode::Moderator, aoiId));
    if (stage == Stage::Resolve) {
        return eventId;
    }

    setModerationRole(TESTS_USER, social::ModerationMode::Supervisor, aoiId);
    auto json = acquireTasks(social::ModerationMode::Supervisor, aoiId, social::TasksOrder::OldestFirst);
    const auto& task = checkAcquireNonEmpty(json);
    const auto& resolved = task["resolved"];
    WIKI_TEST_REQUIRE(resolved.isObject());
    WIKI_TEST_REQUIRE_EQUAL(resolved["uid"].toString(), uidStr);
    WIKI_TEST_REQUIRE(!resolved["date"].toString().empty());
    WIKI_TEST_REQUIRE_EQUAL(resolved["resolution"].toString(),
        boost::lexical_cast<std::string>(social::ResolveResolution::Edit));

    checkNonEmptyToken(closeTasks(aoiId, social::CloseResolution::Approve, {eventId}));

    //stage == Stage::Close;
    return eventId;
}

json::Value
approveCommit(TCommitId commitId)
{
    return performAndValidateJson<CommitsApprove>(TESTS_USER, commitId);
}

void
checkApproveNonEmpty(const json::Value& json, TCommitId commitId)
{
    WIKI_TEST_REQUIRE(json.isObject());
    checkNonEmptyToken(json);

    const auto& commits = json["commits"];
    WIKI_TEST_REQUIRE(commits.isArray());
    WIKI_TEST_REQUIRE_EQUAL(commits.size(), 1);
    const auto& commit = commits[0];
    WIKI_TEST_REQUIRE(commit.isObject());
    WIKI_TEST_REQUIRE_EQUAL(commit["id"].toString(), std::to_string(commitId));
    WIKI_TEST_REQUIRE_EQUAL(commit["uid"].toString(), std::to_string(TESTS_USER));
    WIKI_TEST_REQUIRE(!commit["date"].toString().empty());
    WIKI_TEST_REQUIRE_EQUAL(commit["action"].toString(), "object-created");
    WIKI_TEST_REQUIRE_EQUAL(commit["sourceBranchId"].toString(), "0");
}

} // namespace


WIKI_FIXTURE_TEST_CASE(test_create_aoi_and_get_feed, EditorTestFixture)
{
    performSaveObjectRequest("tests/data/create_aoi.xml");
    performSaveObjectRequest("tests/data/create_ad_el.xml");
    createCommonTestData(getAoiRevisionId(), Stage::GetFeed);

    auto checkUserFeed = [&](TUid uid, TUid subscriber = TESTS_USER)
    {
        checkFeedAndGetEvents(
            getFeed(uid, social::FeedType::User, subscriber),
            social::FeedType::User, subscriber, 1);
    };

    // invalid subscriber
    catchLogicException([&]() { checkUserFeed(TESTS_USER, 0); }, ERR_BAD_REQUEST);

    checkUserFeed(0);
    checkUserFeed(TESTS_USER); // the same
    checkUserFeed(TESTS_USER2); // another user

    //check hiding cartographers events
    setModerationRole(TESTS_USER, social::ModerationMode::Supervisor, 0);
    catchLogicException([&]() { checkUserFeed(0); }, ERR_FORBIDDEN);
    checkUserFeed(TESTS_USER); // the same
    catchLogicException([&]() { checkUserFeed(TESTS_USER2); }, ERR_FORBIDDEN);

    //check accepted cartographers events by cartographer
    setModerationRole(TESTS_USER2, social::ModerationMode::Supervisor, 0);

    checkUserFeed(TESTS_USER); // the same
    checkUserFeed(TESTS_USER2); // another user
}

WIKI_FIXTURE_TEST_CASE(test_create_aoi_and_approve_after_create, EditorTestFixture)
{
    performSaveObjectRequest("tests/data/create_aoi.xml");
    performSaveObjectRequest("tests/data/create_ad_el.xml");
    auto aoiRevisionId = getAoiRevisionId();
    auto aoiId = aoiRevisionId.objectId();
    auto commitId = aoiRevisionId.commitId();
    createCommonTestData(aoiRevisionId, Stage::Create);

    setModerationRole(TESTS_USER, social::ModerationMode::Supervisor, aoiId);
    checkApproveNonEmpty(approveCommit(commitId), commitId); // auto close not resolved

    checkAcquireEmpty(acquireTasks(social::ModerationMode::Supervisor, aoiId));
}

WIKI_FIXTURE_TEST_CASE(test_create_aoi_and_defer_after_acquire, EditorTestFixture)
{
    performSaveObjectRequest("tests/data/create_aoi.xml");
    performSaveObjectRequest("tests/data/create_ad_el.xml");
    auto aoiRevisionId = getAoiRevisionId();
    auto aoiId = aoiRevisionId.objectId();
    auto eventId = createCommonTestData(aoiRevisionId, Stage::Acquire);
    checkAcquireNonEmpty(acquireTasks(social::ModerationMode::Moderator, aoiId));

    auto json = performAndValidateJson<SocialModerationTasksDefer>(
        TESTS_USER, social::TaskIds{eventId}, "2222-12-31");
    checkNonEmptyToken(json);
    UNIT_ASSERT_EQUAL(json["taskIds"][0].toString(), std::to_string(eventId));
    checkAcquireEmpty(acquireTasks(social::ModerationMode::Moderator, aoiId));
}

WIKI_FIXTURE_TEST_CASE(test_create_aoi_and_approve_after_acquire, EditorTestFixture)
{
    performSaveObjectRequest("tests/data/create_aoi.xml");
    performSaveObjectRequest("tests/data/create_ad_el.xml");
    auto aoiRevisionId = getAoiRevisionId();
    auto aoiId = aoiRevisionId.objectId();
    auto commitId = aoiRevisionId.commitId();
    createCommonTestData(aoiRevisionId, Stage::Acquire);

    setModerationRole(TESTS_USER, social::ModerationMode::Supervisor, aoiId);
    checkApproveNonEmpty(approveCommit(commitId), commitId); // auto close locked task

    checkAcquireEmpty(acquireTasks(social::ModerationMode::Supervisor, aoiId));
}

WIKI_FIXTURE_TEST_CASE(test_create_aoi_and_approve_after_resolve, EditorTestFixture)
{
    performSaveObjectRequest("tests/data/create_aoi.xml");
    performSaveObjectRequest("tests/data/create_ad_el.xml");
    auto aoiRevisionId = getAoiRevisionId();
    auto aoiId = aoiRevisionId.objectId();
    auto commitId = getObjRevisionId("ad_el").commitId();
    createCommonTestData(aoiRevisionId, Stage::Resolve);

    setModerationRole(TESTS_USER, social::ModerationMode::Supervisor, aoiId);
    checkApproveNonEmpty(approveCommit(commitId), commitId); // auto close resolved task

    checkAcquireEmpty(acquireTasks(social::ModerationMode::Supervisor, aoiId));
}

WIKI_FIXTURE_TEST_CASE(test_create_aoi_and_autoapprove_after_resolve, EditorTestFixture)
{
    performSaveObjectRequest("tests/data/create_aoi.xml");
    performSaveObjectRequest("tests/data/create_ad_el.xml");
    auto aoiRevisionId = getAoiRevisionId();
    auto aoiId = aoiRevisionId.objectId();
    setModerationRole(TESTS_USER, social::ModerationMode::Supervisor, aoiId); // cartographer
    createCommonTestData(aoiRevisionId, Stage::Resolve);
    checkAcquireEmpty(acquireTasks(social::ModerationMode::Supervisor, aoiId));
}

WIKI_FIXTURE_TEST_CASE(test_create_aoi_and_approve_after_close, EditorTestFixture)
{
    performSaveObjectRequest("tests/data/create_aoi.xml");
    performSaveObjectRequest("tests/data/create_ad_el.xml");
    auto aoiRevisionId = getAoiRevisionId();
    auto aoiId = aoiRevisionId.objectId();
    auto commitId = aoiRevisionId.commitId();
    createCommonTestData(aoiRevisionId, Stage::Close);
    checkAcquireEmpty(acquireTasks(social::ModerationMode::Supervisor, aoiId));

    // approve commit but task already closed task
    checkApproveNonEmpty(approveCommit(commitId), commitId);

    checkAcquireEmpty(acquireTasks(social::ModerationMode::Supervisor, aoiId));
}

WIKI_FIXTURE_TEST_CASE(test_invalid_requests, EditorTestFixture)
{
    const TOid AOI_ID = 0; // unknown
    catchLogicException(
        [&]() { SocialModerationTasksResolve::Request{
                    {TESTS_USER, {}}, AOI_ID, social::ResolveResolution::Accept, {0,1,2,3}}; },
        ERR_BAD_REQUEST);

    catchLogicException(
        [&]() { SocialModerationTasksResolve::Request{
                    {0, {}}, AOI_ID, social::ResolveResolution::Accept, {1,2,3}}; },
        ERR_BAD_REQUEST);

    catchLogicException(
        [&]() { SocialModerationTasksResolve::Request{
                    {TESTS_USER, {}}, AOI_ID, social::ResolveResolution::Edit, {}}; },
        ERR_BAD_REQUEST);

    catchLogicException(
        [&]() { SocialModerationTasksClose::Request{
                    {TESTS_USER, {}}, AOI_ID, social::CloseResolution::Approve, {0,1,2,3}}; },
        ERR_BAD_REQUEST);

    catchLogicException(
        [&]() { SocialModerationTasksClose::Request{
                    {0, {}}, AOI_ID, social::CloseResolution::Approve, {1,2,3}}; },
        ERR_BAD_REQUEST);

    catchLogicException(
        [&]() { SocialModerationTasksClose::Request{
                    {TESTS_USER, {}}, AOI_ID, social::CloseResolution::Edit, {}}; },
        ERR_BAD_REQUEST);
}

WIKI_FIXTURE_TEST_CASE(test_create_aoi_and_close_after_resolve_autoapproved, EditorTestFixture)
{
    const std::string AUTOAPPROVED_CATEGORY_ID = CATEGORY_AOI;
    WIKI_TEST_REQUIRE(cfg()->editor()->categories()[AUTOAPPROVED_CATEGORY_ID].autoApprove());

    performSaveObjectRequest("tests/data/create_aoi.xml");
    auto aoiRevisionId = getAoiRevisionId();
    auto aoiId = aoiRevisionId.objectId();
    auto taskId = createCommonTestData(aoiRevisionId, Stage::Resolve, AUTOAPPROVED_CATEGORY_ID);
    catchLogicException(
        [&](){ closeTasks(aoiId, social::CloseResolution::Approve, {taskId}); },
        ERR_MODERATION_CONFLICT);
}

WIKI_FIXTURE_TEST_CASE(test_create_aoi_and_commits_revert_after_resolve_self, EditorTestFixture)
{
    performSaveObjectRequest("tests/data/create_aoi.xml");
    performSaveObjectRequest("tests/data/create_ad_el.xml");
    auto aoiRevision = getAoiRevision();
    createCommonTestData(aoiRevision.id(), Stage::Resolve);
    UNIT_ASSERT_EQUAL(aoiRevision.data().deleted, false);
    commitRevert(aoiRevision.id().commitId(), TESTS_USER);
    auto newAoiRevision = getAoiRevision();
    UNIT_ASSERT_EQUAL(newAoiRevision.data().deleted, true);
    UNIT_ASSERT_EQUAL(aoiRevision.id().objectId(), newAoiRevision.id().objectId());
    UNIT_ASSERT(aoiRevision.id().commitId() < newAoiRevision.id().commitId());
}

WIKI_FIXTURE_TEST_CASE(test_create_aoi_and_commits_revert_after_approve, EditorTestFixture)
{
    performSaveObjectRequest("tests/data/create_aoi.xml");
    performSaveObjectRequest("tests/data/create_ad_el.xml");
    auto aoiRevisionId = getAoiRevisionId();
    createCommonTestData(aoiRevisionId, Stage::Resolve);

    // manual approve because to approve commits queue disabled while unit testing
    {
        auto work = cfg()->poolCore().masterWriteableTransaction();
        revision::BranchManager(*work).createApproved(TESTS_USER, {});
        revision::CommitManager(*work).approve(
            revision::DBIDSet{aoiRevisionId.commitId()});
        work->commit();
    }

    catchLogicException(
        [&](){ commitRevert(aoiRevisionId.commitId(), TESTS_USER); },
        ERR_REVERT_NOT_DRAFT_COMMIT);

    setModerationRole(TESTS_USER, social::ModerationMode::Supervisor, aoiRevisionId.objectId());
    commitRevert(aoiRevisionId.commitId(), TESTS_USER); // allowed for superviser
}

WIKI_FIXTURE_TEST_CASE(test_create_aoi_check_conflict_after_resolve, EditorTestFixture)
{
    performSaveObjectRequest("tests/data/create_aoi.xml");
    performSaveObjectRequest("tests/data/create_ad_el.xml");
    auto aoiRevisionId = getAoiRevisionId();
    auto aoiId = aoiRevisionId.objectId();
    auto taskId = createCommonTestData(aoiRevisionId, Stage::Resolve);

    // task already resolved
    catchLogicException(
        [&](){ resolveTasks(aoiId, social::ResolveResolution::Accept, {taskId}); },
        ERR_MODERATION_CONFLICT);
    catchLogicException(
        [&](){ resolveTasks(aoiId, social::ResolveResolution::Revert, {taskId}); },
        ERR_MODERATION_CONFLICT);
}

WIKI_FIXTURE_TEST_CASE(test_create_aoi_and_check_revert_not_owner, EditorTestFixture)
{
    performSaveObjectRequest("tests/data/create_aoi.xml");
    performSaveObjectRequest("tests/data/create_ad_el.xml");
    auto aoiRevisionId = getAoiRevisionId();
    auto aoiId = aoiRevisionId.objectId();
    auto commitId = aoiRevisionId.objectId();
    createCommonTestData(aoiRevisionId, Stage::Resolve);

    // check revert by another user
    // reverter - common, forbidden
    catchLogicException(
        [&](){ commitRevert(commitId, TESTS_USER2); },
        ERR_REVERT_NOT_OWNER);

    // object owner - cartographer
    setModerationRole(TESTS_USER, social::ModerationMode::Supervisor, aoiId);

    // reverter - moderator, forbidden
    setModerationRole(TESTS_USER2, social::ModerationMode::Moderator, aoiId);
    catchLogicException(
        [&](){ commitRevert(commitId, TESTS_USER2); },
        ERR_REVERT_NOT_OWNER);

    // reverter - yandex-moderator, forbidden
    setModerationRole(TESTS_USER2, social::ModerationMode::SuperModerator, aoiId);
    catchLogicException(
        [&](){ commitRevert(commitId, TESTS_USER2); },
        ERR_REVERT_NOT_OWNER);

    // reverter - cartographer, allowed
    setModerationRole(TESTS_USER2, social::ModerationMode::Supervisor, aoiId);
    UNIT_ASSERT_NO_EXCEPTION(commitRevert(commitId, TESTS_USER2));
}

WIKI_FIXTURE_TEST_CASE(test_create_aoi_and_check_conflict_after_close, EditorTestFixture)
{
    performSaveObjectRequest("tests/data/create_aoi.xml");
    performSaveObjectRequest("tests/data/create_ad_el.xml");
    auto aoiRevisionId = getAoiRevisionId();
    auto aoiId = aoiRevisionId.objectId();
    auto taskId = createCommonTestData(aoiRevisionId, Stage::Close);

    catchLogicException(
        [&](){ resolveTasks(aoiId, social::ResolveResolution::Accept, {taskId}); },
        ERR_MODERATION_CONFLICT);

    catchLogicException(
        [&](){ closeTasks(aoiId, social::CloseResolution::Edit, {taskId}); },
        ERR_MODERATION_CONFLICT);

    catchLogicException(
        [&](){ closeTasks(aoiId, social::CloseResolution::Revert, {taskId}); },
        ERR_MODERATION_CONFLICT);
}

WIKI_FIXTURE_TEST_CASE(test_release_tasks, EditorTestFixture)
{
    performSaveObjectRequest("tests/data/create_aoi.xml");
    performSaveObjectRequest("tests/data/create_ad_el.xml");
    auto aoiRevisionId = getAoiRevisionId();
    auto taskId = createCommonTestData(aoiRevisionId, Stage::Acquire);
    auto json = performAndValidateJson<SocialModerationTasksRelease>(TESTS_USER);
    UNIT_ASSERT_EQUAL(json["taskIds"][0].toString(), std::to_string(taskId));
}

WIKI_FIXTURE_TEST_CASE(test_select_moderated_resolved, EditorTestFixture)
{
    performSaveObjectRequest("tests/data/create_aoi.xml");
    performSaveObjectRequest("tests/data/create_ad_el.xml");
    auto aoiRevisionId = getAoiRevisionId();
    createCommonTestData(aoiRevisionId, Stage::Resolve);
    {
        auto json = performAndValidateJsonGetRequest<GetSocialModerationTasks>(
                TESTS_USER,
                "", //token
                social::TId(0), //before
                social::TId(0), //after
                size_t(10), //perPage
                std::optional<std::string>(),
                TESTS_USER, //resolved-by
                std::optional<social::DateTimeCondition>(),
                std::optional<social::ResolveResolution>(),
                std::optional<social::TUid>(),
                std::optional<social::DateTimeCondition>(),
                std::optional<social::CloseResolution>());
        UNIT_ASSERT_EQUAL(json["tasks"].size(), 1); // because self-commit-events excluded from select
        WIKI_TEST_REQUIRE_EQUAL(json["hasMore"].toString(), "false");
    }
    {
        auto json = performAndValidateJsonGetRequest<GetSocialModerationTasks>(
                TESTS_USER2,
                "", //token
                social::TId(0), //before
                social::TId(0), //after
                size_t(10), //perPage
                std::optional<std::string>(),
                TESTS_USER, //resolved-by
                std::optional<social::DateTimeCondition>(),
                std::optional<social::ResolveResolution>(),
                std::optional<social::TUid>(),
                std::optional<social::DateTimeCondition>(),
                std::optional<social::CloseResolution>());
        UNIT_ASSERT_EQUAL(json["tasks"].size(), 1);
        WIKI_TEST_REQUIRE_EQUAL(json["hasMore"].toString(), "false");
    }
}

WIKI_FIXTURE_TEST_CASE(test_select_moderated_closed, EditorTestFixture)
{
    performSaveObjectRequest("tests/data/create_aoi.xml");
    performSaveObjectRequest("tests/data/create_ad_el.xml");
    auto aoiRevisionId = getAoiRevisionId();
    createCommonTestData(aoiRevisionId, Stage::Close);
    auto json = performAndValidateJsonGetRequest<GetSocialModerationTasks>(
            TESTS_USER,
            "", //token
            social::TId(0), //before
            social::TId(0), //after
            size_t(10), //perPage
            std::optional<std::string>(),
            std::optional<social::TUid>(),
            std::optional<social::DateTimeCondition>(),
            std::optional<social::ResolveResolution>(),
            TESTS_USER, //closed-by
            std::optional<social::DateTimeCondition>(),
            std::optional<social::CloseResolution>());
    UNIT_ASSERT_EQUAL(json["tasks"].size(), 1);
    WIKI_TEST_REQUIRE_EQUAL(json["hasMore"].toString(), "false");
}
} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
