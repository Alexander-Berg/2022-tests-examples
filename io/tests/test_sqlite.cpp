#include <yandex_io/libs/sqlite/sqlite_database.h>

#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/device/device.h>

#include <yandex_io/protos/model_objects.pb.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

#include <iostream>
#include <memory>
#include <numeric>
#include <thread>

#include <sys/stat.h>

using quasar::SqliteDatabase;

namespace {
    class MockDatabase: public SqliteDatabase {
        using SqliteDatabase::SqliteDatabase;

    public:
        void createScheme() {
            runQueryWithoutCallback("CREATE TABLE STRINGS(ID INTEGER PRIMARY KEY AUTOINCREMENT, DATA TEXT)");
            sqlite3_busy_timeout(db_, 5000);
        }

        void putString(const std::string& s) {
            const auto query = "INSERT INTO STRINGS(DATA) VALUES('" + s + "')";
            char* errmsg = nullptr;
            int resCode = sqlite3_exec(db_, query.c_str(), nullptr, nullptr, &errmsg);
            UNIT_ASSERT_C(resCode == SQLITE_OK, errmsg);
            sqlite3_free(errmsg);
        }
    };

    class SqliteTestFixture: public QuasarUnitTestFixture {
    public:
        SqliteTestFixture() {
            dbFilename = tryGetRamDrivePath() + "/test.db";
            db = std::make_unique<MockDatabase>(dbFilename, dbMaxSize);
        }

        ~SqliteTestFixture() {
            std::remove(dbFilename.c_str());
        }

        std::string dbFilename;
        const int dbMaxSize = 100;
        YandexIO::Configuration::TestGuard testGuard;
        std::unique_ptr<MockDatabase> db;
    };
} // namespace

Y_UNIT_TEST_SUITE(TestSqliteDataBase) {
    Y_UNIT_TEST_F(testSqliteBackup, SqliteTestFixture) {
        db->createScheme();
        db->putString("aaa");

        // create load on database
        std::atomic_bool insertThreadStopped = false;
        std::thread insertThread([&]() {
            while (!insertThreadStopped) {
                db->putString("aaa");
                std::this_thread::sleep_for(std::chrono::milliseconds(50));
            }
        });

        const auto backupPath = tryGetRamDrivePath() + "/backup.db";
        {
            SqliteDatabase database(dbFilename, dbMaxSize);
            database.backupToFile(backupPath);
        }

        insertThreadStopped = true;
        insertThread.join();

        {
            UNIT_ASSERT(quasar::fileExists(backupPath));
            // ensure backup is ok
            MockDatabase backupDb(backupPath, dbMaxSize);
            backupDb.checkIntegrity();

            backupDb.putString("asd");
        }
    }
}
