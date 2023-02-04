#include <maps/wikimap/feedback/api/src/yacare/lib/params.h>

#include <maps/wikimap/feedback/api/src/libs/feedback_task_query_builder/where_conditions.h>
#include <maps/wikimap/feedback/api/src/libs/test_helpers/db_fixture.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::feedback::api::tests {

namespace {

auto& dbPool()
{
    static api::tests::DbFixture db;
    return db.pool();
}

using feedback_task_query_builder::WhereConditions;

} // namespace

Y_UNIT_TEST_SUITE(test_search_param)
{

Y_UNIT_TEST(update_where_conditions_with_search_param)
{
    auto txn = dbPool().masterReadOnlyTransaction();
    {
        WhereConditions whereConditions;
        updateWhereConditionsWithSearchParam(
            whereConditions, "9c4b945a-fbd9-4206-add6-791ba4814271");
        UNIT_ASSERT_VALUES_EQUAL(
            whereConditions.asString(*txn),
            "WHERE id = '9c4b945a-fbd9-4206-add6-791ba4814271'");
    }
    {
        WhereConditions whereConditions;
        updateWhereConditionsWithSearchParam(whereConditions, "230208859");
        UNIT_ASSERT_VALUES_EQUAL(
            whereConditions.asString(*txn),
            "WHERE original_task->'metadata'->>'uid' = '230208859'");
    }
    {
        WhereConditions whereConditions;
        updateWhereConditionsWithSearchParam(
            whereConditions, "c5a6f54d7f2e621b68bf6d12fcecc26e");
        UNIT_ASSERT_VALUES_EQUAL(
            whereConditions.asString(*txn),
            "WHERE original_task->'metadata'->>'uuid' = 'c5a6f54d7f2e621b68bf6d12fcecc26e'");
    }
    {
        WhereConditions whereConditions;
        updateWhereConditionsWithSearchParam(
            whereConditions, "127.0.0.1");
        UNIT_ASSERT_VALUES_EQUAL(
            whereConditions.asString(*txn),
            "WHERE original_task->'metadata'->>'ip' = '127.0.0.1'");
    }
    {
        WhereConditions whereConditions;
        updateWhereConditionsWithSearchParam(
            whereConditions, "2a02:6b8:c04:1f1:0:663:7a9c:e95b");
        UNIT_ASSERT_VALUES_EQUAL(
            whereConditions.asString(*txn),
            "WHERE original_task->'metadata'->>'ip' = '2a02:6b8:c04:1f1:0:663:7a9c:e95b'");
    }
    {
        WhereConditions whereConditions;
        updateWhereConditionsWithSearchParam(
            whereConditions, "2a02:6b8:b080:8003::1:8");
        UNIT_ASSERT_VALUES_EQUAL(
            whereConditions.asString(*txn),
            "WHERE original_task->'metadata'->>'ip' = '2a02:6b8:b080:8003::1:8'");
    }
    {
        WhereConditions whereConditions;
        updateWhereConditionsWithSearchParam(whereConditions, "test@test.test");
        UNIT_ASSERT_VALUES_EQUAL(
            whereConditions.asString(*txn),
            "WHERE original_task->>'user_email' ILIKE 'test@test.test'");
    }
    {
        WhereConditions whereConditions;
        updateWhereConditionsWithSearchParam(whereConditions, "something");
        UNIT_ASSERT_VALUES_EQUAL(
            whereConditions.asString(*txn),
            "WHERE to_tsvector('simple', original_task->>'message') @@ "
            "plainto_tsquery('something')");
    }
}

} // test_search_param suite

} // namespace maps::wiki::feedback::api::tests
