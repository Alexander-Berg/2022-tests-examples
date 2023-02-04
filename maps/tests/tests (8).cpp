#include <yandex/maps/wiki/unittest/localdb.h>
#include <yandex/maps/wiki/unittest/query_helpers.h>
#include <maps/libs/common/include/exception.h>
#include <maps/wikimap/mapspro/libs/taskutils/include/taskutils.h>
#include <maps/libs/concurrent/include/threadpool.h>
#include <maps/libs/pgpool/include/pgpool3.h>

#include <library/cpp/testing/common/env.h>
#include <boost/test/unit_test.hpp>

#include <list>
#include <iostream>
#include <stdexcept>
#include <memory>
#include <atomic>
#include <thread>


namespace maps::wiki::taskutils::tests {

namespace {

const std::string DB_SCHEMA = "service";
const TUid TEST_USER_ID = 777;
const TUid TEST_USER2_ID = 888;
const std::string TEST_SERVICE_NAME = "test srv";
const std::string TEST_TASK_NAME = "test method";
const std::string TEST_INPUT_DATA = "test_input_data=123";
const std::string EMPTY_METADATA = "{}";

class GlobalFixture: public wiki::unittest::RandomDatabaseFixture {
public:
    GlobalFixture(const std::string& dirWithSqlFiles)
        : RandomDatabaseFixture()
    {
        using wiki::unittest::QueryHelpers;

        QueryHelpers(pool().getMasterConnection().get())
            .applySqlFiles(dirWithSqlFiles);
    }
};


pgpool3::Pool&
pool()
{
    static const auto sql =
        ArcadiaSourceRoot() + "/maps/wikimap/mapspro/libs/taskutils/sql";
    static GlobalFixture fixture(sql);
    return fixture.pool();
}

} // namespace

class ConnectionGetter {
public:
    static pqxx::connection& get()
    {
        static ConnectionGetter cg;
        return cg.connHandle_.get();
    }

private:
    ConnectionGetter()
        : connHandle_(pool().getMasterConnection())
    {}

private:
    pgpool3::ConnectionHandle connHandle_;
};


class TaskContext;

class TaskNotifier {
public:
    virtual ~TaskNotifier() {}

    virtual void beforeStart(const TaskContext&) {}
    virtual void afterStart(const TaskContext&) {}
    virtual void beforeFinish(const TaskContext&) {}
    virtual void afterFinish(const TaskContext&) {}
};
typedef std::shared_ptr<TaskNotifier> TaskNotifierPtr;


class TaskContext {
public:
    TaskContext(TaskNotifierPtr notifier, const Task& task)
        : notifier_(notifier)
        , task_(task)
    {
    }

    const Task& task() const { return task_; }
    const TaskResult& result() const { return result_; }
    const std::string& outputData() const { return outputData_; }

    void run() noexcept
    {
        try {
            exec();
        }
        catch (const std::exception &e) {
            std::cerr << "catch exception: " << e.what() << std::endl;
        }
        catch (...) {
            std::cerr << "catch unknown exception" << std::endl;
        }
    }

private:
    void exec()
    {
        ASSERT(result_.status() == TaskStatus::Unknown);
        if (notifier_) {
            notifier_->beforeStart(*this);
        }

        // acquire connection/transaction from pool
        auto connHandle = pool().getMasterConnection();
        {
            pqxx::work txn(connHandle.get());

            result_ = task_.start(txn); // commit
            if (notifier_) {
                notifier_->afterStart(*this);
            }
            if (result_.status() != TaskStatus::Started) {
                return;
            }
        } // we can release connection/transaction into pool here if it is not used below


        // hard work
        //
        // we do not release connection because it is used here
        pqxx::work txn(connHandle.get());
        pqxx::result r = txn.exec("select now()");

        outputData_ = std::to_string(task_.id()) + " : completed at " + r[0][0].as<std::string>();

        if (notifier_) {
            notifier_->beforeFinish(*this);
        }
        result_ = task_.finish(txn, outputData_); // commit
        if (notifier_) {
            notifier_->afterFinish(*this);
        }
    }

private:
    TaskNotifierPtr notifier_;
    Task task_;
    TaskResult result_;
    std::string outputData_;
};


class TaskNotifierDoneCount : public TaskNotifier {
public:
    TaskNotifierDoneCount() : doneCount_(0) {}

