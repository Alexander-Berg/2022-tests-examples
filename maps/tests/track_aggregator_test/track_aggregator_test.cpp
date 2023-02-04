#include <maps/analyzer/services/jams_analyzer/modules/usershandler/lib/config.h>
#include <maps/analyzer/services/jams_analyzer/modules/usershandler/lib/graphs_wrapper.h>
#include <maps/analyzer/services/jams_analyzer/modules/usershandler/lib/track_aggregator.h>
#include <maps/analyzer/services/jams_analyzer/modules/usershandler/tests/test_tools/include/test_tools.h>

#include <maps/libs/concurrent/include/atomic_shared_ptr.h>
#include <maps/libs/deprecated/boost_time/utils.h>
#include <maps/libs/road_graph/include/types.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <algorithm>
#include <memory>

namespace pt = boost::posix_time;

using maps::concurrent::AtomicSharedPtr;

using maps::analyzer::VehicleId;

using maps::road_graph::EdgeId;
using maps::road_graph::SegmentIndex;
using maps::road_graph::SegmentId;

const SegmentId FIRST_SEGMENT{EdgeId(1), SegmentIndex(0)};
const SegmentId SECOND_SEGMENT{EdgeId(2), SegmentIndex(0)};

const pt::ptime startTime = maps::fromTimestamp(1000000000);

struct TestContext {
    Config config;
    GraphsWrapper graphsWrapper_;
    TestConsumer consumer;
    AtomicSharedPtr<ManoeuvresStorage> storage_;
    TrackAggregator aggregator;

    TestContext():
        config(makeUsershandlerConfig("usershandler.conf")),
        graphsWrapper_(config.roadGraphConfig(), config.cacheConfig(), false),
        storage_(std::make_shared<ManoeuvresStorage>(config.pathToManoeuvres(), false, config.lockMemory())),
        aggregator(config.trackAggregatorConfig(), consumer, graphsWrapper_, storage_)
    {}

    template <typename TravelTimeData>
    bool hasPersistentEdgeId(const TravelTimeData& data, size_t shortId) {
        const auto expectedLongId =
            graphsWrapper_.edgeLongId(EdgeId(shortId));
        const auto foundLongId = data.persistentEdgeId();
        return foundLongId && *foundLongId == expectedLongId;
    }
};

