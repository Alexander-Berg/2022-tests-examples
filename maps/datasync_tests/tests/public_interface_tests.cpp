#include <yandex/datasync/database_manager_factory.h>
#include <yandex/datasync/collection.h>
#include <yandex/datasync/database.h>
#include <yandex/datasync/database_manager.h>
#include <yandex/datasync/error.h>
#include <yandex/datasync/iterator.h>
#include <yandex/datasync/list.h>
#include <yandex/datasync/record.h>
#include <yandex/datasync/resolution_rule.h>
#include <yandex/datasync/snapshot.h>
#include <yandex/datasync/value_type.h>

#include <yandex/maps/runtime/auth/test/mock_account.h>
#include <yandex/maps/runtime/config/mock_config.h>
#include <yandex/maps/runtime/async/future.h>
#include <yandex/maps/runtime/async/promise.h>
#include <yandex/maps/runtime/network/exceptions.h>
#include <yandex/maps/runtime/network/async.h>
#include <yandex/maps/runtime/network/auth.h>
#include <yandex/maps/runtime/logging/logging.h>
#include <yandex/maps/runtime/async/dispatcher.h>

#include <boost/test/unit_test.hpp>
#include <boost/optional/optional.hpp>
#include <boost/variant/variant.hpp>
#include <boost/variant/get.hpp>
#include <boost/variant/static_visitor.hpp>

#include <algorithm>
#include <string>
#include <vector>
#include <memory>

using namespace yandex::datasync;
namespace ymr = yandex::maps::runtime;

const std::string TEST_DATABASE = "non_existing_database";
const std::string COLLECTION = "collectionId";
const std::string RECORD = "recordId";
const std::string FIELD = "fieldId";

#define UI_THREAD_BEGIN() \
    ymr::async::ui()->spawn([&] {
#define UI_THREAD_END() \
    }).wait();

namespace {

auto mockAccount()
{
    return std::make_shared<ymr::auth::test::MockAccount>();
}

template<typename ExpectedErrorType = ymr::Error>
class MockDatabaseListener: public DatabaseListener {
public:
    MockDatabaseListener():
        futureCall_(promiseCall_.future()),
        futureInfo_(promiseInfo_.future()),
        futureSnapshot_(promiseSnapshot_.future()),
        futureSyncStarted_(promiseSyncStarted_.future()),
        futureSyncFinished_(promiseSyncFinished_.future()),
        futureReset_(promiseReset_.future()),
        futureError_(promiseError_.future()),
        timeout_(std::chrono::seconds(3))
    {}

    void setSnapshot(Snapshot* snapshot)
    {
        promiseSnapshot_.yield(snapshot);
        promiseCall_.yield(0);
    }

    virtual void onDatabaseInfo(const DatabaseInfo& info) override
    {
        promiseInfo_.yield(info);
        promiseCall_.yield(0);
    }

    virtual void onDatabaseSyncStarted() override
    {
        promiseSyncStarted_.yield(0);
        promiseCall_.yield(0);
    }

    virtual void onDatabaseSyncFinished() override
    {
        promiseSyncFinished_.yield(0);
        promiseCall_.yield(0);
    }

    virtual void onDatabaseReset() override
    {
        promiseReset_.yield(0);
        promiseCall_.yield(0);
    }

    virtual void onDatabaseError(ymr::Error* error) override
    {
        promiseError_.yield(dynamic_cast<ExpectedErrorType*>(error));
        promiseCall_.yield(0);
    }

    DatabaseInfo waitInfo()
    {
        waitCall();

        auto future = promiseInfo_.future();
        BOOST_REQUIRE(
            future.waitFor(std::chrono::steady_clock::duration::zero()) ==
                ymr::async::FutureStatus::Ready);
        return future.get();
    }

    Snapshot* waitSnapshot()
    {
        waitCall();

        BOOST_REQUIRE(
            futureSnapshot_.waitFor(std::chrono::steady_clock::duration::zero()) ==
                ymr::async::FutureStatus::Ready);
        return futureSnapshot_.get();
    }

