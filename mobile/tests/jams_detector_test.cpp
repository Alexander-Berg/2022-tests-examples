#include "../jams_detector.h"

#include <yandex/maps/mapkit/geometry/geo/geo.h>

#include <boost/test/auto_unit_test.hpp>

using namespace yandex::maps::navikit::routing;
using namespace yandex::maps;

namespace driving = yandex::maps::mapkit::directions::driving;

namespace {

static const double DISTANT_POINT_THRESEHOLD_M = 10; // 10 meters
static const double TEST_PRECISION_SEC = 1; // 1 second

// Distance should be ±100 meters betweeen following points
static const mapkit::geometry::Point ANCHOR_ONE = mapkit::geometry::Point(55.733363, 37.587707);
static const mapkit::geometry::Point ANCHOR_TWO = mapkit::geometry::Point(55.733959, 37.588940);

mapkit::geometry::Point advancePoint(const mapkit::geometry::Point& point)
{
    return mapkit::geometry::geo::distance(ANCHOR_ONE, point) > DISTANT_POINT_THRESEHOLD_M ?
        ANCHOR_ONE : ANCHOR_TWO;
}

struct MyTestCase {
    mapkit::geometry::Polyline polyline;
    std::vector<mapkit::directions::driving::JamSegment> segments;

    std::vector<JamInfo> expected;

    void drive(double time, mapkit::directions::driving::JamType jam)
    {
        if (polyline.points->empty()) {
            polyline.points->push_back(ANCHOR_ONE);
        }

        mapkit::geometry::Point previous = polyline.points->back();
        mapkit::geometry::Point next = advancePoint(previous);

        double distance = mapkit::geometry::geo::distance(previous, next);
        BOOST_CHECK(distance > DISTANT_POINT_THRESEHOLD_M);

        double speed = distance / time;

        polyline.points->push_back(next);
        segments.emplace_back(jam, speed);
    }
};

bool check(const MyTestCase& my)
{
    std::vector<JamInfo> result = detectJams(jamSegmentDurations(my.polyline, my.segments));

    if (result.size() != my.expected.size())
        return false;

    auto itResult = result.begin();
    auto itExpected = my.expected.begin();

    for (; itResult < result.end(); ++itResult, ++itExpected) {
        if ((itResult->segmentStart != itExpected->segmentStart) ||
            (itResult->segmentCount != itExpected->segmentCount) ||
            std::fabs(itResult->info.time - itExpected->info.time) > TEST_PRECISION_SEC) {
            return false;
        }
    }
    return true;
}

} // ns

BOOST_AUTO_TEST_SUITE(RoutingJamsDetector)

BOOST_AUTO_TEST_CASE(checkDistancePoints)
{
    bool distantEnough = mapkit::geometry::geo::distance(ANCHOR_ONE, ANCHOR_TWO) > DISTANT_POINT_THRESEHOLD_M;
    BOOST_CHECK(distantEnough);
}

BOOST_AUTO_TEST_CASE(checkSegmentCompute)
{
    MyTestCase test;

    test.drive(1*60, driving::JamType::Free);
    test.drive(3*60, driving::JamType::Hard);
    test.drive(1*60, driving::JamType::Light);

    auto info = jamSegmentDurations(test.polyline, test.segments);

    BOOST_CHECK(info.size() == test.segments.size());

    BOOST_CHECK_CLOSE(info[0].info.time, 1*60, TEST_PRECISION_SEC);
    BOOST_CHECK_CLOSE(info[1].info.time, 3*60, TEST_PRECISION_SEC);
    BOOST_CHECK_CLOSE(info[2].info.time, 1*60, TEST_PRECISION_SEC);

    BOOST_CHECK(info[0].type == driving::JamType::Free);
    BOOST_CHECK(info[1].type == driving::JamType::Hard);
    BOOST_CHECK(info[2].type == driving::JamType::Light);
}

