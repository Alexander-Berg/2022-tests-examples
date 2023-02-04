#include <yandex/maps/carparks/geometry/carpark_index.h>

#include <library/cpp/testing/unittest/env.h>
#include <boost/test/unit_test.hpp>
#include <util/system/shellcommand.h>

using namespace maps::carparks::geometry;
using namespace maps::geolib3;

const maps::geolib3::Point2 BASE_POINT(37.59251922369003, 55.7352565811183);
const maps::geolib3::Point2 POINT_NEAR_POINT_CARPARK(37.59377, 55.73551);
const maps::geolib3::Point2 POINT_INSIDE_POLYGON(37.59342849, 55.7349464);
const maps::geolib3::Point2 POINT_NEAR_LAST_SIDE(46.9999, 55.0005);
const maps::geolib3::Point2 POINT_NEAR_LINE2(46.9999, 65.0005);

class CarparkIndexFixture {
public:
    CarparkIndexFixture() {
        prepareData();
        index.reset(new CarparkIndex("data"));
    }

    std::unique_ptr<CarparkIndex> index;

private:
    void prepareData()
    {
        TShellCommand(
                BinaryPath(
                        "maps/carparks/libs/geometry/build_test_data/build_test_data"
                ),
                TList<TString>{"data"}
        ).Run().Wait();
    }
};

BOOST_FIXTURE_TEST_SUITE(CarparkIndexSuite, CarparkIndexFixture)

BOOST_AUTO_TEST_CASE(test_count)
{
    BOOST_REQUIRE_EQUAL(index->findNearest(BASE_POINT, 3, 100).size(), 3);
    BOOST_REQUIRE_EQUAL(index->findNearest(BASE_POINT, 4, 100).size(), 4);
    BOOST_REQUIRE_EQUAL(index->findNearest(BASE_POINT, 7, 200).size(), 7);
    BOOST_REQUIRE_EQUAL(index->findNearest(BASE_POINT, 8, 200).size(), 7);
}

BOOST_AUTO_TEST_CASE(test_distance_sort)
{
    auto relations = index->findNearest(BASE_POINT, 8, 200);
    for (size_t i = 0; i + 1 < relations.size(); i++) {
        BOOST_REQUIRE_LE(relations[i].closestPoint.distance,
                         relations[i + 1].closestPoint.distance);
    }
}

BOOST_AUTO_TEST_CASE(test_near_point)
{
    auto relations = index->findNearest(POINT_NEAR_POINT_CARPARK, 4, 20);
    BOOST_REQUIRE_EQUAL(relations.size(), 1);
    auto closest = relations[0];
    BOOST_REQUIRE_EQUAL(closest.id, 1);
    BOOST_REQUIRE_LT(closest.closestPoint.distance, 2.0);
}

BOOST_AUTO_TEST_CASE(test_near_polyline)
{
    auto relations = index->findNearest(BASE_POINT, 4, 20);
    BOOST_REQUIRE_EQUAL(relations.size(), 1);
    auto closest = relations[0];
    BOOST_REQUIRE_EQUAL(closest.id, 4);
    BOOST_REQUIRE_CLOSE(closest.closestPoint.distance, 7.117, 1E-3);
}

BOOST_AUTO_TEST_CASE(test_inside_polygon)
{
    auto relations = index->findNearest(POINT_INSIDE_POLYGON, 10, 1);
    BOOST_REQUIRE_EQUAL(relations.size(), 1);
    auto closest = relations[0];
    BOOST_REQUIRE_EQUAL(closest.id, 6);
    BOOST_REQUIRE_EQUAL(closest.closestPoint.distance, 0.);
}

BOOST_AUTO_TEST_CASE(test_near_polygon_last_side)
{
    auto relations = index->findNearest(POINT_NEAR_LAST_SIDE, 1, 1000);
    BOOST_REQUIRE_EQUAL(relations.size(), 1);
    auto closest = relations[0];
    BOOST_REQUIRE_EQUAL(closest.id, 8);
    BOOST_REQUIRE_LT(closest.closestPoint.distance, 10.);
}

BOOST_AUTO_TEST_CASE(test_near_polyline_segment_from_last_to_first_point)
{
    auto relations = index->findNearest(POINT_NEAR_LINE2, 1, 1000);
    BOOST_REQUIRE_EQUAL(relations.size(), 1);
    auto closest = relations[0];
    BOOST_REQUIRE_EQUAL(closest.id, 5);
    BOOST_REQUIRE_GT(closest.closestPoint.distance, 30.);
}

BOOST_AUTO_TEST_CASE(test_capacity)
{
    auto relations = index->findNearest({37.590974271297455, 55.73537074681084}, 10, 1);
    auto capacity = relations[0].capacity;
    BOOST_REQUIRE(capacity > 10);
    BOOST_REQUIRE(capacity < 50);
}

BOOST_AUTO_TEST_SUITE_END()
