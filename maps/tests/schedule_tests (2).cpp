#include <library/cpp/testing/unittest/registar.h>

#include <maps/wikimap/jams_arm2/libs/common/include/closure_template.h>
#include <maps/wikimap/jams_arm2/libs/db/include/helper.h>

#include <iostream>

namespace maps {
namespace wiki {
namespace jams_arm2 {
namespace db {

class GeobaseRegionStub: public IGeobase
{
    int getRegionId(geolib3::Point2 /*geoPosition*/) const override
    {
        return 213; // Moscow
    }

    std::string getRegionName(int regionId) const override
    {
        Y_UNUSED(regionId);
        throw maps::RuntimeError("Not implemented");
    }

};

class FakeGeobase : public GeobaseRegionStub {
public:
    std::chrono::seconds getTimezoneShift(int /*regionId*/, chrono::TimePoint /*when*/) const override
    {
        return std::chrono::seconds(0);
    }
};

class GeobaseTest1 : public GeobaseRegionStub {
    const chrono::TimePoint summer = chrono::parseSqlDateTime("2018-05-14 03:00:00+00:00");

public:
    std::chrono::seconds getTimezoneShift(int /*regionId*/, chrono::TimePoint when) const override
    {
        if (when < summer) {
            return std::chrono::seconds(3600);
        }
        return std::chrono::seconds(7200);
    }
};

class GeobaseTest2 : public GeobaseRegionStub {
    const chrono::TimePoint summer = chrono::parseSqlDateTime("2018-05-14 03:00:00+00:00");

public:
    std::chrono::seconds getTimezoneShift(int /*regionId*/, chrono::TimePoint when) const override
    {
        if (when > summer) {
            return std::chrono::seconds(3600);
        }
        return std::chrono::seconds(7200);
    }
};

class GeobaseTest3 : public GeobaseRegionStub {
    const chrono::TimePoint summer = chrono::parseSqlDateTime("2018-05-14 03:00:00+00:00");

public:
    std::chrono::seconds getTimezoneShift(int /*regionId*/, chrono::TimePoint when) const override
    {
        if (when > summer) {
            return std::chrono::seconds(-3600);
        }
        return std::chrono::seconds(-7200);
    }
};

static const FakeGeobase fakeUser;
static const GeobaseTest1 geobase1;
static const GeobaseTest2 geobase2;
static const GeobaseTest3 geobase3;

Y_UNIT_TEST_SUITE(schedule) {
    Y_UNIT_TEST(test1) {
        Schedule sch = makeSchedule(R"([
            {
             "since":"2018-05-07 20:30:00+00:00",
             "till":"2018-05-08 01:00:00+00:00",
             "regularity":
                 {
                  "until":"2018-10-01 20:59:59+00:00",
                  "byday":["mon","wed"]
                 }
            },
            {
             "since":"2018-06-14 03:30:44+00:00",
             "till":"2018-06-21 01:00:00+00:00"
            }])");
        auto res1  = findNextBySchedule(sch, fakeUser, geolib3::Point2(), chrono::parseSqlDateTime("2018-05-14 21:30:00+00:00"));
        UNIT_ASSERT(res1);
        UNIT_ASSERT(res1->begin == chrono::parseSqlDateTime("2018-05-14 20:30:00+00:00"));
        UNIT_ASSERT(res1->end == chrono::parseSqlDateTime("2018-05-15 01:00:00+00:00"));

        auto res2  = findNextBySchedule(sch, fakeUser, geolib3::Point2(), chrono::parseSqlDateTime("2018-05-04 21:30:00+00:00"));
        UNIT_ASSERT(res2);
        UNIT_ASSERT(res2->begin == chrono::parseSqlDateTime("2018-05-07 20:30:00+00:00"));
        UNIT_ASSERT(res2->end == chrono::parseSqlDateTime("2018-05-08 01:00:00+00:00"));

        auto res3  = findNextBySchedule(sch, fakeUser, geolib3::Point2(), chrono::parseSqlDateTime("2018-06-12 12:30:00+00:00"));
        UNIT_ASSERT(res3);
        UNIT_ASSERT(res3->begin == chrono::parseSqlDateTime("2018-06-13 20:30:00+00:00"));
        UNIT_ASSERT(res3->end == chrono::parseSqlDateTime("2018-06-14 01:00:00+00:00"));

        auto res4  = findNextBySchedule(sch, fakeUser, geolib3::Point2(), chrono::parseSqlDateTime("2018-06-14 01:01:00+00:00"));
        UNIT_ASSERT(res4);
        UNIT_ASSERT(res4->begin == chrono::parseSqlDateTime("2018-06-14 03:30:44+00:00"));
        UNIT_ASSERT(res4->end == chrono::parseSqlDateTime("2018-06-21 01:00:00+00:00"));

        auto res5  = findNextBySchedule(sch, fakeUser, geolib3::Point2(), chrono::parseSqlDateTime("2018-10-01 22:59:59+00:00"));
        UNIT_ASSERT(res5);
        UNIT_ASSERT(res5->begin == chrono::parseSqlDateTime("2018-10-01 20:30:00+00:00"));
        UNIT_ASSERT(res5->end == chrono::parseSqlDateTime("2018-10-02 01:00:00+00:00"));

        auto res6  = findNextBySchedule(sch, fakeUser, geolib3::Point2(), chrono::parseSqlDateTime("2018-10-02 21:00:59+00:00"));
        UNIT_ASSERT(!res6);
    }
    Y_UNIT_TEST(test2) {
        Schedule sch = makeSchedule(R"([
            {
             "since":"2018-04-05 21:00:00+00:00",
             "till":"2018-04-07 20:59:00+00:00",
             "regularity":
                 {
                  "until":"2018-04-06 20:59:59+00:00",
                  "byday":["mon","tue","wed","thu","fri","sat","sun"]
                 }
            }])");
        auto res1  = findNextBySchedule(sch, fakeUser, geolib3::Point2(), chrono::parseSqlDateTime("2018-04-06 18:30:00+00:00"));
        UNIT_ASSERT(res1);
        UNIT_ASSERT(res1->begin == chrono::parseSqlDateTime("2018-04-05 21:00:00+00:00"));
        UNIT_ASSERT(res1->end == chrono::parseSqlDateTime("2018-04-07 20:59:00+00:00"));

