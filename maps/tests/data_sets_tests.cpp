#include "data_set_builder.h"

#include <maps/carparks/renderer/yacare/lib/data_sets.h>
#include <maps/renderer/libs/data_sets/data_set_test_util/schema_util.h>
#include <maps/renderer/libs/style2_renderer/include/vec3_render.h>
#include <maps/renderer/libs/style2_renderer/include/vec3_render_params.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/gtest.h>

namespace maps::carparks::renderer::tests {

namespace {

namespace mr = maps::renderer;

using common2::CarparkType;

const std::string SCHEMA_PATH =
    ArcadiaSourceRoot() + "/maps/doc/schemas/renderer/source/sources/carparks/0.x/source.json";

} // namespace


Y_UNIT_TEST_SUITE(data_sets_tests) {

Y_UNIT_TEST(sourceSchemaConsistency)
{
    DataSetBuilder builder;

    builder.addPoint({30, 50}, buildInfo(1, CarparkType::Toll,    "BY", "234", ""), "points");
    builder.addPoint({30, 51}, buildInfo(2, CarparkType::TollBld, "TR", "567", ""), "points");
    builder.addPoint({30, 52}, buildInfo(3, CarparkType::ControlledZone), "points");
    builder.addPoint({30, 53}, buildInfo(4, CarparkType::Free),           "points");
    builder.addPoint({30, 54}, buildInfo(5, CarparkType::FreeBld),        "points");
    builder.addPoint({30, 55}, buildInfo(6, CarparkType::ParkAndRide),    "points");
    builder.addPoint({30, 56}, buildInfo(7, CarparkType::Restricted),     "points");
    builder.addPoint({30, 57}, buildInfo(8, CarparkType::RestrictedBld),  "points");
    builder.addPoint({30, 58}, buildInfo(9, CarparkType::TollBld),        "points");

    builder.addPoint({31, 51}, buildInfo(10,  CarparkType::Free),      "markers");
    builder.addPoint({31, 52}, buildInfo(11, CarparkType::Prohibited), "markers");
    builder.addPoint({31, 53}, buildInfo(12, CarparkType::Restricted), "markers");
    builder.addPoint({31, 54}, buildInfo(13, CarparkType::Toll),       "markers");

    builder.addPoint({32, 51}, buildInfo(14, CarparkType::Free),       "shields");
    builder.addPoint({32, 52}, buildInfo(15, CarparkType::Prohibited), "shields");
    builder.addPoint({32, 53}, buildInfo(16, CarparkType::Restricted), "shields");
    builder.addPoint({32, 54}, buildInfo(17, CarparkType::Toll),       "shields");

    builder.addPoint({33, 51}, buildInfo(18, CarparkType::Free),       "dynamic_markers");
    builder.addPoint({33, 52}, buildInfo(19, CarparkType::Prohibited), "dynamic_markers");
    builder.addPoint({33, 53}, buildInfo(20, CarparkType::Restricted), "dynamic_markers");
    builder.addPoint({33, 54}, buildInfo(21, CarparkType::Toll),       "dynamic_markers");

    builder.addPolyline({{20, 20}, {25, 21}}, buildInfo(22, CarparkType::Free));
    builder.addPolyline({{20, 20}, {25, 22}}, buildInfo(23, CarparkType::Prohibited));
    builder.addPolyline({{20, 20}, {25, 23}}, buildInfo(24, CarparkType::Restricted));
    builder.addPolyline({{20, 20}, {25, 24}}, buildInfo(25, CarparkType::Toll));

    builder.addPolygon({{{10, 10}, {20, 10}, {15, 15}}}, buildInfo(26, CarparkType::Toll));
    builder.addPolygon({{{10, 10}, {20, 11}, {15, 15}}}, buildInfo(27, CarparkType::Lot));
    builder.addPolygon({{{10, 10}, {20, 12}, {15, 15}}}, buildInfo(28, CarparkType::Free));
    builder.addPolygon({{{10, 10}, {20, 13}, {15, 15}}}, buildInfo(29, CarparkType::FreeBld));
    builder.addPolygon({{{10, 10}, {20, 14}, {15, 15}}}, buildInfo(30, CarparkType::ParkAndRide));
    builder.addPolygon({{{10, 10}, {20, 15}, {15, 15}}}, buildInfo(31, CarparkType::Restricted));
    builder.addPolygon({{{10, 10}, {20, 16}, {15, 15}}}, buildInfo(32, CarparkType::RestrictedBld));
    builder.addPolygon({{{10, 10}, {20, 17}, {15, 15}}}, buildInfo(33, CarparkType::TollBld));

    builder.finalize();

    mr::style2_renderer::Vec3RenderParams params({0, 0, 0});
    params.ftZoomRange = {0, 21};

    mr::style2_renderer::Vec3Renderer renderer;
    auto bytes = renderer.render(createDataSet(builder.path()), params);

    mr::data_set_test_util::matchSchema(
        SCHEMA_PATH,
        mr::data_set_test_util::generateSchema({{bytes.data(), bytes.size()}}));
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::carparks::renderer::tests
