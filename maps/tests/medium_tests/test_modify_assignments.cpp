#include <maps/wikimap/ugc/libs/common/constants.h>
#include <maps/wikimap/ugc/libs/common/helpers.h>
#include <maps/wikimap/ugc/libs/test_helpers/test_dbpools.h>
#include <maps/wikimap/ugc/backoffice/src/lib/assignments/modify.h>
#include <maps/wikimap/ugc/backoffice/src/tests/helpers/test_request_validator.h>
#include <maps/wikimap/mapspro/libs/query_builder/include/select_query.h>

#include <maps/doc/proto/converters/geolib/include/yandex/maps/geolib3/proto.h>
#include <maps/libs/chrono/include/time_point.h>
#include <maps/libs/geolib/include/distance.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::ugc::backoffice::tests {

namespace {

using namespace std::chrono_literals;

const Uid UID1{111111};
const Uid UID2{222222};
const geolib3::Point2 POSITION{37.37, 55.55};
const maps::auth::TvmId FAKETVM = 1;

const proto::backoffice::Task task()
{
    proto::backoffice::Task task;
    *task.add_uid() = std::to_string(UID1.value());
    *task.add_uid() = std::to_string(UID2.value());
    *task.mutable_point() = geolib3::proto::encode(POSITION);

    auto& langToMetadata = *task.mutable_lang_to_metadata();
    {
        proto::assignment::AssignmentMetadata metadata;
        auto* addressAdd = metadata.mutable_address_add_assignment();
        *addressAdd->mutable_uri() = "ymapsbm://geo?ll=37.37,55.55&z=17";
        langToMetadata["ru_RU"] = metadata;
    }
    {
        proto::assignment::AssignmentMetadata metadata;
        auto* addressAdd = metadata.mutable_address_add_assignment();
        *addressAdd->mutable_uri() = "ymapsbm://geo?ll=37.37,55.55&z=17&lang=en_US";
        langToMetadata["en_US"] = metadata;
    }
    return task;
}

} // namespace

Y_UNIT_TEST_SUITE(test_modify_assignment)
{

Y_UNIT_TEST(create_assignment)
{
    ugc::tests::TestDbPools dbPools;

    AssignmentId id1{"id1"};
    createAssignments(dbPools, /*dryRun*/ false, id1, task(), FAKETVM, 3600s, TestRequestValidator());

    auto txn = dbPools.pool().slaveTransaction();
    const auto rows = maps::wiki::query_builder::SelectQuery(
        tables::ASSIGNMENT,
        {"*", columns::GET_X_COORD, columns::GET_Y_COORD}
    ).exec(*txn);
    UNIT_ASSERT_VALUES_EQUAL(rows.size(), 2);

    const auto& row = rows[0];
    UNIT_ASSERT_VALUES_EQUAL(
        row[columns::METADATA_ID].as<MetadataId::ValueType>(),
        static_cast<unsigned>(proto::assignment::AssignmentMetadata::AssignmentCase::kAddressAddAssignment)
    );
    UNIT_ASSERT_VALUES_EQUAL(row[columns::TASK_ID].as<std::string>(), "id1");
    UNIT_ASSERT_VALUES_EQUAL(row[columns::UID].as<Uid::ValueType>(), UID1.value());
    UNIT_ASSERT(
         // check distance between original position and result in database less than 1mm
        geolib3::fastGeoDistance(*parsePoint(row), POSITION) < 0.001
    );

    auto dataRows = maps::wiki::query_builder::SelectQuery(tables::ASSIGNMENT_DATA).exec(*txn);
    UNIT_ASSERT_VALUES_EQUAL(dataRows.size(), 4);
    UNIT_ASSERT_VALUES_EQUAL(
        maps::wiki::query_builder::SelectQuery(
            tables::ASSIGNMENT_DATA,
            maps::wiki::query_builder::WhereConditions()
                .append(columns::UID, std::to_string(UID1.value()))
                .appendQuoted(columns::TASK_ID, "id1")
        ).exec(*txn).size(),
        2
    );
    proto::assignment::AssignmentMetadata restored;
    pqxx::binarystring blob(dataRows[0][columns::DATA]);
    UNIT_ASSERT(restored.ParseFromString(TString{blob.str()}));
    UNIT_ASSERT(
        std::string{restored.address_add_assignment().uri()}.starts_with("ymapsbm://geo?ll=37.37,55.55&z=17")
    );
}


Y_UNIT_TEST(delete_assignment)
{
    ugc::tests::TestDbPools dbPools;

    AssignmentId id1{"1d1"};
    createAssignments(dbPools, /*dryRun*/ false, id1, task(), FAKETVM, 3600s, TestRequestValidator());

    auto txn = dbPools.pool().masterWriteableTransaction();
    const auto rows = maps::wiki::query_builder::SelectQuery(
        tables::ASSIGNMENT,
        maps::wiki::query_builder::WhereConditions()
            .append(columns::UID, std::to_string(UID1.value()))
    ).exec(*txn);
    UNIT_ASSERT_VALUES_EQUAL(rows.size(), 1);
    UNIT_ASSERT_VALUES_EQUAL(
        rows[0][columns::STATUS].as<std::string>(),
        toString(AssignmentStatus::Active)
    );

    deleteAssignment(*txn, UID1, id1);

    const auto newRows = maps::wiki::query_builder::SelectQuery(
        tables::ASSIGNMENT,
        maps::wiki::query_builder::WhereConditions()
            .append(columns::UID, std::to_string(UID1.value()))
    ).exec(*txn);
    UNIT_ASSERT_VALUES_EQUAL(newRows.size(), 1);
    UNIT_ASSERT_VALUES_EQUAL(
        newRows[0][columns::STATUS].as<std::string>(),
        toString(AssignmentStatus::Expired)
    );
}

Y_UNIT_TEST(done_assignment)
{
    ugc::tests::TestDbPools dbPools;

    AssignmentId id1{"1d1"};
    createAssignments(dbPools, /*dryRun*/ false, id1, task(), FAKETVM, 3600s, TestRequestValidator());

    auto txn = dbPools.pool().masterWriteableTransaction();
    const auto rows = maps::wiki::query_builder::SelectQuery(
        tables::ASSIGNMENT,
        {"*", "extract (epoch from " + columns::TTL + ") as ttl_in_seconds"},
        maps::wiki::query_builder::WhereConditions()
            .append(columns::UID, std::to_string(UID1.value()))
    ).exec(*txn);
    UNIT_ASSERT_VALUES_EQUAL(rows.size(), 1);
    UNIT_ASSERT_VALUES_EQUAL(
        rows[0][columns::STATUS].as<std::string>(),
        toString(AssignmentStatus::Active)
    );

    UNIT_ASSERT_VALUES_EQUAL(
        rows[0]["ttl_in_seconds"].as<uint64_t>(),
        3600
    );

    doneAssignment(*txn, UID1, id1);

    const auto newRows = maps::wiki::query_builder::SelectQuery(
        tables::ASSIGNMENT,
        maps::wiki::query_builder::WhereConditions()
            .append(columns::UID, std::to_string(UID1.value()))
    ).exec(*txn);
    UNIT_ASSERT_VALUES_EQUAL(newRows.size(), 1);
    UNIT_ASSERT_VALUES_EQUAL(
        newRows[0][columns::STATUS].as<std::string>(),
        toString(AssignmentStatus::Done)
    );
}

} // test_modify_assignment suite

} // namespace maps::wiki::ugc::backoffice::tests
