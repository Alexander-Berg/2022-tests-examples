#include <yandex/maps/wiki/common//retry_duration.h>
#include <maps/libs/common/include/exception.h>
#include <maps/libs/common/include/profiletimer.h>

#include <library/cpp/testing/unittest/registar.h>
#include <thread>

namespace maps::wiki::common::tests {

namespace {

RetryDurationPolicy getTestPolicy()
{
    return RetryDurationPolicy()
        .setSleepTime(std::chrono::milliseconds(300))
        .setDuration(std::chrono::seconds(1));
}

} // namespace

Y_UNIT_TEST_SUITE(retry_duration) {

Y_UNIT_TEST(test_invalid_policy)
{
    RetryDurationPolicy policy;
    UNIT_ASSERT_EXCEPTION(
        policy.setSleepTime(std::chrono::milliseconds(0)),
        maps::RuntimeError
    );
    UNIT_ASSERT_EXCEPTION(
        policy.setDuration(std::chrono::seconds(0)),
        maps::RuntimeError
    );
}

Y_UNIT_TEST(test_default_policy)
{
    RetryDurationPolicy policy;
    UNIT_ASSERT(policy.sleepTime().count() > 1000); //1000ms
    UNIT_ASSERT(policy.duration().count() > 60); //60s
}

Y_UNIT_TEST(test_init_policy)
{
    auto policy = getTestPolicy();
    UNIT_ASSERT_EQUAL(policy.sleepTime().count(), 300); //ms
    UNIT_ASSERT_EQUAL(policy.duration().count(), 1); //s
    UNIT_ASSERT_EQUAL(policy.maxTries(), 4);
}

Y_UNIT_TEST(test_init_custom_policy)
{
    auto policy = RetryDurationPolicy()
        .setSleepTime(std::chrono::milliseconds(250))
        .setDuration(std::chrono::seconds(1));

    UNIT_ASSERT_EQUAL(policy.sleepTime().count(), 250); //ms
    UNIT_ASSERT_EQUAL(policy.duration().count(), 1); //s
    UNIT_ASSERT_EQUAL(policy.maxTries(), 4); // (1000+249) / 250

    policy.setSleepTime(std::chrono::milliseconds(300));
    UNIT_ASSERT_EQUAL(policy.maxTries(), 4); // (1000+299) / 300

    policy.setSleepTime(std::chrono::milliseconds(333));
    UNIT_ASSERT_EQUAL(policy.maxTries(), 4); // (1000+332) / 333

    policy.setSleepTime(std::chrono::milliseconds(334));
    UNIT_ASSERT_EQUAL(policy.maxTries(), 3); // (1000+333) / 334
}

Y_UNIT_TEST(test_retry_duration_all_failed)
{
    auto policy = getTestPolicy();

    // 1,2,3,4 - fail
    // 1    2    3    4
    // 0.0  0.3  0.6  0.9
    int counter = 0;

    ProfileTimer timer;
    UNIT_ASSERT_EXCEPTION(
        wiki::common::retryDuration(
            [&counter] {
                ++counter;
                throw maps::LogicError();
            },
            policy
        ),
        maps::common::RetryNumberExceeded
    );

    double execTime = timer.getElapsedTimeNumber(); // ~ 0.9 sec
    UNIT_ASSERT(execTime > 0.8);
    UNIT_ASSERT(execTime < 1.5);

    UNIT_ASSERT_EQUAL(counter, 4);
}

Y_UNIT_TEST(test_retry_duration_expired)
{
    auto policy = getTestPolicy();

    // 1,2 - fail, 3 ok
    // 1    2    3
    // 0.0  0.6  1.2
    int counter = 0;

    ProfileTimer timer;
    UNIT_ASSERT_EXCEPTION(
        wiki::common::retryDuration(
            [&counter] {
                if (++counter <= 2) {
                    std::this_thread::sleep_for(std::chrono::milliseconds(300));
                    throw maps::LogicError();
                }
            },
            policy
        ),
        maps::wiki::common::RetryDurationExpired
    );

    double execTime = timer.getElapsedTimeNumber(); // ~ 1.2 sec
    UNIT_ASSERT(execTime > 1.0);
    UNIT_ASSERT(execTime < 2.0);

    UNIT_ASSERT_EQUAL(counter, 2);
}

Y_UNIT_TEST(test_retry_duration_ok)
{
    auto policy = getTestPolicy();

    // 1,2 - fail, 3 ok
    // 1    2    3
    // 0.0  0.3  0.6
    int counter = 0;

    ProfileTimer timer;
    UNIT_ASSERT_NO_EXCEPTION(
        wiki::common::retryDuration(
            [&counter] {
                if (++counter <= 2) {
                    throw maps::LogicError();
                }
            },
            policy
        )
    );

    double execTime = timer.getElapsedTimeNumber(); // ~ 0.6 sec
    UNIT_ASSERT(execTime > 0.5);
    UNIT_ASSERT(execTime < 1.5);

    UNIT_ASSERT_EQUAL(counter, 3);
}

} //Y_UNIT_TEST_SUITE

} // namespace maps::wiki::common::tests
