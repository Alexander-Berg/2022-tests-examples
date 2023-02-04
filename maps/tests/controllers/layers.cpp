#include <maps/wikimap/mapspro/services/editor/src/configs/config.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/formatter.h>
#include <maps/wikimap/mapspro/services/editor/src/utils.h>

#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>

#include <maps/libs/common/include/file_utils.h>
#include <maps/libs/json/include/prettify.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace maps::wiki::tests {

namespace {

std::string
prettify(const std::string& value, common::FormatType formatType)
{
    switch (formatType) {
        case common::FormatType::JSON:
            return json::prettifyJson(value) + '\n';

        case common::FormatType::XML:
            return prettifyXml(value);

        default:
            return value;
    }
}

std::string fileExtention(common::FormatType formatType)
{
    auto ext = boost::lexical_cast<std::string>(formatType);
    std::transform(ext.begin(), ext.end(), ext.begin(), ::tolower);
    return ext;
}

} // namespace

Y_UNIT_TEST_SUITE(layers)
{

WIKI_FIXTURE_TEST_CASE(test_renderer_sublayers, EditorTestFixture)
{
    log8::setLevel(log8::Level::FATAL);
    cfg()->onLoad();
    for (auto project2formatType : std::map<std::string, common::FormatType>{
            {"mpro",  common::FormatType::XML},
            {"nmaps", common::FormatType::JSON} } ) {

        const auto& projectName = project2formatType.first;
        const auto  formatType  = project2formatType.second;

        auto filePath =  SRC_("../data/renderer_sublayers_orig.") +
            fileExtention(formatType);

        GetLayers::Request request{0, "skl", projectName, {}};
        GetLayers controller(request);
        auto controllerResult = controller();
        auto formatter = Formatter::create(formatType);
        auto prettyData = prettify((*formatter)(*controllerResult), formatType);

        // uncomment to update data files
        //maps::common::writeFile(filePath, prettyData);
        //
        //ASSERT_EQ(
        //    prettyData,
        //    maps::common::readFileToString(filePath))
        //        << "yandex-maps-wiki-mapspro-layers changed? (" << formatType << ")";
    }
}

namespace {

std::string testFilePath(const GetLayers2::Request& request, common::FormatType formatType)
{
    auto filePath = SRC_("../data/renderer_sublayers2_") +
        request.project + "_" + request.mapType +
        "." + fileExtention(formatType);
    return filePath;
}

} // namespace

WIKI_FIXTURE_TEST_CASE(test_renderer_sublayers2, EditorTestFixture)
{
    log8::setLevel(log8::Level::FATAL);
    cfg()->onLoad();
    const std::vector<std::pair<std::string, std::string>> projectAndMapTypes = {
        {"nmaps", "skl"},
        {"nmaps", "map"}
    };
    for (auto projectAndMapType : projectAndMapTypes) {
        const auto formatType = common::FormatType::JSON;
        const auto project = projectAndMapType.first;
        const auto mapType = projectAndMapType.second;

        GetLayers2::Request request{0, mapType, project, {}};
        GetLayers2 controller(request);
        auto controllerResult = controller();
        auto formatter = Formatter::create(formatType);
        auto prettyData = prettify((*formatter)(*controllerResult), formatType);

        auto filePath = testFilePath(request, formatType);

        // uncomment to update data files
        //maps::common::writeFile(filePath, prettyData);
        //
        //ASSERT_EQ(
        //    prettyData,
        //    maps::common::readFileToString(filePath))
        //        << "yandex-maps-wiki-mapspro-layers2 changed? (" << formatType << ")";
    }
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
