#pragma once

#include <yandex/maps/renderer5/core/Map.h>

#include <boost/test/unit_test.hpp>

using namespace maps::renderer5;
using namespace boost::unit_test;

namespace maps { namespace renderer5 { namespace test { namespace map
{
    void sortedLayersTest();

    void linkedWithLayersTest();

    void dependentLayersTest();

    void getParentLayerIdTest();

    void getChildLayerIdByRowTest();

    void getLayerRowInGroupTest();

    void getChildLayersCountTest();

    void moveLayerRowTest();

    void copyLayerTest();

    void createGroupLayerTest();

    // test on task: MAPSCORE-2828
    void annotateLayerInOpenedMapTest();

    // test on task: MAPSCORE-2846
    void copyGroupLayerAndCheckOpenState();

    test_suite * initMapLayersAccessSuite();

} } } } // namespace maps::renderer5::test::map
