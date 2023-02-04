#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>

#include <maps/analyzer/modules/matcher/matcher/lib/matching_processor.h>
#include <maps/analyzer/modules/matcher/matcher/lib/gps_signal_storage.h>
#include <maps/analyzer/libs/shortest_path/include/shortest_paths_cache.h>
#include <yandex/maps/mms/holder2.h>

#include <fstream>

using namespace std;
using namespace maps::road_graph;
using namespace maps::analyzer;
using namespace maps::analyzer::data;
using namespace maps::analyzer::graphmatching;
using namespace maps::analyzer::matcher;
using namespace boost::posix_time;
namespace pt = boost::posix_time;
using namespace maps::geolib3;
namespace mp = yandex::maps::proto::matcher;

const double SPEED = 60.0 / 3.6;
const std::string CLID = "CLID";
const std::string UUID = "UUID";
const double SEGMENT_POSITION = 0.5;

const Config& config()
{
    static Config CONFIG;
    static std::once_flag FLAG;
    std::call_once(FLAG, [&]()
    {
        CONFIG.graph = BinaryPath("maps/data/test/graph3/road_graph.fb");
        CONFIG.edgesPersistentIndex = BinaryPath("maps/data/test/graph3/edges_persistent_index.fb");
        CONFIG.edgesRTree = BinaryPath("maps/data/test/graph3/rtree.fb");
        CONFIG.mapmatcherConfig = ArcadiaSourceRoot() + "/maps/analyzer/libs/graphmatching/conf/geometry.json";
    });
    return CONFIG;
}

const MappedGraph& mappedGraph()
{
    const static MappedGraph MAPPED_GRAPH(config());
    return MAPPED_GRAPH;
}

const MatcherConfig& mapmatcherConfig()
{
    const static MatcherConfig MATCHER_CONFIG = MatcherConfig::fromFile(config().mapmatcherConfig);
    return MATCHER_CONFIG;
}

uint64_t ptimeToTimestamp(const boost::posix_time::ptime& pt)
{
    using namespace boost::posix_time;
    static ptime epoch(boost::gregorian::date(1970, 1, 1));
    auto diff = (pt - epoch).total_seconds();
    return diff;
}

pt::ptime makeTime(
        const Point2& point,
        const boost::optional<Point2>& prevPoint,
        const boost::optional<pt::ptime>& prevTime) {
    pt::ptime time;
    if (prevPoint && prevTime) {
        const auto distance = geoDistance(
            point,
            *prevPoint);
        int delta = distance / SPEED  + 1;
        time = *prevTime + (pt::seconds(delta));
    } else {
        const static pt::ptime START_TIME = maps::nowUtc();
        time = START_TIME;
    }
    return time;
}

GpsSignal makeSignal(
        const Point2& point,
        boost::optional<GpsSignal> prev) {
    pt::ptime time;
    if (prev) {
        time = makeTime(point, Point2(prev->lon(), prev->lat()), prev->time());
    } else {
        time = makeTime(point, {},{});
    }
    GpsSignal signal;
    signal.setClid(CLID);
    signal.setLat(point.y());
    signal.setLon(point.x());
    signal.setTime(time);
    signal.setUuid(UUID);
    return signal;
}

GpsSignal makeSignal(
        const Segment2& segment,
        double position,
        boost::optional<GpsSignal> prev) {
    auto point = segment.pointByPosition(position);
    return makeSignal(point, prev);
}

GpsSignal makeSignal(unsigned time, double lon, double lat) {
    GpsSignal signal;
    signal.setClid(CLID);
    signal.setUuid(UUID);
    signal.setLat(lat);
    signal.setLon(lon);
    signal.setTime(boost::posix_time::from_time_t(time));
    return signal;
}

std::vector<GpsSignal> makeSignals(const std::vector<EdgeId>& edgeIds) {
    std::vector<GpsSignal> signals;
    for (auto edgeId : edgeIds) {
        const Polyline2 polyline = mappedGraph().graph().edgeData(edgeId).geometry();
        for (const auto& segment : polyline.segments()) {
            boost::optional<GpsSignal> prev;
            if (!signals.empty()) {
                prev = signals.back();
            }
            signals.emplace_back(makeSignal(segment, SEGMENT_POSITION, prev));
        }
    }
    return signals;
}

