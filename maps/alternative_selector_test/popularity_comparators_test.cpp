#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include "alternatives_selector/selector.h"
#include "alternatives_selector/comparators.h"

#include <algorithm>
#include <vector>

namespace mrg = maps::road_graph;

using maps::routing::alternatives_selector::PopularityComparator;
using maps::routing::PathSegment;
using maps::jams::edge_seq_stat::TracksNumber;
using maps::jams::edge_seq_stat::CompressedEdgeSequenceDicts;
using maps::jams::edge_seq_stat::CompressedMutableEdgeSequenceDict;
using maps::jams::edge_seq_stat::CompressedMutableEdgeSequenceDicts;
using maps::routing::alternatives_selector::ComparingResult;

namespace {

const maps::road_graph::Graph ROAD_GRAPH(
    BinaryPath("maps/data/test/graph4/road_graph.fb"));

const mrg::PersistentIndex PERSISTENT_INDEX(
        BinaryPath("maps/data/test/graph4/edges_persistent_index.fb"));

Route makeRoute(const std::vector<mrg::EdgeId::ValueType>& edgeIds)
{
    Route route;

    for (auto edgeId: edgeIds) {
        route.pathSegments.emplace_back(
                mrg::EdgeId{edgeId},
                0 /* length */,
                0 /* time */,
                0 /* jamsTime */,
                0.0 /* fromPosition */,
                1.0 /* toPosition */);
    }

    mrg::EdgeId firstEdgeId{edgeIds.front()};
    mrg::EdgeId lastEdgeId{edgeIds.back()};

    route.requestPoints.emplace_back(
        0,
        RequestPoint(
            ROAD_GRAPH.vertexGeometry(ROAD_GRAPH.edge(firstEdgeId).source)));
    route.requestPoints.emplace_back(
        route.pathSegments.size(),
        RequestPoint(
            ROAD_GRAPH.vertexGeometry(ROAD_GRAPH.edge(lastEdgeId).target)));
    return route;
}

RouterResult makeRouteResult(
    int alternativeIndex,
    const std::vector<mrg::EdgeId::ValueType>& edgeIds)
{
    RouterResult result {
        VehicleParameters(), Avoid(), makeRoute(edgeIds), &ROAD_GRAPH};
    result.alternativeIndex = alternativeIndex;
    return result;
}

template<class Range>
std::vector<mrg::LongEdgeId> createSequence(const Range& edgeIds) {
    std::vector<mrg::LongEdgeId> result;
    result.reserve(edgeIds.size());
    for (auto edgeId : edgeIds) {
        result.emplace_back(
            PERSISTENT_INDEX.findLongId(mrg::EdgeId(edgeId)).value());
    }
    return result;
}

struct Track {
    std::vector<uint64_t> edgeIds;
    uint32_t number;
};

struct EdgeSequenceDictsHolder {
    EdgeSequenceDictsHolder(
            size_t keySize,
            std::vector<Track> tracks) {
        CompressedMutableEdgeSequenceDicts mutableDicts;
        CompressedMutableEdgeSequenceDict dict(keySize);
        for (const auto& track : tracks) {
            auto persistentIds = createSequence(track.edgeIds);
            auto compressed = mutableDicts.CompressPersistentIds(
                persistentIds.begin(),
                persistentIds.end());
            for (size_t i = 0; i + keySize <= compressed.size(); ++i) {
                CompressedMutableEdgeSequenceDict::CompressedEdgeIdSeq subSeq(
                    compressed.begin() + i,
                    compressed.begin() + i + keySize);
                dict.insert(subSeq, TracksNumber(track.number));
            }
        }
        mutableDicts.GetDicts().emplace_back(std::move(dict));
        std::ostringstream out;
        mms::write(out, mutableDicts);
        buf = out.str();
        dicts = &(mms::safeCast<CompressedEdgeSequenceDicts>(buf.c_str(), buf.size()));
    }


    std::string buf;
    const CompressedEdgeSequenceDicts* dicts = nullptr;
};


} // namespace


Y_UNIT_TEST_SUITE(popularity_comparator_tests)
{

Y_UNIT_TEST(popularity_comparator_test)
{
    AlternativeInfo userRoute (
        makeRouteResult(0, {4, 36925, 98940, 6}),
        &PERSISTENT_INDEX);
    std::vector<AlternativeInfo> selected;
    selected.emplace_back(std::move(userRoute));

    AlternativeInfo info1 (
        makeRouteResult(0, {4, 68650, 224908, 6}),
        &PERSISTENT_INDEX);

    AlternativeInfo info2 (
        makeRouteResult(1, {4, 36925, 23558, 286930, 301382, 155287, 192755, 98940, 6}),
        &PERSISTENT_INDEX);

    {
        EdgeSequenceDictsHolder statHolder(
            2,
            {
                Track{{4, 36925, 98940, 6}, 10},
                Track{{4, 68650, 224908, 6}, 100},
                Track{{4, 36925, 23558, 286930, 301382, 155287, 192755, 98940, 6}, 0}
            }
        );
        PopularityComparator comparator(statHolder.dicts, nullptr, selected, &ROAD_GRAPH, &PERSISTENT_INDEX);
        UNIT_ASSERT_EQUAL(comparator.compare(info1, info2), ComparingResult::FirstBetter);
        UNIT_ASSERT_EQUAL(comparator.compare(info2, info1), ComparingResult::SecondBetter);
        UNIT_ASSERT_EQUAL(comparator.compare(info1, info1), ComparingResult::Equal);
        UNIT_ASSERT_EQUAL(comparator.compare(info2, info2), ComparingResult::Equal);
    }
    {
        EdgeSequenceDictsHolder statHolder(
            2,
            {
                Track{{4, 36925, 98940, 6}, 10},
                Track{{4, 68650, 224908, 6}, 100},
                Track{{4, 36925, 23558, 286930, 301382, 155287, 192755, 98940, 6}, 100}
            }
        );
        PopularityComparator comparator(statHolder.dicts, nullptr, selected, &ROAD_GRAPH, &PERSISTENT_INDEX);
        UNIT_ASSERT_EQUAL(comparator.compare(info1, info2), ComparingResult::Equal);
        UNIT_ASSERT_EQUAL(comparator.compare(info2, info1), ComparingResult::Equal);
    }
    {
        EdgeSequenceDictsHolder statHolder(
            2,
            {
                Track{{4, 36925, 98940, 6}, 0},
                Track{{4, 68650, 224908, 6}, 100},
                Track{{4, 36925, 23558, 286930, 301382, 155287, 192755, 98940, 6}, 200}
            }
        );
        PopularityComparator comparator(statHolder.dicts, nullptr, selected, &ROAD_GRAPH, &PERSISTENT_INDEX);
        UNIT_ASSERT_EQUAL(comparator.compare(info1, info2), ComparingResult::Equal);
        UNIT_ASSERT_EQUAL(comparator.compare(info2, info1), ComparingResult::Equal);
    }
}
}
