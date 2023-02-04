#include <maps/infra/ratelimiter2/common/include/test_helpers.h>
#include <maps/infra/ratelimiter2/common/include/nova_access_limiter.h>

#include <boost/uuid/uuid_io.hpp>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::rate_limiter2::tests {

using std::chrono::system_clock;
using std::chrono::seconds;

Y_UNIT_TEST_SUITE(nova_access_limiter_test) {

Y_UNIT_TEST(rate_access)
{
    NovaAccessLimiter limiter{"Z"};

    // We're at 10sec from the beginning of the epoch
    system_clock::time_point now{std::chrono::seconds(10)};

    auto client1 = uuid("11111111-1111-1111-1111-111111111111");
    auto rps10 = Limit{.rate = 10, .unit = 1, .gen=1};
    auto rpm60 = Limit{.rate = 60, .unit = 1, .gen=2};

    // allowed: burst=5, weight=1
    EXPECT_TRUE(limiter.rateAccess(client1, "resA", {rps10, 5}, 1, now)); // +1
    // rejected (burst=5, weight=5)
    EXPECT_FALSE(limiter.rateAccess(client1, "resA", {rps10, 5}, 5, now));
    // allowed (burst=5, weight=4), uses rest of the budget
    EXPECT_TRUE(limiter.rateAccess(client1, "resA", {rps10, 5}, 4, now)); // +4
    // rejected (burst=5, weight=1), no ore budger
    EXPECT_FALSE(limiter.rateAccess(client1, "resA", {rps10, 5}, 1, now));
    // allowed: burst=10, weight=1, budget increased with burst
    EXPECT_TRUE(limiter.rateAccess(client1, "resA", {rps10, 10}, 1, now)); // +1

    // Expect limits kept in state
    EXPECT_EQ(limiter.limits(), Limits({{"resA", {{client1, rps10}}}}));
    // Expect increments added to the shard
    EXPECT_EQ(limiter.shardTotal(), Counters({{"resA", {{client1, 6}}}}));
    // Expect lower bound advanced in the other
    EXPECT_EQ(limiter.otherTotal(), Counters({{"resA", {{client1, rps10.lowerBound(now)}}}}));

    // time goes on
    now += seconds(1);
    // allowed (burst=5, weight=5), whole budget available
    EXPECT_TRUE(limiter.rateAccess(client1, "resA", {rps10, 5}, 5, now)); // +5

    EXPECT_EQ(
        limiter.shardTotal() + limiter.otherTotal(),
        Counters({{"resA", {{client1, rps10.lowerBound(now) + 5}}}})
    );

    EXPECT_THROW_MESSAGE_HAS_SUBSTR(  // Invalid limit
        limiter.rateAccess(client1, "resA", {{}, 10}, 1, now),
        InvalidLimitError, "Invalid limit"
    );

    // limit upgrade (nex generation)
    EXPECT_TRUE(limiter.rateAccess(client1, "resA", {rpm60, 10}, 1, now)); // +1
    // Limit changed
    EXPECT_EQ(limiter.limits(), Limits({{"resA", {{client1, rpm60}}}}));

    EXPECT_EQ(
        limiter.shardTotal() + limiter.otherTotal(),
        Counters({{"resA", {{client1, rpm60.lowerBound(now) + 6}}}})
    );

    // Limit downgrade not allowed
    EXPECT_THROW_MESSAGE_HAS_SUBSTR(
        limiter.rateAccess(client1, "resA", {rps10, 10}, 1, now),
        InvalidLimitError, "Inconsistent limit generation"
    );
    // Inconsistent limit - same generation with different params
    EXPECT_THROW_MESSAGE_HAS_SUBSTR(
        limiter.rateAccess(client1, "resA", {{.rate=100, .unit=1, .gen=rpm60.gen}, 10}, 1, now),
        InvalidLimitError,
        "Inconsistent limit:(rate=100,unit=1,gen=2) != expected:(rate=60,unit=1,gen=2)"
    );
}

Y_UNIT_TEST(rate_check_spend)
{
    NovaAccessLimiter limiter{"Z"};

    // We're at 10sec from the beginning of the epoch
    system_clock::time_point now{std::chrono::seconds(10)};

    auto client1 = uuid("11111111-1111-1111-1111-111111111111");
    auto rps10 = Limit{.rate = 10, .unit = 1, .gen=1};

    // allowed: burst=5, weight=2
    EXPECT_TRUE(limiter.rateCheck(client1, "resA", {rps10, 5}, 2, now));
    // spend: burst=5, weight=2
    limiter.rateSpend(client1, "resA", {rps10, 5}, 2, now);    // +2
    // allowed: burst=5, weight=2
    EXPECT_TRUE(limiter.rateCheck(client1, "resA", {rps10, 5}, 2, now));
    // allowed: burst=5, weight=2
    EXPECT_TRUE(limiter.rateCheck(client1, "resA", {rps10, 5}, 2, now));
    // spend: burst=5, weight=2
    limiter.rateSpend(client1, "resA", {rps10, 5}, 2, now);    // +2
    // rejected: burst=5, weight=2
    EXPECT_FALSE(limiter.rateCheck(client1, "resA", {rps10, 5}, 2, now));
    // allowed: burst=5, weight=1
    EXPECT_TRUE(limiter.rateCheck(client1, "resA", {rps10, 5}, 1, now));
    // spend: burst=5, weight=2
    limiter.rateSpend(client1, "resA", {rps10, 5}, 2, now);    // +2
    // spend: burst=5, weight=1
    limiter.rateSpend(client1, "resA", {rps10, 5}, 1, now);    // +1
    // rejected: burst=5, weight=1
    EXPECT_FALSE(limiter.rateCheck(client1, "resA", {rps10, 5}, 1, now));
    
    // Expect limits kept in state
    EXPECT_EQ(limiter.limits(), Limits({{"resA", {{client1, rps10}}}}));
    // Expect counters changed only with rateSpend (+7)
    EXPECT_EQ(limiter.shardTotal(), Counters({{"resA", {{client1, 7}}}}));
    // Expect lower bound advanced in the other
    EXPECT_EQ(limiter.otherTotal(), Counters({{"resA", {{client1, rps10.lowerBound(now)}}}}));

    // time goes on
    now += seconds(1);
    // allowed: (burst=5, weight=3)
    EXPECT_TRUE(limiter.rateCheck(client1, "resA", {rps10, 5}, 3, now));
    // spend: (burst=5, weight=3)
    limiter.rateSpend(client1, "resA", {rps10, 5}, 3, now); // +3
    // rejected (burst=5, weight=3)
    EXPECT_FALSE(limiter.rateCheck(client1, "resA", {rps10, 5}, 3, now));

    EXPECT_EQ(
        limiter.shardTotal() + limiter.otherTotal(),
        Counters({{"resA", {{client1, rps10.lowerBound(now) + 3}}}})
    );
}

Y_UNIT_TEST(update_from_upstream)
{
    NovaAccessLimiter limiter{"Z"};
    system_clock::time_point now{std::chrono::seconds(10)};

    auto client1 = uuid("11111111-1111-1111-1111-111111111111");
    auto client2 = uuid("22222222-2222-2222-2222-222222222222");
    auto client3 = uuid("33333333-3333-3333-3333-333333333333");

    auto limitX = Limit{.rate = 10, .unit = 1, .gen=1};
    auto limitY = Limit{.rate = 100, .unit = 1, .gen=2};

    // Add some increments
    EXPECT_TRUE(limiter.rateAccess(client1, "resA", {limitX, 5}, 1, now)); // +1
    EXPECT_TRUE(limiter.rateAccess(client2, "resA", {limitY, 50}, 25, now)); // +25
    // Check state before updating
    EXPECT_EQ(
        limiter.limits(),
        Limits({{"resA", {{client1, limitX}, {client2, limitY}}}})
    );
    EXPECT_EQ(
        limiter.shardTotal(),
        Counters({{"resA", {{client1, 1}, {client2, 25}}}})
    );
    EXPECT_EQ(
        limiter.otherTotal(),
        Counters({
            {"resA", {{client1, limitX.lowerBound(now)}, {client2, limitY.lowerBound(now)}}}
        })
    );

    // Update from upstream
    auto upstreamCounters = Counters{
        {"resA", {{client1, 100500}, {client2, 100500}, {client3, 100500}}}
    };
    // client1 newer limit, client2 older limit, client3 seeing first time
    auto upstreamLimits = Limits{
        {"resA", {{client1, limitY}, {client2, limitX}, {client3, limitX}}}
    };

    limiter.upstreamUpdate(upstreamCounters, upstreamLimits, now);

    auto diffXY = limitX.lowerBound(now) - limitY.lowerBound(now);
    auto diffYX = limitY.lowerBound(now) - limitX.lowerBound(now);
    // Expect existing limits not changed
    EXPECT_EQ(
        limiter.limits(),
        Limits({
            {"resA", {
                {client1, limitX}, {client2, limitY},
                {client3, limitX}  // added client3 limit to collection
            }}
        })
    );
    // Expect shard not changed
    EXPECT_EQ(
        limiter.shardTotal(),
        Counters({{"resA", {{client1, 1}, {client2, 25}}}})
    );
    // Expect update in the `other`
    EXPECT_EQ(
        limiter.otherTotal(),
        Counters({
            {"resA", {
                {client1, 100500+diffXY}, {client2, 100500+diffYX},
                {client3, 100500},  // client3 set as it is
            }}
        })
    );
}

Y_UNIT_TEST(allowed_generation_downgrade)
{
    NovaAccessLimiter limiter{"Z"};
    system_clock::time_point now{std::chrono::seconds(10)};

    auto client1 = uuid("11111111-1111-1111-1111-111111111111");
    auto rps10 = Limit{.rate = 10, .unit = 1, .gen=1};      // old limit
    auto rps100 = Limit{.rate = 100, .unit = 1, .gen=2};    // new limit

    // Agent gets newer (generation=2) from server
    limiter.upstreamUpdate(
        {{"resA", {{client1, rps100.lowerBound(now)}}}},
        {{"resA", {{client1, rps100}}}},
        now
    );
    // Check state has generation=2
    EXPECT_EQ(limiter.limits(), Limits({{"resA", {{client1, rps100}}}}));
    EXPECT_EQ(limiter.shardTotal(), Counters());
    EXPECT_EQ(limiter.otherTotal(), Counters({{"resA", {{client1, rps100.lowerBound(now)}}}}));

    // Access with old generation=1 (limits update late on this agent)
    // Here limit downgrade allowed, because counter has shard==0 (no local increments)
    EXPECT_TRUE(limiter.rateAccess(client1, "resA", {rps10, 10}, 5, now)); // +5
    // Expect state has changed to generation=1
    EXPECT_EQ(limiter.limits(), Limits({{"resA", {{client1, rps10}}}}));
    EXPECT_EQ(limiter.shardTotal(), Counters({{"resA", {{client1, 5}}}}));
    EXPECT_EQ(limiter.otherTotal(), Counters({{"resA", {{client1, rps10.lowerBound(now)}}}}));

    // But downgrade fails if counter has shard > 0
    EXPECT_TRUE(limiter.rateAccess(client1, "resA", {rps100, 10}, 1, now)); // upgrade
    EXPECT_THROW_MESSAGE_HAS_SUBSTR(  // failed downgrade
        limiter.rateAccess(client1, "resA", {rps10, 10}, 1, now),
        InvalidLimitError, "Inconsistent limit generation"
    );
}


Y_UNIT_TEST(garbage_collect)
{
    NovaAccessLimiter limiter{"Z"};

    // We're at 10sec from the beginning of the epoch
    system_clock::time_point now{std::chrono::seconds(10)};

    auto client1 = uuid("11111111-1111-1111-1111-111111111111");
    auto client2 = uuid("22222222-2222-2222-2222-222222222222");
    auto client3 = uuid("33333333-3333-3333-3333-333333333333");
    auto rps10 = Limit{.rate = 10, .unit = 1, .gen=1};
    auto rpm60 = Limit{.rate = 60, .unit = 60, .gen=2};

    // Receive update from upstream
    limiter.upstreamUpdate(  // client3 with 5secs overbound
        Counters({{"resA", {{client3, rps10.lowerBound(now) + 5 * rps10.rate}}}}),
        Limits({{"resA", {{client3, rps10}}}}),
        now
    );

    // Add some increments
    EXPECT_TRUE(limiter.rateAccess(client1, "resA", {rps10, 5}, 1, now)); // +1
    EXPECT_TRUE(limiter.rateAccess(client2, "resA", {rpm60, 60}, 30, now)); // +30

    EXPECT_EQ(
        limiter.shardTotal() + limiter.otherTotal(),
        Counters({{"resA", {
            {client1, rps10.lowerBound(now) + 1},
            {client2, rpm60.lowerBound(now) + 30},
            {client3, rps10.lowerBound(now) + 5 * rps10.rate}}
        }})
    );

    limiter.garbageCollect(now);

    // Nothing collected, all counters above lower bound
    EXPECT_EQ(
        limiter.limits(),
        Limits({{"resA", {{client1, rps10}, {client2, rpm60}, {client3, rps10}}}})
    );
    EXPECT_EQ(
        limiter.shardTotal() + limiter.otherTotal(),
        Counters({{"resA", {
            {client1, rps10.lowerBound(now) + 1},
            {client2, rpm60.lowerBound(now) + 30},
            {client3, rps10.lowerBound(now) + 5 * rps10.rate}}
        }})
    );

    // time goes on
    now += seconds(1);

    limiter.garbageCollect(now);

    // client1 collected, client2 and client3 still above lower bound
    EXPECT_EQ(
        limiter.limits(),
        Limits({{"resA", {{client2, rpm60}, {client3, rps10}}}})
    );
    EXPECT_EQ(
        limiter.shardTotal() + limiter.otherTotal(),
        Counters({{"resA", {
            {client2, rpm60.lowerBound(now) + 30 - 1},  // -1 since rpm60 eats 1 per-sec
            {client3, rps10.lowerBound(now) + (5-1) * rps10.rate}}} // 1sec passed
        })
    );

    // time +30 sec from the beginning
    now += seconds(29);

    limiter.garbageCollect(now);

    // Empty resource left at this point
    EXPECT_EQ(limiter.limits(), Limits({{"resA", {}}}));
    EXPECT_EQ(limiter.shardTotal(), Counters({{"resA", {}}}));
    EXPECT_EQ(limiter.otherTotal(), Counters({{"resA", {}}}));
}

} // Y_UNIT_TEST_SUITE

}  // namespace maps::rate_limiter2::tests
