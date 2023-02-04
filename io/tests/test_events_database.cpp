#include <yandex_io/libs/metrica/base/events_database.h>

#include <yandex_io/libs/device/device.h>

#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

#include <iostream>
#include <memory>
#include <numeric>
#include <thread>

#include <sys/stat.h>

using quasar::EventsDatabase;
using quasar::proto::DatabaseMetricaEvent;

namespace {
    class SqliteTestFixture: public QuasarUnitTestFixture {
    public:
        using Base = QuasarUnitTestFixture;

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);

            dbFilename = tryGetRamDrivePath() + "/test.db";
            db = std::make_unique<EventsDatabase>(dbFilename, dbMaxSize);
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            std::remove(dbFilename.c_str());

            Base::TearDown(context);
        }

        std::string dbFilename;
        const int dbMaxSize = 100;
        YandexIO::Configuration::TestGuard testGuard;
        std::unique_ptr<EventsDatabase> db;
    };

    class SqliteTestFixtureSmall: public QuasarUnitTestFixture {
    public:
        using Base = QuasarUnitTestFixture;

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);

            dbFilename = tryGetRamDrivePath() + "/test.db";
            db = std::make_unique<EventsDatabase>(dbFilename, dbMaxSize);
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            std::remove(dbFilename.c_str());

            Base::TearDown(context);
        }

        std::string dbFilename;
        const int dbMaxSize = 20;
        YandexIO::Configuration::TestGuard testGuard;
        std::unique_ptr<EventsDatabase> db;
    };

    class SqliteTestFixturePS: public QuasarUnitTestFixture {
    public:
        using Base = QuasarUnitTestFixture;

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);

            runtimeDbFilename = tryGetRamDrivePath() + "/test_rt.db";
            persistentDbFilename = tryGetRamDrivePath() + "/test_ps.db";
            reinitDb();
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            std::remove(runtimeDbFilename.c_str());
            std::remove(persistentDbFilename.c_str());

            Base::TearDown(context);
        }

        void reinitDb() {
            db.reset();
            db = std::make_unique<EventsDatabase>(
                EventsDatabase::DbParams{
                    runtimeDbFilename,
                    dbMaxSize,
                    [this](auto... args) {
                        return (runtimeFilter ? runtimeFilter(std::forward<decltype(args)>(args)...) : true);
                    },
                    false,
                    ""},
                EventsDatabase::DbParams{
                    persistentDbFilename,
                    dbMaxSize,
                    [this](auto... args) {
                        return (persistentFilter ? persistentFilter(std::forward<decltype(args)>(args)...) : true);
                    },
                    false,
                    ""});
        }

        const int dbMaxSize = 20;
        std::string runtimeDbFilename;
        std::string persistentDbFilename;
        YandexIO::Configuration::TestGuard testGuard;
        std::unique_ptr<EventsDatabase> db;
        EventsDatabase::Filter runtimeFilter;
        EventsDatabase::Filter persistentFilter;
    };
} // namespace

