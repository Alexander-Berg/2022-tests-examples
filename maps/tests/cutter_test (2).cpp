#define BOOST_TEST_ALTERNATIVE_INIT_API

#include "helpers.h"
#include "suite.h"
#include "cutter.h"

#include <yandex/maps/wiki/test_tools/suite_builder.h>

#include <boost/test/unit_test.hpp>
#include <boost/test/test_tools.hpp>

using namespace maps;
using namespace maps::wiki;
using namespace maps::wiki::geom_tools;
using namespace maps::wiki::geom_tools::test;

void
checkPolygons(
    Relation relation,
    const GeolibPolygonVector& expected, const GeolibPolygonVector& received)
{
    const std::string relStr = relation == Relation::Less ? "less" : "greater";
    checkPolygons(relStr, expected, received);
}

CutLine
transpose(const CutLine& cutLine)
{
    return CutLine(
        cutLine.direction() == Direction::X ? Direction::Y : Direction::X,
        cutLine.coord(),
        cutLine.tolerance());
}

PolygonCutterTestData
transpose(const PolygonCutterTestData& data)
{
    TransposeTransform trf;
    return PolygonCutterTestData {
        transform(data.polygon, trf),
        transpose(data.cutLine),
        PolygonCutter::Result {
            transform(data.expectedResult.less, trf),
            transform(data.expectedResult.greater, trf)
        }
    };
}

PolygonCutterTestData
reflect(const PolygonCutterTestData& data)
{
    ReflectTransform trf(data.cutLine);
    return PolygonCutterTestData {
        transform(data.polygon, trf),
        data.cutLine,
        PolygonCutter::Result {
            transform(data.expectedResult.greater, trf),
            transform(data.expectedResult.less, trf)
        }
    };
}

class PolygonCutterTestRunner
{
public:

    static void run(const PolygonCutterTestData& testData)
    {
        runImpl(testData);
        runImpl(transpose(testData));
        runImpl(reflect(testData));
    }

private:
    static void runImpl(const PolygonCutterTestData& testData)
    {
        PolygonCutter cutter(testData.cutLine);

        boost::optional<PolygonCutter::Result> polygons;

        /*BOOST_REQUIRE_NO_THROW(*/polygons = cutter(testData.polygon)/*)*/;
        BOOST_REQUIRE(polygons);

        checkPolygons(Relation::Less, testData.expectedResult.less, polygons->less);
        checkPolygons(Relation::Greater, testData.expectedResult.greater, polygons->greater);
    }
};

boost::unit_test::test_suite* init_unit_test_suite(int, char**)
{
    test_tools::BoostTestSuiteBuilder<PolygonCutterTestData, PolygonCutterTestRunner>
        builder(boost::unit_test::framework::master_test_suite());

    mainTestSuite()->visit(builder);

    return nullptr;
}

#ifdef YANDEX_MAPS_BUILD
bool init_unit_test_suite()
{
    throw 1;
    init_unit_test_suite(0, NULL);
    return true;
}

#include<iostream>

int main(int argc, char** argv)
{
#if defined(BOOST_TEST_MAIN) && !defined(BOOST_TEST_ALTERNATIVE_INIT_API)
#error hhuaaiaiaia
#endif
    return boost::unit_test::unit_test_main(&init_unit_test_suite, argc, argv);
}
#endif //YANDEX_MAPS_BUILD
