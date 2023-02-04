#include <maps/wikimap/ugc/account/src/lib/gdpr/takeout.h>

#include <maps/wikimap/ugc/libs/common/constants.h>
#include <maps/wikimap/ugc/libs/test_helpers/printers.h>
#include <maps/wikimap/ugc/libs/test_helpers/takeout.h>
#include <maps/wikimap/ugc/libs/test_helpers/test_dbpools.h>

#include <maps/wikimap/mapspro/libs/query_builder/include/select_query.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::ugc::gdpr::tests {

namespace {

const Uid UID{42};
const gdpr::RequestId REQUEST_ID{"some_request_id"};

} // namespace

Y_UNIT_TEST_SUITE(test_takeout)
{

Y_UNIT_TEST(create_takeout_delete_request)
{
    ugc::tests::TestDbPools dbPools;
    auto txn = dbPools.pool().masterWriteableTransaction();

    UNIT_ASSERT(createTakeoutDeleteRequest(*txn, UID, REQUEST_ID, TakeoutState::Empty));
    auto takeoutsForEmpty = ugc::tests::loadTakeouts(*txn);
    UNIT_ASSERT_VALUES_EQUAL(takeoutsForEmpty.size(), 0);

    UNIT_ASSERT(!createTakeoutDeleteRequest(*txn, UID, REQUEST_ID, TakeoutState::DeleteInProgress));
    auto takeoutsForDeleteInProgress = ugc::tests::loadTakeouts(*txn);
    UNIT_ASSERT_VALUES_EQUAL(takeoutsForDeleteInProgress.size(), 0);


    UNIT_ASSERT(createTakeoutDeleteRequest(*txn, UID, REQUEST_ID, TakeoutState::ReadyToDelete));
    auto takeoutsForReadyToDelete = ugc::tests::loadTakeouts(*txn);
    UNIT_ASSERT_VALUES_EQUAL(takeoutsForReadyToDelete.size(), 1);

    const auto& takeout = takeoutsForReadyToDelete[0];
    UNIT_ASSERT_VALUES_EQUAL(takeout.uid.value(), UID.value());
    UNIT_ASSERT_VALUES_EQUAL(takeout.requestId.value(), REQUEST_ID.value());
    UNIT_ASSERT(!takeout.completedAt);
}

}

} // namespace maps::wiki::ugc::gdpr::tests
