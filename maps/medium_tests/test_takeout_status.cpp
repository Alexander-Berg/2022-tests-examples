#include <maps/wikimap/feedback/userapi/src/yacare/lib/takeout_status.h>

#include <maps/wikimap/feedback/api/src/libs/common/original_task.h>
#include <maps/wikimap/feedback/api/src/libs/common/feedback_task.h>
#include <maps/wikimap/feedback/api/src/libs/dbqueries/constants.h>
#include <maps/wikimap/feedback/api/src/libs/gdpr/types.h>
#include <maps/wikimap/feedback/api/src/libs/gdpr/takeout.h>
#include <maps/wikimap/feedback/api/src/libs/test_helpers/db_fixture.h>
#include <maps/wikimap/feedback/api/src/libs/test_helpers/common.h>

#include <maps/wikimap/mapspro/libs/query_builder/include/insert_query.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <maps/libs/json/include/value.h>

namespace maps::wiki::feedback::userapi::tests {

namespace {

using namespace maps::wiki::feedback::userapi;
using namespace maps::wiki::feedback::api;
namespace tests = maps::wiki::feedback::api::tests;

const Uid UID{42};
const gdpr::RequestId REQUEST_ID{"some_request_id"};

const auto TIME_POINT = maps::chrono::parseIsoDateTime("2021-01-01 12:00:00+00:00");
gdpr::Takeout NEW_REQUEST{
    gdpr::TakeoutId(0),
    UID,
    REQUEST_ID,
    TIME_POINT,
    std::nullopt
};

const OriginalTask ORIGINAL_TASK_SAMPLE{maps::json::Value::fromString(R"({
    "form_id": "organization",
    "message": "IDDQD_AEZAKMI",
    "form_point": {
        "lon": 37.37,
        "lat": 55.55
    },
    "metadata": {
        "uid": 42,
        "uuid": "c5216b6a-2379-4bab-a729-515831a42e72",
        "device_id": "ABC012",
        "yandexuid": "123456",
        "ip": "192.168.0.1",
        "locale": "ru_RU",
        "client_id": "mobile_maps_android",
        "version": "1.0"
    },
    "answer_id": "toponym",
    "object_id": "object_id",
    "question_id": "add_object",
    "user_full_name": "Test Test",
    "user_email": "email@test.ru"
})")};

const FeedbackTask FEEDBACK_TASK_SAMPLE{
    TaskId("fa5dd2f3-dd1b-7fb4-4425-21aaf5dbb885"),
    Service::Support,
    ServiceObjectId("serviceObjectId"),
    "http://serviceObjectUrl",
    TaskStatus::NeedInfo,
    ORIGINAL_TASK_SAMPLE,
    {/*integration*/},
    maps::chrono::parseIsoDateTime("2020-04-01 01:00:00+00:00"),
    maps::chrono::parseIsoDateTime("2020-04-02 01:00:00+00:00")
};

auto& dbPool()
{
    static tests::DbFixture db;
    return db.pool();
}

void insertGdprRequest(pqxx::transaction_base& txn, const gdpr::Takeout& request)
{
    query_builder::InsertQuery insertQuery(dbqueries::tables::GDPR_TAKEOUT);
    insertQuery
        .append(dbqueries::columns::UID, std::to_string(request.uid.value()))
        .appendQuoted(dbqueries::columns::REQUEST_ID, request.requestId.value())
        .appendQuoted(dbqueries::columns::REQUESTED_AT,
            maps::chrono::formatSqlDateTime(request.requestedAt));

    insertQuery.exec(txn);
}

} // namespace

Y_UNIT_TEST_SUITE(test_takeout_status)
{

Y_UNIT_TEST(get_takeout_status)
{
    auto txn = dbPool().masterWriteableTransaction();

    auto statusEmpty = getTakeoutStatus(*txn, UID);
    UNIT_ASSERT_VALUES_EQUAL(statusEmpty.state, TakeoutState::Empty);
    UNIT_ASSERT(!statusEmpty.updateDate);

    tests::insertTask(*txn, FEEDBACK_TASK_SAMPLE);
    auto statusReadyToDelete = getTakeoutStatus(*txn, UID);
    UNIT_ASSERT_VALUES_EQUAL(statusReadyToDelete.state, TakeoutState::ReadyToDelete);

    insertGdprRequest(*txn, NEW_REQUEST);
    auto statusDeleteInProgress = getTakeoutStatus(*txn, UID);
    UNIT_ASSERT_VALUES_EQUAL(statusDeleteInProgress.state, TakeoutState::DeleteInProgress);
    UNIT_ASSERT(*statusDeleteInProgress.updateDate == TIME_POINT);
}

}

} // namespace maps::wiki::feedback::userapi::tests
