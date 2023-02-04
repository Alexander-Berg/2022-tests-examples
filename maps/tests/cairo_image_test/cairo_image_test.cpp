#include <maps/factory/signals_update_mgr/yt_operations/lib/cairo_image.h>

#define BOOST_TEST_MODULE Cairoimage
#include <boost/test/unit_test.hpp>

namespace mfs = maps::factory::signalsupdater;

BOOST_AUTO_TEST_CASE(CairoArrow)
{
    mfs::CairoImage img(256, 256);

    img.arrow(mfs::CairoPixel(20, 20), 14, 3.1415926, 2, mfs::CairoColor(1, 0, 0, 1),
        mfs::CairoColor(1, 1, 1, 1));

    auto resultPng = img.getPng();
}

BOOST_AUTO_TEST_CASE(CairoRectangle)
{
    mfs::CairoImage img(256, 256);

    img.rectangle(mfs::CairoPixel(10, 10), 1, 1, mfs::CairoColor(1, 1, 1, 1));

    auto resultPng = img.getPng();
}
