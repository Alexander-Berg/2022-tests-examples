#include <yandex_io/libs/rate_limiter/bucket/bucket_rate_limiter.h>

#include <yandex_io/tests/testlib/test_utils.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace quasar;
using namespace quasar::TestUtils;

class TestBucketRateLimiter: public BucketRateLimiter {
public:
    TestBucketRateLimiter(uint32_t bucketsCount,
                          uint32_t limit,
                          std::unordered_set<std::string> filteredEvents)
        : BucketRateLimiter(bucketsCount, limit, filteredEvents)
    {
    }

    void manuallyRotateBuckets() {
        rotateBuckets();
    }
};

Y_UNIT_TEST_SUITE(BucketRateLimiterTests) {
    Y_UNIT_TEST(testBucketRateLimiter) {
        TestBucketRateLimiter rateLimiter(3, 3, {"event1"});

        UNIT_ASSERT(rateLimiter.addEvent("event1") == BucketRateLimiter::OverflowStatus::NOT_OVERFLOWED);
        UNIT_ASSERT(rateLimiter.addEvent("event1") == BucketRateLimiter::OverflowStatus::NOT_OVERFLOWED);
        UNIT_ASSERT(rateLimiter.addEvent("event1") == BucketRateLimiter::OverflowStatus::NOT_OVERFLOWED);
        UNIT_ASSERT(rateLimiter.addEvent("event1") == BucketRateLimiter::OverflowStatus::OVERFLOWED);
    }

    Y_UNIT_TEST(testRotationBucketRateLimiter) {
        TestBucketRateLimiter rateLimiter(3, 3, {"event1"});

        UNIT_ASSERT(rateLimiter.addEvent("event1") == BucketRateLimiter::OverflowStatus::NOT_OVERFLOWED);
        UNIT_ASSERT(rateLimiter.addEvent("event1") == BucketRateLimiter::OverflowStatus::NOT_OVERFLOWED);
        // Events in buckets: 0, 0, 2
        rateLimiter.manuallyRotateBuckets();
        UNIT_ASSERT(rateLimiter.addEvent("event1") == BucketRateLimiter::OverflowStatus::NOT_OVERFLOWED);
        // Events in buckets: 0, 2, 1
        rateLimiter.manuallyRotateBuckets();
        UNIT_ASSERT(rateLimiter.addEvent("event1") == BucketRateLimiter::OverflowStatus::OVERFLOWED);
        // Events in buckets: 2, 1, 1
        rateLimiter.manuallyRotateBuckets();
        UNIT_ASSERT(rateLimiter.addEvent("event1") == BucketRateLimiter::OverflowStatus::NOT_OVERFLOWED);
        // Events in buckets: 1, 1, 1
        UNIT_ASSERT(rateLimiter.addEvent("event1") == BucketRateLimiter::OverflowStatus::OVERFLOWED);
        // Events in buckets: 1, 1, 2
    }

    Y_UNIT_TEST(testBucketRateLimiterSeveralEvents) {
        TestBucketRateLimiter rateLimiter(3, 3, {"event1", "event2"});

        UNIT_ASSERT(rateLimiter.addEvent("event1") == BucketRateLimiter::OverflowStatus::NOT_OVERFLOWED);
        UNIT_ASSERT(rateLimiter.addEvent("event2") == BucketRateLimiter::OverflowStatus::NOT_OVERFLOWED);
        UNIT_ASSERT(rateLimiter.addEvent("event1") == BucketRateLimiter::OverflowStatus::NOT_OVERFLOWED);
        UNIT_ASSERT(rateLimiter.addEvent("event2") == BucketRateLimiter::OverflowStatus::NOT_OVERFLOWED);
        rateLimiter.manuallyRotateBuckets();
        UNIT_ASSERT(rateLimiter.addEvent("event1") == BucketRateLimiter::OverflowStatus::NOT_OVERFLOWED);
        UNIT_ASSERT(rateLimiter.addEvent("event2") == BucketRateLimiter::OverflowStatus::NOT_OVERFLOWED);
        rateLimiter.manuallyRotateBuckets();
        UNIT_ASSERT(rateLimiter.addEvent("event1") == BucketRateLimiter::OverflowStatus::OVERFLOWED);
        UNIT_ASSERT(rateLimiter.addEvent("event2") == BucketRateLimiter::OverflowStatus::OVERFLOWED);

        for (int i = 0; i < 100; ++i) {
            UNIT_ASSERT(rateLimiter.addEvent("not_monitored_event") == BucketRateLimiter::OverflowStatus::NOT_OVERFLOWED);
        }
    }

    Y_UNIT_TEST(testBucketRateLimiterCallbacksOnRotating) {
        EventChecker<TestBucketRateLimiter::OverflowInfo> checker({{.event = "event1", .limit = 3, .eventCount = 4}, {.event = "event1", .limit = 3, .eventCount = 6}, {.event = "event1", .limit = 3, .eventCount = 4}});

        TestBucketRateLimiter rateLimiter(3, 3, {"event1"});
        rateLimiter.setOverflowOnBucketRotatingCb([&](const TestBucketRateLimiter::OverflowInfo& overflowInfo) {
            UNIT_ASSERT(checker.addEvent(overflowInfo));
        });

        UNIT_ASSERT(rateLimiter.addEvent("event1") == BucketRateLimiter::OverflowStatus::NOT_OVERFLOWED);
        UNIT_ASSERT(rateLimiter.addEvent("event1") == BucketRateLimiter::OverflowStatus::NOT_OVERFLOWED);
        UNIT_ASSERT(rateLimiter.addEvent("event1") == BucketRateLimiter::OverflowStatus::NOT_OVERFLOWED);
        rateLimiter.manuallyRotateBuckets();
        UNIT_ASSERT(rateLimiter.addEvent("event1") == BucketRateLimiter::OverflowStatus::OVERFLOWED);
        rateLimiter.manuallyRotateBuckets();
        UNIT_ASSERT(rateLimiter.addEvent("event1") == BucketRateLimiter::OverflowStatus::OVERFLOWED);
        UNIT_ASSERT(rateLimiter.addEvent("event1") == BucketRateLimiter::OverflowStatus::OVERFLOWED);
        rateLimiter.manuallyRotateBuckets();
        UNIT_ASSERT(rateLimiter.addEvent("event1") == BucketRateLimiter::OverflowStatus::OVERFLOWED);
        rateLimiter.manuallyRotateBuckets();

        checker.waitAllEvents();
    }
}