        auto res2  = findNextBySchedule(sch, fakeUser, geolib3::Point2(), chrono::parseSqlDateTime("2018-04-07 11:30:00+00:00"));
        UNIT_ASSERT(res2);
        UNIT_ASSERT(res2->begin == chrono::parseSqlDateTime("2018-04-05 21:00:00+00:00"));
        UNIT_ASSERT(res2->end == chrono::parseSqlDateTime("2018-04-07 20:59:00+00:00"));

        auto res3  = findNextBySchedule(sch, fakeUser, geolib3::Point2(), chrono::parseSqlDateTime("2018-04-07 21:30:00+00:00"));
        UNIT_ASSERT(!res3);
    }
    Y_UNIT_TEST(test3) {
        Schedule sch = makeSchedule(R"([
            {
             "since":"2018-04-05 21:00:00+00:00",
             "till":"2018-04-06 21:09:00+00:00",
             "regularity":
                 {
                  "until":"2018-04-17 20:59:59+00:00",
                  "byday":["mon","tue","wed","thu","fri","sat","sun"]
                 }
            }])");
        auto res1  = findNextBySchedule(sch, fakeUser, geolib3::Point2(), chrono::parseSqlDateTime("2018-04-06 18:30:00+00:00"));
        UNIT_ASSERT(res1);
        UNIT_ASSERT(res1->begin == chrono::parseSqlDateTime("2018-04-05 21:00:00+00:00"));
        UNIT_ASSERT(res1->end == chrono::parseSqlDateTime("2018-04-12 21:09:00+00:00"));

