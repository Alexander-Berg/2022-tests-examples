#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <maps/bicycle/router/lib/uri.h>

namespace maps::bicycle::tests {

using namespace maps::bicycle::uri;
using namespace maps::geolib3;

#define CHECK_URI(uri, path) \
    ASSERT((uri).starts_with(URI_PREFIX)); \
    const auto tildaNum = std::count((uri).cbegin(), (uri).cend(), '~'); \
    ASSERT_EQ(std::size_t(tildaNum), (path).wayPoints().size() * 2 - 2); \
    const auto dotsNum = std::count((uri).cbegin(), (uri).cend(), '.'); \
    ASSERT_EQ(std::size_t(dotsNum), (path).wayPoints().size() * 2); \
    const auto percentNum = std::count((uri).cbegin(), (uri).cend(), '%'); \
    ASSERT_EQ(std::size_t(percentNum), (path).wayPoints().size());

#define CHECK_POINT(actual, expected) \
    EXPECT_NEAR((actual).x(), (expected).x(), \
        1 / maps::masstransit::uri::COORDINATE_PRECISION); \
    EXPECT_NEAR((actual).y(), (expected).y(), \
        1 / maps::masstransit::uri::COORDINATE_PRECISION);

#define CHECK_POINTS(actual, expectedPath) \
    ASSERT_EQ((actual).size(), (expectedPath).size()); \
    for (std::size_t j = 0; j < (actual).size(); ++j) { \
        CHECK_POINT((actual)[j].wayPoint(), (expectedPath)[j]); \
    } 

void checkSegments(const maps::masstransit::uri::path::UriSegments& uriSegments,
        const Path& path) {
    ASSERT_EQ(uriSegments.size(), path.wayPoints().size() - 1);

    for (std::size_t i = 0; i < uriSegments.size(); ++i) {
        if (path.legs()[i].begin + 2 >= path.legs()[i].end - 1) {
            continue;
        }
        std::vector<Point2> expectedPoints(
            std::next(path.polyline().points().begin(), path.legs()[i].begin + 2),
            std::next(path.polyline().points().begin(), path.legs()[i].end - 1)
        );
        CHECK_POINTS(uriSegments[i].intermediatePoints(), expectedPoints);
        CHECK_POINT(uriSegments[i].from<RequestPoint>().wayPoint(), path.wayPoints()[i].wayPoint());
        CHECK_POINT(uriSegments[i].to<RequestPoint>().wayPoint(), path.wayPoints()[i + 1].wayPoint());
    }
}

Y_UNIT_TEST_SUITE(Uri)
{

Y_UNIT_TEST(UriEncodeDecodeShortPath)
{
    const Path path(
        {
            WayPoint({35.123213, 53.891289}),
            WayPoint({38.927583, 54.917482})
        },
        Polyline2({
            {35.123213, 53.891289}, // WayPoint
            {35.382672, 52.546473},
            {37.123214, 54.891299}, 
            {38.927583, 54.917482} // WayPoint
        }), {
            Path::Leg{Weight{1., 2.}, 0, 3},
        },
        {}, // accessPassesIndexes
        {{Construction::ID::UNKNOWN, 4}},
        {{TrafficType::ID::BICYCLE, 4}},
        VehicleType::Bicycle, // vehicleType
        0.5 // bindingSpeed
    );
    const auto uri = encodePathToUri(path);
    CHECK_URI(uri, path);

    const auto [uriSegments, vehicleType] = decodeUri(uri);

    ASSERT_EQ(vehicleType, path.vehicleType());

    checkSegments(uriSegments, path);
}

Y_UNIT_TEST(UriEncodeDecodeLongPath)
{
    const Path path(
        {
            WayPoint({35.123213, 53.891289}),
            WayPoint({36.917582, 52.017821}),
            WayPoint({38.927583, 54.917482})
        },
        Polyline2({
            {35.123213, 53.891289}, // WayPoint
            {35.382672, 52.546473},
            {35.779964, 52.454937},
            {35.687059, 53.198624},
            {36.917581, 52.017820}, // near WayPoint
            {36.917582, 52.017821}, // WayPoint
            {36.917583, 52.017823}, // near WayPoint
            {37.659466, 53.789236},
            {37.130266, 54.815474},
            {38.927583, 54.917482} // WayPoint
        }), {
            Path::Leg{Weight{1., 2.}, 0, 4},
            Path::Leg{Weight{3., 4.}, 6, 9},
        },
        {}, // accessPassesIndexes
        {{Construction::ID::UNKNOWN, 9}},
        {{TrafficType::ID::BICYCLE, 9}},
        VehicleType::Bicycle, // vehicleType
        0.5 // bindingSpeed
    );

    const auto uri = encodePathToUri(path);
    CHECK_URI(uri, path);

    const auto [uriSegments, vehicleType] = decodeUri(uri);

    ASSERT_EQ(vehicleType, path.vehicleType());

    checkSegments(uriSegments, path);
}

}

} // namespace maps::bicycle::tests
