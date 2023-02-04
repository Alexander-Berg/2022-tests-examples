#include <maps/libs/log8/include/log8.h>
#include <maps/infra/ratelimiter2/plugin/lib/access_limiter.h>
#include <maps/infra/ratelimiter2/common/include/test_helpers.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <maps/libs/common/include/exception.h>

namespace maps::rate_limiter2::tests {

using std::chrono::system_clock;
using std::chrono::seconds;

namespace
{

int64_t toSeconds(system_clock::time_point t) {
    return std::chrono::duration_cast<std::chrono::seconds>(t.time_since_epoch()).count();
}

} // anonymous namespace

Y_UNIT_TEST_SUITE(access_limiter_test) {

Y_UNIT_TEST(standalone)
{
    ManualClock timeMeter;
    AccessLimiter limiter(std::ref(timeMeter));
    LimitsRegistry registry({
        { makeClientHash(""), {    // 'anybody' limits
            {"resource.1", {{.rate = 5, .unit = 1}, 5}} ,
            {"resource.forbidden", {{}, 0}} }}, // 'forbidden'
        { makeClientHash("client.1"), {
            {"resource.1", {{.rate = 10, .unit = 1}, 10}}, // specific limits
            {"resource.2", {{.rate = 5, .unit = 1}, 5}} }}
    }, 153);
    limiter.resetLimits(std::move(registry));

    // check correct limits version reported
    EXPECT_EQ(limiter.limitsVersion(), 153);

    // test undefined limit
    EXPECT_THROW(limiter.access("client.1", "", 1), UndefinedLimit);
    EXPECT_THROW(limiter.access("client.1", "no.such.resource", 1), UndefinedLimit);

    // test explicitly forbidden
    EXPECT_THROW(limiter.access("client.1", "resource.forbidden", 1), AccessForbidden);

    // access weight 1
    EXPECT_TRUE(limiter.access("some.client", "resource.1", 1));
    // access weight 4
    EXPECT_TRUE(limiter.access("some.client", "resource.1", 4));
    // access weight 1, expect reject ('anybody' limit applied)
    EXPECT_FALSE(limiter.access("some.client", "resource.1", 1));

    // another client access (higher specific limit applied)
    EXPECT_TRUE(limiter.access("client.1", "resource.1", 10));
    // and it's exceeded too
    EXPECT_FALSE(limiter.access("client.1", "resource.1", 1));

    { // check counters
        auto expected = Counters({
            {"resource.1", {{ makeClientHash("client.1"), 10 }, { makeClientHash("some.client"), 5 }}}
        });
        EXPECT_EQ(limiter.shardCounters(), expected);
    }

    timeMeter() += seconds(1);  // time step

    // allowed weight 5
    EXPECT_TRUE(limiter.access("some.client", "resource.1", 5));

    // client1 is good too
    EXPECT_TRUE(limiter.access("client.1", "resource.1", 5));
    // and for resource2
    EXPECT_TRUE(limiter.access("client.1", "resource.2", 5));
    // limits exceeded
    EXPECT_FALSE(limiter.access("client.1", "resource.2", 1));

    // but resource2 allowed to client1 only
    EXPECT_THROW(limiter.access("some.client", "resource.2", 1), UndefinedLimit);

    { // check counters
        auto expected = Counters({
            {"resource.1", {{ makeClientHash("client.1"), 15 }, { makeClientHash("some.client"), 10 }}},
            {"resource.2", {{ makeClientHash("client.1"), 5} }}
        });
        EXPECT_EQ(limiter.shardCounters(), expected);
    }
}

Y_UNIT_TEST(update_external)
{
    ManualClock timeMeter; //(10);
    AccessLimiter limiter(std::ref(timeMeter));
    LimitsRegistry registry ({  // 'anybody' limit to resource 3
        {makeClientHash(""), { {"resource.1", {{.rate = 10, .unit = 1}, 10}} }}
    }, 1);
    limiter.resetLimits(std::move(registry));

    { // external update +3    ( adjusted by rate*time bound)
        auto localVal = 0;
        limiter.applyUpdateResponse(plugin::CountersMessage {
            .counters = Counters(
                {{"resource.1",
                  {{makeClientHash("client.1"), 10 * toSeconds(timeMeter()) - localVal + 3}}}})});
    }

    // access weight 7
    EXPECT_TRUE(limiter.access("client.1", "resource.1", 7));
    // reject weight 1
    EXPECT_FALSE(limiter.access("client.1", "resource.1", 1));

    timeMeter() += seconds(1);  // time step

    // check locals
    EXPECT_EQ(
        limiter.shardCounters(), Counters({{"resource.1", {{makeClientHash("client.1"), 7}}}}));
    { // external update +3    ( adjusted by rate*time bound)
        auto localVal = 7;
        limiter.applyUpdateResponse(plugin::CountersMessage{
            .counters = Counters({{
                "resource.1",
                {{makeClientHash("client.1"), 10 * toSeconds(timeMeter()) - localVal + 3}}}
            })
        });
    }

    // allowed weight 7
    EXPECT_TRUE(limiter.access("client.1", "resource.1", 7));
    // rejected weight 1
    EXPECT_FALSE(limiter.access("client.1", "resource.1", 1));

    // counters check at the end
    EXPECT_EQ(limiter.shardCounters(), Counters({{ "resource.1", {{ makeClientHash("client.1"), 14 }}}}));
}


Y_UNIT_TEST(reset_limits)
{
    ManualClock timeMeter;
    AccessLimiter limiter(std::ref(timeMeter));
    LimitsRegistry registry ({  // specific limit
        {makeClientHash("client.1"), { {"resource.3", {{.rate = 10, .unit = 1}, 10}} }}
    }, 1);
    limiter.resetLimits(std::move(registry));

    // spend weight 3
    limiter.spend("client.1", "resource.3", 3);

    // set lower limit
    limiter.resetLimits(LimitsRegistry{
        {{ makeClientHash("client.1"), { {"resource.3", {{.rate = 5, .unit = 1}, 5}} } }}, 1
    });

    // access weight 2, expect success
    EXPECT_TRUE(limiter.access("client.1", "resource.3", 2));

    // access weight 1, now expect reject
    EXPECT_FALSE(limiter.access("client.1", "resource.3", 1));

    // set higher limit
    limiter.resetLimits(LimitsRegistry{
        {{ makeClientHash("client.1"), { {"resource.3", {{.rate = 10, .unit = 1}, 10}} } }}, 1
    });

    // access weight 5, now expect success again
    EXPECT_TRUE(limiter.access("client.1", "resource.3", 5));

    // access weight 1, now limit is exceeded
    EXPECT_FALSE(limiter.access("client.1", "resource.3", 1));

    // time tick
    timeMeter() += seconds(1);

    // access weight 10, we're good again
    EXPECT_TRUE(limiter.access("client.1", "resource.3", 10));

    // access weight 1, limit exceeded one more time
    EXPECT_FALSE(limiter.access("client.1", "resource.3", 1));
}

Y_UNIT_TEST(late_limits)
{
    ManualClock timeMeter;
    AccessLimiter limiter(std::ref(timeMeter));

    // No limits configuration at start (all requests allowed)

    // clientA
    EXPECT_TRUE(limiter.access("client.A", "resource.1", 10));
    timeMeter() += seconds(1);
    // time passes nothing changes
    EXPECT_TRUE(limiter.access("client.A", "resource.1", 10));

    // clientB
    EXPECT_TRUE(limiter.access("client.B", "resource.1", 10));

    // Limits config arrived
    limiter.resetLimits({
        {{ makeClientHash("client.A"), {{"resource.1", {{.rate = 10, .unit = 1}, 10} }}}}, 1
    });

    // clientA reject, limit 200 already reached (100+100)
    EXPECT_FALSE(limiter.access("client.A", "resource.1", 1));

    timeMeter() += seconds(1);
    // but time passes and we good again
    EXPECT_TRUE(limiter.access("client.A", "resource.1", 9));
    limiter.spend("client.A", "resource.1", 1);
    // until limit is reached
    EXPECT_FALSE(limiter.access("client.A", "resource.1", 1));

    // clientB now gets UndefinedLimit
    EXPECT_THROW(limiter.access("clientB", "resource.1", 1), UndefinedLimit);
}

Y_UNIT_TEST(garbage_collect)
{
    ManualClock timeMeter;
    AccessLimiter limiter(std::ref(timeMeter));
    LimitsRegistry registry ({  // 'anybody' limit to resource.1
        {makeClientHash(""), { {"resource.1", {{.rate = 10, .unit = 1}, 30}} }}
    }, 1);
    limiter.resetLimits(std::move(registry));

    EXPECT_TRUE(limiter.access("client.1", "resource.1", 5));
    EXPECT_TRUE(limiter.access("client.2", "resource.1", 5));
    limiter.spend("client.3", "resource.1", 14);
    EXPECT_TRUE(limiter.access("client.3", "resource.1", 1));

    // check counters state
    EXPECT_EQ(
        limiter.shardCounters(),
        Counters({{ "resource.1", {
            { makeClientHash("client.1"), 5 },
            { makeClientHash("client.2"), 5 },
            { makeClientHash("client.3"), 15 }
        } }})
    );

    timeMeter() += seconds(1);

    limiter.spend("client.1", "resource.1", 2);
    EXPECT_TRUE(limiter.access("client.1", "resource.1", 3));
    limiter.garbageCollectCounters();

    EXPECT_EQ( // client.2 counter thrown into garbage
        limiter.shardCounters(),
        Counters({{ "resource.1", {{ makeClientHash("client.1"), 10 }, { makeClientHash("client.3"), 15 }} }})
    );

    timeMeter() += seconds(1);

    EXPECT_TRUE(limiter.access("client.1", "resource.1", 5));
    limiter.garbageCollectCounters();

    EXPECT_EQ(  // client.3 counter thrown into garbage
        limiter.shardCounters(),
        Counters({{ "resource.1", {{ makeClientHash("client.1"), 15 }} }})
    );

    timeMeter() += seconds(1);

    limiter.garbageCollectCounters();

    // no counters left, just empty resource entry
    EXPECT_EQ(limiter.shardCounters(), Counters({{ "resource.1", {}}}));
}

Y_UNIT_TEST(proto_counters_message)
{
    ManualClock timeMeter;
    AccessLimiter limiter(std::ref(timeMeter));
    // Initialize counters with some values.
    EXPECT_TRUE(limiter.access("client.1", "resource.1", 5));
    // We initialize AccessLimiter state with applyUpdateResponse method as it would requested on start.
    auto pluginState = limiter.createUpdateMessage();  // We create update message for ratelimiter proxy.
    // Suppose proxy responded with some lamport.
    auto responseLamport = 42;
    limiter.applyUpdateResponse(plugin::CountersMessage{.lamport = responseLamport});

    pluginState = limiter.createUpdateMessage();
    EXPECT_EQ(pluginState.counters, Counters({{"resource.1", {{makeClientHash("client.1"), 5}}}}));
    ASSERT_TRUE(pluginState.lamport);
    EXPECT_EQ(*pluginState.lamport, responseLamport + 1);  // lamport incremented on prepare counters message

    UNIT_ASSERT_EXCEPTION_CONTAINS(
        limiter.applyUpdateResponse(plugin::CountersMessage{.lamport = *pluginState.lamport - 10}),
        maps::RuntimeError,
        "incorrect lamport");

    // Update with correct lamport
    EXPECT_NO_THROW(
        limiter.applyUpdateResponse(plugin::CountersMessage {.lamport = *pluginState.lamport}));
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::rate_limiter2::tests
