#include <maps/renderer/libs/base/include/compiler.h>

#ifdef REN_PLATFORM_LINUX
#include "tests/boost-tests/include/tools/map_tools.h"

#include <yandex/maps/renderer5/hotspots/HotspotsRenderer.h>
#include <yandex/maps/renderer5/hotspots/i_hotspots_provider.h>
#include <yandex/maps/renderer5/mapcompiler/mapcompiler.h>
#include <yandex/maps/renderer5/rasterizer/Rasterizer.h>
#include <yandex/maps/renderer5/core/Map.h>

#include <yandex/maps/hotspots-base5/generator_base.h>
#include <yandex/maps/renderer/proj/mercator.h>
#include <yandex/maps/renderer/proj/tile.h>

#include <boost/test/unit_test.hpp>
#include <sstream>
#include <fstream>

namespace hb5 = maps::hotspots::base5;

namespace maps {
namespace renderer5 {
namespace hotspots {

using namespace maps::renderer;

namespace {
const char* DYNAMIC_MAP = "tests/boost-tests/maps/HotspotsFullTestMap.xml";
const char* STATIC_MAP = "tmp/StaticHotspotsMap.xml";
const char* RESULT_HOTSPOT_FNAME = "tests/boost-tests/data/hotspots_full_test_result.json";

core::IMapGuiPtr compileMap(
    const std::string& dynamicMapName,
    const std::string& staticMapName,
    unsigned int zoomIndex)
{
    core::IMapGuiPtr dynamicMap = test::map::openMap(dynamicMapName);

    std::set<unsigned int> zoomIndices;
    zoomIndices.insert(zoomIndex);

    mapcompiler::Options options;
    options.importAttributes = true;
    BOOST_REQUIRE(options.attributesStorage == mapcompiler::Options::MMS);

    mapcompiler::compile(
        dynamicMap->map(), staticMapName,
        options, zoomIndices, {}, test::map::createProgressStub());

    return test::map::openMap(staticMapName);
}

class GeneratorMock : public hb5::GeneratorBase
{
public:
    virtual ~GeneratorMock() {}

    GeneratorMock()
    {
        init("{\"layerName\":\"test\", \"enableRenderedGeometry\":true}");
    }

protected:
    std::string extractHintContent(
        const rapidjson::Value& source,
        const maps::Locale&) const override
    {
        return source["data"].GetString();
    }
};

agg::trans_affine createTransform(const HotspotSearchRequest& request)
{
    agg::trans_affine mtx;

    double scale = proj::pixelPerUnit(request.zoom);
    double halfWidth  = proj::TILE_SIZE / 2.0;
    double halfHeight = proj::TILE_SIZE / 2.0;

    auto bbox = proj::tileToMerc({0, 0}, request.zoom);

    mtx.translate(-(bbox.x1 + bbox.x2) / 2.0, -(bbox.y1 + bbox.y2) / 2.0);
    mtx.scale(scale);
    mtx.translate(
        halfWidth - request.x * proj::TILE_SIZE,
        halfHeight + request.y * proj::TILE_SIZE);

    return mtx;
}

std::string renderHotspots(core::Map& map,
                           const HotspotSearchRequest& request)
{
    BOOST_REQUIRE(hasHotspotsData(map));
    GeneratorMock generator;

    HotspotsRenderer renderer(request, &generator);

    rasterizer::Rasterizer rast;
    agg::trans_affine mtx = createTransform(request);
    rast.drawHotspots(renderer, test::map::createProgressStub(),
        proj::TILE_SIZE, proj::TILE_SIZE, mtx, map, nullptr);

    return renderer.result();
}
}

BOOST_AUTO_TEST_SUITE( hotspot_tests )

BOOST_AUTO_TEST_CASE( hotspots_full_test_mms )
{
    test::map::deleteFilesFromTmpDir();

    const unsigned int zoomIndex = 13;
    core::IMapGuiPtr staticMapGui = compileMap(
        DYNAMIC_MAP, STATIC_MAP, zoomIndex);

    auto& map = staticMapGui->map();

    HotspotSearchRequest request = { 5461, 3384, zoomIndex };
    auto result = renderHotspots(map, request);

    auto expectedResult = io::file::open(RESULT_HOTSPOT_FNAME).readAllToString();
    BOOST_CHECK_EQUAL(result, expectedResult);
}

BOOST_AUTO_TEST_SUITE_END()

} // namespace hotspots
} // namespace renderer5
} // namespace maps

#endif // REN_PLATFORM_LINUX
