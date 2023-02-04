#include <maps/wikimap/feedback/api/src/samsara_importer/lib/importer.h>
#include <maps/wikimap/feedback/api/src/samsara_importer/lib/import_for_nmaps.h>
#include <maps/wikimap/feedback/api/src/samsara_importer/tests/medium_tests/common.h>

#include <maps/libs/chrono/include/time_point.h>
#include <maps/libs/http/include/test_utils.h>
#include <maps/libs/log8/include/log8.h>
#include <maps/wikimap/feedback/api/src/libs/common/config.h>
#include <maps/wikimap/feedback/api/src/libs/dbqueries/constants.h>
#include <maps/wikimap/feedback/api/src/libs/feedback_task_query_builder/select_query.h>
#include <maps/wikimap/feedback/api/src/libs/test_helpers/common.h>
#include <maps/wikimap/feedback/api/src/libs/test_helpers/db_fixture.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <sstream>

namespace maps::wiki::feedback::api::samsara_importer::tests {

namespace {

pgpool3::Pool& dbPool()
{
    static api::tests::DbFixture db;
    return db.pool();
}

} // unnamed namespace

Y_UNIT_TEST_SUITE(test_importer_2)
{

Y_UNIT_TEST(import_locale)
{
    TestGlobals globals(dbPool());

    // import position from article
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
                return http::MockResponse::fromFile(SRC_("data/samsara_tickets_location.json"));
            });
        importUnstructuredFeedback(globals, samsara::QUEUE_CONTENT_2L, ImportForNmaps(globals));

        auto txn = dbPool().slaveTransaction();
        const auto taskIds = getAllTaskIds(*txn);
        UNIT_ASSERT_VALUES_EQUAL(taskIds.size(), 1);

        auto task = loadMandatoryFeedbackTask(*txn, taskIds.front());

        auto position = task.originalTask.formPoint();
        UNIT_ASSERT_DOUBLES_EQUAL(position.x(), 30.55136123, 0.0001);
        UNIT_ASSERT_DOUBLES_EQUAL(position.y(), 60.14745922, 0.0001);
    }
}

} // test_importer_2 suite

} // namespace maps::wiki::feedback::api::samsara_importer:tests
