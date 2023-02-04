#include "tests/boost-tests/include/tools/map_tools.h"
#include "tests/boost-tests/include/tools/transform_tools.h"
#include "tests/boost-tests/include/hotspot_tools.h"

#include <yandex/maps/renderer5/core/Map.h>
#include <yandex/maps/renderer5/core/MultiLayersFilter.h>
#include <yandex/maps/renderer5/core/OperationProgress.h>
#include <yandex/maps/renderer5/hotspots/HotspotsRenderer.h>
#include <yandex/maps/renderer5/rasterizer/Rasterizer.h>
#include <yandex/maps/renderer/io/io.h>
#include <yandex/maps/renderer/proj/tile.h>

#include <boost/lexical_cast.hpp>
#include <boost/test/unit_test.hpp>

#include <iostream>
#include <fstream>
#include <string>
#include <stdlib.h>
#include <vector>

namespace hotspots = maps::renderer5::hotspots;
namespace core = maps::renderer5::core;
namespace rasterizer = maps::renderer5::rasterizer;
namespace test = maps::renderer5::test;

using namespace maps::renderer;

const int TILE_WIDTH = 1 << 15;
const int UNITS_IN_DIP = TILE_WIDTH / 256;

agg::trans_affine
mercatorToTile256(unsigned int x, unsigned int y, unsigned int zoom)
{
    auto scale = proj::pixelPerUnit(zoom);
    auto bbox = proj::tileToMerc({x, y}, zoom);

    agg::trans_affine mtx;
    mtx.translate(-bbox.x1, -bbox.y1);
    mtx.scale(scale);

    return mtx;
}

namespace {

const char* MapName   = "tests/boost-tests/maps/vector/day_map.xml";

std::string getFileName(unsigned int scale)
{
    std::stringstream fileName;
    fileName.setf(std::ios::fixed, std:: ios::floatfield);
    fileName << "tests/boost-tests/referenceTiles/tile_" << scale << ".json";
    return fileName.str();
}

core::IMapGuiPtr createGUI(const char* mapName)
{
    core::IMapGuiPtr mapGui = test::map::createTestMapGui();
    mapGui->loadFromXml(mapName, false);
    core::OperationProgressPtr progress = test::map::createProgressStub();
    mapGui->open(progress);
    return mapGui;
}

agg::trans_affine mercatorToTile32k(const agg::trans_affine& mcToTile256)
{
    auto result = mcToTile256;
    result.translate(-128, -128);
    result.scale(UNITS_IN_DIP, -UNITS_IN_DIP);
    return result;
}

} // anonymous

BOOST_AUTO_TEST_SUITE( hotspot_tests )

BOOST_AUTO_TEST_SUITE( hotspots_zoom_test )

BOOST_AUTO_TEST_CASE( zoom_test )
{
    auto gui = createGUI(MapName);
    test::GeneratorMock generator("");

    unsigned zmin = 15;
    unsigned zmax = 16;

    for (unsigned zoom = zmin; zoom <= zmax; zoom++) {
        hotspots::HotspotSearchRequest request = { 0, 0, zmin, "", zoom };

        hotspots::HotspotsRenderer renderer(request, &generator);

        renderer.setClipBox(TILE_WIDTH << (zoom - zmin), TILE_WIDTH << (zoom - zmin));

        auto mcToTile = mercatorToTile32k(mercatorToTile256(0, 0, 0));

        mcToTile.scale(1, -1);
        mcToTile.translate(TILE_WIDTH / 2, TILE_WIDTH / 2);

        rasterizer::Rasterizer rast;
        rast.drawHotspots(renderer, test::map::createProgressStub(),
            TILE_WIDTH, TILE_WIDTH, mcToTile, gui->map(), nullptr, zoom);

        auto reference = io::file::open(getFileName(zoom)).readAllToString();
        BOOST_CHECK_EQUAL(reference, renderer.result());
    }
}

BOOST_AUTO_TEST_SUITE_END()
BOOST_AUTO_TEST_SUITE_END()
