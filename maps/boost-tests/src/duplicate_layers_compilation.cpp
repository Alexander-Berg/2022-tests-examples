#include "tests/boost-tests/include/tools/map_tools.h"

#include <yandex/maps/renderer5/core/Map.h>
#include <yandex/maps/renderer5/core/layers_filter.h>
#include <yandex/maps/renderer5/hotspots/IAttributesHolder.h>
#include <yandex/maps/renderer5/mapcompiler/mapcompiler.h>
#include <boost/test/unit_test.hpp>

using namespace maps::renderer5;
using namespace maps::renderer5::test;

namespace {
const char* DYNAMIC_MAP_NAME = "tests/boost-tests/maps/LayerDuplicatesMap.xml";
const char* STATIC_MAP_NAME = "tmp/LayerDuplicatesStaticMap.xml";
}

BOOST_AUTO_TEST_SUITE(duplicate_layers_compilation)

BOOST_AUTO_TEST_CASE(duplicate_layers_compilation)
{
    map::deleteFilesFromTmpDir();
    core::OperationProgressPtr operationProgress(map::createProgressStub());

    core::IMapGuiPtr mapGui = map::createTestMapGui();
    mapGui->loadFromXml(DYNAMIC_MAP_NAME, false);
    mapGui->open(operationProgress);
    mapcompiler::compile(
        mapGui->map(),
        STATIC_MAP_NAME,
        mapcompiler::Options(),
        mapGui->map().zoomIndexes(),
        mapGui->map().locales(),
        operationProgress);
    mapGui->close();

    mapGui->loadFromXml(STATIC_MAP_NAME, false);
    mapGui->open(operationProgress);

    auto& map = mapGui->map();
    auto layers = map.rootGroupLayer()->getChildrenPtrRecursive(
        *core::createRegExpFilter(L".*/first_layer"));
    BOOST_REQUIRE_EQUAL(layers.size(), 1);
    auto firstLayer = layers.front();

    layers = map.rootGroupLayer()->getChildrenPtrRecursive(
        *core::createRegExpFilter(L".*/second_layer"));
    BOOST_REQUIRE_EQUAL(layers.size(), 1);
    auto secondLayer = layers.front();

    BOOST_REQUIRE(firstLayer->metadata()->has("data"));
    BOOST_CHECK_EQUAL(*firstLayer->metadata()->get("data"), "first");
    auto attrsHolder = firstLayer->cast<hotspots::IAttributesHolder>();
    BOOST_REQUIRE(attrsHolder);
    BOOST_CHECK(!attrsHolder->dataColumns().empty());

    BOOST_REQUIRE(secondLayer->metadata()->has("data"));
    BOOST_CHECK_EQUAL(*secondLayer->metadata()->get("data"), "second");
    attrsHolder = secondLayer->cast<hotspots::IAttributesHolder>();
    BOOST_REQUIRE(attrsHolder);
    BOOST_CHECK(attrsHolder->dataColumns().empty());
}

BOOST_AUTO_TEST_SUITE_END()
