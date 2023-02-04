#include "schema_util.h"

#include <maps/renderer/libs/data_sets/geojson_data_set/include/data_set.h>
#include <maps/renderer/libs/vecdata_converters/include/vec3_to_geojson.h>
#include <maps/libs/common/include/file_utils.h>
#include <maps/libs/json/include/builder.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace maps::renderer::data_set_test_util {

namespace {

namespace mj = maps::json;

const std::string GEOM_TYPE = "geometryType";
const std::string PROP = "properties";
const std::string TYPE = "type";
const std::string LAYERS = "layers";
const std::string ENUM = "enum";

} //namespace

void matchSchema(const std::string& sourceSchemaPath,
                 const std::string& generatedSchema)
{
    const std::string sourceSchema = common::readFileToString(sourceSchemaPath);
    const auto source = mj::Value::fromString(sourceSchema)[LAYERS];
    const auto generated = mj::Value::fromString(generatedSchema)[LAYERS];

    EXPECT_THAT(source.fields(), testing::UnorderedElementsAreArray(generated.fields()))
        << "Where source schema is " << sourceSchemaPath
        << " and generated schema represents actual data contents";

    for (const auto& layer : source.fields()) {
        const auto& sourceLayer = source[layer];
        const auto& generatedLayer = generated[layer];

        EXPECT_EQ(sourceLayer[GEOM_TYPE], generatedLayer[GEOM_TYPE])
            << "\""<< layer << "\" geometry type don't match source schema "
            << sourceSchemaPath;

        if (!sourceLayer.hasField(PROP))
            continue;

        ASSERT_TRUE(generatedLayer.hasField(PROP))
            << "Generated schema has no properties for layer \"" << layer << "\""
            << " but some properties are described in source schema " << sourceSchemaPath;

        const auto& sourceProperties = sourceLayer[PROP];
        const auto& generatedProperties = generatedLayer[PROP];

        for (const auto& property : sourceProperties.fields()) {
            ASSERT_TRUE(generatedProperties.hasField(property))
                << "Property \"" << property  << "\" is described in schema "
                << sourceSchemaPath << " but it's missing in actual data";

            ASSERT_EQ(sourceProperties[property][TYPE], generatedProperties[property][TYPE])
               << "\"" << property << "\" type from layer \"" << layer
               << "\" don't match source schema " << sourceSchemaPath;

            if (sourceProperties[property].hasField(ENUM)) {
                ASSERT_TRUE(generatedProperties[property].hasField(ENUM))
                    << "property \"" << property << "\" has enum values in source schema "
                    << sourceSchemaPath << ", but it's missing in generated schema";

                EXPECT_THAT(sourceProperties[property][ENUM].as<std::vector<std::string>>(),
                    testing::UnorderedElementsAreArray(
                        generatedProperties[property][ENUM].as<std::vector<std::string>>()))
                    << "Generated enum values mismatched source schema " << sourceSchemaPath
                    << " for property \"" << property << "\" from layer \"" << layer << "\"";
            }
        }
    }
}


std::string generateSchema(
    const std::vector<std::string>& v3Tiles)
{
    std::unordered_map<std::string, mj::Value> schemaLayers;

    for (const auto& tile : v3Tiles) {
        const auto geoJsonTile = mj::Value::fromString(
            vecdata_converters::convertToGeojson(
                {0, 0, 0}, // tile id affect the features geometry coordinates only
                tile));

        for (const auto& geoJsonlayer : geoJsonTile[LAYERS]) {
            const auto layerName = geoJsonlayer["name"].as<std::string>();

            const auto generatedSchema = mj::Value::fromString(
                geojson_data_set::dataSetSchema(mj::Builder() << geoJsonlayer));

            auto layerIt = schemaLayers.find(layerName);

            // assuming set of atributes the same for every features in layer
            if (layerIt == schemaLayers.end()) {
                schemaLayers.emplace(layerName, generatedSchema);
                continue;
            }
            const auto& layerSchema = layerIt->second;

            REQUIRE(layerSchema[GEOM_TYPE] == generatedSchema[GEOM_TYPE],
                "Tiles has different geometry type for layer " + layerName);
        }
    }

    mj::Builder builder;
    builder << [&](mj::ObjectBuilder schemaBuilder) {
        schemaBuilder[LAYERS] = [&](mj::ObjectBuilder layerBuilder) {
            for (const auto& [layerName, layerSchema] : schemaLayers)
                layerBuilder[layerName] = layerSchema[LAYERS]["features"];
        };
    };

    return builder.str();
}

} // namespace maps::renderer::data_set_test_util
