#include <maps/wikimap/feedback/api/src/libs/common/route_points.h>
#include <maps/wikimap/feedback/api/src/libs/common/tests/helpers/printers.h>

#include <maps/libs/common/include/base64.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <filesystem>
#include <fstream>
#include <unordered_map>

namespace fs = std::filesystem;

namespace maps::wiki::feedback::api::tests {

namespace {

struct RoutesTestData {
    struct Sample {
        std::string encodedRoute;
        maps::geolib3::PointsVector decodedRoute;

        Sample(
            std::string encodedRoute,
            maps::geolib3::PointsVector decodedRoute)
            : encodedRoute(std::move(encodedRoute))
            , decodedRoute(std::move(decodedRoute))
        {}
    };

    RoutesTestData(
        const std::string& encodedPath,
        const std::string& decodedPath)
        : samples(readSamples(encodedPath, decodedPath))
    {}

    const std::vector<Sample> samples;

private:
    std::vector<Sample> readSamples(
        const std::string& encodedPath,
        const std::string& decodedPath)
    {
        const auto encodedRoutes = readEncodedRoutes(encodedPath);
        const auto decodedRoutes = readDecodedRoutes(decodedPath);
        UNIT_ASSERT_VALUES_EQUAL(encodedRoutes.size(), decodedRoutes.size());

        std::vector<Sample> samples;
        samples.reserve(encodedRoutes.size());

        for (const auto& [key, route] : encodedRoutes) {
            auto iter = decodedRoutes.find(key);
            UNIT_ASSERT(iter != decodedRoutes.end());
            samples.emplace_back(route, decodedRoutes.at(key));
        }
        return samples;
    }

    std::unordered_map<std::string, std::string> readEncodedRoutes(
        const std::string& dataPath)
    {
        const std::string PATH = SRC_(dataPath);
        std::unordered_map<std::string, std::string> routes;
        for (const auto& entry : fs::directory_iterator(PATH)) {
            std::ifstream input(entry.path());
            std::string route;
            input >> route;
            routes.emplace(entry.path().filename(), std::move(route));
        }
        return routes;
    }

    std::unordered_map<std::string, maps::geolib3::PointsVector> readDecodedRoutes(
        const std::string& dataPath)
    {
        const std::string PATH = SRC_(dataPath);
        double x, y;
        std::unordered_map<std::string, maps::geolib3::PointsVector> routes;
        for (const auto& entry : fs::directory_iterator(PATH)) {
            std::ifstream input(entry.path());
            maps::geolib3::PointsVector route;
            while (input >> x >> y) {
                route.emplace_back(x, y);
            }
            routes.emplace(entry.path().filename(), std::move(route));
        }
        return routes;
    }
};

} // namespace

Y_UNIT_TEST_SUITE(test_route_points_serialization)
{

Y_UNIT_TEST(serialization_small)
{
    UNIT_ASSERT_VALUES_EQUAL(encodePoints({}), "");
    UNIT_ASSERT(decodePoints("").empty());

    maps::geolib3::PointsVector points{
        maps::geolib3::Point2{42.42, 53.53},
        maps::geolib3::Point2{53.53, 42.42},
        maps::geolib3::Point2{53.53, 42.42},
    };
    std::vector<uint8_t> bytes{
        0x20, 0x47, 0x87, 0x02,
        0x90, 0xcd, 0x30, 0x03,
        0x70, 0x86, 0xa9, 0x00,
        0x90, 0x79, 0x56, 0xff,
        0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00,
    };
    std::string encodedPoints =
        maps::base64EncodeUrlSafe(TArrayRef{bytes.data(), bytes.size()});

    UNIT_ASSERT_VALUES_EQUAL(encodePoints(points), encodedPoints);
    UNIT_ASSERT_VALUES_EQUAL(decodePoints(encodedPoints), points);
}

Y_UNIT_TEST(serialization_large)
{
    RoutesTestData testData("data/encoded_points", "data/decoded_points");

    for (const auto& sample : testData.samples) {
        UNIT_ASSERT_VALUES_EQUAL(
            encodePoints(sample.decodedRoute),
            sample.encodedRoute);
        UNIT_ASSERT_VALUES_EQUAL(
            decodePoints(sample.encodedRoute),
            sample.decodedRoute);
    }
}

} // test_route_points_serialization suite

} // namespace maps::wiki::feedback::api::tests
