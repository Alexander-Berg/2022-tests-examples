#include "../include/tools.h"
#include "../include/contexts.hpp"

#include <yandex/maps/tilerenderer4/IOnlineRenderer.h>
#include <yandex/maps/renderer5/postgres/PostgresTools.h>
#include <yandex/maps/renderer/io/io.h>
#include <yandex/maps/renderer5/core/box_legacy.h>
#include <yandex/maps/renderer/proj/tile.h>
#include <maps/renderer/libs/base/include/string_convert.h>

#include <boost/filesystem.hpp>

#include <cstdlib>

using namespace maps;
using namespace maps::renderer;
using namespace maps::renderer5;
using namespace maps::renderer5::postgres;
using namespace maps::renderer5::labeler;
using namespace maps::tilerenderer4;
using namespace maps::tilerenderer4::test;

namespace {
const std::string SMALL_TEST_MAP_NAME = "tests/boost-tests/maps/SmallMap.xml";

inline double rand01()
{
    return std::rand() / static_cast<double>(RAND_MAX);
}

template <typename T>
T rand(T minValue, T maxValue)
{
    // maybe this check is redundant and leads to potential bugs?
    if (minValue > maxValue) {
        return rand(maxValue, minValue);
    }

    return static_cast<T>(minValue + (maxValue - minValue) * rand01());
}

void getRandomTile(
    maps::renderer5::core::BoundingBox bb,
    unsigned int startZoom, unsigned int numTiles, std::vector<unsigned int> offsets,
    unsigned int& x, unsigned int& y, unsigned int& z)
{
    unsigned int i = rand<unsigned>(1, numTiles - 1);
    unsigned int curZ = 0;
    while (offsets[curZ] < i)
        ++curZ;
    --curZ;

    unsigned int numTile = i - offsets[curZ];

    core::BoxD box = core::boxFromCoreBBox(bb);
    z = curZ + startZoom;
    base::BoxU32 tileBox = proj::mercToTile(box, z);

    y = tileBox.y1 + numTile / (tileBox.y2 - tileBox.y1);
    x = tileBox.x1 + numTile % (tileBox.x2 - tileBox.x1);
}
}

BOOST_AUTO_TEST_SUITE( OnlineRenderer )

BOOST_FIXTURE_TEST_CASE( pfc_params, OnlineRendererContext<TransactionProviderContext<>> )
{
    boost::filesystem::remove_all("tmp/regular/pfcParams");
    boost::filesystem::create_directories("tmp/regular/pfcParams");

    renderer->open(SMALL_TEST_MAP_NAME);

    xyz tile;
    tile.x = 39585;
    tile.y = 20569;
    tile.z = 16;

    maps::renderer5::postgres::PfcParams params;
    params.layerId = 1;
    params.sourceTableName = "renderer_autotest.wiki_streets";
    params.filterClause = "id <> 3800140 AND id <> 3800139 AND id <> 3800142";

    maps::renderer5::postgres::PfcParams oldParams = renderer->setPfcParams(params);

    maps::tilerenderer4::RenderedTile tileRegular;
    BOOST_CHECK_NO_THROW(tileRegular = renderer->render(tile.x, tile.y, tile.z, maps::tilerenderer4::OutputFormatRgbaPng, provider));

    if (tileRegular.outputDataSize)
    {
        std::stringstream filename;
        filename << "tmp/regular/pfcParams/" << tileRegular.zoom << "-" << tileRegular.x << "-" << tileRegular.y << ".png";
        tools::bufferToFile(tileRegular.outputData.get(), tileRegular.outputDataSize, filename.str());
    }

    std::string newParamsFullTableName =
        base::ws2s(
            postgres::PostgresTools::getFullTableName(
                base::s2ws(
                    params.sourceTableName)));

    BOOST_CHECK(oldParams.sourceTableName == newParamsFullTableName);
    BOOST_CHECK(oldParams.layerId == params.layerId);
    BOOST_CHECK(oldParams.filterClause != params.filterClause);
}

BOOST_AUTO_TEST_SUITE_END()
