#include "../common.h"

#include <maps/bicycle/router/lib/bicycle_router.h>
#include <maps/bicycle/router/tests/lib/shared.h>
#include <maps/bicycle/router/tests/lib/request_points.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace maps::bicycle::tests {

using namespace maps::geolib3;
using namespace maps::masstransit::routing;

void testMatrixRouteEquality(VehicleType vehicleType)
{
    constexpr double relativeDistanceDifferenceThreshold = 0.01;
    constexpr double relativeTimeDifferenceThreshold = 0.01;
    auto relativeDifference = [](const double value1, const double value2) {
        return std::abs(value1 - value2) / std::max(value1, value2);
    };

    BicycleRouter router{Shared::config()};

    RequestPointsReader pointsReader;
    auto requestOptional = pointsReader.nextRequest();
    while (requestOptional.has_value()) {
        const auto& request = requestOptional.value();
        const auto& fromPoint = request.from;
        const auto& toPoint = request.to;
        const auto paths = router.findPaths(
            {
                RequestPoint(fromPoint),
                RequestPoint(toPoint)
            },
            vehicleType,
            true // forceNoAlternatives
        );
        ASSERT_GT(paths.size(), 0u) << request;
        const auto pathWeight = paths[0].weight();
        const auto pathLength = pathWeight.length();
        const auto pathTime = pathWeight.time();
        const auto matrix1x1 = router.findWeightsMatrix(
            {fromPoint},
            {toPoint},
            Shared::config().maxMatrixPathLength * 2.,
            vehicleType
        );
        ASSERT_EQ(matrix1x1.size(), 1u) << request;
        ASSERT_EQ(matrix1x1[0].size(), 1u) << request;
        const double matrixLength = matrix1x1[0][0].length();
        const double matrixTime = matrix1x1[0][0].time();

        EXPECT_LE(
            relativeDifference(pathLength, matrixLength),
            relativeDistanceDifferenceThreshold
        ) << request << " path distance: " <<  pathLength << " matrix length: " << matrixLength;
        if (std::max(pathTime, matrixTime) > 180.) {
            EXPECT_LE(
                relativeDifference(pathTime, matrixTime),
                relativeTimeDifferenceThreshold
            ) << request << " path time: " <<  pathTime << " matrix time: " << matrixTime;
        }

        const auto matrix2x1 = router.findWeightsMatrix(
            {fromPoint, fromPoint},
            {toPoint},
            // Have to multiply by 2 here because the result is not accurate
            // because of this limitation
            Shared::config().maxMatrixPathLength * 2.,
            vehicleType
        );
        ASSERT_EQ(matrix2x1.size(), 2u);
        ASSERT_EQ(matrix2x1[0].size(), 1u);
        ASSERT_EQ(matrix2x1[1].size(), 1u);
        EXPECT_WEIGHT_EQ(matrix2x1[0][0], matrix2x1[1][0]);
        EXPECT_LE(
            relativeDifference(matrix1x1[0][0].length(), matrix2x1[0][0].length()),
            relativeDistanceDifferenceThreshold
        ) << request << " matrix1x1 length: " << matrix1x1[0][0].length() <<
            " matrix2x1 length: " << matrix2x1[0][0].length();
        EXPECT_LE(
            relativeDifference(matrix1x1[0][0].time(), matrix2x1[0][0].time()),
            relativeDistanceDifferenceThreshold
        ) << request << " matrix1x1 time: " << matrix1x1[0][0].time() <<
            " matrix2x1 time: " << matrix2x1[0][0].time();

        requestOptional = pointsReader.nextRequest();
    }
}

Y_UNIT_TEST_SUITE(Router_medium_tests)
{

    Y_UNIT_TEST(Matrix_route_equality)
    {
        testMatrixRouteEquality(VehicleType::Bicycle);
    }

    Y_UNIT_TEST(Matrix_route_equality_scooter)
    {
        testMatrixRouteEquality(VehicleType::Scooter);
    }
}

} // namespace maps::bicycle::tests
