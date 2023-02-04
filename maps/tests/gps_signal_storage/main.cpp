#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <maps/analyzer/modules/matcher/matcher/lib/gps_signal_storage.h>

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

const std::shared_ptr<maps::analyzer::shortest_path::ShortestPathsCache> cache()
{
    const static std::shared_ptr<maps::analyzer::shortest_path::ShortestPathsCache> CACHE =
        std::make_shared<maps::analyzer::shortest_path::ShortestPathsThreadedCache<>>(mappedGraph().graph());
    return CACHE;
}

const MatcherConfig& mapmatcherConfig() {
    const static MatcherConfig MATCHER_CONFIG = MatcherConfig::fromFile(config().mapmatcherConfig);
    return MATCHER_CONFIG;
}

const MatchingProcessorFactory& matchingProcessorFactory()
{
    const static MatchingProcessorFactory FACTORY(
        mappedGraph(),
        cache(),
        mapmatcherConfig()
    );
    return FACTORY;
}

pt::ptime makeTime(
    const Point2& point,
    const boost::optional<Point2>& prevPoint,
    const boost::optional<pt::ptime>& prevTime)
{
    pt::ptime time;
    if (prevPoint && prevTime) {
        const auto distance = geoDistance(point, *prevPoint);
        int delta = distance / SPEED + 1;
        time = *prevTime + (pt::seconds(delta));
    } else {
        const static pt::ptime START_TIME = maps::nowUtc();
        time = START_TIME;
    }
    return time;
}

GpsSignal makeSignal(const Point2& point, boost::optional<GpsSignal> prev)
{
    pt::ptime time;
    if (prev) {
        time = makeTime(point, Point2(prev->lon(), prev->lat()), prev->time());
    } else {
        time = makeTime(point, {}, {});
    }
    GpsSignal signal;
    signal.setAverageSpeed(SPEED);
    signal.setClid(CLID);
    signal.setLat(point.y());
    signal.setLon(point.x());
    signal.setTime(time);
    signal.setUuid(UUID);
    return signal;
}

GpsSignal makeSignal(const Segment2& segment,
    double position,
    boost::optional<GpsSignal> prev)
{
    auto point = segment.pointByPosition(position);
    return makeSignal(point, prev);
}

std::vector<GpsSignal> makeSignals(const std::vector<EdgeId>& edgeIds)
{
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

namespace maps::analyzer::data {

bool operator==(const GpsSignal& x, const GpsSignal& y)
{
    return x.data().DebugString() == y.data().DebugString();
}

} // namespace maps::analyzer::data

namespace maps::analyzer::matcher {

bool operator==(const GpsSignalSequence& x, const GpsSignalSequence& y)
{
    auto l = x.signals_;
    std::copy(x.buffer_.begin(), x.buffer_.end(), std::back_inserter(l));
    auto r = y.signals_;
    std::copy(y.buffer_.begin(), y.buffer_.end(), std::back_inserter(r));
    return l == r;
}

bool operator==(const GpsSignalStorage& x, const GpsSignalStorage& y)
{
    std::unique_lock<std::mutex> guardX(x.mutex_);
    std::unique_lock<std::mutex> guardY(y.mutex_);
    if (x.data_.size() != y.data_.size()) {
        return false;
    }
    for (auto& [uuid, item]: x.data_) {
        const auto& l = *GpsSignalSequenceGuard(item.sequence);
        const auto& r = *GpsSignalSequenceGuard(y.data_.at(uuid).sequence);
        if ( !(r == l) )  {
            return false;
        }
    }
    return true;
}

class MockNow
{
public:
    MockNow()
        : now_(std::chrono::seconds(0))
    {
    }

    void turnArrows(const std::chrono::seconds& seconds)
    {
        now_ += seconds;
    }

    std::chrono::system_clock::time_point operator()()
    {
        return now_;
    }

private:
    std::chrono::system_clock::time_point now_;
};

void setCustomNow(
    GpsSignalStorage* storage,
    std::function<std::chrono::system_clock::time_point(void)> customNow)
{
    ASSERT(storage);
    storage->setCustomNow(customNow);
}

} // namespace maps::analyzer::matcher


Y_UNIT_TEST_SUITE(gps_signal_storage)
{
    Y_UNIT_TEST(lru)
    {
        GpsSignalStorage storage({30, std::chrono::seconds(5)}, matchingProcessorFactory());
        MockNow mockNow;
        setCustomNow(&storage, [&]{return mockNow();});

        auto sequence1 = storage.acquireGpsSignalSequence("1");
        for (const auto& signal : makeSignals({ EdgeId(199026) })) {
            sequence1->addSignalDeferred(signal);
        };
        auto sequence2 = storage.acquireGpsSignalSequence("2");
        for (const auto& signal : makeSignals({ EdgeId(199026) })) {
            sequence2->addSignalDeferred(signal);
        };
        UNIT_ASSERT_EQUAL(storage.size(), 2);

        mockNow.turnArrows(std::chrono::seconds(10));
        storage.cleanup();

        auto sequence3 = storage.acquireGpsSignalSequence("3");
        for (const auto& signal : makeSignals({ EdgeId(199026) })) {
            sequence3->addSignalDeferred(signal);
        };
        UNIT_ASSERT_EQUAL(storage.size(), 1);
    }

    Y_UNIT_TEST(rematch)
    {
        GpsSignalStorage sourceStorage({}, matchingProcessorFactory());
        {
            auto sequence = sourceStorage.acquireGpsSignalSequence("1");
            for (const auto& signal : makeSignals({ EdgeId(199026) })) {
                sequence->addSignalDeferred(signal);
            };
            sequence->matchedPath();
            for (const auto& signal : makeSignals({ EdgeId(100574), EdgeId(198915) })) {
                sequence->addSignal(signal);
            };
            sequence->matchedPath();
        }

        GpsSignalStorage targertStorage({}, matchingProcessorFactory());;
        {
            auto sequence = targertStorage.acquireGpsSignalSequence("1");

            for (const auto& signal : makeSignals({ EdgeId(100574), EdgeId(198915) })) {
                sequence->addSignal(signal);
            };
            sequence->matchedPath();
        }

        UNIT_ASSERT_EQUAL(
            sourceStorage.acquireGpsSignalSequence("1")->matchedPath().DebugString(),
            targertStorage.acquireGpsSignalSequence("1")->matchedPath().DebugString()
        );
    }
}
