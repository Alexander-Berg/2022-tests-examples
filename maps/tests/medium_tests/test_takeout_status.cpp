#include <maps/wikimap/ugc/account/src/tests/medium_tests/helpers.h>

#include <maps/wikimap/ugc/account/src/lib/gdpr/takeout_status.h>

#include <maps/wikimap/ugc/libs/common/constants.h>
#include <maps/wikimap/ugc/libs/common/takeout.h>
#include <maps/wikimap/ugc/libs/test_helpers/takeout.h>
#include <maps/wikimap/ugc/libs/test_helpers/test_dbpools.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <maps/libs/json/include/value.h>

namespace maps::wiki::ugc::gdpr::tests {

namespace {

const RequestId REQUEST_ID{"some_request_id"};

const auto TIME_POINT = maps::chrono::parseIsoDateTime("2021-01-01 12:00:00+00:00");

} // namespace

Y_UNIT_TEST_SUITE(test_takeout_status)
{

Y_UNIT_TEST(get_takeout_status)
{
    ugc::tests::TestDbPools dbPools;
    auto txn = dbPools.pool().masterWriteableTransaction();

    auto statusEmpty = getTakeoutStatus(*txn, account::tests::UID1);
    UNIT_ASSERT_VALUES_EQUAL(statusEmpty.state, TakeoutState::Empty);
    UNIT_ASSERT(!statusEmpty.updateDate);

    account::tests::insertAssignments(dbPools);

    auto statusReadyToDelete = getTakeoutStatus(*txn, account::tests::UID1);
    UNIT_ASSERT_VALUES_EQUAL(statusReadyToDelete.state, TakeoutState::ReadyToDelete);
    UNIT_ASSERT(!statusReadyToDelete.updateDate);

    auto takeoutId = ugc::tests::putTakeout(*txn, account::tests::UID1, REQUEST_ID);
    auto takeout = ugc::tests::getTakeout(*txn, takeoutId);

    auto statusDeleteInProgress = getTakeoutStatus(*txn, account::tests::UID1);
    UNIT_ASSERT_VALUES_EQUAL(statusDeleteInProgress.state, TakeoutState::DeleteInProgress);
    UNIT_ASSERT(statusDeleteInProgress.updateDate &&
        *statusDeleteInProgress.updateDate == takeout.requestedAt);
}

}

} // namespace maps::wiki::ugc::gdpr::tests
