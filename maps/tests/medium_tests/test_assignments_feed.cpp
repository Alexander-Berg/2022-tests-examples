#include <maps/wikimap/ugc/account/src/tests/medium_tests/helpers.h>

#include <maps/wikimap/ugc/account/src/lib/assignments.h>
#include <maps/wikimap/ugc/libs/common/constants.h>
#include <maps/wikimap/ugc/libs/test_helpers/test_dbpools.h>

#include <maps/wikimap/mapspro/libs/query_builder/include/select_query.h>
#include <maps/libs/geolib/include/distance.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::ugc::account::tests {

namespace qb = maps::wiki::query_builder;


const geolib3::BoundingBox INCLUDING_BBOX{
    geolib3::Point2{37.37, 55.55},
    /*width=*/0.1,
    /*height=*/0.1
};
const geolib3::BoundingBox NOT_INCLUDING_BBOX{
    geolib3::Point2{27.27, 54.54},
    /*width=*/0.1,
    /*height=*/0.1
};

Y_UNIT_TEST_SUITE(test_assignments_feed)
{

Y_UNIT_TEST(test_load_assignments)
{
    ugc::tests::TestDbPools dbPools;
    insertAssignments(dbPools);

    auto txn = dbPools.pool().slaveTransaction();

    auto doneRows = loadAssignments(*txn, UID1, AssignmentStatus::Done, {MetadataId{1}, MetadataId{2}});
    UNIT_ASSERT_VALUES_EQUAL(doneRows.size(), 0);

    auto rows = loadAssignments(*txn, UID1, AssignmentStatus::Active, {MetadataId{1}});
    UNIT_ASSERT_VALUES_EQUAL(rows.size(), 1);
    UNIT_ASSERT_VALUES_EQUAL(rows[0][columns::TASK_ID].as<std::string>(), "id1");
    UNIT_ASSERT_VALUES_EQUAL(rows[0][columns::STATUS].as<std::string>(), "active");
    UNIT_ASSERT_VALUES_EQUAL(rows[0][columns::UID].as<std::uint64_t>(), UID1.value());
}

Y_UNIT_TEST(test_load_assignments_in_bbox)
{
    ugc::tests::TestDbPools dbPools;
    insertAssignments(dbPools);

    auto txn = dbPools.pool().slaveTransaction();

    auto rows = loadAssignments(
        *txn,
        UID1,
        AssignmentStatus::Active,
        {MetadataId{1}},
        INCLUDING_BBOX,
        /*limit=*/ 10
    );
    UNIT_ASSERT_VALUES_EQUAL(rows.size(), 1);
    UNIT_ASSERT_VALUES_EQUAL(rows[0][columns::TASK_ID].as<std::string>(), "id1");
    UNIT_ASSERT_VALUES_EQUAL(rows[0][columns::STATUS].as<std::string>(), "active");
    UNIT_ASSERT_VALUES_EQUAL(rows[0][columns::UID].as<std::uint64_t>(), UID1.value());

    auto notIncludingRows = loadAssignments(
        *txn,
        UID1,
        AssignmentStatus::Active,
        {MetadataId{1}},
        NOT_INCLUDING_BBOX,
        /*limit=*/ 10
    );
    UNIT_ASSERT_VALUES_EQUAL(notIncludingRows.size(), 0);
}

Y_UNIT_TEST(test_make_assignments)
{
    ugc::tests::TestDbPools dbPools;
    insertAssignments(dbPools);

    auto txn = dbPools.pool().slaveTransaction();

    auto rows = loadAssignments(*txn, UID1, AssignmentStatus::Active, {MetadataId{1}, MetadataId{2}});
    const auto assignments = makeAssignments(*txn, rows, AssignmentStatus::Active, UID1);
    UNIT_ASSERT_VALUES_EQUAL(assignments.size(), 2);
    const auto& assignment1 = assignments[0];
    UNIT_ASSERT_VALUES_EQUAL(assignment1.id.value(), "id1");
    UNIT_ASSERT_VALUES_EQUAL(assignment1.uid.value(), UID1.value());
    UNIT_ASSERT_VALUES_EQUAL(assignment1.langToMetadata.size(), 2);
    UNIT_ASSERT(assignment1.position);
    UNIT_ASSERT(
        // check distance between original position and result in database less than 1mm
        geolib3::fastGeoDistance(*assignment1.position, POSITION) < 0.001
    );
    const auto& assignment2 = assignments[1];
    UNIT_ASSERT_VALUES_EQUAL(assignment2.id.value(), "id2");
    UNIT_ASSERT_VALUES_EQUAL(assignment2.uid.value(), UID1.value());
    UNIT_ASSERT_VALUES_EQUAL(assignment2.langToMetadata.size(), 1);
    UNIT_ASSERT_VALUES_EQUAL(assignment2.langToMetadata.begin()->first, "ru_RU");
    UNIT_ASSERT(assignment2.langToMetadata.begin()->second.has_organization_edit_status_assignment());
    UNIT_ASSERT(!assignment2.position);
}

Y_UNIT_TEST(test_find_assignments)
{
    ugc::tests::TestDbPools dbPools;
    insertAssignments(dbPools);

    auto txn = dbPools.pool().slaveTransaction();

    const auto assignments = findAssignments(
        *txn,
        UID1,
        AssignmentStatus::Active,
        {MetadataId{1}, MetadataId{2}},
        Paging{/*before*/ 5, /*after*/ 5, std::nullopt},
        Lang{"ru_RU"}.locale()
    );
    UNIT_ASSERT_VALUES_EQUAL(assignments.assignment_size(), 2);
    auto assignment1 = assignments.assignment(0);
    UNIT_ASSERT_VALUES_EQUAL(assignment1.task_id(), "id2");
    UNIT_ASSERT(assignment1.metadata().has_organization_edit_status_assignment());
    auto assignment2 = assignments.assignment(1);
    UNIT_ASSERT_VALUES_EQUAL(assignment2.task_id(), "id1");
    UNIT_ASSERT(assignment2.status() == proto::assignment::ACTIVE);
}

Y_UNIT_TEST(test_find_assignments_in_bbox)
{
    ugc::tests::TestDbPools dbPools;
    insertAssignments(dbPools);

    auto txn = dbPools.pool().slaveTransaction();

    const auto assignments = findAssignments(
        *txn,
        UID1,
        AssignmentStatus::Active,
        {MetadataId{1}, MetadataId{2}},
        INCLUDING_BBOX,
        /*limit=*/ 10,
        Lang{"ru_RU"}.locale()
    );
    UNIT_ASSERT_VALUES_EQUAL(assignments.assignment_size(), 1);
    auto assignment1 = assignments.assignment(0);
    UNIT_ASSERT_VALUES_EQUAL(assignment1.task_id(), "id1");

    const auto emptyAssignments = findAssignments(
        *txn,
        UID1,
        AssignmentStatus::Active,
        {MetadataId{1}, MetadataId{2}},
        NOT_INCLUDING_BBOX,
        /*limit=*/ 10,
        Lang{"ru_RU"}.locale()
    );
    UNIT_ASSERT_VALUES_EQUAL(emptyAssignments.assignment_size(), 0);
}

} // test_feed suite

Y_UNIT_TEST_SUITE(test_skip_assignment)
{

Y_UNIT_TEST(skip_assignment)
{
    ugc::tests::TestDbPools dbPools;
    insertAssignments(dbPools);

    auto txn = dbPools.pool().masterWriteableTransaction();

    UNIT_ASSERT_VALUES_EQUAL(
        loadAssignments(*txn, UID1, AssignmentStatus::Active, {MetadataId{1}, MetadataId{2}}).size(),
        2
    );
    skipAssignment(*txn, UID1, AssignmentId{"id1"});
    UNIT_ASSERT_VALUES_EQUAL(
        loadAssignments(*txn, UID1, AssignmentStatus::Active, {MetadataId{1}, MetadataId{2}}).size(),
        1
    );
    auto rows = loadAssignments(*txn, UID1, AssignmentStatus::Skipped, {MetadataId{1}, MetadataId{2}});
    UNIT_ASSERT_VALUES_EQUAL(rows.size(), 1);
    UNIT_ASSERT_VALUES_EQUAL(rows[0][columns::TASK_ID].as<std::string>(), "id1");
}

}

} // namespace maps::wiki::ugc::account::tests
