#include <boost/test/unit_test.hpp>
#include <iostream>
#include <fstream>
#include <random>

#include <library/cpp/testing/common/env.h>
#include <maps/indoor/libs/indoor_positioning/impl/likelihood_log_model.h>
#include <maps/indoor/libs/indoor_positioning/include/device_capabilities.h>
#include <maps/indoor/libs/indoor_positioning/include/indoor_positioning_client.h>
#include <maps/indoor/libs/indoor_positioning/include/level_index.h>
#include <maps/indoor/libs/indoor_positioning/include/metrics.h>
#include <maps/indoor/libs/indoor_positioning/include/sensor.h>
#include <maps/indoor/libs/indoor_positioning/include/transmitter.h>
#include <maps/indoor/libs/indoor_positioning/include/utils.h>
#include <maps/libs/common/include/exception.h>
#include <maps/libs/log8/include/log8.h>

#include "geometry.h"
#include "ground_truth_metrics.h"

using namespace boost::unit_test;
using namespace INDOOR_POSITIONING_NAMESPACE;

namespace {

const std::string DATASET_PATH = "maps/indoor/libs/indoor_positioning/tests/logs/";

const auto ANDROID_OLD_INDOOR = DeviceCapabilities{
    .hasAccelerometer = true,
    .hasGyroscope = true,
    .hasMagnetometer = true,
    .hasBarometer = true,
    .hasLocation = false,
    .hasBeacons = true,
    .hasWifi = true,
    .hasBle = true,
    .hasWifiThrottling = false};

const auto ANDROID_INDOOR = DeviceCapabilities{
    .hasAccelerometer = true,
    .hasGyroscope = true,
    .hasMagnetometer = true,
    .hasBarometer = true,
    .hasLocation = false,
    .hasBeacons = true,
    .hasWifi = true,
    .hasBle = true,
    .hasWifiThrottling = true};

const auto ANDROID_FUSED = DeviceCapabilities{
    .hasAccelerometer = true,
    .hasGyroscope = true,
    .hasMagnetometer = true,
    .hasBarometer = true,
    .hasLocation = true,
    .hasBeacons = true,
    .hasWifi = true,
    .hasBle = true,
    .hasWifiThrottling = true};

const auto POSITIONING_UPDATE_INTERVAL = std::chrono::milliseconds(1000);
const auto POSITIONING_CONVERGENCE_INTERVAL = std::chrono::milliseconds(30000);

const int ITERATIONS_NUMBER = 5;

std::string readFile(const std::string& fileName)
{
    std::ifstream ifs(fileName, std::ios::binary);
    std::string chunk(1024, '\0');
    std::string data;

    while (ifs) {
        ifs.read(chunk.data(), chunk.size());
        if (ifs) {
            data += chunk;
        } else {
            data += std::string(chunk.data(), ifs.gcount());
        }
    }

    return data;
}

std::shared_ptr<LevelIndex> loadTile(const std::string& tileFile)
{
    const auto data = readFile(tileFile);
    const auto tileData = decodeRadiomapTile(reinterpret_cast<const uint8_t*>(data.data()), data.size());
    auto levelIndex = std::make_shared<LevelIndex>(0);

    for(const auto& p : tileData.levels) {
        levelIndex->updateLevel(p.first, p.second);
    }

    return levelIndex;
}

TransmitterType txTypeFromString(const std::string& str)
{
    if (str == "BEACON") {
        return TransmitterType::BEACON;
    }
    else if (str == "WIFI") {
        return TransmitterType::WIFI;
    }
    else if (str == "BLE") {
        return TransmitterType::BLE;
    }
    REQUIRE(false, "Invalid transmitter type " + str);
}

SensorType sensorTypeFromString(const std::string& str)
{
    if (str == "ACCELEROMETER") {
        return SensorType::ACCELEROMETER;
    }
    else if (str == "MAGNETOMETER") {
        return SensorType::MAGNETOMETER;
    }
    else if (str == "GYROSCOPE") {
        return SensorType::GYROSCOPE;
    }
    else if (str == "BAROMETER") {
        return SensorType::BAROMETER;
    }
    else if (str == "LOCATION") {
        return SensorType::LOCATION;
    }

    REQUIRE(false, "Invalid sensor type " + str);
}

TransmitterMeasurements loadSignals(const std::string& signalsFile)
{
    // Parsing file format:
    //  time txType txId rssi\n
    //  time txType txId rssi\n
    //  ...

    TransmitterMeasurements measurements;
    std::ifstream ifs(signalsFile);
    std::string line;

    while (std::getline(ifs, line)) {
        if (line.empty() || line.front() == '#') {
            // Skipping empty lines and comments
            continue;
        }

        int64_t time;
        std::string txType;
        std::string txId;
        double rssi;

        std::stringstream ss(line);
        ss >> time >> txType >> txId >> rssi;

        measurements.emplace_back(TransmitterMeasurement{
            txId,
            txTypeFromString(txType),
            rssi,
            getTimestamp(time)});
    }

    return measurements;
}

SensorMeasurements loadSensors(const std::string& sensorsFile)
{
    // Parsing file format:
    //  time sensorType x y z\n
    //  time sensorType x y z\n
    //  ...

    SensorMeasurements measurements;
    std::ifstream ifs(sensorsFile);
    std::string line;

    while (std::getline(ifs, line)) {
        if (line.empty() || line.front() == '#') {
            // Skipping empty lines and comments
            continue;
        }

        int64_t time;
        std::string type;
        double x, y, z;

        std::stringstream ss(line);
        ss >> time >> type >> x >> y >> z;

        measurements.emplace_back(SensorMeasurement{
            sensorTypeFromString(type),
            {x, y, z},
            getTimestamp(time)});
    }

    return measurements;
}

std::map<Timestamp, IndoorPosition> loadGroundTruthPoints(const std::string& groundTruthFile)
{
    // Parsing file format:
    //  time lat lon levelId\n
    //  time lat lon levelId\n
    //  ...

    std::map<Timestamp, IndoorPosition> points;
    std::ifstream ifs(groundTruthFile);
    std::string line;

    while (std::getline(ifs, line)) {
        if (line.empty() || line.front() == '#') {
            // Skipping empty lines and comments
            continue;
        }

        int64_t time;
        double latitude, longitude;
        std::string levelId;

        std::stringstream ss(line);
        ss >> time >> latitude >> longitude >> levelId;

        points[getTimestamp(time)] = IndoorPosition{latitude, longitude, 0.0, levelId};
    }

    return points;
}

struct GroundTruthTest {
    GroundTruthTest(
        const std::string& name,
        const std::string& tileFile,
        const std::string& signalsFile,
        const std::string& sensorsFile,
        const std::string& groundTruthFile,
        const DeviceCapabilities& dev)
        : name(name)
        , transmitterMeasurements(loadSignals(signalsFile))
        , sensorMeasurements(loadSensors(sensorsFile))
        , groundTruthPoints(loadGroundTruthPoints(groundTruthFile))
        , deviceCapabilities(dev)
        , levelIndex(loadTile(tileFile))
    {
    }