    size_t doneCount() const
    {
        return doneCount_;
    }


    void afterFinish(const TaskContext& tctx) override
    {
        if (tctx.result().status() == TaskStatus::Done) {
            ++doneCount_;
        }
    }

private:
    std::atomic<size_t> doneCount_;
};



class TestTable {
public:
    TestTable()
    {
        static bool initialized = false;
        if (!initialized) {
            auto& conn = ConnectionGetter::get();

            pqxx::work txn(conn);
            txn.exec("TRUNCATE " + DB_SCHEMA + ".taskutils_tasks");
            txn.commit();
            initialized = true;
        }
        manager.reset(new TaskManager("test secret words", DB_SCHEMA));
    }

    ~TestTable()
    {
        cleanup();
    }

    Task createTask(pqxx::work& txn, size_t deadlineTimeout, const std::string& metaDataJson = EMPTY_METADATA)
    {
        TaskInfo info(TEST_SERVICE_NAME, TEST_TASK_NAME, TEST_INPUT_DATA, metaDataJson);
        Task task = manager->create(txn, info, TEST_USER_ID, deadlineTimeout);
        BOOST_CHECK_EQUAL(task.id(), task.token().id());
        ids_.push_back(task.id());
        return task;
    }

    Task createTask(pqxx::connection& conn, size_t deadlineTimeout, const std::string& metaDataJson = EMPTY_METADATA)
    {
        pqxx::work txn(conn);
        return createTask(txn, deadlineTimeout, metaDataJson);
    }

    std::unique_ptr<TaskManager> manager;

private:
    void cleanup()
    {
        if (ids_.empty()) {
            return;
        }

        std::stringstream ss;
        auto it = ids_.begin();
        ss << *it++;
        while (it != ids_.end()) {
            ss << ',' << *it++;
        }

        pqxx::connection& conn = ConnectionGetter::get();
        pqxx::work txn(conn);
        txn.exec("delete from " + DB_SCHEMA + ".taskutils_tasks where id in (" + ss.str() + ")");
        txn.commit();
    }

