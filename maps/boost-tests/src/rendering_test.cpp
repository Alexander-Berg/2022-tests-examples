#include "../include/tools.h"
#include "../include/contexts.hpp"

#include <yandex/maps/renderer5/core/TaskQueue.h>
#include <yandex/maps/renderer5/core/box_legacy.h>
#include <yandex/maps/renderer/proj/tile.h>

#include <boost/filesystem.hpp>

using namespace maps::renderer5;
using namespace maps::renderer5::postgres;
using namespace maps::tilerenderer4;
using namespace maps::tilerenderer4::test;
using namespace maps::renderer;

namespace {
// different svg library on win32/lucid/trusty - different reference tiles
#if defined(_WIN32)
#define SYS_EXTENTION "win32"
#elif  YANDEX_MAPS_UBUNTU_VERSION >= 1404
#define SYS_EXTENTION "trusty"
#else
#define SYS_EXTENTION "lucid"
#endif
#define S(s) s

const std::string referenceTileScale05Filename =
    "tests/boost-tests/referenceTiles/three_points.scale0.5." S(SYS_EXTENTION) ".png";
const std::string referenceTileScale1Filename =
    "tests/boost-tests/referenceTiles/three_points.scale1." S(SYS_EXTENTION) ".png";
const std::string referenceTileScale2Filename =
    "tests/boost-tests/referenceTiles/three_points.scale2." S(SYS_EXTENTION) ".png";

const std::string mapXml = "tests/boost-tests/maps/three_points.xml";
const std::string SMALL_TEST_MAP_NAME = "tests/boost-tests/maps/SmallMap.xml";

inline std::string getPngFileName(const std::string& baseFileName, size_t i)
{
    std::stringstream ss;
    ss << baseFileName << "_" << i << ".png";
    return ss.str();
}

void renderTileAndSaveDataAndMask(
    IOnlineRenderer& renderer,
    const xyz& tileCoords,
    PostgresTransactionProviderPtr provider,
    const std::string& dirToSaveTiles)
{
    RenderedTile tile = renderer.render(
        tileCoords.x, tileCoords.y, tileCoords.z, OutputFormatRgbaPng, provider);

    if (tile.outputDataSize)
    {
        std::stringstream filename;
        filename << dirToSaveTiles << "/tiles/" << tileCoords.z << "-" << tileCoords.x << "-" << tileCoords.y << ".png";
        tools::bufferToFile(tile.outputData.get(), tile.outputDataSize, filename.str());
    }

    if (tile.outputDataMaskSize)
    {
        std::stringstream filename;
        filename << dirToSaveTiles << "/masks/" << tileCoords.z << "-" << tileCoords.x << "-" << tileCoords.y << ".png";
        tools::bufferToFile(tile.outputDataMask.get(), tile.outputDataMaskSize, filename.str());
    }
}

class RenderTileTask: public core::Task
{
public:
    std::vector<xyz> toRender;
};

class RenderTileWorker: public core::Worker
{
public:
    RenderTileWorker(
        IOnlineRenderer& renderer,
        postgres::PostgresTransactionProviderPtr provider,
        const std::string dirToSaveTiles)
        : m_renderer(renderer)
        , m_provider(provider)
        , m_dirToSaveTiles(dirToSaveTiles)
    {}

    virtual void doWork(core::Task& task)
    {
        const RenderTileTask& realTask = static_cast<RenderTileTask&>(task);

        for (const xyz tileCoords: realTask.toRender)
            renderTileAndSaveDataAndMask(m_renderer, tileCoords, m_provider, m_dirToSaveTiles);
    }

protected:
    virtual void reportError(const std::unique_ptr<core::Task>& task, const std::wstring& errorMsg)
    {}

protected:
    IOnlineRenderer& m_renderer;
    postgres::PostgresTransactionProviderPtr m_provider;
    const std::string m_dirToSaveTiles;
};

struct TileComparator
{
    TileComparator(): tmpFileName(io::tempFileName()), i(0)
    {
    }

    bool compare(const RenderedTile& tile, agg::rendering_buffer referenceBuffer)
    {
        const std::string pngFileName = getPngFileName(tmpFileName, ++i);

        tools::bufferToFile(
            tile.outputData.get(), tile.outputDataSize,  pngFileName);

        auto tileBuffer = tools::loadPng(pngFileName);

        return tools::isEqualBuffers(tileBuffer, referenceBuffer);
    }

