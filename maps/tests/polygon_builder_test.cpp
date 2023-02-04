#define BOOST_TEST_ALTERNATIVE_INIT_API

#include "helpers.h"
#include "suite.h"
#include "cutter.h"

#include <maps/libs/common/include/exception.h>
#include <maps/libs/geolib/include/spatial_relation.h>
#include <maps/libs/geolib/include/conversion_geos.h>
#include <maps/libs/geolib/include/serialization.h>
#include <yandex/maps/wiki/geom_tools/polygonal/builder.h>
#include <yandex/maps/wiki/test_tools/suite_builder.h>

#include <boost/test/unit_test.hpp>
#include <boost/test/test_tools.hpp>

#include <geos/geom/LinearRing.h>

using namespace maps;
using namespace maps::wiki;
using namespace maps::wiki::geom_tools;
using namespace maps::wiki::geom_tools::test;

struct GeosRingsVector {
    explicit GeosRingsVector(const GeolibLinearRingVector& rings)
    {
        ptrVector.reserve(rings.size());

        for (const auto& ring : rings) {
            ptrVector.push_back(geolib3::internal::geolib2geosGeometry(ring));
        }
    }

    GeosGeometryPtrVector ptrVector;
};

class PolygonBuilderTestRunner {
public:
    static void run(const PolygonBuilderTestData& testData)
    {
        if (!testData.expectedPolygons) {
            BOOST_REQUIRE_THROW(
                PolygonBuilder::build(
                    testData.shells,
                    testData.holes,
                    ValidateResult::Yes),
                PolygonBuildError);
            GeosRingsVector geosShells(testData.shells);
            GeosRingsVector geosHoles(testData.holes);
            BOOST_REQUIRE_THROW(
                PolygonBuilder::build(
                    geosShells.ptrVector,
                    geosHoles.ptrVector,
                    ValidateResult::Yes),
                PolygonBuildError);
            return;
        }

        GeolibPolygonVector result;

        BOOST_REQUIRE_NO_THROW(result = PolygonBuilder::build(
                                            testData.shells,
                                            testData.holes,
                                            ValidateResult::Yes));
        checkPolygons("Build polygons (geolib)", *testData.expectedPolygons, result);

        GeosRingsVector geosShells(testData.shells);
        GeosRingsVector geosHoles(testData.holes);
        BOOST_REQUIRE_NO_THROW(result = PolygonBuilder::build(
                                            geosShells.ptrVector,
                                            geosHoles.ptrVector,
                                            ValidateResult::Yes));
        checkPolygons("Build polygons", *testData.expectedPolygons, result);
    }
};

boost::unit_test::test_suite* init_unit_test_suite(int, char**)
{
    test_tools::BoostTestSuiteBuilder<PolygonBuilderTestData, PolygonBuilderTestRunner>
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
