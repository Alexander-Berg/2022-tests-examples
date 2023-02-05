#pragma once

#include <maps/wikimap/gpstiles_realtime/libs/index/include/interval_index.h>
#include <maps/wikimap/gpstiles_realtime/libs/index/include/index.h>

#include <maps/libs/geolib/include/point.h>

#include <vector>
#include <chrono>

namespace maps {
namespace gpstiles_realtime {
namespace index {
namespace test {

using Clock = TimePoint::clock;


TimePoint getTime(int sec)
{
    using std::chrono::seconds;
    static const TimePoint T_0 = TimePoint() + seconds(10000);
    return sec >= 0 ? T_0 + seconds(sec) : T_0 - seconds(-sec);
}


struct TestPoint {
    geolib3::Point2 point;
    TimePoint timePoint_;

    const TimePoint& time() const { return timePoint_; }

    bool operator == (const TestPoint& o) const
    {
        return point == o.point && timePoint_ == o.timePoint_;
    }
};

using TimePointsVector = std::vector<TimePoint>;
using PointsVector = std::vector<TestPoint>;
using TestIntervalIndex = IntervalIndex<TestPoint, geolib3::Point2>;
using TestIndex = Index<TestPoint, geolib3::Point2>;
using TestIndexPtr = std::shared_ptr<TestIndex>;

using TestBuilder = IntervalIndexBuilder<TestPoint, geolib3::Point2>;

TestBuilder g_testBuilder(
    [] (const TestPoint& p) { return &p.point; },
    Grid {{{-30.0, -30.0}, {30.0, 30.0}}, 4, 4});

inline TestIntervalIndex createTestIntervalIndex(
    const TimeInterval& timeInterval,
    PointsVector&& data)
{
    return g_testBuilder(timeInterval, std::move(data));
}

using TestIntervalIndexData = std::pair<TimeInterval, PointsVector>;
using TestIndexData = std::vector<TestIntervalIndexData>;

inline TestIndexPtr createTestIndex(TestIndexData&& data)
{
    auto i = std::make_shared<TestIndex>();
    for (auto&& d : data) {
        i->add(g_testBuilder(std::move(d.first), std::move(d.second)));
    }
    return i;
}

} // namespace test
} // namespace index
} // namespace gpstiles_realtime
} // namespace maps
