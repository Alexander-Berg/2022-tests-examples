#include <yandex_io/android_sdk/cpp/metrics/cold_start_detector/cold_start_detector.h>

#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>
#include <library/cpp/testing/unittest/registar.h>

#include <fstream>

using namespace quasar;

namespace {
    class Fixture: public QuasarUnitTestFixture {
    public:
        void SetUp(NUnitTest::TTestContext& context) override {
            QuasarUnitTestFixture::SetUp(context);

            auto& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard_);
            config["common"]["persistBootIdPath"] = "persisted-boot-id";
            config["common"]["systemBootIdPath"] = "system-boot-id";
        }

        static void writeToFile(const std::string& path, const std::string& data) {
            std::ofstream f(path.c_str(), std::ios_base::out | std::ios_base::trunc);
            if (!data.empty()) {
                f << data;
            }
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            std::remove("persisted-boot-id");
            std::remove("system-boot-id");

            QuasarUnitTestFixture::TearDown(context);
        }

        YandexIO::Configuration::TestGuard testGuard_;
    };
} // namespace

Y_UNIT_TEST_SUITE_F(TestColdStartDetector, Fixture) {
    Y_UNIT_TEST(testWithoutPersistedBootId) {
        writeToFile("system-boot-id", "boot.id");
        ColdStartDetector detector(getDeviceForTests());
        ASSERT_TRUE(detector.checkIfColdStart());
        ASSERT_FALSE(detector.checkIfColdStart()); // second call should always return false
    }

    Y_UNIT_TEST(testWithSamePersistedBootId) {
        writeToFile("persisted-boot-id", "boot.id");
        writeToFile("system-boot-id", "boot.id");
        ColdStartDetector detector(getDeviceForTests());
        ASSERT_FALSE(detector.checkIfColdStart());
        ASSERT_FALSE(detector.checkIfColdStart()); // second call should always return false
    }

    Y_UNIT_TEST(testWithOtherPersistedBootId) {
        writeToFile("persisted-boot-id", "boot.id");
        writeToFile("system-boot-id", "other.boot.id");
        ColdStartDetector detector(getDeviceForTests());
        ASSERT_TRUE(detector.checkIfColdStart());
        ASSERT_FALSE(detector.checkIfColdStart()); // second run should always return false
    }

    Y_UNIT_TEST(testWithApplicationRestart) {
        writeToFile("persisted-boot-id", "boot.id");
        writeToFile("system-boot-id", "other.boot.id");

        {
            ColdStartDetector detector(getDeviceForTests());
            ASSERT_TRUE(detector.checkIfColdStart());
        }

        /* application restarts */

        {
            ColdStartDetector detector(getDeviceForTests());
            ASSERT_FALSE(detector.checkIfColdStart());
        }
    }
}
