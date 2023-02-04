#include <maps/wikimap/mapspro/services/editor/src/configs/config.h>
#include <maps/wikimap/mapspro/services/editor/src/geom.h>
#include <maps/wikimap/mapspro/services/editor/src/utils.h>
#include <maps/wikimap/mapspro/services/editor/src/utils.h>

#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/tests_common.h>

namespace maps::wiki::tests {

Y_UNIT_TEST_SUITE(geom_poly_buffers)
{
WIKI_FIXTURE_TEST_CASE(test_polygon_rings_buffer, EditorTestFixture)
{
    common::Geom g(createGeomFromJsonStr(loadFile("tests/data/polygon.json")));
    WIKI_TEST_REQUIRE_EQUAL(g->getGeometryTypeId(), geos::geom::GEOS_POLYGON);
    auto buffers = createPolygonBuffers(g, 5, PolygonBufferPolicy::Rings);
    UNIT_ASSERT_EQUAL(buffers.size(), 2);
    UNIT_ASSERT_EQUAL(buffers.front()->getGeometryTypeId(), geos::geom::GEOS_POLYGON);
    UNIT_ASSERT_EQUAL(buffers.back()->getGeometryTypeId(), geos::geom::GEOS_POLYGON);
}
} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