    const std::string name;
    const TransmitterMeasurements transmitterMeasurements;
    const SensorMeasurements sensorMeasurements;
    const std::map<Timestamp, IndoorPosition> groundTruthPoints;
    const DeviceCapabilities deviceCapabilities;

    std::shared_ptr<LevelIndex> levelIndex;
    std::map<Timestamp, boost::optional<IndoorPosition>> solutionPoints;
};

void doPositioning(GroundTruthTest& test)
{
    for(int iter = 0; iter < ITERATIONS_NUMBER; ++iter) {
        auto client = createIndoorPositioningClient(test.levelIndex, test.deviceCapabilities);
        auto timestamp = getTimestamp(iter);
        auto txIter = test.transmitterMeasurements.begin();
        auto sensorIter = test.sensorMeasurements.begin();

        client->position({}, {}, timestamp);

        while (txIter != test.transmitterMeasurements.end() ||
               sensorIter != test.sensorMeasurements.end())
        {
            timestamp += POSITIONING_UPDATE_INTERVAL;

            TransmitterMeasurements txMeasurements;
            for (; txIter != test.transmitterMeasurements.end() && txIter->timestamp < timestamp; ++txIter) {
                txMeasurements.push_back(*txIter);
            }

            SensorMeasurements sensorMeasurements;
            for (; sensorIter != test.sensorMeasurements.end() && sensorIter->timestamp < timestamp; ++sensorIter) {
                sensorMeasurements.push_back(*sensorIter);
            }

            const auto pos = client->position(
                std::move(txMeasurements),
                std::move(sensorMeasurements),
                timestamp);

            const auto gtPos = groundTruthAtTime(test.groundTruthPoints, timestamp);

            if (pos && gtPos) {
                const auto error = geoDistance(
                    Point{pos->latitude, pos->longitude},
                    Point{gtPos->latitude, gtPos->longitude});

                    DEBUG() << std::setprecision(8) << std::fixed << test.name << ": "
                            << "POSITION for time=" << getMilliseconds(timestamp) << " :: "
                            << pos->latitude << ", " << pos->longitude << ", " << pos->indoorLevelId << ", "
                            << "error=" << error;
            }

            if (timestamp - Timestamp() < POSITIONING_CONVERGENCE_INTERVAL) {
                // Filtering out solution points prior to the solution convergence time
                continue;
            }

            test.solutionPoints[timestamp] = pos;
        }
    }
}

void checkMetrics(GroundTruthTest& test)
{
    auto metrics = calculateMetrics(test.solutionPoints, test.groundTruthPoints);

    metrics.duration /= ITERATIONS_NUMBER;
    metrics.noSolutionTime /= ITERATIONS_NUMBER;
    metrics.levelMismatchTime /= ITERATIONS_NUMBER;
    metrics.levelSwitchCount /= ITERATIONS_NUMBER;

    // Formatted metrics output suitable to be copy-pasted into ground_truth_metrics.h
    // This allows to follow metrics evolution in Arcadia.
    std::cerr << std::fixed << std::setprecision(1);
    std::cerr << "        {{\"" << test.name << "\", \"median_50\"}, " << metrics.median50 << "},\n";
    std::cerr << "        {{\"" << test.name << "\", \"median_70\"}, " << metrics.median70 << "},\n";
    std::cerr << "        {{\"" << test.name << "\", \"median_90\"}, " << metrics.median90 << "},\n";
    std::cerr << "        {{\"" << test.name << "\", \"duration\"}, " << metrics.duration << "},\n";
    std::cerr << "        {{\"" << test.name << "\", \"no_solution_time\"}, " << metrics.noSolutionTime << "},\n";
    std::cerr << "        {{\"" << test.name << "\", \"level_mismatch_time\"}, " << metrics.levelMismatchTime << "},\n";
    std::cerr << "        {{\"" << test.name << "\", \"level_switch_count\"}, " << metrics.levelSwitchCount << "},\n\n";

    const auto makeKey =
        [&test](const std::string& paramName)
        {
            return std::make_pair(test.name, paramName);
        };

    const auto makeMessage =
        [&test](const std::string& paramName, double value, double threshold)
        {
            std::stringstream ss;
            ss << std::fixed << std::setprecision(1) << test.name << "." << paramName << ": "
               << "difference=" << value - threshold << " between "
               << "metrics=" << value << " and threshold=" << threshold << " exceeds the limit";
            return ss.str();
        };

    const auto threshold = SolutionMetrics{
        .median50 = TEST_METRICS.at(makeKey("median_50")),
        .median70 = TEST_METRICS.at(makeKey("median_70")),
        .median90 = TEST_METRICS.at(makeKey("median_90")),
        .duration = TEST_METRICS.at(makeKey("duration")),
        .noSolutionTime = TEST_METRICS.at(makeKey("no_solution_time")),
        .levelMismatchTime = TEST_METRICS.at(makeKey("level_mismatch_time")),
        .levelSwitchCount = TEST_METRICS.at(makeKey("level_switch_count"))};

    // Since we round metrics with precision=0.1, the absolute error shouldn't exceed precision/2.
    const auto absoluteError = 0.05;

    BOOST_CHECK_MESSAGE(
        std::fabs(metrics.median50 - threshold.median50) <= absoluteError,
        makeMessage("median_50", metrics.median50, threshold.median50));

    BOOST_CHECK_MESSAGE(
        std::fabs(metrics.median70 - threshold.median70) <= absoluteError,
        makeMessage("median_70", metrics.median70, threshold.median70));

    BOOST_CHECK_MESSAGE(
        std::fabs(metrics.median90 - threshold.median90) <= absoluteError,
        makeMessage("median_90", metrics.median90, threshold.median90));

    BOOST_CHECK_MESSAGE(
        std::fabs(metrics.noSolutionTime - threshold.noSolutionTime) <= absoluteError,
        makeMessage("no_solution_time", metrics.noSolutionTime, threshold.noSolutionTime));

    BOOST_CHECK_MESSAGE(
        std::fabs(metrics.levelMismatchTime - threshold.levelMismatchTime) <= absoluteError,
        makeMessage("level_mismatch_time", metrics.levelMismatchTime, threshold.levelMismatchTime));

    BOOST_CHECK_MESSAGE(std::fabs(metrics.levelSwitchCount - threshold.levelSwitchCount) <= absoluteError,
        makeMessage("level_switch_count", metrics.levelSwitchCount, threshold.levelSwitchCount));
}

} // namespace

