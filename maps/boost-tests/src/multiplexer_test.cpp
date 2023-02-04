#include "../include/tools.h"
#include "../include/contexts.hpp"
#include "../../../online_renderer.h"

#include <yandex/maps/renderer/io/io.h>

#include <iostream>
#include <fstream>
#include <streambuf>

using namespace maps::renderer5;
using namespace maps::renderer5::postgres;

namespace maps { namespace tilerenderer4 { namespace test {

namespace {

const std::string referenceTile11 =
    "tests/boost-tests/referenceTiles/stations_z1_zdata1.png";
const std::string referenceTile12 =
    "tests/boost-tests/referenceTiles/stations_z1_zdata2.png";
const std::string referenceTile22 =
    "tests/boost-tests/referenceTiles/stations_z2_zdata2.png";

const std::string mapXml = "tests/boost-tests/maps/stations.xml";

inline std::string getPngFileName(const std::string& baseFileName, size_t i)
{
    std::stringstream ss;
    ss << baseFileName << "_" << i << ".png";
    return ss.str();
}

struct TileComparator
{
    TileComparator() :
        tmpFileName(renderer::io::tempFileName()),
        i(0)
    {
    }

    std::string loadJSON(const std::string& fname)
    {
        std::ifstream fin(fname);
        return std::string((std::istreambuf_iterator<char>(fin)),
            std::istreambuf_iterator<char>());
    }

    bool compare(const RenderedTile& tile, agg::rendering_buffer referenceBuffer)
    {
        const std::string pngFileName = getPngFileName(tmpFileName, ++i);

        tools::bufferToFile(
            tile.outputData.get(), tile.outputDataSize,  pngFileName);

        auto tileBuffer = tools::loadPng(pngFileName);

        return tools::isEqualBuffers(tileBuffer, referenceBuffer);
    }

    bool compare(const std::string& s1, const std::string& s2)
    {
        return s1.compare(s2) == 0;
    }

    size_t i;
    const std::string tmpFileName;
};

} // namespace

BOOST_AUTO_TEST_SUITE( Multiplexer )

BOOST_FIXTURE_TEST_CASE( png_render, OnlineRendererContext<TransactionProviderContext<CleanContext<>>> )
{
    return; // FIXME: update stations.xml to modern format

    TileComparator tileCmp;

    auto refTile11 = tools::loadPng(referenceTile11);
    auto refTile12 = tools::loadPng(referenceTile12);
    auto refTile22 = tools::loadPng(referenceTile22);

    BOOST_REQUIRE(tools::isEqualBuffers(refTile11, refTile11));
    BOOST_REQUIRE(tools::isEqualBuffers(refTile12, refTile12));
    BOOST_REQUIRE(!tools::isEqualBuffers(refTile11, refTile12));
    BOOST_REQUIRE(!tools::isEqualBuffers(refTile22, refTile12));

    renderer->open(mapXml);

    {
        ReusableRenderedTile tile;
        auto stack = renderer->createRasterizerStack(1.0);

        BOOST_CHECK_NO_THROW(tile
            = renderer->render2(1, 0, 1, 1, OutputFormatRgbaPng, stack));

        BOOST_CHECK(tileCmp.compare(tile, refTile11));
    }

    {
        ReusableRenderedTile tile;
        auto stack = renderer->createRasterizerStack(1.0);

        BOOST_CHECK_NO_THROW(tile
            = renderer->render2(1, 0, 1, 2, OutputFormatRgbaPng, stack));

        BOOST_CHECK(tileCmp.compare(tile, refTile12));
    }

    {
        ReusableRenderedTile tile;
        auto stack = renderer->createRasterizerStack(1.0);
        BOOST_CHECK_NO_THROW(tile
            = renderer->render2(2, 0, 2, 2, OutputFormatRgbaPng, stack));

        BOOST_CHECK(tileCmp.compare(tile, refTile22));
    }
}

BOOST_AUTO_TEST_SUITE_END()

}}} // maps::tilerenderer4::test