mp::MatchedPath makeMatchedPath(const std::vector<EdgeId>& edgeIds) {
    mp::MatchedPath result;
    auto* points = result.mutable_points();
    auto* pathParts = result.mutable_path_parts();
    boost::optional<SegmentId> prevSegment;
    boost::optional<Point2> prevPoint;
    boost::optional<pt::ptime> prevTime;
    for (auto edgeId : edgeIds) {
        const Polyline2 polyline = mappedGraph().graph().edgeData(edgeId).geometry();
        for (size_t segmentIndex = 0;
                segmentIndex < polyline.segmentsNumber();
                ++ segmentIndex) {
            const auto& segment = polyline.segmentAt(segmentIndex);
            auto* point = points->Add();
            const auto matchedPoint = segment.pointByPosition(
                SEGMENT_POSITION);

            point->mutable_point()->set_lon(matchedPoint.x());
            point->mutable_point()->set_lat(matchedPoint.y());
            auto time = makeTime(
                matchedPoint,
                prevPoint,
                prevTime);
            point->set_timestamp(ptimeToTimestamp(time));
            prevPoint = matchedPoint;
            prevTime = time;

            auto* positionOnGraph = point->mutable_position_on_graph();
            positionOnGraph->set_persistent_edge_id(
                mappedGraph().persistentIndex().findLongId(edgeId).value().value());
            auto* polylinePosition = positionOnGraph->mutable_position();
            polylinePosition->set_segment_index(segmentIndex);
            polylinePosition->set_segment_position(SEGMENT_POSITION);
            if (prevSegment) {
                auto* pathPart = pathParts->Add();
                auto* persistent_edge_ids =
                    pathPart->mutable_persistent_edge_ids();
                if (prevSegment->edgeId != edgeId) {
                    persistent_edge_ids->Add(
                        mappedGraph().persistentIndex().findLongId(
                            prevSegment->edgeId).value().value()
                    );
                }
                persistent_edge_ids->Add(
                    mappedGraph().persistentIndex().findLongId(edgeId).value().value()
                );
                Cerr << edgeId.value() << " , "
                    << mappedGraph().persistentIndex().findLongId(edgeId).value().value() << Endl;
                auto* source = pathPart->mutable_source_position();
                source->set_segment_index(prevSegment->segmentIndex.value());
                source->set_segment_position(SEGMENT_POSITION);
                auto* target = pathPart->mutable_target_position();
                target->set_segment_index(segmentIndex);
                target->set_segment_position(SEGMENT_POSITION);
            }
            prevSegment = SegmentId{edgeId, SegmentIndex(segmentIndex)};
        }
    }
    return result;
}

void comparePoints(
        const mp::Point& expected,
        const mp::Point& received) {
    UNIT_ASSERT_DOUBLES_EQUAL(
        expected.point().lon(),
        received.point().lon(),
        0.001);
    UNIT_ASSERT_DOUBLES_EQUAL(
        expected.point().lat(),
        received.point().lat(),
        0.001);
    UNIT_ASSERT_VALUES_EQUAL(expected.timestamp(), received.timestamp());
    UNIT_ASSERT_VALUES_EQUAL(
        expected.has_position_on_graph(),
        received.has_position_on_graph());
    if (expected.has_position_on_graph() && received.has_position_on_graph()) {
        UNIT_ASSERT_VALUES_EQUAL(
            expected.position_on_graph().persistent_edge_id(),
            received.position_on_graph().persistent_edge_id()
        );
        UNIT_ASSERT_VALUES_EQUAL(
            expected.position_on_graph().position().segment_index(),
            received.position_on_graph().position().segment_index()
        );
        UNIT_ASSERT_DOUBLES_EQUAL(
            expected.position_on_graph().position().segment_position(),
            received.position_on_graph().position().segment_position(),
            0.00001
        );
    }
}

void compareParts(
        const mp::PathPart& expected,
        const mp::PathPart& received) {
    UNIT_ASSERT_VALUES_EQUAL(
        expected.persistent_edge_ids().size(),
        received.persistent_edge_ids().size());
    for (int i = 0; i < expected.persistent_edge_ids().size(); ++i) {
        UNIT_ASSERT_VALUES_EQUAL(
            expected.persistent_edge_ids()[i],
            received.persistent_edge_ids()[i]);
    }
    UNIT_ASSERT_VALUES_EQUAL(
        expected.source_position().segment_index(),
        received.source_position().segment_index()
    );
    UNIT_ASSERT_DOUBLES_EQUAL(
        expected.source_position().segment_position(),
        received.source_position().segment_position(),
        0.00001
    );
    UNIT_ASSERT_VALUES_EQUAL(
        expected.target_position().segment_index(),
        received.target_position().segment_index()
    );
    UNIT_ASSERT_DOUBLES_EQUAL(
        expected.target_position().segment_position(),
        received.target_position().segment_position(),
        0.00001
    );

}

