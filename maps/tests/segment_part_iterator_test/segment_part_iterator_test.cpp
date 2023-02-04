#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

#include <maps/analyzer/libs/shortest_path/include/segment_part_iterator.h>
#include <maps/analyzer/libs/shortest_path/include/types.h>

#include <iostream>
#include <vector>
#include <unordered_map>


namespace mas = maps::analyzer::shortest_path;
using maps::road_graph::EdgeId;
using maps::road_graph::SegmentIndex;
using maps::road_graph::SegmentId;
using maps::road_graph::SegmentPart;
using maps::analyzer::shortest_path::EdgeIds;
using maps::analyzer::shortest_path::Route;

namespace std {

std::ostream& operator << (std::ostream& ostr, const SegmentPart& p) {
    return ostr
        << "SegmentPart("
        << p.segmentId.edgeId.value() << ":"
        << p.segmentId.segmentIndex.value() << ":"
        << p.start << "-" << p.finish;
}

} // std

struct MockEdgeId {};

namespace maps::analyzer::shortest_path::tpl {

template <>
struct Types<MockEdgeId> {
    using EdgeId = typename Types<road_graph::EdgeId>::EdgeId;
    using SegmentId = typename Types<road_graph::EdgeId>::SegmentId;
    using SegmentPart = typename Types<road_graph::EdgeId>::SegmentPart;
};

namespace internal {

template <>
struct IteratorData<MockEdgeId> {
    using EdgeId = typename Types<MockEdgeId>::EdgeId;

    std::size_t segmentsNumber(EdgeId edgeId) const {
        auto it = edgesInfo.find(edgeId);
        REQUIRE(it != edgesInfo.end(), "Unknown test edge id: " << edgeId.value());
        return it->second;
    }

    bool operator== (const IteratorData&) const = default;
    bool operator!= (const IteratorData&) const = default;

    std::unordered_map<EdgeId, std::size_t> edgesInfo;
};

}

} // maps::analyzer::shortest_path::tpl

using MockIteratorData = mas::tpl::internal::IteratorData<MockEdgeId>;

const mas::tpl::SegmentPartIterator<MockEdgeId> endIterator{};


TEST(SegmentPartIteratorTests, EmptyIterator) {
    mas::tpl::SegmentPartIterator<MockEdgeId> emptyIterator;
    EXPECT_THROW(*emptyIterator, std::out_of_range);
    EXPECT_THROW(++emptyIterator, std::out_of_range);
}

TEST(SegmentPartIteratorTests, EmptySequence) {
    MockIteratorData edgeData;
    Route routeFromSource;
    EdgeIds edgeIds;
    Route routeToTarget;
    mas::tpl::SegmentPartIterator<MockEdgeId> iterator{
        edgeData,
        &routeFromSource,
        &edgeIds,
        &routeToTarget
    };
    EXPECT_TRUE(iterator == endIterator);
    EXPECT_THROW(*iterator, std::out_of_range);
    EXPECT_THROW(++iterator, std::out_of_range);
}

TEST(SegmentPartIteratorTests, OnlyRouteFromSource) {
    MockIteratorData edgeData;
    Route routeFromSource;
    SegmentPart part{SegmentId{EdgeId{0}, SegmentIndex{0}}, 0.0, 1.0};
    routeFromSource.push_back(part);
    EdgeIds edgeIds;
    Route routeToTarget;
    mas::tpl::SegmentPartIterator<MockEdgeId> iterator{
        edgeData,
        &routeFromSource,
        &edgeIds,
        &routeToTarget
    };
    EXPECT_EQ(*iterator++, part);
    EXPECT_TRUE(iterator == endIterator);
    EXPECT_THROW(*iterator, std::out_of_range);
    EXPECT_THROW(++iterator, std::out_of_range);
}

TEST(SegmentPartIteratorTests, OnlyEdges) {
    MockIteratorData edgeData;

    Route routeFromSource;
    EdgeIds edgeIds;
    edgeIds.push_back(EdgeId{0});
    edgeIds.push_back(EdgeId{1});
    edgeData.edgesInfo[EdgeId{0}] = 1;
    edgeData.edgesInfo[EdgeId{1}] = 2;
    Route routeToTarget;

    mas::tpl::SegmentPartIterator<MockEdgeId> iterator{
        edgeData,
        &routeFromSource,
        &edgeIds,
        &routeToTarget
    };

    for (const auto& edgeId: edgeIds) {
        for (std::size_t segmentIndex = 0; segmentIndex < edgeData.segmentsNumber(edgeId); ++segmentIndex) {
            const SegmentPart part{
                SegmentId{edgeId, SegmentIndex{segmentIndex}},
                0.0, 1.0
            };
            EXPECT_EQ(*iterator++, part);
        }
    }

    EXPECT_TRUE(iterator == endIterator);
    EXPECT_THROW(*iterator, std::out_of_range);
    EXPECT_THROW(++iterator, std::out_of_range);
}

