
#include "tests/boost-tests/include/tools/map_tools.h"
#include <maps/renderer/libs/base/include/test_util.h>
#include <boost/test/unit_test.hpp>

using namespace maps::renderer;
using namespace maps::renderer5;


/*
 Map:

 .
 |-- G1      (id: 3)
 |   `-- L1  (id: 1)
 `-- G2      (id: 4)

*/
class LayersCacheTest
{
public:
    LayersCacheTest()
    {
        mapGui = test::map::createTestMapGui();
        mapGui->loadFromXml("tests/boost-tests/maps/MapGuiTest.xml", false);
        mapGui->open(test::map::createProgressStub());
    }

    void layerByIdTest()
    {
        auto layer = mapGui->getLayerById(1);
        BOOST_CHECK_EQUAL(layer->name(), L"L1");
        BOOST_CHECK_EQUAL(mapGui->getParentLayerId(1), 3);
        BOOST_CHECK_EQUAL(mapGui->getLayerRowInGroup(1), 0);
    }

    void moveLayerTest()
    {
        mapGui->moveLayerRow(1 /*id*/, 4 /*parentId*/, 0 /*row*/);
        BOOST_CHECK_EQUAL(mapGui->getParentLayerId(1), 4);
    }

    void mapReopenTest()
    {
        mapGui->close();
        mapGui->loadFromXml("tests/boost-tests/maps/MapGuiTest.xml", false);
        mapGui->open(test::map::createProgressStub());
        BOOST_CHECK_EQUAL(mapGui->getParentLayerId(1), 3);
    }

    void layerRemoveTest()
    {
        mapGui->removeLayer(1);
        BOOST_CHECK_THROW(mapGui->getParentLayerId(1), base::Exception);
    }
private:
    core::IMapGuiPtr mapGui;
};

BOOST_AUTO_TEST_CASE(layers_cache_test)
{
    LayersCacheTest tst;
    tst.layerByIdTest();
    tst.layerByIdTest(); // cached
    tst.moveLayerTest();
    tst.mapReopenTest();
    tst.layerRemoveTest();
}