    void waitSyncStarted()
    {
        waitCall();

        BOOST_REQUIRE(
            futureSyncStarted_.waitFor(std::chrono::steady_clock::duration::zero()) ==
                ymr::async::FutureStatus::Ready);
        futureSyncStarted_.get();
    }

    void waitSyncFinished()
    {
        waitCall();

        BOOST_REQUIRE(isSyncFinished());
    }

    void waitReset()
    {
        waitCall();

        BOOST_REQUIRE(
            futureReset_.waitFor(std::chrono::steady_clock::duration::zero()) ==
                ymr::async::FutureStatus::Ready);
        futureReset_.get();
    }

    void waitExpectedError()
    {
        waitCall();

        BOOST_REQUIRE(isExpectedError());
    }

    void waitCall()
    {
        BOOST_REQUIRE(futureCall_.waitFor(timeout_) ==
            ymr::async::FutureStatus::Ready);
        futureCall_.get();
    }

    bool isSyncFinished()
    {
        if (futureSyncFinished_.waitFor(timeout_) ==
                ymr::async::FutureStatus::Ready) {
            futureSyncFinished_.get();
            return true;
        }
        return false;
    }

    bool isExpectedError()
    {
        if (futureError_.waitFor(std::chrono::steady_clock::duration::zero()) ==
                ymr::async::FutureStatus::Ready) {
            return futureError_.get();
        }
        return false;
    }

    void setWaitTimeout(const std::chrono::steady_clock::duration& timeout)
    {
        timeout_ = timeout;
    }

private:
    ymr::async::MultiPromise<int> promiseCall_;
    ymr::async::MultiFuture<int> futureCall_;
    ymr::async::MultiPromise<DatabaseInfo> promiseInfo_;
    ymr::async::MultiFuture<DatabaseInfo> futureInfo_;
    ymr::async::MultiPromise<Snapshot*> promiseSnapshot_;
    ymr::async::MultiFuture<Snapshot*> futureSnapshot_;
    ymr::async::MultiPromise<int> promiseSyncStarted_;
    ymr::async::MultiFuture<int> futureSyncStarted_;
    ymr::async::MultiPromise<int> promiseSyncFinished_;
    ymr::async::MultiFuture<int> futureSyncFinished_;
    ymr::async::MultiPromise<int> promiseReset_;
    ymr::async::MultiFuture<int> futureReset_;
    ymr::async::MultiPromise<bool> promiseError_;
    ymr::async::MultiFuture<bool> futureError_;

    std::chrono::steady_clock::duration timeout_;
};

} // namespace

struct GlobalFixture {
    GlobalFixture() {
        UI_THREAD_BEGIN()
            databaseManager()->initialize("0", "0");
            databaseManager()->onResume();
            databaseManager()->setApiKey("");
        UI_THREAD_END()
    }
};

struct ClearDatabaseFixture {
    ClearDatabaseFixture() {
        //ensure remote doesn't exist
        ymr::async::Promise<bool> promiseDeleted;
        auto future = promiseDeleted.future();
        UI_THREAD_BEGIN()
            databaseManager()->deleteDatabase(TEST_DATABASE,
                [&] ()
                {
                    promiseDeleted.setValue(true);
                },
                [&] (ymr::Error* error)
                {
                    promiseDeleted.setValue(dynamic_cast<ymr::network::NotFoundError*>(error));
                },
                mockAccount());
        UI_THREAD_END()
        BOOST_CHECK(future.get());
        //reset local
        auto listener = std::make_shared<MockDatabaseListener<>>();
        UI_THREAD_BEGIN()
            Database* database = databaseManager()->openDatabase(TEST_DATABASE, mockAccount());
            database->addListener(listener);
            database->requestReset();
        UI_THREAD_END()
        listener->waitReset();
    }
};

BOOST_GLOBAL_FIXTURE(GlobalFixture);

BOOST_FIXTURE_TEST_SUITE(missing_database_suite, ClearDatabaseFixture)

BOOST_AUTO_TEST_CASE(info_missing_remote)
{
    auto listener = std::make_shared<MockDatabaseListener<ymr::network::NotFoundError>>();
    UI_THREAD_BEGIN()
        Database* database = databaseManager()->openDatabase(TEST_DATABASE, mockAccount());
        database->addListener(listener);
        database->requestInfo();
    UI_THREAD_END()
    listener->waitExpectedError();
}

