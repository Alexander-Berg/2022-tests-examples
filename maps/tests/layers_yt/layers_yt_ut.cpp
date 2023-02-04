#include <library/cpp/testing/unittest/registar.h>

#include <maps/renderer/libs/data_sets/data_set/include/special_attributes.h>
#include <maps/renderer/libs/data_sets/yt_data_set/impl/yql_query.h>
#include <maps/renderer/libs/data_sets/yt_data_set/impl/yql_query_processor.h>
#include <maps/renderer/libs/data_sets/yt_data_set/impl/yt_utils.h>
#include <maps/renderer/libs/data_sets/yt_data_set/include/source_layer.h>
#include <maps/libs/log8/include/log8.h>
#include <maps/libs/common/include/file_utils.h>
#include <maps/renderer/libs/base/include/zoom_range.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/yson/node/node.h>
#include <library/cpp/yson/node/node_io.h>
#include <util/generic/is_in.h>

#include <contrib/libs/yaml-cpp/include/yaml-cpp/yaml.h>

namespace maps::garden::modules::renderer_denormalization_source_config {

namespace data_set = renderer::yt_data_set;
namespace attr_conv = renderer::data_set::special_attributes::convention;

namespace {

void testLayers(const std::string& gardenEnv)
{
    const auto pathPrefix = "maps/garden/modules/renderer_denormalization_source_config";
    const auto layersPath = common::joinPath(
        BuildRoot(), pathPrefix, "data/layers_yt.yaml");
    const auto schemaPath = common::joinPath(
        BuildRoot(), pathPrefix, "tests/layers_yt/schema_" + gardenEnv + "/schema.json");

    auto layers = data_set::parseSourceLayers(YAML::LoadFile(layersPath));
    const auto schemas = NYT::NodeFromJsonString(
        common::readFileToString(schemaPath))["schema"];

    if (gardenEnv == "production") {
        EraseIf(layers, [](const auto& layer) { return layer.id.starts_with("hd_"); });
    }

    for (const auto& layer: layers) {
        INFO() << "checking layer " << layer.id;

        NYT::TTableSchema schema;
        Deserialize(schema, schemas.At(layer.table).At("schema"));

        if (!layer.featureId &&
            !IsIn(data_set::ytSchemaColumnNames(schema), attr_conv::FEATURE_ID)) {
            schema.AddColumn(NYT::TColumnSchema()
                .Name(TString{attr_conv::FEATURE_ID})
                .TypeV3(NTi::Uint64()));
        }

        renderer::base::ZoomRange zoomRange{layer.minzoom, renderer::base::MAX_ZOOM};

        auto query = data_set::buildYqlQuery(layer, schema, zoomRange);
        data_set::YqlQueryProcessor proc{query, schema};  // checking query syntax

        auto columnNames = data_set::ytSchemaColumnNames(proc.outputSchema());

        EXPECT_TRUE(IsIn(columnNames, layer.geometryColumn));
        if (layer.featureId) {
            EXPECT_TRUE(IsIn(columnNames, *layer.featureId));
        }
        if (layer.zlevel) {
            EXPECT_TRUE(IsIn(columnNames, layer.zlevel->first));
            EXPECT_TRUE(IsIn(columnNames, layer.zlevel->second));
        }
        if (layer.mesh) {
            EXPECT_TRUE(IsIn(columnNames, *layer.mesh));
        }
        for (const auto& name: layer.properties) {
            EXPECT_TRUE(IsIn(columnNames, name))
                << "column " << name << " not found in layer " << layer.id;
        }
    }
}

} // namespace

Y_UNIT_TEST_SUITE(layers_yt_tests) {

Y_UNIT_TEST(datatesting_test)
{
    testLayers("datatesting");
}

Y_UNIT_TEST(production_test)
{
    testLayers("production");
}

} // layers_yt_tests

} // namespace maps::garden::modules::renderer_denormalization_source_config
