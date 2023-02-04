#include <maps/indoor/libs/unittest/fixture.h>

#include <maps/indoor/libs/unittest/include/yandex/maps/mirc/unittest/unittest_config.h>
#include <maps/indoor/long-tasks/src/evotor/include/persistent_id_cache.h>
#include <maps/libs/http/include/test_utils.h>

namespace maps::mirc::evotor::persistent_id_cache::tests {

namespace {

void prepareDataBase(pgpool3::Pool& pool_)
{
    static const std::string query = R"end(
        INSERT INTO ugc.evotor_transmitter
        (
            evotor_transmitter_id,
            source_id,
            transmitter_id,
            valid_since,
            valid_until
        )
        VALUES
        (0, '0', '1', 1, 2000000000),
        (1, '1', '2', 1, 2)
    )end";

    auto txn = pool_.masterWriteableTransaction();
    txn->exec(query);
    txn->commit();
}

} // namespace

Y_UNIT_TEST_SUITE_F(persistent_id_cache_tests, unittest::Fixture) 
{

Y_UNIT_TEST(get_source_id_by_timstamp_exist_test)
{
    prepareDataBase(pgPool());

    PersistentIdCache persistentIdCache(pgPool());
    auto timeStamp = chrono::TimePoint::clock::now();
    auto sourceId = persistentIdCache.getSourceIdByTimeStamp(timeStamp, "1");
    UNIT_ASSERT(sourceId);
}

Y_UNIT_TEST(get_source_id_by_timstamp_not_exist_test)
{
    prepareDataBase(pgPool());

    PersistentIdCache persistentIdCache(pgPool());
    auto timeStamp = chrono::TimePoint::clock::now();
    auto sourceId = persistentIdCache.getSourceIdByTimeStamp(timeStamp, "2");
    UNIT_ASSERT(!sourceId);
}

Y_UNIT_TEST(get_transmitters_id_by_timestamp_exist_test) {
    prepareDataBase(pgPool());

    PersistentIdCache persistentIdCache(pgPool());
    auto timeStamp = chrono::TimePoint::clock::now();
    auto transmittersId = persistentIdCache.getTransmittersIdBySourceId(
        timeStamp,
        "0"
    );
    UNIT_ASSERT_VALUES_EQUAL(transmittersId.size(), size_t{1});
    UNIT_ASSERT_VALUES_EQUAL(transmittersId[0], "1");
}

Y_UNIT_TEST(get_transmitters_id_by_timestamp_not_exist_test) {
    prepareDataBase(pgPool());

    PersistentIdCache persistentIdCache(pgPool());
    auto timeStamp = chrono::TimePoint::clock::now();
    auto transmitterId = persistentIdCache.getTransmittersIdBySourceId(
        timeStamp,
        "1"
    );

    // timeStamp > validUntil
    UNIT_ASSERT(transmitterId.empty());

    transmitterId = persistentIdCache.getTransmittersIdBySourceId(
        timeStamp,
        "2"
    );

    // sourceId does not exist in DB
    UNIT_ASSERT(transmitterId.empty());
}

}

} // namespace maps::mirc::evotor::persistent_id_cache::tests
