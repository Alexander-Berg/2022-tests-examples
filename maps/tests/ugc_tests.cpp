#include "fixture.h"
#include "data.h"
#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/tests_data.h>
#include <maps/libs/http/include/test_utils.h>
#include <maps/libs/log8/include/log8.h>
#include <maps/indoor/libs/db/include/barrier_gateway.h>
#include <maps/indoor/libs/db/include/task.h>
#include <maps/indoor/libs/db/include/task_gateway.h>
#include <maps/indoor/libs/db/include/assignment.h>
#include <maps/indoor/libs/db/include/assignment_gateway.h>

#include <maps/libs/geolib/include/multipolygon.h>
#include <maps/libs/geolib/include/polygon.h>
#include <yandex/maps/proto/common2/geometry.sproto.h>
#include <yandex/maps/proto/common2/response.sproto.h>
#include <yandex/maps/proto/mrc/indoor/indoor.sproto.h>
#include <yandex/maps/proto/mrc/ugc/ugc.sproto.h>
#include <maps/infra/yacare/include/yacare.h>
#include <maps/infra/yacare/include/test_utils.h>

using namespace std::chrono_literals;

namespace pgeometry = yandex::maps::sproto::common2::geometry;
namespace presponse = yandex::maps::sproto::common2::response;
namespace pmetadata = yandex::maps::sproto::common2::metadata;
namespace pindoor = yandex::maps::sproto::mrc::indoor;
namespace pugc = yandex::maps::sproto::mrc::ugc;

using namespace yacare::tests;

namespace maps::mirc::agent::tests {

namespace {

const std::string USER_ID = "111111111";
const std::string TEST_TASK_NAME = "Task name";
const std::string TEST_INDOOR_PLAN_ID = "indoor_plan_id";
const geolib3::Point2 TEST_POINT{0,0};
const geolib3::Polyline2 TEST_POLYLINE{
    geolib3::PointsVector{
        {0, 0},
        {0, 1},
        {1, 1},
        {1, 0}
    }};

db::ugc::Task makeTestTask()
{
    db::ugc::Task task{};
    task
        .setStatus(db::ugc::TaskStatus::Available)
        .setDuration(24h * 80)
        .setDistanceInMeters(40000000)
        .setGeodeticPoint(TEST_POINT)
        .setIndoorPlanId(TEST_INDOOR_PLAN_ID);
    task.addName(maps::mirc::db::ugc::localeFromString("Ru-ru"), TEST_TASK_NAME);
    return task;
}

db::ugc::TaskPath makeTestTaskPath()
{
    db::ugc::TaskPath taskPath{};
    taskPath
        .setDuration(24h * 40)
        .setDistanceInMeters(20000000)
        .setGeodeticPolyline(TEST_POLYLINE)
        .setIndoorLevelId("test_level")
        .setDescription("test_description");
    return taskPath;
}

} // namespace

Y_UNIT_TEST_SUITE_F(indoor_assignment_tests, Fixture) {

Y_UNIT_TEST(tasks_search_big_bbox_test)
{
    {
        auto txn = pgPool().masterWriteableTransaction();
        auto task = makeTestTask();
        db::ugc::TaskGateway{*txn}.insert(task);

        txn->commit();
    }

    auto generateSearchRequest = [](maps::geolib3::Point2 point,
                                    maps::geolib3::Vector2 span) {
        auto ll = std::to_string(point.x()) + "," + std::to_string(point.y());
        auto spn = std::to_string(span.x()) + "," + std::to_string(span.y());
        return http::MockRequest(
            http::GET, http::URL("http://localhost/indoor/tasks/search")
                           .addParam("spn", spn)
                           .addParam("ll", ll)
                           .addParam("lang", "ru_RU"));
    };

    std::vector<std::pair<maps::geolib3::Point2, maps::geolib3::Vector2>> requests = {
        {maps::geolib3::Point2{0.0, 0.0}, maps::geolib3::Vector2{120.0, 120.0}},
        {maps::geolib3::Point2{90.0, 0.0}, maps::geolib3::Vector2{130.0, 150.0}},
        {maps::geolib3::Point2{90.0, 90.0}, maps::geolib3::Vector2{160.0, 160.0}},
        {maps::geolib3::Point2{90.0, 90.0}, maps::geolib3::Vector2{180.0, 160.0}},
    };

    for (auto&& [ll, spn] : requests) {
        auto request = generateSearchRequest(ll, spn);
        auto response = yacare::performTestRequest(request);
        ASSERT_TRUE(response.status < 400);
    }
}

Y_UNIT_TEST(tasks_acquire_test)
{
    db::TId taskId = 0;
    {
        auto txn = pgPool().masterWriteableTransaction();
        auto task = makeTestTask();
        db::ugc::TaskGateway{*txn}.insert(task);

        auto taskPath = makeTestTaskPath();
        taskPath.setTaskId(task.id());
        db::ugc::TaskPathGateway{*txn}.insert(taskPath);

        txn->commit();

        taskId = task.id();
    }

    http::MockRequest request(
        http::POST,
        http::URL("http://localhost/indoor/tasks/acquire")
            .addParam("id", taskId)
            .addParam("userId", USER_ID)
            .addParam("lang", "ru_RU"));

    request.headers.emplace(USER_ID_HEADER, USER_ID);
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 201);
}

Y_UNIT_TEST(assignments_list_test)
{
    {
        auto txn = pgPool().masterWriteableTransaction();
        auto task = makeTestTask();
        db::ugc::TaskGateway{*txn}.insert(task);

        auto taskPath = makeTestTaskPath();
        taskPath.setTaskId(task.id());
        db::ugc::TaskPathGateway{*txn}.insert(taskPath);

        auto assignment = task.assignTo(USER_ID);
        db::ugc::AssignmentGateway{*txn}.insert(assignment);
        txn->commit();
    }

    http::MockRequest request(
        http::GET,
        http::URL("http://localhost/indoor/assignments/list")
            .addParam("userId", USER_ID)
            .addParam("lang", "ru_RU"));

    request.headers.emplace(USER_ID_HEADER, USER_ID);
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 200);
}

