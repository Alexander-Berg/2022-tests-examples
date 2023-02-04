#include <maps/infra/ratelimiter2/common/include/test_helpers.h>
#include <maps/infra/ratelimiter2/common/include/nova_sharded_counter.h>

#include <boost/uuid/uuid_io.hpp>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::rate_limiter2::tests {

using std::chrono::system_clock;
using std::chrono::seconds;

Y_UNIT_TEST_SUITE(nova_counters_test) {


Y_UNIT_TEST(limits_conversion_corner_cases)
{
    // NB: toNewerOnly flag has no effect in these trivial cases

    auto client1 = uuid("11111111-1111-1111-1111-111111111111");
    auto client2 = uuid("22222222-2222-2222-2222-222222222222");
    auto limitX = Limit{.rate = 10, .unit = 1, .gen = 5};

    system_clock::time_point now{std::chrono::seconds(10)};

    SortedLimits a = {
        {"resourceA", {{client1, limitX}, {client2, limitX}}},
        {"resourceB", {}},
    };
    // Convert to empty changes nothing
    auto t = a;
    EXPECT_EQ(convertLimits(&t, {}, true, now), SortedCounters()); // zero diff
    EXPECT_EQ(t, a);
    EXPECT_EQ(convertLimits(&t, {}, false, now), SortedCounters());
    EXPECT_EQ(t, a);

    t = {};  // Convert from empty will results zero diff
    EXPECT_EQ(convertLimits(&t, a, true, now), SortedCounters());
    EXPECT_EQ(t, a);
    t = {};
    EXPECT_EQ(convertLimits(&t, a, false, now), SortedCounters());
    EXPECT_EQ(t, a);
    t = a;
    EXPECT_EQ(convertLimits(&t, a, true, now), SortedCounters());
    EXPECT_EQ(convertLimits(&t, a, false, now), SortedCounters());
}

Y_UNIT_TEST(limits_converions)  // Nontrivial conversion cases
{
    // Now is 10sec from beginning of the epoch
    system_clock::time_point now{std::chrono::seconds(10)};

    // Single value tests

    {   // toNewerOnly == false cases

        auto t = Limit{.rate=10, .unit=1, .gen=5};
        // Convert to older generation
        EXPECT_EQ(
            t.convert({.rate=100, .unit=1, .gen=1}, false, now),
            900 // 100*now/1 - 10*now/1
        );
        // Converted
        EXPECT_EQ(t, Limit({.rate=100, .unit=1, .gen=1}));

        // Convert to newer generation
        EXPECT_EQ(
            t.convert({.rate=120, .unit=60, .gen=10}, false, now),
            -980 // 120*now/60 - 100*now/1
        );
        // Converted
        EXPECT_EQ(t, Limit({.rate=120, .unit=60, .gen=10}));
    }
    {  // toNewerOnly == true cases

        auto t = Limit{.rate=300, .unit=60, .gen=5};
        // Convert to newer generation
        EXPECT_EQ(
            t.convert({.rate=100, .unit=1, .gen=10}, true, now),
            950   // += (100*now/1 - 300*now/60)
        );
        // Converted
        EXPECT_EQ(t, Limit({.rate=100, .unit=1, .gen=10}));
        // Ignored convert to older generation
        EXPECT_EQ(
            t.convert({.rate=100, .unit=1, .gen=1}, true, now),
            0
        );
        // Nothing changed
        EXPECT_EQ(t, Limit({.rate=100, .unit=1, .gen=10}));
    }

    // Collections
    auto client1 = uuid("11111111-1111-1111-1111-111111111111");
    auto client2 = uuid("22222222-2222-2222-2222-222222222222");
    auto client3 = uuid("33333333-3333-3333-3333-333333333333");
    auto limitX = Limit{.rate = 10, .unit = 1, .gen = 5};
    auto limitY = Limit{.rate = 100, .unit = 1, .gen = 10};  // newer then X
    auto limitZ = Limit{.rate = 300, .unit = 60, .gen = 3};  // older then X

    const SortedLimits a {
        {"resourceA", {{client1, limitX}, {client2, limitX}, {client3, limitX}}},
        {"resourceB", {{client1, limitX}, {client3, limitX}}}
    };
    const SortedLimits b {
        {"resourceA", {
            {client1, limitX},  // same as A
            {client2, limitY},  // newer than A
            {client3, {}}   // `empty` value
        }},
        {"resourceB", {
           {client1, limitZ}}  // older that A
        },
        {"resourceC", {{client1, limitY}}}
    };

    {   // toNewerOnly == true
        auto t = a;
        EXPECT_EQ(
            convertLimits(&t, b, true, now),
            SortedCounters({
               {"resourceA", {{client2, 900}}},  // (100*now/1) - (10*now/1)
            })
        );
        EXPECT_EQ(
            t, SortedLimits({
                {"resourceA", {
                    {client1, limitX}, // same limit
                    {client2, limitY}, // converted to newer limit
                    {client3, limitX}  // ignored empty
                }},
                {"resourceB", {
                    {client1, limitX},  // ignored old limit
                    {client3, limitX}   // not touched
                }},
                {"resourceC", {{client1, limitY}}} // new entry
            })
        );
    }
    {   // toNewerOnly == false
        auto t = a;
        EXPECT_EQ(
            convertLimits(&t, b, false, now),
            SortedCounters({
               {"resourceA", {{client2, 900}}}, // (100*now/1) - (10*now/1)
               {"resourceB", {{client1, -50}}}  // (300*now/60) - (10*now/1)
            })
        );
        EXPECT_EQ(
            t, SortedLimits({
                {"resourceA", {
                    {client1, limitX}, // same limit
                    {client2, limitY}, // converted to newer limit
                    {client3, limitX}  // ignored empty
                }},
                {"resourceB", {
                    {client1, limitZ},  // converted to older limit
                    {client3, limitX}   // not touched
                }},
                {"resourceC", {{client1, limitY}}} // new entry
            })
        );
    }
}

Y_UNIT_TEST(calculate_lower_bounds)
{
    auto client1 = makeClientHash("client.1");
    auto client2 = makeClientHash("client.2");

    auto rps10 = Limit{.rate = 10, .unit = 1, .gen=1};
    auto rps100 = Limit{.rate = 100, .unit = 1, .gen=1};
    auto rph300 = Limit{.rate = 300, .unit = 3600, .gen=1};

    SortedLimits limitsProfile = {
        {"resourceA", {{client1, rps10}, {client2, rps100}}},
        {"resourceB", {{client1, rph300}}},
        {"resourceC ", {}}
    };

    auto now = system_clock::now();
    EXPECT_EQ(
        calculateLowerBounds(limitsProfile, now),
        SortedCounters({
            {"resourceA", {
                {client1, rps10.lowerBound(now)},
                {client2, rps100.lowerBound(now)}}},
            {"resourceB", {{client1, rph300.lowerBound(now)}}},
            {"resourceC ", {}}
        })
    );
}

Y_UNIT_TEST(limits_update_from_downstream)
{
    ManualClock testClock{system_clock::time_point(seconds(10))};

    NovaShardedCounter state("Z");

    auto client1 = uuid("11111111-1111-1111-1111-111111111111");

    auto limitX = Limit{.rate = 10, .unit = 1, .gen=1};
    auto limitY = Limit{.rate = 300, .unit = 60, .gen=2};

    state.downstreamUpdateLimits({{"resource1", {{client1, limitX}}}}, testClock());
    EXPECT_EQ(
        state.limits(),
        SortedLimits({{"resource1", {{client1, limitX}}}})
    );
    EXPECT_EQ(state.otherTotal(), SortedCounters()); // other not touched

    // Update with new limitY
    state.downstreamUpdateLimits({{"resource1", {{client1, limitY}}}}, testClock());
    EXPECT_EQ(
        state.limits(),  // converted to new limitY
        SortedLimits({{"resource1", {{client1, limitY}}}})
    );
    EXPECT_EQ(
        state.otherTotal(),
        SortedCounters({{"resource1", {{client1, -50}}}}) // client1 conversion diff
    );

    // Update with older limitX
    state.downstreamUpdateLimits({{"resource1", {{client1, limitX}}}}, testClock());
    EXPECT_EQ(
        state.limits(),  // no conversion to older
        SortedLimits({{"resource1", {{client1, limitY}}}})
    );
    EXPECT_EQ(
        state.otherTotal(),  // other stays same
        SortedCounters({{"resource1", {{client1, -50}}}})
    );
}

Y_UNIT_TEST(update_from_upstream)
{
    NovaShardedCounter state("Z");

    ManualClock testClock{system_clock::time_point(seconds(10))};

    auto client1 = uuid("11111111-1111-1111-1111-111111111111");
    auto client2 = uuid("22222222-2222-2222-2222-222222222222");
    auto client3 = uuid("33333333-3333-3333-3333-333333333333");

    auto limitX = Limit{.rate = 10, .unit = 1, .gen=1};
    auto limitY = Limit{.rate = 100, .unit = 1, .gen=2};

    // Set state
    state.downstreamUpdateLimits(
        {{"resource1", {{client1, limitX}, {client2, limitY}}}},
        testClock()
    );
    EXPECT_EQ(
        state.limits(),
        SortedLimits({{"resource1", {{client1, limitX}, {client2, limitY}}}})
    );

    auto upstreamCounters = SortedCounters{
        {"resource1", {{client1, 100500}, {client2, 100500}, {client3, 100500}}}
    };
    // client1 newer limit, client2 older limit, client3 seeing first time
    auto upstreamLimits = SortedLimits{
        {"resource1", {{client1, limitY}, {client2, limitX}, {client3, limitX}}}
    };

    state.upstreamUpdate(upstreamCounters, upstreamLimits, testClock());

    auto diffXY = limitX.lowerBound(testClock()) - limitY.lowerBound(testClock());
    auto diffYX = limitY.lowerBound(testClock()) - limitX.lowerBound(testClock());
    EXPECT_EQ(
        state.limits(),
        SortedLimits({
            {"resource1", {
                {client1, limitX}, {client2, limitY},
                {client3, limitX}  // added client3 limit to collection
            }}
        })
    );
    EXPECT_EQ(
        state.otherTotal(),
        SortedCounters({
            {"resource1", {
                {client1, 100500+diffXY}, {client2, 100500+diffYX},
                {client3, 100500},  // didn't know client3 limit before
            }}
        })
    );
}

Y_UNIT_TEST(update_from_peer)
{
    NovaShardedCounter state("Z");

    ManualClock testClock{system_clock::time_point(seconds(10))};

    auto client1 = uuid("11111111-1111-1111-1111-111111111111");
    auto client2 = uuid("22222222-2222-2222-2222-222222222222");
    auto limitX = Limit{.rate = 10, .unit = 1, .gen=1};
    auto limitY = Limit{.rate = 100, .unit = 1, .gen=2};

    // Receiving p2p when I'm empty (e.g. right after start)
    state.peerUpdate(
        SortedCounters{{"resA", {{client1, 100}}}},
        SortedLimits{{"resA", {{client1, limitX}}}},
        testClock()
    );
    // Check state contains received data
    EXPECT_EQ(
        state.limits(),
        SortedLimits({{"resA", {{client1, limitX}}}})
    );
    EXPECT_EQ(  // Received values put to 'other'
        state.otherTotal(),
        SortedCounters({{"resA", {{client1, 100}}}})
    );
    EXPECT_EQ(state.shardTotal(), SortedCounters()); // shard not touched

    // Empty p2p sync
    state.peerUpdate({}, {}, testClock());
    // Check state not changed
    EXPECT_EQ(
        state.limits(),
        SortedLimits({{"resA", {{client1, limitX}}}})
    );
    EXPECT_EQ(  // Received values put to 'other'
        state.otherTotal(),
        SortedCounters({{"resA", {{client1, 100}}}})
    );
    EXPECT_EQ(state.shardTotal(), SortedCounters()); // shard not touched

    // p2p: received < my total
    state.peerUpdate(
        SortedCounters{{"resA", {{client1, 90}, {client2, 150}}}},
        SortedLimits{{"resA", {{client1, limitX}, {client2, limitY}}}},
        testClock()
    );
    // Check state changes, expect new client2 entry
    EXPECT_EQ(
        state.limits(),
        SortedLimits({{"resA", {{client1, limitX}, {client2, limitY}}}})
    );
    EXPECT_EQ(
        state.otherTotal(), // client1: max(100, 90)
        SortedCounters({{"resA", {{client1, 100}, {client2, 150}}}})
    );

    // p2p: received > my total
    state.peerUpdate(
        SortedCounters{{"resA", {{client1, 200}}}},
        SortedLimits{{"resA", {{client1, limitX}}}},
        testClock()
    );
    // Check state, expect only otherTotal changed
    EXPECT_EQ(
        state.limits(),
        SortedLimits({{"resA", {{client1, limitX}, {client2, limitY}}}})
    );
    EXPECT_EQ(
        state.otherTotal(), // client1: max(100, 200)
        SortedCounters({{"resA", {{client1, 200}, {client2, 150}}}})
    );

    // p2p: different limits
    state.peerUpdate(   // newer limit for client1, older limit for client2
        SortedCounters{{"resA", {{client1, 777}, {client2, 1234}}}},
        SortedLimits{{"resA", {{client1, limitY}, {client2, limitX}}}},
        testClock()
    );
    // Check state, expect limits upgraded to newer
    EXPECT_EQ(
        state.limits(), // client1 has new limitY
        SortedLimits({{"resA", {{client1, limitY}, {client2, limitY}}}})
    );
    EXPECT_EQ(
        state.otherTotal(), // client1: max(200, 777 + 10*10/1 - 100*10/1)
        SortedCounters({
            {"resA", {
                {client1, 1100},  // max(200 + 100*10/1 - 10*10/1, 777)
                {client2, 2134}}} // max(150, 1234 + 100*10/1 - 10*10/1)
        })
    );
    EXPECT_EQ(state.shardTotal(), SortedCounters()); // shard still not touched
}

Y_UNIT_TEST(limits_filtering)
{
    NovaShardedCounter state("Z");

    // Set limits
    auto client1 = uuid("11111111-1111-1111-1111-111111111111");
    auto client2 = uuid("22222222-2222-2222-2222-222222222222");
    auto limitX = Limit{.rate = 10, .unit = 1, .gen=1};
    auto limitY = Limit{.rate = 300, .unit = 60, .gen=2};
    state.downstreamUpdateLimits(
        {{"resource1", {{client1, limitX}, {client2, limitY}}}, {"resource2", {{client1, limitX}}}},
        system_clock::time_point(seconds(10))
    );

    EXPECT_EQ(
        state.limitsFiltered(SortedCounters({{"resource1", {}}})),
        SortedLimits({{"resource1", {{client1, limitX}, {client2, limitY}}}})
    );
    EXPECT_EQ(
        state.limitsFiltered(SortedCounters({{"resource1", {}}, {"resource2", {}}})),
        SortedLimits({
            {"resource1", {{client1, limitX}, {client2, limitY}}},
            {"resource2", {{client1, limitX}}}
        })
    );
    // Unknown resource3
    EXPECT_THROW(
        state.limitsFiltered(SortedCounters({{"resource1", {}}, {"resource3", {}}})),
        std::out_of_range
    );
}

Y_UNIT_TEST(advance_by_time)
{
    auto client1 = uuid("11111111-1111-1111-1111-111111111111");
    auto client2 = uuid("22222222-2222-2222-2222-222222222222");

    auto rps10 = Limit{.rate = 10, .unit = 1, .gen=1};
    auto rpm300 = Limit{.rate = 300, .unit = 60, .gen=2};

    NovaShardedCounter state("C");
    state.advanceByTime(system_clock::time_point(seconds(5)));
    // No changes if empty
    EXPECT_TRUE(state.shardTotal().empty() && state.otherTotal().empty());

    state.downstreamUpdate("A", {
        {"resource1", {{client1, 10}, {client2, 100}}}
    });

    state.advanceByTime(system_clock::time_point(seconds(5)));
    EXPECT_EQ(  // Zeroes 'cause no limits
        state.otherTotal(),
        SortedCounters({{"resource1", {{client1, 0}, {client2, 0}}}})
    );

    // set limits
    state.downstreamUpdateLimits(
        {
            {"resource1", {{client1, rps10}, {client2, rpm300}}},
            {"resource2", {{client1, rps10}}}
        },
        system_clock::time_point(seconds(5))
    );

    state.advanceByTime(system_clock::time_point(seconds(5)));
    EXPECT_EQ(
        state.otherTotal(),
        SortedCounters({
            {"resource1", {
                {client1, 40}, // rps10.lowerBound - 10
                {client2, 0},  // rpm300.lowerBound - 100 < 0
            }},
            {"resource2", {{client1, 50}}}
        })
    );

    state.advanceByTime(system_clock::time_point(seconds(30)));
    EXPECT_EQ(
        state.otherTotal(),
        SortedCounters({
            {"resource1", {
                {client1, 290},  // rps10.lowerBound - 10
                {client2, 50},  // rpm300.loweBound - 100
            }},
            {"resource2", {{client1, 300}}} // rps10.loweBound - 0
        })
    );
}

Y_UNIT_TEST(garbage_collect)
{
    system_clock::time_point now{std::chrono::seconds(10)};
    NovaShardedCounter state("C");

    auto client1 = uuid("11111111-1111-1111-1111-111111111111");
    auto client2 = uuid("22222222-2222-2222-2222-222222222222");
    auto client3 = uuid("33333333-3333-3333-3333-333333333333");

    auto rps10 = Limit{.rate = 10, .unit = 1, .gen=1};
    auto rpm300 = Limit{.rate = 300, .unit = 60, .gen=2};

    // Updates from children
    state.downstreamUpdate("A", {{"resource1", {{client1, 10}, {client2, 100}}}});
    state.downstreamUpdate("B", {{"resource1", {{client1, 5}, {client2, 50}}}});
    state.downstreamUpdateLimits(
        {{"resource1", {{client1, rps10}, {client2, rpm300}}}},
        now
    );

    // Update from parent
    state.upstreamUpdate(  // client3 will have limit and 'other' entries but no 'shard'
        {{"resource1", {{client3, rps10.lowerBound(now) + 10}}}},
        {{"resource1", {{client3, rps10}}}},
        now
    );

    state.advanceByTime(now);

    // client1 < lowerBound but not collected
    // client3 collected (no 'shard')
    EXPECT_EQ(state.garbageCollect(now), 1ul);
    EXPECT_EQ(
        state.shardTotal(),
        SortedCounters({{"resource1", {{client1, 15}, {client2, 150}}}})
    );
    EXPECT_EQ(
        state.otherTotal(),
        SortedCounters({{"resource1", {{client1, 85}, {client2, 0}}}})
    );
    EXPECT_EQ(
        state.limits(),
        SortedLimits({{"resource1", {{client1, rps10}, {client2, rpm300}}}})
    );

    // Drop children state
    state.downstreamReset("A");
    state.downstreamReset("B");

    // Now client1 collected
    EXPECT_EQ(state.garbageCollect(now), 1ul);
    EXPECT_EQ(
        state.shardTotal(),
        SortedCounters({{"resource1", {{client2, 150}}}})
    );
    EXPECT_EQ(
        state.otherTotal(),
        SortedCounters({{"resource1", {{client2, 0}}}})
    );
    EXPECT_EQ(
        state.limits(),
        SortedLimits({{"resource1", {{client2, rpm300}}}})
    );

    // 30 seconds passed
    now += seconds(30);
    // Now client2 collected
    EXPECT_EQ(state.garbageCollect(now), 1ul);
    // And nothing left
    EXPECT_EQ(state.shardTotal(), SortedCounters());
    EXPECT_EQ(state.otherTotal(), SortedCounters());
    EXPECT_EQ(state.limits(), SortedLimits());
}

Y_UNIT_TEST(garbage_collect_shard0)
{
    system_clock::time_point now{std::chrono::seconds(10)};
    NovaShardedCounter state("C");

    auto client1 = uuid("11111111-1111-1111-1111-111111111111");
    auto rps10 = Limit{.rate = 10, .unit = 1, .gen=1};

    // Zero update from child
    state.downstreamReset("A", {{"res1", {{client1, 153}}}});
    state.downstreamUpdate("A", {{"res1", {{client1, 153}}}});
    state.downstreamUpdateLimits({{"res1", {{client1, rps10}}}}, now);

    // Non-zero update from peer
    state.peerUpdate(
        {{"res1", {{client1, 100500}}}},
        {{"res1", {{client1, rps10}}}},
        now
    );

    // Check state is (shard==0, other!=0, limit)
    EXPECT_EQ(state.limits(), SortedLimits({{"res1", {{client1, rps10}}}}));
    EXPECT_EQ(state.shardTotal(), SortedCounters({{"res1", {{client1, 0}}}}));
    EXPECT_EQ(state.otherTotal(), SortedCounters({{"res1", {{client1, 100500}}}}));

    // shard==0 counter is garbage
    state.garbageCollect(now);
    // Expect all components dropped: shard, other and limit
    EXPECT_EQ(state.limits(), SortedLimits());
    EXPECT_EQ(state.shardTotal(), SortedCounters());
    EXPECT_EQ(state.otherTotal(), SortedCounters());
    // Even if counter is referenced by child state
    EXPECT_EQ(state.childCounters("A"), SortedCounters({{"res1", {{client1, 153}}}}));
}

Y_UNIT_TEST(complex_sync)
{
    NovaShardedCounter childA("a"), childB("b"), srv("S");

    // Starting at 10sec from the beginning of epoch
    ManualClock testClock{system_clock::time_point(seconds(10))};

    auto doSync = [&testClock](auto& child, auto& parent) {
        // request
        parent.downstreamUpdate(child.id(), child.shardTotal());
        parent.downstreamUpdateLimits(child.limits(), testClock());
        // response
        child.upstreamUpdate(
            parent.downstreamResponse(child.shardTotal()),
            parent.limitsFiltered(child.shardTotal()),
            testClock()
        );
    };

    auto client1 = uuid("11111111-1111-1111-1111-111111111111");
    auto client2 = uuid("22222222-2222-2222-2222-222222222222");
    auto rps20 = Limit{.rate = 20, .unit = 1, .gen=1};
    auto rps100 = Limit{.rate = 100, .unit = 1, .gen=2};

    // Setup state
    childA.downstreamUpdate("_", {{"resourceA", {{client1, 10}, {client2, 100}}}});
    childB.downstreamUpdate("_", {{"resourceA", {{client1, 5}}}});
    childA.downstreamUpdateLimits(
        {{"resourceA", {{client1, rps20}, {client2, rps20}}}},
        testClock()
    );
    childB.downstreamUpdateLimits({{"resourceA", {{client1, rps20}}}}, testClock());

    childA.advanceByTime(testClock());
    childB.advanceByTime(testClock());
    srv.advanceByTime(testClock());

    // Sync both A,B <-> srv
    doSync(childA, srv);
    doSync(childB, srv);

    // Check limits delivered
    EXPECT_EQ(
        childA.limits(),
        SortedLimits({{"resourceA", {{client1, rps20}, {client2, rps20}}}})
    );
    EXPECT_EQ(
        childB.limits(),  // B received both limits
        SortedLimits({{"resourceA", {{client1, rps20}, {client2, rps20}}}})
    );
    EXPECT_EQ(
        srv.limits(),  // srv has both limits
        SortedLimits({{"resourceA", {{client1, rps20}, {client2, rps20}}}})
    );

    // Check counters (totals)
    EXPECT_EQ( // A ain't got B delta (B synced after A)
        childA.shardTotal() + childA.otherTotal(),
        SortedCounters({{"resourceA", {{client1, 10}, {client2, 100}}}})
    );
    EXPECT_EQ(  // B got A1 delta
        childB.shardTotal() + childB.otherTotal(),
        SortedCounters({{"resourceA", {{client1, 15}, {client2, 100}}}})
    );
    EXPECT_EQ( // srv got both deltas
        srv.otherTotal() + srv.shardTotal(),
        SortedCounters({{"resourceA", {{client1, 15}, {client2, 100}}}})
    );

    ////// 1 sec passed
    testClock() += seconds(1);

    // Add deltas to A and B
    childA.downstreamUpdate("_", {{"resourceA", {{client1, 20}}}});  // client1: +10
    childB.downstreamUpdate("_", {{"resourceA", {{client1, 10}}}});  // client1: +5

    childA.advanceByTime(testClock());
    childB.advanceByTime(testClock());
    srv.advanceByTime(testClock());

    // Sync both A,B <-> srv
    doSync(childA, srv);
    doSync(childB, srv);

    // Check counters states
    // lowerBound at 11sec (from the epoch beginning)
    EXPECT_EQ( // NB: Again A ain't got client1:+5 from B
        childA.shardTotal() + childA.otherTotal(),
        SortedCounters({{"resourceA", {{client1, 230}, {client2, 220}}}})
        // client1: loBound(11sec)+10, client2: loBound
    );
    EXPECT_EQ(
        childB.shardTotal() + childB.otherTotal(),
        SortedCounters({{"resourceA", {{client1, 235}, {client2, 220}}}})
        // client1: loBound(11sec)+10+5, client2: loBound
    );
    EXPECT_EQ(
        srv.shardTotal(),
        SortedCounters({{"resourceA", {{client1, 30}, {client2, 100}}}})
    );
    EXPECT_EQ(
        srv.shardTotal() + srv.otherTotal(),
        SortedCounters({{"resourceA", {{client1, 235}, {client2, 220}}}})
        // client1: loBound(11sec)+10+5, client2: loBound
    );

    ////// 1 sec passed
    testClock() += seconds(1);

    // Add deltas to A and B
    childA.downstreamUpdate("_", {{"resourceA", {{client1, 30}}}});  // client1: +10
    childB.downstreamUpdate("_", {{"resourceA", {{client1, 15}}}});  // client1: +5
    // New limit on A
    childA.downstreamUpdateLimits(
        {{"resourceA", {{client1, rps100}, {client2, rps20}}}},
        testClock()
    );

    childA.advanceByTime(testClock());
    childB.advanceByTime(testClock());
    srv.advanceByTime(testClock());

    // Sync both A,B <-> srv
    doSync(childA, srv);
    doSync(childB, srv);

    // Check limits delivered
    EXPECT_EQ(
        childA.limits(),
        SortedLimits({{"resourceA", {{client1, rps100}, {client2, rps20}}}})
    );
    EXPECT_EQ(
        childB.limits(),  // B kept old limit
        SortedLimits({{"resourceA", {{client1, rps20}, {client2, rps20}}}})
    );
    EXPECT_EQ(
        srv.limits(),  // srv limit updated
        SortedLimits({{"resourceA", {{client1, rps100}, {client2, rps20}}}})
    );

    // Check counters states
    // lowerBound at 12 sec (from the epoch beginning)
    EXPECT_EQ(  // NB: client1 rescaled
        childA.shardTotal() + childA.otherTotal(),
        SortedCounters({{"resourceA", {{client1, 1210}, {client2, 240}}}})
        // client1: loBound(12sec)+10
    );
    EXPECT_EQ(
        childB.shardTotal() + childB.otherTotal(),
        SortedCounters({{"resourceA", {{client1, 255}, {client2, 240}}}})
        // client1: loBound(12sec)+10+5
    );
    EXPECT_EQ(
        srv.shardTotal(),
        SortedCounters({{"resourceA", {{client1, 45}, {client2, 100}}}})
    );
    EXPECT_EQ(
        srv.shardTotal() + srv.otherTotal(),
        SortedCounters({{"resourceA", {{client1, 1215}, {client2, 240}}}})
        // client1: lowBound(12sec)+10+5
    );
}

} // Y_UNIT_TEST_SUITE

}  // namespace maps::rate_limiter2::tests
