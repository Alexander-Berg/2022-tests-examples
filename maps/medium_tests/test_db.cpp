#include <maps/wikimap/feedback/api/src/libs/test_helpers/db_fixture.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::feedback::api::dbqueries::tests {

namespace {

auto& dbPool()
{
    static api::tests::DbFixture db;
    return db.pool();
}

} // namespace

Y_UNIT_TEST_SUITE(test_db)
{

Y_UNIT_TEST(select_from_txn)
{
    {
        auto txn = dbPool().masterWriteableTransaction();
        auto result = txn->exec("SELECT 1");
        UNIT_ASSERT_EQUAL(result[0][0].as<size_t>(), 1);
    }
    {
        auto txn = dbPool().masterReadOnlyTransaction();
        auto result = txn->exec("SELECT 2");
        UNIT_ASSERT_EQUAL(result[0][0].as<size_t>(), 2);
    }
    {
        auto txn = dbPool().slaveTransaction();
        auto result = txn->exec("SELECT 3");
        UNIT_ASSERT_EQUAL(result[0][0].as<size_t>(), 3);
    }
}

Y_UNIT_TEST(find_feedback_tables)
{
    auto query =
        "SELECT COUNT(*)"
        " FROM information_schema.tables"
        " WHERE table_name LIKE 'feedback%'";

    auto txn = dbPool().slaveTransaction();
    auto result = txn->exec(query)[0][0].as<size_t>();
    UNIT_ASSERT_EQUAL(result, 3);
}

} // test_db suite

} // namespace maps::wiki::feedback::api::dbqueries::tests
