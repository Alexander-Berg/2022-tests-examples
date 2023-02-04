#include <maps/infra/ratelimiter2/common/include/counters.h>
#include <maps/infra/ratelimiter2/common/include/sharded_counter.h>
#include <maps/infra/ratelimiter2/common/include/sorted_counters.h>
#include <maps/infra/ratelimiter2/common/include/test_helpers.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::rate_limiter2::tests {

namespace {

// The way changes propagated upstream
template<typename T>
void propagateChanges(ShardedCounter<T>& child, ShardedCounter<T>& parent)
{
    const auto& update = child.shardTotal();
    parent.downstreamUpdate(child.id(), update);
    child.otherTotal() = parent.downstreamResponse(update);
}

} // anonymous namespace

Y_UNIT_TEST_SUITE(shared_counter_test) {

Y_UNIT_TEST(single_counter)
{
    using CountersType = impl::AdditiveMap<std::string, int>;  // <id, value>
    ShardedCounter<CountersType> worker1{"w1"}, worker2{"w2"}, proxy{"proxy"}, server{"srv"};

    worker1.shardTotal() += CountersType({{"c1", 1}});
    worker2.shardTotal() += CountersType({{"c1", 5}});

    propagateChanges(worker1, proxy); // w1 -> proxy

    propagateChanges(worker2, proxy); // w2 -> proxy

    propagateChanges(proxy, server);  // proxy -> server

    // check workers state
    EXPECT_EQ(worker1.shardTotal(), CountersType({{"c1", 1}}));
    EXPECT_EQ(worker1.otherTotal(), CountersType({{"c1", 0}}));  // worker1 sync was before worker2

    EXPECT_EQ(worker2.shardTotal(), CountersType({{"c1", 5}}));
    EXPECT_EQ(worker2.otherTotal(), CountersType({{"c1", 1}}));

    { // check proxy state
        EXPECT_EQ(proxy.shardTotal(), CountersType({{"c1", 6}}));
        EXPECT_EQ(proxy.otherTotal(), CountersType({{"c1", 0}}));
        EXPECT_EQ(proxy.childCounters("w1"), CountersType({{"c1", 1}}));
        EXPECT_EQ(proxy.childCounters("w2"), CountersType({{"c1", 5}}));
    }

    { // check server state
        EXPECT_EQ(server.shardTotal(), CountersType({{"c1", 6}}));
        EXPECT_EQ(server.otherTotal(), CountersType());
        EXPECT_EQ(server.childCounters("proxy"), CountersType({{"c1", 6}}));
    }
}


Y_UNIT_TEST(complex_counter)
{
    const auto client1 = makeClientHash("client.1");
    const auto client2 = makeClientHash("client.2");

    ShardedCounter<> worker1{"w1"}, worker2{"w2"}, proxy{"proxy"};

    auto upd1 = Counters({
        {"resource.1", { {client1, 1} }},
        {"resource.2", { {client2, 2} }}
    });

    worker1.shardTotal() += upd1;

    auto upd2 = Counters({
        {"resource.1", { {client1, 2}, {client2, 1} }},
        {"resource.2", { {client1, 1} }}
    });
    worker2.shardTotal() += upd2;

    propagateChanges(worker1, proxy); // w1 -> proxy

    propagateChanges(worker2, proxy); // w2 -> proxy

    propagateChanges(worker1, proxy); // w1 -> proxy again (to receive w2 update)

    // check workers state
    EXPECT_EQ(worker1.shardTotal(), upd1);
    EXPECT_EQ(worker1.otherTotal(), upd1+upd2-upd1);  // worker1 sync was before worker2

    EXPECT_TRUE(worker2.shardTotal() == upd2);
    {
        // NB: currently (upd1+upd2-upd2 != upd1) 'cause zero counters not dropped (see Counters type)
        EXPECT_EQ(worker2.otherTotal(), upd1+upd2-upd2);
    }

    EXPECT_TRUE(worker1.shardTotal() + worker1.otherTotal() == worker2.shardTotal() + worker2.otherTotal());
    EXPECT_TRUE(worker1.shardTotal() + worker1.otherTotal() == upd1 + upd2);

    { // check proxy state
        EXPECT_TRUE(proxy.shardTotal() == upd1+upd2);
        EXPECT_TRUE(proxy.otherTotal() == Counters());
        EXPECT_EQ(proxy.childCounters("w1"), upd1);
        EXPECT_EQ(proxy.childCounters("w2"), upd2);
    }
}

Y_UNIT_TEST(garbage_and_downstream)
{
    const auto client1 = makeClientHash("client.1");
    const auto client2 = makeClientHash("client.2");
    const auto client3 = makeClientHash("client.3");

    ShardedCounter<SortedCounters> c("test");

    // client.1 - below loBound, removed by children
    // client.2 - below loBound, still on one children
    // client.3 - above loBound, removed by children

    for (const auto& client : {"A", "B"}) {
        c.downstreamUpdate(client,
            SortedCounters{{"resource", {{
                { client1, 99 },
                { client2, 100 },
                { client3, 200 },
        }}}});
    }

    c.downstreamUpdate("A", SortedCounters{{"resource", {{{ client2, 100 }}}}});
    c.downstreamUpdate("B", SortedCounters{});

    auto dropped = c.garbageCollect(
        SortedCounters{{"resource", {{
            { client1, 220 },
            { client2, 220 },
            { client3, 220 },
        }}}});
    EXPECT_EQ(dropped, 1ul);

    const auto& result = c.shardTotal()["resource"];
    SortedResourceCounters expected = {{
        { client2, 200 },
        { client3, 400 }}};

    EXPECT_EQ(result, expected);
}

Y_UNIT_TEST(garbage_and_upstream)
{
    const auto client1 = makeClientHash("client1");
    const auto client2 = makeClientHash("client2");
    const auto client3 = makeClientHash("client3");

    ShardedCounter<SortedCounters> c("test");

    // update from client
    auto fromBelow = SortedCounters{{"res", { {client1, 1}, {client2, 2} }}};
    c.downstreamUpdate("below", fromBelow);

    // update from upstream
    auto fromAbove = SortedCounters{{"res", { {client2, 222}, {client3, 333} }}};
    c.upstreamUpdate(SortedCounters(fromAbove));

    EXPECT_EQ(c.shardTotal(), fromBelow);
    EXPECT_EQ(c.otherTotal(), fromAbove);
    EXPECT_EQ(c.childCounters("below"), fromBelow);

    // next update - client dropped counter1
    auto belowUpd = SortedCounters{{"res", {{client2, 22}} }};
    c.downstreamUpdate("below", SortedCounters(belowUpd));

    EXPECT_EQ(c.shardTotal(), SortedCounters({{"res", {{client1, 1}, {client2, 22}} }}));
    EXPECT_EQ(c.otherTotal(), fromAbove);
    EXPECT_EQ(c.childCounters("below"), belowUpd);

    // garbageCollect(limits.lowerBound(c.shardTotal()))
    auto dropped = c.garbageCollect(SortedCounters{{"res", {
        {client1, 100},
        {client2, 200}
    }}});
    EXPECT_EQ(dropped, 2ul);

    EXPECT_EQ(c.shardTotal(), SortedCounters({{"res", {{client2, 22}} }}));
    EXPECT_EQ(c.otherTotal(), SortedCounters({{"res", {{client2, 222}} }}));
    EXPECT_EQ(c.childCounters("below"), belowUpd);

    // next update - client dropped all counters
    c.downstreamUpdate("below", SortedCounters());

    EXPECT_EQ(c.shardTotal(), SortedCounters({{"res", {{client2, 22}} }}));
    EXPECT_EQ(c.otherTotal(), SortedCounters({{"res", {{client2, 222}} }}));
    EXPECT_EQ(c.childCounters("below"), SortedCounters());

    dropped = c.garbageCollect(SortedCounters{{"res", {{client2, 400}} }});
    EXPECT_EQ(dropped, 1ul);

    // expect no counters left in the end
    EXPECT_EQ(c.shardTotal(), SortedCounters());
    EXPECT_EQ(c.otherTotal(), SortedCounters());
    EXPECT_EQ(c.childCounters("below"), SortedCounters());
}

Y_UNIT_TEST(garbage_and_downstream_remove)
{
    const auto id1 = makeClientHash("client1");
    const auto id2 = makeClientHash("client2");

    using ShardedType = ShardedCounter<SortedCounters>;
    ShardedType c("test");

    // updates from downstream
    auto updA1 = SortedCounters{{"res", {{id1, 153}, {id2, 222}}}};
    c.downstreamUpdate("agent1", updA1);
    auto updA2 = SortedCounters{{"res", {{id1, 351}}}};
    c.downstreamUpdate("agent2", updA2);

    { // check what we got
        EXPECT_EQ(c.shardTotal(), updA1 + updA2);
        EXPECT_EQ(c.childCounters("agent1"), updA1);
        EXPECT_EQ(c.childCounters("agent2"), updA2);
    }

    // try to garbageCollect everything
    auto dropped = c.garbageCollect(SortedCounters{{"res", { {id1, 1000}, {id2, 1000} }}});
    EXPECT_EQ(dropped, 0ul);
    {  // but no luck, all counters still there
        EXPECT_EQ(c.shardTotal(), updA1 + updA2);
        EXPECT_EQ(c.childCounters("agent1"), updA1);
        EXPECT_EQ(c.childCounters("agent2"), updA2);
    }

    // now force remove agent1
    c.downstreamReset("agent1");
    {   // check it
        EXPECT_TRUE(c.childCounters("agent1").empty());
    }

    // and garbageCollect again
    dropped = c.garbageCollect(SortedCounters{{"res", { {id1, 1000}, {id2, 1000} }}});
    EXPECT_EQ(dropped, 1ul);
    {  // expect agent2 stuff to remain
        EXPECT_EQ(c.shardTotal(), SortedCounters({{"res", {{id1, 153+351}}}}));
        EXPECT_EQ(c.childCounters("agent1"), SortedCounters());
        EXPECT_EQ(c.childCounters("agent2"), updA2);
    }

    // force remove agent2
    c.downstreamReset("agent2");
    // expect nothing left after garbageCollect
    dropped = c.garbageCollect(SortedCounters{{"res", { {id1, 1000} }}});
    EXPECT_EQ(dropped, 1ul);
    {
        EXPECT_TRUE(c.shardTotal().empty());
        EXPECT_TRUE(c.childCounters("agent1").empty());
        EXPECT_TRUE(c.childCounters("agent2").empty());
    }
}

Y_UNIT_TEST(garbage_shard0)
{
    const auto client1 = makeClientHash("client1");
    ShardedCounter<SortedCounters> c("wat");

    // Zero update from child
    SortedCounters fromBelow{{"res", { {client1, 153} }}};
    c.downstreamReset("below", fromBelow);
    c.downstreamUpdate("below", fromBelow);

    // Non-zero update from above (or from peer, result is same)
    SortedCounters fromAbove{{"res", { {client1, 100500} }}};
    c.upstreamUpdate(SortedCounters(fromAbove));

    // Check state is (shard==0, other!=0)
    EXPECT_EQ(c.shardTotal(), SortedCounters({{"res", { {client1, 0} }}}));
    EXPECT_EQ(c.otherTotal(), fromAbove);

    // shard==0 counter is garbage
    auto dropped = c.garbageCollect({{"res", { {client1, 0} }}});
    EXPECT_EQ(dropped, 1ul);
    EXPECT_EQ(c.shardTotal(), SortedCounters());
    EXPECT_EQ(c.otherTotal(), SortedCounters());
    // Even if counter is referenced by child state
    EXPECT_EQ(c.childCounters("below"), fromBelow);
}

Y_UNIT_TEST(complex_counter_filtering)
{
    auto client1 = makeClientHash("client.1");
    auto shardTotal = Counters({
        {"resource.1", {{ client1, 1 }} },
        {"resource.2", {{ client1, 1 }} }
    });
    auto otherTotal = Counters({
        {"resource.1", {{ client1, 2 }} },
        {"resource.3", {{ client1, 2 }} }
    });
    auto update = Counters({
        {"resource.1", {{ client1, 10 }} },
        {"resource.4", {{ client1, 10 }} }
    });

    // resource filtering - reply contain only resources mentioned in request
    ShardedCounter<> node{"n1", shardTotal, ShardedCounter<>::Registry(), otherTotal};

    node.downstreamUpdate("child", update);
    auto reply = node.downstreamResponse(update);
    EXPECT_EQ(reply, Counters({
        {"resource.1", {{ client1, 3 }} },
        {"resource.4", {{ client1, 0 }} }
    }));

    EXPECT_EQ(node.shardTotal(), Counters({
        {"resource.1", {{ client1, 11 }} },
        {"resource.2", {{ client1, 1 }} },
        {"resource.4", {{ client1, 10 }} }
    }));
    EXPECT_EQ(node.otherTotal(), otherTotal);
}

Y_UNIT_TEST(downstream_reset)
{
    const auto client = makeClientHash("client1");

    ShardedCounter<SortedCounters> c("test");

    auto a = 2000;  // starting values
    auto b = 1000;

    c.downstreamUpdate("A", SortedCounters{{"resource", {{{ client, a }}}}});
    c.downstreamUpdate("B", SortedCounters{{"resource", {{{ client, b }}}}});

    // check what we got
    EXPECT_EQ(c.shardTotal(), SortedCounters({{"resource", {{ client, a + b }}}}));
    EXPECT_EQ(c.childCounters("A"), SortedCounters({{"resource", {{ client, a }}}}));
    EXPECT_EQ(c.childCounters("B"), SortedCounters({{"resource", {{ client, b }}}}));

    // no updates from a (A lost connection), but b still here
    b += 100;
    c.downstreamUpdate("B", SortedCounters{{"resource", {{{ client, b }}}}});

    a += 500;
    // a is back bu late, so we reset before update
    c.downstreamReset("A", SortedCounters{{"resource", {{{ client, a }}}}});
    auto reply = c.downstreamResponse(SortedCounters{{"resource", {{{ client, a }}}}});
    EXPECT_EQ(reply, SortedCounters({{"resource", {{ client, b - 500 }}}}));

    b += 100;   // b expects no a delta in reply
    c.downstreamUpdate("B", SortedCounters{{"resource", {{{ client, b }}}}});
    reply = c.downstreamResponse(SortedCounters{{"resource", {{{ client, b }}}}});
    EXPECT_EQ(reply, SortedCounters({{"resource", {{ client, a - 500 }}}}));

    // check state after Reset
    EXPECT_EQ(c.shardTotal(), SortedCounters({{"resource", {{ client, a + b - 500 }}}}));
    EXPECT_EQ(c.childCounters("A"), SortedCounters({{"resource", {{ client, a }}}}));
    EXPECT_EQ(c.childCounters("B"), SortedCounters({{"resource", {{ client, b }}}}));

    // now continue as usual
    a += 20;
    c.downstreamUpdate("A", SortedCounters{{"resource", {{{ client, a }}}}});
    reply = c.downstreamResponse(SortedCounters{{"resource", {{{ client, a }}}}});
    EXPECT_EQ(reply, SortedCounters({{"resource", {{ client, b - 500 }}}}));
    b += 10;
    c.downstreamUpdate("B", SortedCounters{{"resource", {{{ client, b }}}}});
    reply = c.downstreamResponse(SortedCounters{{"resource", {{{ client, b }}}}});
    EXPECT_EQ(reply, SortedCounters({{"resource", {{ client, a - 500 }}}}));

    // final state check
    EXPECT_EQ(c.shardTotal(), SortedCounters({{"resource", {{ client, a + b - 500 }}}}));
    EXPECT_EQ(c.childCounters("A"), SortedCounters({{"resource", {{ client, a }}}}));
    EXPECT_EQ(c.childCounters("B"), SortedCounters({{"resource", {{ client, b }}}}));
}

Y_UNIT_TEST(downstream_reset_to_smaller_value)
{
    const auto client = makeClientHash("client1");

    ShardedCounter<SortedCounters> c("test");

    auto a0 = SortedCounters{{"resource", {{{ client, 100 }}}}};
    auto b = SortedCounters{{"resource", {{{ client, 10 }}}}};
    c.downstreamUpdate("A", a0);
    c.downstreamUpdate("B", b);

    // Assume a lost connection and garbage collected counters
    auto a1 = SortedCounters{{"resource", {{{ client, 1 }}}}};  // now a value is smaller
    // and sync is late, so we reset before update
    c.downstreamReset("A", a1);
    auto reply = c.downstreamResponse(a1);
    EXPECT_EQ(reply, a0 + b - a1);

    b += SortedCounters{{"resource", {{{ client, 10 }}}}};  // b += 10
    c.downstreamUpdate("B", b);
    reply = c.downstreamResponse(b);
    EXPECT_EQ(reply, a0);   // 'cause total is same after a reset/update

    // final state check
    EXPECT_EQ(c.shardTotal(), a0 + b);
    EXPECT_EQ(c.childCounters("A"), a1);
    EXPECT_EQ(c.childCounters("B"), b);
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::rate_limiter2::tests
