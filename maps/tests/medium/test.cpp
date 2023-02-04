#include <maps/indoor/long-tasks/src/barrier-feedback/lib/emitter_impl.h>

#include <maps/libs/common/include/file_utils.h>
#include <maps/libs/http/include/test_utils.h>
#include <maps/libs/local_postgres/include/instance.h>
#include <maps/libs/pgpool/include/pgpool3.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>

namespace maps::mirc::barriers::test {

namespace {
    const std::string MOCK_URL = "http://mock/";
    const std::string BARRIER_ID = "6";
    const std::string FEEDBACK_TASK_ID = "123";
} // namespace

class FeedbackEmitter : private FeedbackEmitterImpl {
public:
    FeedbackEmitter(http::URL nmapsFeedbackUrl, const std::string& connString)
        : FeedbackEmitterImpl(std::move(nmapsFeedbackUrl))
        , dbPool_(connString, pgpool3::PoolConstants{4, 4, 4, 4})
    {
    }

    void run()
    {
        auto txn = dbPool_.slaveTransaction();
        auto barriers = getUnprocessedBarrierIds(*txn);

        UNIT_ASSERT(barriers.size() == 1);
        const auto& barrierId = barriers[0];
        UNIT_ASSERT_EQUAL(barrierId, BARRIER_ID);

        txn = dbPool_.masterWriteableTransaction();
        auto maybeBarrier = loadBarrier(*txn, barrierId);
        UNIT_ASSERT(maybeBarrier);
        auto taskDefinition = generateFeedbackTaskDefinition(*maybeBarrier);

        auto mockHandle = http::addMock(MOCK_URL, [&](const http::MockRequest& request) {
            UNIT_ASSERT_EQUAL(taskDefinition, request.body);
            return http::MockResponse(R"(
                {
                    "feedbackTask": {
                        "id": ")" + FEEDBACK_TASK_ID + R"("
                    }
                }
            )");
        });

        auto feedbackTaskId = postFeedbackTask(taskDefinition);
        UNIT_ASSERT_EQUAL(feedbackTaskId, FEEDBACK_TASK_ID);
        assignFeedbackTaskInDb(*txn, maybeBarrier->id, feedbackTaskId);
        txn->commit();

        txn = dbPool_.masterReadOnlyTransaction();
        auto selectResult = txn->exec("SELECT feedback_task_id FROM ugc.barrier WHERE barrier_id = "
            + txn->quote(maybeBarrier->id));
        UNIT_ASSERT(!selectResult.empty());
        UNIT_ASSERT_EQUAL(selectResult[0][0].as<std::string>(), feedbackTaskId);

        auto barriers2 = getUnprocessedBarrierIds(*txn);
        UNIT_ASSERT(barriers2.empty());
    }

private:
    pgpool3::Pool dbPool_;
};

Y_UNIT_TEST_SUITE(medium_test_mirc_barriers)
{

Y_UNIT_TEST(send_feedback)
{
    local_postgres::Database db;
    db.createExtension("postgis");
    db.createExtension("pg_trgm");
    db.runPgMigrate(SRC_("../../../../../migrations/migrations").c_str());
    db.executeSql(maps::common::readFileToString(SRC_("data/stuff.sql")).c_str());

    FeedbackEmitter emitter{MOCK_URL, db.connectionString()};
    emitter.run();
}

}

} // namespace maps::mirc::barriers::test
