#include <maps/wikimap/feedback/api/src/samsara_importer/tests/medium_tests/common.h>

#include <maps/wikimap/feedback/api/src/samsara_importer/lib/importer.h>

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

const avatars::Client& avatarsClientStatic()
{
    static const auto config = maps::json::Value::fromFile(SRC_("data/feedback_api.conf"));
    static const auto client = avatars::Client(
        config["yandexHosts"]["avatarsInt"].as<std::string>(),
        config["yandexHosts"]["avatars"].as<std::string>());

    return client;
}

const samsara::Client& samsaraClientStatic()
{
    static const auto client = samsara::Client(
        maps::json::Value::fromFile(SRC_("data/feedback_api.conf")),
        "test_token");

    return client;
}

TestGlobals::TestGlobals(pgpool3::Pool& dbPool)
    : dbPool_(dbPool)
{
    auto config = maps::json::Value::fromFile(SRC_("data/feedback_api.conf"));

    samsaraToTrackerRules_ = parseSamsaraToTrackerRules(config["samsaraToTrackerRules"]);
}

pgpool3::Pool& TestGlobals::pool() const
{
    return dbPool_;
}

const ClassificationToRedirectRule& TestGlobals::samsaraToTrackerRules() const
{
    return samsaraToTrackerRules_;
}

const avatars::Client& TestGlobals::avatarsClient() const
{
    return avatarsClientStatic();
}

const samsara::Client& TestGlobals::samsaraClient() const
{
    return samsaraClientStatic();
}

std::vector<TaskId> getAllTaskIds(pqxx::transaction_base& txn)
{
    std::vector<TaskId> retVal;
    auto rows = txn.exec("SELECT id FROM feedback_task");
    for (const auto& row : rows) {
        retVal.emplace_back(row[0].as<std::string>());
    }
    return retVal;
}

FeedbackTask loadMandatoryFeedbackTask(
    pqxx::transaction_base& txn,
    const TaskId& taskId)
{
    auto query = feedback_task_query_builder::SelectQuery(
        feedback_task_query_builder::WhereConditions().id(taskId));
    auto result = query.exec(txn);
    REQUIRE(
        result.size() == 1,
        "Found " << result.size() << " tasks for id="
            << taskId.value() << ", expected 1 task"
    );
    return result.front();
}

} // namespace maps::wiki::feedback::api::samsara_importer:tests
