#include <maps/libs/local_mongo/include/mongo_server.h>
#include <maps/libs/mongo/include/init.h>

#include <maps/libs/async_executor/include/database_mdb.h>

#include <library/cpp/testing/unittest/registar.h>

#include <chrono>
#include <cstdlib>
#include <exception>
#include <memory>
#include <optional>
#include <string>

namespace ae = maps::async_executor;

using ae::Database;
using ae::MdbDatabaseConfig;
using ae::Request;
using ae::Task;
using ae::TaskState;
using ae::TaskStatus;
using ae::TaskDcStatus;
using ae::Token;
using ae::Url;
using ae::Dc;

const auto DC1 = Dc{"DC1"};
const auto DC2 = Dc{"DC2"};
const auto DC3 = Dc{"DC3"};

constexpr uint32_t TASK_REPLICATION_FACTOR = 2;

struct Fixture : NUnitTest::TBaseFixture {
    Fixture()
    {
        maps::mongo::init();
        _localMongoServer.emplace();
    }

    std::unique_ptr<Database> makeMdbDatabase()
    {
        MdbDatabaseConfig databaseConfig;
        databaseConfig.tasksCollectionName = "test_collection";
        databaseConfig.connectionUrl = _localMongoServer->uri() + "/test_db";
        databaseConfig.expirationTime = std::chrono::seconds{2 * 60 * 60};

        return ae::createMdbDatabase(databaseConfig);
    }

    std::optional<maps::local_mongo::MongoServer> _localMongoServer;
};

Token generateToken()
{
    static int tokenNumber = 0;
    return Token{std::string{"test_token_"} + std::to_string(++tokenNumber)};
}

TaskState newTaskState() {
    return TaskState{TASK_REPLICATION_FACTOR};
}

Url generateUrl()
{
    static int urlNumber = 0;
    return Url{std::string{"test_url_"} + std::to_string(++urlNumber)};
}

std::chrono::system_clock::time_point epoch()
{
    return {};
}

Request makeRequest() {
    return Request{"test_request"};
}

void setWorkerInProgress(
    const std::unique_ptr<Database>& database,
    const Token& token,
    const Dc& dc)
{
    auto state = database->taskState(token);
    UNIT_ASSERT(state.has_value());
    state->setWorkerInProgress(dc);
    UNIT_ASSERT(database->updateTaskState(token, *state));
}

void setWorkerCompleted(
    const std::unique_ptr<Database>& database,
    const Token& token,
    const Url& url,
    const Dc& dc)
{
    auto state = database->taskState(token);
    UNIT_ASSERT(state.has_value());
    state->setWorkerCompleted(dc, url);
    UNIT_ASSERT(database->updateTaskState(token, *state));
}

void setWorkerFailed(
    const std::unique_ptr<Database>& database,
    const Token& token,
    const Dc& dc)
{
    auto state = database->taskState(token);
    UNIT_ASSERT(state.has_value());
    state->setWorkerFailed(dc);
    UNIT_ASSERT(database->updateTaskState(token, *state));
}

void assertFetchTaskResult(
    const std::unique_ptr<Database>& database,
    const Dc& dc,
    const std::optional<Task>& task)
{
    auto fetchedTask = database->fetchTask(dc);
    if (task) {
        UNIT_ASSERT(fetchedTask.has_value());
        UNIT_ASSERT_VALUES_EQUAL(fetchedTask->token, task->token);
        UNIT_ASSERT_VALUES_EQUAL(fetchedTask->request, task->request);
    } else {
        UNIT_ASSERT(!fetchedTask.has_value());
    }
};

void assertTaskStatus(
    const std::unique_ptr<Database>& database,
    const Token& token,
    TaskStatus status)
{
    auto state = database->taskState(token);
    UNIT_ASSERT(state.has_value());
    UNIT_ASSERT_VALUES_EQUAL((int)state->status(), (int)status);
}

