#include <yandex/maps/navikit/lanes_utils.h>

#include <yandex/maps/mapkit/directions/driving/lane.h>
#include <yandex/maps/mapkit/directions/driving/unit_test/route.h>
#include <yandex/maps/mapkit/geometry/geometry.h>

#include <boost/test/unit_test.hpp>

namespace yandex::maps::navikit {

using namespace mapkit::directions::driving;
using namespace mapkit::geometry;

bool performResourceSizesTest(
    const std::vector<std::map<LaneDirection, ResourceSize>>& answer)
{
    std::vector<Lane> lanes;
    for (const auto& sizes : answer) {
        std::vector<LaneDirection> directions;
        std::transform(sizes.begin(), sizes.end(), std::back_inserter(directions),
            [](const std::pair<LaneDirection, ResourceSize>& v) {
                return v.first;
            });
        lanes.emplace_back(LaneKind::PlainLane, std::move(directions), boost::none);
    }

    LaneSign laneSign({0, 0}, lanes);

    return answer == arrowsSizes(&laneSign);
}

BOOST_AUTO_TEST_CASE(SingleLaneTests)
{
    BOOST_CHECK(performResourceSizesTest({{
        { LaneDirection::StraightAhead, ResourceSize::Big }
    }}));

    BOOST_CHECK(performResourceSizesTest({{
        { LaneDirection::StraightAhead, ResourceSize::Big },
        { LaneDirection::Left90, ResourceSize::Small }
    }}));

    BOOST_CHECK(performResourceSizesTest({{
        { LaneDirection::Left90, ResourceSize::Big }
    }}));

    BOOST_CHECK(performResourceSizesTest({{
        { LaneDirection::StraightAhead, ResourceSize::Big },
        { LaneDirection::Left45, ResourceSize::Big }
    }}));

    BOOST_CHECK(performResourceSizesTest({{
        { LaneDirection::StraightAhead, ResourceSize::Big },
        { LaneDirection::Left45, ResourceSize::Big }
    }}));

    BOOST_CHECK(performResourceSizesTest({{
        { LaneDirection::LeftFromRight, ResourceSize::Small },
        { LaneDirection::Right90, ResourceSize::Big }
    }}));

    BOOST_CHECK(performResourceSizesTest({{
        { LaneDirection::Left135, ResourceSize::Small },
        { LaneDirection::Left90, ResourceSize::Small },
        { LaneDirection::Left45, ResourceSize::Big },
        { LaneDirection::StraightAhead, ResourceSize::Big },
        { LaneDirection::Right45, ResourceSize::Big },
        { LaneDirection::Right90, ResourceSize::Small },
        { LaneDirection::Right135, ResourceSize::Small }
    }}));

    BOOST_CHECK(performResourceSizesTest({{
        { LaneDirection::StraightAhead, ResourceSize::Big },
        { LaneDirection::LeftFromRight, ResourceSize::Small }
    }}));

    BOOST_CHECK(performResourceSizesTest({{
        { LaneDirection::LeftFromRight, ResourceSize::Big }
    }}));

    BOOST_CHECK(performResourceSizesTest({{
        { LaneDirection::StraightAhead, ResourceSize::Big },
        { LaneDirection::Left180, ResourceSize::Small }
    }}));

    BOOST_CHECK(performResourceSizesTest({{
        { LaneDirection::StraightAhead, ResourceSize::Big },
        { LaneDirection::Right45, ResourceSize::Big },
        { LaneDirection::Left90, ResourceSize::Small }
    }}));

    BOOST_CHECK(performResourceSizesTest({{
        { LaneDirection::StraightAhead, ResourceSize::Big },
        { LaneDirection::Right45, ResourceSize::Big },
        { LaneDirection::Left180, ResourceSize::Small }
    }}));
}

BOOST_AUTO_TEST_CASE(MultipleLanesTest)
{
    BOOST_CHECK(performResourceSizesTest({
        {
            { LaneDirection::StraightAhead, ResourceSize::Big },
            { LaneDirection::Left90, ResourceSize::Small }
        },
        {
            { LaneDirection::Left90, ResourceSize::Small }
        },
        {
            { LaneDirection::Right90, ResourceSize::Small }
        }
    }));

    BOOST_CHECK(performResourceSizesTest({
        {
            { LaneDirection::StraightAhead, ResourceSize::Big },
            { LaneDirection::LeftFromRight, ResourceSize::Small }
        },
        {
            { LaneDirection::LeftFromRight, ResourceSize::Small }
        },
        {
            { LaneDirection::Right90, ResourceSize::Big }
        }
    }));
}

struct LaneSignTest {
    PolylinePosition userPosition;
    LaneSign laneSign;
    BalloonPlacement placement;
};

BOOST_AUTO_TEST_CASE(LaneSignPlacementTest)
{
    Polyline polyline {{
        {0, 0},
        {0, 1},
        {1, 1},
        {2, 1},
        {3, 1}
    }};

    Lane first(LaneKind::PlainLane, {LaneDirection::Right90}, LaneDirection::Right90);
    Lane second(LaneKind::PlainLane, {LaneDirection::Left90}, LaneDirection::Left90);

    std::vector<LaneSignTest> TESTS {
        { {0, 0.5}, {{2, 0.5}, {first}}, BalloonPlacement::Above },
        { {0, 0.5}, {{3, 0.5}, {second}}, BalloonPlacement::Above },
        { {1, 0.5}, {{2, 0.5}, {first}}, BalloonPlacement::Left },
        { {1, 0.5}, {{3, 0.5}, {second}}, BalloonPlacement::Right }
    };

    unit_test::MockRoute route(polyline);
    for (size_t i = 0; i < TESTS.size(); ++i) {
        const auto& test = TESTS[i];
        route.setPosition(test.userPosition);
        BOOST_CHECK_MESSAGE(test.placement == getLaneSignPlacement(&route, &test.laneSign),
            "Failed on " << i);
    }
}

} // namespace yandex
