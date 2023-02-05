#include <maps/wikimap/gpsrealtime_hypgen/libs/gpsrt_lib/common/globals.h>
#include <maps/wikimap/gpsrealtime_hypgen/libs/gpsrt_lib/db/common.h>
#include <maps/wikimap/gpsrealtime_hypgen/libs/gpsrt_lib/generator.h>

#include <library/cpp/testing/unittest/registar.h>
#include <maps/libs/chrono/include/time_point.h>

namespace maps::wiki::gpsrealtime_hypgen {

class FakeGeobase : public jams_arm2::IGeobase {
public:
    FakeGeobase(time_t shift) : shift_(shift) {}
    int getRegionId(geolib3::Point2) const override {return 0;}
    std::string getRegionName(int) const override {return "";}
    std::chrono::seconds getTimezoneShift(int, chrono::TimePoint) const override
    {
        return std::chrono::seconds(shift_);
    }
private:
    time_t shift_;
};

inline time_t timeFromString(std::string str)
{
    return chrono::convertToUnixTime(chrono::parseSqlDateTime(str));
}
inline uint32_t hourFromString(std::string str)
{
    return timeFromString(str) / PART_SIZE_SECONDS;
}

Y_UNIT_TEST_SUITE(generator) {
    Y_UNIT_TEST(testOldHistoryPositiveTimeShift) {

        Globals::get().initHypgenForTest("test_config.conf", std::make_shared<FakeGeobase>(10800));

        EdgeIds edgeIds{33,-44,55};

        time_t nowTs1 = timeFromString("2018-10-19 12:00:00+00:00"); /* Fri */
        uint32_t lastHour1 = nowTs1 / PART_SIZE_SECONDS;
        auto now = chrono::TimePoint::clock::from_time_t(nowTs1);

        TimedTrafficIndex index;
        index[33][lastHour1] = 17;
        index[-44][lastHour1] = 15;
        index[55][lastHour1] = 19;

        index[33][lastHour1 - 1] = 15;
        index[-44][lastHour1 - 1] = 14;
        index[55][lastHour1 - 1] = 16;

        index[33][lastHour1 - 2] = 7;
        index[-44][lastHour1 - 2] = 11;
        index[55][lastHour1 - 2] = 9;

        Closure cl("infopoint1",
                   timeFromString("2018-10-18 11:30:00+00:00") /* Thu */,
                   timeFromString("2018-10-19 14:00:00+00:00") /* Fri */,
                   geolib3::Polyline2(),
                   geolib3::Point2(30.376222,59.86370),
                   "graph_version",
                   now,
                   now,
                   edgeIds
            );

        History hist({hourFromString("2018-10-17 21:00:00+00:00") /* Wed */,
                {3,0,0,0,0,0,0,0,0,11,7,13 /*12-00*/, 11,15,14,17,16,7,0,0,0,0,0,4} });
        cl.setWorkdayHistory(hist);

        auto cand = generateOneCandidateByOldHistory(
            lastHour1,
            index,
            cl);
        UNIT_ASSERT(cand);

        std::string result1 = R"(By old history:
current traffic
2018-10-19 11:00:00+00:00    15
2018-10-19 12:00:00+00:00    17
compared to
2018-10-18 11:00:00+00:00    14
2018-10-18 12:00:00+00:00    17
)";
        UNIT_ASSERT(cand->isCandidate && cand->comment == result1);

        uint32_t lastHour2 = hourFromString("2018-10-18 21:00:00+00:00"); /* Thu */

        index[33][lastHour2] = 5;
        index[-44][lastHour2] = 5;
        index[55][lastHour2] = 5;

        index[33][lastHour2 - 1] = 6;
        index[-44][lastHour2 - 1] = 6;
        index[55][lastHour2 - 1] = 6;

        cand = generateOneCandidateByOldHistory(
            lastHour2,
            index,
            cl);
        UNIT_ASSERT(cand);

        std::string result2 = R"(By old history:
current traffic
2018-10-18 20:00:00+00:00    6
2018-10-18 21:00:00+00:00    5
compared to
2018-10-18 20:00:00+00:00    4
2018-10-17 21:00:00+00:00    3
)";
        UNIT_ASSERT(cand->isCandidate && cand->comment == result2);

