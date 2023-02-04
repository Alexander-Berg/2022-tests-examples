#include <boost/test/unit_test.hpp>
#include <library/cpp/testing/common/env.h>

#include <fstream>

#include <maps/indoor/libs/indoor_positioning/impl/sensor_fusion.h>
#include <maps/indoor/libs/indoor_positioning/include/sensor.h>
#include <maps/indoor/libs/indoor_positioning/include/transmitter.h>
#include <maps/indoor/libs/indoor_positioning/include/utils.h>
#include <maps/libs/common/include/exception.h>
#include <maps/libs/log8/include/log8.h>

using namespace boost::unit_test;
using namespace INDOOR_POSITIONING_NAMESPACE;

namespace {

constexpr double STEP_COUNTER_PRECISION_THRESHOLD = 10; // percent of steps
constexpr double STEP_LENGTH_PRECISION_THRESHOLD = 10;  // percent of steps length
constexpr double ANGLE_PRECISION_THRESHOLD = 2.0;       // deg/sec

const std::string DATASET_PATH = "maps/indoor/libs/indoor_positioning/tests/logs/";

struct SensorLog
{
    std::string name;
    size_t stepCount  = 0;
    double stepLength = 0.0;
    double deltaAngle = 0.0;
    SensorMeasurements sensors;
};

SensorLog readSensorLog(const std::string& name)
{
    std::string filename = BinaryPath(DATASET_PATH + name);

    std::ifstream dataStream(filename);
    REQUIRE(dataStream.is_open(), "Cannot open resource " + name);

    SensorLog log;
    log.name = name;

    if (dataStream >> log.stepCount >> log.stepLength >> log.deltaAngle) {
        std::string sensorType;
        long long timestamp;
        double x, y, z;
        while (dataStream >> sensorType >> timestamp >> x >> y >> z) {
            SensorMeasurement sensor;
            REQUIRE(sensorType == "ACC" || sensorType == "GYR", "Invalid resource " + name);
            sensor.type = sensorType == "ACC" ?
                SensorType::ACCELEROMETER :
                SensorType::GYROSCOPE;
            sensor.timestamp = Timestamp(std::chrono::milliseconds(timestamp));
            sensor.values[0] = x;
            sensor.values[1] = y;
            sensor.values[2] = z;
            log.sensors.push_back(sensor);
        }
    }

    REQUIRE(dataStream.eof(), "Invalid resource" + name);
    REQUIRE(log.stepCount > 0, "Invalid resource " + name);
    REQUIRE(log.stepLength > 0, "Invalid resource " + name);
    return log;
}

void testPedometer(const SensorLog& log)
{
    REQUIRE(!log.sensors.empty(), "Sensors log shouldn't be empty");

    Pedometer pedometer;
    size_t stepCount = 0;
    double stepLength = 0;

    for(const auto& sensor : log.sensors) {
        pedometer.update(sensor);
        auto step = pedometer.stepLength();
        if (step) {
            stepCount++;
            stepLength += *step;
        }
    }

    stepLength /= std::max<size_t>(stepCount, 1);

    INFO() << log.name << ":: stepCounter: " << stepCount << " (should be " << log.stepCount << ")";
    INFO() << log.name << ":: stepLength: " << stepLength << " (should be " << log.stepLength << ")";

    BOOST_CHECK_CLOSE(
        static_cast<double>(stepCount),
        static_cast<double>(log.stepCount),
        STEP_COUNTER_PRECISION_THRESHOLD);

    BOOST_CHECK_CLOSE(stepLength, log.stepLength, STEP_LENGTH_PRECISION_THRESHOLD);
}

void testOrientationFilter(const SensorLog& log)
{
    if (log.sensors.empty())
        return;

    OrientationFilter orientationFilter;
    double prevDirection = orientationFilter.getHorizontalDirection();
    double deltaAngle = 0;

    for(const auto& sensor : log.sensors) {
        orientationFilter.update(sensor);
        double direction = orientationFilter.getHorizontalDirection();
        double delta = direction - prevDirection;

        if (delta > M_PI)
            delta -= 2 * M_PI;
        if (delta < -M_PI)
            delta += 2 * M_PI;

        deltaAngle += delta;
        prevDirection = direction;
    }

    deltaAngle = radToDeg(deltaAngle);
    const auto ts0 = log.sensors.front().timestamp;
    const auto ts1 = log.sensors.back().timestamp;
    const double dt = std::chrono::duration<double>(ts1 - ts0).count();
    const double precision = std::fabs(deltaAngle - log.deltaAngle) / std::max(dt, 0.1);

    INFO() << log.name << ":: deltaAngle: " << deltaAngle << " (should be " << log.deltaAngle << ")";
    INFO() << log.name << ":: deltaTime: " << dt;
    INFO() << log.name << ":: angle precision: " << precision;

    BOOST_CHECK_EQUAL(precision < ANGLE_PRECISION_THRESHOLD, true);
}

} // namespace

BOOST_AUTO_TEST_CASE(pedometer_huawei)
{
    const auto sensorLog = readSensorLog("Huawei.log");
    testPedometer(sensorLog);
}

BOOST_AUTO_TEST_CASE(pedometer_iphone)
{
    const auto sensorLog = readSensorLog("iPhone.log");
    testPedometer(sensorLog);
}

BOOST_AUTO_TEST_CASE(pedometer_nexus)
{
    const auto sensorLog = readSensorLog("Nexus.log");
    testPedometer(sensorLog);
}

BOOST_AUTO_TEST_CASE(sensor_fusion_samsung)
{
    const auto sensorLog = readSensorLog("Samsung.log");
    testPedometer(sensorLog);
    testOrientationFilter(sensorLog);
}

BOOST_AUTO_TEST_CASE(sensor_fusion_sony_xperia_1)
{
    const auto sensorLog = readSensorLog("SonyXperia-1.log");
    testPedometer(sensorLog);
    testOrientationFilter(sensorLog);
}

BOOST_AUTO_TEST_CASE(sensor_fusion_sony_xperia_2)
{
    const auto sensorLog = readSensorLog("SonyXperia-2.log");
    testPedometer(sensorLog);
    testOrientationFilter(sensorLog);
}

BOOST_AUTO_TEST_CASE(sensor_fusion_sony_xperia_3)
{
    const auto sensorLog = readSensorLog("SonyXperia-3.log");
    testPedometer(sensorLog);
    testOrientationFilter(sensorLog);
}

BOOST_AUTO_TEST_CASE(sensor_fusion_zte_1)
{
    const auto sensorLog = readSensorLog("ZTE-1.log");
    testPedometer(sensorLog);
    testOrientationFilter(sensorLog);
}

BOOST_AUTO_TEST_CASE(sensor_fusion_zte_2)
{
    const auto sensorLog = readSensorLog("ZTE-2.log");
    testPedometer(sensorLog);
    testOrientationFilter(sensorLog);
}

BOOST_AUTO_TEST_CASE(sensor_fusion_zte_3)
{
    const auto sensorLog = readSensorLog("ZTE-3.log");
    testPedometer(sensorLog);
    testOrientationFilter(sensorLog);
}
