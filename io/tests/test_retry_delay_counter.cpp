#include <yandex_io/libs/base/retry_delay_counter.h>

#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

#include <chrono>

using namespace quasar;
using namespace std::chrono;

Y_UNIT_TEST_SUITE_F(TestRetryDelayCounter, QuasarUnitTestFixture) {
    Y_UNIT_TEST(testRetryDelayCounter)
    {
        RetryDelayCounter delayCounter;
        RetryDelayCounter::Settings settings;
        settings.init = std::chrono::seconds(1);
        settings.factor = 2;
        settings.offset = std::chrono::seconds(1);
        settings.max = std::chrono::seconds(10);
        delayCounter.setSettings(settings);
        UNIT_ASSERT_VALUES_EQUAL(duration_cast<std::chrono::seconds>(delayCounter.get()).count(), 1);
        delayCounter.increase();
        UNIT_ASSERT_VALUES_EQUAL(duration_cast<std::chrono::seconds>(delayCounter.get()).count(), 3);
        delayCounter.increase();
        UNIT_ASSERT_VALUES_EQUAL(duration_cast<std::chrono::seconds>(delayCounter.get()).count(), 7);
        delayCounter.increase();
        UNIT_ASSERT_VALUES_EQUAL(duration_cast<std::chrono::seconds>(delayCounter.get()).count(), 10);
        delayCounter.reset();
        UNIT_ASSERT_VALUES_EQUAL(duration_cast<std::chrono::seconds>(delayCounter.get()).count(), 1);
        delayCounter.increase();
        delayCounter.reset();
        UNIT_ASSERT_VALUES_EQUAL(duration_cast<std::chrono::seconds>(delayCounter.get()).count(), 1);
    }
}
