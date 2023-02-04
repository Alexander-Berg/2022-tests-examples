#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/detection/tests/config_template.h>

#include <maps/libs/common/include/exception.h>

#include <string>
#include <fstream>

namespace maps::wiki::autocart::pipeline::tests {

namespace {

static const std::string TILE_SOURCE_URL_TEMPLATE = "{{ TILE_SOURCE_URL }}";

} // namespace

json::Value makeConfigFromTemplate(
    const std::string& configTemplatePath, const std::string& tileSourceUrl)
{
    std::ifstream ifs(configTemplatePath);
    REQUIRE(ifs.is_open(), "Failed to open file: " + configTemplatePath);
    std::string configTemplate(std::istreambuf_iterator<char>(ifs), {});
    ifs.close();

    size_t pos = configTemplate.find(TILE_SOURCE_URL_TEMPLATE);
    REQUIRE(pos != std::string::npos, "Invalid config template");
    configTemplate.replace(pos, TILE_SOURCE_URL_TEMPLATE.size(), tileSourceUrl);

    return json::Value::fromString(configTemplate);
}

} // namespace maps::wiki::autocart::pipeline::tests
