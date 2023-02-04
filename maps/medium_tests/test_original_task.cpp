#include <maps/wikimap/feedback/api/src/libs/dbqueries/constants.h>
#include <maps/wikimap/feedback/api/src/libs/dbqueries/original_task.h>
#include <maps/wikimap/feedback/api/src/libs/test_helpers/db_fixture.h>

#include <maps/wikimap/mapspro/libs/query_builder/include/insert_query.h>
#include <maps/wikimap/mapspro/libs/query_builder/include/select_query.h>

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

Y_UNIT_TEST_SUITE(test_original_task)
{

Y_UNIT_TEST(load_original_task_field_values)
{
    auto txn = dbPool().masterWriteableTransaction();
    query_builder::InsertQuery("original_task.form_context_id")
        .appendQuoted(dbqueries::columns::VALUE, "map.context")
        .exec(*txn);

    query_builder::InsertQuery("original_task.form_context_id")
        .appendQuoted(dbqueries::columns::VALUE, "images")
        .exec(*txn);

    std::vector<std::string> expected{
        "images",
        "map.context",
    };
    UNIT_ASSERT_VALUES_EQUAL(
        loadOriginalTaskFieldValues(*txn, OriginalTaskField::FormContextId),
        expected);
}


} // test_original_task suite


} // namespace maps::wiki::feedback::api::dbqueries::tests