        auto res2  = findNextBySchedule(sch, fakeUser, geolib3::Point2(), chrono::parseSqlDateTime("2018-04-15 18:30:00+00:00"));
        UNIT_ASSERT(res2);
        UNIT_ASSERT(res2->begin == chrono::parseSqlDateTime("2018-04-14 21:00:00+00:00"));
        UNIT_ASSERT(res2->end == chrono::parseSqlDateTime("2018-04-17 21:09:00+00:00"));
    }
    Y_UNIT_TEST(test4) {
        Schedule sch = makeSchedule(R"([
            {
             "since":"2018-04-05 21:00:00+00:00",
             "till":"2018-04-06 20:59:59+00:00",
             "regularity":
                 {
                  "until":"2018-04-17 20:59:59+00:00",
                  "byday":["mon","tue","wed","thu","fri","sat","sun"]
                 }
            }])");
        auto res1  = findNextBySchedule(sch, fakeUser, geolib3::Point2(), chrono::parseSqlDateTime("2018-04-06 18:30:00+00:00"));
        UNIT_ASSERT(res1);
        UNIT_ASSERT(res1->begin == chrono::parseSqlDateTime("2018-04-05 21:00:00+00:00"));
        UNIT_ASSERT(res1->end == chrono::parseSqlDateTime("2018-04-06 20:59:59+00:00"));

        auto res2  = findNextBySchedule(sch, fakeUser, geolib3::Point2(), chrono::parseSqlDateTime("2018-04-18 18:30:00+00:00"));
        UNIT_ASSERT(!res2);
    }
    Y_UNIT_TEST(test5) {
        Schedule sch = makeSchedule(R"([
            {
             "since":"2018-05-14 09:30:00+00:00",
             "till":"2018-05-14 11:00:00+00:00",
             "regularity":
                 {
                  "until":"2018-10-01 20:59:59+00:00",
                  "byday":["mon","wed"]
                 }
            },
            {
             "since":"2018-05-13 10:30:00+00:00",
             "till":"2018-05-14 10:30:00+00:00",
             "regularity":
                 {
                  "until":"2018-10-01 20:59:59+00:00",
                  "byday":["sun","fri"]
                 }
            }])");
        auto res1  = findNextBySchedule(sch, fakeUser, geolib3::Point2(), chrono::parseSqlDateTime("2018-05-13 18:30:00+00:00"));
        UNIT_ASSERT(res1);
        UNIT_ASSERT(res1->begin == chrono::parseSqlDateTime("2018-05-13 10:30:00+00:00"));
        UNIT_ASSERT(res1->end == chrono::parseSqlDateTime("2018-05-14 11:00:00+00:00"));

        auto res2  = findNextBySchedule(sch, fakeUser, geolib3::Point2(), chrono::parseSqlDateTime("2018-05-12 18:30:00+00:00"));
        UNIT_ASSERT(res2);
        UNIT_ASSERT(res2->begin == chrono::parseSqlDateTime("2018-05-13 10:30:00+00:00"));
        UNIT_ASSERT(res2->end == chrono::parseSqlDateTime("2018-05-14 11:00:00+00:00"));

        auto res3  = findNextBySchedule(sch, fakeUser, geolib3::Point2(), chrono::parseSqlDateTime("2018-05-25 18:30:00+00:00"));
        UNIT_ASSERT(res3);
        UNIT_ASSERT(res3->begin == chrono::parseSqlDateTime("2018-05-25 10:30:00+00:00"));
        UNIT_ASSERT(res3->end == chrono::parseSqlDateTime("2018-05-26 10:30:00+00:00"));

        auto res4  = findNextBySchedule(sch, geobase1, geolib3::Point2(), chrono::parseSqlDateTime("2018-05-13 18:30:00+00:00"));
        UNIT_ASSERT(res4);
        UNIT_ASSERT(res4->begin == chrono::parseSqlDateTime("2018-05-13 9:30:00+00:00"));
        UNIT_ASSERT(res4->end == chrono::parseSqlDateTime("2018-05-14 9:00:00+00:00"));

        auto res5  = findNextBySchedule(sch, geobase2, geolib3::Point2(), chrono::parseSqlDateTime("2018-05-13 18:30:00+00:00"));
        UNIT_ASSERT(res5);
        UNIT_ASSERT(res5->begin == chrono::parseSqlDateTime("2018-05-13 8:30:00+00:00"));
        UNIT_ASSERT(res5->end == chrono::parseSqlDateTime("2018-05-14 10:00:00+00:00"));

