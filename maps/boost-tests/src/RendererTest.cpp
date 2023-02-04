#if defined(_WIN32)
#   ifndef NOMINMAX
#       define NOMINMAX
#   endif
#endif

#include "../include/stringsTest.h"
#include "../include/RendererTest.h"
#include "../include/MapTest.h"
#include "../include/RasterizerTest.h"
#include "../include/xmlwrappTest.h"
#include "../include/metadataTest.h"

#include <maps/renderer/libs/base/include/geom/vertices.h>
#include <yandex/maps/renderer/geometry/intersections.h>

using namespace boost::unit_test;
using namespace maps::renderer5;
using maps::renderer::geometry::pointInPoly;

namespace maps { namespace renderer5 { namespace test { namespace geometry  {

void boundaryChecker()
{
    maps::renderer::base::Vertices vertices;
    vertices.addLineTo({10.0, 10.0});
    vertices.addLineTo({20.0, 20.0});
    vertices.addLineTo({40.0, 20.0});
    vertices.addLineTo({50.0, 10.0});
    vertices.addLineTo({30.0,  0.0});
    vertices.addEndPoly();

    BOOST_CHECK(!pointInPoly(vertices, {15, 30}));
    BOOST_CHECK(pointInPoly(vertices, {30, 15}));

    BOOST_CHECK(pointInPoly(vertices, {30, 10}));
    BOOST_CHECK(!pointInPoly(vertices, {0, 20}));

    BOOST_CHECK(pointInPoly(vertices, {30, 20}));
}

test_suite* init_suite()
{
    test_suite* suite = BOOST_TEST_SUITE("Geometry functions test suite");

    suite->add(
        BOOST_TEST_CASE(&boundaryChecker));

    return suite;
}

} } } }

test_suite* maps::renderer5::test::init_renderer_suite()
{
    test_suite* suite = BOOST_TEST_SUITE("Renderer test suite");

    suite->add(test::xmlwrapp::init_suite());
    suite->add(test::geometry::init_suite());
    suite->add(test::rasterizer::init_suite());
    suite->add(test::strings::init_suite());
    suite->add(test::map::init_suite());
    suite->add(test::metadata::init_suite());

    return suite;
}