BOOST_AUTO_TEST_CASE(delete_missing_remote)
{
    ymr::async::Promise<bool> promiseSuccess;
    UI_THREAD_BEGIN()
        databaseManager()->deleteDatabase(TEST_DATABASE,
            [&] ()
            {
                promiseSuccess.setValue(true);
            },
            [&] (ymr::Error*)
            {
                promiseSuccess.setValue(false);
            },
            mockAccount());
    UI_THREAD_END()
    BOOST_REQUIRE(promiseSuccess.future().get());
}

BOOST_AUTO_TEST_CASE(small_record)
{
    auto listener = std::make_shared<MockDatabaseListener<>>();
    Database* database = nullptr;
    UI_THREAD_BEGIN()
        database = databaseManager()->openDatabase(TEST_DATABASE, mockAccount());
        database->addListener(listener);
        BOOST_REQUIRE_NO_THROW(database->openSnapshot([listener](Snapshot* snapshot) {
            listener->setSnapshot(snapshot);
        }));
    UI_THREAD_END()

    Snapshot* snapshot = listener->waitSnapshot();
    BOOST_REQUIRE(snapshot);

    UI_THREAD_BEGIN()
        Collection* collection = snapshot->collection(COLLECTION);
        Record* record = collection->insertRecord(RECORD);
        for(size_t i = 0; i < 10000; ++i) {
            record->setField(std::to_string(i), std::to_string(i));
        }

        snapshot->sync();
        snapshot->close();
        database->requestSync();
    UI_THREAD_END()

    listener->setWaitTimeout(std::chrono::seconds(40));
    listener->waitSyncStarted();
    listener->waitSyncFinished();
}

//Request Entity Too Large
BOOST_AUTO_TEST_CASE(big_record)
{
    auto listener = std::make_shared<MockDatabaseListener<SizeLimitError>>();
    Database* database = nullptr;
    UI_THREAD_BEGIN()
        database = databaseManager()->openDatabase(TEST_DATABASE, mockAccount());
        database->addListener(listener);
        database->openSnapshot([listener](Snapshot* snapshot) {
            listener->setSnapshot(snapshot);
        });
    UI_THREAD_END()

    Snapshot* snapshot = listener->waitSnapshot();

    UI_THREAD_BEGIN()
        Collection* collection = snapshot->collection(COLLECTION);
        Record* record = collection->insertRecord(RECORD);
        //local database 10.4Mb
        for(size_t i = 0; i < 500*1000; ++i) {
            record->setField(std::to_string(i), std::to_string(i));
        }

        snapshot->sync();
        snapshot->close();
        database->requestSync();
    UI_THREAD_END()

    listener->setWaitTimeout(std::chrono::seconds(40));
    listener->waitSyncStarted();
    listener->waitExpectedError();
}

/* TODO: this test waits for a speed up fix for big records from yaDisk
//Database reached size limit
BOOST_AUTO_TEST_CASE(iterative_big_remote)
{
    auto listener = std::make_shared<MockDatabaseListener<SizeLimitError>>();
    listener->setWaitTimeout(std::chrono::seconds(40));
    Database* database = nullptr;
    UI_THREAD_BEGIN()
        database = databaseManager()->openDatabase(TEST_DATABASE, mockAccount());
        database->addListener(listener);
    UI_THREAD_END()

    size_t iter = 0;
    bool sizeLimitReached = false;
    while (!sizeLimitReached) {
        MAPS_DEBUG() << "Iteration# " + std::to_string(iter);

        UI_THREAD_BEGIN()
            database->openSnapshot([listener](Snapshot* snapshot) {
                listener->setSnapshot(snapshot);
            });
        UI_THREAD_END()

        Snapshot* snapshot = listener->waitSnapshot();

        UI_THREAD_BEGIN()
            Collection* collection = snapshot->collection(
                COLLECTION + std::to_string(iter));
            Record* record = collection->insertRecord(RECORD + std::to_string(iter));
            //local database 10.4Mb
            for(size_t i = 0; i < 15000; ++i) {
                record->setField(std::to_string(i), std::to_string(i));
            }

            snapshot->sync();
            snapshot->close();
            database->requestSync();
        UI_THREAD_END()
        listener->waitSyncStarted();
        listener->waitCall();
        sizeLimitReached = listener->isExpectedError();
        BOOST_REQUIRE(sizeLimitReached || listener->isSyncFinished());

        ++iter;
    }
}
*/

