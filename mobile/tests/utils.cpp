#include <boost/test/auto_unit_test.hpp>
#include "fixture.h"

#include <yandex/maps/mapkit/geometry/geo/geo.h>

#include <yandex/maps/navikit/routing/routing_utils.h>

using namespace yandex::maps::navikit::routing;

BOOST_FIXTURE_TEST_SUITE(RoutingUtils, Fixture)

BOOST_AUTO_TEST_CASE(progressTest)
{
    auto route = makeRoute(
        {
            {0.0, 0.0}, {1.0, 1.0}, {2.0, 1.0}
        },

        {
            makeSection("", {{0, 0.0}, {1, 0.3}}),
            makeSection("", {{1, 0.3}, {1, 1.0}})
        },

        {},

        {}

    );

    route->setPosition({0, 0.5});

    double tripleProgress = progress<double>(
        *route,
        [] (const mapkit::geometry::Point& p1, const mapkit::geometry::Point& p2) {
            return geo::distance(p1, p2) * 3;
        }
    );

    double expect =
        geo::length({
            {0.0, 0.0}, {1.0, 1.0}
        }) * 3.0 / 2.;

    BOOST_CHECK_CLOSE(expect, tripleProgress, 1E-8);
}

BOOST_AUTO_TEST_CASE(onNextSectionTest)
{
    auto route = makeRoute(
        {
            {0.0, 0.0}, {1.0, 1.0}, {2.0, 1.0}
        },

        {
            makeSection("", {{0, 0.0}, {1, 0.3}}),
            makeSection("", {{1, 0.3}, {1, 1.0}})
        },

        {},

        {}

    );

    BOOST_CHECK(areDifferentSections(*route, {0, 0.5}, {1, 0.4}));
    BOOST_CHECK(!areDifferentSections(*route, {0, 0.5}, {1, 0.2}));
}

BOOST_AUTO_TEST_CASE(positionTest)
{
    Polyline polyline {
        std::vector<geometry::Point> {
            {0.0, 0.0}, {1.0, 1.0}, {2.0, 1.0}
        }
    };

    auto result = position(
        polyline,
        geo::distance(
            {0.0, 0.0},
            {1.0, 1.0}) +
        geo::distance(
            {1.0, 1.0},
            {1.5, 1.0})
    );

    auto expect = PolylinePosition{1, 0.5};

    BOOST_CHECK_EQUAL(result, expect);
}

BOOST_AUTO_TEST_SUITE_END()
