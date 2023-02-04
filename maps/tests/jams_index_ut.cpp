#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>

#include <maps/jams/libs/joiner/impl/jams_index.h>

using namespace maps::jams::common;
using namespace maps::jams::joiner;
using namespace maps::jams;
using maps::road_graph::VertexId;

struct Edge {
    int region;
    VertexId source;
    VertexId target;

    bool operator<(const Edge& other) const
    {
        if (region != other.region) { return region < other.region; }
        if (source != other.source) { return source < other.source; }
        if (target != other.target) { return target < other.target; }

        return false;
    }
};

using EdgePair = std::pair<Edge, Edge>;
using Edges = std::set<Edge>;
using ExpectedEdges = std::map<VertexId, std::set<EdgePair>>;
using ExpectedVertices = std::set<VertexId>;

std::ostream& operator<<(std::ostream& os, const Edge& edge)
{
    return os << "Edge(region=" << edge.region << ": "<< edge.source << ", " << edge.target << ")";
}

std::ostream& operator<<(std::ostream& os, const EdgePair& ep)
{
    return os << "{" << ep.first << " -> " << ep.second << "}";
}

ShardedJamIndex createIndex(
    size_t shardsCount,
    Edges edges,
    boost::optional<IndexBuckets> buckets = boost::none
) {
    ShardedJamIndex index(shardsCount, buckets);

    size_t cur = 0;
    auto push = [&] (auto edge) {
        index.emplace(
            cur, // index
            edge.region, // region
            edge.source, // source
            edge.target, // target
            maps::road_graph::EdgeId(edge.source.value()), // edgeId
            edge.region, // region
            1, // category
            Severity::Light, // severity
            16.6, // speed
            16.6, // freeSpeed
            100, // length
            true
        );

        std::cerr << "Emplaced in shard " << cur << " with region " << edge.region << "\n";

        // Next time we'll emplace edge into the next shard:
        cur++;
        cur %= shardsCount;
    };

    for (auto&& edge: edges) { push(edge); }

    return index;
}

void testWalkAround(
    size_t shardsCount,
    Edges edges,
    ExpectedEdges expected,
    boost::optional<IndexBuckets> buckets = boost::none
) {
    auto index = createIndex(shardsCount, edges, buckets);
    size_t factor = buckets? buckets->size(): 1;

    for (auto&& [k, _]: expected) {
        for (size_t i = 0; i < factor; ++i) {
            index.walkAround(i, k, [&, k=k] (auto to, auto from) {
                EdgePair edgePair {
                    {from->region, from->source, from->target},
                    {to->region, to->source, to->target}
                };

                std::cerr << "Found: " << edgePair << "\n";

                auto found = expected[k].find(edgePair);
                UNIT_ASSERT(found != expected[k].end());
                expected[k].erase(found);
            });
        }

        UNIT_ASSERT(expected[k].empty());
    }

    UNIT_ASSERT_EQUAL(index.jamsCount(), edges.size());
}

void testForEachVertex(
    size_t shardsCount,
    Edges edges,
    ExpectedVertices expected,
    boost::optional<IndexBuckets> buckets = boost::none
) {
    auto index = createIndex(shardsCount, edges, buckets);
    size_t factor = buckets? buckets->size(): 1;

    for (size_t i = 0; i < factor; ++i) {
        index.forEachVertex(i, [&] (auto vertex) {
            std::cerr << "Found vertex: " << vertex << "\n";

            auto found = expected.find(vertex);
            UNIT_ASSERT(found != expected.end());
            expected.erase(found);
        });
    }

    UNIT_ASSERT_EQUAL(expected.size(), 0);
}

