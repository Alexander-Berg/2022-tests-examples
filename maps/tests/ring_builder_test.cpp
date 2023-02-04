#define BOOST_TEST_ALTERNATIVE_INIT_API

#include "helpers.h"
#include "ring_builder.h"
#include "suite.h"

#include <maps/libs/common/include/exception.h>
#include <maps/libs/geolib/include/spatial_relation.h>
#include <maps/libs/geolib/include/conversion_geos.h>
#include <maps/libs/geolib/include/serialization.h>
#include <yandex/maps/wiki/geom_tools/polygonal/builder.h>
#include <yandex/maps/wiki/test_tools/suite_builder.h>

#include <boost/test/unit_test.hpp>
#include <boost/test/test_tools.hpp>

#include <geos/geom/LineString.h>

using namespace maps;
using namespace maps::wiki;
using namespace maps::wiki::geom_tools;
using namespace maps::wiki::geom_tools::test;

struct GeosPolylinesVector {
    explicit GeosPolylinesVector(const geolib3::PolylinesVector& geoms)
    {
        this->geoms.reserve(geoms.size());
        geomPtrVector.reserve(geoms.size());

        for (const auto& poly : geoms) {
            this->geoms.push_back(geolib3::internal::geolib2geosGeometry(poly));
            geomPtrVector.push_back(this->geoms.back().get());
        }
    }

    std::vector<std::unique_ptr<geos::geom::LineString>> geoms;
    GeosGeometryPtrVector geomPtrVector;
};

class RingBuilderTestRunner {
public:
    static void run(const RingBuilderTestData& testData)
    {
        if (!testData.expectedRing) {
            BOOST_REQUIRE_THROW(
                LinearRingBuilder::build(testData.geoms, testData.tolerance),
                RingBuildError);
            GeosPolylinesVector geosGeoms(testData.geoms);
            BOOST_REQUIRE_THROW(
                LinearRingBuilder::build(geosGeoms.geomPtrVector, testData.tolerance),
                RingBuildError);
            return;
        }

        geolib3::LinearRing2 resultRing;
        BOOST_REQUIRE_NO_THROW(resultRing = LinearRingBuilder::build(
            testData.geoms,
            testData.tolerance));
        geolib3::Polygon2 resultPoly = {resultRing, {}, /* bool validate = */ false};
        geolib3::Polygon2 expectedPoly = {*testData.expectedRing, {}, /* bool validate = */ false};

        BOOST_REQUIRE_MESSAGE(
            geolib3::spatialRelation(resultPoly, expectedPoly, geolib3::Equals),
            "(geolib) Result mismatch: expected "
                << geolib3::WKT::toString(expectedPoly)
                << ", received: " << geolib3::WKT::toString(resultPoly));

        GeosPolylinesVector geosGeoms(testData.geoms);
        BOOST_REQUIRE_NO_THROW(resultRing = LinearRingBuilder::build(
            geosGeoms.geomPtrVector,
            testData.tolerance));
        resultPoly = {resultRing, {}, /* bool validate = */ false};
        BOOST_REQUIRE_MESSAGE(
            geolib3::spatialRelation(resultPoly, expectedPoly, geolib3::Equals),
            "(geos) Result mismatch: expected "
                << geolib3::WKT::toString(expectedPoly)
                << ", received: " << geolib3::WKT::toString(resultPoly));
    }
};

boost::unit_test::test_suite* init_unit_test_suite(int, char**)
{
    test_tools::BoostTestSuiteBuilder<RingBuilderTestData, RingBuilderTestRunner>
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
