#include <maps/wikimap/ugc/backoffice/src/lib/assignments/load.h>
#include <maps/wikimap/ugc/backoffice/src/lib/assignments/modify.h>
#include <maps/wikimap/ugc/libs/common/dbqueries.h>
#include <maps/wikimap/ugc/libs/test_helpers/test_dbpools.h>
#include <maps/wikimap/ugc/backoffice/src/tests/helpers/test_request_validator.h>
#include <maps/infra/yacare/include/error.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::ugc::backoffice::tests {

namespace {

using namespace std::chrono_literals;

const Uid UID1{111111};
const Uid UID2{222222};
const maps::auth::TvmId FAKETVM = 1;

const proto::backoffice::Task task()
{
    proto::backoffice::Task task;
    *task.add_uid() = std::to_string(UID1.value());
    *task.add_uid() = std::to_string(UID2.value());

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

Y_UNIT_TEST_SUITE(test_load_assignment)
{

Y_UNIT_TEST(get_assignment_status)
{
    ugc::tests::TestDbPools dbPools;

    AssignmentId id{"id1"};
    createAssignments(dbPools, /*dryRun*/ false, id, task(), FAKETVM, 3600s, TestRequestValidator());

    auto txn = dbPools.pool().masterWriteableTransaction();
    const auto status = getAssignmentStatus(*txn, UID1, id);

    UNIT_ASSERT_VALUES_EQUAL(
        static_cast<int>(status.status()),
        static_cast<int>(proto::assignment::ACTIVE)
    );

    updateAssignmentStatus(
        *txn,
        UID1,
        AssignmentStatus::Skipped,
        id,
        maps::chrono::TimePoint::clock::now()
    );

    const auto updatedStatus = getAssignmentStatus(*txn, UID1, id);

    UNIT_ASSERT_VALUES_EQUAL(
        static_cast<int>(updatedStatus.status()),
        static_cast<int>(proto::assignment::SKIPPED)
    );

    UNIT_ASSERT_EXCEPTION_CONTAINS(
        getAssignmentStatus(*txn, UID2, AssignmentId{"id:12321"}),
        yacare::errors::NotFound,
        "Not found assignment for uid=222222 and task_id=id:12321"
    );
}

} // test_load_assignment suite

} // namespace maps::wiki::ugc::backoffice::tests