        uint32_t lastHour3 = hourFromString("2018-10-19 11:00:00+00:00"); /* Fri */
        cand = generateOneCandidateByOldHistory(
            lastHour3,
            index,
            cl);
        UNIT_ASSERT(!cand);

        cl.addHypothesis(335, HypType::Moving);
        cand = generateOneCandidateByOldHistory(
            lastHour3,
            index,
            cl);
        UNIT_ASSERT(cand);

        std::string result3 = R"(By old history:
current traffic
2018-10-19 10:00:00+00:00    9
2018-10-19 11:00:00+00:00    15
compared to
2018-10-18 10:00:00+00:00    15
2018-10-18 11:00:00+00:00    14
)";
        UNIT_ASSERT(!cand->isCandidate && cand->comment == result3);

    }

    Y_UNIT_TEST(testOldHistoryNegativeTimeShiftAndDuration) {

        Globals::get().initHypgenForTest("test_config.conf", std::make_shared<FakeGeobase>(-7200));

        EdgeIds edgeIds{33,-44,55};

        time_t nowTs = timeFromString("2018-10-20 01:00:00+00:00"); /* Sat */
        uint32_t lastHour = nowTs / PART_SIZE_SECONDS;
        auto now = chrono::TimePoint::clock::from_time_t(nowTs);

        TimedTrafficIndex index;
        index[33][lastHour] = 7;
        index[-44][lastHour] = 5;
        index[55][lastHour] = 9;

        index[33][lastHour - 1] = 15;
        index[-44][lastHour - 1] = 14;
        index[55][lastHour - 1] = 16;

        index[33][lastHour - 2] = 7;
        index[-44][lastHour - 2] = 11;
        index[55][lastHour - 2] = 9;

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

        History hist({hourFromString("2018-10-17 02:00:00+00:00") /* Wed */,
                {3,0,0,0,0,0,0,0,0,11,7,13 /*12-00*/, 18,15,14,17,16,7,0,0,0,10,11,4} });
        cl1.setWorkdayHistory(hist);

        auto cand = generateOneCandidateByOldHistory(
            lastHour,
            index,
            cl1);
        UNIT_ASSERT(cand);

        std::string result1 = R"(By old history:
current traffic
2018-10-20 00:00:00+00:00    15
2018-10-20 01:00:00+00:00    7
compared to
2018-10-18 00:00:00+00:00    11
2018-10-18 01:00:00+00:00    4
)";
        UNIT_ASSERT(cand->isCandidate && cand->comment == result1);

        Closure cl2("infopoint2",
                    timeFromString("2018-10-19 01:00:00+00:00") /* Fri */,
                    timeFromString("2018-10-21 14:00:00+00:00") /* Sun */,
                    geolib3::Polyline2(),
                    geolib3::Point2(30.376222,59.86370),
                    "graph_version",
                    now,
                    now,
                    edgeIds
            );

        cl2.setWorkdayHistory(hist);
        cl2.addHypothesis(335, HypType::Moving);

        cand = generateOneCandidateByOldHistory(
            lastHour,
            index,
            cl2);
        UNIT_ASSERT(cand);

        std::string result2 = R"(By old history:
current traffic
2018-10-19 23:00:00+00:00    9
2018-10-20 00:00:00+00:00    15
2018-10-20 01:00:00+00:00    7
compared to
2018-10-17 23:00:00+00:00    10
2018-10-18 00:00:00+00:00    11
2018-10-18 01:00:00+00:00    4
)";

        UNIT_ASSERT(!cand->isCandidate && cand->comment == result2);
    }

    Y_UNIT_TEST(testFindHourToCompareWith) {
        time_t nowT = timeFromString("2018-10-19 12:00:00+00:00"); /* Fri */
        uint32_t lastHour = nowT / PART_SIZE_SECONDS;
        uint32_t toCompare = findHourToCompareWith(lastHour, 10800);
        UNIT_ASSERT(toCompare == hourFromString("2018-10-18 12:00:00+00:00")); /* Thu */

        nowT = timeFromString("2018-10-20 12:00:00+00:00"); /* Sat */
        lastHour = nowT / PART_SIZE_SECONDS;
        toCompare = findHourToCompareWith(lastHour, 10800);
        UNIT_ASSERT(toCompare == hourFromString("2018-10-14 12:00:00+00:00")); /* Sun */

        nowT = timeFromString("2018-10-19 23:00:00+00:00"); /* Fri */
        lastHour = nowT / PART_SIZE_SECONDS;
        toCompare = findHourToCompareWith(lastHour, 10800);
        UNIT_ASSERT(toCompare == hourFromString("2018-10-13 23:00:00+00:00")); /* Sat */
    }

    Y_UNIT_TEST(testRecentHistory_1) {
        Globals::get().initHypgenForTest("test_config.conf", std::make_shared<FakeGeobase>(0));

        EdgeIds edgeIds{33,55};

        time_t nowTs = timeFromString("2018-10-20 01:00:00+00:00"); /* Sat */
        uint32_t lastHour = nowTs / PART_SIZE_SECONDS;
        auto now = chrono::TimePoint::clock::from_time_t(nowTs);

        time_t compareHour = findHourToCompareWith(lastHour, 10800);
        UNIT_ASSERT(compareHour == hourFromString("2018-10-14 01:00:00+00:00")); /* Sun */

        TimedTrafficIndex current;
        current[33][lastHour - 3] = 22;
        current[55][lastHour - 3] = 24;

        current[33][lastHour - 2] = 18;
        current[55][lastHour - 2] = 16;

        current[33][lastHour - 1] = 12;
        current[55][lastHour - 1] = 14;

        current[33][lastHour] = 15;
        current[55][lastHour] = 17;

        TimedTrafficIndex recent;
        recent[33][compareHour - 3] = 5;
        recent[55][compareHour - 3] = 4;

        recent[33][compareHour - 2] = 2;
        recent[55][compareHour - 2] = 3;

        recent[33][compareHour - 1] = 3;
        recent[55][compareHour - 1] = 4;

        recent[33][compareHour] = 1;
        recent[55][compareHour] = 2;

        Closure cl("infopoint2",
                   timeFromString("2018-10-19 01:00:00+00:00") /* Fri */,
                   timeFromString("2018-10-22 14:00:00+00:00") /* Mon */,
                   geolib3::Polyline2(),
                   geolib3::Point2(30.376222,59.86370),
                   "graph_version",
                   now,
                   now,
                   edgeIds
            );

        auto cand = generateOneCandidateByRecentHistory(
            lastHour, compareHour, current, recent, cl);
        UNIT_ASSERT(!cand);
    }

    Y_UNIT_TEST(testRecentHistory_2) {
        Globals::get().initHypgenForTest("test_config.conf", std::make_shared<FakeGeobase>(0));

        EdgeIds edgeIds{33,55};

        time_t nowTs = timeFromString("2018-10-20 22:00:00+00:00"); /* Sat */
        uint32_t lastHour = nowTs / PART_SIZE_SECONDS;
        auto now = chrono::TimePoint::clock::from_time_t(nowTs);
        time_t compareHour = findHourToCompareWith(lastHour, 10800);
        UNIT_ASSERT(compareHour == hourFromString("2018-10-19 22:00:00+00:00")); /* Fri */

        TimedTrafficIndex current;
        current[33][lastHour - 2] = 22;
        current[55][lastHour - 2] = 24;

        current[33][lastHour - 1] = 18;
        current[55][lastHour - 1] = 16;

        current[33][lastHour] = 12;
        current[55][lastHour] = 14;

        current[33][lastHour + 1] = 15;
        current[55][lastHour + 1] = 17;

        current[33][lastHour + 2] = 5;
        current[55][lastHour + 2] = 7;

        TimedTrafficIndex recent;
        recent[33][compareHour - 2] = 5;
        recent[55][compareHour - 2] = 4;

        recent[33][compareHour - 1] = 2;
        recent[55][compareHour - 1] = 3;

        recent[33][compareHour] = 3;
        recent[55][compareHour] = 4;

        recent[33][compareHour + 1] = 1;
        recent[55][compareHour + 1] = 2;

        Closure cl("infopoint2",
                   timeFromString("2018-10-19 01:00:00+00:00") /* Fri */,
                   timeFromString("2018-10-22 14:00:00+00:00") /* Mon */,
                   geolib3::Polyline2(),
                   geolib3::Point2(30.376222,59.86370),
                   "graph_version",
                   now,
                   now,
                   edgeIds
            );

        auto cand = generateOneCandidateByRecentHistory(
            lastHour, compareHour, current, recent, cl);
        /* 2018-10-20T22:00:00+00:00 Sat */
        UNIT_ASSERT(cand);

        std::string result1 = R"(By recent:
current traffic
2018-10-20 20:00:00+00:00    23
2018-10-20 21:00:00+00:00    17
2018-10-20 22:00:00+00:00    13
compared to
2018-10-19 20:00:00+00:00    4.5
2018-10-19 21:00:00+00:00    2.5
2018-10-19 22:00:00+00:00    3.5
)";

        UNIT_ASSERT(cand->isCandidate && cand->comment == result1);
        cl.addHypothesis(335, HypType::Moving);

        cand = generateOneCandidateByRecentHistory(
            lastHour + 1, compareHour + 1, current, recent, cl);
        /* 2018-10-20T23:00:00+00:00 Sat */
        UNIT_ASSERT(cand);

        std::string result2 = R"(By recent:
current traffic
2018-10-20 21:00:00+00:00    17
2018-10-20 22:00:00+00:00    13
2018-10-20 23:00:00+00:00    16
compared to
2018-10-19 21:00:00+00:00    2.5
2018-10-19 22:00:00+00:00    3.5
2018-10-19 23:00:00+00:00    1.5
)";

        UNIT_ASSERT(cand->isCandidate && cand->comment == result2);

        cand = generateOneCandidateByRecentHistory(
            lastHour + 2, compareHour + 2, current, recent, cl);
        /* 2018-10-21T00:00:00+00:00 Sat */
        UNIT_ASSERT(cand);

        std::string result3 = R"(By recent:
current traffic
2018-10-20 22:00:00+00:00    13
2018-10-20 23:00:00+00:00    16
2018-10-21 00:00:00+00:00    6
compared to
2018-10-19 22:00:00+00:00    3.5
2018-10-19 23:00:00+00:00    1.5
2018-10-20 00:00:00+00:00    0
)";

        UNIT_ASSERT(!cand->isCandidate && cand->comment == result3);
    }

    Y_UNIT_TEST(testRecentHistory_3) {
        Globals::get().initHypgenForTest("test_config.conf", std::make_shared<FakeGeobase>(0));

        EdgeIds edgeIds{33,55};

        time_t nowTs = timeFromString("2018-10-22 10:00:00+00:00"); /* Mon */
        uint32_t lastHour = nowTs / PART_SIZE_SECONDS;
        auto now = chrono::TimePoint::clock::from_time_t(nowTs);

        time_t compareHour = findHourToCompareWith(lastHour, 10800);
        UNIT_ASSERT(compareHour == hourFromString("2018-10-19 10:00:00+00:00")); /* Fri */

        TimedTrafficIndex current;
        current[33][lastHour - 2] = 22;
        current[55][lastHour - 2] = 24;

        current[33][lastHour - 1] = 18;
        current[55][lastHour - 1] = 16;

        current[33][lastHour] = 12;
        current[55][lastHour] = 14;

        TimedTrafficIndex recent;

        Closure cl("infopoint2",
                   timeFromString("2018-10-19 01:00:00+00:00") /* Fri */,
                   timeFromString("2018-10-22 14:00:00+00:00") /* Mon */,
                   geolib3::Polyline2(),
                   geolib3::Point2(30.376222,59.86370),
                   "graph_version",
                   now,
                   now,
                   edgeIds
            );

        auto cand = generateOneCandidateByRecentHistory(
            lastHour, compareHour, current, recent, cl);
        UNIT_ASSERT(cand);

        std::string result1 = R"(By recent:
current traffic
2018-10-22 08:00:00+00:00    23
2018-10-22 09:00:00+00:00    17
2018-10-22 10:00:00+00:00    13
compared to
2018-10-19 08:00:00+00:00    0
2018-10-19 09:00:00+00:00    0
2018-10-19 10:00:00+00:00    0
)";

        UNIT_ASSERT(cand->isCandidate && cand->comment == result1);
    }

} // test suite end

} // maps::wiki::gpsrealtime_hypgen
