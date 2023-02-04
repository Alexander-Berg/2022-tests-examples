#include "common.h"

#include <maps/bicycle/router/lib/bicycle_router.h>
#include <maps/bicycle/router/lib/uri.h>
#include <maps/masstransit/libs/uri/include/path_uri.h>
#include <maps/bicycle/router/tests/lib/shared.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace maps::bicycle::tests {

using namespace maps::geolib3;
using namespace maps::masstransit::routing;
namespace uri = maps::bicycle::uri;
namespace path_uri = maps::masstransit::uri::path;

Y_UNIT_TEST_SUITE(Router)
{

Y_UNIT_TEST(Router)
{
    BicycleRouter router{Shared::config()};
    std::vector<Path> result1 = router.findPaths({
        RequestPoint{{37.582444, 55.731834}},
        RequestPoint{{37.553598, 55.724971}}
    });
    ASSERT_EQ(result1.size(), 3u);
    const Path& path1 = result1[0];
    EXPECT_EQ(path1.hasAutoRoad(), true);
    EXPECT_EQ(path1.hasAccessPass(), false);

    std::vector<Path> result2 = router.findPaths({
        RequestPoint{{37.598048, 55.726156}},
        RequestPoint{{37.601958, 55.731015}}
    });
    ASSERT_EQ(result2.size(), 3u);
    const Path& path2 = result2[0];
    EXPECT_EQ(path2.hasAutoRoad(), false);
    EXPECT_EQ(path2.hasAccessPass(), false);

    std::vector<Path> result3 = router.findPaths({
        RequestPoint{{37.578670, 55.760998}},
        RequestPoint{{37.578664, 55.761789}}
    });
    ASSERT_EQ(result3.size(), 1u);
    const Path& path3 = result3[0];
    EXPECT_EQ(path3.hasAutoRoad(), false);
    EXPECT_EQ(path3.hasAccessPass(), true);
}

Y_UNIT_TEST(RequestPoints)
{
    RequestPoint exitOkhotny{{37.617553, 55.756112}};
    RequestPoint exitKitay{{37.630000, 55.756278}};
    RequestPoint gum{
        {37.621680, 55.754473},
        {
            {37.619586, 55.755322},
            {37.620037, 55.755589},
            {37.621142, 55.755383},
            {37.621915, 55.754996},
            {37.622719, 55.754600},
            {37.623250, 55.754055},
            {37.622623, 55.753774}
        }
    };

    BicycleRouter router{Shared::config()};

    auto shortestPath = [] (const std::vector<Path>& paths) -> const Path& {
        auto it = std::min_element(
            paths.begin(),
            paths.end(),
            [] (const Path& lhs, const Path& rhs) {
                return lhs.weight().length() < rhs.weight().length();
            });
        ASSERT(it != paths.end());
        return *it;
    };

    std::vector<Path> result1 = router.findPaths({exitOkhotny, gum});
    ASSERT_EQ(result1.size(), 2u);
    const Path& path1 = shortestPath(result1);
    EXPECT_EQ(path1.from(), WayPoint(exitOkhotny.wayPoint()));
    EXPECT_EQ(path1.to(), WayPoint(gum.wayPoint(), gum.arrivalPoints().at(0)));
    EXPECT_NEAR(219.7, path1.weight().length(), DISTANCE_EPSILON);
    EXPECT_NEAR(131.4, path1.weight().time(), TIME_EPSILON);

    std::vector<Path> result1rev = router.findPaths({
        gum,
        exitOkhotny
    });
    ASSERT_EQ(result1rev.size(), 1u);
    const Path& path1rev = shortestPath(result1rev);
    EXPECT_EQ(path1rev.from(), WayPoint(gum.wayPoint(), gum.arrivalPoints().at(0)));
    EXPECT_EQ(path1rev.to(), WayPoint(exitOkhotny.wayPoint()));
    EXPECT_NEAR(381.5, path1rev.weight().length(), DISTANCE_EPSILON);
    EXPECT_NEAR(212.3, path1rev.weight().time(), TIME_EPSILON);

    std::vector<Path> result2 = router.findPaths({exitKitay, gum});
    ASSERT_EQ(result2.size(), 2u);
    const Path& path20 = result2.at(0);
    EXPECT_EQ(path20.from(), WayPoint(exitKitay.wayPoint()));
    EXPECT_EQ(path20.to(), WayPoint(gum.wayPoint(), gum.arrivalPoints().at(4)));
    EXPECT_NEAR(583.9, path20.weight().length(), DISTANCE_EPSILON);
    EXPECT_NEAR(205.3, path20.weight().time(), TIME_EPSILON);
    const Path& path21 = result2.at(1);
    EXPECT_EQ(path21.from(), WayPoint(exitKitay.wayPoint()));
    EXPECT_EQ(path21.to(), WayPoint(gum.wayPoint(), gum.arrivalPoints().at(4)));
    EXPECT_NEAR(639.2, path21.weight().length(), DISTANCE_EPSILON);
    EXPECT_NEAR(253.5, path21.weight().time(), TIME_EPSILON);

    std::vector<Path> result2rev = router.findPaths({gum, exitKitay});
    ASSERT_EQ(result2rev.size(), 2u);
    const Path& path20rev = result2rev.at(0);
    EXPECT_EQ(path20rev.from(), WayPoint(gum.wayPoint(), gum.arrivalPoints().at(4)));
    EXPECT_EQ(path20rev.to(), WayPoint(exitKitay.wayPoint()));
    EXPECT_NEAR(649.2, path20rev.weight().length(), DISTANCE_EPSILON);
    EXPECT_NEAR(238.0, path20rev.weight().time(), TIME_EPSILON);
}

Y_UNIT_TEST(WayPoints)
{
    BicycleRouter router{Shared::config()};
    std::vector<Path> result1 = router.findPaths({
        RequestPoint{{37.582444, 55.731834}},
        RequestPoint{{37.553598, 55.724971}},
        RequestPoint{{37.582444, 55.731834}},
    });
    ASSERT_EQ(1u, result1.size());
    const Path& path1 = result1[0];
    EXPECT_EQ(path1.hasAutoRoad(), true);
    EXPECT_EQ(path1.hasAccessPass(), false);

    ASSERT_EQ(3u, path1.wayPoints().size());
    ASSERT_EQ(path1.polyline().segmentsNumber(), 248u);
    ASSERT_EQ(2u, path1.legs().size());

    EXPECT_LEG_EQ(
        (Path::Leg{Weight{2487.2, 719.3}, 0, 125u}),
        path1.legs()[0]);
    EXPECT_LEG_EQ(
        (Path::Leg{Weight{2785.9, 843.3}, 125u, 248u}),
        path1.legs()[1]);

    EXPECT_WEIGHT_EQ((Weight{5273.1, 1563.3}), path1.weight());
}

Y_UNIT_TEST(Summary)
{
    BicycleRouter router{Shared::config()};
    std::vector<Summary> summaries1 = router.findSummaries({
        RequestPoint{{37.582444, 55.731834}},
        RequestPoint{{37.553598, 55.724971}}
    }, VehicleType::Bicycle);
    std::vector<Path> paths1 = router.findPaths({
        RequestPoint{{37.582444, 55.731834}},
        RequestPoint{{37.553598, 55.724971}}
    });
    ASSERT_EQ(paths1.size(), 3u);
    ASSERT_EQ(summaries1.size(), 1u);
    const Path& path1 = paths1.front();
    const Summary& summary1 = summaries1.front();

    EXPECT_EQ(path1.hasAutoRoad(), summary1.flags.autoRoad);
    EXPECT_EQ(path1.hasAccessPass(), summary1.flags.accessPass);

    EXPECT_WEIGHT_EQ(path1.weight(), summary1.weight);

    std::vector<Summary> summaries2 = router.findSummaries({
        RequestPoint{{37.582444, 55.731834}},
        RequestPoint{{47.553598, 15.724971}}
    }, VehicleType::Bicycle);
    ASSERT_TRUE(summaries2.empty());

    std::vector<Summary> summaries3 = router.findSummaries({
        RequestPoint{{37.582444, 55.731834}, {{37.5824, 55.7318}, {37.58245, 55.73184}}},
        RequestPoint{{37.553598, 55.724971}},
        RequestPoint{{37.582444, 55.731834}, {{37.5824, 55.7318}, {37.58245, 55.73184}}}
    }, VehicleType::Bicycle);
    ASSERT_EQ(1u, summaries3.size());
    const Summary& summary3 = summaries3.front();

    EXPECT_EQ(true, summary3.flags.autoRoad);
    EXPECT_EQ(false, summary3.flags.accessPass);

    EXPECT_WEIGHT_EQ((Weight{5269, 1562}), summary3.weight);
}

Y_UNIT_TEST(Matrix)
{
    BicycleRouter router{Shared::config()};
    const maps::geolib3::Point2 point1(37.582444, 55.731834);
    const maps::geolib3::Point2 point2(37.553598, 55.724971);
    const Weight expectedWeight{2487, 720};

    const auto matrix1x1 = router.findWeightsMatrix(
        {point1},
        {point2},
        Shared::config().maxMatrixPathLength,
        VehicleType::Bicycle
    );
    ASSERT_EQ(matrix1x1.size(), 1u);
    ASSERT_EQ(matrix1x1[0].size(), 1u);
    EXPECT_WEIGHT_EQ(expectedWeight, matrix1x1[0][0]);

    const auto matrix2x2 = router.findWeightsMatrix(
        {point1, point1},
        {point2, point2},
        Shared::config().maxMatrixPathLength,
        VehicleType::Bicycle
    );
    ASSERT_EQ(matrix2x2.size(), 2u);
    for (const auto& row : matrix2x2) {
        ASSERT_EQ(row.size(), 2u);
        for (const auto& weight : row) {
            EXPECT_WEIGHT_EQ(expectedWeight, weight);
        }
    }

    const maps::geolib3::Point2 straightWayPoint1{37.578363, 55.731142};
    const maps::geolib3::Point2 straightWayPoint2{37.578052, 55.731034};
    const Weight expectedStraightWay{23., 6.};

    const auto matrixStraightWay1x1 = router.findWeightsMatrix(
        {straightWayPoint1},
        {straightWayPoint2},
        Shared::config().maxMatrixPathLength,
        VehicleType::Bicycle
    );
    ASSERT_EQ(matrixStraightWay1x1.size(), 1u);
    ASSERT_EQ(matrixStraightWay1x1.size(), 1u);
    EXPECT_WEIGHT_EQ(expectedStraightWay, matrixStraightWay1x1[0][0]);

    const auto matrixStraightWay2x1 = router.findWeightsMatrix(
        {straightWayPoint1, straightWayPoint1},
        {straightWayPoint2},
        Shared::config().maxMatrixPathLength,
        VehicleType::Bicycle
    );
    ASSERT_EQ(matrixStraightWay2x1.size(), 2u);
    for (const auto& row : matrixStraightWay2x1) {
        ASSERT_EQ(row.size(), 1u);
        EXPECT_WEIGHT_EQ(expectedStraightWay, row[0]);
    }
}

void checkPathsEq(const Path& actual, const Path& expected)
{
    ASSERT_POLYLINE_EQ(actual.polyline(), expected.polyline());

    ASSERT_EQ(actual.wayPoints().size(), expected.wayPoints().size());
    for (std::size_t i = 0; i < actual.wayPoints().size(); ++i) {
        EXPECT_POINT_EQ(actual.wayPoints()[i].wayPoint(),
            expected.wayPoints()[i].wayPoint());
    }

    ASSERT_EQ(actual.legs().size(), expected.legs().size());
    for (std::size_t i = 0; i < actual.legs().size(); ++i) {
        EXPECT_LEG_EQ(actual.legs()[i], expected.legs()[i]);
    }
    ASSERT_WEIGHT_EQ(actual.weight(), expected.weight());

    ASSERT_EQ(actual.accessPassesIndexes().size(), expected.accessPassesIndexes().size());
    for (std::size_t i = 0; i < actual.accessPassesIndexes().size(); ++i) {
        ASSERT_EQ(actual.accessPassesIndexes()[i], expected.accessPassesIndexes()[i]);
    }

    ASSERT_EQ(actual.constructions().size(), expected.constructions().size());
    for (std::size_t i = 0; i < actual.constructions().size(); ++i) {
        ASSERT_EQ(actual.constructions()[i].id, expected.constructions()[i].id);
        EXPECT_EQ(actual.constructions()[i].count, expected.constructions()[i].count);
    }

    ASSERT_EQ(actual.trafficTypes().size(), expected.trafficTypes().size());
    for (std::size_t i = 0; i < actual.trafficTypes().size(); ++i) {
        ASSERT_EQ(actual.trafficTypes()[i].id, expected.trafficTypes()[i].id);
        EXPECT_EQ(actual.trafficTypes()[i].count, expected.trafficTypes()[i].count);
    }
}

Y_UNIT_TEST(UriResolvingSimple)
{
    BicycleRouter router{Shared::config()};
    const RequestPoint from({37.619915, 55.754216});
    const RequestPoint to({37.588612, 55.733721});

    const auto paths = router.findPaths({from, to});
    ASSERT_GT(paths.size(), 0ul);
    const auto uri = uri::encodePathToUri(paths[0]);
    const auto resolvedPathOpt = router.resolveUri(uri);
    ASSERT_TRUE(resolvedPathOpt.has_value());

    checkPathsEq(resolvedPathOpt.value(), paths[0]);
}

Y_UNIT_TEST(UriResolvingSamePoints)
{
    BicycleRouter router{Shared::config()};
    const RequestPoint from({37.619915, 55.754216});
    const RequestPoint via({37.619915, 55.754216});
    const RequestPoint to({37.619915, 55.754216});

    const auto paths = router.findPaths({from, via, to});
    ASSERT_GT(paths.size(), 0ul);
    const auto uri = uri::encodePathToUri(paths[0]);
    const auto delimitersNumber =
        std::count(uri.cbegin(), uri.cend(), path_uri::URI_DELIMITER[0]);
    ASSERT_EQ(delimitersNumber, 4l);
    const auto resolvedPathOpt = router.resolveUri(uri);
    ASSERT_TRUE(resolvedPathOpt.has_value());

    checkPathsEq(resolvedPathOpt.value(), paths[0]);
}

Y_UNIT_TEST(UriResolvingManyPoints)
{
    BicycleRouter router{Shared::config()};
    const std::vector<RequestPoint> requestPoints({
        RequestPoint({37.598152,55.757756}),
        RequestPoint({37.645118,55.759461}),
        RequestPoint({37.630449,55.739394}),
        RequestPoint({37.622150,55.755680})
    });
    const auto paths = router.findPaths(requestPoints);
    ASSERT_GT(paths.size(), 0ul);
    const auto uri = uri::encodePathToUri(paths[0]);
    const auto resolvedPathOpt = router.resolveUri(uri);
    ASSERT_TRUE(resolvedPathOpt.has_value());

    checkPathsEq(resolvedPathOpt.value(), paths[0]);
}

}

} // namespace maps::bicycle::tests