//check that remote accepts list
BOOST_AUTO_TEST_CASE(list_to_remote)
{
    auto listener = std::make_shared<MockDatabaseListener<>>();
    Database* database = nullptr;
    UI_THREAD_BEGIN()
        database = databaseManager()->openDatabase(TEST_DATABASE, mockAccount());
        database->addListener(listener);
        database->openSnapshot([listener](Snapshot* snapshot) {
            listener->setSnapshot(snapshot);
        });
    UI_THREAD_END()

    Snapshot* snapshot = listener->waitSnapshot();

    UI_THREAD_BEGIN()
        Collection* collection = snapshot->collection(COLLECTION);
        Record* record = collection->insertRecord(RECORD);
        record->setEmptyList(FIELD);
        List* list = record->fieldAsList(FIELD);
        list->appendNull();
        list->appendNull();
        list->insert(0, true);
        list->deleteItem(0);
        list->move(0, 1);
        list->set(0, true);

        snapshot->sync();
        snapshot->close();
        database->requestSync();
    UI_THREAD_END()

    listener->waitSyncStarted();
    listener->waitSyncFinished();
}

BOOST_AUTO_TEST_CASE(many_records)
{
    auto listener = std::make_shared<MockDatabaseListener<>>();
    Database* database = nullptr;
    UI_THREAD_BEGIN()
        database = databaseManager()->openDatabase(TEST_DATABASE, mockAccount());
        database->addListener(listener);
        database->openSnapshot([listener](Snapshot* snapshot) {
            listener->setSnapshot(snapshot);
        });
    UI_THREAD_END()

    Snapshot* snapshot = listener->waitSnapshot();

    UI_THREAD_BEGIN()
        Collection* collection = snapshot->collection(COLLECTION);
        //local database 995 kB
        for(size_t i = 0; i < 5000; ++i) {
            Record* record = collection->insertRecord(RECORD+std::to_string(i));
            record->setField(std::to_string(i), static_cast<int64_t>(i));
        }

        snapshot->sync();
        snapshot->close();
        database->requestSync();
    UI_THREAD_END()

    listener->setWaitTimeout(std::chrono::seconds(40));
    listener->waitSyncStarted();
    listener->waitSyncFinished();
}

BOOST_AUTO_TEST_CASE(delete_field)
{
    auto listener = std::make_shared<MockDatabaseListener<>>();
    Database* database = nullptr;
    UI_THREAD_BEGIN()
        database = databaseManager()->openDatabase(TEST_DATABASE, mockAccount());
        database->addListener(listener);
        database->openSnapshot([listener](Snapshot* snapshot) {
            listener->setSnapshot(snapshot);
        });
    UI_THREAD_END()

    Snapshot* snapshot = listener->waitSnapshot();

    UI_THREAD_BEGIN()
        Collection* collection = snapshot->collection(COLLECTION);
        Record* record = collection->insertRecord(RECORD);
        record->setField(FIELD, "test");
        snapshot->sync();
        record->deleteField(FIELD);
        snapshot->sync();
        database->requestSync();
    UI_THREAD_END()

    listener->waitSyncStarted();
    listener->waitSyncFinished();

    UI_THREAD_BEGIN()
        Collection* collection = snapshot->collection(COLLECTION);
        Record* record = collection->record(RECORD);
        record->setEmptyList(FIELD);
        snapshot->sync();
        record->deleteField(FIELD);
        snapshot->sync();
        snapshot->close();
        database->requestSync();
    UI_THREAD_END()

    listener->waitSyncStarted();
    listener->waitSyncFinished();
}

BOOST_AUTO_TEST_SUITE_END()

