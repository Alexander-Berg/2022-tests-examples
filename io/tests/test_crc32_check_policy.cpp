#include <yandex_io/services/updatesd/crc32_check_policy.h>

#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace quasar;

namespace {
    class Fixture: public QuasarUnitTestFixture {
    public:
        const std::string path_ = "updates-counter.json";

        void SetUp(NUnitTest::TTestContext& context) override {
            QuasarUnitTestFixture::SetUp(context);
            TFsPath(path_).ForceDelete();
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            QuasarUnitTestFixture::TearDown(context);
            TFsPath(path_).ForceDelete();
        }
    };
} /* anonymous namespace */

Y_UNIT_TEST_SUITE(TestCrc32CheckPolicy) {
    Y_UNIT_TEST_F(EmptyUpdatesdConfig, Fixture) {
        const std::string toVersion = "1.91.1.8.1134702328.20211122.54";
        Json::Value updatesdDeviceConfig;
        Crc32CheckPolicy policy(updatesdDeviceConfig);
        UNIT_ASSERT_VALUES_EQUAL(false, policy.checkAfterWriteEnabled());
        UNIT_ASSERT_VALUES_EQUAL(true, policy.updateIsAllowed(toVersion));

        Json::Value systemConfig;
        policy.applySystemConfig(systemConfig);
        UNIT_ASSERT_VALUES_EQUAL(false, policy.checkAfterWriteEnabled());
        UNIT_ASSERT_VALUES_EQUAL(true, policy.updateIsAllowed(toVersion));

        systemConfig["checkCrc32AfterWrite"] = true;
        policy.applySystemConfig(systemConfig);
        UNIT_ASSERT_VALUES_EQUAL(false, policy.checkAfterWriteEnabled());
        UNIT_ASSERT_VALUES_EQUAL(true, policy.updateIsAllowed(toVersion));

        systemConfig["checkCrc32AfterWrite"] = false;
        policy.applySystemConfig(systemConfig);
        UNIT_ASSERT_VALUES_EQUAL(false, policy.checkAfterWriteEnabled());
        UNIT_ASSERT_VALUES_EQUAL(true, policy.updateIsAllowed(toVersion));
    }

    Y_UNIT_TEST_F(CheckIsEnabledInDeviceConfig, Fixture) {
        const std::string toVersion = "1.91.1.8.1134702328.20211122.54";
        Json::Value updatesdDeviceConfig;
        updatesdDeviceConfig["checkCrc32AfterWrite"] = true;
        Crc32CheckPolicy policy(updatesdDeviceConfig);
        UNIT_ASSERT_VALUES_EQUAL(true, policy.checkAfterWriteEnabled());
        UNIT_ASSERT_VALUES_EQUAL(true, policy.updateIsAllowed(toVersion));

        Json::Value systemConfig;
        policy.applySystemConfig(systemConfig);
        UNIT_ASSERT_VALUES_EQUAL(true, policy.checkAfterWriteEnabled());
        UNIT_ASSERT_VALUES_EQUAL(true, policy.updateIsAllowed(toVersion));

        systemConfig["checkCrc32AfterWrite"] = true;
        policy.applySystemConfig(systemConfig);
        UNIT_ASSERT_VALUES_EQUAL(true, policy.checkAfterWriteEnabled());
        UNIT_ASSERT_VALUES_EQUAL(true, policy.updateIsAllowed(toVersion));

        systemConfig["checkCrc32AfterWrite"] = false;
        policy.applySystemConfig(systemConfig);
        UNIT_ASSERT_VALUES_EQUAL(false, policy.checkAfterWriteEnabled());
        UNIT_ASSERT_VALUES_EQUAL(true, policy.updateIsAllowed(toVersion));
    }

    Y_UNIT_TEST_F(CheckIsDisabledInDeviceConfig, Fixture) {
        const std::string toVersion = "1.91.1.8.1134702328.20211122.54";
        Json::Value updatesdDeviceConfig;
        updatesdDeviceConfig["checkCrc32AfterWrite"] = false;
        Crc32CheckPolicy policy(updatesdDeviceConfig);
        UNIT_ASSERT_VALUES_EQUAL(false, policy.checkAfterWriteEnabled());
        UNIT_ASSERT_VALUES_EQUAL(true, policy.updateIsAllowed(toVersion));

        Json::Value systemConfig;
        policy.applySystemConfig(systemConfig);
        UNIT_ASSERT_VALUES_EQUAL(false, policy.checkAfterWriteEnabled());
        UNIT_ASSERT_VALUES_EQUAL(true, policy.updateIsAllowed(toVersion));

        systemConfig["checkCrc32AfterWrite"] = true;
        policy.applySystemConfig(systemConfig);
        UNIT_ASSERT_VALUES_EQUAL(false, policy.checkAfterWriteEnabled());
        UNIT_ASSERT_VALUES_EQUAL(true, policy.updateIsAllowed(toVersion));

        systemConfig["checkCrc32AfterWrite"] = false;
        policy.applySystemConfig(systemConfig);
        UNIT_ASSERT_VALUES_EQUAL(false, policy.checkAfterWriteEnabled());
        UNIT_ASSERT_VALUES_EQUAL(true, policy.updateIsAllowed(toVersion));
    }

    Y_UNIT_TEST_F(UpdateIsDisallowedAfterSeveralAttemptsToSameVersion, Fixture) {
        const std::string toVersion = "1.91.1.8.1134702328.20211122.54";
        Json::Value updatesdDeviceConfig;
        Crc32CheckPolicy policy(true, path_, 2);

        UNIT_ASSERT_VALUES_EQUAL(true, policy.updateIsAllowed(toVersion));

        policy.registerFail(toVersion);
        UNIT_ASSERT_VALUES_EQUAL(true, policy.updateIsAllowed(toVersion));

        policy.registerFail(toVersion);
        UNIT_ASSERT_VALUES_EQUAL(false, policy.updateIsAllowed(toVersion));
    }

    Y_UNIT_TEST_F(CounterResetsAfterSuccessfulAttempt, Fixture) {
        const std::string toVersion = "1.91.1.8.1134702328.20211122.54";
        Json::Value updatesdDeviceConfig;
        Crc32CheckPolicy policy(true, path_, 2);

        UNIT_ASSERT_VALUES_EQUAL(true, policy.updateIsAllowed(toVersion));

        policy.registerFail(toVersion);
        UNIT_ASSERT_VALUES_EQUAL(true, policy.updateIsAllowed(toVersion));

        policy.registerSuccess(toVersion); /* Counter should reset here */

        /* Now we have 2 attempts again */
        UNIT_ASSERT_VALUES_EQUAL(true, policy.updateIsAllowed(toVersion));

        policy.registerFail(toVersion);
        UNIT_ASSERT_VALUES_EQUAL(true, policy.updateIsAllowed(toVersion));

        policy.registerFail(toVersion);
        /* No attempts left */
        UNIT_ASSERT_VALUES_EQUAL(false, policy.updateIsAllowed(toVersion));
    }

    Y_UNIT_TEST_F(CounterDiscardsOnVersionChange, Fixture) {
        const std::string toVersion = "1.91.1.8.1134702328.20211122.54";
        const std::string toVersion2 = "1.92.1.5.453455435.20211125.11";

        Crc32CheckPolicy policy(true, path_, 2);

        UNIT_ASSERT_VALUES_EQUAL(true, policy.updateIsAllowed(toVersion));

        policy.registerFail(toVersion);
        UNIT_ASSERT_VALUES_EQUAL(true, policy.updateIsAllowed(toVersion));

        policy.registerFail(toVersion);
        /* No attempts left */
        UNIT_ASSERT_VALUES_EQUAL(false, policy.updateIsAllowed(toVersion));

        /* But we can still try to update to other version */
        UNIT_ASSERT_VALUES_EQUAL(true, policy.updateIsAllowed(toVersion2));

        policy.registerFail(toVersion2); /* Counter should reset to 1 here */

        /* Now we have only 1 attempt left to update to toVersion2 (1  attempt is already used) */
        UNIT_ASSERT_VALUES_EQUAL(true, policy.updateIsAllowed(toVersion2));

        policy.registerFail(toVersion2);
        /* No attempts left */
        UNIT_ASSERT_VALUES_EQUAL(false, policy.updateIsAllowed(toVersion2));
    }

    Y_UNIT_TEST_F(CounterIsPersisted, Fixture) {
        const std::string toVersion = "1.91.1.8.1134702328.20211122.54";

        {
            Crc32CheckPolicy policy(true, path_, 2);
            policy.registerFail(toVersion);
        }

        {
            Crc32CheckPolicy policy(true, path_, 2);
            UNIT_ASSERT_VALUES_EQUAL(true, policy.updateIsAllowed(toVersion));
            policy.registerFail(toVersion);
            UNIT_ASSERT_VALUES_EQUAL(false, policy.updateIsAllowed(toVersion));
        }

        {
            Crc32CheckPolicy policy(true, path_, 2);
            UNIT_ASSERT_VALUES_EQUAL(false, policy.updateIsAllowed(toVersion));
        }
    }

    Y_UNIT_TEST_F(UpdateCanBeUnblockedBySystemConfig, Fixture) {
        const std::string toVersion = "1.91.1.8.1134702328.20211122.54";
        Crc32CheckPolicy policy(true, path_, 3);
        policy.registerFail(toVersion);
        policy.registerFail(toVersion);
        policy.registerFail(toVersion);
        UNIT_ASSERT_VALUES_EQUAL(false, policy.updateIsAllowed(toVersion));

        Json::Value systemConfig;
        systemConfig["checkCrc32AfterWrite"] = false;
        policy.applySystemConfig(systemConfig);

        UNIT_ASSERT_VALUES_EQUAL(true, policy.updateIsAllowed(toVersion));
    }
}