Y_UNIT_TEST_SUITE(TestEventsDatabase) {
    Y_UNIT_TEST_F(testSqliteInsertReadAndDelete, SqliteTestFixture) {
        quasar::proto::DatabaseMetricaEvent event;
        event.mutable_new_event()->set_account_id("test_id");
        event.mutable_new_event()->set_account_type("test_login");
        event.mutable_new_event()->set_value("test_value");
        event.mutable_new_event()->set_name("test_name");
        event.mutable_new_event()->set_timestamp(std::time(nullptr));
        event.mutable_new_event()->set_serial_number(1);

        db->addEvent(event);
        auto readEvent = db->getEarliestEvent({});
        UNIT_ASSERT_VALUES_EQUAL(readEvent->id, 1u);
        UNIT_ASSERT(readEvent->databaseMetricaEvent.new_event().account_id() == event.new_event().account_id());
        UNIT_ASSERT(readEvent->databaseMetricaEvent.new_event().account_type() == event.new_event().account_type());
        UNIT_ASSERT(readEvent->databaseMetricaEvent.new_event().value() == event.new_event().value());
        UNIT_ASSERT(readEvent->databaseMetricaEvent.new_event().name() == event.new_event().name());
        UNIT_ASSERT(readEvent->databaseMetricaEvent.new_event().timestamp() == event.new_event().timestamp());
        db->deleteEvents({1});
        UNIT_ASSERT(db->getEarliestEvent({}) == nullptr);
    }

    Y_UNIT_TEST_F(testSqliteMultithread, SqliteTestFixture) {
        std::vector<std::thread> inserters;
        // Insert in different threads
        for (int i = 0; i < 10; ++i) {
            inserters.emplace_back(std::thread([&]() {
                quasar::proto::DatabaseMetricaEvent event;
                event.mutable_new_event()->set_account_id("fsd");
                event.mutable_new_event()->set_account_type("login");
                event.mutable_new_event()->set_value("value");
                event.mutable_new_event()->set_name("name");
                event.mutable_new_event()->set_timestamp(std::time(nullptr));
                for (int j = 0; j < 100; ++j) {
                    db->addEvent(event);
                }
            }));
        }
        for (auto& inserter : inserters) {
            inserter.join();
        }
        // Ensure all events were inserted, so last event has index 1000
        std::vector<uint64_t> allIndicesButLast(999);
        std::iota(allIndicesButLast.begin(), allIndicesButLast.end(), 1);
        auto lastEvent = db->getEarliestEvent(allIndicesButLast);
        UNIT_ASSERT(lastEvent != nullptr);
        std::vector<std::thread> deleters;
        // Delete in different threads
        for (int i = 0; i < 10; ++i) {
            deleters.emplace_back(std::thread([&]() {
                std::unique_ptr<EventsDatabase::Event> event;
                while ((event = db->getEarliestEvent({}))) {
                    db->deleteEvents({event->id});
                }
            }));
        }

        for (auto& deleter : deleters) {
            deleter.join();
        }
        // Ensure we deleted all events
        lastEvent = db->getEarliestEvent(allIndicesButLast);
        UNIT_ASSERT(lastEvent == nullptr);
    }

    Y_UNIT_TEST_F(testSqliteHandlesBlobWithNullChars, SqliteTestFixture) {
        DatabaseMetricaEvent event;
        auto newEvent = event.mutable_new_event();
        newEvent->set_type(DatabaseMetricaEvent::NewEvent::CLIENT);
        newEvent->set_timestamp(std::time(nullptr));
        newEvent->set_name("testName");
        newEvent->set_value("");
        newEvent->set_account_type("login");
        newEvent->set_account_id("257695511");
        newEvent->set_serial_number(1);
        // Get sure our serialization has at least one null char
        bool hasNullChar = false;
        // If we change blob format in database we should change buffer construction too
        std::string buffer = event.SerializeAsString();
        for (const auto& letter : buffer) {
            if (letter == '\0') {
                hasNullChar = true;
                break;
            }
        }
        UNIT_ASSERT(hasNullChar);

        // Put one event, get it and check if we get all values correctly
        db->addEvent(event);
        auto eventFromDb = db->getEarliestEvent({});
        UNIT_ASSERT(eventFromDb->databaseMetricaEvent.new_event().has_value());
        UNIT_ASSERT(eventFromDb->databaseMetricaEvent.new_event().has_timestamp());
        UNIT_ASSERT(eventFromDb->databaseMetricaEvent.new_event().has_name());
        UNIT_ASSERT(eventFromDb->databaseMetricaEvent.new_event().has_account_type());
        UNIT_ASSERT(eventFromDb->databaseMetricaEvent.new_event().has_account_id());
        UNIT_ASSERT(eventFromDb->databaseMetricaEvent.new_event().has_type());

        UNIT_ASSERT(eventFromDb->databaseMetricaEvent.new_event().value() == newEvent->value());
        UNIT_ASSERT(eventFromDb->databaseMetricaEvent.new_event().timestamp() == newEvent->timestamp());
        UNIT_ASSERT(eventFromDb->databaseMetricaEvent.new_event().name() == newEvent->name());
        UNIT_ASSERT(eventFromDb->databaseMetricaEvent.new_event().account_type() == newEvent->account_type());
        UNIT_ASSERT(eventFromDb->databaseMetricaEvent.new_event().account_id() == newEvent->account_id());
        UNIT_ASSERT(eventFromDb->databaseMetricaEvent.new_event().type() == newEvent->type());
    }

    Y_UNIT_TEST_F(testSqliteOverflow, SqliteTestFixtureSmall) {
        constexpr int NUM_EVENTS = 100;
        // We check database size before insertion, so after insertion one more page can be created. Page size is 4096
        const int MAX_SIZE = dbMaxSize * 1024 + 4096;
        struct stat status;
        if (stat(dbFilename.c_str(), &status)) {
            UNIT_FAIL("Cannot stat database file");
        }
        // Check if our base is created with less size than max -- if it's not, this test has no meaning and MAX_SIZE should be increased
        UNIT_ASSERT_LT(status.st_size, MAX_SIZE);
        quasar::proto::DatabaseMetricaEvent event;
        event.mutable_new_event()->set_account_id("longlonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglongid");
        event.mutable_new_event()->set_account_type("longlonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglogin");
        event.mutable_new_event()->set_value("longlonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglongvalue");
        event.mutable_new_event()->set_name("longlonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglonglongname");
        event.mutable_new_event()->set_timestamp(std::time(nullptr));
        event.mutable_new_event()->set_serial_number(1);

        // Check that we put more memory than we have in limits
        UNIT_ASSERT_LT(MAX_SIZE, (int)event.SerializeAsString().size() * NUM_EVENTS);
        for (size_t j = 0; j < NUM_EVENTS; ++j) {
            db->addEvent(event);
            stat(dbFilename.c_str(), &status);
        }
        if (stat(dbFilename.c_str(), &status)) {
            UNIT_FAIL("Cannot stat database file");
        }
        // Check our database is still less than max size
        UNIT_ASSERT_LE(status.st_size, MAX_SIZE);

        // Delete all events and check if we can put one new event
        std::vector<uint64_t> indices(NUM_EVENTS);
        std::iota(indices.begin(), indices.end(), 1);
        db->deleteEvents(indices);

        quasar::proto::DatabaseMetricaEvent smallEvent;
        smallEvent.mutable_new_event()->set_account_id("id");
        smallEvent.mutable_new_event()->set_account_type("login");
        smallEvent.mutable_new_event()->set_value("value");
        smallEvent.mutable_new_event()->set_name("name");
        smallEvent.mutable_new_event()->set_timestamp(std::time(nullptr));
        UNIT_ASSERT_LT((int)smallEvent.SerializeAsString().size(), MAX_SIZE);
        db->addEvent(smallEvent);
        // At this moment only smallEvent must be in database
        auto readEvent = db->getEarliestEvent({});
        std::cout << readEvent->databaseMetricaEvent.new_event().account_id() << std::endl;
        UNIT_ASSERT(readEvent->databaseMetricaEvent.new_event().account_id() == smallEvent.new_event().account_id());
        UNIT_ASSERT(readEvent->databaseMetricaEvent.new_event().account_type() == smallEvent.new_event().account_type());
        UNIT_ASSERT(readEvent->databaseMetricaEvent.new_event().value() == smallEvent.new_event().value());
        UNIT_ASSERT(readEvent->databaseMetricaEvent.new_event().name() == smallEvent.new_event().name());
        UNIT_ASSERT(readEvent->databaseMetricaEvent.new_event().timestamp() == smallEvent.new_event().timestamp());
    }

    Y_UNIT_TEST_F(testSimplePersistentFilter, SqliteTestFixturePS) {
        size_t rtCounter = 0;
        size_t psCounter = 0;
        runtimeFilter = [&](uint64_t /*id*/, const auto& event, bool /*saved*/) {
            if (!std::string(event.new_event().name().c_str()).starts_with("persistent_")) {
                ++rtCounter;
                return true;
            }
            return false;
        };
        persistentFilter = [&](uint64_t /*id*/, const auto& event, bool /*saved*/) {
            if (std::string(event.new_event().name().c_str()).starts_with("persistent_")) {
                ++psCounter;
                return true;
            }
            return false;
        };

        {
            DatabaseMetricaEvent event;
            auto newEvent = event.mutable_new_event();
            newEvent->set_type(DatabaseMetricaEvent::NewEvent::CLIENT);
            newEvent->set_timestamp(std::time(nullptr));
            newEvent->set_name("testName");
            newEvent->set_value("");
            newEvent->set_account_type("login");
            newEvent->set_account_id("257695511");
            newEvent->set_serial_number(1);
            db->addEvent(event);
        }

        {
            DatabaseMetricaEvent event;
            auto newEvent = event.mutable_new_event();
            newEvent->set_type(DatabaseMetricaEvent::NewEvent::CLIENT);
            newEvent->set_timestamp(std::time(nullptr));
            newEvent->set_name("persistent_testName");
            newEvent->set_value("");
            newEvent->set_account_type("login");
            newEvent->set_account_id("257695511");
            newEvent->set_serial_number(1);
            db->addEvent(event);
        }

        UNIT_ASSERT_VALUES_EQUAL(rtCounter, 1);
        UNIT_ASSERT_VALUES_EQUAL(psCounter, 1);

        auto readEvent = db->getEarliestEvent({});
        UNIT_ASSERT_VALUES_EQUAL(readEvent->databaseMetricaEvent.new_event().name(), "testName");
        readEvent = db->getEarliestEvent({});
        UNIT_ASSERT_VALUES_EQUAL(readEvent->databaseMetricaEvent.new_event().name(), "testName"); // same
        db->deleteEvents({readEvent->id});
        readEvent = db->getEarliestEvent({});
        UNIT_ASSERT_VALUES_EQUAL(readEvent->databaseMetricaEvent.new_event().name(), "persistent_testName");
        db->deleteEvents({readEvent->id});
        UNIT_ASSERT(db->getEarliestEvent({}) == nullptr);
    }

    Y_UNIT_TEST_F(testMakePersistentByFilter, SqliteTestFixturePS) {
        size_t rtCounter = 0;
        size_t psCounter = 0;
        runtimeFilter = [&](uint64_t /*id*/, const auto& event, bool /*saved*/) {
            if (!std::string(event.new_event().name().c_str()).starts_with("persistent_")) {
                ++rtCounter;
                return true;
            }
            return false;
        };
        persistentFilter = [&](uint64_t /*id*/, const auto& event, bool /*saved*/) {
            if (std::string(event.new_event().name().c_str()).starts_with("persistent_")) {
                ++psCounter;
                return true;
            }
            return false;
        };

        {
            DatabaseMetricaEvent event;
            auto newEvent = event.mutable_new_event();
            newEvent->set_name("testName");
            db->addEvent(event);
        }

        {
            DatabaseMetricaEvent event;
            auto newEvent = event.mutable_new_event();
            newEvent->set_name("persistent_testName");
            db->addEvent(event);
        }

        {
            DatabaseMetricaEvent event;
            auto newEvent = event.mutable_new_event();
            newEvent->set_name("may_be_persistent_testName");
            db->addEvent(event);
        }

        UNIT_ASSERT_VALUES_EQUAL(rtCounter, 2);
        UNIT_ASSERT_VALUES_EQUAL(psCounter, 1);

        std::vector<uint64_t> skipIds;

        auto readEvent = db->getEarliestEvent(skipIds);
        UNIT_ASSERT_VALUES_EQUAL(readEvent->databaseMetricaEvent.new_event().name(), "testName");
        skipIds.push_back(readEvent->id);

        readEvent = db->getEarliestEvent(skipIds);
        UNIT_ASSERT_VALUES_EQUAL(readEvent->databaseMetricaEvent.new_event().name(), "persistent_testName");
        uint64_t eventId_2 = readEvent->id;
        skipIds.push_back(readEvent->id);

        readEvent = db->getEarliestEvent(skipIds);
        UNIT_ASSERT_VALUES_EQUAL(readEvent->databaseMetricaEvent.new_event().name(), "may_be_persistent_testName");
        uint64_t eventId_3 = readEvent->id;
        skipIds.push_back(readEvent->id);

        UNIT_ASSERT(db->getEarliestEvent(skipIds) == nullptr);

        auto res = db->makePersistent(
            [&](uint64_t eventId, const auto& event, bool /*saved*/) {
                UNIT_ASSERT_VALUES_UNEQUAL(eventId, eventId_2); // already persistent
                if (event.new_event().name() == "may_be_persistent_testName") {
                    UNIT_ASSERT_VALUES_EQUAL(readEvent->id, eventId_3);
                    return true;
                } else {
                    return false;
                }
            }, 0);
        UNIT_ASSERT_VALUES_EQUAL(res.eventCount, 1);
        UNIT_ASSERT_VALUES_EQUAL(res.eventMaxId, eventId_3);

        db.reset();
        std::remove(runtimeDbFilename.c_str());

        reinitDb();
        skipIds.clear();

        readEvent = db->getEarliestEvent(skipIds);
        UNIT_ASSERT_VALUES_EQUAL(readEvent->databaseMetricaEvent.new_event().name(), "persistent_testName");
        UNIT_ASSERT_VALUES_EQUAL(readEvent->id, eventId_2);
        skipIds.push_back(readEvent->id);

        readEvent = db->getEarliestEvent(skipIds);
        UNIT_ASSERT_VALUES_EQUAL(readEvent->databaseMetricaEvent.new_event().name(), "may_be_persistent_testName");
        UNIT_ASSERT_VALUES_EQUAL(readEvent->id, eventId_3);
        skipIds.push_back(readEvent->id);

        UNIT_ASSERT(db->getEarliestEvent(skipIds) == nullptr);
    }
}
