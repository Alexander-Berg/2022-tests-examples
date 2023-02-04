#define BOOST_TEST_ALTERNATIVE_INIT_API

#include "helpers.h"
#include "partitioner.h"
#include "suite.h"

#include <maps/libs/common/include/exception.h>
#include <maps/libs/geolib/include/distance.h>
#include <maps/libs/geolib/include/bounding_box.h>
#include <yandex/maps/wiki/geom_tools/polygonal/partition.h>
#include <yandex/maps/wiki/test_tools/suite_builder.h>

#include <boost/test/unit_test.hpp>
#include <boost/test/test_tools.hpp>

#include <geos/geom/LinearRing.h>

using namespace maps;
using namespace maps::wiki;
using namespace maps::wiki::geom_tools;
using namespace maps::wiki::geom_tools::test;

const double CHECK_GRID_SIZE = 0.1;

enum class PointType { External, Internal, Within };

PointType
computePointType(
    const geolib3::Point2& point, const GeolibPolygonVector& polygons, double tolerance)
{
    bool isExternal = true;
    bool isInternal = false;
    for (const auto& poly : polygons) {
        bool contains = geolib3::distance(poly, point) < tolerance;
        isExternal = isExternal && !contains;
        isInternal = isInternal || contains;
    }

    if (isExternal) {
        return PointType::External;
    } else if (isInternal) {
        return PointType::Internal;
    } else {
        return PointType::Within;
    }
}

class PolygonPartitionTestRunner {
public:

    static void run(const PolygonPartitionTestData& testData)
    {
        GeolibPolygonVector result;
        BOOST_REQUIRE_NO_THROW(
            result = partition(
                testData.polygons,
                testData.maxVertices,
                testData.minSize,
                testData.tolerance));

        if (testData.polygons.empty()) {
            return;
        }

        checkPolygons("partition", result, testData.expected);

    // //    for (const auto& poly : result) {
    // //         BOOST_CHECK_MESSAGE(!geolib3::spatialRelation(poly, geolib3::Point2(3.5, 6.5), geolib3::Contains),
    // //                             geolib3::WKT<geolib3::Polygon2>::write(poly)
    // //         );
    // //         BOOST_CHECK_MESSAGE(false, geolib3::WKT<geolib3::Polygon2>::write(poly));
    // //    }

        geolib3::BoundingBox bbox = testData.polygons.front().boundingBox();
        for (auto it = std::next(testData.polygons.begin()); it != testData.polygons.end(); ++it) {
            bbox = geolib3::expand(bbox, it->boundingBox());
        }

        for (double x = bbox.minX(); x < bbox.maxX(); x += CHECK_GRID_SIZE) {
            for (double y = bbox.minY(); y < bbox.maxY(); y += CHECK_GRID_SIZE) {
                geolib3::Point2 point(x, y);
                auto originalPointType = computePointType(point, testData.polygons, testData.tolerance);
                auto resultPointType = computePointType(point, result, testData.tolerance);
                BOOST_CHECK_MESSAGE(originalPointType != PointType::External || resultPointType == PointType::External,
                    "Point (" << point.x() << ", " << point.y() << ")"
                        << " is outside of all original polygons but is within one of result polygons");
                BOOST_CHECK_MESSAGE(originalPointType != PointType::Internal || resultPointType != PointType::External,
                    "Point (" << point.x() << ", " << point.y() << ")"
                        << " is within one of original polygons but is outside of all result polygons");
            }
        }
    }
};

boost::unit_test::test_suite* init_unit_test_suite(int, char**)
{
    test_tools::BoostTestSuiteBuilder<PolygonPartitionTestData, PolygonPartitionTestRunner>
    builder(boost::unit_test::framework::master_test_suite());

    mainTestSuite()->visit(builder);

    return nullptr;
}

#ifdef YANDEX_MAPS_BUILD
bool init_unit_test_suite()
{
    init_unit_test_suite(0, NULL);
    return true;
}

int main(int argc, char** argv)
{
    return boost::unit_test::unit_test_main(&init_unit_test_suite, argc, argv);
}
#endif //YANDEX_MAPS_BUILD
