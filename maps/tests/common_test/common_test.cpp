#include <maps/factory/signals_update_mgr/yt_operations/lib/common.h>

#include <string>
#include <fstream>

#define BOOST_TEST_MODULE Common
#include <boost/test/unit_test.hpp>

namespace mt = maps::tile;
namespace mg = maps::geolib3;
namespace mfs = maps::factory::signalsupdater;

const double EPS = 1e-6;

BOOST_AUTO_TEST_CASE(mercatorPoint_test)
{
    mfs::GpsSignal gpsSignal(55.0, 37.0);

    mg::Point2 point = gpsSignal.mercPoint();
    BOOST_CHECK_CLOSE(6122571.993630046, point.x(), EPS);
    BOOST_CHECK_CLOSE(4413389.8886857564, point.y(), EPS);
}

BOOST_AUTO_TEST_CASE(signalToTile_test)
{
    mfs::GpsSignal gpsSignal(37.00, 55.00);

    maps::tile::Tile tile12 = mfs::signalToTile(gpsSignal, 12);
    BOOST_CHECK_EQUAL(2468, tile12.x());
    BOOST_CHECK_EQUAL(1299, tile12.y());
    BOOST_CHECK_EQUAL(12, tile12.z());

    maps::tile::Tile tile17 = mfs::signalToTile(gpsSignal, 17);
    BOOST_CHECK_EQUAL(79007, tile17.x());
    BOOST_CHECK_EQUAL(41572, tile17.y());
    BOOST_CHECK_EQUAL(17, tile17.z());
}

BOOST_AUTO_TEST_CASE(point2Pixel_test)
{
    mfs::GpsSignal gpsSignal(37.54852, 55.7007);

    maps::tile::Tile tile = mfs::signalToTile(gpsSignal, 17);
    maps::factory::signalsupdater::PixelCoord
        pixel = maps::factory::signalsupdater::point2pixel(mg::Point2(37.54852, 55.7007), tile);

    BOOST_CHECK_EQUAL(pixel.x, 255);
    BOOST_CHECK_EQUAL(pixel.y, 156);
}
