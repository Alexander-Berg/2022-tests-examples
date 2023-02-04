#include "tests/boost-tests/include/tools/map_tools.h"
#include "tests/boost-tests/include/tools/transform_tools.h"

#include <yandex/maps/rapidjsonhelper/rapidjsonhelper.h>
#include <yandex/maps/renderer5/core/Map.h>
#include <yandex/maps/renderer5/core/OperationProgress.h>
#include <yandex/maps/renderer/io/io.h>
#include <yandex/maps/renderer5/hotspots/HotspotsRenderer.h>
#include <yandex/maps/renderer5/hotspots/HotspotsTools.h>
#include <yandex/maps/renderer5/rasterizer/Rasterizer.h>
#include <yandex/maps/renderer/feature/attributes.h>

#include <boost/lexical_cast.hpp>
#include <boost/test/unit_test.hpp>

#include <sstream>

namespace ut = boost::unit_test;

namespace maps {
namespace renderer5 {
namespace hotspots {

using namespace maps::renderer;

namespace
{
const char* LINE_MAP = "tests/boost-tests/maps/HorLineTestMap.xml";
const char* LINE_HOTSPOT_FNAME = "tests/boost-tests/data/line_hotspot.json";

const char* LINE_WITH_EQUIDISTANT_MAP = "tests/boost-tests/maps/HorLineEquidistantTestMap.xml";
const char* LINE_WITH_EQUIDISTANT_HOTSPOT_FNAME = "tests/boost-tests/data/line_with_equidistant_hotspot.json";

const char* LINE_OFFSETTED_OUTSIDE_MAP = "tests/boost-tests/maps/HorLineOffsettedOutsideTestMap.xml";
const char* LINE_OFFSETTED_OUTSIDE_HOTSPOT_FNAME = "tests/boost-tests/data/line_offsetted_outside_hotspot.json";
const char* EMPTY_HOTSPOT_FNAME = "tests/boost-tests/data/empty_hotspot.json";

std::string settingsString(double clipMargin)
{
    std::stringstream ss;
    ss << "{\"enableRenderedGeometry\":true,\"clipMargin\":" << clipMargin << "}";
    return ss.str();
}

class GeneratorMock : public maps::hotspots::base5::IGenerator
{
public:
    GeneratorMock(double clipMargin)
        : m_settings(settingsString(clipMargin))
    {}

    rapidjson::Value generate(
        const rapidjson::Value& source,
        const std::string& /*locale*/,
        rapidjson::Allocator* alloc) const override
    {
        maps::rjhelper::ValueRef ref(source);
        std::string layerKey = ref.GetMember<std::string>("layer_key_for_test");
        std::string id = ref.GetMember<std::string>(feature::SOURCE_ID_ATTR_NAME);
        std::string uid = hotspots::FeatureKeyGenerator(layerKey).generate(std::stol(id));

        rapidjson::Value hsMeta;
        maps::rjhelper::ObjectBuilder b(&hsMeta, alloc);
        b.Put("HotspotMetaData", [&](maps::rjhelper::ObjectBuilder b) {
            b.Put("id", uid);
            b.Put("layer", "jams");
        });
        return hsMeta;
    }

    const std::string& settings(const std::string&) const override
    {
        return m_settings;
    }

private:
    const std::string m_settings;
};

std::string generateHotspots(const std::string& mapName,
                             unsigned int zoom,
                             base::PointD center,
                             double clipMargin = -1.0)
{
    core::IMapGuiPtr mapGui = test::map::createTestMapGui();
    mapGui->loadFromXml(mapName, false);
    core::OperationProgressPtr progress = test::map::createProgressStub();
    mapGui->open(progress);
    core::Map& map = mapGui->map();
    agg::trans_affine mtx = test::mtxFromMerc(center.x, center.y, zoom);
    GeneratorMock generator(clipMargin);

    HotspotSearchRequest request;
    request.locale = "ru_RU";
    request.x = 0; // fictive value
    request.y = 0; // fictive value
    request.zoom = zoom;

    HotspotsRenderer ren(request, &generator);
    rasterizer::Rasterizer rasterizer;
    rasterizer.drawHotspots(ren, progress, 256, 256, mtx, map, nullptr);
    return ren.result();
}
} // anonymous

BOOST_AUTO_TEST_SUITE(hotspot_tests)

BOOST_AUTO_TEST_CASE( hotspots_line_equidistant )
{
    const unsigned int zoom = 17;
    base::PointD center = { 6679347.00444337, 3482186.75847369 };

    // render hotspots with line with and without equidistant
    std::string lineHotspot = generateHotspots(LINE_MAP, zoom, center);
    std::string lineEquidistantHotspot =
        generateHotspots(LINE_WITH_EQUIDISTANT_MAP, zoom, center);

    // load expected results from files (they are too long to hardcode it)
    std::string expectedLineHotspot =
        io::file::open(LINE_HOTSPOT_FNAME).readAllToString();
    std::string expectedLineEquidistantHotspot =
        io::file::open(LINE_WITH_EQUIDISTANT_HOTSPOT_FNAME).readAllToString();

    BOOST_CHECK_NE(lineHotspot, lineEquidistantHotspot);
    BOOST_CHECK_EQUAL(lineHotspot, expectedLineHotspot);
    BOOST_CHECK_EQUAL(lineEquidistantHotspot, expectedLineEquidistantHotspot);

    // render and check clipping equidistant lines
    std::string resultHotspot = generateHotspots(
        LINE_OFFSETTED_OUTSIDE_MAP, zoom, center, 0.0);
    std::string expectedHotspot =
        io::file::open(LINE_OFFSETTED_OUTSIDE_HOTSPOT_FNAME).readAllToString();
    BOOST_CHECK_EQUAL(resultHotspot, expectedHotspot);

    double dy = 6 * proj::pixelPerUnit(zoom);
    resultHotspot = generateHotspots(
        LINE_OFFSETTED_OUTSIDE_MAP, zoom, {center.x, center.y + dy}, 0.0);
    std::string emptyHotspot = io::file::open(EMPTY_HOTSPOT_FNAME).readAllToString();
    BOOST_CHECK_EQUAL(resultHotspot, emptyHotspot);

    dy = 5 * proj::pixelPerUnit(zoom);
    resultHotspot = generateHotspots(
        LINE_OFFSETTED_OUTSIDE_MAP, zoom, {center.x, center.y + dy}, 0.0);
    BOOST_CHECK_NE(resultHotspot, emptyHotspot);
}

BOOST_AUTO_TEST_SUITE_END()


} // namespace hotspots
} // namespace renderer5
} // namespace maps