        auto res6  = findNextBySchedule(sch, geobase3, geolib3::Point2(), chrono::parseSqlDateTime("2018-05-13 18:30:00+00:00"));
        UNIT_ASSERT(res6);
        UNIT_ASSERT(res6->begin == chrono::parseSqlDateTime("2018-05-13 12:30:00+00:00"));
        UNIT_ASSERT(res6->end == chrono::parseSqlDateTime("2018-05-14 12:00:00+00:00"));
    }
    Y_UNIT_TEST(test6) {
        Schedule sch = makeSchedule(R"([
            {
             "since":"2018-05-14 01:30:00+00:00",
             "till":"2018-05-14 04:00:00+00:00",
             "regularity":
                 {
                  "until":"2018-10-01 20:59:59+00:00",
                  "byday":["mon","wed"]
                 }
            }])");
        auto res1  = findNextBySchedule(sch, geobase2, geolib3::Point2(), chrono::parseSqlDateTime("2018-05-13 18:30:00+00:00"));
        UNIT_ASSERT(res1);
        UNIT_ASSERT(res1->begin == chrono::parseSqlDateTime("2018-05-13 23:30:00+00:00"));
        UNIT_ASSERT(res1->end == chrono::parseSqlDateTime("2018-05-14 02:00:00+00:00"));
    }
    Y_UNIT_TEST(test7) {
        Schedule sch = makeSchedule(R"([
            {
             "since":"2018-05-14 03:31:00+00:00",
             "till":"2018-05-14 04:30:00+00:00",
             "regularity":
                 {
                  "until":"2018-10-01 20:59:59+00:00",
                  "byday":["mon","wed"]
                 }
            }])");
        auto res1  = findNextBySchedule(sch, geobase1, geolib3::Point2(), chrono::parseSqlDateTime("2018-05-13 18:30:00+00:00"));
        UNIT_ASSERT(res1);
        UNIT_ASSERT(res1->begin == chrono::parseSqlDateTime("2018-05-16 01:31:00+00:00"));
        UNIT_ASSERT(res1->end == chrono::parseSqlDateTime("2018-05-16 02:30:00+00:00"));
//        UNIT_ASSERT(res1->begin == chrono::parseSqlDateTime("2018-05-14 02:31:00+00:00"));
//        UNIT_ASSERT(res1->end == chrono::parseSqlDateTime("2018-05-14 02:30:00+00:00"));  // error!!! without check start<end interval would be like that

        auto res2  = findNextBySchedule(sch, geobase1, geolib3::Point2(), chrono::parseSqlDateTime("2018-05-14 03:00:00+00:00"));
        UNIT_ASSERT(res2);
        UNIT_ASSERT(res2->begin == chrono::parseSqlDateTime("2018-05-16 01:31:00+00:00"));
        UNIT_ASSERT(res2->end == chrono::parseSqlDateTime("2018-05-16 02:30:00+00:00"));

        auto res3  = findNextBySchedule(sch, geobase2, geolib3::Point2(), chrono::parseSqlDateTime("2018-05-14 03:00:00+00:00"));
        UNIT_ASSERT(res3);
        UNIT_ASSERT(res3->begin == chrono::parseSqlDateTime("2018-05-16 02:31:00+00:00"));
        UNIT_ASSERT(res3->end == chrono::parseSqlDateTime("2018-05-16 03:30:00+00:00"));
    }
    Y_UNIT_TEST(test8) {
        Schedule sch = makeSchedule(R"([
            {
             "since":"2018-05-14 00:31:00+00:00",
             "till":"2018-05-14 01:30:00+00:00",
             "regularity":
                 {
                  "until":"2018-10-01 20:59:59+00:00",
                  "byday":["mon","wed"]
                 }
            }])");
        auto res1  = findNextBySchedule(sch, geobase3, geolib3::Point2(), chrono::parseSqlDateTime("2018-05-13 03:00:00+00:00"));
        UNIT_ASSERT(res1);
        UNIT_ASSERT(res1->begin == chrono::parseSqlDateTime("2018-05-16 01:31:00+00:00"));
        UNIT_ASSERT(res1->end == chrono::parseSqlDateTime("2018-05-16 02:30:00+00:00"));
//        UNIT_ASSERT(res1->begin == chrono::parseSqlDateTime("2018-05-14 02:31:00+00:00"));
//        UNIT_ASSERT(res1->end == chrono::parseSqlDateTime("2018-05-14 02:30:00+00:00"));  // error!!! without check start<end interval would be like that
    }

} // test suite end

} // namespace db
} // namespace jams_arm2
} // namespace wiki
} // namespace maps
