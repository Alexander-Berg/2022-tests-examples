#include <maps/wikimap/feedback/userapi/src/yacare/lib/takeout.h>

#include <maps/wikimap/feedback/api/src/libs/gdpr/types.h>
#include <maps/wikimap/feedback/api/src/libs/gdpr/takeout.h>
#include <maps/wikimap/feedback/api/src/libs/dbqueries/constants.h>
#include <maps/wikimap/feedback/api/src/libs/test_helpers/db_fixture.h>

#include <maps/wikimap/mapspro/libs/query_builder/include/select_query.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

template <>
void Out<maps::wiki::feedback::api::gdpr::Takeout>(
    IOutputStream& os,
    const maps::wiki::feedback::api::gdpr::Takeout& request)
{
    std::ostringstream ostr;
    ostr << request;
    os << ostr.str();
}

namespace maps::wiki::feedback::userapi::tests {

namespace {

namespace tests = api::tests;

const Uid UID{42};
const gdpr::RequestId REQUEST_ID{"some_request_id"};

const auto TIME_POINT = maps::chrono::parseIsoDateTime("2021-01-01 12:00:00+00:00");
const TakeoutStatus TAKEOUT_STATUS_EMPTY{TakeoutState::Empty, TIME_POINT};
const TakeoutStatus TAKEOUT_STATUS_READY_TO_DELETE{TakeoutState::ReadyToDelete, TIME_POINT};
const TakeoutStatus TAKEOUT_STATUS_DELETE_IN_PROGRESS{TakeoutState::DeleteInProgress, TIME_POINT};

gdpr::Takeout NEW_REQUEST{
    gdpr::TakeoutId(0),
    UID,
    REQUEST_ID,
    TIME_POINT,
    std::nullopt
};

auto& dbPool()
{
    static tests::DbFixture db;
    return db.pool();
}

auto loadRequests(pqxx::transaction_base& txn)
{
    return query_builder::SelectQuery(
        dbqueries::tables::GDPR_TAKEOUT,
        query_builder::WhereConditions()).exec(txn);
}

void checkGdprRequest(const gdpr::Takeout& left, const gdpr::Takeout& right) {
    UNIT_ASSERT_VALUES_EQUAL(left.uid, right.uid);
    UNIT_ASSERT_VALUES_EQUAL(left.requestId, right.requestId);
    UNIT_ASSERT_VALUES_EQUAL(bool(left.completedAt), bool(right.completedAt));
}

} // namespace

Y_UNIT_TEST_SUITE(test_takeout)
{

Y_UNIT_TEST(create_takeout_delete_request)
{
    auto txn = dbPool().masterWriteableTransaction();

    UNIT_ASSERT(createTakeoutDeleteRequest(*txn, UID, REQUEST_ID, TAKEOUT_STATUS_EMPTY));
    auto requestsForEmpty = loadRequests(*txn);
    UNIT_ASSERT_VALUES_EQUAL(requestsForEmpty.size(), 0);

    UNIT_ASSERT(!createTakeoutDeleteRequest(*txn, UID, REQUEST_ID, TAKEOUT_STATUS_DELETE_IN_PROGRESS));
    auto requestsForDeleteInProgress = loadRequests(*txn);
    UNIT_ASSERT_VALUES_EQUAL(requestsForDeleteInProgress.size(), 0);


    UNIT_ASSERT(createTakeoutDeleteRequest(*txn, UID, REQUEST_ID, TAKEOUT_STATUS_READY_TO_DELETE));
    auto requestsForReadyToDelete = loadRequests(*txn);
    UNIT_ASSERT_VALUES_EQUAL(requestsForReadyToDelete.size(), 1);

    auto request = gdpr::Takeout(requestsForReadyToDelete[0]);
    checkGdprRequest(request, NEW_REQUEST);
}

}

} // namespace maps::wiki::feedback::userapi::tests
