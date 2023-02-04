#include <yandex_io/services/fluent-bitd/fluent_bit_client.h>

#include <yandex_io/libs/base/persistent_file.h>
#include <yandex_io/libs/device/device.h>
#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/libs/sqlite/sqlite_database.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <fstream>

using namespace quasar;
using namespace TestUtils;

namespace {

    class FluentBitTestClient: public FluentBitClient {
        using FluentBitClient::FluentBitClient;

    public:
        Json::Value getMetrics(const std::string& name) {
            std::lock_guard lock(metricsMutex);
            return metrics[name];
        }

    protected:
        void reportMetrics(const std::string& name, const Json::Value& value) override {
            std::lock_guard lock(metricsMutex);
            metrics[name] = value;
        }

    private:
        std::map<std::string, Json::Value> metrics;
        std::mutex metricsMutex;
    };

    class MockDatabase: public SqliteDatabase {
    public:
        MockDatabase(std::string databasePath)
            : SqliteDatabase(databasePath)
        {
        }

        void createScheme() {
            runQueryWithoutCallback("CREATE TABLE STRINGS(ID INTEGER PRIMARY KEY AUTOINCREMENT, DATA TEXT)");
        }

        void putString(const std::string& s) {
            const auto query = "INSERT INTO STRINGS(DATA) VALUES('" + s + "')";
            char* errmsg = nullptr;
            int resCode = sqlite3_exec(db_, query.c_str(), nullptr, nullptr, &errmsg);
            UNIT_ASSERT_C(resCode == SQLITE_OK, errmsg);
            sqlite3_free(errmsg);
        }
    };

    class Fixture: public QuasarUnitTestFixture {
    public:
        YandexIO::Configuration::TestGuard testGuard_;

        std::unique_ptr<FluentBitTestClient> testFluentBitClient_;

        Json::Value config;

        std::string deviceCfgFileName_;
        std::string variablesCfgFileName_;
        std::string setModeScriptFileName_;
        std::string killScriptFileName_;
        std::string tailDbBackupFileName_;
        std::string tailDbFile_;
        std::string setModeCheckFile_;
        std::string killCheckFile_;

        const bool defaultConfigEnabledValue = false;
        const std::string defaultConfigFlushIntervalValue = "5";

        using Base = QuasarUnitTestFixture;

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);

            const std::string basePath = tryGetRamDrivePath();

            deviceCfgFileName_ = basePath + "/device.cfg";
            variablesCfgFileName_ = basePath + "/variables.cfg";
            setModeScriptFileName_ = basePath + "/set_mode.sh";
            killScriptFileName_ = basePath + "/kill.sh";
            tailDbBackupFileName_ = basePath + "/tail_backup.db";
            tailDbFile_ = basePath + "/tail.db";
            setModeCheckFile_ = basePath + "/mode.out";
            killCheckFile_ = basePath + "/kill.out";

            config["fluent-bit"]["enabled"] = defaultConfigEnabledValue;
            config["fluent-bit"]["variables"]["flushInterval"] = defaultConfigFlushIntervalValue;
            config["fluent-bit"]["variables"]["httpServer"] = "On";
            config["fluent-bit"]["variables"]["httpServerPort"] = std::getenv("FB_SERVER_PORT");
            config["fluent-bit"]["variables"]["tailDbFile"] = tailDbFile_;

            createMockScripts();
            config["deviceCfgFileName"] = deviceCfgFileName_;
            config["variablesCfgFileName"] = variablesCfgFileName_;
            config["setModeScriptFileName"] = setModeScriptFileName_;
            config["killScriptFileName"] = killScriptFileName_;
            config["tailDbBackupFileName"] = tailDbBackupFileName_;

            config["collectMetricsPeriodMs"] = 500;

            testFluentBitClient_ = std::make_unique<FluentBitTestClient>(getDeviceForTests(), config);
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            std::remove(deviceCfgFileName_.c_str());
            std::remove(variablesCfgFileName_.c_str());
            std::remove(setModeScriptFileName_.c_str());
            std::remove(killScriptFileName_.c_str());
            std::remove(tailDbBackupFileName_.c_str());
            std::remove(tailDbFile_.c_str());
            std::remove(setModeCheckFile_.c_str());
            std::remove(killCheckFile_.c_str());
            YIO_LOG_DEBUG("Cleanup done");

