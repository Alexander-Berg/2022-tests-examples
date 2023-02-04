#include <maps/renderer/denormalization/lib/tasks/tests/data_tests/common.h>

#include <maps/renderer/denormalization/lib/base/include/db/utils.h>
#include <maps/renderer/denormalization/lib/tasks/include/tasks.h>
#include <maps/renderer/denormalization/lib/tasks/tests/lib/compare_tables.h>
#include <maps/renderer/denormalization/lib/tasks/tests/lib/yt/test_context.h>

#include <maps/libs/common/include/file_utils.h>
#include <maps/libs/log8/include/log8.h>

#include <library/cpp/iterator/enumerate.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/env.h>
#include <util/generic/algorithm.h>
#include <util/generic/guid.h>

#include <string>

namespace maps::renderer::denormalization {

namespace {

const std::string DATA_TESTS_PATH = "maps/renderer/denormalization/lib/tasks/tests/data_tests";

std::string loadData(const std::string& testName, const std::string& filename)
{
    return common::readFileToString(
        common::joinPath(ArcadiaSourceRoot(), DATA_TESTS_PATH, testName, filename));
}

const std::string INPUT_SQL_FILE = "input.sql";
const std::string VALIDATE_QUERY_FILE = "validate.sql";

void checkOutputData(db::Client& client, const std::string& testName, const GardenTask& task)
{
    for (const auto& resource: task.creates()) {
        if (auto table = std::get_if<Table>(&resource)) {
            auto actualData =
                db::fetchAll(*client.reader(table->name, {}, ResourceStage::Output)->source());

            client.createTable(*table);
            client.execSqlQuery(loadData(testName, table->name + ".sql"));

            auto expectedData =
                db::fetchAll(*client.reader(table->name, {}, ResourceStage::Output)->source());

            EXPECT_TRUE(compareTables(*table, expectedData, actualData))
                << "Tables does not match, see stderr for detailed diff";
        }
    }
}

void checkValidateQuery(db::Client& client, const std::string& testName)
{
    std::string tmpTableName = "validate_query_" + CreateGuidAsString();
    Erase(tmpTableName, '-');

    client.ensureTableCreation(tmpTableName);
    client.execSqlQuery(
        "INSERT INTO {" + tmpTableName + "}\n" + loadData(testName, VALIDATE_QUERY_FILE));

    auto rows = db::fetchAll(*client.reader(tmpTableName)->source());

    for (auto [rowIdx, row]: Enumerate(rows)) {
        for (auto&& [columnName, value]: row.AsMap()) {
            ASSERT_FALSE(value.IsNull()) << "null at row " << rowIdx;
            EXPECT_TRUE(value.AsBool()) << "at row " << rowIdx;
        }
    }
}

void loadTestData(
    db::Client& client,
    const std::string& testName,
    const std::vector<TaskPtr>& tasks,
    const std::map<std::string, GardenResource>& outputResources)
{
    for (const auto& task: tasks) {
        for (const auto& name: task->demandsOutput()) {
            if (!outputResources.count(name)) {
                continue; // skip external resources
            }
            const auto& resource = outputResources.at(name);

            if (const auto* table = std::get_if<Table>(&resource)) {
                client.createTable(*table);
            }
        }
    }

    client.execSqlQuery(loadData(testName, INPUT_SQL_FILE));
    client.finalize();
}

} // namespace

DataTestFixture::DataTestFixture() : tasksMap{createTasksMap()}
{
    log8::setLevel(log8::Level::DEBUG);

    for (const auto& [name, task]: tasksMap) {
        for (const auto& resource: task->creates()) {
            REQUIRE(
                !outputResources.count(resource.name()),
                "resource " << resource.name() << " has duplicates");
            outputResources.insert({resource.name(), resource});
        }
    }
}

void DataTestFixture::run(
    const std::string& testName, TestType testType, const std::vector<std::string>& taskNames)
{
    ASSERT_FALSE(taskNames.empty());
    INFO() << "Running test " << testName;

    YtTestContext ctx;
    prepareEmptyInputData(ctx);

    std::vector<TaskPtr> tasks;
    for (const auto& name: taskNames) {
        ASSERT_TRUE(tasksMap.count(name)) << "task " << name << " not found";
        tasks.push_back(tasksMap.at(name));
    }

    loadTestData(*ctx.taskArgs()->dbClient(), testName, tasks, outputResources);

    for (const auto& task: tasks) {
        INFO() << "Running task " << task->name();
        try {
            task->call(*ctx.taskArgs());
        } catch (...) {
            ERROR() << "Error while running task " << task->name();
            throw;
        }
    }

    if (testType == TestType::ExpectedData) {
        // check only last task
        checkOutputData(*ctx.taskArgs()->dbClient(), testName, *tasks.back());
    }

    if (testType == TestType::ValidateQuery) {
        checkValidateQuery(*ctx.taskArgs()->dbClient(), testName);
    }
}

} // namespace maps::renderer::denormalization
