#include <library/cpp/testing/unittest/registar.h>

#include <maps/mobile/server/init/lib/common.h>
#include <maps/mobile/server/init/lib/experimental_config.h>
#include <maps/mobile/server/init/lib/fonts_experiment.h>
#include <yandex/maps/proto/mobile_config/mapkit2/fonts.pb.h>

Y_UNIT_TEST_SUITE(Fonts)
{

Y_UNIT_TEST(ConfigExperiment)
{
    Settings settings;
    namespace proto_fonts = yandex::maps::proto::mobile_config::mapkit2::fonts;
    proto_fonts::Config* config = settings.MutableExtension(proto_fonts::config);
    config->set_version("123");

    Settings experimentSettings = experimentalConfigSettings(
        parseExperimentalConfigParams("MAPS_RENDERER:experimental_fonts=456"));
    settings.appendSettings(experimentSettings);

    std::string fontsExperimentVersion = getExperimentValue(settings, "MAPS_RENDERER", "experimental_fonts");
    if (!fontsExperimentVersion.empty()) {
        patchFontsVersionForExperiment(fontsExperimentVersion, &settings);
    }
    UNIT_ASSERT(config->version() == "123-e456");
}

} // Y_UNIT_TEST_SUITE