            Base::TearDown(context);
        }

        void createMockScripts() const {
            {
                AtomicFile setModeScript(setModeScriptFileName_);
                std::stringstream setMode;
                setMode << "echo \"$1\" > " << setModeCheckFile_ << std::endl;
                setModeScript.write(setMode.str());
            }

            std::stringstream setModeChmod;
            setModeChmod << "chmod +x " << setModeScriptFileName_;
            std::system(setModeChmod.str().c_str());

            {
                AtomicFile killScript(killScriptFileName_);
                std::stringstream kill;
                kill << "echo 'killed' > " << killCheckFile_ << std::endl;
                killScript.write(kill.str());
            }

            std::stringstream killChmod;
            killChmod << "chmod +x " << killScriptFileName_;
            std::system(killChmod.str().c_str());
        }

        void resetKillCheck() const {
            std::stringstream ss;
            ss << "echo '' > " << killCheckFile_ << std::endl;
            std::system(ss.str().c_str());
        }

        bool checkEnabled(bool enabled) const {
            std::ifstream checkFile(setModeCheckFile_);
            std::string content;
            checkFile >> content;

            bool isEnabled = false;
            if (content == "enabled") {
                isEnabled = true;
            } else if (content == "disabled") {
                isEnabled = false;
            } else {
                std::stringstream ss;
                ss << "Unexpected content in set_mode check file: " << content;
                UNIT_FAIL(ss.str());
            }

            return enabled == isEnabled;
        }

        bool checkKill() const {
            std::ifstream checkFile(killCheckFile_);
            std::string content;
            checkFile >> content;

            if (content == "killed") {
                return true;
            } else if (content.empty()) {
                return false;
            } else {
                std::stringstream ss;
                ss << "Unexpected content in kill script check file: " << content;
                UNIT_FAIL(ss.str());
                return false;
            }
        }
    };

} /* anonymous namespace */