BOOST_AUTO_TEST_CASE(checkJamPrecompute)
{
    MyTestCase test;

    test.drive(1*60, driving::JamType::Hard);    // 1 + 1
    test.drive(3*60, driving::JamType::Free);
    test.drive(0.05*60, driving::JamType::Free);
    test.drive(1*60, driving::JamType::Light);   // 3 + 2
    test.drive(4*60, driving::JamType::Light);
    test.drive(1*60, driving::JamType::Free);

    auto info = jamSegmentDurations(test.polyline, test.segments);

    BOOST_CHECK(info.size() == test.segments.size());

    std::vector<JamInfo> jams = {
        {0, 1, TimeDistance { 1*60, 1*200 }},
        {3, 2, TimeDistance { 5*60, 5*200 }}
    };

    auto precomputed = precomputeJamTimes(info, jams);

    BOOST_CHECK(precomputed.size() == test.segments.size());

    // First jam
    BOOST_CHECK(precomputed[0]);

    BOOST_CHECK_CLOSE(precomputed[0]->info.time, 1*60, TEST_PRECISION_SEC);
    BOOST_CHECK_CLOSE(precomputed[0]->beforeJam.time, 0, TEST_PRECISION_SEC);
    BOOST_CHECK_CLOSE(precomputed[0]->jamTotal.time, 1*60, TEST_PRECISION_SEC);

    // Second jam
    BOOST_CHECK_CLOSE(precomputed[1]->info.time, 3*60, TEST_PRECISION_SEC);
    BOOST_CHECK_CLOSE(precomputed[1]->beforeJam.time, 3.05*60, TEST_PRECISION_SEC);
    BOOST_CHECK_CLOSE(precomputed[1]->jamTotal.time, 5*60, TEST_PRECISION_SEC);

    BOOST_CHECK(precomputed[5] == boost::none);  // last segment should be none since there is no jam

    auto time = leftInJam(precomputed, mapkit::geometry::PolylinePosition(0, 0));
    BOOST_CHECK(time);
    BOOST_CHECK_CLOSE(time->time, 1*60, TEST_PRECISION_SEC);

    time = leftInJam(precomputed, mapkit::geometry::PolylinePosition(1, 0));
    BOOST_CHECK(!time); // 3 minutes before next jam -- too far

    // right before jam
    time = leftInJam(precomputed, mapkit::geometry::PolylinePosition(2, 0.9));
    BOOST_CHECK(time);
    BOOST_CHECK_CLOSE(time->time, 5*60 + 0.05*60, TEST_PRECISION_SEC);

    // inside jam
    time = leftInJam(precomputed, mapkit::geometry::PolylinePosition(4, .5));
    BOOST_CHECK(time);
    BOOST_CHECK_CLOSE(time->time, 2*60, TEST_PRECISION_SEC);
}

BOOST_AUTO_TEST_CASE(detectJamsTest1)
{
    MyTestCase test;

    test.drive(1*60, driving::JamType::Free);
    test.drive(3*60, driving::JamType::Light);    // 3 |
    test.drive(0.95*60, driving::JamType::Hard);  // 1 |
    test.drive(0.05*60, driving::JamType::Free);  // 0 | ==> 5 min jam (segments 1 + 4)
    test.drive(1*60, driving::JamType::Light);    // 1 |
    test.drive(1*60, driving::JamType::Free);
    test.drive(8*60, driving::JamType::Free);

    test.expected = {
        {1, 4, TimeDistance { 5*60, 5*200 } }
    };

    BOOST_CHECK(check(test));
}

BOOST_AUTO_TEST_CASE(detectJamsTest2)
{
    MyTestCase test;

    test.drive(20*60, driving::JamType::Free);   // 1 |
    test.drive(30*60, driving::JamType::Free);   // 3 |
    test.drive(8*60, driving::JamType::Free);    // 1 | free

    test.expected = { };

    BOOST_CHECK(check(test));
}

BOOST_AUTO_TEST_CASE(detectJamsTest3)
{
    MyTestCase test;

    test.drive(20*60, driving::JamType::Hard);   // |
    test.drive(30*60, driving::JamType::Light);  // |
    test.drive(8*60, driving::JamType::Hard);    // | ==> 58 min jam (segments 1 + 3)

    test.expected = {
        {0, 3, TimeDistance { 58*60, 58*200 } }
    };

    BOOST_CHECK(check(test));
}

BOOST_AUTO_TEST_CASE(detectJamsTest4)
{
    MyTestCase test;

    test.drive(1*60, driving::JamType::Light);    // |
    test.drive(3*60, driving::JamType::Blocked);  // |
    test.drive(1*60, driving::JamType::Hard);     // | ==> 6 min
    test.drive(1*60, driving::JamType::Light);    // |
    test.drive(1*60, driving::JamType::Free);
    test.drive(8*60, driving::JamType::Unknown);
    test.drive(1*60, driving::JamType::Free);
    test.drive(1*60, driving::JamType::Hard);   // | ==> 1 min (@16 min)
    test.drive(5*60, driving::JamType::Free);
    test.drive(5*60, driving::JamType::Hard);   // | ==> 5 min (@21 min)

    test.expected = {
        {0, 4, TimeDistance { 6*60, 6*200 } },
        {7, 1, TimeDistance { 1*60, 1*200 } },
        {9, 1, TimeDistance { 5*60, 5*200 } }
    };

    BOOST_CHECK(check(test));
}

BOOST_AUTO_TEST_CASE(detectJamsTest5)
{
    MyTestCase test;
    test.drive(1*60, driving::JamType::Free);   // 1 |

    test.expected = { };

    BOOST_CHECK(check(test));
}

BOOST_AUTO_TEST_CASE(detectJamsTest6)
{
    MyTestCase test;

    test.drive(1*60, driving::JamType::Hard);   // 1 |

    test.expected = { {0, 1, TimeDistance { 1*60, 1*200 } } };

    BOOST_CHECK(check(test));
}

BOOST_AUTO_TEST_SUITE_END()
