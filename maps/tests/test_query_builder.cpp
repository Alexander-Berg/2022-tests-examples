#include <maps/wikimap/mapspro/libs/query_builder/include/compound_query.h>
#include <maps/wikimap/mapspro/libs/query_builder/include/count_query.h>
#include <maps/wikimap/mapspro/libs/query_builder/include/delete_query.h>
#include <maps/wikimap/mapspro/libs/query_builder/include/join.h>
#include <maps/wikimap/mapspro/libs/query_builder/include/insert_query.h>
#include <maps/wikimap/mapspro/libs/query_builder/include/select_query.h>
#include <maps/wikimap/mapspro/libs/query_builder/include/update_query.h>
#include <maps/libs/common/include/exception.h>
#include <maps/wikimap/mapspro/libs/unittest/include/yandex/maps/wiki/unittest/localdb.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::query_builder::tests {

namespace {

auto& dbPool()
{
    static maps::wiki::unittest::RandomDatabaseFixture db;
    return db.pool();
}

} // namespace

Y_UNIT_TEST_SUITE(test_query_builder)
{

using namespace query_builder;
using namespace query_builder::internal;

Y_UNIT_TEST(build_insert_query)
{
    auto txn = dbPool().slaveTransaction();
    WhereConditions where;
    where.appendQuoted("key3", "value3");
    auto query = buildInsertQuery(
        *txn,
        "test_table",
        {
            {"key1", {"value1", Quote::No}},
            {"key2", {"value2", Quote::Yes}}
        },
        OnConflict::fromColumns({"key1", "key2"}, UpdateOnConflict{}, where));
    auto expected =
        "INSERT INTO test_table (key1, key2) VALUES (value1, 'value2') "
        "ON CONFLICT (key1, key2) WHERE key3 = 'value3' DO NOTHING";
    UNIT_ASSERT_VALUES_EQUAL(query, expected);

    UNIT_ASSERT_EXCEPTION(
        buildInsertQuery(*txn, "test_table", {/*keyValues*/}, std::nullopt),
        maps::RuntimeError);
}

Y_UNIT_TEST(on_conflict_column)
{
    auto txn = dbPool().slaveTransaction();
    UpdateOnConflict update;
    update.appendQuoted("key2", "value2_new");
    auto query = buildInsertQuery(
        *txn,
        "test_table",
        {
            {"key1", {"value1", Quote::No}},
            {"key2", {"value2", Quote::Yes}}
        },
        OnConflict::fromConstraint("constraint", std::move(update)));
    auto expected =
        "INSERT INTO test_table (key1, key2) VALUES (value1, 'value2') "
        "ON CONFLICT ON CONSTRAINT constraint DO UPDATE  SET key2 = 'value2_new'";
    UNIT_ASSERT_VALUES_EQUAL(query, expected);

    UNIT_ASSERT_EXCEPTION(
        buildInsertQuery(*txn, "test_table", {/*keyValues*/}, std::nullopt),
        maps::RuntimeError);
}

Y_UNIT_TEST(build_update_query)
{
    auto txn = dbPool().slaveTransaction();
    auto WhereConditionsConditions = WhereConditions().append("key", "value", Relation::Less);

    auto queryOrdinary = buildUpdateQuery(
        *txn,
        "test_table",
        {
            {"key1", {"value1", Quote::No}},
            {"key2", {"value2", Quote::Yes}}
        },
        {/*json values*/},
        WhereConditionsConditions);
    auto expectedOrdinary =
        "UPDATE test_table SET key1 = value1, key2 = 'value2' "
        "WHERE key < value";
    UNIT_ASSERT_VALUES_EQUAL(queryOrdinary, expectedOrdinary);

    auto queryWithoutWhere = buildUpdateQuery(
        *txn,
        "test_table",
        {
            {"key1", {"value1", Quote::No}},
            {"key2", {"value2", Quote::Yes}}
        },
        {/*json values*/},
        {/*WhereConditions*/});
    auto expectedWithoutWhere =
        "UPDATE test_table SET key1 = value1, key2 = 'value2'";
    UNIT_ASSERT_VALUES_EQUAL(queryWithoutWhere, expectedWithoutWhere);

    UNIT_ASSERT_EXCEPTION(
        buildUpdateQuery(
            *txn, "test_table", {/*keyValues*/}, {/*json values*/}, {/*WhereConditions*/}),
        maps::RuntimeError);
}

Y_UNIT_TEST(append_no_duplicate_key)
{
    ModifyData data;
    UNIT_ASSERT_NO_EXCEPTION(data.append("key", "value1"));
    UNIT_ASSERT_EXCEPTION(data.append("key", "value2"), maps::RuntimeError);
}

Y_UNIT_TEST(where_conditions)
{
    auto txn = dbPool().slaveTransaction();
    WhereConditions conditions;

    UNIT_ASSERT_VALUES_EQUAL(conditions.asString(*txn), "");

    conditions.append("key1", "value1");
    UNIT_ASSERT_VALUES_EQUAL(
        conditions.asString(*txn),
        "WHERE key1 = value1");

    conditions.appendQuoted("key2", "value2", Relation::GreaterOrEqual);
    UNIT_ASSERT_VALUES_EQUAL(
        conditions.asString(*txn),
        "WHERE key1 = value1 AND key2 >= 'value2'");

    conditions.notNull("key3");
    UNIT_ASSERT_VALUES_EQUAL(
        conditions.asString(*txn),
        "WHERE key1 = value1 AND key2 >= 'value2' AND key3 IS NOT NULL");

    conditions.keyInValues("key4", {"one", "two", "three"});
    UNIT_ASSERT_VALUES_EQUAL(
        conditions.asString(*txn),
        "WHERE key1 = value1 AND key2 >= 'value2' "
        "AND key4 IN (one, three, two) "
        "AND key3 IS NOT NULL");

    conditions.keyInQuotedValues("key5", {"one", "two", "three"});
    UNIT_ASSERT_VALUES_EQUAL(
        conditions.asString(*txn),
        "WHERE key1 = value1 AND key2 >= 'value2' "
        "AND key4 IN (one, three, two) AND key5 IN ('one', 'three', 'two') "
        "AND key3 IS NOT NULL");

    conditions.textSearch("simple", "original_task->>'message'", "test");
    UNIT_ASSERT_VALUES_EQUAL(
        conditions.asString(*txn),
        "WHERE key1 = value1 AND key2 >= 'value2' "
        "AND key4 IN (one, three, two) AND key5 IN ('one', 'three', 'two') "
        "AND key3 IS NOT NULL AND "
        "to_tsvector('simple', original_task->>'message') @@ plainto_tsquery('test')");

    conditions.isNull("key5");
    UNIT_ASSERT_VALUES_EQUAL(
        conditions.asString(*txn),
        "WHERE key1 = value1 AND key2 >= 'value2' "
        "AND key4 IN (one, three, two) AND key5 IN ('one', 'three', 'two') "
        "AND key5 IS NULL "
        "AND key3 IS NOT NULL AND "
        "to_tsvector('simple', original_task->>'message') @@ plainto_tsquery('test')");

    UNIT_ASSERT_VALUES_EQUAL(
        WhereConditions().isNull("key3").asString(*txn),
        "WHERE key3 IS NULL");

    UNIT_ASSERT_VALUES_EQUAL(
        WhereConditions().notNull("key3").asString(*txn),
        "WHERE key3 IS NOT NULL");

    UNIT_ASSERT_VALUES_EQUAL(
        WhereConditions()
            .textSearch("simple", "original_task->>'message'", "test")
            .asString(*txn),
        "WHERE to_tsvector('simple', original_task->>'message') @@ plainto_tsquery('test')");

    UNIT_ASSERT_EXCEPTION(
        WhereConditions().keyInValues("key", {/*values*/}),
        maps::RuntimeError);
    UNIT_ASSERT_EXCEPTION(
        WhereConditions().keyInQuotedValues("key", {/*values*/}),
        maps::RuntimeError);
}

Y_UNIT_TEST(insert_query)
{
    auto txn = dbPool().slaveTransaction();

    auto query = InsertQuery("test_table")
        .append("key1", "value1")
        .append("key2", "value2")
        .appendQuoted("key3", "value3")
        .setReturning({"key1", "key2"})
        .asString(*txn);
    auto expected =
        "INSERT INTO test_table (key1, key2, key3) "
        "VALUES (value1, value2, 'value3') "
        "RETURNING key1, key2";
    UNIT_ASSERT_VALUES_EQUAL(query, expected);

    UNIT_ASSERT_EXCEPTION(InsertQuery("table").asString(*txn), maps::RuntimeError);
}

Y_UNIT_TEST(update_query)
{
    auto txn = dbPool().slaveTransaction();

    auto query = UpdateQuery(
        "test_table",
        WhereConditions()
            .append("time", "timepoint", Relation::Less))
        .append("key1", "value1")
        .appendQuoted("key2", "value2")
        .setReturning({"key1", "key2"})
        .asString(*txn);
    auto expected =
        "UPDATE test_table SET key1 = value1, key2 = 'value2' "
        "WHERE time < timepoint "
        "RETURNING key1, key2";
    UNIT_ASSERT_VALUES_EQUAL(query, expected);

    UNIT_ASSERT_EXCEPTION(UpdateQuery("table").asString(*txn), maps::RuntimeError);
}

Y_UNIT_TEST(select_query)
{
    auto txn = dbPool().slaveTransaction();

    auto query = SelectQuery(
        "test_table",
        {"col1", "col2"},
        WhereConditions()
            .append("time", "timepoint", Relation::GreaterOrEqual)
            .and_(SubConditions(AppendType::Or)
                .appendQuoted("col1", "1")
                .appendQuoted("col2", "2")
            ))
        .updateSeed();
    std::string expected =
        "SELECT SETSEED(-1.000000);\n"
        "SELECT col1, col2 FROM test_table WHERE time >= timepoint "
        "AND ( col1 = '1' OR col2 = '2' ) ";
    UNIT_ASSERT_VALUES_EQUAL(query.asString(*txn), expected);

    query.orderBy("col1 ASC");
    expected += " ORDER BY col1 ASC";
    UNIT_ASSERT_VALUES_EQUAL(query.asString(*txn), expected);

    query.offset(5);
    expected += " OFFSET 5";
    UNIT_ASSERT_VALUES_EQUAL(query.asString(*txn), expected);

    query.limit(50);
    expected += " LIMIT 50";
    UNIT_ASSERT_VALUES_EQUAL(query.asString(*txn), expected);

    UNIT_ASSERT_EXCEPTION(
        SelectQuery("table", std::vector<std::string>{/*columns*/}).asString(*txn),
        maps::RuntimeError);
}

Y_UNIT_TEST(select_query_distinct_on)
{
    auto txn = dbPool().slaveTransaction();

    auto query = SelectQuery(
        "test_table",
        {"col1", "col2"},
        WhereConditions()
            .append("col1", "value1")
    ).distinctOn({"col1", "col2"});

    std::string expected = "SELECT DISTINCT ON (col1, col2) col1, col2 FROM test_table "
        "WHERE col1 = value1";
    UNIT_ASSERT_VALUES_EQUAL(query.asString(*txn), expected);
}

Y_UNIT_TEST(select_query_group_by)
{
    auto txn = dbPool().slaveTransaction();

    auto query = SelectQuery(
        "test_table",
        {"col1", "col2"},
        WhereConditions()
            .append("col1", "value1")
    ).groupBy({"col1", "col2"}).orderBy("col1");

    std::string expected = "SELECT col1, col2 FROM test_table "
        "WHERE col1 = value1 GROUP BY col1, col2 ORDER BY col1";
    UNIT_ASSERT_VALUES_EQUAL(query.asString(*txn), expected);
}

Y_UNIT_TEST(select_query_with_join)
{
    auto txn = dbPool().slaveTransaction();
    auto query = SelectQuery(
        JoinSequence("table1")
            .join({
                .joinType = JoinType::Inner,
                .table = "table2",
                .conditions = {{.column = "col2", .otherTable = "table1", .otherColumn = "col1"}}
            }),
        {"*"}
    );

    std::string expected =
        "SELECT * FROM table1 "
        "INNER JOIN table2 ON table2.col2 = table1.col1 ";

    UNIT_ASSERT_VALUES_EQUAL(query.asString(*txn), expected);
}

Y_UNIT_TEST(select_query_with_join_multiple_columns)
{
    auto txn = dbPool().slaveTransaction();
    auto query = SelectQuery(
        JoinSequence("table1")
            .join({
                .joinType = JoinType::Inner,
                .table = "table2",
                .conditions = {
                    {.column = "col2", .otherTable = "table1", .otherColumn = "col1"},
                    {.column = "col4", .otherTable = "table1", .otherColumn = "col3"},
                    {.column = "col6", .otherTable = "table1", .otherColumn = "col1"},
                }
            }),
        {"*"}
    );

    std::string expected =
        "SELECT * FROM table1 "
        "INNER JOIN table2 ON table2.col2 = table1.col1 "
        "AND table2.col4 = table1.col3 "
        "AND table2.col6 = table1.col1 ";

    UNIT_ASSERT_VALUES_EQUAL(query.asString(*txn), expected);
}

Y_UNIT_TEST(select_query_with_join_multiple_joins)
{
    auto txn = dbPool().slaveTransaction();
    auto query = SelectQuery(
        JoinSequence("table1")
            .join({
                .joinType = JoinType::Inner,
                .table = "table2",
                .conditions = {{.column = "col2", .otherTable = "table1", .otherColumn = "col1"}}
            })
            .join({
                .joinType = JoinType::Left,
                .table = "table3",
                .conditions = {
                    {.column = "col3", .otherTable = "table2", .otherColumn = "col2"},
                    {.column = "col5", .otherTable = "table1", .otherColumn = "col1"},
                }
            })
            .join({
                .joinType = JoinType::Right,
                .table = "table4",
                .conditions = {{.column = "col4", .otherTable = "table1", .otherColumn = "col1"}}
            }),
        {"*"}
    );

    std::string expected =
        "SELECT * FROM table1 "
        "INNER JOIN table2 ON table2.col2 = table1.col1 "
        "LEFT JOIN table3 ON table3.col3 = table2.col2 AND table3.col5 = table1.col1 "
        "RIGHT JOIN table4 ON table4.col4 = table1.col1 ";

    UNIT_ASSERT_VALUES_EQUAL(query.asString(*txn), expected);
}

Y_UNIT_TEST(delete_query)
{
    auto txn = dbPool().slaveTransaction();

    auto query = DeleteQuery(
        "test_table",
        WhereConditions()
            .append("time", "timepoint", Relation::GreaterOrEqual));
    std::string expected =
        "DELETE FROM test_table WHERE time >= timepoint";
    UNIT_ASSERT_VALUES_EQUAL(query.asString(*txn), expected);

    UNIT_ASSERT_EXCEPTION(
        DeleteQuery("table", {/*WhereConditions*/}).asString(*txn),
        maps::RuntimeError);
}

Y_UNIT_TEST(count_query)
{
    auto txn = dbPool().slaveTransaction();

    auto query = CountQuery(
        "test_table",
        WhereConditions()
            .append("time", "timepoint", Relation::LessOrEqual),
        "column");
    std::string expected =
        "SELECT COUNT (column) FROM test_table WHERE time <= timepoint";
    UNIT_ASSERT_VALUES_EQUAL(query.asString(*txn), expected);
}

Y_UNIT_TEST(compound_query)
{
    auto txn = dbPool().slaveTransaction();

    auto query = CompoundQuery()
        .append(
            DeleteQuery(
                "test_table",
                WhereConditions()
                    .append("time", "timepoint", Relation::GreaterOrEqual)))
        .append(
            CountQuery(
                "test_table",
                WhereConditions()
                    .append("time", "timepoint", Relation::LessOrEqual),
                "column"));

    UNIT_ASSERT_VALUES_EQUAL(query.empty(), false);
    UNIT_ASSERT_VALUES_EQUAL(query.size(), 2);

    std::string expected =
        "DELETE FROM test_table WHERE time >= timepoint;"
        "SELECT COUNT (column) FROM test_table WHERE time <= timepoint";
    UNIT_ASSERT_VALUES_EQUAL(query.asString(*txn), expected);

    auto query2 = CompoundQuery()
        .append(query);
    UNIT_ASSERT_VALUES_EQUAL(query2.asString(*txn), expected);

    UNIT_ASSERT_VALUES_EQUAL(query2.empty(), false);
    UNIT_ASSERT_VALUES_EQUAL(query2.size(), 1);

    CompoundQuery emptyQuery;
    UNIT_ASSERT_VALUES_EQUAL(emptyQuery.empty(), true);
    UNIT_ASSERT_VALUES_EQUAL(emptyQuery.size(), 0);

    // skip all empty queries
    query2.append(emptyQuery);
    query2.append(std::optional<CompoundQuery>());
    query2.append(std::optional<CountQuery>());
    query2.append(std::optional<SelectQuery>());

    UNIT_ASSERT_VALUES_EQUAL(query2.empty(), false);
    UNIT_ASSERT_VALUES_EQUAL(query2.size(), 1);

    UNIT_ASSERT_VALUES_EQUAL(query2.asString(*txn), expected);

    UNIT_ASSERT_VALUES_EQUAL(emptyQuery.asString(*txn), "");
    UNIT_ASSERT_NO_EXCEPTION(emptyQuery.execNotEmpty(*txn));
}

} // test_query_builder suite

} // namespace maps::wiki::query_builder::tests
