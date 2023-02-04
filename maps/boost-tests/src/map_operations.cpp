#include "../include/tools.h"
#include "../include/contexts.hpp"

#include <yandex/maps/tilerenderer4/IOnlineRenderer.h>

#include <boost/test/unit_test.hpp>

using namespace maps::renderer5;
using namespace maps::tilerenderer4;

namespace {
const std::string SMALL_TEST_MAP_NAME = "tests/boost-tests/maps/SmallMap.xml";
const std::string MISSING_MAP_XML_FILENAME = "tests/boost-tests/maps/Map-That-Does-Not-Exist.xml";
}

BOOST_AUTO_TEST_SUITE( OnlineRenderer )
BOOST_AUTO_TEST_SUITE( map_operations )

BOOST_FIXTURE_TEST_CASE( open, OnlineRendererContext<> )
{
    BOOST_CHECK_NO_THROW(renderer->open(SMALL_TEST_MAP_NAME));
    BOOST_CHECK_NO_THROW(renderer->close());
    BOOST_CHECK_NO_THROW(renderer.reset());
}

BOOST_FIXTURE_TEST_CASE( openMissingMap, OnlineRendererContext<> )
{
    BOOST_CHECK_THROW(renderer->open(MISSING_MAP_XML_FILENAME), std::exception);
    BOOST_CHECK_NO_THROW(renderer->close());
    BOOST_CHECK_NO_THROW(renderer.reset());
}

BOOST_AUTO_TEST_SUITE_END()
BOOST_AUTO_TEST_SUITE_END()
