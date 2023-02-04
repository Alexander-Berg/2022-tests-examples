#include <maps/automotive/parking/fastcgi/parking_receiver/lib/vehicle_data_storage.h>
#include <maps/automotive/parking/lib/util/include/defines.h>
#include <maps/automotive/parking/lib/track_spotter/include/track_spotter.h>

#include <maps/libs/mongo/include/init.h>

#include <library/cpp/testing/unittest/registar.h>

#include <cstdlib>
#include <string>

namespace maps::automotive::parking::receiver::tests {

using DeviceId = maps::automotive::parking::DeviceId;
using Point2 = maps::geolib3::Point2;

namespace {

using namespace maps::automotive::parking::track_spotter;

// value from maps/automotive/parking/recipes/mongodb/__main__.py
constexpr char ENV_MONGO_PORT[] = "RECIPE_MONGO_PORT";

const std::string MONGO_DB = "parking-device-state-test";
constexpr TimestampSec TS_ORIGINAL = 42157;

std::string mongoUri() {
    const std::string port = std::getenv(ENV_MONGO_PORT);
    UNIT_ASSERT(!port.empty());
    return "mongodb://127.0.0.1:" + port;
}

Position pos(TimestampSec ts, double lon, double lat) {
    return Position{ts, Point2{lon, lat}};
}

Path generatePath(TimestampSec ts) {
    return Path{{pos(ts-5, 1.0, 1.0), pos(ts, 100.0, 100.0)}, ts};
}

TelemetryEvents generateFewTelemetryEvents() {
    return TelemetryEvents {
        { TS_ORIGINAL    , IgnitionState::LOCK,  std::nullopt, std::nullopt },
        { TS_ORIGINAL + 1, IgnitionState::OFF,   std::nullopt, std::nullopt },
        { TS_ORIGINAL + 2, IgnitionState::ACC,   std::nullopt, std::nullopt },
        { TS_ORIGINAL + 3, IgnitionState::ON,    std::nullopt, std::nullopt },
        { TS_ORIGINAL + 4, IgnitionState::START, std::nullopt, std::nullopt },
    };
}

void checkIsAlive(std::unique_ptr<VehicleDataStorage> storage) {
    UNIT_ASSERT(storage->isAlive());
}

void checkUpdatePathWithNewerTimestamp(std::unique_ptr<VehicleDataStorage> storage) {
    const DeviceId deviceId = "checkUpdatePathWithNewerTimestamp";
    auto pathOriginal = generatePath(TS_ORIGINAL);
    UNIT_ASSERT(storage->updatePath(deviceId, pathOriginal));

    track_spotter::TimestampSec tsNewer = TS_ORIGINAL + 1;
    auto pathNewer = generatePath(tsNewer);
    UNIT_ASSERT(storage->updatePath(deviceId, pathNewer));

    auto vehicleData = storage->loadVehicleData(deviceId);
    UNIT_ASSERT_EQUAL(vehicleData.path_, pathNewer);
}

void checkUpdatePathWithOlderTimestamp(std::unique_ptr<VehicleDataStorage> storage) {
    const DeviceId deviceId = "checkUpdatePathWithOlderTimestamp";
    auto pathOriginal = generatePath(TS_ORIGINAL);
    UNIT_ASSERT(storage->updatePath(deviceId, pathOriginal));

    track_spotter::TimestampSec tsOlder = TS_ORIGINAL - 1;
    auto pathOlder = generatePath(tsOlder - 1);
    UNIT_ASSERT(!storage->updatePath(deviceId, pathOlder));

    auto vehicleData = storage->loadVehicleData(deviceId);
    UNIT_ASSERT_EQUAL(vehicleData.path_, pathOriginal);
}

void checkAppendTelemetryEvents(std::unique_ptr<VehicleDataStorage> storage) {
    const DeviceId deviceId = "CheckAppendTelemetryEvents";
    const auto events = generateFewTelemetryEvents();

    storage->appendTelemetryEvents(deviceId, events);

    auto vehicleData = storage->loadVehicleData(deviceId);
    UNIT_ASSERT_EQUAL(vehicleData.telemetry_.events_, events);
}

void checkCompareAndStoreStatusWithMatchingOriginalStatus(std::unique_ptr<VehicleDataStorage> storage) {
    const DeviceId deviceId = "checkCompareAndStoreStatusWithMatchingOriginalStatus";
    const auto events = generateFewTelemetryEvents();
    storage->appendTelemetryEvents(deviceId, events);
    auto originalStatus = storage->loadVehicleData(deviceId).status_;

    VehicleStatus newStatus = VehicleStatus{CarState::MOVING, Point2{1.0, 1.0}, 112330, 1};
    UNIT_ASSERT(storage->compareAndStoreStatus(deviceId, originalStatus, newStatus));

    auto updatedStatus = storage->loadVehicleData(deviceId).status_;
    UNIT_ASSERT_EQUAL(updatedStatus, newStatus);
}

void checkCompareAndStoreStatusWithMismatchingOriginalStatus(std::unique_ptr<VehicleDataStorage> storage) {
    const DeviceId deviceId = "checkCompareAndStoreStatusWithMatchingOriginalStatus";
    const auto events = generateFewTelemetryEvents();
    storage->appendTelemetryEvents(deviceId, events);
    auto originalStatus = storage->loadVehicleData(deviceId).status_;

    VehicleStatus wrongOriginalStatus = VehicleStatus{CarState::PARKED, Point2{1.0, 1.0}, 110000, 1};
    VehicleStatus newStatus = VehicleStatus{CarState::MOVING, Point2{1.0, 1.0}, 112330, 1};
    UNIT_ASSERT(!storage->compareAndStoreStatus(deviceId, wrongOriginalStatus, newStatus));

    auto updatedStatus = storage->loadVehicleData(deviceId).status_;
    UNIT_ASSERT_EQUAL(updatedStatus, originalStatus);
}

struct Fixture : NUnitTest::TBaseFixture {
    Fixture()
    {
        maps::mongo::init();
    }
};

} // anonymous namespace

Y_UNIT_TEST_SUITE_F(test_vehicle_data_storage_in_memory, Fixture) {

    Y_UNIT_TEST(check_is_alive)
    {
        checkIsAlive(createInMemoryStorage());
    }

    Y_UNIT_TEST(check_update_path_with_newer_timestamp)
    {
        checkUpdatePathWithNewerTimestamp(createInMemoryStorage());
    }

    Y_UNIT_TEST(check_update_path_with_older_timestamp)
    {
        checkUpdatePathWithOlderTimestamp(createInMemoryStorage());
    }

    Y_UNIT_TEST(check_append_telemetry_events)
    {
        checkAppendTelemetryEvents(createInMemoryStorage());
    }

    Y_UNIT_TEST(check_compare_and_store_status_with_matching_original_status)
    {
        checkCompareAndStoreStatusWithMatchingOriginalStatus(createInMemoryStorage());
    }

    Y_UNIT_TEST(check_compare_and_store_status_with_mismatching_original_status)
    {
        checkCompareAndStoreStatusWithMismatchingOriginalStatus(createInMemoryStorage());
    }

} //Y_UNIT_TEST_SUITE

Y_UNIT_TEST_SUITE(test_vehicle_data_storage_mongo) {

    Y_UNIT_TEST(check_is_alive)
    {
        checkIsAlive(createMongoStorage(mongoUri(), MONGO_DB));
    }

    Y_UNIT_TEST(check_update_path_with_newer_timestamp)
    {
        checkUpdatePathWithNewerTimestamp(createMongoStorage(mongoUri(), MONGO_DB));
    }

    Y_UNIT_TEST(check_update_path_with_older_timestamp)
    {
        checkUpdatePathWithOlderTimestamp(createMongoStorage(mongoUri(), MONGO_DB));
    }

    Y_UNIT_TEST(check_append_telemetry_events)
    {
        checkAppendTelemetryEvents(createMongoStorage(mongoUri(), MONGO_DB));
    }

    Y_UNIT_TEST(check_compare_and_store_status_with_matching_original_status)
    {
        checkCompareAndStoreStatusWithMatchingOriginalStatus(createMongoStorage(mongoUri(), MONGO_DB));
    }

    Y_UNIT_TEST(check_compare_and_store_status_with_mismatching_original_status)
    {
        checkCompareAndStoreStatusWithMismatchingOriginalStatus(createMongoStorage(mongoUri(), MONGO_DB));
    }

} //Y_UNIT_TEST_SUITE

} //maps::automotive::parking::tests
