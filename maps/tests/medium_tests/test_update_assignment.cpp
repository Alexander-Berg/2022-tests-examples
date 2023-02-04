#include <maps/wikimap/ugc/libs/common/constants.h>
#include <maps/wikimap/ugc/libs/common/dbqueries.h>
#include <maps/wikimap/ugc/libs/test_helpers/db_fixture.h>
#include <maps/wikimap/mapspro/libs/query_builder/include/insert_query.h>
#include <maps/wikimap/mapspro/libs/query_builder/include/select_query.h>
#include <maps/libs/chrono/include/time_point.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::ugc::backoffice::tests {

using namespace std::literals::chrono_literals;

namespace {

auto& dbPool()
{
    static ugc::tests::DbFixture db;
    return db.pool();
}

} // namespace

Y_UNIT_TEST_SUITE(update_assignment_status)
{

Y_UNIT_TEST(update_assignment_status)
{
    const Uid uid{111111};
    AssignmentId id1{"id1"};
    auto now = maps::chrono::TimePoint::clock::now();
    auto txn = dbPool().masterWriteableTransaction();
    maps::wiki::query_builder::InsertQuery insertQuery(tables::ASSIGNMENT);
    insertQuery
        .append(columns::UID, std::to_string(uid.value()))
        .appendQuoted(columns::TASK_ID, id1.value())
        .append(columns::METADATA_ID, "1")
        .appendQuoted(columns::STATUS, std::string{toString(AssignmentStatus::Active)});
    insertQuery.exec(*txn);


    const auto activeRows = maps::wiki::query_builder::SelectQuery(
        tables::ASSIGNMENT,
        maps::wiki::query_builder::WhereConditions()
            .append(columns::UID, std::to_string(uid.value()))
    ).exec(*txn);
    UNIT_ASSERT_VALUES_EQUAL(activeRows.size(), 1);
    UNIT_ASSERT_VALUES_EQUAL(
        activeRows[0][columns::STATUS].as<std::string>(),
        toString(AssignmentStatus::Active)
    );

    updateAssignmentStatus(*txn, uid, AssignmentStatus::Skipped, id1, now);

    const auto skippedRows = maps::wiki::query_builder::SelectQuery(
        tables::ASSIGNMENT,
        maps::wiki::query_builder::WhereConditions()
            .append(columns::UID, std::to_string(uid.value()))
    ).exec(*txn);
    UNIT_ASSERT_VALUES_EQUAL(skippedRows.size(), 1);
    UNIT_ASSERT_VALUES_EQUAL(
        skippedRows[0][columns::STATUS].as<std::string>(),
        toString(AssignmentStatus::Skipped)
    );
    UNIT_ASSERT_VALUES_EQUAL(
        maps::chrono::sinceEpoch<std::chrono::seconds>(
            maps::chrono::parseSqlDateTime(skippedRows[0][columns::UPDATED_AT].as<std::string>())),
        maps::chrono::sinceEpoch<std::chrono::seconds>(now)
    );

    updateAssignmentStatus(*txn, uid, AssignmentStatus::Active, id1, now + 10s);

    const auto newActiveRows = maps::wiki::query_builder::SelectQuery(
        tables::ASSIGNMENT,
        maps::wiki::query_builder::WhereConditions()
            .append(columns::UID, std::to_string(uid.value()))
    ).exec(*txn);
    UNIT_ASSERT_VALUES_EQUAL(newActiveRows.size(), 1);
    UNIT_ASSERT_VALUES_EQUAL(
        newActiveRows[0][columns::STATUS].as<std::string>(),
        toString(AssignmentStatus::Active)
    );
    UNIT_ASSERT_VALUES_EQUAL(
        maps::chrono::sinceEpoch<std::chrono::seconds>(
            maps::chrono::parseSqlDateTime(newActiveRows[0][columns::UPDATED_AT].as<std::string>())),
        maps::chrono::sinceEpoch<std::chrono::seconds>(now + 10s)
    );
}

} // test_modify_assignment suite

} // namespace maps::wiki::ugc::backoffice::tests
