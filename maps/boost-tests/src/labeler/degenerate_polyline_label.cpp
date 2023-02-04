#include "tests/boost-tests/include/tools/map_tools.h"

#include <yandex/maps/renderer5/core/Map.h>
#include <yandex/maps/renderer5/labeler/label_generator.h>

#include <boost/test/unit_test.hpp>

using namespace maps::renderer5;
using namespace maps::renderer5::labeler;
using namespace maps::renderer5::test;

namespace {
const unsigned int ZOOM_INDEX = 20;
const char* MAP_NAME = "tests/boost-tests/maps/PointAsLineMap.xml";
}

BOOST_AUTO_TEST_SUITE( labeler )

BOOST_AUTO_TEST_CASE( degeneratePolylineLabel )
{
    core::IMapGuiPtr mapGui = test::map::openMap(MAP_NAME);
    auto& map = mapGui->map();
    const auto mapExtent = map.getExtent();
    LabelGenerator lg(map, map.labelableLayers());
    BOOST_CHECK_NO_THROW(lg.placeLabels(
        test::map::createProgressStub(), mapExtent, ZOOM_INDEX));
}

BOOST_AUTO_TEST_SUITE_END()
