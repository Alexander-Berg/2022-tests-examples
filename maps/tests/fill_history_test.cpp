#include <maps/wikimap/gpsrealtime_hypgen/libs/gpsrt_lib/history_filler.h>
#include <maps/libs/chrono/include/time_point.h>

#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::gpsrealtime_hypgen {

inline time_t timeFromString(std::string str)
{
    return chrono::convertToUnixTime(chrono::parseSqlDateTime(str));
}

Y_UNIT_TEST_SUITE(history) {
    Y_UNIT_TEST(testFillHistory) {
        EdgeIds edgeIds{33,-44,55};

        time_t nowTs1 = timeFromString("2018-10-19 01:00:00+00:00"); /* Fri */
        auto now = chrono::TimePoint::clock::from_time_t(nowTs1);

        Closure cl1("infopoint2",
                    timeFromString("2018-10-19 01:00:00+00:00") /* Fri */,
                    timeFromString("2018-10-20 14:00:00+00:00") /* Sat */,
                    geolib3::Polyline2(),
                    geolib3::Point2(30.376222,59.86370),
                    "graph_version",
                    now,
                    now,
                    edgeIds
            );

        auto times = findStartPointsToStoreHistory(cl1, -7200, nowTs1);
        UNIT_ASSERT_EQUAL(times.workday, timeFromString("2018-10-17 02:00:00+00:00")); // Wed
        UNIT_ASSERT_EQUAL(times.weekend, timeFromString("2018-10-14 02:00:00+00:00")); // Sun

        times = findStartPointsToStoreHistory(cl1, 10800, nowTs1);
        UNIT_ASSERT_EQUAL(times.workday, timeFromString("2018-10-17 21:00:00+00:00")); // Wed
        UNIT_ASSERT_EQUAL(times.weekend, timeFromString("2018-10-13 21:00:00+00:00")); // Sat

        time_t nowTs2 = timeFromString("2018-10-18 11:30:00+00:00"); /* Thu */
        Closure cl2("infopoint1",
                    timeFromString("2018-10-18 11:30:00+00:00") /* Thu */,
                    timeFromString("2018-10-19 14:00:00+00:00") /* Fri */,
                    geolib3::Polyline2(),
                    geolib3::Point2(30.376222,59.86370),
                    "graph_version",
                    now,
                    now,
                    edgeIds
            );

        times = findStartPointsToStoreHistory(cl2, 10800, nowTs2);
        UNIT_ASSERT_EQUAL(times.workday, timeFromString("2018-10-16 21:00:00+00:00")); // Thu
        UNIT_ASSERT_EQUAL(times.weekend, 0);

        times = findStartPointsToStoreHistory(cl2, -10800, nowTs2);
        UNIT_ASSERT_EQUAL(times.workday, timeFromString("2018-10-17 03:00:00+00:00")); // Wed
        UNIT_ASSERT_EQUAL(times.weekend, 0);
    }
} // test suite end

} // maps::wiki::gpsrealtime_hypgen