    std::list<TaskID> ids_;
};

BOOST_FIXTURE_TEST_SUITE(Tests, TestTable)

BOOST_AUTO_TEST_CASE(test_connection)
{
    auto& conn = ConnectionGetter::get();
    pqxx::work txn(conn);

    pqxx::result r = txn.exec("select 123");
    BOOST_CHECK(!r.empty());
    BOOST_CHECK_EQUAL(r[0][0].as<int>(), 123);
}

BOOST_AUTO_TEST_CASE(test_create_empty_timeout)
{
    auto& conn = ConnectionGetter::get();
    BOOST_CHECK_THROW(createTask(conn, 0), WrongEmptyTimeoutException);
}

BOOST_AUTO_TEST_CASE(test_invalid_tokens)
{
    std::string secretWord = manager->tokenSecretWord();

    BOOST_CHECK_THROW(Token("", secretWord), InvalidTokenException);
    BOOST_CHECK_THROW(Token("123", secretWord), InvalidTokenException);
    BOOST_CHECK_THROW(Token("123.123", secretWord), InvalidTokenException);
    BOOST_CHECK_THROW(Token("123.123.123", secretWord), InvalidTokenException);
    BOOST_CHECK_THROW(Token("123.123.123.123", secretWord), InvalidTokenException);
    BOOST_CHECK_THROW(Token("123.123.12345678901234567890123456789012.123", secretWord), InvalidTokenException);

    BOOST_CHECK_THROW(Token(0, 0, time(0), secretWord), InvalidTokenException);

    Token token(1, 0, time(0), secretWord);
    std::string str = token.str();

    BOOST_CHECK_THROW(Token("xxx" + str, secretWord), InvalidTokenException);
}

BOOST_AUTO_TEST_CASE(test_expired_token)
{
    std::string secretWord = manager->tokenSecretWord();
    BOOST_CHECK_THROW(Token(1, 0, time(0), secretWord).checkExpires(), ExpiredException);
}

BOOST_AUTO_TEST_CASE(test_parse_token)
{
    std::string secretWord = manager->tokenSecretWord();
    Token token(1, 0, time(0), secretWord);

    std::string str = token.str();

    Token clonedToken(str, secretWord);
    BOOST_CHECK_EQUAL(str, clonedToken.str());
}

BOOST_AUTO_TEST_CASE(test_create_and_load)
{
    auto& conn = ConnectionGetter::get();

    size_t deadlineTimeout = 60; // 1 min
    Task task = createTask(conn, deadlineTimeout);

    const TaskInfo &info = task.info();
    BOOST_CHECK(task.id() > 0);

    BOOST_CHECK_EQUAL(info.serviceName(), TEST_SERVICE_NAME);
    BOOST_CHECK_EQUAL(info.taskName(), TEST_TASK_NAME);
    BOOST_CHECK_EQUAL(info.inputData(), TEST_INPUT_DATA);

    pqxx::work txn(conn);

    BOOST_CHECK_EQUAL(manager->load(txn, task.id()).status(), TaskStatus::Created);
    BOOST_CHECK_EQUAL(manager->load(txn, task.token()).status(), TaskStatus::Created);
}

BOOST_AUTO_TEST_CASE(test_create_and_find_and_load)
{
    auto& conn = ConnectionGetter::get();

    size_t deadlineTimeout = 60; // 1 min
    BOOST_CHECK_NO_THROW(createTask(conn, deadlineTimeout,
        R"({"primary-object":"1111", "object_name":"город 'Москва'"})"));
    pqxx::work txn(conn);
    auto tokens = manager->findTasksTokens(txn, R"({"primary-object":"2222"})",
        TEST_USER_ID, TaskManager::FindPolicy::Active);
    BOOST_CHECK(tokens.empty());
    tokens = manager->findTasksTokens(txn, R"({"primary-object":"1111"})",
        TEST_USER_ID, TaskManager::FindPolicy::Active);
    BOOST_CHECK_EQUAL(tokens.size(), 1);
    BOOST_CHECK_EQUAL(manager->load(txn, tokens[0]).status(), TaskStatus::Created);
    tokens = manager->findTasksTokens(txn, R"({"primary-object":"1111"})",
        TEST_USER2_ID, TaskManager::FindPolicy::Active);
    BOOST_CHECK(tokens.empty());
}


BOOST_AUTO_TEST_CASE(test_exec_and_try_cancel)
{
    auto& conn = ConnectionGetter::get();

    size_t deadlineTimeout = 60; // 1 min
    Task task = createTask(conn, deadlineTimeout);

    std::list<TaskID> ids;
    std::shared_ptr<TaskNotifierDoneCount> notifier(new TaskNotifierDoneCount());

    concurrent::ThreadPool tpool(concurrent::ThreadsNumber{3});

    tpool.add(std::bind(&TaskContext::run, TaskContext(notifier, task)));
    ids.push_back(task.id());

    for (size_t i = 0; i < 10; ++i) {
        Task task = createTask(conn, deadlineTimeout);

        tpool.add(std::bind(&TaskContext::run, TaskContext(notifier, task)));
        ids.push_back(task.id());
    }

    for (size_t pass = 0; pass < 3 && notifier->doneCount() != ids.size(); ++pass) {
        std::this_thread::sleep_for(std::chrono::seconds(1));
    }
    BOOST_CHECK(notifier->doneCount() == ids.size());

    for (auto it = ids.begin(), end = ids.end(); it != end; ++it) {
        pqxx::work txn(conn);
        BOOST_CHECK(!manager->cancel(txn, *it));
    }
}

BOOST_AUTO_TEST_CASE(test_fail)
{
    auto& conn = ConnectionGetter::get();

    size_t deadlineTimeout = 60; // 1 min
    Task task = createTask(conn, deadlineTimeout);

    static const std::string ERR = "err";
    {
        pqxx::work txn(conn);
        TaskResult res = task.fail(txn, ERR);
        BOOST_CHECK_EQUAL(res.status(), TaskStatus::Failed);
        BOOST_CHECK_EQUAL(res.statusStr(), "failed");
    }
    {
        pqxx::work txn(conn);
        TaskResult res = manager->load(txn, task.id());
        BOOST_CHECK_EQUAL(res.status(), TaskStatus::Failed);
        BOOST_CHECK_EQUAL(res.statusStr(), "failed");
    }

    {
        pqxx::work txn(conn);
        TaskResult res = task.start(txn);
        BOOST_CHECK_EQUAL(res.status(), TaskStatus::Unknown);
        BOOST_CHECK_EQUAL(res.statusStr(), "unknown");
    }
    {
        pqxx::work txn(conn);
        task.fail(txn, ERR);
    }

    pqxx::work txn(conn);
    TaskResult res = manager->load(txn, task.id());
    BOOST_CHECK_EQUAL(res.status(), TaskStatus::Failed);
    BOOST_CHECK_EQUAL(res.statusStr(), "failed");
    BOOST_CHECK_EQUAL(res.errorMessage(), ERR);
}

BOOST_AUTO_TEST_CASE(test_cancel)
{
    auto& conn = ConnectionGetter::get();

    size_t deadlineTimeout = 60; // 1 min
    TaskID id = createTask(conn, deadlineTimeout).id(); // commit

    pqxx::work txn(conn);
    BOOST_CHECK(manager->cancel(txn, id));
    {
        pqxx::work txn(conn);
        TaskResult res = manager->load(txn, id);
        BOOST_CHECK_EQUAL(res.status(), TaskStatus::Canceled);
        BOOST_CHECK_EQUAL(res.statusStr(), "canceled");
    }
}

BOOST_AUTO_TEST_CASE(test_cancel_after_start)
{
    auto& conn = ConnectionGetter::get();

    size_t deadlineTimeout = 60; // 1 min
    Task task = createTask(conn, deadlineTimeout); // commit

    {
        pqxx::work txn(conn);
        BOOST_CHECK_EQUAL(task.start(txn).status(), TaskStatus::Started);
    }

    {
        pqxx::work txn(conn);
        BOOST_CHECK(manager->cancel(txn, task.id()));
    }
    {
        pqxx::work txn(conn);
        TaskResult res = task.finish(txn, "must be skipped!");
        BOOST_CHECK_EQUAL(res.status(), TaskStatus::Unknown); // finish skipped without load real status
        BOOST_CHECK_EQUAL(res.statusStr(), "unknown");
    }
    {
        pqxx::work txn(conn);
        BOOST_CHECK_EQUAL(task.load(txn).status(), TaskStatus::Canceled);
    }
}

BOOST_AUTO_TEST_CASE(test_expired)
{
    auto& conn = ConnectionGetter::get();

    size_t deadlineTimeout = 1; // 1 second
    Task task = createTask(conn, deadlineTimeout);

    std::this_thread::sleep_for(std::chrono::seconds(2));

    BOOST_CHECK(task.token().expired());
    BOOST_CHECK_THROW(task.token().checkExpires(), ExpiredException);

    pqxx::work txn(conn);
    TaskResult res = task.start(txn);
    BOOST_CHECK_EQUAL(res.status(), TaskStatus::Expired);
    BOOST_CHECK_EQUAL(res.statusStr(), "expired");
}

BOOST_AUTO_TEST_CASE(test_expired_on_work)
{
    auto& conn = ConnectionGetter::get();

    size_t deadlineTimeout = 1; // 1 second
    Task task = createTask(conn, deadlineTimeout);

    {
        pqxx::work txn(conn);
        BOOST_CHECK_EQUAL(task.start(txn).status(), TaskStatus::Started);
    }
    std::this_thread::sleep_for(std::chrono::seconds(2));

    BOOST_CHECK(task.token().expired());
    BOOST_CHECK_THROW(task.token().checkExpires(), ExpiredException);

    {
        pqxx::work txn(conn);
        TaskResult res = task.finish(txn, "must be skipped!");
        BOOST_CHECK_EQUAL(res.status(), TaskStatus::Expired);
    }
}

BOOST_AUTO_TEST_CASE(test_remove_expired)
{
    auto& conn = ConnectionGetter::get();

    size_t deadlineTimeout = 1; // 1 second
    size_t sleepTimeout = deadlineTimeout + 1;

    Task taskCreated = createTask(conn, deadlineTimeout);

    Task taskStarted = createTask(conn, deadlineTimeout);
    {
        pqxx::work txn(conn);
        BOOST_CHECK_EQUAL(taskStarted.start(txn).status(), TaskStatus::Started);
    }
    std::this_thread::sleep_for(std::chrono::seconds(sleepTimeout));

    BOOST_CHECK(taskCreated.token().expired());
    BOOST_CHECK_THROW(taskStarted.token().checkExpires(), ExpiredException);

    Task taskNotExpired = createTask(conn, deadlineTimeout);
    BOOST_CHECK(!taskNotExpired.token().expired());

    auto removeExpired = [this, &conn](size_t keepTimeSeconds)
    {
        pqxx::work txn(conn);
        return manager->removeExpired(txn, keepTimeSeconds);
    };

    BOOST_CHECK_EQUAL(removeExpired(sleepTimeout + 1), 0);
    BOOST_CHECK_EQUAL(removeExpired(deadlineTimeout), 2); // del Created, Started
    BOOST_CHECK_EQUAL(removeExpired(0), 0); // skip NotExpired
}

BOOST_AUTO_TEST_CASE(test_terminate)
{
    auto& conn = ConnectionGetter::get();

    size_t deadlineTimeout = 60; // 1 min

    Task taskCreated = createTask(conn, deadlineTimeout);

    Task taskStarted = createTask(conn, deadlineTimeout);
    {
        pqxx::work txn(conn);
        BOOST_CHECK_EQUAL(taskStarted.start(txn).status(), TaskStatus::Started);
    }

    Task taskDone = createTask(conn, deadlineTimeout);
    {
        pqxx::work txn(conn);
        BOOST_CHECK_EQUAL(taskDone.start(txn).status(), TaskStatus::Started);
    }

    static const std::string DONE = "done";
    {
        pqxx::work txn(conn);
        TaskResult res = taskDone.finish(txn, DONE);
        BOOST_CHECK_EQUAL(res.status(), TaskStatus::Done);
    }

    {
        pqxx::work txn(conn);
        BOOST_CHECK_EQUAL(taskCreated.load(txn).status(), TaskStatus::Created);
        BOOST_CHECK_EQUAL(taskStarted.load(txn).status(), TaskStatus::Started);
        BOOST_CHECK_EQUAL(taskDone.load(txn).status(), TaskStatus::Done);
    }
    {
        pqxx::work txn(conn);
        manager->terminateAll(txn);
    }
    {
        pqxx::work txn(conn);

        TaskResult resCreated = taskCreated.load(txn);
        BOOST_CHECK_EQUAL(resCreated.status(), TaskStatus::Terminated);
        BOOST_CHECK_EQUAL(resCreated.statusStr(), "terminated");

        BOOST_CHECK_EQUAL(taskStarted.load(txn).status(), TaskStatus::Terminated);

        TaskResult res = taskDone.load(txn);
        BOOST_CHECK_EQUAL(res.status(), TaskStatus::Done);
        BOOST_CHECK_EQUAL(res.statusStr(), "done");
        BOOST_CHECK_EQUAL(res.result(), DONE);
    }
}

BOOST_AUTO_TEST_SUITE_END()

} // namespace maps::wiki::taskutils::tests
