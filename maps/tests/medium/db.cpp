#include <yandex/maps/mirc/unittest/database_fixture.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::mirc::unittest::tests {

Y_UNIT_TEST_SUITE_F(db, WithUnittestConfig<DatabaseFixture>) {

Y_UNIT_TEST(test_data_1)
{
    auto txn = pool().masterWriteableTransaction();
    txn->exec(R"(
        INSERT INTO ugc.task (task_id, status, geom, duration, distance, interval, created_by)
        VALUES (2, 'available', ST_GeomFromText('Point(0 0)',4326), 3600, 3.0, 100, '111')
    )");
    txn->exec(R"(
        INSERT INTO ugc.task_path (task_path_id, task_id, geom, indoor_level_universal_id, duration, distance)
        VALUES (1, 2, ST_GeomFromText('LINESTRING(0 0, 1 1)',4326), 'test_level', 1800, 4.0)
    )");
    auto res = txn->exec("SELECT COUNT(*) FROM ugc.task_path;");
    EXPECT_EQ(res.front().front().as<size_t>(), 1u);
    txn->commit();
}

Y_UNIT_TEST(test_data_2)
{
    auto txn = pool().masterReadOnlyTransaction();
    auto res = txn->exec("SELECT COUNT(*) FROM ugc.task_path;");
    EXPECT_EQ(res.front().front().as<size_t>(), 0u);
}

} //Y_UNIT_TEST_SUITE_F

} //namespace maps::mirc::unittests::tests