void comparePaths(
        const mp::MatchedPath& expected,
        const mp::MatchedPath& received) {
    UNIT_ASSERT_VALUES_EQUAL(expected.points().size(), received.points().size());
    for (int i = 0; i < expected.points().size(); ++ i) {
        comparePoints(expected.points()[i], received.points()[i]);
    }
    UNIT_ASSERT_VALUES_EQUAL(expected.path_parts().size(), received.path_parts().size());
    for (int i = 0; i < expected.path_parts().size(); ++ i) {
        compareParts(expected.path_parts()[i], received.path_parts()[i]);
    }
}

Y_UNIT_TEST_SUITE(matcher_processor) {
Y_UNIT_TEST(match_long_thack) {
    MatchingProcessor processor(
        mappedGraph(),
        std::make_shared<maps::analyzer::shortest_path::ShortestPathsLRUCache>(),
        mapmatcherConfig()
    );
    for (auto& signal : makeSignals({EdgeId(199026), EdgeId(100574)})) {
        processor.add(signal);
        processor.processBuffered();
    };
    processor.processBuffered();
    auto expectedPath = makeMatchedPath({EdgeId(199026), EdgeId(100574)});
    const auto& path = processor.matchedPath();
    Cerr << "expected path: " << expectedPath.DebugString() << Endl;
    Cerr << "---------------------------------------------" << Endl;
    Cerr << "received path: " << path.DebugString()<< Endl;
    comparePaths(expectedPath, path);
}

Y_UNIT_TEST(match_points) {
    const Config CONFIG = [] {
        Config c;
        c.graph = BinaryPath("maps/data/test/graph3/road_graph.fb");
        c.edgesPersistentIndex = BinaryPath("maps/data/test/graph3/edges_persistent_index.fb");
        c.edgesRTree = BinaryPath("maps/data/test/graph3/rtree.fb");
        c.mapmatcherConfig = ArcadiaSourceRoot() + "/maps/analyzer/libs/graphmatching/conf/online.json";
        c.signalsHistoryLength = 30;
        c.signalsStorageTime = 7200;
        c.lockMemory = true;
        return c;
    }();
    const MappedGraph MAPPED_GRAPH(CONFIG);
    const MatcherConfig MATCHER_CONFIG = MatcherConfig::fromFile(CONFIG.mapmatcherConfig);

    const std::shared_ptr<maps::analyzer::shortest_path::ShortestPathsCache> cache =
        std::make_shared<maps::analyzer::shortest_path::ShortestPathsThreadedCache<>>(MAPPED_GRAPH.graph());

    const std::unique_ptr<MatchingProcessorFactory> factory =
        std::make_unique<MatchingProcessorFactory>(MAPPED_GRAPH, cache, MATCHER_CONFIG);

    const std::vector<GpsSignal> signals {
        makeSignal(1502379981, 37.663558, 55.628578),
        makeSignal(1502379995, 37.663412, 55.629947),
        makeSignal(1502380020, 37.661263, 55.63101),
        makeSignal(1502380030, 37.659753, 55.632015),
        makeSignal(1502380040, 37.65903, 55.633928),
        makeSignal(1502380045, 37.658477, 55.634695),
        makeSignal(1502380055, 37.65735, 55.635063),
        makeSignal(1502380070, 37.656257, 55.63601),
        makeSignal(1502380085, 37.656158, 55.636315)
    };

    GpsSignalSequence sequence = GpsSignalSequence(*factory, std::nullopt);
    for (const GpsSignal& signal : signals) {
        sequence.addSignal(signal);
    }

    const yandex::maps::proto::matcher::MatchedPath& path = sequence.matchedPath();
    UNIT_ASSERT_VALUES_EQUAL(path.points().size(), 9);

    int matched_count = 0;
    Cerr << "Result of matching:" << Endl;
    for (const auto& p : path.points()) {
        if (p.position_on_graph().persistent_edge_id() != 0) {
            ++matched_count;
        }
        Cerr << p.point().lat() << " " << p.point().lon() << " " << p.timestamp()
            << " " << p.position_on_graph().persistent_edge_id() << Endl;
    }

    // The current implementation matches 5 signals from 9 input signals.
    // Check for this minimum.
    UNIT_ASSERT_GE(matched_count, 5);
}
}
