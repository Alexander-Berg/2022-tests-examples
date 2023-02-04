#include <library/cpp/testing/unittest/registar.h>

#include <maps/mobile/server/init/lib/common.h>
#include <maps/mobile/server/init/lib/experimental_config.h>
#include <maps/mobile/server/init/lib/map_layers_experiment.h>
#include <yandex/maps/proto/mobile_config/mapkit2/map.pb.h>

Y_UNIT_TEST_SUITE(Layers) {

Y_UNIT_TEST(ConfigExperiment)
{
    Settings settings;
    namespace map_layers =
        yandex::maps::proto::mobile_config::mapkit2::map;
    map_layers::Config* config =
        settings.MutableExtension(map_layers::config);

    auto layer = config->add_layer();
    *layer->mutable_name() = "map";
    layer->mutable_version()->mutable_fixed()->set_value("123");

    layer = config->add_layer();
    *layer->mutable_name() = "abc";
    layer->mutable_version()->mutable_fixed()->set_value("123");

    Settings experimentSettings =
        experimentalConfigSettings(parseExperimentalConfigParams(
            "MAPS_RENDERER:experimental_map_design=456"));
    settings.appendSettings(experimentSettings);

    std::string mapExperimentVersion =
        getExperimentValue(settings, "MAPS_RENDERER", "experimental_map_design");
    UNIT_ASSERT(!mapExperimentVersion.empty());

    patchLayersVersionForExperiment(mapExperimentVersion, &settings);

    UNIT_ASSERT(config->layer(0).version().fixed().value() == "123~~456");
    UNIT_ASSERT(config->layer(1).version().fixed().value() == "123");
}

} // Y_UNIT_TEST_SUITE(Layers)
