#include <library/cpp/testing/common/env.h>

#include "../test_tools/geom_test_utils.h"
#include "../test_tools/geom_test_types.h"
#include "../test_tools/geom_transform.h"

#include "../geom/linear_ring.h"
#include "../geom/polygon.h"
#include "../geom/multipolygon.h"

#include "../cut/cutter.h"

#include <maps/libs/geolib/include/segment.h>
#include <maps/libs/geolib/include/point.h>
#include <maps/libs/geolib/include/polyline.h>
#include <maps/libs/common/include/exception.h>

#include <boost/test/unit_test.hpp>

#include <fstream>

using maps::geolib3::Polyline2;

using maps::coverage5::geom::StandaloneLinearRing;
using maps::coverage5::geom::StandalonePolygon;
using maps::coverage5::geom::StandaloneMultiPolygon;

using maps::coverage5::test::TestRing;
using maps::coverage5::test::TestPolyline;
using maps::coverage5::test::TestPolygon;
using maps::coverage5::test::TestMultiPolygon;
using maps::coverage5::test::TestLinealCut;
using maps::coverage5::test::TestPolygonalCut;
using maps::coverage5::test::IdentityTransform;
using maps::coverage5::test::SwitchXYTransform;
using maps::coverage5::test::ReflectionTransform;

using maps::coverage5::test::polylineCutChecker;
using maps::coverage5::test::ringCutChecker;
using maps::coverage5::test::polygonCutChecker;
using maps::coverage5::test::multiPolygonCutChecker;

using maps::coverage5::test::operator>>;

using std::operator<<;

typedef void (*CutChecker)(std::ifstream&, const char*, const std::string&);

const std::string g_polylineTestFile = SRC_("in/polyline.in");
const std::string g_ringTestFile = SRC_("in/ring.in");
const std::string g_polygonTestFile = SRC_("in/polygon.in");
const std::string g_multiPolygonTestFile = SRC_("in/multipolygon.in");

void testCut(const char* message, const std::string& filename,
    CutChecker cutChecker)
{
    std::ifstream input;
    input.open(filename);
    BOOST_ASSERT(input.is_open());
    size_t numTests;
    input >> numTests;
    for (size_t i = 0; i < numTests; ++i) {
        cutChecker(input, message, filename);
    }
    input.close();
}

BOOST_AUTO_TEST_CASE(test_polyline_cut)
{
    testCut("Running polyline cut test",
        g_polylineTestFile,
        &polylineCutChecker<IdentityTransform>);
    testCut("Running polyline cut test with XY-coords switched",
        g_polylineTestFile,
        &polylineCutChecker<SwitchXYTransform>);
    testCut("Running polyline cut test with coords reflected related to cut line",
        g_polylineTestFile,
        &polylineCutChecker<ReflectionTransform>);
}

BOOST_AUTO_TEST_CASE(test_ring_cut)
{
    testCut("Running ring cut test",
        g_ringTestFile,
        &ringCutChecker<IdentityTransform>);
    testCut("Running ring cut test with XY-coords switched",
        g_ringTestFile,
        &ringCutChecker<SwitchXYTransform>);
    testCut("Running ring cut test with coords reflected related to cut line",
        g_ringTestFile,
        &ringCutChecker<ReflectionTransform>);
}

BOOST_AUTO_TEST_CASE(test_polygon_cut)
{
    testCut("Running polygon cut test",
        g_polygonTestFile,
        &polygonCutChecker<IdentityTransform>);
    testCut("Running polygon cut test with XY-coords switched",
        g_polygonTestFile,
        &polygonCutChecker<SwitchXYTransform>);
    testCut("Running polygon cut test with coords reflected related to cut line",
        g_polygonTestFile,
        &polygonCutChecker<ReflectionTransform>);
}

BOOST_AUTO_TEST_CASE(test_multipolygon_cut)
{
    testCut("Running multipolygon cut test",
        g_multiPolygonTestFile,
        &multiPolygonCutChecker<IdentityTransform>);
    testCut("Running multipolygon cut test with XY-coords switched",
        g_multiPolygonTestFile,
        &multiPolygonCutChecker<SwitchXYTransform>);
    testCut("Running multipolygon cut test with coords reflected related to cut line",
        g_multiPolygonTestFile,
        &multiPolygonCutChecker<ReflectionTransform>);
}