Y_UNIT_TEST(assignments_complete_not_all_accepted_tracks_test)
{
    db::TId assignmentId = 1;
    db::TId taskId = 0;
    {
        auto txn = pgPool().masterWriteableTransaction();
        auto task = makeTestTask();
        db::ugc::TaskGateway{*txn}.insert(task);

        auto taskPath = makeTestTaskPath();
        taskPath.setTaskId(task.id());
        db::ugc::TaskPathGateway{*txn}.insert(taskPath);

        txn->commit();

        taskId = task.id();
    }

    http::MockRequest accuireRequest(
        http::POST,
        http::URL("http://localhost/indoor/tasks/acquire")
            .addParam("id", taskId)
            .addParam("userId", USER_ID)
            .addParam("lang", "ru_RU"));

    accuireRequest.headers.emplace(USER_ID_HEADER, USER_ID);
    auto response = yacare::performTestRequest(accuireRequest);

    ASSERT_EQ(response.status, 201);

    http::MockRequest completeRequest(
        http::PUT,
        http::URL("http://localhost/indoor/assignments/complete")
            .addParam("id", assignmentId)
            .addParam("userId", USER_ID));

    completeRequest.headers.emplace(USER_ID_HEADER, USER_ID);
    response = yacare::performTestRequest(completeRequest);
    ASSERT_EQ(response.status, 403);
}

Y_UNIT_TEST(assignments_abandon_test)
{
    db::TId assignmentId = 1;
    db::TId taskId = 0;
    {
        auto txn = pgPool().masterWriteableTransaction();
        auto task = makeTestTask();
        db::ugc::TaskGateway{*txn}.insert(task);

        auto taskPath = makeTestTaskPath();
        taskPath.setTaskId(task.id());
        db::ugc::TaskPathGateway{*txn}.insert(taskPath);

        txn->commit();

        taskId = task.id();
    }

    http::MockRequest accuireRequest(
        http::POST,
        http::URL("http://localhost/indoor/tasks/acquire")
            .addParam("id", taskId)
            .addParam("userId", USER_ID)
            .addParam("lang", "ru_RU"));

    accuireRequest.headers.emplace(USER_ID_HEADER, USER_ID);
    auto response = yacare::performTestRequest(accuireRequest);

    ASSERT_EQ(response.status, 201);

    http::MockRequest completeRequest(
        http::PUT,
        http::URL("http://localhost/indoor/assignments/abandon")
            .addParam("id", assignmentId)
            .addParam("userId", USER_ID));

    completeRequest.headers.emplace(USER_ID_HEADER, USER_ID);
    response = yacare::performTestRequest(completeRequest);
    ASSERT_EQ(response.status, 200);
}