std::tuple<Edges, ExpectedEdges, ExpectedVertices> simpleData()
{
    Edges edges = {
        {1, VertexId(1), VertexId(3)}, {1, VertexId(2), VertexId(3)},
        {1, VertexId(3), VertexId(4)}, {1, VertexId(3), VertexId(5)},
        {1, VertexId(3), VertexId(6)}
    };

    ExpectedEdges expectedEdges {
        {
            VertexId(3), {
                {{1, VertexId(1), VertexId(3)}, {1, VertexId(3), VertexId(4)}},
                {{1, VertexId(1), VertexId(3)}, {1, VertexId(3), VertexId(5)}},
                {{1, VertexId(1), VertexId(3)}, {1, VertexId(3), VertexId(6)}},
                {{1, VertexId(2), VertexId(3)}, {1, VertexId(3), VertexId(4)}},
                {{1, VertexId(2), VertexId(3)}, {1, VertexId(3), VertexId(5)}},
                {{1, VertexId(2), VertexId(3)}, {1, VertexId(3), VertexId(6)}}
            }
        }
    };

    ExpectedVertices expectedVertices = {VertexId(1), VertexId(2), VertexId(3)};

    return {edges, expectedEdges, expectedVertices};
}

std::tuple<Edges, ExpectedEdges, ExpectedVertices, IndexBuckets> dataWithBuckets()
{
    Edges edges = {
        {1, VertexId(1), VertexId(3)}, {1, VertexId(2), VertexId(3)},
        {1, VertexId(3), VertexId(4)}, {2, VertexId(4), VertexId(5)},
        {2, VertexId(5), VertexId(6)}
    };

    ExpectedEdges expectedEdges = {
        {
            VertexId(3), {
                {{1, VertexId(1), VertexId(3)}, {1, VertexId(3), VertexId(4)}},
                {{1, VertexId(2), VertexId(3)}, {1, VertexId(3), VertexId(4)}}
            }
        },
        {
            VertexId(5), {
                {{2, VertexId(4), VertexId(5)}, {2, VertexId(5), VertexId(6)}}
            }
        }
    };

    ExpectedVertices expectedVertices = {
        VertexId(1), VertexId(2), VertexId(3), VertexId(4), VertexId(5)};

    IndexBuckets buckets = {{1}, {2}};

    return {edges, expectedEdges, expectedVertices, buckets};
}


Y_UNIT_TEST_SUITE(JamsIndex)
{
    Y_UNIT_TEST(WalkAroundSingleShardSingleBucket)
    {
        auto [edges, expected,_ ] = simpleData();
        testWalkAround(1, edges, expected, boost::none);
    }

    Y_UNIT_TEST(WalkAroundMultipleShardsSingleBucket)
    {
        auto [edges, expected, _] = simpleData();
        testWalkAround(2, edges, expected, boost::none);
    }

    Y_UNIT_TEST(WalkAroundSingleShardMultipleBuckets)
    {
        auto [edges, expected, _, buckets] = dataWithBuckets();
        testWalkAround(1, edges, expected, buckets);
    }

    Y_UNIT_TEST(WalkAroundMultipleShardsMultipleBuckets)
    {
        auto [edges, expected, _, buckets] = dataWithBuckets();
        testWalkAround(2, edges, expected, buckets);
    }

    Y_UNIT_TEST(ForEachVertexSingleShardSimpleBucket)
    {
        auto [edges, _, expected] = simpleData();
        testForEachVertex(1, edges, expected);
    }

    Y_UNIT_TEST(ForEachVertexMutipleShardsSimpleBucket)
    {
        auto [edges, _, expected] = simpleData();
        testForEachVertex(2, edges, expected);
    }

    Y_UNIT_TEST(ForEachVertexSingleShardMultipleBuckets)
    {
        auto [edges, _, expected, buckets] = dataWithBuckets();
        testForEachVertex(1, edges, expected, buckets);
    }

    Y_UNIT_TEST(ForEachVertexMultipleShardsMultipleBuckets)
    {
        auto [edges, _, expected, buckets] = dataWithBuckets();
        testForEachVertex(2, edges, expected, buckets);
    }
}
