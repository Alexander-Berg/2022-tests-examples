#include "common.h"
#include "data.h"
#include "fixture.h"

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>
#include <maps/indoor/libs/db/include/task_gateway.h>
#include <maps/indoor/libs/db/include/assignment_gateway.h>
#include <maps/indoor/libs/db/include/radiomap_transmitter_gateway.h>
#include <maps/infra/yacare/include/test_utils.h>
#include <maps/libs/stringutils/include/join.h>
#include <maps/libs/stringutils/include/to_string.h>
#include <maps/libs/tile/include/tile.h>
#include <yandex/maps/proto/mrc/indoor/transmitters.sproto.h>
#include <yandex/maps/proto/mrc/indoor/metatask.sproto.h>
#include <yandex/maps/geolib3/sproto.h>

#include <maps/indoor/radiomap-capturer-admin/lib/serialization.h>

using namespace yacare::tests;

namespace pindoor = yandex::maps::sproto::mrc::indoor;

namespace maps::mirc::admin::tests {
namespace {

const std::string USER_ID = "111111111";
const std::string TRANSMITTER_ID = "transmitterId";
const db::ugc::TransmitterType TRANSMITTER_TYPE = db::ugc::TransmitterType::Beacon;
const db::ugc::SignalModelParameters TRANSMITTER_MODEL = db::ugc::SignalModelParameters{10.0, 0.5, 10.0, -90.0};
const std::string TEST_TASK_NAME = "Task name";
const std::string TEST_INDOOR_PLAN_ID = "11111";
const std::string TEST_INDOOR_LEVEL_ID = "2222";
const geolib3::Point2 TEST_POINT{10,20};

const geolib3::Point2 TEST_POINT_REAL_1{37.349689, 55.691695};
const tile::Tile TEST_TILE_1 = tile::Tile(316538, 164521, 19);
const geolib3::Point2 TEST_POINT_REAL_2{37.347672, 55.691865};
const tile::Tile TEST_TILE_2 = tile::Tile(316535, 164521, 19);

db::ugc::Transmitters makeTestTransmitters(unsigned int size)
{
    db::ugc::Transmitters transmitters;
    for (unsigned int i = 0; i < size; ++i) {
        transmitters.emplace_back(
            TEST_INDOOR_PLAN_ID,
            TEST_INDOOR_LEVEL_ID,
            TRANSMITTER_TYPE,
            TRANSMITTER_ID,
            TEST_POINT_REAL_1,
            TRANSMITTER_MODEL,
            false);
        transmitters.emplace_back(
            TEST_INDOOR_PLAN_ID,
            TEST_INDOOR_LEVEL_ID,
            TRANSMITTER_TYPE,
            TRANSMITTER_ID,
            TEST_POINT_REAL_2,
            TRANSMITTER_MODEL,
            false);
    }
    return transmitters;
}

db::ugc::Tasks makeTestTasks(
    unsigned int pendingTasks,
    unsigned int availableTasks,
    unsigned int doneTasks,
    unsigned int cancelledTasks)
{
    db::ugc::Tasks tasks{};

    for(unsigned int i = 0; i < pendingTasks; ++i) {
        db::ugc::Task task;
        task.setStatus(db::ugc::TaskStatus::Pending)
            .setDistanceInMeters(40000000)
            .setGeodeticPoint(TEST_POINT)
            .setIndoorPlanId(TEST_INDOOR_PLAN_ID);
        tasks.emplace_back(std::move(task));
    }

    for(unsigned int i = 0; i < availableTasks; ++i) {
        db::ugc::Task task;
        task.setStatus(db::ugc::TaskStatus::Available)
            .setDistanceInMeters(40000000)
            .setGeodeticPoint(TEST_POINT)
            .setIndoorPlanId(TEST_INDOOR_PLAN_ID);
        tasks.emplace_back(std::move(task));
    }

    for(unsigned int i = 0; i < doneTasks; ++i) {
        db::ugc::Task task;
        task.setStatus(db::ugc::TaskStatus::Done)
            .setDistanceInMeters(40000000)
            .setGeodeticPoint(TEST_POINT)
            .setIndoorPlanId(TEST_INDOOR_PLAN_ID);
        tasks.emplace_back(std::move(task));
    }

    for(unsigned int i = 0; i < cancelledTasks; ++i) {
        db::ugc::Task task;
        task.setStatus(db::ugc::TaskStatus::Cancelled)
            .setDistanceInMeters(40000000)
            .setGeodeticPoint(TEST_POINT)
            .setIndoorPlanId(TEST_INDOOR_PLAN_ID);
        tasks.emplace_back(std::move(task));
    }
    return tasks;
}

}

Y_UNIT_TEST_SUITE_F(indoor_path_generate_tests, Fixture) {

Y_UNIT_TEST(indoor_path_generate_test)
{
    auto generateRequest = [](const std::string& indoorPlanId,
                              const std::string& name,
                              const maps::geolib3::Point2& point,
                              std::string intervals) {
        auto ll = std::to_string(point.x()) + "," + std::to_string(point.y());
        auto request = http::MockRequest(
            http::POST,
            http::URL("http://localhost/indoor/tasks/generate")
                .addParam("indoor_plan_id", indoorPlanId)
                .addParam("name", name)
                .addParam("ll", ll)
                .addParam("intervals", intervals)
                .addParam("userId", USER_ID));

        request.body = testdata::indoor_tasks_generate_pb;
        request.headers.emplace(USER_ID_HEADER, USER_ID);
        return request;
    };

    std::vector<std::string> intervals = {"1", "02", "t3", "4", "T5"};
    auto request = generateRequest(TEST_INDOOR_PLAN_ID, TEST_TASK_NAME, TEST_POINT, stringutils::join(intervals, ","));

    auto response = yacare::performTestRequest(request);

    ASSERT_EQ(response.status, 200);
    EXPECT_TRUE(getTokenHeader(response));

    auto txn = pgPool().slaveTransaction();
    db::ugc::TaskGateway gtw{*txn};
    auto tasks = gtw.load(sql_chemistry::orderBy(db::ugc::table::Task::id));

    ASSERT_EQ(tasks.size(), intervals.size());

    gtw.loadPaths(tasks);
    size_t i = 0;
    for(const auto& task : tasks) {
        ASSERT_EQ(task.status(), db::ugc::TaskStatus::Pending);
        ASSERT_EQ(task.paths().size(), 2u);

        // T/t tokens are ignored!
        ASSERT_EQ(task.skipEvaluation(), true);
        ASSERT_EQ(task.isTest(), true);
        ++i;
    }
}

Y_UNIT_TEST(indoor_path_regenerate_test)
{
    std::vector<std::string> intervals = {"1"};
    auto generateRequest = http::MockRequest(
        http::POST,
        http::URL("http://localhost/indoor/tasks/generate")
            .addParam("indoor_plan_id", TEST_INDOOR_PLAN_ID)
            .addParam("name", TEST_TASK_NAME)
            .addParam("ll", std::to_string(TEST_POINT.x()) + "," + std::to_string(TEST_POINT.y()))
            .addParam("intervals", stringutils::join(intervals, ","))
            .addParam("userId", USER_ID)
    );

    generateRequest.body = testdata::indoor_tasks_generate_pb;
    generateRequest.headers.emplace(USER_ID_HEADER, USER_ID);

    auto generateResponse = yacare::performTestRequest(generateRequest);

    ASSERT_EQ(generateResponse.status, 200);
    EXPECT_TRUE(getTokenHeader(generateResponse));

    auto regenerateRequest = http::MockRequest(
        http::POST,
        http::URL("http://localhost/indoor/tasks/regenerate")
            .addParam("indoor_plan_id", TEST_INDOOR_PLAN_ID)
            .addParam("name", TEST_TASK_NAME)
            .addParam("ll", std::to_string(TEST_POINT.x()) + "," + std::to_string(TEST_POINT.y()))
            .addParam("userId", USER_ID)
    );

    regenerateRequest.body = testdata::indoor_tasks_generate2_pb;
    regenerateRequest.headers.emplace(USER_ID_HEADER, USER_ID);

    auto regenerateResponse = yacare::performTestRequest(regenerateRequest);

    ASSERT_TRUE(regenerateResponse.status == 200);
    EXPECT_TRUE(getTokenHeader(regenerateResponse));

    auto txn = pgPool().slaveTransaction();
    db::ugc::TaskGateway gtw{*txn};
    auto tasks = gtw.load(sql_chemistry::orderBy(db::ugc::table::Task::id));

    ASSERT_EQ(tasks.size(), 2u);

    gtw.loadPaths(tasks);
    ASSERT_EQ(tasks[0].status(), db::ugc::TaskStatus::Cancelled);
    ASSERT_EQ(tasks[0].paths().size(), 2u);
    ASSERT_EQ(tasks[1].status(), db::ugc::TaskStatus::Pending);
    ASSERT_EQ(tasks[1].paths().size(), 1u);
}

Y_UNIT_TEST(indoor_path_generate_test_without_intervals)
{
    auto generateRequest = [](const std::string& indoorPlanId,
                              const std::string& name,
                              const maps::geolib3::Point2& point) {
        auto ll = std::to_string(point.x()) + "," + std::to_string(point.y());
        auto request = http::MockRequest(
            http::POST,
            http::URL("http://localhost/indoor/tasks/generate")
                .addParam("indoor_plan_id", indoorPlanId)
                .addParam("name", name)
                .addParam("ll", ll)
                .addParam("userId", USER_ID)
            );
        request.body = testdata::indoor_tasks_generate_pb;
        request.headers.emplace(USER_ID_HEADER, USER_ID);
        return request;
    };

    auto request = generateRequest(TEST_INDOOR_PLAN_ID, TEST_TASK_NAME, TEST_POINT);
    auto response = yacare::performTestRequest(request);

    ASSERT_EQ(response.status, 200);
    EXPECT_TRUE(getTokenHeader(response));

    auto txn = pgPool().slaveTransaction();
    db::ugc::TaskGateway gtw{*txn};
    auto tasks = gtw.load();
    ASSERT_EQ(tasks.size(), 1u);

    gtw.loadPaths(tasks);
    for(const auto& task : tasks) {
        ASSERT_EQ(task.status(), db::ugc::TaskStatus::Pending);
        ASSERT_EQ(task.paths().size(), 2u);
    }
}

Y_UNIT_TEST(indoor_path_generate_test_invalid_intervals)
{
    auto generateRequest = [](const std::string& indoorPlanId,
                              const std::string& name,
                              const maps::geolib3::Point2& point,
                              std::string intervals) {
        auto ll = std::to_string(point.x()) + "," + std::to_string(point.y());
        auto request = http::MockRequest(
            http::POST,
            http::URL("http://localhost/indoor/tasks/generate")
                .addParam("indoor_plan_id", indoorPlanId)
                .addParam("name", name)
                .addParam("ll", ll)
                .addParam("intervals", intervals)
                .addParam("userId", USER_ID));

        request.body = testdata::indoor_tasks_generate_pb;
        request.headers.emplace(USER_ID_HEADER, USER_ID);
        return request;
    };

    auto checkFailTest = [&](const std::vector<std::string>& intervals) {
        auto request = generateRequest(TEST_INDOOR_PLAN_ID, TEST_TASK_NAME, TEST_POINT, stringutils::join(intervals, ","));
        auto response = yacare::performTestRequest(request);

        ASSERT_EQ(response.status, 400);
    };

    checkFailTest(std::vector<std::string>{"1", "r", "&"});
    checkFailTest(std::vector<std::string>{"1", "", "2"});
}

} // Y_UNIT_TEST_SUITE_F

Y_UNIT_TEST_SUITE_F(indoor_transmitters_list_tests, Fixture) {

Y_UNIT_TEST(indoor_transmitters_list_test)
{
    const unsigned int txCount = 10;
    {
        auto txn = pgPool().masterWriteableTransaction();
        auto transmitters = makeTestTransmitters(txCount);
        db::ugc::TransmitterGateway{*txn}.insert(transmitters);

        txn->commit();
    }

    auto generateListRequestWithIndoorLevelIdParam = [](const std::string& indoorPlanId,
                                                        const std::string& indoorLevelId,
                                                        uint64_t timestamp,
                                                        const tile::Tile& tile) {
        auto request = http::MockRequest(
            http::GET,
            http::URL("http://localhost/indoor/transmitters/list")
                .addParam("indoor_plan_id", indoorPlanId)
                .addParam("indoor_level_id", indoorLevelId)
                .addParam("timestamp", timestamp)
                .addParam("userId", USER_ID)
                .addParam("x", tile.x())
                .addParam("y", tile.y())
                .addParam("z", tile.z()));
        request.headers.emplace(USER_ID_HEADER, USER_ID);
        return request;
    };

    auto generateListRequestWithoutIndoorLevelIdParam =[](const std::string& indoorPlanId,
                                                          uint64_t timestamp,
                                                          const tile::Tile& tile) {
        auto request = http::MockRequest(
            http::GET,
            http::URL("http://localhost/indoor/transmitters/list")
                .addParam("indoor_plan_id", indoorPlanId)
                .addParam("timestamp", timestamp)
                .addParam("userId", USER_ID)
                .addParam("x", tile.x())
                .addParam("y", tile.y())
                .addParam("z", tile.z()));
        request.headers.emplace(USER_ID_HEADER, USER_ID);
        return request;
    };

    auto timestamp = chrono::sinceEpoch<std::chrono::seconds>();

    auto request = generateListRequestWithIndoorLevelIdParam(
        TEST_INDOOR_PLAN_ID, TEST_INDOOR_LEVEL_ID, timestamp, TEST_TILE_1);
    auto response = yacare::performTestRequest(request);
    ASSERT_TRUE(response.status == 200);
    auto ptransmitters = boost::lexical_cast<pindoor::Transmitters>(response.body);
    ASSERT_EQ(ptransmitters.transmitters().size(), 0u);

    request = generateListRequestWithoutIndoorLevelIdParam(
        TEST_INDOOR_PLAN_ID, timestamp, TEST_TILE_1);
    response = yacare::performTestRequest(request);
    ptransmitters = boost::lexical_cast<pindoor::Transmitters>(response.body);
    ASSERT_EQ(ptransmitters.transmitters().size(), 0u);

    request = generateListRequestWithIndoorLevelIdParam(
        TEST_INDOOR_PLAN_ID, "123", timestamp + 1, TEST_TILE_1);
    response = yacare::performTestRequest(request);
    ptransmitters = boost::lexical_cast<pindoor::Transmitters>(response.body);
    ASSERT_EQ(ptransmitters.transmitters().size(), 0u);

    request = generateListRequestWithoutIndoorLevelIdParam(
        TEST_INDOOR_PLAN_ID, timestamp + 1, TEST_TILE_1);
    response = yacare::performTestRequest(request);
    ASSERT_TRUE(response.status == 200);

    ptransmitters = boost::lexical_cast<pindoor::Transmitters>(response.body);
    ASSERT_EQ(ptransmitters.transmitters().size(), txCount);

    for (const auto& ptransmitter : ptransmitters.transmitters()) {
        ASSERT_EQ(*(ptransmitter.id()), TRANSMITTER_ID);
        ASSERT_EQ(*(ptransmitter.indoor_plan_id()), TEST_INDOOR_PLAN_ID);
        ASSERT_EQ(*(ptransmitter.indoor_level_id()), TEST_INDOOR_LEVEL_ID);
        ASSERT_EQ(maps::geolib3::sproto::decode(*(ptransmitter.position())), TEST_POINT_REAL_1);
        ASSERT_EQ(*(ptransmitter.signal_model_parameters())->A(), TRANSMITTER_MODEL.a);
        ASSERT_EQ(*(ptransmitter.signal_model_parameters())->B(), TRANSMITTER_MODEL.b);
        ASSERT_EQ(*(ptransmitter.type()), serializeTransmitterType(TRANSMITTER_TYPE));
    }

    request = generateListRequestWithIndoorLevelIdParam(
        TEST_INDOOR_PLAN_ID, TEST_INDOOR_LEVEL_ID, timestamp + 1, TEST_TILE_2);

    response = yacare::performTestRequest(request);
    ASSERT_TRUE(response.status == 200);

    ptransmitters = boost::lexical_cast<pindoor::Transmitters>(response.body);

    ASSERT_EQ(ptransmitters.transmitters().size(), txCount);

    for (const auto& ptransmitter : ptransmitters.transmitters()) {
        ASSERT_EQ(*(ptransmitter.id()), TRANSMITTER_ID);
        ASSERT_EQ(*(ptransmitter.indoor_plan_id()), TEST_INDOOR_PLAN_ID);
        ASSERT_EQ(*(ptransmitter.indoor_level_id()), TEST_INDOOR_LEVEL_ID);
        ASSERT_EQ(maps::geolib3::sproto::decode(*(ptransmitter.position())), TEST_POINT_REAL_2);
        ASSERT_EQ(*(ptransmitter.signal_model_parameters())->A(), TRANSMITTER_MODEL.a);
        ASSERT_EQ(*(ptransmitter.signal_model_parameters())->B(), TRANSMITTER_MODEL.b);
        ASSERT_EQ(*(ptransmitter.type()), serializeTransmitterType(TRANSMITTER_TYPE));
    }
}
}

Y_UNIT_TEST_SUITE_F(indoor_tasks_abandon_tests, Fixture) {

Y_UNIT_TEST(indoor_tasks_abandon_test_pending_and_available)
{
    const unsigned int pendingTasks = 4;
    const unsigned int availableTasks = 5;
    {
        auto txn = pgPool().masterWriteableTransaction();
        auto tasks = makeTestTasks(
            pendingTasks,
            availableTasks,
            0,
            0);
            db::ugc::TaskGateway{*txn}.insert(tasks);

        txn->commit();
    }

    auto generateRequest = [](const std::string& indoorPlanId) {
        auto request = http::MockRequest(
            http::POST,
            http::URL("http://localhost/indoor/tasks/abandon")
                .addParam("indoor_plan_id", indoorPlanId)
                .addParam("userId", USER_ID));
        request.headers.emplace(USER_ID_HEADER, USER_ID);
        return request;
    };

    auto request = generateRequest(TEST_INDOOR_PLAN_ID);
    auto response = yacare::performTestRequest(request);

    ASSERT_TRUE(response.status == 200);

    auto txn = pgPool().slaveTransaction();
    db::ugc::TaskGateway gtw{*txn};
    auto tasks = gtw.load();

    ASSERT_EQ(tasks.size(), pendingTasks + availableTasks);

    for (const auto& task : tasks) {
        ASSERT_EQ(task.status(), db::ugc::TaskStatus::Cancelled);
    }
}

Y_UNIT_TEST(indoor_tasks_abandon_test_others)
{
    const unsigned int pendingTasks = 4;
    const unsigned int availableTasks = 5;
    const unsigned int acquiredTasks = 1;
    const unsigned int cancelledTasks = 6;
    const unsigned int doneTasks = 6;
    {
        auto txn = pgPool().masterWriteableTransaction();
        auto tasks = makeTestTasks(
            pendingTasks,
            availableTasks,
            doneTasks,
            cancelledTasks);
            db::ugc::TaskGateway{*txn}.insert(tasks);

        if (acquiredTasks == 1) {
            db::ugc::Task acquiredTask;
            acquiredTask
                .setStatus(db::ugc::TaskStatus::Available)
                .setDistanceInMeters(40000000)
                .setGeodeticPoint(TEST_POINT)
                .setIndoorPlanId(TEST_INDOOR_PLAN_ID);
            db::ugc::TaskGateway{*txn}.insert(acquiredTask);

            auto assignment = acquiredTask.assignTo(USER_ID);
            db::ugc::AssignmentGateway{*txn}.insert(assignment);

            db::ugc::TaskGateway{*txn}.update(acquiredTask);
        }

        txn->commit();
    }

    auto generateRequest = [](const std::string& indoorPlanId) {
        auto request = http::MockRequest(
            http::POST,
            http::URL("http://localhost/indoor/tasks/abandon")
                .addParam("indoor_plan_id", indoorPlanId)
                .addParam("userId", USER_ID)
            );
        request.headers.emplace(USER_ID_HEADER, USER_ID);
        return request;
    };

    auto request = generateRequest(TEST_INDOOR_PLAN_ID);
    auto response = yacare::performTestRequest(request);

    ASSERT_TRUE(response.status == 200);

    auto txn = pgPool().slaveTransaction();
    db::ugc::TaskGateway gtw{*txn};
    auto tasks = gtw.load();

    ASSERT_EQ(tasks.size(), pendingTasks + availableTasks + acquiredTasks + doneTasks + cancelledTasks);

    for (const auto& task : tasks) {
        ASSERT_NE(task.status(), db::ugc::TaskStatus::Pending);
        ASSERT_NE(task.status(), db::ugc::TaskStatus::Available);
    }
}
}

Y_UNIT_TEST_SUITE_F(indoor_transmitters_statistics_tests, Fixture) {

Y_UNIT_TEST(indoor_transmitters_statistics_test)
{
    const unsigned int pendingTasks = 2;
    const unsigned int availableTasks = 6;
    const unsigned int acquiredTasks = 1;
    const unsigned int cancelledTasks = 14;
    const unsigned int doneTasks = 8;

    auto timestamp = chrono::sinceEpoch<std::chrono::seconds>();
    {
        auto txn = pgPool().masterWriteableTransaction();
        auto tasks = makeTestTasks(
            pendingTasks,
            availableTasks,
            doneTasks,
            cancelledTasks);
            db::ugc::TaskGateway{*txn}.insert(tasks);

        if (acquiredTasks == 1) {
            db::ugc::Task acquiredTask;
            acquiredTask.setStatus(db::ugc::TaskStatus::Available)
                .setDistanceInMeters(40000000)
                .setGeodeticPoint(TEST_POINT)
                .setIndoorPlanId(TEST_INDOOR_PLAN_ID);
            db::ugc::TaskGateway{*txn}.insert(acquiredTask);

            auto assignment = acquiredTask.assignTo(USER_ID);
            db::ugc::AssignmentGateway{*txn}.insert(assignment);

            db::ugc::TaskGateway{*txn}.update(acquiredTask);
        }

        txn->commit();
    }

    auto generateRequest = [](const std::string& indoorPlanId) {
        auto request = http::MockRequest(
            http::GET,
            http::URL("http://localhost/indoor/tasks/statistics")
                .addParam("indoor_plan_id", indoorPlanId)
                .addParam("userId", USER_ID));
        request.headers.emplace(USER_ID_HEADER, USER_ID);
        return request;
    };

    auto request = generateRequest(TEST_INDOOR_PLAN_ID);
    auto response = yacare::performTestRequest(request);

    ASSERT_TRUE(response.status == 200);

    auto statistics = boost::lexical_cast<pindoor::TasksStatistics>(response.body);

    ASSERT_EQ(*(statistics.available_tasks_count()), availableTasks);
    ASSERT_EQ(*(statistics.acquired_tasks_count()), acquiredTasks);
    ASSERT_EQ(*(statistics.done_tasks_count()), doneTasks);
    ASSERT_EQ(*(statistics.pending_tasks_count()), pendingTasks);
    ASSERT_EQ(*(statistics.last_generate_timestamp()), timestamp);
    ASSERT_EQ(*(statistics.acquired_to()), "");
    ASSERT_EQ(*(statistics.acquired_at()), 0u);
}

Y_UNIT_TEST(indoor_transmitters_statistics_fake_indoor_plan_test)
{
    const unsigned int pendingTasks = 0;
    const unsigned int availableTasks = 0;
    const unsigned int acquiredTasks = 0;
    const unsigned int doneTasks = 0;
    const unsigned int timestamp = 0;

    auto generateRequest = [](const std::string& indoorPlanId) {
        auto request = http::MockRequest(
            http::GET,
            http::URL("http://localhost/indoor/tasks/statistics")
                .addParam("indoor_plan_id", indoorPlanId)
                .addParam("userId", USER_ID));
        request.headers.emplace(USER_ID_HEADER, USER_ID);
        return request;
    };

    auto request = generateRequest("FakeIndoorPlanId");
    auto response = yacare::performTestRequest(request);

    ASSERT_TRUE(response.status == 200);

    auto statistics = boost::lexical_cast<pindoor::TasksStatistics>(response.body);

    ASSERT_EQ(*(statistics.available_tasks_count()), availableTasks);
    ASSERT_EQ(*(statistics.acquired_tasks_count()), acquiredTasks);
    ASSERT_EQ(*(statistics.done_tasks_count()), doneTasks);
    ASSERT_EQ(*(statistics.pending_tasks_count()), pendingTasks);
    ASSERT_EQ(*(statistics.last_generate_timestamp()), timestamp);
    ASSERT_EQ(*(statistics.acquired_to()), "");
    ASSERT_EQ(*(statistics.acquired_at()), 0u);
}
}

Y_UNIT_TEST_SUITE_F(indoor_tasks_free_acquired_task_tests, Fixture) {
using db::ugc::Assignment;

Y_UNIT_TEST(indoor_tasks_free_acquired_task_test_have_acquired_tasks)
{   
    db::TId acquiredTaskId = static_cast<db::TId>(0);
    db::TId activeAssingmentId = static_cast<db::TId>(0);
    {
        auto txn = pgPool().masterWriteableTransaction();

        db::ugc::Task acquiredTask;
        acquiredTask.setStatus(db::ugc::TaskStatus::Available)
            .setDistanceInMeters(40000000)
            .setGeodeticPoint(TEST_POINT)
            .setIndoorPlanId(TEST_INDOOR_PLAN_ID);
        db::ugc::TaskGateway{*txn}.insert(acquiredTask);

        acquiredTaskId = acquiredTask.id();

        std::vector<db::ugc::TaskPath>taskPaths(10);
        for (auto& taskPath : taskPaths) {
            taskPath.setTaskId(acquiredTaskId);
        }
        db::ugc::TaskPathGateway{*txn}.insert(taskPaths);

        auto assignment = acquiredTask.assignTo(USER_ID);
        db::ugc::AssignmentGateway{*txn}.insert(assignment);
        assignment.createAssignmentPaths(taskPaths);
        
        activeAssingmentId = assignment.id();
        
        auto assignmentTaskPaths = assignment.assignmentTaskPaths();
        assignmentTaskPaths[1].markAsAccepted();
        db::ugc::AssignmentTaskPathGateway{*txn}.insert(assignmentTaskPaths);

        db::ugc::TaskGateway{*txn}.update(acquiredTask);
        
        txn->commit();
    }

    auto generateRequest = [](const std::string& indoorPlanId, const std::string& userId) {
        auto request = http::MockRequest(
            http::POST,
            http::URL("http://localhost/indoor/tasks/free_acquired_task")
                .addParam("indoor_plan_id", indoorPlanId)
                .addParam("userId", userId)
            );
        request.headers.emplace(USER_ID_HEADER, userId);
        return request;
    };

    auto request = generateRequest(TEST_INDOOR_PLAN_ID, USER_ID);
    auto response = yacare::performTestRequest(request);

    ASSERT_EQ(response.status, 200);

    auto txn = pgPool().slaveTransaction();

    auto acquiredTask = db::ugc::TaskGateway{*txn}.load(
        db::ugc::table::Task::id == acquiredTaskId).back();
    
    ASSERT_EQ(acquiredTask.status(), db::ugc::TaskStatus::Available);

    auto activeAssignment = db::ugc::AssignmentGateway{*txn}.load(
        db::ugc::table::Assignment::taskId == activeAssingmentId).back();
    
    ASSERT_EQ(activeAssignment.status(), db::ugc::AssignmentStatus::Abandoned);

    auto acceptedAssignmentTaskPaths = db::ugc::AssignmentTaskPathGateway(*txn).load(
        db::ugc::table::AssignmentTaskPath::assignmentId == activeAssingmentId);

    for (const auto& assignmentTaskPath : acceptedAssignmentTaskPaths) {
        ASSERT_EQ(assignmentTaskPath.status(), db::ugc::AssignmentTaskPathStatus::Abandoned);
    }
}

Y_UNIT_TEST(indoor_tasks_free_acquired_task_test_have_no_acquired_tasks)
{
    const unsigned int pendingTasks = 1;
    const unsigned int availableTasks = 2;
    const unsigned int cancelledTasks = 3;
    const unsigned int doneTasks = 1;
    {
        auto txn = pgPool().masterWriteableTransaction();
        auto tasks = makeTestTasks(
            pendingTasks,
            availableTasks,
            doneTasks,
            cancelledTasks);
            db::ugc::TaskGateway{*txn}.insert(tasks);
    }

    auto generateRequest = [](const std::string& indoorPlanId, const std::string& userId) {
        auto request = http::MockRequest(
            http::POST,
            http::URL("http://localhost/indoor/tasks/free_acquired_task")
                .addParam("indoor_plan_id", indoorPlanId)
                .addParam("userId", userId)
            );
        request.headers.emplace(USER_ID_HEADER, userId);
        return request;
    };

    auto request = generateRequest(TEST_INDOOR_PLAN_ID, USER_ID);
    auto response = yacare::performTestRequest(request);

    ASSERT_EQ(response.status, 404);
}
} // Y_UNIT_TEST_SUITE_F(indoor_tasks_free_acquired_task_tests)

} // namespace maps::mirc::admin::tests
