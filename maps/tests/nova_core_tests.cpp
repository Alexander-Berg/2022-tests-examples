#include <maps/infra/ratelimiter2/common/include/test_helpers.h>
#include <maps/infra/ratelimiter2/core/include/nova_core.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::rate_limiter2::tests {

using std::chrono::system_clock;
using std::chrono::seconds;
using std::chrono::steady_clock;

namespace {

struct CoreTester : public NovaCore
{
    CoreTester(
        NovaCore::WallClock clock = system_clock::now,
        seconds syncGap = seconds(10)) : NovaCore("test", syncGap, std::move(clock))
    {
    }

    const auto& shard() const { return counters_.shardTotal(); }
    const auto& other() const { return counters_.otherTotal(); }
    const auto& limits() const { return counters_.limits(); }
    const auto& lamport() const { return lamport_; }
};

} // anonymous namespace

Y_UNIT_TEST_SUITE(core_nova_test) {

Y_UNIT_TEST(incoming_syncs_and_gaps)
{
    // NB: lamport ignored in this test
    CoreTester core(system_clock::now, seconds(10)); // after sync gap > 10sec delta dropped

    auto client1 = makeClientHash("client.1");
    auto client2 = makeClientHash("client.2");
    // NB: No limits/generations changes during this test
    // Therefore 'other' always empty  because no limits upgrade or counters advance
    auto limitA = Limit{.rate = 100, .unit = 1, .gen=1};
    auto limitB = Limit{.rate = 10, .unit = 60, .gen=3};

    NovaCore::SyncData a1 = {
        .counters = {{"resource1", {{client1, 10}}}, {"resource2", {{client2, 10}}}},
        .limits = {{"resource1", {{client1, limitA}}}, {"resource2", {{client2, limitB}}}}
    };
    NovaCore::SyncData a2 = {
        .counters = {{"resource1", {{client1, 20}}}, {"resource2", {{client2, 100}}}},
        .limits = {{"resource1", {{client1, limitA}}}, {"resource2", {{client2, limitB}}}}
    };

    NovaCore::SyncData b1 = {
        .counters = {{"resource1", {{client1, 50}, {client2, 15}}}},
        .limits = {{"resource1", {{client1, limitA}, {client2, limitB}}}}
    };
    NovaCore::SyncData b2 = {
        .counters = {{"resource1", {{client1, 100}, {client2, 30}}}},
        .limits = {{"resource1", {{client1, limitA}, {client2, limitB}}}}
    };

    auto timestamp = NovaCore::Timestamp(steady_clock::now());

    {   // first A sync dropped ('cause of sync gap)
        auto response = core.handleSyncRequest("A", a1, timestamp);
        EXPECT_EQ(response.counters, SortedCounters() - a1.counters);
        EXPECT_EQ(core.childCounters("A"), a1.counters);
        EXPECT_EQ(core.shard(), SortedCounters());
        EXPECT_EQ(core.other(), SortedCounters());
        EXPECT_EQ(core.limits(), a1.limits);
    }
    timestamp += seconds(1);
    {   // first B sync
        auto response = core.handleSyncRequest("B", {}, timestamp);
        EXPECT_EQ(response.counters, SortedCounters());
        EXPECT_EQ(response.limits, SortedLimits());
    }
    {   // A sync
        auto response = core.handleSyncRequest("A", a2, timestamp);
        EXPECT_EQ(response.counters, (a2.counters - a2.counters) - a1.counters);  // no updates from anyone except A
        EXPECT_EQ(core.childCounters("A"), a2.counters);
        EXPECT_EQ(core.shard(), a2.counters - a1.counters);
        EXPECT_EQ(core.other(), SortedCounters());
        EXPECT_EQ(core.limits(), a2.limits);
    }
    timestamp += seconds(2);
    {   // B sync
        auto response = core.handleSyncRequest("B", b1, timestamp);
        EXPECT_EQ(
            response.counters,  // update from A, but no resource2
            SortedCounters({{"resource1", {{client1, 10}, {client2, 0}}}})
        );
        EXPECT_EQ(
            response.limits,
            SortedLimits({{"resource1", {{client1, limitA}, {client2, limitB}}}})
        );
        EXPECT_EQ(core.childCounters("B"), b1.counters);
        EXPECT_EQ(core.shard(), b1.counters + a2.counters - a1.counters);
        EXPECT_EQ(core.other(), SortedCounters());
    }
    timestamp += seconds(30);
    {   // B sync after gap, expect delta dropped
        auto response = core.handleSyncRequest("B", b2, timestamp);
        EXPECT_EQ(
            response.counters,  // (b1 - b2) + (a2 - a1), but no resource2
            b1.counters - b2.counters + SortedCounters({{"resource1", {{client1, 10}}}})
        );
        EXPECT_EQ(
            response.limits,
            SortedLimits({{"resource1", {{client1, limitA}, {client2, limitB}}}})
        );
        EXPECT_EQ(core.childCounters("B"), b2.counters);
        EXPECT_EQ(core.shard(), b1.counters + a2.counters - a1.counters);
        EXPECT_EQ(core.other(), SortedCounters());
    }
}

Y_UNIT_TEST(incoming_syncs_and_lamports)
{
    CoreTester core;

    auto client1 = makeClientHash("client.1");
    auto client2 = makeClientHash("client.2");
    auto limitA = Limit{.rate = 100, .unit = 1, .gen=1};
    auto limitB = Limit{.rate = 10, .unit = 60, .gen=3};

    // NB: same limits/generations collection for all agent requests
    SortedLimits limits{{"resource1", {{client1, limitA}}}, {"resource2", {{client2, limitB}}}};

    SortedCounters a1{{"resource1", {{client1, 1}}}, {"resource2", {{client2, 17}}}};
    SortedCounters a2{{"resource1", {{client1, 3}}}, {"resource2", {{client2, 19}}}};
    SortedCounters a3{{"resource1", {{client1, 5}}}, {"resource2", {{client2, 23}}}};
    SortedCounters a4{{"resource1", {{client1, 7}}}, {"resource2", {{client2, 29}}}};
    SortedCounters a5{{"resource1", {{client1, 13}}}, {"resource2", {{client2, 31}}}};

    auto syncTimestamp = NovaCore::Timestamp(steady_clock::now()); // timestamp that is checked for sync gap
    auto agentLamport = 1;

    SortedCounters expectedTotal;
    SortedCounters expectedChild;
    {   // First A sync dropped by sync gap
        auto response = core.handleSyncRequest("A", {a1, limits, agentLamport}, syncTimestamp);
        expectedTotal = SortedCounters();
        expectedChild = a1;

        EXPECT_EQ(response.counters, expectedTotal - a1);  // response is (total - agent)
        EXPECT_EQ(core.childCounters("A"), expectedChild);
        EXPECT_EQ(core.shard(), expectedTotal);
        EXPECT_EQ(core.limits(), limits);

        ASSERT_TRUE(response.lamport);
        EXPECT_EQ(*response.lamport, agentLamport); // server responds with current lamport
    }
    // agent always increments lamport between requests
    ++agentLamport;
    syncTimestamp += seconds(1);
    {
        auto response = core.handleSyncRequest("A", {a2, limits, agentLamport}, syncTimestamp);
        expectedTotal += a2 - expectedChild; // += (new - current)
        expectedChild = a2;

        EXPECT_EQ(response.counters, expectedTotal - a2);   // response is (total - agent)

        EXPECT_EQ(core.childCounters("A"), expectedChild);
        EXPECT_EQ(core.shard(), expectedTotal);
        EXPECT_EQ(core.limits(), limits);

        ASSERT_TRUE(response.lamport);
        EXPECT_EQ(*response.lamport, agentLamport);
    }
    syncTimestamp += seconds(1);
    {   // Test incorrect lamport from agent (no increment)
        // Server drops update with incorrect lamport, so expectedTotal and expectedChild not changed
        auto response = core.handleSyncRequest("A", {a3, limits, agentLamport}, syncTimestamp);
        ASSERT_TRUE(response.lamport);
        EXPECT_EQ(*response.lamport, agentLamport); // server responded with its lamport

        EXPECT_EQ(response.counters, expectedTotal - a3); // response is (total - agent)
        EXPECT_EQ(core.childCounters("A"), expectedChild); // no change in child state
        EXPECT_EQ(core.shard(), expectedTotal); // no change in shard/other
        EXPECT_EQ(core.other(), SortedCounters());  // other stays empty in this test
    }
    ++agentLamport; // agent lamport increment
    syncTimestamp += seconds(1);
    {  // Server accepts update with correct lamport
        auto response = core.handleSyncRequest("A", {a4, limits, agentLamport}, syncTimestamp);
        expectedTotal += a4 - expectedChild;
        expectedChild = a4;

        ASSERT_TRUE(response.lamport);
        EXPECT_EQ(*response.lamport, agentLamport);

        EXPECT_EQ(response.counters, expectedTotal - a4); // response is (total - agent)
        EXPECT_EQ(core.childCounters("A"), expectedChild);
        EXPECT_EQ(core.shard(), expectedTotal);
        EXPECT_EQ(core.other(), SortedCounters());  // other stays empty in this test
        EXPECT_EQ(core.limits(), limits);
    }
    ++agentLamport;
    syncTimestamp += seconds(1);
    {   // Imitate server restart
        CoreTester freshCore;
        // First sync request after restart is dropped
        auto response = freshCore.handleSyncRequest("A", {a5, limits, agentLamport}, syncTimestamp);
        ASSERT_TRUE(response.lamport);
        EXPECT_EQ(*response.lamport, agentLamport); // server accepted our lamport

        EXPECT_EQ(response.counters, SortedCounters() - a5);
        EXPECT_EQ(freshCore.childCounters("A"), a5);
        EXPECT_EQ(freshCore.shard(), SortedCounters());
        EXPECT_EQ(core.other(), SortedCounters());  // other stays empty in this test
    }
}

Y_UNIT_TEST(incoming_syncs_and_generations)
{
    ManualClock testClock{system_clock::time_point(seconds(10))};
    CoreTester core{std::ref(testClock)};
    // NB: lamport ignored in this test

    auto client1 = makeClientHash("client.1");
    auto client2 = makeClientHash("client.2");
    auto limitX = Limit{.rate = 120, .unit = 60, .gen=1};
    auto limitY = Limit{.rate = 10, .unit = 1, .gen=2};

    auto a1 = NovaCore::SyncData {
        .counters = SortedCounters{{"resource1", {{client1, 10}}}},
        .limits = SortedLimits{{"resource1", {{client1, limitX}}}}
    };
    // New limitY for client1 in a2
    auto a2 = NovaCore::SyncData {
        .counters = SortedCounters{{"resource1", {{client1, 20}}}},
        .limits = SortedLimits{{"resource1", {{client1, limitY}}}}
    };

    auto b1 = NovaCore::SyncData {
        .counters = SortedCounters{{"resource1", {{client1, 50}, {client2, 15}}}},
        .limits = SortedLimits{{"resource1", {{client1, limitX}, {client2, limitX}}}}
    };
    auto b2 = NovaCore::SyncData {
        .counters = SortedCounters{{"resource1", {{client1, 100}, {client2, 30}}}},
        .limits = SortedLimits{{"resource1", {{client1, limitX}, {client2, limitX}}}}
    };

    auto timestamp = NovaCore::Timestamp(steady_clock::now());

    // first sync dropped ('cause of sync gap)
    core.handleSyncRequest("A", {}, timestamp);
    core.handleSyncRequest("B", {}, timestamp);

    timestamp += seconds(1);
    {   // A sync a1
        auto response = core.handleSyncRequest("A", a1, timestamp);
        EXPECT_EQ(response.counters, a1.counters - a1.counters);
        //TODO: EXPECT_EQ(response.limits, a1.counters - a1.counters);
        EXPECT_EQ(core.childCounters("A"), a1.counters);
        EXPECT_EQ(core.shard(), a1.counters);
        EXPECT_EQ(core.other(), SortedCounters());
        EXPECT_EQ(
            core.limits(),
            SortedLimits({{"resource1", {{client1, limitX}}}}) // a1.limits
        );
    }
    {   // B sync with b1
        auto response = core.handleSyncRequest("B", b1, timestamp);
        EXPECT_EQ(response.counters, a1.counters + b1.counters - b1.counters);
        //TODO: EXPECT_EQ(response.limits, a1.counters - a1.counters);
        EXPECT_EQ(core.childCounters("B"), b1.counters);
        EXPECT_EQ(core.shard(), b1.counters + a1.counters);
        EXPECT_EQ(core.other(), SortedCounters());
        EXPECT_EQ(
            core.limits(),
            SortedLimits({{"resource1", {{client1, limitX}, {client2, limitX}}}})
        );
    }

    // A gets new limits (limitY)
    // B still has older (limitX)
    // Limit conversion diff is (Y.loBound - X.lowBound)
    auto conversionDiff = limitY.lowerBound(testClock()) -
            limitX.lowerBound(testClock());

    timestamp += seconds(1);
    {   // A sync will upgrade core generation
        auto response = core.handleSyncRequest("A", a2, timestamp);
        EXPECT_EQ(
            response.counters,
            SortedCounters({
                {"resource1", {
                    {client1, 50 + conversionDiff},
                    {client2, 15}, // no conversion for client2
                }}
            })
        );
        EXPECT_EQ(
            response.limits,
            SortedLimits({{"resource1", {{client1, limitY}, {client2, limitX}}}})
        );

        EXPECT_EQ(core.childCounters("A"), a2.counters);
        EXPECT_EQ(core.shard(), b1.counters + a2.counters);
        EXPECT_EQ(
            core.other(), // other has client1 entry after conversion
            SortedCounters({{"resource1", {{client1, conversionDiff}}}})
        );
        EXPECT_EQ(
            core.limits(), // client1 has new limitY
            SortedLimits({{"resource1", {{client1, limitY}, {client2, limitX}}}})
        );
    }

    {   // B sync (new limitY in response)
        auto response = core.handleSyncRequest("B", b2, timestamp);
        EXPECT_EQ(
            response.counters,
            SortedCounters({  // a2 + conversionDiff for client1
                {"resource1", {
                    {client1, 20 + conversionDiff},
                    {client2, 0},
                }}
            })
        );
        EXPECT_EQ(
            response.limits,  // NB: new limitY in response
            SortedLimits({{"resource1", {{client1, limitY}, {client2, limitX}}}})
        );
        EXPECT_EQ(core.childCounters("B"), b2.counters);
        EXPECT_EQ(core.shard(), b2.counters + a2.counters);
        EXPECT_EQ(
            core.other(), // still just client1 entry in the other
            SortedCounters({{"resource1", {{client1, conversionDiff}}}})
        );
        EXPECT_EQ(
            core.limits(), // limits not changed
            SortedLimits({{"resource1", {{client1, limitY}, {client2, limitX}}}})
        );
    }
}

Y_UNIT_TEST(outgoing_sync_request_and_response)
{
    auto client1 = makeClientHash("client.1");
    auto client2 = makeClientHash("client.2");
    auto client3 = makeClientHash("client.3");
    auto rps10 = Limit{.rate = 10, .unit = 1, .gen=1};   // oldest
    auto rpm300 = Limit{.rate = 300, .unit = 60, .gen=2};
    auto rps50 = Limit{.rate = 50, .unit = 1, .gen=3}; // newest

    // NB: We're at 10sec from the beginning of epoch
    ManualClock testClock{system_clock::time_point(seconds(10))};
    // Setup core state
    CoreTester core{std::ref(testClock)};
    auto timestamp = NovaCore::Timestamp(steady_clock::now());
    core.handleSyncRequest("A", {}, timestamp);  // overcome max_sync_gap
    core.handleSyncRequest("A", {
        .counters = {{"resA", {{client1, 10}, {client2, 20}}}},
        .limits = {{"resA", {{client1, rpm300}, {client2, rpm300}}}}
        },
        timestamp + seconds(1)
    );
    core.advanceCounters();

    // Check 'other' and lamport before sync
    EXPECT_EQ(
        core.other(),
        SortedCounters({  // rpm300.loBound(10sec) - shard
            {"resA", {{client1, 40}, {client2, 30}}}
        })
    );
    EXPECT_EQ(core.lamport(), 0); // zero lamport 'cause no sync requests yet

    // Mock sender emulates responses to sync request
    auto mockSender = [&](const ShardId&, const NovaCore::SyncData& requestData) {
        // Check core sends current 'shard' values, limits and lamport
        EXPECT_EQ(requestData.counters, core.shard());
        EXPECT_EQ(requestData.limits, core.limits());
        EXPECT_EQ(requestData.lamport, core.lamport());

        // 2 upstreams responses
        return  std::vector<NovaCore::SyncData>{
            NovaCore::SyncData {
                .counters = SortedCounters{
                    {"resA", {{client1, 505}}},
                },
                .limits = SortedLimits{
                    {"resA", {{client1, rps50}}}  // client1 newer limit
                },
                .lamport = 153
            },
            NovaCore::SyncData {
                .counters = SortedCounters{
                    {"resA", {{client1, 111}, {client3, 666}}}
                },
                .limits = SortedLimits{  // client1 older limit, client3 never seen before
                    {"resA", {{client1, rps10}, {client3, rps50}}}
                },
                .lamport = 13  // lower lamport on this upstream
            }
        };
    };
    // Send request and receive responses
    core.sendSyncRequest(mockSender);

    // Check result state
    EXPECT_EQ(core.lamport(),  153);  // max of received lamports
    EXPECT_EQ(
        core.shard(),  // Expect shard not changed
        SortedCounters({
            {"resA", {{client1, 10}, {client2, 20}}}
        })
    );
    EXPECT_EQ(
        core.limits(),  // Expect same limits, but new client3 entry added
        SortedLimits({
            {"resA", {{client1, rpm300}, {client2, rpm300}, {client3, rps50}}}
        })
    );
    EXPECT_EQ(
        core.other(),  // Expect 'other' contain aggregate from both received responses
        SortedCounters({
            {"resA", {
                {client1, 61},   // max(505, 111 + 50*10/1 - 10*10/1) + 300*10/60 - 50*10/1
                {client3, 666}}  // taken 'as is' for fresh entries
            }
        })
    );
}

Y_UNIT_TEST(peer2peer)
{
    auto client1 = makeClientHash("client.1");
    auto client2 = makeClientHash("client.2");
    auto rps10 = Limit{.rate = 10, .unit = 1, .gen=1};
    auto rpm100 = Limit{.rate = 100, .unit = 60, .gen=1};

    ManualClock testClock;
    CoreTester coreA(std::ref(testClock));
    CoreTester coreB(std::ref(testClock));
    // Set for both cores total > loBound
    auto timestamp = NovaCore::Timestamp(steady_clock::now());
    // NB: first sync dropped by sync gap
    coreA.handleSyncRequest(
        "AA", {
            .counters = {{"resA", {{client1, 0}}}, {"resB", {{client2, 0}}}},
            .limits = {{"resA", {{client1, rps10}}}, {"resB", {{client2, rpm100}}}}
        }, timestamp
    );
    coreA.advanceCounters();
    coreA.handleSyncRequest(
        "AA", {
            .counters = {{"resA", {{client1, 900}}}, {"resB", {{client2, 150}}}},
            .limits = {{"resA", {{client1, rps10}}}, {"resB", {{client2, rpm100}}}}
        }, timestamp + seconds(1)
    );

    coreB.handleSyncRequest(
        "BB", {
            .counters = {{"resA", {{client1, 0}}}},
            .limits = {{"resA", {{client1, rps10}}}}
        }, timestamp
    );
    coreB.advanceCounters();
    coreB.handleSyncRequest(
        "BB", {
            .counters = {{"resA", {{client1, 750}}}},
            .limits = {{"resA", {{client1, rps10}}}}
        }, timestamp + seconds(1)
    );

    testClock() += seconds(30);  // time +30sec

    // sending A -> B
    auto peerDataA = coreA.peerData();
    EXPECT_EQ(
        peerDataA.counters, // after +30sec
        SortedCounters({
            {"resA", {{client1, 600 + rps10.lowerBound(testClock())}}},
            {"resB", {{client2, 100 + rpm100.lowerBound(testClock())}}}
        })
    );
    EXPECT_EQ(peerDataA.limits, coreA.limits());

    // sending B -> A
    auto peerDataB = coreB.peerData();
    EXPECT_EQ(
        peerDataB.counters, // after +30sec
        SortedCounters({
            {"resA", {{client1, 450 + rps10.lowerBound(testClock())}}},
        })
    );
    EXPECT_EQ(peerDataB.limits, coreB.limits());

    {  // A -> B receiving
        coreB.handlePeerData(peerDataA);
        EXPECT_EQ(coreB.other() + coreB.shard(), coreA.other() + coreA.shard());
    }
    {  // B -> A receiving
        auto savedTotal = coreA.shard() + coreA.other();
        coreA.handlePeerData(peerDataB);
        // no changes 'cause total A > total B
        EXPECT_EQ(coreA.other() + coreA.shard(), savedTotal);
    }

    // receiving A->C B->C
    CoreTester coreC(std::ref(testClock));

    coreC.handlePeerData(peerDataA);
    coreC.handlePeerData(peerDataB);
    EXPECT_EQ(coreC.shard(), SortedCounters());  // yep, still empty
    EXPECT_EQ(coreC.other(), coreA.other() + coreA.shard()); // all goes to 'other'
    EXPECT_EQ(
        coreC.limits(),
        SortedLimits({{"resA", {{client1, rps10}}}, {"resB", {{client2, rpm100}}}})
    );

    // Test p2p sync empty if total < loBound
    testClock() += seconds(600);  // time +10 minutes
    {
        coreA.advanceCounters();
        peerDataA = coreA.peerData();
        EXPECT_EQ(peerDataA.counters, SortedCounters());

        coreB.advanceCounters();
        peerDataB = coreA.peerData();
        EXPECT_EQ(peerDataB.counters, SortedCounters());
    }
}

Y_UNIT_TEST(peer2peer_generations)
{
    auto client1 = makeClientHash("client.1");
    auto client2 = makeClientHash("client.2");
    auto limitX = Limit{.rate = 10, .unit = 1, .gen=1};
    auto limitY = Limit{.rate = 300, .unit = 60, .gen=2};

    // We're at 30secs from beginning of the epoch
    ManualClock testClock{system_clock::time_point(seconds(30))};
    CoreTester coreA(std::ref(testClock));
    CoreTester coreB(std::ref(testClock));

    // Set for both cores total > loBound
    auto timestamp = NovaCore::Timestamp(steady_clock::now());
    coreA.handleSyncRequest(
        "AA", {
            .counters = {{"resA", {{client1, 0}}}, {"resB", {{client2, 0}}}},
            .limits = {{"resA", {{client1, limitX}}}, {"resB", {{client2, limitY}}}}
        }, timestamp
    );
    coreA.advanceCounters();
    coreA.handleSyncRequest(
        "AA", {
            .counters = {{"resA", {{client1, 900}}}, {"resB", {{client2, 150}}}},
            .limits = {{"resA", {{client1, limitX}}}, {"resB", {{client2, limitY}}}}
        }, timestamp + seconds(1)
    );

    coreB.handleSyncRequest(
        "BB", {
            .counters = {{"resA", {{client1, 0}}}, {"resB", {{client2, 0}}}},
            .limits = {{"resA", {{client1, limitY}}}, {"resB", {{client2, limitX}}}}
        }, timestamp
    );
    coreB.advanceCounters();
    coreB.handleSyncRequest(
        "BB", {
            .counters = {{"resA", {{client1, 750}}}, {"resB", {{client2, 250}}}},
            .limits = {{"resA", {{client1, limitY}}}, {"resB", {{client2, limitX}}}}
        }, timestamp + seconds(1)
    );

    auto peerDataA = coreA.peerData();
    auto peerDataB = coreB.peerData();
    coreA.handlePeerData(peerDataB);
    coreB.handlePeerData(peerDataA);

    // Expect both cores have same limits and totals
    EXPECT_EQ(coreA.shard() + coreA.other(), coreB.shard() + coreB.other());
    EXPECT_EQ(coreA.limits(), coreB.limits());
    // Check internals
    EXPECT_EQ(
        coreA.other(),
        SortedCounters({{"resA", {{client1, 150}}}, {"resB", {{client2, 250}}}})
    );
    EXPECT_EQ(
        coreB.other(),
        SortedCounters({{"resA", {{client1, 300}}}, {"resB", {{client2, 150}}}})
    );
}

Y_UNIT_TEST(garbage_collection)
{
    ManualClock testClock{system_clock::time_point(seconds(1))};
    CoreTester core{std::ref(testClock)};

    auto garbageThreshold = seconds(15);

    auto client1 = makeClientHash("client.1");
    auto client2 = makeClientHash("client.2");
    auto rps10 = Limit{.rate = 10, .unit = 1, .gen=1};
    auto rpm100 = Limit{.rate = 100, .unit = 60, .gen=1};

    auto timestamp = NovaCore::Timestamp(steady_clock::now());
    // First sync dropped
    core.handleSyncRequest("A", {}, timestamp);
    core.handleSyncRequest("B", {}, timestamp);

    timestamp += seconds(1);
    NovaCore::SyncData a {
        .counters = SortedCounters{{"resource.1", {{client1, 5}}}},
        .limits = SortedLimits{{"resource.1", {{client1, rps10}}}},
    };
    core.handleSyncRequest("A", a, timestamp);

    timestamp += seconds(5);  // B update is 5 sec later
    NovaCore::SyncData b {
        .counters = SortedCounters{{"resource.1", {{client1, 5}, {client2, 30}}}},
        .limits = SortedLimits{{"resource.1", {{client1, rps10}, {client2, rpm100}}}},
    };
    core.handleSyncRequest("B", b, timestamp);

    core.garbageCollect(timestamp - garbageThreshold);
    EXPECT_NE(core.childCounters("A"), SortedCounters());
    EXPECT_NE(core.childCounters("B"), SortedCounters());

    timestamp += seconds(10);
    // Now A is dropped, but B still kept
    core.garbageCollect(timestamp - garbageThreshold);
    EXPECT_EQ(core.childCounters("A"), SortedCounters());
    EXPECT_NE(core.childCounters("B"), SortedCounters());

    timestamp += seconds(10);
    // Now B dropped too
    core.garbageCollect(timestamp - garbageThreshold);
    EXPECT_EQ(core.childCounters("A"), SortedCounters());
    EXPECT_EQ(core.childCounters("B"), SortedCounters());
}

Y_UNIT_TEST(metrics)
{
    CoreTester core(
        system_clock::now, std::chrono::seconds(10));
    auto client1 = makeClientHash("client.1");
    auto client2 = makeClientHash("client.2");

    auto limitA = Limit{.rate = 100, .unit = 1, .gen = 1};
    auto limitB = Limit{.rate = 10, .unit = 60, .gen = 3};

    NovaCore::SyncData a1 = {
        .counters =
            {{"resource1", {{client1, 10}}}, {"resource2", {{client2, 10}}}},
        .limits = {
            {"resource1", {{client1, limitA}}},
            {"resource2", {{client2, limitB}}}}};

    auto timestamp = NovaCore::Timestamp(steady_clock::now());

    { // first A sync dropped ('cause of sync gap)
        core.handleSyncRequest("A", {}, timestamp);
        auto response = core.handleSyncRequest("A", a1, timestamp + std::chrono::seconds(1));
        EXPECT_EQ(response.counters, a1.counters - a1.counters);
        EXPECT_EQ(core.shard(), a1.counters);
        EXPECT_EQ(core.limits(), a1.limits);

        EXPECT_EQ(boost::lexical_cast<std::string>(core.metrics()),
            "[[\"022ac64d-9ae8-5dba-8384-ecbd262fae9d@resource2_v2_dxxm\",10]," \
            "[\"340b5be0-94db-50a1-a509-adcde7cd0604@resource1_v2_dxxm\",10]," \
            "[\"340b5be0-94db-50a1-a509-adcde7cd0604@resource1_v2_quota_annn\",100]," \
            "[\"022ac64d-9ae8-5dba-8384-ecbd262fae9d@resource2_v2_quota_annn\",10]," \
            "[\"number_of_counters_v2_axxv\",2]]");
    }
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::rate_limiter2::tests
