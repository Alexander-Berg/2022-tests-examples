#include <maps/wikimap/ugc/backoffice/src/lib/links.h>
#include <maps/wikimap/ugc/libs/common/constants.h>
#include <maps/wikimap/ugc/libs/test_helpers/db_fixture.h>
#include <maps/infra/yacare/include/error.h>
#include <maps/wikimap/mapspro/libs/query_builder/include/count_query.h>
#include <maps/wikimap/mapspro/libs/query_builder/include/insert_query.h>
#include <maps/wikimap/mapspro/libs/query_builder/include/select_query.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::ugc::backoffice::tests {

namespace {

auto& dbPool()
{
    static ugc::tests::DbFixture db;
    return db.pool();
}

} // namespace

Y_UNIT_TEST_SUITE(test_links)
{

Y_UNIT_TEST(add_link)
{
    Uid uid{11111};
    AssignmentId taskId{"task:1"};
    ContributionId contributionId{"contributionId:1"};
    {
        auto txn = dbPool().masterWriteableTransaction();

        // Cannot add link between unknown objects
        UNIT_ASSERT_EXCEPTION_CONTAINS(
            addLink(*txn, uid, taskId, contributionId),
            yacare::errors::NotFound,
            "Cannot add link: unknown task_id 'task:1' or contribution_id "
            "'contributionId:1' for uid '11111'"
        );
    }

    auto txn = dbPool().masterWriteableTransaction();
    // Insert contribution and assignment before adding link
    maps::wiki::query_builder::InsertQuery(tables::CONTRIBUTION)
        .append(columns::UID, std::to_string(uid.value()))
        .appendQuoted(columns::CONTRIBUTION_ID, contributionId.value())
        .append(columns::METADATA_ID, "1")
    .exec(*txn);
    maps::wiki::query_builder::InsertQuery(tables::ASSIGNMENT)
        .append(columns::UID, std::to_string(uid.value()))
        .appendQuoted(columns::TASK_ID, taskId.value())
        .appendQuoted(columns::STATUS, std::string{toString(AssignmentStatus::Active)})
        .append(columns::METADATA_ID, "3")
    .exec(*txn);
    UNIT_ASSERT_VALUES_EQUAL(
        maps::wiki::query_builder::CountQuery(tables::LINKS).exec(*txn),
        0
    );
    UNIT_ASSERT_NO_EXCEPTION(addLink(*txn, uid, taskId, contributionId));
    // test the same query
    UNIT_ASSERT_NO_EXCEPTION(addLink(*txn, uid, taskId, contributionId));
    const auto rows = maps::wiki::query_builder::SelectQuery(tables::LINKS).exec(*txn);
    UNIT_ASSERT_VALUES_EQUAL(rows.size(), 1);
    UNIT_ASSERT_VALUES_EQUAL(rows[0][columns::TASK_ID].as<std::string>(), taskId.value());
    UNIT_ASSERT_VALUES_EQUAL(rows[0][columns::CONTRIBUTION_ID].as<std::string>(), contributionId.value());
    UNIT_ASSERT_VALUES_EQUAL(rows[0][columns::UID].as<uint64_t>(), uid.value());
}

Y_UNIT_TEST(delete_link)
{
    auto txn = dbPool().masterWriteableTransaction();

    Uid uid{11111};
    AssignmentId taskId{"task:1"};
    ContributionId contributionId{"contributionId:1"};

    // delete on empty table
    UNIT_ASSERT_NO_EXCEPTION(deleteLink(*txn, uid, taskId, contributionId));

    // insert link
    maps::wiki::query_builder::InsertQuery(tables::CONTRIBUTION)
        .append(columns::UID, std::to_string(uid.value()))
        .appendQuoted(columns::CONTRIBUTION_ID, contributionId.value())
        .append(columns::METADATA_ID, "1")
    .exec(*txn);
    maps::wiki::query_builder::InsertQuery(tables::ASSIGNMENT)
        .append(columns::UID, std::to_string(uid.value()))
        .appendQuoted(columns::TASK_ID, taskId.value())
        .appendQuoted(columns::STATUS, std::string{toString(AssignmentStatus::Active)})
        .append(columns::METADATA_ID, "3")
    .exec(*txn);
    addLink(*txn, uid, taskId, contributionId);
    UNIT_ASSERT_VALUES_EQUAL(
        maps::wiki::query_builder::CountQuery(tables::LINKS).exec(*txn),
        1
    );

    // delete inserted link
    UNIT_ASSERT_NO_EXCEPTION(deleteLink(*txn, uid, taskId, contributionId));

    // check that table is empty
    UNIT_ASSERT_VALUES_EQUAL(
        maps::wiki::query_builder::CountQuery(tables::LINKS).exec(*txn),
        0
    );
}


} // test_links suite

} // namespace maps::wiki::ugc::backoffice::tests
