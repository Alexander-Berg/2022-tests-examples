#include <maps/indoor/long-tasks/src/radiomap-evaluation-cron-job/lib/include/radiomap_evaluator.h>
#include <maps/indoor/long-tasks/src/radiomap-evaluation-cron-job/lib/impl/utils.h>
#include <maps/indoor/libs/db/include/assignment_task_path.h>
#include <maps/indoor/libs/db/include/assignment_gateway.h>
#include <maps/indoor/libs/db/include/radiomap_transmitter_gateway.h>
#include <maps/indoor/libs/db/include/task_gateway.h>
#include <maps/indoor/libs/db/include/task_path_track_gateway.h>
#include <maps/libs/geolib/include/distance.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>
#include <util/generic/guid.h>

#include <random>

#include "maps/indoor/libs/unittest/fixture.h"

namespace maps::mirc::radiomap_evaluator::tests {
using namespace ::testing;

namespace pindoor = yandex::maps::sproto::offline::mrc::indoor;

namespace {

const std::string TEST_USER_ID = "TestUserId";
const std::string TEST_INDOOR_PLAN_ID = "12345";
const std::string TEST_INDOOR_LEVEL_ID = "1";
const db::ugc::TransmitterType TEST_TRANSMITTER_TYPE = TransmitterType::Ble;
const geolib3::Point2 TEST_POINT{40.0000, 60.0000};
const double TEST_WIDTH = 100;  // meters
const double TEST_HEIGHT = 100; // meters
const double RESIDUAL_THRESHOLD = 1;

double getRand(double min, double max)
{
    static std::mt19937 rng;
    std::uniform_real_distribution<double> distribution(min, max);
    return distribution(rng);
}

std::string generateUUID()
{
    const auto tsGuid = CreateGuidAsString();
    return std::string(tsGuid.data(), tsGuid.length());
}

int generateModelSignal(const db::ugc::Transmitter& transmitter, const geolib3::Point2& point)
{
    double r = geolib3::geoDistance(transmitter.geodeticPoint(), point);
    auto rssi = round(transmitter.signalParams().a - transmitter.signalParams().b * std::log(r));
    return rssi;
}

std::vector<db::ugc::Transmitter> createRandomTransmitters(size_t count)
{
    std::vector<db::ugc::Transmitter> transmitters;
    for (size_t i = 0; i < count; ++i) {
        const auto txId = std::to_string(i);
        const double txX = getRand(0, TEST_WIDTH);
        const double txY = getRand(0, TEST_HEIGHT);
        const double txA = getRand(-20, -90);
        const double txB = getRand(1, 20);
        const double rssiDeviation = 10;
        const double rssiThreshold = -100;

        transmitters.emplace_back(
            TEST_INDOOR_PLAN_ID,
            TEST_INDOOR_LEVEL_ID,
            TEST_TRANSMITTER_TYPE,
            std::to_string(i),
            localPlanarToGeodetic(geolib3::Point2(txX, txY), TEST_POINT),
            db::ugc::SignalModelParameters{txA, txB, rssiDeviation, rssiThreshold},
            false);
    }

    return transmitters;
}

class RadioMapEvaluatorTest : public RadioMapEvaluator
{
public:
    RadioMapEvaluatorTest(
        pgpool3::Pool& pool,
        const std::vector<db::ugc::Transmitter>& transmitters,
        const std::unordered_map<std::string, geolib3::Polyline2> taskPaths)
        : RadioMapEvaluator(pool, std::make_unique<unittest::MockS3Storage>(
            [this](const std::string& key)
            {
                return tracks_.at(key);
            }), nullptr, nullptr)
        , pool_(pool)
        , transmitters_(transmitters)
        , taskPaths_(taskPaths)
    { }

    void prepareDataBase();

    std::unordered_map<std::string, db::ugc::Transmitter> restoredTransmitters() const;

    size_t calculateResidual();

private:
    std::string createTrack(const db::ugc::TaskPath& taskPath);

