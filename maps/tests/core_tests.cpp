#include <maps/infra/ratelimiter2/common/include/test_helpers.h>
#include <maps/infra/ratelimiter2/core/include/core.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <maps/libs/common/include/exception.h>

namespace maps::rate_limiter2::tests {

using std::chrono::system_clock;
using std::chrono::seconds;
using std::chrono::steady_clock;

namespace {

struct CoreTester : public Core
{
    CoreTester(
        Core::WallClock clock = system_clock::now,
        seconds syncGap = seconds(10)) : Core("", syncGap, std::move(clock))
    {
    }

    SortedCounters& shard() { return counters_.shardTotal(); }
    SortedCounters& other() { return counters_.otherTotal(); }
    std::shared_ptr<LimitsRegistry> limits() const { return limitsRegistry_; }
};

Core::SyncData makeSyncData(const SortedCounters& counters)
{
    return {.counters=counters};
}

} // anonymous namespace

Y_UNIT_TEST_SUITE(core_test) {

Y_UNIT_TEST(incoming_syncs_and_gaps)
{
    Core core("core", seconds(10)); // after sync gap > 10sec delta dropped

    auto client1 = makeClientHash("client.1");
    auto client2 = makeClientHash("client.2");

    auto a1 = SortedCounters({{"resource.1", {{client1, 10} }}, {"resource.2", {{client2, 10} }}});
    auto a2 = SortedCounters({{"resource.1", {{client1, 20} }}, {"resource.2", {{client2, 100} }}});

    auto b1 = SortedCounters({{"resource.1", {{client1, 50}, {client2, 15}}}});
    auto b2 = SortedCounters({{"resource.1", {{client1, 100}, {client2, 30}}}});

    auto timestamp = Core::Timestamp(steady_clock::now());

    { // first A sync dropped ('cause of sync gap)
        auto response = core.handleSyncRequest("A", makeSyncData(a1), timestamp);
        EXPECT_EQ(response.counters, SortedCounters() - a1);
        EXPECT_EQ(response.limitsVersion, 0);  // no limits set in this test
        EXPECT_EQ(core.childCounters("A"), a1);
        EXPECT_EQ(core.totalCounters(), std::make_pair(SortedCounters(), SortedCounters()));  // check total state
    }
    timestamp += seconds(1);
    { // first B sync
        auto response = core.handleSyncRequest("B", makeSyncData(SortedCounters()), timestamp);
        EXPECT_EQ(response.counters, SortedCounters());
    }
    { // A sync
        auto response = core.handleSyncRequest("A", makeSyncData(a2), timestamp);
        EXPECT_EQ(response.counters, (a2 - a2) - a1);  // 'cause no updates from anyone except A
        EXPECT_EQ(core.childCounters("A"), a2);
        EXPECT_EQ(core.totalCounters(), std::make_pair(a2 - a1, SortedCounters()));  // check total state
    }
    timestamp += seconds(2);
    { // B sync
        auto response = core.handleSyncRequest("B", makeSyncData(b1), timestamp);
        EXPECT_EQ(
            response.counters,  // update from A, but no resource.2
            SortedCounters({{"resource.1", {{client1, 10}, {client2, 0}}}})
        );
        EXPECT_EQ(core.childCounters("B"), b1);
        EXPECT_EQ(core.totalCounters(), std::make_pair(b1 + (a2 - a1), SortedCounters()));  // check state
    }
    timestamp += seconds(30);
    { // B sync after gap, expect delta dropped
        auto response = core.handleSyncRequest("B", makeSyncData(b2), timestamp);
        EXPECT_EQ(
            response.counters,  // (b1 - b2) + (a2 - a1), but no resource.2
            b1 - b2 + SortedCounters({{"resource.1", {{client1, 10}}}})
        );
        EXPECT_EQ(response.limitsVersion, 0);  // still no limits
        EXPECT_EQ(core.childCounters("B"), b2);
        EXPECT_EQ(core.totalCounters(), std::make_pair(b1 + (a2 - a1), SortedCounters()));  // check state
    }
}

Y_UNIT_TEST(outgoing_sync_request_response)
{
    CoreTester core;

    auto client1 = makeClientHash("client.1");
    auto client2 = makeClientHash("client.2");

    auto shardValues = SortedCounters({{"resource.1", {{client1, 10}}}});
    auto otherValues = SortedCounters({{"resource.1", {{client1, 153}}}});

    core.shard() = shardValues;
    core.other() = otherValues;

    auto responseValues = SortedCounters({{"resource.1", {{client1, 222}, {client2, 333}}}});

    Core::SyncData sentRequest{.counters=SortedCounters(), .limitsVersion=-1};
    auto sender = [&sentRequest, &responseValues](const ShardId&, const Core::SyncData& data) {
        sentRequest = data;
        return Core::SyncData{.counters=responseValues, .lamport=*data.lamport};
    };

    core.sendSyncRequest(sender);
    // check what was sent
    EXPECT_EQ(sentRequest.counters, shardValues);
    EXPECT_EQ(sentRequest.limitsVersion, 0);
    // check state: received response in 'other' counters component
    EXPECT_EQ(core.totalCounters(), std::make_pair(shardValues, responseValues));
}

Y_UNIT_TEST(outgoing_sync_overlaps_limits_update)
{
    CoreTester core;

    auto client1 = makeClientHash("client.1");
    auto a = SortedCounters({{"resource.1", {{client1, 10}}}});
    auto b = SortedCounters({{"resource.1", {{client1, 111}}}});

    core.shard() = a;
    EXPECT_EQ(core.limits()->version(), 0);  // starting limits version

    auto sender = [&core, b](const ShardId&, const Core::SyncData&) {
        // limits change while core sending request
        core.reconfigureLimits(LimitsRegistry{LimitsRegistry::Storage(), 153});
        return makeSyncData(b);
    };

    // sync request failed
    UNIT_ASSERT_EXCEPTION_CONTAINS(
        core.sendSyncRequest(sender),
        maps::RuntimeError,
        "Limits version doesn't match"
    );
    // because limits changed
    EXPECT_EQ(core.limits()->version(), 153);
}

Y_UNIT_TEST(limits_update)
{
    ManualClock testClock{system_clock::time_point(seconds(1))};
    CoreTester core{std::ref(testClock)};
    // check initials
    ASSERT_TRUE(core.limits());
    ASSERT_EQ(core.limits()->version(), 0);

    auto client1 = makeClientHash("client.1");
    LimitsRegistry limits10({
        { client1, { {"resource.1", LimitInfo({.rate = 10, .unit = 1}, 50)}} },
    }, 10);
    LimitsRegistry limits11({
        { client1, { {"resource.1", LimitInfo({.rate = 100, .unit = 1}, 50)}} },
    }, 11);

    core.reconfigureLimits(limits10);
    EXPECT_TRUE(core.limits()->storage() == limits10.storage());
    EXPECT_EQ(core.limits()->version(), 10);
    EXPECT_EQ(core.totalCounters(), std::make_pair(SortedCounters(), SortedCounters()));

    // update should adjusts counters
    core.other() = SortedCounters({{"resource.1", {{client1, 10}}}});

    core.reconfigureLimits(limits11);
    EXPECT_EQ(
        core.totalCounters(),
        std::make_pair(SortedCounters(), SortedCounters({{"resource.1", {{client1, 100}}}}))
    );

    // can't update to older version
    UNIT_ASSERT_EXCEPTION_CONTAINS(
        (core.reconfigureLimits(LimitsRegistry({}, 1))),
        maps::RuntimeError,
        "Refuse to accept older limits"
    );

    // update to same version different content
    core.reconfigureLimits(LimitsRegistry({}, 11));
    EXPECT_TRUE(core.limits()->storage().empty());
    EXPECT_EQ(core.limits()->version(), 11);
}

Y_UNIT_TEST(counters_advance)
{
    ManualClock testClock{system_clock::time_point(seconds(1))};
    CoreTester core{std::ref(testClock)};

    // set limits
    auto client1 = makeClientHash("client.1");
    LimitsRegistry limits({
        { client1, {{"resource.1", LimitInfo({.rate = 100, .unit = 1}, 50)}} },
    }, 153);

    core.reconfigureLimits(limits);

    // no changes if empty
    core.advanceCounters();
    EXPECT_EQ(
        core.totalCounters(),
        std::make_pair(SortedCounters(), SortedCounters())
    );

    core.shard() = SortedCounters({{"resource.1", {{client1, 10}}}});
    core.advanceCounters();

    EXPECT_EQ(
        core.totalCounters(),
        std::make_pair(
            SortedCounters({{"resource.1", {{client1, 10}}}}),
            SortedCounters({{"resource.1", {{client1, 90}}}})
        )
    );

    // no shard component
    core.shard() = SortedCounters();
    core.other() = SortedCounters({{"resource.1", {{client1, 10}}}});
    core.advanceCounters();

    EXPECT_EQ(
        core.totalCounters(),
        std::make_pair(
            SortedCounters(),
            SortedCounters({{"resource.1", {{client1, 100}}}})
        )
    );
}

Y_UNIT_TEST(garbage_collection)
{
    ManualClock testClock{system_clock::time_point(seconds(1))};
    CoreTester core{std::ref(testClock), seconds(100)};  // 100sec max sync gap

    auto garbageThreshold = seconds(20);  // child state expires in 20secs

    auto client1 = makeClientHash("client.1");
    auto client2 = makeClientHash("client.2");

    auto rps10 = LimitInfo{{.rate = 10, .unit = 1}, 50};
    core.reconfigureLimits({
        {{makeClientHash(""), {{"resource.1", rps10 }}}}, // default limit
        153
    });

    auto syncTimestamp = steady_clock::now();
    // first syncs dropped by max sync gap
    core.handleSyncRequest("A",
        makeSyncData({{"resource.1", {{client1, 0}}}}), syncTimestamp);
    core.handleSyncRequest("B",
        makeSyncData({{"resource.1", {{client1, 0}, {client2, 0}}}}), syncTimestamp);

    syncTimestamp += seconds(1);
    // now send deltas
    core.handleSyncRequest("A",
        makeSyncData({{"resource.1", {{client1, 20}}}}), syncTimestamp);
    core.handleSyncRequest("B",
        makeSyncData({{"resource.1", {{client1, 20}, {client2, 15}}}}), syncTimestamp);

    core.advanceCounters();
    auto collected = core.garbageCollect(syncTimestamp - garbageThreshold);
    EXPECT_EQ(collected, 0ul);
    // both counters > lowerbound
    EXPECT_EQ(core.shard(), SortedCounters({{"resource.1", {{client1, 40}, {client2, 15}}}}));
    EXPECT_EQ(core.other(), SortedCounters({{"resource.1", {{client1, 0}, {client2, 0}}}}));

    syncTimestamp += seconds(10);
    testClock() += seconds(10);  // now == 11sec
    core.advanceCounters();

    core.handleSyncRequest("B", makeSyncData({{"resource.1", {{client2, 30}}}}), syncTimestamp);
    EXPECT_EQ(core.garbageCollect(syncTimestamp - garbageThreshold), 0ul);
    // client1 < lowerbound, but both counters alive (referenced by children states)
    EXPECT_EQ(core.shard(), SortedCounters({{"resource.1", {{client1, 40}, {client2, 30}}}}));
    EXPECT_EQ(core.other(), SortedCounters({{"resource.1", {{client1, 70}, {client2, 95}}}}));

    syncTimestamp += seconds(10);
    testClock() += seconds(10);  // now == 21sec
    core.advanceCounters();

    EXPECT_EQ(core.garbageCollect(syncTimestamp - garbageThreshold), 1ul);
    EXPECT_EQ(core.childCounters("A"), SortedCounters());  // expect A state gone
    EXPECT_TRUE(core.childCounters("B") != SortedCounters());  // but B still here
    // A is gone, so client1 counter became zombie
    // client1 is dropped, client2 is alive but < lowerbound
    EXPECT_EQ(core.shard(), SortedCounters({{"resource.1", {{client2, 30}}}}));
    EXPECT_EQ(core.other(), SortedCounters({{"resource.1", {{client2, 180}}}}));

    syncTimestamp += seconds(10);
    testClock() += seconds(10);  // now == 31sec
    core.advanceCounters();

    EXPECT_EQ(core.garbageCollect(syncTimestamp - garbageThreshold), 1ul);
    EXPECT_EQ(core.childCounters("B"), SortedCounters());  // B state gone too
    // B is gone, so client2 counter zombie is dropped
    EXPECT_EQ(core.shard(), SortedCounters());
    EXPECT_EQ(core.other(), SortedCounters());
}

Y_UNIT_TEST(lamport_syncs)
{
    CoreTester core;

    auto client1 = makeClientHash("client.1");
    auto client2 = makeClientHash("client.2");

    auto a1 = SortedCounters({{"resource.1", {{client1, 1} }}, {"resource.2", {{client2, 17} }}});
    auto a2 = SortedCounters({{"resource.1", {{client1, 3} }}, {"resource.2", {{client2, 19} }}});
    auto a3 = SortedCounters({{"resource.1", {{client1, 5} }}, {"resource.2", {{client2, 23} }}});
    auto a4 = SortedCounters({{"resource.1", {{client1, 7} }}, {"resource.2", {{client2, 29} }}});
    auto a5 = SortedCounters({{"resource.1", {{client1, 13} }}, {"resource.2", {{client2, 31} }}});

    auto syncTimestamp = Core::Timestamp(steady_clock::now()); // timestamp that is checked for sync gap
    auto agentLamport = 1;
    auto makeRequestData = [&agentLamport](const SortedCounters& counters) -> Core::SyncData {
        return {.counters = counters, .lamport = agentLamport};
    };

    // We have one agent, so other total is always: (An - a1) - An -> -a1
    auto expectedTotal = SortedCounters();
    auto expectedResponse = expectedTotal - a1; // response should contain others total = (total - agent)
    auto expectedChildCounters = a1;  // child A
    { // first A sync dropped ('cause of sync gap)
        auto response = core.handleSyncRequest("A", makeRequestData(a1), syncTimestamp);
        EXPECT_EQ(response.counters, expectedResponse);
        EXPECT_EQ(core.childCounters("A"), expectedChildCounters);
        EXPECT_EQ(core.totalCounters(), std::make_pair(expectedTotal, SortedCounters()));  // no total yet
        ASSERT_TRUE(response.lamport);
        EXPECT_EQ(*response.lamport, agentLamport); // server don't change lamport
    }
    // agent should always increment its lamport before request
    ++agentLamport;
    syncTimestamp += seconds(1);
    {
        expectedTotal += a2 - expectedChildCounters; // +(new values - current values)
        expectedChildCounters = a2; // update current values
        auto response = core.handleSyncRequest("A", makeRequestData(a2), syncTimestamp);
        EXPECT_EQ(response.counters, expectedResponse); // total didn't change because no other agents
        EXPECT_EQ(core.childCounters("A"), expectedChildCounters);
        EXPECT_EQ(core.totalCounters(), std::make_pair(expectedTotal, SortedCounters()));  // check total state
        ASSERT_TRUE(response.lamport);
        EXPECT_EQ(*response.lamport, agentLamport);
    }
    // agent doesn't increment its lamport
    syncTimestamp += seconds(1);
    { // server won't update shard if lamport isn't correct
        // expectedTotal and expectedChildCounters won't change
        auto response = core.handleSyncRequest("A", makeRequestData(a3), syncTimestamp);
        ASSERT_TRUE(response.lamport);
        EXPECT_EQ(*response.lamport, agentLamport); // server responded with its lamport
        EXPECT_EQ(response.counters, expectedTotal - a3); // total - agent counters
        EXPECT_EQ(core.childCounters("A"), expectedChildCounters);  // shard didn't change
        EXPECT_EQ(core.totalCounters(), std::make_pair(expectedTotal, SortedCounters()));  // total didn't change
    }
    ++agentLamport; // agent corrected its lamport
    syncTimestamp += seconds(1);
    { // server will accept new correct lamport
        expectedTotal += a4 - expectedChildCounters;
        expectedChildCounters = a4; // shard updated
        auto response = core.handleSyncRequest("A", makeRequestData(a4), syncTimestamp);
        ASSERT_TRUE(response.lamport);
        EXPECT_EQ(*response.lamport, agentLamport);
        EXPECT_EQ(response.counters, expectedResponse);
        EXPECT_EQ(core.childCounters("A"), expectedChildCounters);
        EXPECT_EQ(core.totalCounters(), std::make_pair(expectedTotal, SortedCounters()));  // check total state
        agentLamport = *response.lamport;
    }
    // server restarts
    CoreTester restartedServer;
    ++agentLamport;
    syncTimestamp += seconds(1);
    {
        expectedResponse = SortedCounters() - a5; // after restart server drops drops all counters
        expectedChildCounters = a5; // shard updated
        // restarted server has no counters yet
        auto response = restartedServer.handleSyncRequest("A", makeRequestData(a5), syncTimestamp);
        ASSERT_TRUE(response.lamport);
        EXPECT_EQ(*response.lamport, agentLamport); // server accepted our lamport
        EXPECT_EQ(response.counters, expectedResponse);
        EXPECT_EQ(restartedServer.childCounters("A"), expectedChildCounters);  // shard updated
        EXPECT_EQ(restartedServer.totalCounters(), std::make_pair(SortedCounters(), SortedCounters()));  // total is empty
    }
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::rate_limiter2::tests
