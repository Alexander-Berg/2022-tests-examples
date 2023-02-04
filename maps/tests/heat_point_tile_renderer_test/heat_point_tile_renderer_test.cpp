#include <maps/factory/signals_update_mgr/yt_operations/lib/heat_point_tile_renderer.h>
#include <maps/factory/signals_update_mgr/yt_operations/lib/common.h>

#include <memory>

#define BOOST_TEST_MODULE Heatrenderer
#include <boost/test/unit_test.hpp>

namespace mfs = maps::factory::signalsupdater;

BOOST_AUTO_TEST_CASE(PointTileCreation)
{
    std::shared_ptr<mfs::ITileRenderer> renderer(new mfs::HeatPointTileRenderer(12));

    mfs::GpsSignal gpsSignal(37.54852, 55.7007);

    maps::tile::Tile tile = mfs::signalToTile(gpsSignal, 17);
    renderer->addSignal(gpsSignal, tile);

    auto image = renderer->drawTile();
}