    size_t i;
    const std::string tmpFileName;
};

} // namespace

BOOST_AUTO_TEST_SUITE( Rendering )

BOOST_FIXTURE_TEST_CASE( scale_rendering, OnlineRendererContext<TransactionProviderContext<CleanContext<>>> )
{
    TileComparator tileCmp;

    auto refTileScale1 = tools::loadPng(referenceTileScale1Filename);
    auto refTileScale2 = tools::loadPng(referenceTileScale2Filename);
    auto refTileScale05 = tools::loadPng(referenceTileScale05Filename);

    BOOST_REQUIRE(tools::isEqualBuffers(refTileScale1, refTileScale1));
    BOOST_REQUIRE(tools::isEqualBuffers(refTileScale2, refTileScale2));
    BOOST_REQUIRE(!tools::isEqualBuffers(refTileScale1, refTileScale2));
    BOOST_REQUIRE(!tools::isEqualBuffers(refTileScale1, refTileScale05));

    renderer->open(mapXml);

    int x = 65538;
    int y = 65536;
    unsigned int z = 17;

    {
        ReusableRenderedTile tile;
        BOOST_CHECK_NO_THROW(tile = renderer->render(
            x, y, z, OutputFormatRgbaPng, provider, 1.0));

        BOOST_CHECK(tileCmp.compare(tile, refTileScale1));
    }

    {
        ReusableRenderedTile tile;
        BOOST_CHECK_NO_THROW(tile = renderer->render(
            x, y, z, OutputFormatRgbaPng, provider, 2.0));
        BOOST_CHECK(tileCmp.compare(tile, refTileScale2));
    }

    {
        ReusableRenderedTile tile;
        BOOST_CHECK_NO_THROW(tile = renderer->render(
            x, y, z, OutputFormatRgbaPng, provider, 2.0));
        BOOST_CHECK(tileCmp.compare(tile, refTileScale2));
    }

    {
        ReusableRenderedTile tile;
        BOOST_CHECK_NO_THROW(tile = renderer->render(
            x, y, z, OutputFormatRgbaPng, provider, 2.0));
        BOOST_CHECK(tileCmp.compare(tile, refTileScale2));
    }

    {
        ReusableRenderedTile tile;
        BOOST_CHECK_NO_THROW(tile = renderer->render(
            x, y, z, OutputFormatRgbaPng, provider, 0.5));
        BOOST_CHECK(tileCmp.compare(tile, refTileScale05));
    }
}

BOOST_FIXTURE_TEST_CASE( draw_tiles, OnlineRendererContext<TransactionProviderContext<CleanContext<>>> )
{
    boost::filesystem::create_directories(L"tmp/regular/masks");
    boost::filesystem::create_directories(L"tmp/regular/tiles");
    boost::filesystem::create_directories(L"tmp/shift/masks");
    boost::filesystem::create_directories(L"tmp/shift/tiles");

    renderer->open(SMALL_TEST_MAP_NAME);

    const unsigned int zoom = 17;

    base::BoxD bbox(4168786, 7459130, 4169439, 7459810);

    base::BoxU32 tileBox = proj::mercToTile(bbox, zoom);

    std::vector<xyz> tiles;
    for (unsigned int x = tileBox.x1; x <= tileBox.x2; ++x)
        for (unsigned int y = tileBox.y1; y <= tileBox.y2; ++y)
        {
            const xyz tile = {x, y, zoom};
            tiles.push_back(tile);
        }

    {
        core::TaskQueue tq;
        tq.addWorker(std::unique_ptr<RenderTileWorker>(
            new RenderTileWorker(*renderer, provider,  "tmp/parallel")));

        for (const xyz& tileCoords: tiles) {
            std::unique_ptr<RenderTileTask> task(new RenderTileTask());
            task->toRender.push_back(tileCoords);
            tq.add(std::move(task));
        }

        BOOST_CHECK_NO_THROW(tq.shutdown());
    }

    for (const xyz& tileCoords: tiles)
        renderTileAndSaveDataAndMask(*renderer, tileCoords, provider, "tmp/regular");
}

BOOST_AUTO_TEST_SUITE_END()