    pgpool3::Pool& pool_;
    const std::vector<db::ugc::Transmitter> transmitters_;
    const std::unordered_map<std::string, geolib3::Polyline2> taskPaths_;
    std::unordered_map<std::string, std::string> tracks_;
};

std::string RadioMapEvaluatorTest::createTrack(const db::ugc::TaskPath& taskPath)
{
    const auto startPoint = geodeticToLocalPlanar(taskPath.geodeticGeom().pointAt(0), TEST_POINT);
    const auto stopPoint = geodeticToLocalPlanar(taskPath.geodeticGeom().pointAt(1), TEST_POINT);
    const auto dist = geolib3::distance(startPoint, stopPoint);
    const auto trackId = generateUUID();

    long long timestamp = 0;

    pindoor::IndoorTrack track;
    track.startTimestamp() = timestamp;

    pindoor::CheckPoint checkPoint;
    checkPoint.timestamp() = timestamp;
    track.checkPoints().emplace_back(checkPoint);

    const auto stepLength = 1.0;
    const auto numSteps = static_cast<size_t>(round(dist / stepLength));

    for(size_t i = 0; i <= numSteps; ++i, timestamp += 1000) {
        const auto point = geolib3::Point2(
            (startPoint.x() * (numSteps - i) + stopPoint.x() * i) / numSteps,
            (startPoint.y() * (numSteps - i) + stopPoint.y() * i) / numSteps);

        for (const auto& tx : transmitters_) {
            pindoor::BleEvent event;
            event.timestamp() = timestamp;
            event.address() = toUpperCase(tx.txId());
            event.scanData() = std::string();
            event.rssi() = generateModelSignal(tx, localPlanarToGeodetic(point, TEST_POINT));
            track.bleEvents().emplace_back(event);
        }
    }

    checkPoint.timestamp() = timestamp;
    track.checkPoints().emplace_back(checkPoint);
    tracks_[trackId] = boost::lexical_cast<std::string>(track);
    return trackId;
}

void RadioMapEvaluatorTest::prepareDataBase()
{
    db::ugc::Task task;
    // create task & task_paths
    {
        auto txn = pool_.masterWriteableTransaction();

        task.setStatus(db::ugc::TaskStatus::Available)
            .setDistanceInMeters(40000000)
            .setGeodeticPoint(TEST_POINT)
            .setIndoorPlanId(TEST_INDOOR_PLAN_ID);

        for (const auto& [name, taskPath] : taskPaths_) {
            task.addPath(
                taskPath,
                TEST_INDOOR_LEVEL_ID,
                std::chrono::seconds(100),
                40000,
                name);
        }

        db::ugc::TaskGateway{*txn}.insert(task);
        txn->commit();
    }

    db::TId assignmentId = 0;
    // create assignment
    {
        auto txn = pool_.masterWriteableTransaction();

        auto assignment = task.assignTo(TEST_USER_ID);
        assignment.markAsCompleted();

        db::ugc::AssignmentGateway{*txn}.insert(assignment);
        task.markAsDone();
        db::ugc::TaskGateway{*txn}.update(task);
        txn->commit();
        assignmentId = assignment.id();
    }

    // create createAssignmentPaths
    {
        auto txn = pool_.masterWriteableTransaction();

        auto assignment = db::ugc::AssignmentGateway{*txn}.loadById(assignmentId);
        assignment.createAssignmentPaths(task.paths(), db::ugc::AssignmentTaskPathStatus::Accepted);
        db::ugc::AssignmentTaskPathGateway{*txn}.insert(assignment.assignmentTaskPaths());

        txn->commit();
    }

    // create TaskPathTracks
    {
        auto txn = pool_.masterWriteableTransaction();

        auto assignmentTaskPaths = db::ugc::AssignmentTaskPathGateway{*txn}.load(
            db::ugc::table::AssignmentTaskPath::assignmentId == assignmentId);

        for (auto& assignmentTaskPath : assignmentTaskPaths) {
            auto taskPaths = db::ugc::TaskPathGateway{*txn}.load(
                db::ugc::table::TaskPath::id == assignmentTaskPath.taskPathId());
            auto trackId = createTrack(taskPaths.front());
            assignmentTaskPath.markAsAccepted();
            auto taskPathTrack = assignmentTaskPath.createTaskPathTracks(trackId);
            db::ugc::TaskPathTrackGateway{*txn}.insert(taskPathTrack);
        }

        txn->commit();
    }
}

std::unordered_map<std::string, db::ugc::Transmitter> RadioMapEvaluatorTest::restoredTransmitters() const
{
    auto txn = pool_.slaveTransaction();
    auto transmitters = db::ugc::TransmitterGateway{*txn}.load();

    std::unordered_map<std::string, db::ugc::Transmitter> restoredTransmitters;
    for(const auto& transmitter : transmitters) {
        restoredTransmitters.emplace(std::make_pair(transmitter.txId(), transmitter));
    }

    return restoredTransmitters;
}

size_t RadioMapEvaluatorTest::calculateResidual()
{
    const auto restoredTransmitters = this->restoredTransmitters();

    size_t failedTxCount = 0;

    for(const auto& txOld : transmitters_) {
        const auto txId = txOld.txId();
        auto iter = restoredTransmitters.find(txId);
        if (iter == restoredTransmitters.end()) {
            ++failedTxCount;
            continue;
        }

        const auto& txNew = iter->second;
        auto xyPointOld = geodeticToLocalPlanar(txOld.geodeticPoint(), TEST_POINT);
        auto xyPointNew = geodeticToLocalPlanar(txNew.geodeticPoint(), TEST_POINT);

        double residual =
            (std::fabs(xyPointOld.x() - xyPointNew.x()) +
             std::fabs(xyPointOld.y() - xyPointNew.y()) +
             std::fabs(txOld.signalParams().a - txNew.signalParams().a) +
             std::fabs(txOld.signalParams().b - txNew.signalParams().b)) / 4;

        INFO() << txId << " :: residual = " << residual;
        if (residual > RESIDUAL_THRESHOLD) {
            ++failedTxCount;
        }
    }

    INFO() << "Failed TX count: " << failedTxCount << " of " << transmitters_.size();
    return failedTxCount;
}

}

Y_UNIT_TEST_SUITE_F(radiomap_evaluate_tests, unittest::Fixture) {

Y_UNIT_TEST(radiomap_evaluate_test)
{
    const size_t txCount = 1000;
    const size_t tracksCount = 10;
    const double padding = 5.0;

    std::unordered_map<std::string, geolib3::Polyline2> taskPaths;

    // Generating vertical paths, covering the test area
    for (size_t i = 0; i < tracksCount; ++i) {
        const double x = padding + (TEST_WIDTH / tracksCount) * i;
        const double y0 = padding;
        const double y1 = TEST_HEIGHT - padding;
        auto startPoint = localPlanarToGeodetic(geolib3::Point2(x, y0), TEST_POINT);
        auto stopPoint = localPlanarToGeodetic(geolib3::Point2(x, y1), TEST_POINT);
        auto taskPathName = TEST_INDOOR_LEVEL_ID + "_" + std::to_string(i);
        taskPaths[taskPathName] = geolib3::Polyline2({{startPoint, stopPoint}});
    }

    auto evaluator = RadioMapEvaluatorTest(
        pgPool(),
        createRandomTransmitters(txCount),
        taskPaths);

    evaluator.prepareDataBase();
    evaluator.evaluate();

    const auto failedTxCount = evaluator.calculateResidual();

    ASSERT_TRUE(static_cast<double>(failedTxCount) / txCount < 0.1);
}

Y_UNIT_TEST(radiomap_evaluate_outdoor_test)
{
    std::vector<db::ugc::Transmitter> transmitters;

    transmitters.emplace_back(
        TEST_INDOOR_PLAN_ID,
        TEST_INDOOR_LEVEL_ID,
        TEST_TRANSMITTER_TYPE,
        "TX1",
        localPlanarToGeodetic(geolib3::Point2(10, 30), TEST_POINT),
        db::ugc::SignalModelParameters{-60, 6, std::nullopt, std::nullopt},
        false);

    transmitters.emplace_back(
        TEST_INDOOR_PLAN_ID,
        TEST_INDOOR_LEVEL_ID,
        TEST_TRANSMITTER_TYPE,
        "TX2",
        localPlanarToGeodetic(geolib3::Point2(70, 30), TEST_POINT),
        db::ugc::SignalModelParameters{-60, 6, std::nullopt, std::nullopt},
        false);

    std::unordered_map<std::string, geolib3::Polyline2> taskPaths;
    const auto point1 = localPlanarToGeodetic(geolib3::Point2(0, 0), TEST_POINT);
    const auto point2 = localPlanarToGeodetic(geolib3::Point2(0, 60), TEST_POINT);
    const auto point3 = localPlanarToGeodetic(geolib3::Point2(20, 0), TEST_POINT);
    const auto point4 = localPlanarToGeodetic(geolib3::Point2(20, 60), TEST_POINT);
    const auto point5 = localPlanarToGeodetic(geolib3::Point2(60, 0), TEST_POINT);
    const auto point6 = localPlanarToGeodetic(geolib3::Point2(60, 60), TEST_POINT);

    taskPaths["1_1"] = geolib3::Polyline2({{point1, point2}});
    taskPaths["1_2"] = geolib3::Polyline2({{point3, point4}});
    taskPaths["1_3_outdoor"] = geolib3::Polyline2({{point5, point6}});

    auto evaluator = RadioMapEvaluatorTest(
        pgPool(),
        transmitters,
        taskPaths);

    evaluator.prepareDataBase();
    evaluator.evaluate();

    const auto failedTxCount = evaluator.calculateResidual();
    ASSERT_TRUE(failedTxCount == 1);

    const auto rssiThreshold = round(
        transmitters[0].signalParams().a -
        transmitters[0].signalParams().b * std::log(50)) + 1;

    for(const auto& [txId, tx] : evaluator.restoredTransmitters()) {
        INFO() << "Restored " << txId << " with A=" << tx.signalParams().a << ", B=" << tx.signalParams().b;
        ASSERT_TRUE(tx.signalParams().rssiThreshold == rssiThreshold);
    }
}

} // Y_UNIT_TEST_SUITE_F

} // namespace maps::mirc::radiomap_evaluator::tests
