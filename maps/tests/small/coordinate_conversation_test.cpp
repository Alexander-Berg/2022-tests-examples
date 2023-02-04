#include <maps/indoor/long-tasks/src/radiomap-evaluation-cron-job/lib/impl/utils.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

#include <random>

namespace maps::mirc::radiomap_evaluator::tests {
using namespace ::testing;

namespace {

void checkCloseGeodeticPoints(
    const geolib3::Point2& p1,
    const geolib3::Point2& p2,
    double epsilon)
{
    double deltaLat = p1.y() - p2.y();
    double deltaLon = p1.x() - p2.x();
    if (deltaLon > 180) {
        deltaLon -= 360;
    } else if (deltaLon <= -180) {
        deltaLon += 360;
    }

    ASSERT_EQ(
        std::fabs(deltaLat) < epsilon,
        true);

    ASSERT_EQ(
        std::fabs(deltaLon) < epsilon,
        true);
}

void checkClosePlanarPoints(
    const geolib3::Point2& p1,
    const geolib3::Point2& p2,
    double epsilon)
{
    ASSERT_EQ(
        std::fabs(p1.x() - p2.x()) < epsilon,
        true);

    ASSERT_EQ(
        std::fabs(p1.y() - p2.y()) < epsilon,
        true);
}

} // namespace

Y_UNIT_TEST_SUITE(conversation_tests) {

Y_UNIT_TEST(geodeticToLocalPlanar)
{
    geolib3::Point2 originPoint(45, 60);

    // Geo-coordinates are entered with precision 6 decimal digits which implies 0.1 m accuracy
    std::vector<std::pair<geolib3::Point2, geolib3::Point2>> testPoints = {
        {{0, 0}, {45.000000, 60.000000}},
        {{10, 0}, {45.000180, 60.000000}},
        {{-10, 0}, {44.999820, 60.000000}},
        {{10, 10}, {45.000180, 60.000090}},
        {{-10, -10}, {44.999820, 59.999910}},
        {{0, 10}, {45.000000, 60.000090}},
        {{0, -10}, {45.000000, 59.999910}}};

    for(const auto& testPoint : testPoints) {
        checkCloseGeodeticPoints(
            localPlanarToGeodetic(testPoint.first, originPoint),
            testPoint.second, 1e-6);

        checkClosePlanarPoints(
            geodeticToLocalPlanar(testPoint.second, originPoint),
            testPoint.first, 0.1);
    }
}

Y_UNIT_TEST(geodeticToLocalPlanar_random)
{
    const int NUM_POINTS = 1000;
    geolib3::Point2 originPoint(37.619822, 55.752096);

    std::mt19937 rng;
    std::uniform_real_distribution<double> distribution(-100, 100);

    for(int i = 0; i < NUM_POINTS; ++i) {
        const auto planarPoint = geolib3::Point2(
            distribution(rng),
            distribution(rng));

        const auto geoPoint = localPlanarToGeodetic(planarPoint, originPoint);

        checkClosePlanarPoints(
            geodeticToLocalPlanar(geoPoint, originPoint),
            planarPoint, 1e-6);
    }
}

Y_UNIT_TEST(geodeticToLocalPlanar_close180)
{
    const int NUM_POINTS = 1000;
    geolib3::Point2 originPoint1(179.999999, 60);
    geolib3::Point2 originPoint2(-179.999999, 60);

    std::mt19937 rng;
    std::uniform_real_distribution<double> distribution(-100, 100);

    for(int i = 0; i < NUM_POINTS; ++i) {
        const auto planarPoint = geolib3::Point2(
            distribution(rng),
            distribution(rng));

        checkClosePlanarPoints(
            geodeticToLocalPlanar(localPlanarToGeodetic(planarPoint, originPoint1), originPoint1),
            planarPoint, 1e-6);

        checkClosePlanarPoints(
            geodeticToLocalPlanar(localPlanarToGeodetic(planarPoint, originPoint2), originPoint2),
            planarPoint, 1e-6);
    }
}

} // suite
} // aps::mirc::radiomap_evaluator::tests
