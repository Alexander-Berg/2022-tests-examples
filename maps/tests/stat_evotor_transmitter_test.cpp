#include <maps/indoor/libs/unittest/fixture.h>
#include <maps/indoor/libs/unittest/include/yandex/maps/mirc/unittest/unittest_config.h>

#include <maps/indoor/long-tasks/src/stat-evotor-transmitter/lib/worker.h>

#include <maps/libs/chrono/include/time_point.h>
#include <maps/libs/http/include/test_utils.h>

namespace maps::mirc::stat_evotor_transmitter::tests {

namespace {

void addSourceIdInEvotorTransmitterDB(
    chrono::TimePoint timestamp,
    std::string sourceId,
    int64_t evotorTransmitterId,
    pgpool3::Pool& pool_)
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

    auto txn = pool_.masterWriteableTransaction();

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

void addSourceIdInEvotorSentDataDB(
    std::string sourceId,
    int64_t evotorSendDataId,
    pgpool3::Pool& pool_)
{
    static const std::string query = R"end(
        INSERT INTO ugc.evotor_sent_data (
            evotor_sent_data_id,
            source_id,
            sent_at
        )
        VALUES (
            {0},
            {1},
            NOW()
        )
    )end";

    auto txn = pool_.masterWriteableTransaction();

    txn->exec(fmt::format(
        query,
        txn->quote(evotorSendDataId),
        txn->quote(sourceId)
    ));

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
    addSourceIdInEvotorSentDataDB("1", 1, pool);

    stat_evotor_transmitter::Worker worker(pgPool());
    worker.run();

    static const std::string query = R"end(
        SELECT total_count, total_valid_count, total_sent, total_valid_sent
        FROM stat.evotor_transmitter;
    )end";

    auto txn = pool.slaveTransaction();
    auto rows = txn->exec(query);

    UNIT_ASSERT_VALUES_EQUAL(rows.size(), 1u);
    auto row = rows[0];

    enum {
        transmitters_count_idx,
        total_valid_count_idx,
        total_sent_idx,
        total_valid_sent_idx
    };

    UNIT_ASSERT_VALUES_EQUAL(row[transmitters_count_idx].as<int64_t>(), 1);
    UNIT_ASSERT_VALUES_EQUAL(row[total_valid_count_idx].as<int64_t>(), 1);
    UNIT_ASSERT_VALUES_EQUAL(row[total_sent_idx].as<int64_t>(), 1);
    UNIT_ASSERT_VALUES_EQUAL(row[total_valid_sent_idx].as<int64_t>(), 1);
}

Y_UNIT_TEST(worker_with_invalid_timestamp)
{
    auto& pool = pgPool();

    auto currentTime = chrono::TimePoint::clock::now() - std::chrono::hours(6);
    addSourceIdInEvotorTransmitterDB(currentTime, "1", 1, pool);
    addSourceIdInEvotorSentDataDB("1", 1, pool);

    stat_evotor_transmitter::Worker worker(pgPool());
    worker.run();

    static const std::string query = R"end(
        SELECT total_count, total_valid_count, total_sent, total_valid_sent
        FROM stat.evotor_transmitter;
    )end";

    auto txn = pool.slaveTransaction();
    auto rows = txn->exec(query);

    UNIT_ASSERT_VALUES_EQUAL(rows.size(), 1u);
    auto row = rows[0];

    enum {
        transmitters_count_idx,
        total_valid_count_idx,
        total_sent_idx,
        total_valid_sent_idx
    };

    UNIT_ASSERT_VALUES_EQUAL(row[transmitters_count_idx].as<int64_t>(), 1);
    UNIT_ASSERT_VALUES_EQUAL(row[total_valid_count_idx].as<int64_t>(), 0);
    UNIT_ASSERT_VALUES_EQUAL(row[total_sent_idx].as<int64_t>(), 1);
    UNIT_ASSERT_VALUES_EQUAL(row[total_valid_sent_idx].as<int64_t>(), 0);
}

}

} // namespace maps::mirc::stat_evotor_transmitter::tests