#define GROUND_TRUTH_TRACK_TEST(TEST_NAME, PLAN_NAME, TRACK_NAME)                           \
    BOOST_AUTO_TEST_CASE(android_old_indoor_##TEST_NAME) {                                  \
        GroundTruthTest test(                                                               \
            #TEST_NAME "_android_old_indoor",                                               \
            BinaryPath(DATASET_PATH + PLAN_NAME + "/tile.pb"),                              \
            BinaryPath(DATASET_PATH + PLAN_NAME + "/" + TRACK_NAME + "/signals.txt"),       \
            BinaryPath(DATASET_PATH + PLAN_NAME + "/" + TRACK_NAME + "/sensors.txt"),       \
            BinaryPath(DATASET_PATH + PLAN_NAME + "/" + TRACK_NAME + "/ground_truth.txt"),  \
            ANDROID_OLD_INDOOR);                                                            \
        doPositioning(test);                                                                \
        checkMetrics(test);                                                                 \
    }                                                                                       \
    BOOST_AUTO_TEST_CASE(android_indoor_##TEST_NAME) {                                      \
        GroundTruthTest test(                                                               \
            #TEST_NAME "_android_indoor",                                                   \
            BinaryPath(DATASET_PATH + PLAN_NAME + "/tile.pb"),                              \
            BinaryPath(DATASET_PATH + PLAN_NAME + "/" + TRACK_NAME + "/signals.txt"),       \
            BinaryPath(DATASET_PATH + PLAN_NAME + "/" + TRACK_NAME + "/sensors.txt"),       \
            BinaryPath(DATASET_PATH + PLAN_NAME + "/" + TRACK_NAME + "/ground_truth.txt"),  \
            ANDROID_INDOOR);                                                                \
        doPositioning(test);                                                                \
        checkMetrics(test);                                                                 \
    }                                                                                       \
    BOOST_AUTO_TEST_CASE(android_fused_##TEST_NAME) {                                       \
        GroundTruthTest test(                                                               \
            #TEST_NAME "_android_fused",                                                    \
            BinaryPath(DATASET_PATH + PLAN_NAME + "/tile.pb"),                              \
            BinaryPath(DATASET_PATH + PLAN_NAME + "/" + TRACK_NAME + "/signals.txt"),       \
            BinaryPath(DATASET_PATH + PLAN_NAME + "/" + TRACK_NAME + "/sensors.txt"),       \
            BinaryPath(DATASET_PATH + PLAN_NAME + "/" + TRACK_NAME + "/ground_truth.txt"),  \
            ANDROID_FUSED);                                                                 \
        doPositioning(test);                                                                \
        checkMetrics(test);                                                                 \
    }                                                                                       \


