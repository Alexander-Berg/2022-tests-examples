#include <maps/infra/ratelimiter2/common/include/test_helpers.h>
#include <maps/infra/ratelimiter2/core/include/core.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <boost/uuid/uuid_io.hpp>

namespace maps::rate_limiter2::tests {

using std::chrono::system_clock;
using std::chrono::seconds;
using std::chrono::milliseconds;

namespace {

struct CoreTester : public Core
{
    CoreTester(Core::WallClock clock) : Core("", seconds(5), std::move(clock))
    {
    }

    SortedCounters& shard() { return counters_.shardTotal(); }
    SortedCounters& other() { return counters_.otherTotal(); }
};

} // anonymous namespace

Y_UNIT_TEST_SUITE(peers_test) {

Y_UNIT_TEST(peer2peer)
{
    auto client1 = makeClientHash("client.1");
    auto client2 = makeClientHash("client.2");

    auto limit10RPS = LimitInfo({.rate = 10, .unit = 1}, 50);  // per second
    auto limit500RPM = LimitInfo({.rate = 100, .unit = 60}, 500); // per minute
    LimitsRegistry limits({
        { makeClientHash(""), {{"resource.1", limit500RPM}} },
        { client1, {{"resource.1", limit10RPS},} },
    });

    ManualClock testClock;
    CoreTester coreA(std::ref(testClock));
    CoreTester coreB(std::ref(testClock));

    coreA.reconfigureLimits(limits);
    coreB.reconfigureLimits(limits);

    coreA.shard() = SortedCounters({{"resource.1", {{client1, 900}, {client2, 150}}}});
    coreA.other() = Core::calculateLowerBound(coreA.shard(), limits, testClock());

    coreB.shard() = SortedCounters({{"resource.1", {{client1, 750}}}});
    coreB.other() = Core::calculateLowerBound(coreB.shard(), limits, testClock());

    testClock() += seconds(30);  // time +30sec

    // sending A -> B
    auto overBoundA = coreA.peerData().counters;
    {
        SortedCounters expected({
            {"resource.1", {{client1, 600}, {client2, 100}}}  // after +30sec
        });
        EXPECT_EQ(overBoundA, expected);
    }

    // sending B -> A
    auto overBoundB = coreB.peerData().counters;
    {
        SortedCounters expected({
            {"resource.1", {{client1, 450}}}  // after +30sec
        });
        EXPECT_EQ(overBoundB, expected);
    }

    // receive empty
    auto saved = coreB.other();
    coreB.handlePeerData(SortedCounters());
    EXPECT_EQ(coreB.other(), saved);   // expect no change

    // A -> B receiving
    saved = coreB.shard();

    coreB.handlePeerData(overBoundA);
    EXPECT_EQ(coreB.shard(), saved);  // 'shard' not touched
    EXPECT_EQ(coreB.other() + coreB.shard(), coreA.other() + coreA.shard());

    // B -> A receiving
    saved = coreA.shard() + coreA.other();

    coreA.handlePeerData(overBoundB);
    EXPECT_EQ(coreA.other() + coreA.shard(), saved);  // no changes 'cause total A > total B

    // receiving A->C B->C
    CoreTester coreC(std::ref(testClock));
    coreC.reconfigureLimits(limits);

    coreC.handlePeerData(overBoundA);
    coreC.handlePeerData(overBoundB);
    EXPECT_EQ(coreC.shard(), SortedCounters());  // yep, still empty
    EXPECT_EQ(coreC.other(), coreA.other() + coreA.shard()); // all goes to 'other'

    // corner cases tests

    // check send if shard component is empty
    saved = SortedCounters();
    std::swap(coreA.shard(), saved);  // drop shard
    overBoundA = coreA.peerData().counters;
    EXPECT_EQ(overBoundA, SortedCounters());  // expect empty
    std::swap(coreA.shard(), saved);  // restore shard

    // check send if other component is empty
    std::swap(coreA.other(), saved);  // drop other
    overBoundA = coreA.peerData().counters;
    EXPECT_EQ(overBoundA, SortedCounters()); // expect empty
    std::swap(coreA.other(), saved);  // restore other
}


Y_UNIT_TEST(calculateLowerBound_corner_cases)
{
    auto now = system_clock::now();
    auto client1 = makeClientHash("client.1");

    auto loBound = Core::calculateLowerBound(SortedCounters(), LimitsRegistry(), now);
    EXPECT_EQ(loBound, SortedCounters());

    LimitsRegistry limits({
        { client1, {{"resource.1", LimitInfo({.rate = 153, .unit = 1}, 153)}} }
    });

    loBound = Core::calculateLowerBound(SortedCounters(), limits, now);
    EXPECT_EQ(loBound, SortedCounters());

    // test no limit defined
    SortedCounters counters ({
        {"resource.1", {{client1, 1}}},
        {"resource.hbz", {}}  // just hanging resource
    });

    loBound = Core::calculateLowerBound(counters, LimitsRegistry(), now);
    SortedCounters expected({
        {"resource.1", {{client1, LimitInfo::UNDEFINED.lowerBound(now)}}}, // expect lower bound by LimitInfo::UNDEFINED
        {"resource.hbz", {}}  // still hanging
    });
    EXPECT_EQ(loBound, expected);
    EXPECT_TRUE(loBound != counters);
}


Y_UNIT_TEST(calculate_lower_bound)
{
    auto client1 = makeClientHash("client.1");
    auto clientHbz = makeClientHash("client.hbz");

    auto limit10RPS = LimitInfo({.rate = 10, .unit = 1}, 50);
    auto limit100RPS = LimitInfo({.rate = 100, .unit = 1}, 500);
    auto limit300RPH = LimitInfo({.rate = 10, .unit = 1}, 50);

    LimitsRegistry limits({
        { makeClientHash(""), {{"resource.1", limit10RPS}} },  // 'anybody' limit to 'resource.1'
        { client1, {
            {"resource.1", limit100RPS},
            {"resource.2", limit300RPH}  // per hour
        }},
    });

    SortedCounters counters({
        {"resource.1", {{client1, 1}, {clientHbz, 1}} },
        {"resource.2", {{client1, 1}, {clientHbz, 1}} }
    });

    auto now = system_clock::now();

    auto loBound = Core::calculateLowerBound(counters, limits, now);

    SortedCounters expected({
        {"resource.1", {
            { client1, limit100RPS.lowerBound(now)},
            { clientHbz, limit10RPS.lowerBound(now)}
        }},
        {"resource.2", {
            { client1, limit300RPH.lowerBound(now)},
            { clientHbz, LimitInfo::UNDEFINED.lowerBound(now)}
        }}
    });

    EXPECT_EQ(loBound, expected);
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::rate_limiter2::tests
