#include <maps/wikimap/mapspro/libs/assessment/include/rate_limiter.h>
#include <maps/wikimap/mapspro/libs/unittest/include/yandex/maps/wiki/unittest/unittest.h>

#include "helpers.h"

#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::assessment::tests::ratelimiter {

class TestRateLimiter : public RateLimiter {
public:
    TestRateLimiter() = default;

    void setTotalAcceptableActivity(social::UserActivity acceptableActivity)
    {
        totalLimits_.acceptableActivity = std::move(acceptableActivity);
        updateTimeIntervals(totalLimits_);
    }

    void setAcceptableActivity(Entity::Domain entityDomain, social::UserActivity acceptableActivity)
    {
        auto& limits = entityDomainLimits_[entityDomain];
        limits.acceptableActivity = std::move(acceptableActivity);
        updateTimeIntervals(limits);
    }

private:
    static void updateTimeIntervals(Limits& limits)
    {
        limits.timeIntervals.clear();
        for (const auto& [interval, _] : limits.acceptableActivity) {
            limits.timeIntervals.push_back(interval);
        }
    }
};

Y_UNIT_TEST_SUITE_F(assessment_ratelimiter_tests, DbFixture) {
    Y_UNIT_TEST(should_not_find_interval_for_empty_ratelimiter) {
        constexpr TUid uid = 1;

        TestRateLimiter rateLimiter;
        pqxx::work txn(conn);

        UNIT_ASSERT(!rateLimiter.checkTotalLimitExceeded(txn, uid));
        for (auto entityDomain : enum_io::enumerateValues<Entity::Domain>()) {
            UNIT_ASSERT(!rateLimiter.checkLimitExceeded(txn, uid, entityDomain));
        }
    }

    Y_UNIT_TEST(should_find_interval_for_zero_limit) {
        constexpr TUid uid = 1;
        constexpr size_t ZERO_LIMIT = 0;
        const social::UserActivity h1zero{ {std::chrono::hours(1), ZERO_LIMIT} };

        TestRateLimiter rateLimiter;
        rateLimiter.setTotalAcceptableActivity(h1zero);
        for (auto entityDomain : enum_io::enumerateValues<Entity::Domain>()) {
            rateLimiter.setAcceptableActivity(entityDomain, h1zero);
        }

        pqxx::work txn(conn);

        auto checkIntervalH1 = [&](const std::optional<std::chrono::seconds>& interval) {
            UNIT_ASSERT(interval);
            UNIT_ASSERT_EQUAL(interval->count(), 3600);
        };

        checkIntervalH1(rateLimiter.checkTotalLimitExceeded(txn, uid));
        for (auto entityDomain : enum_io::enumerateValues<Entity::Domain>()) {
            checkIntervalH1(rateLimiter.checkLimitExceeded(txn, uid, entityDomain));
        }
    }

    Y_UNIT_TEST(should_not_find_interval_for_one_limit) {
        constexpr TUid uid = 1;
        constexpr size_t LIMIT = 1;
        const social::UserActivity h1zero{ {std::chrono::hours(1), LIMIT} };

        TestRateLimiter rateLimiter;
        rateLimiter.setTotalAcceptableActivity(h1zero);
        for (auto entityDomain : enum_io::enumerateValues<Entity::Domain>()) {
            rateLimiter.setAcceptableActivity(entityDomain, h1zero);
        }

        pqxx::work txn(conn);

        UNIT_ASSERT(!rateLimiter.checkTotalLimitExceeded(txn, uid));
        for (auto entityDomain : enum_io::enumerateValues<Entity::Domain>()) {
            UNIT_ASSERT(!rateLimiter.checkLimitExceeded(txn, uid, entityDomain));
        }
    }
}

} // namespace maps::wiki::assessment::tests::ratelimiter
