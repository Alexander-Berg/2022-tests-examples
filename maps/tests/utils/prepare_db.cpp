#include "prepare_db.h"

#include "indoor_model.h"
#include "utils.h"

#include <maps/indoor/long-tasks/src/radiomap-evaluation-cron-job/lib/impl/utils.h>

#include <maps/indoor/libs/db/include/task_path.h>
#include <maps/indoor/libs/db/include/types.h>


#include <maps/indoor/libs/db/include/assignment_gateway.h>
#include <maps/indoor/libs/db/include/assignment_task_path.h>
#include <maps/indoor/libs/db/include/radiomap_transmitter_gateway.h>
#include <maps/indoor/libs/db/include/static_transmitter_gateway.h>
#include <maps/indoor/libs/db/include/static_transmitter.h>
#include <maps/indoor/libs/db/include/task_gateway.h>
#include <maps/indoor/libs/db/include/task_path_track_gateway.h>


namespace maps::mirc::radiomap_evaluator::tests {

namespace {

const std::string TEST_USER_ID = "TestUserId";

} // namespace

std::vector<geolib3::Polyline2> createTaskPaths(const IndoorModel& indoor, const size_t count)
{
    const double X_PADDING = indoor.xSize() / (count + 1);

    std::vector<geolib3::Polyline2> taskPaths;
    for (size_t i = 0; i < count; ++i) {
        const double x = X_PADDING + X_PADDING * i;
        const double y0 = 0;
        const double y1 = indoor.ySize();
        const auto startPoint = localPlanarToGeodetic(geolib3::Point2(x, y0), indoor.geoPoint());
        const auto stopPoint = localPlanarToGeodetic(geolib3::Point2(x, y1), indoor.geoPoint());
        taskPaths.emplace_back(geolib3::Polyline2({{startPoint, stopPoint}}));
    }
    return taskPaths;
}

pindoor::IndoorTrack makeTrack(
    const IndoorModel& indoor,
    const db::ugc::TaskPath& taskPath)
{
    REQUIRE(taskPath.geodeticGeom().pointsNumber() == 2, "Can makeTrack only for taskPath with 2 points.");

    const double x = geodeticToLocalPlanar(taskPath.geodeticGeom().pointAt(0), indoor.geoPoint()).x();
    const double y0 = geodeticToLocalPlanar(taskPath.geodeticGeom().pointAt(0), indoor.geoPoint()).y();
    const double y1 = geodeticToLocalPlanar(taskPath.geodeticGeom().pointAt(1), indoor.geoPoint()).y();
    const double step = 1.0;

    static long long timestamp = 0;

    pindoor::IndoorTrack track;
    track.startTimestamp() = timestamp;

    pindoor::CheckPoint checkPoint;
    checkPoint.timestamp() = timestamp;
    track.checkPoints().emplace_back(checkPoint);

    for (auto y = y0; y <= y1; y += step) {
        const auto signal = indoor.generateModelSignal(
            localPlanarToGeodetic(geolib3::Point2(x, y), indoor.geoPoint()),
            taskPath.indoorLevelId()
        );
        for (const auto& [txId, rssi] : signal) {
            pindoor::BleEvent event;
            event.timestamp() = timestamp;
            event.address() = toUpperCase(txId);
            event.scanData() = std::string();
            event.rssi() = rssi;

            track.bleEvents().emplace_back(event);
        }
        timestamp += 1000;
    }
    checkPoint.timestamp() = timestamp;
    track.checkPoints().emplace_back(checkPoint);

    return track;
}

db::ugc::Task createIndoorTaskWithTaskPaths(const IndoorModel& indoor, pgpool3::Pool& pool) {
    db::ugc::Task task;
    auto txn = pool.masterWriteableTransaction();

    task.setStatus(db::ugc::TaskStatus::Available)
        .setDistanceInMeters(40000000)
        .setGeodeticPoint(indoor.geoPoint())
        .setIndoorPlanId(indoor.indoorPlanId());

    const size_t TASK_PATHS_PER_LEVEL = 30;
    const auto taskPaths = createTaskPaths(indoor, TASK_PATHS_PER_LEVEL);

    for (const auto& level : indoor.indoorLevelIds()) {
        for (const auto& taskPath : taskPaths) {
            task.addPath(
                taskPath,
                level,
                std::chrono::seconds(100),
                40000,
                std::nullopt);
        }
    }

    db::ugc::TaskGateway{*txn}.insert(task);
    txn->commit();

    return task;
}

db::TId assignTask(db::ugc::Task& task, const db::UserId& userId, pgpool3::Pool& pool) {
    db::TId assignmentId = 0;

    { // create assignment
        auto txn = pool.masterWriteableTransaction();

        auto assignment = task.assignTo(userId);
        assignment.markAsCompleted();

        db::ugc::AssignmentGateway{*txn}.insert(assignment);
        task.markAsDone();
        db::ugc::TaskGateway{*txn}.update(task);
        txn->commit();
        assignmentId = assignment.id();
    }

    { // create createAssignmentPaths
        auto txn = pool.masterWriteableTransaction();

        auto assignment = db::ugc::AssignmentGateway{*txn}.loadById(assignmentId);
        assignment.createAssignmentPaths(task.paths(), db::ugc::AssignmentTaskPathStatus::Accepted);
        db::ugc::AssignmentTaskPathGateway{*txn}.insert(assignment.assignmentTaskPaths());

        txn->commit();
    }

    return assignmentId;
}

TaskPathTracksById createTracksForAssignment(
    const IndoorModel& indoorModel,
    const db::TId assignmentId,
    pgpool3::Pool& pool)
{
    auto txn = pool.masterWriteableTransaction();

    auto assignmentTaskPaths = db::ugc::AssignmentTaskPathGateway{*txn}.load(
        db::ugc::table::AssignmentTaskPath::assignmentId == assignmentId);

    TaskPathTracksById taskPathTracksById;
    for (auto& assignmentTaskPath : assignmentTaskPaths) {
        auto taskPaths = db::ugc::TaskPathGateway{*txn}.load(
            db::ugc::table::TaskPath::id == assignmentTaskPath.taskPathId());

        REQUIRE(taskPaths.size() == 1, "Expected single taskPath loaded by ID");
        assignmentTaskPath.markAsAccepted();

        const auto& taskPath = taskPaths.front();
        const auto track = makeTrack(indoorModel, taskPath);
        const auto trackId = toUpperCase(generateUUID());

        auto taskPathTrack = assignmentTaskPath.createTaskPathTracks(trackId);
        db::ugc::TaskPathTrackGateway{*txn}.insert(taskPathTrack);
        taskPathTracksById[trackId] = boost::lexical_cast<std::string>(track);
    }

    {
        auto row = txn->exec1(R"end(select count(*) from ugc.task;)end");
        INFO() << "count(*) from ugc.task: " << row[0].as<size_t>();
    }
    txn->commit();

    return taskPathTracksById;
}

TaskPathTracksById prepareDataBase(const std::vector<IndoorModel>& indoors, pgpool3::Pool& pool)
{
    TaskPathTracksById result;
    for (const auto& indoor : indoors) {
        db::ugc::Task task = createIndoorTaskWithTaskPaths(indoor, pool);
        const db::TId assignmentId = assignTask(task, TEST_USER_ID, pool);
        for (auto trackById : createTracksForAssignment(indoor, assignmentId, pool)) {
            result.emplace(std::move(trackById));
        }
    }
    return result;
}

TaskPathTracksById prepareDataBase(IndoorModel indoor, pgpool3::Pool& pool)
{
    return prepareDataBase(std::vector<IndoorModel>{std::move(indoor)}, pool);
}

db::ugc::StaticTransmitter makeStaticTxAndWriteToStaticTxsTable(
    const Transmitter& tx,
    pgpool3::Pool& pool)
{
    auto staticTransmitter = db::ugc::StaticTransmitter(
        tx.indoorPlanId(),
        tx.indoorLevelId(),
        tx.txType(),
        tx.txId(),
        tx.signalParams().a,
        tx.signalParams().b,
        tx.geodeticPoint().x(),
        tx.geodeticPoint().y(),
        "test_version", // version
        std::nullopt, // description
        db::ugc::StaticTransmitterStatus::Active,
        std::nullopt // originalId
    );
    auto txn = pool.masterWriteableTransaction();
    auto gw = db::ugc::StaticTransmitterGateway(*txn);
    gw.insert(staticTransmitter);
    txn->commit();

    return staticTransmitter;
}

} // namespace maps::mirc::radiomap_evaluator::tests