Y_UNIT_TEST_SUITE(TrackAggregatorTest) {
    SegmentId seg(uint32_t edgeId, uint32_t segIndex) {
        return {EdgeId{edgeId}, SegmentIndex{segIndex}};
    }

    MatchedPart makePart(SegmentId segmentId, int enterTimeOffset, int signalTimeOffset, double part, double travelTime) {
        return {
            {"0", "0"},
            segmentId,
            startTime + pt::seconds(enterTimeOffset),
            startTime + pt::seconds(signalTimeOffset),
            part, travelTime, false, 0
        };
    }

    Y_UNIT_TEST(track_aggregation) {
        std::vector<MatchedPart> testParts({
            makePart(seg(1, 0), 0, 0, 0.5, 1.0),
            makePart(seg(1, 0), 1, 1, 0.5, 1.0),
            makePart(seg(2, 0), 2, 1, 0.5, 2.0),
            makePart(seg(2, 0), 4, 4, 0.5, 4.0),
        });

        TestContext ctx;
        TestConsumer& consumer = ctx.consumer;
        TrackAggregator& aggregator = ctx.aggregator;

        for (const MatchedPart& part : testParts) {
            aggregator.push(part);
        }

        EXPECT_EQ(consumer.segmentVector.size(), 2u);

        EXPECT_EQ(consumer.segmentVector[0].segmentId(), FIRST_SEGMENT);
        EXPECT_EQ(*consumer.segmentVector[0].enterTime(), startTime);
        EXPECT_DOUBLE_EQ((*consumer.segmentVector[0].travelTime()), 2.0);
        EXPECT_TRUE(ctx.hasPersistentEdgeId(consumer.segmentVector[0], 1));

        EXPECT_EQ(consumer.segmentVector[1].segmentId(), SECOND_SEGMENT);
        EXPECT_EQ(*consumer.segmentVector[1].enterTime(), startTime + pt::seconds(2));
        EXPECT_DOUBLE_EQ(*consumer.segmentVector[1].travelTime(), 6.0);
        EXPECT_TRUE(ctx.hasPersistentEdgeId(consumer.segmentVector[1], 2));
    }

    Y_UNIT_TEST(track_aggregation_manoeuvres) {
        std::vector<MatchedPart> testParts({
            makePart(seg(7, 0), 0, 0, 0.5, 1.0),
            makePart(seg(7, 0), 1, 1, 0.5, 1.0),
            makePart(seg(8, 0), 2, 1, 0.5, 2.0),
            makePart(seg(8, 0), 4, 4, 0.5, 4.0),
            makePart(seg(9, 0), 6, 6, 0.5, 6.0),
            makePart(seg(9, 0), 8, 8, 0.5, 8.0),
        });

        TestContext ctx;
        TestConsumer& consumer = ctx.consumer;
        TrackAggregator& aggregator = ctx.aggregator;

        for (const MatchedPart& part : testParts) {
            aggregator.push(part);
        }

        EXPECT_EQ(consumer.segmentVector.size(), 6u);

        // edge with shortId 7 should occur 3 times: 1 as usual, 2 with manoeuvres
        EXPECT_EQ(
            std::count_if(
                consumer.segmentVector.begin(),
                consumer.segmentVector.end(),
                [](auto elem) { return elem.segmentId().edgeId.value() == 7; }),
            3);
        EXPECT_EQ(
            std::count_if(
                consumer.segmentVector.begin(),
                consumer.segmentVector.end(),
                [](auto elem) { return elem.segmentId().edgeId.value() == 7 && elem.manoeuvreId().has_value(); }),
            2);

        // edge with shortId 8 should occur 2 times: 1 as usual, 1 with manoeuvres
        EXPECT_EQ(
            std::count_if(
                consumer.segmentVector.begin(),
                consumer.segmentVector.end(),
                [](auto elem) { return elem.segmentId().edgeId.value() == 8; }),
            2);
        EXPECT_EQ(
            std::count_if(
                consumer.segmentVector.begin(),
                consumer.segmentVector.end(),
                [](auto elem) { return elem.segmentId().edgeId.value() == 8 && elem.manoeuvreId().has_value(); }),
            1);

        // edge with shortId 9 should occur 1 time
        EXPECT_EQ(
            std::count_if(
                consumer.segmentVector.begin(),
                consumer.segmentVector.end(),
                [](auto elem) { return elem.segmentId().edgeId.value() == 9; }),
            1);
    }

    Y_UNIT_TEST(track_aggregation_with_stops) {
        std::vector<MatchedPart> testSegmentsStop({
            makePart(seg(1, 0), 0, 0, 0.5, 1.0),
            makePart(seg(1, 0), 1, 1, 0.5, 1.0),
            makePart(seg(2, 0), 2, 1, 0.5, 2.0),
            makePart(seg(2, 0), 4, 4, 0.0, 1.0),
            makePart(seg(2, 0), 4, 4, 0.0, 2.0),
            makePart(seg(2, 0), 4, 4, 0.5, 4.0),
        });

        TestContext ctx;
        TestConsumer& consumer = ctx.consumer;
        TrackAggregator& aggregator = ctx.aggregator;

        for (const MatchedPart& part : testSegmentsStop) {
            aggregator.push(part);
        }

        EXPECT_EQ(consumer.segmentVector.size(), 2u);

        EXPECT_EQ(consumer.segmentVector[0].segmentId(), FIRST_SEGMENT);
        EXPECT_EQ(*consumer.segmentVector[0].enterTime(), startTime);
        EXPECT_DOUBLE_EQ(*consumer.segmentVector[0].travelTime(), 2.0);

        EXPECT_EQ(consumer.segmentVector[1].segmentId(), SECOND_SEGMENT);
        EXPECT_EQ(*consumer.segmentVector[1].enterTime(), startTime + pt::seconds(2));
        EXPECT_DOUBLE_EQ(*consumer.segmentVector[1].travelTime(), 9.0);
    }

    Y_UNIT_TEST(track_aggregation_with_extrapolation) {
        std::vector<MatchedPart> testSegmentsExtrapolation({
            makePart(seg(1, 0), 0, 0, 0.25, 100.0),
            makePart(seg(1, 0), 0, 0, 0.5, 200.0),
            makePart(seg(1, 0), 0, 0, 0.25, 1.0),
        });

        TestContext ctx;
        TestConsumer& consumer = ctx.consumer;
        TrackAggregator& aggregator = ctx.aggregator;

        for (const MatchedPart& part : testSegmentsExtrapolation) {
            aggregator.push(part);
        }

        EXPECT_EQ(consumer.segmentVector.size(), 2u);

        EXPECT_EQ(consumer.segmentVector[0].segmentId(), FIRST_SEGMENT);
        EXPECT_EQ(*consumer.segmentVector[0].enterTime(), startTime);
        EXPECT_EQ(consumer.segmentVector[0].vehicleId(), VehicleId("0", "0"));
        EXPECT_DOUBLE_EQ(*consumer.segmentVector[0].travelTime(), 400.0);

        EXPECT_EQ(consumer.segmentVector[1].segmentId(), FIRST_SEGMENT);
        EXPECT_EQ(*consumer.segmentVector[1].enterTime(), startTime);
        EXPECT_EQ(consumer.segmentVector[1].vehicleId(), VehicleId("0", "0"));
        EXPECT_DOUBLE_EQ(*consumer.segmentVector[1].travelTime(), 301.0);
    }
}
