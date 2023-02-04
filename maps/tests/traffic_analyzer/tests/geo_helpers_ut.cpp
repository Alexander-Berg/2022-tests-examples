#include <maps/wikimap/mapspro/tools/traffic_analyzer/lib/geo_helpers.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <maps/libs/common/include/exception.h>

namespace mwt = maps::wiki::traffic_analyzer;
using namespace maps::geolib3;

TEST(geo_helpers, polyline_mid_point)
{
    {
        Polyline2 poly;
        ASSERT_THROW(mwt::polylineMidPoint(poly), maps::Exception);
    }
    {
        Point2 point(1, 1);
        Polyline2 poly({point});
        ASSERT_TRUE(mwt::polylineMidPoint(poly) == point);
    }
    {
        Point2 p1(1, 1), p2(3, 3);
        Polyline2 poly(std::vector<Point2>{p1, p2});
        ASSERT_TRUE(mwt::polylineMidPoint(poly) == Point2(2, 2));
    }
    {
        Point2 p1(0, 0), p2(1, 1), p3(3, 1);
        Polyline2 poly({p1, p2, p3});
        ASSERT_TRUE(mwt::polylineMidPoint(poly) == p2);
    }
    {
        Point2 p1(0, 0), p2(1, 1), p3(2, 2), p4(5, 10);
        Polyline2 poly({p1, p2, p3, p4});
        ASSERT_TRUE(mwt::polylineMidPoint(poly) == Point2(1.5, 1.5));
    }
}

TEST(geo_helpers, polyline_global_direction)
{
    {
        Polyline2 poly;
        ASSERT_THROW(mwt::polylineGlobalDirection(poly), maps::Exception);
    }
    {
        Polyline2 poly({Point2(1, 2)});
        ASSERT_THROW(mwt::polylineGlobalDirection(poly), maps::Exception);
    }
    {
        Point2 p1(1, 1), p2(2, 5);
        Polyline2 poly(std::vector<Point2>{p1, p2});
        auto dir = mwt::polylineGlobalDirection(poly);
        ASSERT_TRUE(dir.x() == p2.x() - p1.x());
        ASSERT_TRUE(dir.y() == p2.y() - p1.y());
    }
    {
        Point2 p1(1, 1), p2(2, 5), p3(3, 4);
        Polyline2 poly(std::vector<Point2>{p1, p2, p3});
        auto dir = mwt::polylineGlobalDirection(poly);
        ASSERT_TRUE(dir.x() == p3.x() - p1.x());
        ASSERT_TRUE(dir.y() == p3.y() - p1.y());
    }
}
