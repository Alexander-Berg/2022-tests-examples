#include <maps/wikimap/feedback/api/src/samsara_importer/lib/importer.h>
#include <maps/wikimap/feedback/api/src/samsara_importer/lib/import_for_nmaps.h>
#include <maps/wikimap/feedback/api/src/samsara_importer/tests/medium_tests/common.h>

#include <maps/libs/chrono/include/time_point.h>
#include <maps/libs/http/include/test_utils.h>
#include <maps/libs/log8/include/log8.h>
#include <maps/wikimap/feedback/api/src/libs/common/config.h>
#include <maps/wikimap/feedback/api/src/libs/dbqueries/constants.h>
#include <maps/wikimap/feedback/api/src/libs/sync_queue/action_params.h>
#include <maps/wikimap/feedback/api/src/libs/sync_queue/types.h>
#include <maps/wikimap/feedback/api/src/libs/test_helpers/db_fixture.h>
#include <maps/wikimap/feedback/api/src/yacare/lib/dbqueries.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::feedback::api::samsara_importer::tests {

namespace th = maps::wiki::feedback::api::tests;
namespace sq = maps::wiki::feedback::api::sync_queue;

namespace {

pgpool3::Pool& dbPool()
{
    static api::tests::DbFixture db;
    return db.pool();
}

} // unnamed namespace

Y_UNIT_TEST_SUITE(test_import_for_nmaps)
{

Y_UNIT_TEST(import_nothing)
{
    auto mockHandle = http::addMock(
        "https://test-api.samsara.yandex-team.ru/api/v2/queues/queue/100482/tickets",
        [](const http::MockRequest& request) {
            UNIT_ASSERT_VALUES_EQUAL(request.url.params(),
                "statuses=HOLD&offset=0&limit=200");
            return http::MockResponse::fromFile(SRC_("data/queue_tickets_ids_empty.json"));
        });

    TestGlobals globals(dbPool());
    importUnstructuredFeedback(globals, samsara::QUEUE_CONTENT_2L, ImportForNmaps{globals});

    auto txn = dbPool().slaveTransaction();
    const auto taskIds = getAllTaskIds(*txn);
    UNIT_ASSERT_VALUES_EQUAL(taskIds.size(), 0);
}

Y_UNIT_TEST(import_some_for_nmaps)
{
    TestGlobals globals(dbPool());

    // error if no IN articles
    {
        auto mockHandleList = http::addMock(
            "https://test-api.samsara.yandex-team.ru/api/v2/queues/queue/100482/tickets",
            [](const http::MockRequest& request) {
                UNIT_ASSERT_VALUES_EQUAL(request.url.params(),
                    "statuses=HOLD&offset=0&limit=200");
                return http::MockResponse::fromFile(SRC_("data/queue_tickets_ids_1.json"));
            });
        auto mockHandle1 = http::addMock(
            "https://test-api.samsara.yandex-team.ru/api/v2/tickets/multi",
            [](const http::MockRequest& request) {
                UNIT_ASSERT_VALUES_EQUAL(request.url.params(), "ticketId=1000");
                return http::MockResponse::fromFile(SRC_("data/samsara_tickets_no_articles.json"));
            });
        auto stat = importUnstructuredFeedback(globals, samsara::QUEUE_CONTENT_2L, ImportForNmaps{globals});
        UNIT_ASSERT_VALUES_EQUAL(stat.imported, 0);
        UNIT_ASSERT_VALUES_EQUAL(stat.errorCount, 1);

        auto txn = dbPool().slaveTransaction();
        const auto taskIds = getAllTaskIds(*txn);
        UNIT_ASSERT_VALUES_EQUAL(taskIds.size(), 0);
    }

    // import simple one ticket
    {
        auto mockHandleList = http::addMock(
            "https://test-api.samsara.yandex-team.ru/api/v2/queues/queue/100482/tickets",
            [](const http::MockRequest& request) {
                UNIT_ASSERT_VALUES_EQUAL(request.url.params(),
                    "statuses=HOLD&offset=0&limit=200");
                return http::MockResponse::fromFile(SRC_("data/queue_tickets_ids_1.json"));
            });
        auto mockHandle1 = http::addMock(
            "https://test-api.samsara.yandex-team.ru/api/v2/tickets/multi",
            [](const http::MockRequest& request) {
                UNIT_ASSERT_VALUES_EQUAL(request.url.params(), "ticketId=1000");
                return http::MockResponse::fromFile(SRC_("data/samsara_tickets_1.json"));
            });
        importUnstructuredFeedback(globals, samsara::QUEUE_CONTENT_2L, ImportForNmaps{globals});

        auto txn = dbPool().slaveTransaction();
        const auto taskIds = getAllTaskIds(*txn);
        UNIT_ASSERT_VALUES_EQUAL(taskIds.size(), 1);

        auto task = loadMandatoryFeedbackTask(*txn, taskIds.front());
        auto taskExpected = FeedbackTask::fromJson(
            json::Value::fromFile(SRC_("data/fbapi_ticket_1000.json")));
        taskExpected.id = task.id;
        taskExpected.updatedAt = task.updatedAt;
        taskExpected.createdAt = task.createdAt;
        UNIT_ASSERT_EQUAL(task, taskExpected);
    }

    // don't import second time
    {
        auto mockHandleList = http::addMock(
            "https://test-api.samsara.yandex-team.ru/api/v2/queues/queue/100483/tickets",
            [](const http::MockRequest& request) {
                UNIT_ASSERT_VALUES_EQUAL(request.url.params(),
                    "statuses=HOLD&offset=0&limit=200");
                return http::MockResponse::fromFile(SRC_("data/queue_tickets_ids_2.json"));
            });
        auto mockHandle1 = http::addMock(
            "https://test-api.samsara.yandex-team.ru/api/v2/tickets/multi",
            [](const http::MockRequest& request) {
                UNIT_ASSERT_VALUES_EQUAL(request.url.params(), "ticketId=1001&ticketId=999");
                return http::MockResponse::fromFile(SRC_("data/samsara_tickets_2.json"));
            });
        auto stat = importUnstructuredFeedback(globals, samsara::QUEUE_ROUTES_2L, ImportForNmaps{globals});
        UNIT_ASSERT_VALUES_EQUAL(stat.imported, 1);
        UNIT_ASSERT_VALUES_EQUAL(stat.errorCount, 1);

        auto txn = dbPool().slaveTransaction();
        const auto taskIds = getAllTaskIds(*txn);
        UNIT_ASSERT_VALUES_EQUAL(taskIds.size(), 2);
    }
}

} // test_importer suite

} // namespace maps::wiki::feedback::api::samsara_importer:tests