Y_UNIT_TEST(assignments_paths_test)
{
    db::TId assignmentId = 1;
    db::TId taskId = 0;
    db::TId taskPathId = 0;
    {
        auto txn = pgPool().masterWriteableTransaction();
        auto task = makeTestTask();
        db::ugc::TaskGateway{*txn}.insert(task);

        auto taskPath = makeTestTaskPath();
        taskPath.setTaskId(task.id());
        db::ugc::TaskPathGateway{*txn}.insert(taskPath);

        txn->commit();

        taskId = task.id();
        taskPathId = taskPath.id();
    }

    http::MockRequest accuireRequest(
        http::POST,
        http::URL("http://localhost/indoor/tasks/acquire")
            .addParam("id", taskId)
            .addParam("userId", USER_ID)
            .addParam("lang", "ru_RU"));

    accuireRequest.headers.emplace(USER_ID_HEADER, USER_ID);
    auto response = yacare::performTestRequest(accuireRequest);

    ASSERT_EQ(response.status, 201);

    http::MockRequest completeRequest(
        http::GET,
        http::URL("http://localhost/indoor/assignments/paths")
            .addParam("id", assignmentId)
            .addParam("lang", "ru_RU")
            .addParam("userId", USER_ID));

    completeRequest.headers.emplace(USER_ID_HEADER, USER_ID);
    response = yacare::performTestRequest(completeRequest);
    ASSERT_EQ(response.status, 200);

    http::MockRequest uploadRequest1(
        http::POST,
        http::URL("http://localhost/indoor/paths/upload")
            .addParam("id", taskPathId)
            .addParam("userId", USER_ID)
            .addParam("lang", "ru_RU"));

    uploadRequest1.body = testdata::indoor_track_only_barriers_pb;
    uploadRequest1.headers.emplace(USER_ID_HEADER, USER_ID);
    response = yacare::performTestRequest(uploadRequest1);
    ASSERT_EQ(response.status, 200);

    {
        auto txn = pgPool().masterWriteableTransaction();
        auto assignmentTaskPath = db::ugc::AssignmentTaskPathGateway{*txn}.loadById(taskPathId);
        ASSERT_EQ(assignmentTaskPath.status(), db::ugc::AssignmentTaskPathStatus::Rejected);
    }

    http::MockRequest uploadRequest2(
        http::POST,
        http::URL("http://localhost/indoor/paths/upload")
            .addParam("id", taskPathId)
            .addParam("userId", USER_ID)
            .addParam("lang", "ru_RU"));

    uploadRequest2.body = testdata::indoor_track_only_checkpoints_pb;
    uploadRequest2.headers.emplace(USER_ID_HEADER, USER_ID);
    response = yacare::performTestRequest(uploadRequest2);
    ASSERT_EQ(response.status, 200);

    {
        auto txn = pgPool().masterWriteableTransaction();
        auto assignmentTaskPath = db::ugc::AssignmentTaskPathGateway{*txn}.loadById(taskPathId);
        ASSERT_EQ(assignmentTaskPath.status(), db::ugc::AssignmentTaskPathStatus::Rejected);
    }
}

Y_UNIT_TEST(assignment_expiration_date_test) 
{
    db::TId taskId = 0;
    {
        auto txn = pgPool().masterWriteableTransaction();
        auto task = makeTestTask();
        db::ugc::TaskGateway{*txn}.insert(task);

        auto taskPath = makeTestTaskPath();
        taskPath.setTaskId(task.id());
        db::ugc::TaskPathGateway{*txn}.insert(taskPath);

        txn->commit();

        taskId = task.id();
    }

    http::MockRequest request(
        http::POST,
        http::URL("http://localhost/indoor/tasks/acquire")
            .addParam("id", taskId)
            .addParam("userId", USER_ID)
            .addParam("lang", "ru_RU"));

    request.headers.emplace(USER_ID_HEADER, USER_ID);
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 201);

    auto txn = pgPool().slaveTransaction();
    auto activeAssignment = db::ugc::AssignmentGateway{*txn}.load(
        db::ugc::table::Assignment::taskId == taskId).back();
    
    auto expirationTime = activeAssignment.expiresAt() - activeAssignment.acquiredAt();
    ASSERT_TRUE(expirationTime <= chrono::Days(15) && expirationTime >= chrono::Days(13));
}

} // Y_UNIT_TEST_SUITE_F

} // namespace maps::mirc::agent::tests
