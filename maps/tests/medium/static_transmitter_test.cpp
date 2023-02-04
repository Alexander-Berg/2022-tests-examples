#include <maps/indoor/libs/db/include/static_transmitter_gateway.h>
#include <maps/indoor/libs/db/include/static_transmitter.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>
#include <util/generic/guid.h>

#include <maps/indoor/libs/unittest/fixture.h>

namespace maps::mirc::static_transmitter::tests {
using namespace testing;

namespace {

const std::string TEST_INDOOR_PLAN_ID = "12345";
const std::string TEST_INDOOR_LEVEL_ID = "1a";
const db::ugc::TransmitterType TEST_TRANSMITTER_TYPE = db::ugc::TransmitterType::Ble;
const std::string TEST_TRANSMITTER_ID = "123-abc";
const double TEST_SIGNAL_PARAMETER_A = 0.1;
const double TEST_SIGNAL_PARAMETER_B = 0.2;
const double TEST_LATITUDE = 0.3;
const double TEST_LONGITUDE = 0.4;
const std::string TEST_VERSION = "1234-abc";
const auto TEST_STATUS = db::ugc::StaticTransmitterStatus::Active;

db::ugc::StaticTransmitter getTestStaticTransmitter() {
    return db::ugc::StaticTransmitter(
        TEST_INDOOR_PLAN_ID,
        TEST_INDOOR_LEVEL_ID,
        TEST_TRANSMITTER_TYPE,
        TEST_TRANSMITTER_ID,
        TEST_SIGNAL_PARAMETER_A,
        TEST_SIGNAL_PARAMETER_B,
        TEST_LATITUDE,
        TEST_LONGITUDE,
        TEST_VERSION,
        std::nullopt, // description
        TEST_STATUS,
        std::nullopt // originalId
    );
}

} // namespace

Y_UNIT_TEST_SUITE_F(static_transmitter_gateway_tests, unittest::Fixture) {

Y_UNIT_TEST(static_transmitter_test)
{
    {
        const auto start = chrono::TimePoint::clock::now();
        auto staticTransmitter = getTestStaticTransmitter();
        auto txn = pgPool().masterWriteableTransaction();
        auto gw = db::ugc::StaticTransmitterGateway(*txn);
        EXPECT_EQ(staticTransmitter.id(), 0u);
        ASSERT_NO_THROW(gw.insert(staticTransmitter));
        const auto end = chrono::TimePoint::clock::now();
        EXPECT_EQ(staticTransmitter.id(), 1u);

        EXPECT_LE(start, staticTransmitter.createdAt());
        EXPECT_LE(staticTransmitter.createdAt(), end);

        EXPECT_LE(start, staticTransmitter.modifiedAt());
        EXPECT_LE(staticTransmitter.modifiedAt(), end);

        txn->commit();
    }

    {
        auto txn = pgPool().slaveTransaction();
        auto staticTransmitters = db::ugc::StaticTransmitterGateway{*txn}.load();
        ASSERT_EQ(staticTransmitters.size(), 1u);
        EXPECT_EQ(staticTransmitters[0].id(), 1u);
        EXPECT_EQ(staticTransmitters[0].indoorPlanId(), TEST_INDOOR_PLAN_ID);
        EXPECT_EQ(staticTransmitters[0].indoorLevelId(), TEST_INDOOR_LEVEL_ID);
        EXPECT_EQ(staticTransmitters[0].txType(), TEST_TRANSMITTER_TYPE);
        EXPECT_EQ(staticTransmitters[0].txId(), TEST_TRANSMITTER_ID);
        EXPECT_EQ(staticTransmitters[0].signalParameterA(), TEST_SIGNAL_PARAMETER_A);
        EXPECT_EQ(staticTransmitters[0].signalParameterB(), TEST_SIGNAL_PARAMETER_B);
        EXPECT_EQ(staticTransmitters[0].latitude(), TEST_LATITUDE);
        EXPECT_EQ(staticTransmitters[0].longitude(), TEST_LONGITUDE);
        EXPECT_EQ(staticTransmitters[0].status(), TEST_STATUS);
    }


    {
        auto txn = pgPool().masterWriteableTransaction();
        auto gw = db::ugc::StaticTransmitterGateway(*txn);
        auto staticTransmitter = getTestStaticTransmitter();
        ASSERT_NO_THROW(gw.insert(staticTransmitter));
        EXPECT_EQ(staticTransmitter.id(), 2u);
        txn->commit();
    }
    {
        auto txn = pgPool().slaveTransaction();
        auto loadedTransmitters = db::ugc::StaticTransmitterGateway{*txn}.load();
        EXPECT_EQ(loadedTransmitters.size(), 2u);
    }
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::mirc::static_transmitter::tests
