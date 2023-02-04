#include <maps/indoor/libs/db/include/task_gateway.h>

#include <maps/indoor/libs/unittest/fixture.h>
#include <maps/indoor/libs/unittest/include/yandex/maps/mirc/unittest/unittest_config.h>

#include <maps/indoor/long-tasks/src/radiomap-evaluation-cron-job/lib/impl/utils.h>
#include <maps/indoor/long-tasks/src/stat-evotor-transmitter-indoor/lib/worker.h>

#include <maps/libs/chrono/include/time_point.h>
#include <maps/libs/http/include/test_utils.h>

namespace maps::mirc::stat_evotor_transmitter_indoor::tests {

namespace {

const geolib3::Polyline2 TEST_PATH = geolib3::Polyline2({{
    radiomap_evaluator::localPlanarToGeodetic(geolib3::Point2(0, 0), geolib3::Point2(1, 1)),
    radiomap_evaluator::localPlanarToGeodetic(geolib3::Point2(1, 1), geolib3::Point2(2, 2))
}});
const std::string TEST_INDOOR_LEVEL_ID = "1";

void addSourceIdInEvotorTransmitterDB(
    chrono::TimePoint timestamp,
    std::string sourceId,
    int64_t evotorTransmitterId,
    pgpool3::Pool& pool)
{
    static const std::string query = R"end(
        INSERT INTO ugc.evotor_transmitter (
            evotor_transmitter_id,
            source_id,
            transmitter_id,
            valid_since,
            valid_until
        )
        VALUES (
            {0},
            {1},
            1,
            {2},
            {3}
        )
    )end";

    auto txn = pool.masterWriteableTransaction();

    auto validUntill = timestamp + std::chrono::hours(3);
    auto validSince = timestamp - std::chrono::hours(3);

    txn->exec(fmt::format(
        query,
        txn->quote(evotorTransmitterId),
        txn->quote(sourceId),
        txn->quote(std::chrono::duration_cast<std::chrono::seconds>(validSince.time_since_epoch()).count()),
        txn->quote(std::chrono::duration_cast<std::chrono::seconds>(validUntill.time_since_epoch()).count())
    ));

    txn->commit();
}

void addRadiomapInRadiomapTransmitterDB(
    std::string sourceId,
    std::string indooPlanId,
    chrono::TimePoint evaluatedAt,
    pgpool3::Pool& pool)
{
    static const std::string query = R"end(
        INSERT INTO ugc.radiomap_transmitter (
            indoor_plan_id,
            indoor_level_universal_id,
            transmitter_id,
            source_Id,
            geom,
            signal_parameter_a,
            signal_parameter_b,
            evaluated_at
        )
        VALUES (
            {0},
            0,
            0,
            {1},
            ST_SetSRID(ST_MakePoint(1.1, 2.2), 4326),
            1.0,
            1.0,
            {2}
        )

    )end";

    auto txn = pool.masterWriteableTransaction();

    evaluatedAt -= std::chrono::hours(1);

    txn->exec(fmt::format(
        query,
        txn->quote(indooPlanId),
        txn->quote(sourceId),
        txn->quote(chrono::formatSqlDateTime(evaluatedAt))
    ));

    txn->commit();
}

void addTaskDB(
    std::string indoorPlanId,
    pgpool3::Pool& pool)

{
    db::ugc::Task task;
    auto txn = pool.masterWriteableTransaction();

    task.setStatus(db::ugc::TaskStatus::Available)
        .setDistanceInMeters(40000000)
        .setGeodeticPoint(geolib3::Point2(1.2, 3.4))
        .setIndoorPlanId(indoorPlanId);
    task.addPath(
        TEST_PATH,
        TEST_INDOOR_LEVEL_ID,
        std::chrono::seconds(100),
        40000,
        std::nullopt);

    db::ugc::TaskGateway{*txn}.insert(task);
    txn->commit();
}

} // namespace

Y_UNIT_TEST_SUITE_F(stat_evotor_transmitter_tests, unittest::Fixture)
{

Y_UNIT_TEST(worker_with_valid_timestamp)
{
    auto& pool = pgPool();

    auto currentTime = chrono::TimePoint::clock::now();
    addSourceIdInEvotorTransmitterDB(currentTime, "1", 1, pool);
    currentTime -= std::chrono::seconds(1);
    addSourceIdInEvotorTransmitterDB(currentTime, "2", 2, pool);
    addRadiomapInRadiomapTransmitterDB("1", "1", currentTime, pool);
    addRadiomapInRadiomapTransmitterDB("2", "1", currentTime, pool);
    addTaskDB("1", pool);

    stat_evotor_transmitter_indoor::Worker worker(pgPool());
    worker.run();

    static const std::string query = R"end(
        SELECT indoor_plan_id, source_id_count, lon, lat
        FROM stat.evotor_transmitter_indoor;
    )end";

    auto txn = pool.slaveTransaction();
    auto rows = txn->exec(query);

    UNIT_ASSERT_VALUES_EQUAL(rows.size(), 1u);
    auto row = rows[0];

    enum {
        indoor_plan_id_idx,
        source_id_count_idx,
        lon_idx,
        lat_idx
    };

    UNIT_ASSERT_VALUES_EQUAL(row[indoor_plan_id_idx].as<std::string>(), "1");
    UNIT_ASSERT_VALUES_EQUAL(row[source_id_count_idx].as<int64_t>(), 2);
    UNIT_ASSERT_VALUES_EQUAL(row[lon_idx].as<double>(), 1.2);
    UNIT_ASSERT_VALUES_EQUAL(row[lat_idx].as<double>(), 3.4);
}

}

} // namespace maps::mirc::stat_evotor_transmitter_indoor::tests