Y_UNIT_TEST_SUITE_F(FluentBitClientTests, Fixture) {
    Y_UNIT_TEST(testFluentBitClientInit) {
        testFluentBitClient_->init();
        UNIT_ASSERT(checkKill());

        auto device_id = getDeviceForTests() -> deviceId();
        auto platform = getDeviceForTests() -> configuration() -> getDeviceType();
        auto commonConfig = getDeviceForTests() -> configuration() -> getServiceConfig("common");
        auto crtFilePath = getString(commonConfig, "caCertsFile");

        std::ifstream deviceCfg(deviceCfgFileName_);
        std::string line;

        UNIT_ASSERT(std::getline(deviceCfg, line));
        UNIT_ASSERT(line.ends_with(device_id));

        UNIT_ASSERT(std::getline(deviceCfg, line));
        UNIT_ASSERT(line.ends_with(platform));

        UNIT_ASSERT(std::getline(deviceCfg, line));
        UNIT_ASSERT(line.ends_with(crtFilePath));

        std::ifstream variablesCfg(variablesCfgFileName_);
        UNIT_ASSERT(std::getline(variablesCfg, line));
        std::stringstream ss;
        ss << "@SET flushInterval=" << defaultConfigFlushIntervalValue;
        UNIT_ASSERT_VALUES_EQUAL(line, ss.str());
    }

    Y_UNIT_TEST(testFluentBitClientVariableChange) {
        Json::Value fluentBitConfig;

        resetKillCheck();
        fluentBitConfig["variables"]["flushInterval"] = defaultConfigFlushIntervalValue;
        testFluentBitClient_->processNewConfig(fluentBitConfig);
        UNIT_ASSERT_C(!checkKill(), "No changes -> no restart");

        resetKillCheck();
        fluentBitConfig["variables"]["flushInterval"] = defaultConfigFlushIntervalValue + "0";
        testFluentBitClient_->processNewConfig(fluentBitConfig);
        UNIT_ASSERT_C(checkKill(), "Changes -> restart");
    }

    Y_UNIT_TEST(testFluentBitClientEnabling) {
        testFluentBitClient_->init();
        UNIT_ASSERT(checkEnabled(false));

        resetKillCheck();
        Json::Value fluentBitConfig;
        fluentBitConfig["enabled"] = true;
        testFluentBitClient_->processNewConfig(fluentBitConfig);
        UNIT_ASSERT(checkKill());
        UNIT_ASSERT(checkEnabled(true));
    }

    Y_UNIT_TEST(testFluentBitMetricsCollect) {
        testFluentBitClient_->init();
        testFluentBitClient_->processNewConfig(parseJson("{ \"enabled\": true }"));

        TestUtils::waitUntil([&]() {
            // wait for metrics to be collected
            auto metrics = testFluentBitClient_->getMetrics("fluentBitMetrics");
            return !metrics.isNull() && metrics["output"]["stdout.0"]["proc_records"].asUInt64() > 0;
        });

        auto uptime = testFluentBitClient_->getMetrics("fluentBitUptime");
        UNIT_ASSERT(uptime["uptime_sec"].isNumeric());
        UNIT_ASSERT_GE(uptime["uptime_sec"].asUInt64(), 0);

        auto metrics = testFluentBitClient_->getMetrics("fluentBitMetrics");
        // check that all rows from fluent_bit/data/test.log are processed
        UNIT_ASSERT(metrics["output"]["stdout.0"]["proc_records"].isNumeric());
        UNIT_ASSERT_VALUES_EQUAL(metrics["output"]["stdout.0"]["proc_records"].asUInt64(), 3);
    }

    Y_UNIT_TEST(testFluentBitMetricsUnavailable) {
        testFluentBitClient_->init();
        // enable but set fake port
        testFluentBitClient_->processNewConfig(parseJson("{ \"enabled\": true, \"variables\": {\"httpServerPort\": \"80\"}}"));

        // check that FluentBitClient will not crash
        std::this_thread::sleep_for(std::chrono::seconds(5));
        // check that we didn't got any metrics
        UNIT_ASSERT(testFluentBitClient_->getMetrics("fluentBitUptime").isNull());
    }

    Y_UNIT_TEST(testFluentBitMetricsServerDisable) {
        testFluentBitClient_->init();
        testFluentBitClient_->processNewConfig(parseJson("{ \"enabled\": true, \"variables\": {\"httpServer\": \"Off\"}}"));

        // check that we didn't got any metrics
        std::this_thread::sleep_for(std::chrono::seconds(5));
        UNIT_ASSERT(testFluentBitClient_->getMetrics("fluentBitUptime").isNull());
    }

    Y_UNIT_TEST(testFluentBitDatabaseBackupCreate) {
        {
            // create database
            MockDatabase db(tailDbFile_);
            db.createScheme();
        }

        testFluentBitClient_->teardown();

        // check backup is created
        UNIT_ASSERT(fileExists(tailDbBackupFileName_));

        {
            // check database is ok
            MockDatabase db(tailDbBackupFileName_);
            db.putString("asd");
        }
    }

    Y_UNIT_TEST(testFluentBitDatabaseBackupLoad) {
        {
            // create database
            MockDatabase db(tailDbBackupFileName_);
            db.createScheme();
        }

        testFluentBitClient_->init();

        // check backup is moved
        UNIT_ASSERT(fileExists(tailDbFile_));
        UNIT_ASSERT(!fileExists(tailDbBackupFileName_));

        {
            // check database is ok
            MockDatabase db(tailDbFile_);
            db.putString("asd");
        }
    }

    Y_UNIT_TEST(testFluentBitDatabaseBadBackup) {
        {
            // create bad file
            AtomicFile backup(tailDbBackupFileName_);
            backup.write("not a database");
        }

        testFluentBitClient_->init();

        // check nothing loaded and bad backup is removed
        UNIT_ASSERT(!fileExists(tailDbFile_));
        UNIT_ASSERT(!fileExists(tailDbBackupFileName_));
    }
}