GROUND_TRUTH_TRACK_TEST(Skolkovo_level_1_track_26, "Skolkovo", "level_1_26")
GROUND_TRUTH_TRACK_TEST(Skolkovo_level_1_track_30, "Skolkovo", "level_1_30")
GROUND_TRUTH_TRACK_TEST(Skolkovo_level_1_track_31, "Skolkovo", "level_1_31")
GROUND_TRUTH_TRACK_TEST(Skolkovo_level_1_track_9a, "Skolkovo", "level_1_9a")
GROUND_TRUTH_TRACK_TEST(Skolkovo_level_1_track_dc, "Skolkovo", "level_1_dc")

GROUND_TRUTH_TRACK_TEST(Ohotnyi_Ryad_level_1_track_81, "Ohotnyi_Ryad", "level_-1_track_81")
GROUND_TRUTH_TRACK_TEST(Ohotnyi_Ryad_level_1_track_86, "Ohotnyi_Ryad", "level_-1_track_86")
GROUND_TRUTH_TRACK_TEST(Ohotnyi_Ryad_level_2_track_49, "Ohotnyi_Ryad", "level_-2_track_49")
GROUND_TRUTH_TRACK_TEST(Ohotnyi_Ryad_level_2_track_62, "Ohotnyi_Ryad", "level_-2_track_62")
GROUND_TRUTH_TRACK_TEST(Ohotnyi_Ryad_level_2_track_95, "Ohotnyi_Ryad", "level_-2_track_95")
GROUND_TRUTH_TRACK_TEST(Ohotnyi_Ryad_level_3_track_3c, "Ohotnyi_Ryad", "level_-3_track_3c")
GROUND_TRUTH_TRACK_TEST(Ohotnyi_Ryad_level_3_track_48, "Ohotnyi_Ryad", "level_-3_track_48")
GROUND_TRUTH_TRACK_TEST(Ohotnyi_Ryad_level_3_track_ce, "Ohotnyi_Ryad", "level_-3_track_ce")
