#include <maps/wikimap/mapspro/services/tasks-ng/lib/query.h>

#include <library/cpp/testing/unittest/registar.h>

#include <set>

namespace maps {
namespace wiki {
namespace tasks_ng {
namespace tests {

namespace {

const auto TEST_UID = 100500;
const auto TEST_ID = 123;
const auto TABLE = "task";
const auto TASK_TYPE = "'export'";

} // namespace

Y_UNIT_TEST_SUITE(query) {

Y_UNIT_TEST(test_empty)
{
    UNIT_ASSERT_STRINGS_EQUAL(Query().str(), "");
}

Y_UNIT_TEST(test_count)
{
    auto commonQuery = Query()
        .from(TABLE)
        .where() << "type=" << TASK_TYPE;

    auto EXPECTED1 =
        "SELECT COUNT(1) FROM task WHERE type='export'";
    auto query1 = Query().count() << commonQuery;
    UNIT_ASSERT_STRINGS_EQUAL(query1.str(), EXPECTED1);

    auto EXPECTED_ID =
        "SELECT COUNT(id) FROM task WHERE type='export'";
    auto query = Query().count("id") << commonQuery;
    UNIT_ASSERT_STRINGS_EQUAL(query.str(), EXPECTED_ID);
}

Y_UNIT_TEST(test_select)
{
    auto queryPart = Query()
        .from(TABLE)
        .where() << "type=" << TASK_TYPE;

    auto query = Query()
        .select("*")
        .append(queryPart)
        .orderBy("id DESC")
        .offset(100)
        .limit(10);

    auto EXPECTED =
        "SELECT * FROM task WHERE type='export'"
        " ORDER BY id DESC  OFFSET 100  LIMIT 10";
    UNIT_ASSERT_STRINGS_EQUAL(query.str(), EXPECTED);
}

Y_UNIT_TEST(test_insert)
{
    auto query = Query()
        .insertInto(TABLE)
        .columns("type, created_by")
        .values(Query() << TASK_TYPE << ',' << TEST_UID)
        .returning("*");

    auto EXPECTED =
        "INSERT INTO task"
        " (type, created_by)"
        " VALUES ('export',100500)"
        " RETURNING *";
    UNIT_ASSERT_STRINGS_EQUAL(query.str(), EXPECTED);
}

Y_UNIT_TEST(test_update)
{
    auto query = Query()
        .update(TABLE)
        .set(Query() << "modified=NOW(), modified_by=" << TEST_UID)
        .where(Query() << "id=" << TEST_ID)
        .returning("*");

    auto EXPECTED =
        "UPDATE task"
        " SET modified=NOW(), modified_by=100500"
        " WHERE id=123"
        " RETURNING *";
    UNIT_ASSERT_STRINGS_EQUAL(query.str(), EXPECTED);
}

} // Y_UNIT_TEST_SUITE

} // namespace tests
} // namespace tasks_ng
} // namespace wiki
} // namespace maps
