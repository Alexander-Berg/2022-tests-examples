#include <maps/factory/signals_update_mgr/yt_operations/lib/common.h>
#include <maps/factory/signals_update_mgr/yt_operations/lib/arrow_tile_renderer.h>

#include <memory>

#define BOOST_TEST_MODULE Arrowrenderer
#include <boost/test/unit_test.hpp>

namespace mfs = maps::factory::signalsupdater;

BOOST_AUTO_TEST_CASE(ArrowTileCreation)
{
    std::shared_ptr<mfs::ITileRenderer> renderer(new mfs::ArrowTileRenderer(mfs::OneColor::No, 12));

    mfs::GpsSignal gpsSignal(37.54852, 55.7007);

    maps::tile::Tile tile = mfs::signalToTile(gpsSignal, 17);
    for (size_t i = 0; i <= 30; i++) {
        renderer->addSignal(gpsSignal, tile);
    }

    auto image = renderer->drawTile();
}