Y_UNIT_TEST_SUITE_F(test_database_manager, Fixture)
{

Y_UNIT_TEST(empty_database)
{
    auto database = makeMdbDatabase();

    UNIT_ASSERT(!database->fetchTask(Dc{""}).has_value());
    UNIT_ASSERT(!database->fetchTask(Dc{DC1}).has_value());

    auto nonExistingToken = generateToken();
    UNIT_ASSERT(!database->taskState(nonExistingToken).has_value());

    UNIT_ASSERT(!database->updateTaskState(nonExistingToken, newTaskState()));
}

Y_UNIT_TEST(insert_fetch_order)
{
    auto database = makeMdbDatabase();

    UNIT_ASSERT(!database->fetchTask(DC1));

    Task task1 {.token = generateToken(), .request = makeRequest(),.state = newTaskState()};
    database->insertTask(task1);

    assertFetchTaskResult(database, DC1, task1);
    assertFetchTaskResult(database, DC1, task1);
    assertFetchTaskResult(database, DC2, task1);

    Task task2 {.token = generateToken(), .request = makeRequest(), .state = newTaskState()};
    database->insertTask(task2);

    Task task3 {.token = generateToken(), .request = makeRequest(), .state = newTaskState()};
    database->insertTask(task3);

    assertFetchTaskResult(database, DC1, task1);

    setWorkerInProgress(database, task1.token, DC1);
    assertFetchTaskResult(database, DC1, task2);
    assertFetchTaskResult(database, DC2, task1);

    setWorkerInProgress(database, task2.token, DC2);
    assertFetchTaskResult(database, DC1, task2);
    assertFetchTaskResult(database, DC2, task1);

    setWorkerInProgress(database, task1.token, DC2);
    assertFetchTaskResult(database, DC1, task2);
    assertFetchTaskResult(database, DC2, task3);

    setWorkerInProgress(database, task3.token, DC2);
    assertFetchTaskResult(database, DC2, std::nullopt);

    // Cannot assign a third worker when replication factor is 2.
    UNIT_ASSERT_EXCEPTION(
        setWorkerInProgress(database, task1.token, DC3),
        std::exception);
}

Y_UNIT_TEST(successful_task_lifecycle)
{
    auto database = makeMdbDatabase();

    auto token = generateToken();

    Task task {.token = token, .request = makeRequest(), .state = newTaskState()};

    // Creation time is rounded to a millisecond when stored in the DB
    auto creationTime = std::chrono::time_point_cast<std::chrono::milliseconds>(
        task.state.creationTime());

    database->insertTask(task);

    auto taskState = database->taskState(token);

    UNIT_ASSERT(taskState.has_value());
    UNIT_ASSERT_EQUAL(taskState->creationTime(), creationTime);
    UNIT_ASSERT_VALUES_EQUAL(taskState->revision(), 1);
    UNIT_ASSERT_VALUES_EQUAL((int)taskState->status(), (int)TaskStatus::InProgress);
    UNIT_ASSERT(!taskState->url());
    UNIT_ASSERT(taskState->wantWorker());
    UNIT_ASSERT(taskState->workers().empty());
    UNIT_ASSERT_VALUES_EQUAL(taskState->replicationFactor(), TASK_REPLICATION_FACTOR);

    assertFetchTaskResult(database, DC1, task);

    taskState->setWorkerInProgress(DC1);
    UNIT_ASSERT(database->updateTaskState(token, *taskState));

    assertFetchTaskResult(database, DC2, task);
}

Y_UNIT_TEST(task_status_fail)
{
    auto database = makeMdbDatabase();

    auto token = generateToken();

    Task task {.token = token, .request = makeRequest(), .state = newTaskState()};
    database->insertTask(task);

    assertTaskStatus(database, token, TaskStatus::InProgress);

    setWorkerInProgress(database, token, DC1);
    assertTaskStatus(database, token, TaskStatus::InProgress);
    setWorkerInProgress(database, token, DC2);
    assertTaskStatus(database, token, TaskStatus::InProgress);

    setWorkerFailed(database, token, DC1);
    assertTaskStatus(database, token, TaskStatus::InProgress);

    setWorkerFailed(database, token, DC2);
    assertTaskStatus(database, token, TaskStatus::Failed);
}

Y_UNIT_TEST(test_status_success)
{
    auto database = makeMdbDatabase();

    auto token = generateToken();

    Task task {.token = token, .request = makeRequest(), .state = newTaskState()};
    database->insertTask(task);

    assertTaskStatus(database, token, TaskStatus::InProgress);

    setWorkerInProgress(database, token, DC1);
    assertTaskStatus(database, token, TaskStatus::InProgress);
    setWorkerInProgress(database, token, DC2);
    assertTaskStatus(database, token, TaskStatus::InProgress);

    setWorkerFailed(database, token, DC1);
    assertTaskStatus(database, token, TaskStatus::InProgress);

    setWorkerCompleted(database, token, generateUrl(), DC2);
    assertTaskStatus(database, token, TaskStatus::Succeeded);
}

Y_UNIT_TEST(several_success_statuses)
{
    auto database = makeMdbDatabase();

    auto token = generateToken();

    Task task {.token = token, .request = makeRequest(), .state = newTaskState()};
    database->insertTask(task);

    setWorkerInProgress(database, token, DC1);
    setWorkerInProgress(database, token, DC2);
    assertTaskStatus(database, token, TaskStatus::InProgress);

    auto url1 = generateUrl();
    setWorkerCompleted(database, token, url1, DC1);

    auto state1 = database->taskState(token);
    UNIT_ASSERT(state1.has_value());
    UNIT_ASSERT_VALUES_EQUAL((int)state1->status(), (int)TaskStatus::Succeeded);
    UNIT_ASSERT_VALUES_EQUAL(*state1->url(), url1);

    auto url2 = generateUrl();
    setWorkerCompleted(database, token, url2, DC2);
    auto state2 = database->taskState(token);
    UNIT_ASSERT(state2.has_value());
    UNIT_ASSERT_VALUES_EQUAL((int)state2->status(), (int)TaskStatus::Succeeded);
    UNIT_ASSERT_VALUES_EQUAL(*state2->url(), url1);
}

Y_UNIT_TEST(invalid_task_state)
{
    auto database = makeMdbDatabase();

    auto token = generateToken();

    Task task {.token = token, .request = makeRequest(), .state = newTaskState()};
    database->insertTask(task);

    auto state = database->taskState(token);
    UNIT_ASSERT(state.has_value());

    UNIT_ASSERT(!database->updateTaskState(
        generateToken(),
        TaskState {
            state->creationTime(),
            state->revision(),
            state->url(),
            state->workers(),
            state->replicationFactor()
        }
    ));

    UNIT_ASSERT(!database->updateTaskState(
        token,
        TaskState {
            state->creationTime(),
            state->revision() + 1,
            state->url(),
            state->workers(),
            state->replicationFactor()
        }
    ));

    UNIT_ASSERT_EXCEPTION(
        TaskState(
            state->creationTime(),
            state->revision(),
            state->url().value_or(Url{}) + "_invalid_suffix",
            state->workers(),
            state->replicationFactor()).validate(),
        std::exception);

    // Calling update with another creation time does nothing.
    UNIT_ASSERT(database->updateTaskState(
        token,
        TaskState {
            state->creationTime() + std::chrono::seconds{10},
            state->revision(),
            state->url(),
            state->workers(),
            state->replicationFactor()
        }
    ));
    {
        auto newState = database->taskState(token);
        UNIT_ASSERT(newState.has_value());
        UNIT_ASSERT_VALUES_EQUAL(
            newState->creationTime().time_since_epoch().count(),
            state->creationTime().time_since_epoch().count());
        state.emplace(*newState);
    }

    // Calling update with another replication factor does nothing.
    UNIT_ASSERT(database->updateTaskState(
        token,
        TaskState {
            state->creationTime(),
            state->revision(),
            state->url(),
            state->workers(),
            state->replicationFactor() + 1
        }
    ));
    {
        auto newState = database->taskState(token);
        UNIT_ASSERT(newState.has_value());
        UNIT_ASSERT_VALUES_EQUAL(
            newState->replicationFactor(), state->replicationFactor());
        state.emplace(*newState);
    }
}

Y_UNIT_TEST(update_workers_and_url)
{
    auto database = makeMdbDatabase();

    auto token = generateToken();

    Task task {.token = token, .request = makeRequest(), .state = newTaskState()};
    database->insertTask(task);

    auto state = database->taskState(token);
    UNIT_ASSERT(state.has_value());

    UNIT_ASSERT(database->updateTaskState(
        token,
        TaskState {
            state->creationTime(),
            state->revision(),
            state->url().value_or(Url{}) + "_invalid_suffix",
            {{DC1, TaskDcStatus{TaskStatus::Succeeded, {}}}},
            state->replicationFactor()
        }
    ));

    UNIT_ASSERT(!database->updateTaskState(
        token,
        TaskState {
            state->creationTime(),
            state->revision(),
            state->url(),
            {{DC1, TaskDcStatus{TaskStatus::InProgress, {}}}},
            state->replicationFactor()
        }
    ));
}

} // Y_UNIT_TEST_SUITE_F

