#include <library/cpp/testing/unittest/registar.h>

#include "../helpers.h"

#include <maps/libs/geolib/include/intersection.h>

#include <chrono>
#include <algorithm>
#include <iostream>

namespace maps {
namespace gpstiles_realtime {
namespace index {
namespace test {

struct TestData {
    std::vector<TimePoint> ts;
    std::vector<geolib3::Point2> ps;
    TimeInterval indexTimeInterval;
    Query query;
};

void runTest(const TestData& data)
{
    PointsVector points, expPs;
    for (const auto& t : data.ts) {
        for (const auto& p : data.ps) {
            points.push_back({p, t});
            if (data.query.timeInterval().contains(t)
                && geolib3::intersects(data.query.bbox(), p.boundingBox()))
            {
                expPs.push_back({p, t});
            }
        }
    }

    auto idx = createTestIntervalIndex(data.indexTimeInterval, std::move(points));
    const auto& res = idx.find(data.query);

    auto iStart = std::lower_bound(
        data.ts.begin(), data.ts.end(), data.query.timeInterval().start());
    auto iEnd = std::upper_bound(
        data.ts.begin(), data.ts.end(), data.query.timeInterval().end());

    if (iEnd == data.ts.begin() || iStart == data.ts.end()) {
        // disjoint intervals
        UNIT_ASSERT(!res);
        return;
    }

    UNIT_ASSERT(iStart < iEnd);
    UNIT_ASSERT(res);

    PointsVector recvPs;
    for (auto it = res->begin(); it != res->end(); ++it) {
        recvPs.push_back(*it);
    }

    UNIT_ASSERT(recvPs.size() == expPs.size());
    for (const auto& p : expPs) {
        UNIT_ASSERT(std::find(recvPs.begin(), recvPs.end(), p) != recvPs.end());
    }
    for (const auto& p : recvPs) {
        UNIT_ASSERT(std::find(expPs.begin(), expPs.end(), p) != expPs.end());
    }
}

Y_UNIT_TEST_SUITE(interval_index_tests)
{

Y_UNIT_TEST(filter_points_out_of_interval_test)
{
    auto idx = createTestIntervalIndex(
        TimeInterval {getTime(3), getTime(5)},
        {
            {{-1.0, 1.0}, getTime(0)},
            {{1.0, -1.0}, getTime(2)}
        });
    Query q = {
        TimeInterval {getTime(3), getTime(5)},
        {{-5.0, -5.0}, {5.0, 5.0}}
    };

    auto res = idx.find(q);
    UNIT_ASSERT(res
        && std::count_if(
            res->begin(), res->end(),
            [] (const auto&) { return true; }) == 0);
}

Y_UNIT_TEST(disjoint_interval_test)
{
    TestData data {
        TimePointsVector {getTime(0), getTime(2)},
        {
            {-1.0, 1.0},
            {-10.0, 10.0}
        },
        TimeInterval {getTime(0), getTime(5)},
        Query {
            TimeInterval {getTime(-2), getTime(-1)},
            {{-5.0, -5.0}, {5.0, 5.0}}
        }
    };

    runTest(data);
    data.query = {{getTime(6), getTime(10)}, data.query.bbox()};
    runTest(data);
}

Y_UNIT_TEST(touching_interval_test)
{
    TestData data {
        TimePointsVector {getTime(0), getTime(5)},
        {
            {-1.0, 1.0},
            {1.0, -1.0},
            {-10.0, 10.0},
            {10.0, -10.0}
        },
        TimeInterval {getTime(0), getTime(5)},
        Query {
            TimeInterval {getTime(-5), getTime(0)},
            {{-5.0, -5.0}, {5.0, 5.0}}
        }
    };

    runTest(data);

    data.query = {{getTime(5), getTime(10)}, data.query.bbox()};
    runTest(data);
}

Y_UNIT_TEST(covered_interval_test)
{
    TestData data {
        TimePointsVector {getTime(0), getTime(5)},
        {
            {-1.0, 1.0},
            {1.0, -1.0},
            {-10.0, -10.0},
            {10.0, 10.0}
        },
        TimeInterval {getTime(0), getTime(5)},
        Query {
            TimeInterval {getTime(-5), getTime(5)},
            {{-5.0, -5.0}, {5.0, 5.0}}
        }
    };

    runTest(data);
}

Y_UNIT_TEST(contained_interval_test)
{
    TestData data {
        TimePointsVector {getTime(0), getTime(2), getTime(5), getTime(10)},
        {
            {-1.0, 1.0},
            {1.0, -1.0},
            {-10.0, -10.0},
            {10.0, 10.0}
        },
        TimeInterval {getTime(0), getTime(10)},
        Query {
            TimeInterval {getTime(2), getTime(5)},
            {{-5.0, -5.0}, {5.0, 5.0}}
        }
    };

    runTest(data);
}

Y_UNIT_TEST(overlapping_intervals_test)
{
    TestData data {
        TimePointsVector {getTime(2), getTime(7)},
        {
            {-1.0, 1.0},
            {1.0, -1.0},
            {-10.0, 10.0},
            {10.0, -10.0}
        },
        TimeInterval {getTime(0), getTime(10)},
        Query {
            TimeInterval {getTime(5), getTime(12)},
            {{-5.0, -5.0}, {5.0, 5.0}}
        }
    };

    runTest(data);

    data.query = {{getTime(-2), getTime(3)}, data.query.bbox()};
    runTest(data);
}

Y_UNIT_TEST(empty_bbox_query_test)
{
    TestData data {
        TimePointsVector {getTime(0), getTime(10)},
        {
            {-10.0, 10.0},
            {10.0, -10.0}
        },
        TimeInterval {getTime(0), getTime(10)},
        Query {
            TimeInterval {getTime(0), getTime(10)},
            {{-5.0, -5.0}, {5.0, 5.0}}
        }
    };

    runTest(data);
}

} // test suite end

} // namespace test
} // namespace index
} // namespace gpstiles_realtime
} // namespace maps