TEST(SegmentPartIteratorTests, OnlyRouteToTarget) {
    MockIteratorData edgeData;
    Route routeFromSource;
    EdgeIds edgeIds;
    Route routeToTarget;
    routeToTarget.push_back(SegmentPart{SegmentId{EdgeId{0}, SegmentIndex{0}}, 0.0, 0.5});
    routeToTarget.push_back(SegmentPart{SegmentId{EdgeId{0}, SegmentIndex{0}}, 0.5, 1.0});
    routeToTarget.push_back(SegmentPart{SegmentId{EdgeId{0}, SegmentIndex{1}}, 0.0, 1.0});
    mas::tpl::SegmentPartIterator<MockEdgeId> iterator{
        edgeData,
        &routeFromSource,
        &edgeIds,
        &routeToTarget
    };

    for (const SegmentPart& part: routeToTarget) {
        EXPECT_EQ(*iterator++, part);
    }
    EXPECT_TRUE(iterator == endIterator);
    EXPECT_THROW(*iterator, std::out_of_range);
    EXPECT_THROW(++iterator, std::out_of_range);
}

TEST(SegmentPartIteratorTests, OnlySourceAndTarget) {
    MockIteratorData edgeData;
    Route routeFromSource;
    routeFromSource.push_back(SegmentPart{SegmentId{EdgeId{1}, SegmentIndex{0}}, 0.0, 1.0});
    routeFromSource.push_back(SegmentPart{SegmentId{EdgeId{2}, SegmentIndex{0}}, 0.0, 1.0});
    EdgeIds edgeIds;
    Route routeToTarget;
    routeToTarget.push_back(SegmentPart{SegmentId{EdgeId{0}, SegmentIndex{0}}, 0.0, 0.5});
    routeToTarget.push_back(SegmentPart{SegmentId{EdgeId{0}, SegmentIndex{0}}, 0.5, 1.0});
    routeToTarget.push_back(SegmentPart{SegmentId{EdgeId{0}, SegmentIndex{1}}, 0.0, 1.0});
    mas::tpl::SegmentPartIterator<MockEdgeId> iterator(
        edgeData,
        &routeFromSource,
        &edgeIds,
        &routeToTarget
    );

    for (const SegmentPart& part: routeFromSource) {
        EXPECT_EQ(*iterator++, part);
    }
    for (const SegmentPart& part: routeToTarget) {
        EXPECT_EQ(*iterator++, part);
    }
    EXPECT_TRUE(iterator == endIterator);
    EXPECT_THROW(*iterator, std::out_of_range);
    EXPECT_THROW(++iterator, std::out_of_range);
}

TEST(SegmentPartIteratorTests, FullTrace) {
    MockIteratorData edgeData;
    Route routeFromSource;
    routeFromSource.push_back(SegmentPart{SegmentId{EdgeId{1}, SegmentIndex{0}}, 0.0, 1.0});
    routeFromSource.push_back(SegmentPart{SegmentId{EdgeId{2}, SegmentIndex{0}}, 0.0, 1.0});
    EdgeIds edgeIds;
    edgeIds.push_back(EdgeId{10});
    edgeIds.push_back(EdgeId{12});

    edgeData.edgesInfo[EdgeId{10}] = 1;
    edgeData.edgesInfo[EdgeId{12}] = 2;

    Route routeToTarget;
    routeToTarget.push_back(SegmentPart{SegmentId{EdgeId{0}, SegmentIndex{0}}, 0.0, 0.5});
    routeToTarget.push_back(SegmentPart{SegmentId{EdgeId{0}, SegmentIndex{0}}, 0.5, 1.0});
    routeToTarget.push_back(SegmentPart{SegmentId{EdgeId{0}, SegmentIndex{1}}, 0.0, 1.0});
    mas::tpl::SegmentPartIterator<MockEdgeId> iterator(
        edgeData,
        &routeFromSource,
        &edgeIds,
        &routeToTarget
    );

    for (const SegmentPart& part: routeFromSource) {
        EXPECT_EQ(*iterator++, part);
    }

    for (const auto& edgeId: edgeIds) {
        for (std::size_t segmentIndex = 0; segmentIndex < edgeData.segmentsNumber(edgeId); ++segmentIndex) {
            const SegmentPart part{SegmentId{edgeId, SegmentIndex{segmentIndex}}, 0.0, 1.0};
            EXPECT_EQ(*iterator++, part);
        }
    }

    for (const SegmentPart& part: routeToTarget) {
        EXPECT_EQ(*iterator++, part);
    }
    EXPECT_TRUE(iterator == endIterator);
    EXPECT_THROW(*iterator, std::out_of_range);
    EXPECT_THROW(++iterator, std::out_of_range);
}
