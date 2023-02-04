#include "../include/MapLayersAccessTest.h"
#include "tests/boost-tests/include/tools/map_tools.h"

using namespace boost::unit_test;
using namespace maps::renderer;
using namespace maps::renderer5;
using namespace maps::renderer5::test;

namespace
{
    const char* sourceFileName = "tests/boost-tests/data/TwoPlineTest.mif";
    const char* layersAccessTestXmlFileName = "tests/boost-tests/maps/MapLayersAccessTest.xml";
    const char* sortedLayersTestXmlFileName = "tests/boost-tests/maps/MapSortedLayersTest.xml";
    const char* linkedLayersWithTestXmlFileName = layersAccessTestXmlFileName;
    const char* dependentLayersTestXmlFileName = layersAccessTestXmlFileName;
    const char* moveLayerTestXmlFileName = layersAccessTestXmlFileName;
    const char* getParentLayerIdTestXmlFileName = layersAccessTestXmlFileName;
    const char* getChildLayerIdByRowTestXmlFileName = layersAccessTestXmlFileName;
    const char* getLayerRowInGroupTestXmlFileName = layersAccessTestXmlFileName;
    const char* getChildLayersCountTestXmlFileName = layersAccessTestXmlFileName;
    const char* moveLayerRowTestXmlFileName = layersAccessTestXmlFileName;

    const core::LayersCollection& getChildren(core::ILayer& layer)
    {
        return layer.get<core::IGroupLayer>()->children();
    }

    core::ITypedLayer::Type layerType(core::ILayer& layer)
    {
        return layer.get<core::ITypedLayer>()->type();
    }

    core::ZOrderType getZOrder(core::ILayer& layer)
    {
        return layer.renderStyle()->zOrder();
    }
}

void map::sortedLayersTest()
{
    core::IMapGuiPtr mapGui = map::createTestMapGui();

    mapGui->loadFromXml(sortedLayersTestXmlFileName, true);

    mapGui->open(map::createProgressStub());

    unsigned int layerId = 0;
    core::ZOrderType zOrder = 0;

    core::IGroupLayer* rootGroupLayer
        = mapGui->rootLayer()->get<core::IGroupLayer>();

    BOOST_REQUIRE(rootGroupLayer);

    // call Map::sortedChildren with NonSorted order type
    //
    auto lc = rootGroupLayer->children();

    BOOST_REQUIRE(lc.size() == 3);

    auto layerIt = lc.begin();
    layerId = (*layerIt)->id();

    BOOST_CHECK(layerId == 5);

    ++layerIt;
    layerId = (*layerIt)->id();

    BOOST_CHECK((*layerIt)->id() == 6);

    ++layerIt;
    layerId = (*layerIt)->id();

    BOOST_CHECK((*layerIt)->id() == 3);

    // call Map::sortedChildren with SortByZOrder order type
    //
    lc = rootGroupLayer->sortedChildren();

    BOOST_REQUIRE(lc.size() == 3);

    layerIt = lc.begin();
    layerId = (*layerIt)->id();
    zOrder = getZOrder(**layerIt);

    BOOST_CHECK(layerId == 8);
    BOOST_CHECK(zOrder == 3);

    ++layerIt;
    layerId = (*layerIt)->id();
    zOrder = getZOrder(**layerIt);

    BOOST_CHECK(layerId == 3);
    BOOST_CHECK(zOrder == 5);

    ++layerIt;
    layerId = (*layerIt)->id();
    zOrder = getZOrder(**layerIt);

    BOOST_CHECK(layerId == 5);
    BOOST_CHECK(zOrder == 7);
}

void map::linkedWithLayersTest()
{
    core::IMapGuiPtr mapGui = map::createTestMapGui();

    mapGui->loadFromXml(linkedLayersWithTestXmlFileName, true);

    /*
    map structure:

    Map
        TextLayer (id = 7, sourceContainerId = 3)---------------|
        GroupLayer (id = 8)                                     |
            TextLayer (id = 9, sourceContainerId = 3)-----------|
            GeometryLayer (id = 10, featureContainerId = 2)-|   |
            TextLayer (id = 11, sourceContainerId = 2)------|   |
        GroupLayer (id = 14)                                    |
            GeometryLayer (id = 12, featureContainerId = 3)-----|
            GroupLayer (id = 13)
    */

    mapGui->open(map::createProgressStub());
}

void map::dependentLayersTest()
{
    core::IMapGuiPtr mapGui = map::createTestMapGui();

    mapGui->loadFromXml(dependentLayersTestXmlFileName, true);

    /*
    map structure:

    Map
        TextLayer (id = 7, sourceContainerId = 3)---------------|
        GroupLayer (id = 8)                                     |
            TextLayer (id = 9, sourceContainerId = 3)-----------|
            GeometryLayer (id = 10, featureContainerId = 2)-|   |
            TextLayer (id = 11, sourceContainerId = 2)------|   |
        GroupLayer (id = 14)                                    |
            GeometryLayer (id = 12, featureContainerId = 3)-----|
            GroupLayer (id = 13)
    */

    mapGui->open(map::createProgressStub());

    auto layer = mapGui->getLayerById(13);
    auto lc = mapGui->dependentLayers(layer.get());

    BOOST_REQUIRE(lc.size() == 0);

    // not empty first group layer
    //
    layer = mapGui->getLayerById(8);
    lc = mapGui->dependentLayers(layer.get());

    BOOST_REQUIRE(lc.size() == 3);

    auto layerIt = lc.begin();
    BOOST_CHECK((*layerIt)->id() == 9);

    ++layerIt;
    BOOST_CHECK((*layerIt)->id() == 10);

    ++layerIt;
    BOOST_CHECK((*layerIt)->id() == 11);

    // not empty second group layer
    //
    layer = mapGui->getLayerById(14);
    lc = mapGui->dependentLayers(layer.get());

    BOOST_REQUIRE(lc.size() == 2);

    layerIt = lc.begin();
    BOOST_CHECK((*layerIt)->id() == 12);

    ++layerIt;
    BOOST_CHECK((*layerIt)->id() == 13);
}

void map::getParentLayerIdTest()
{
    core::IMapGuiPtr mapGui = map::createTestMapGui();

    mapGui->loadFromXml(getParentLayerIdTestXmlFileName, true);

    /*
    map structure:

    Map
        TextLayer (id = 7, sourceContainerId = 3)---------------|
        GroupLayer (id = 8)                                     |
            TextLayer (id = 9, sourceContainerId = 3)-----------|
            GeometryLayer (id = 10, featureContainerId = 2)-|   |
            TextLayer (id = 11, sourceContainerId = 2)------|   |
        GroupLayer (id = 14)                                    |
            GeometryLayer (id = 12, featureContainerId = 3)-----|
            GroupLayer (id = 13)
    */

    mapGui->open(map::createProgressStub());

    unsigned int parentLayerId = 0;

    // check that method throw exception
    //
    {
        // try call method for not exists layer
        //
        BOOST_CHECK_THROW(
            parentLayerId = mapGui->getParentLayerId(20),
            base::Exception);

        // try call method for root group layer
        //
        BOOST_CHECK_THROW(
            parentLayerId = mapGui->getParentLayerId(0),
            base::Exception);
    }

    parentLayerId = mapGui->getParentLayerId(8);

    BOOST_CHECK(parentLayerId == 0);

    parentLayerId = mapGui->getParentLayerId(14);

    BOOST_CHECK(parentLayerId == 0);

    parentLayerId = mapGui->getParentLayerId(11);

    BOOST_CHECK(parentLayerId == 8);

    parentLayerId = mapGui->getParentLayerId(12);

    BOOST_CHECK(parentLayerId == 14);

    parentLayerId = mapGui->getParentLayerId(13);

    BOOST_CHECK(parentLayerId == 14);
}

void map::getChildLayerIdByRowTest()
{
    core::IMapGuiPtr mapGui = map::createTestMapGui();

    mapGui->loadFromXml(getChildLayerIdByRowTestXmlFileName, true);

    /*
    map structure:

    Map
        TextLayer (id = 7, sourceContainerId = 3)---------------|
        GroupLayer (id = 8)                                     |
            TextLayer (id = 9, sourceContainerId = 3)-----------|
            GeometryLayer (id = 10, featureContainerId = 2)-|   |
            TextLayer (id = 11, sourceContainerId = 2)------|   |
        GroupLayer (id = 14)                                    |
            GeometryLayer (id = 12, featureContainerId = 3)-----|
            GroupLayer (id = 13)
    */

    mapGui->open(map::createProgressStub());

    unsigned int layerId = 0;

    // check that method throw exception
    //
    {
        // try call method for not exists layer
        //
        BOOST_CHECK_THROW(
            layerId = mapGui->getChildLayerIdByRow(20, 0),
            base::Exception);

        // try call method for not group layer
        //
        BOOST_CHECK_THROW(
            layerId = mapGui->getChildLayerIdByRow(10, 0),
            base::Exception);

        // try call method for wrong position
        //
        BOOST_CHECK_THROW(
            layerId = mapGui->getChildLayerIdByRow(8, -2),
            base::Exception);

        // try call method for wrong position
        //
        BOOST_CHECK_THROW(
            layerId = mapGui->getChildLayerIdByRow(0, 3),
            base::Exception);

        // try call method for wrong position
        //
        BOOST_CHECK_THROW(
            layerId = mapGui->getChildLayerIdByRow(13, 0),
            base::Exception);
    }

    layerId = mapGui->getChildLayerIdByRow(8, 0);

    BOOST_CHECK(layerId == 9);

    layerId = mapGui->getChildLayerIdByRow(8, 1);

    BOOST_CHECK(layerId == 10);

    layerId = mapGui->getChildLayerIdByRow(8, 2);

    BOOST_CHECK(layerId == 11);

    layerId = mapGui->getChildLayerIdByRow(0, 0);

    BOOST_CHECK(layerId == 7);

    layerId = mapGui->getChildLayerIdByRow(0, 1);

    BOOST_CHECK(layerId == 8);

    layerId = mapGui->getChildLayerIdByRow(0, 2);

    BOOST_CHECK(layerId == 14);

    layerId = mapGui->getChildLayerIdByRow(14, 0);

    BOOST_CHECK(layerId == 12);

    layerId = mapGui->getChildLayerIdByRow(14, 1);

    BOOST_CHECK(layerId == 13);
}

void map::getLayerRowInGroupTest()
{
    core::IMapGuiPtr mapGui = map::createTestMapGui();

    mapGui->loadFromXml(getLayerRowInGroupTestXmlFileName, true);

    /*
    map structure:

    Map
        TextLayer (id = 7, sourceContainerId = 3)---------------|
        GroupLayer (id = 8)                                     |
            TextLayer (id = 9, sourceContainerId = 3)-----------|
            GeometryLayer (id = 10, featureContainerId = 2)-|   |
            TextLayer (id = 11, sourceContainerId = 2)------|   |
        GroupLayer (id = 14)                                    |
            GeometryLayer (id = 12, featureContainerId = 3)-----|
            GroupLayer (id = 13)
    */

    mapGui->open(map::createProgressStub());

    int row = 0;

    // check that method throw exception
    //
    {
        // try call method for not exists layer
        //
        BOOST_CHECK_THROW(
            row = mapGui->getLayerRowInGroup(20),
            base::Exception);

        // try call method for root group layer
        //
        BOOST_CHECK_THROW(
            row = mapGui->getLayerRowInGroup(0),
            base::Exception);
    }

    row = mapGui->getLayerRowInGroup(11);

    BOOST_CHECK(row == 2);

    row = mapGui->getLayerRowInGroup(9);

    BOOST_CHECK(row == 0);

    row = mapGui->getLayerRowInGroup(10);

    BOOST_CHECK(row == 1);

    row = mapGui->getLayerRowInGroup(7);

    BOOST_CHECK(row == 0);

    row = mapGui->getLayerRowInGroup(14);

    BOOST_CHECK(row == 2);

    row = mapGui->getLayerRowInGroup(13);

    BOOST_CHECK(row == 1);

    row = mapGui->getLayerRowInGroup(8);

    BOOST_CHECK(row == 1);
}

void map::getChildLayersCountTest()
{
    core::IMapGuiPtr mapGui = map::createTestMapGui();

    mapGui->loadFromXml(getChildLayersCountTestXmlFileName, true);

    /*
    map structure:

    Map
        TextLayer (id = 7, sourceContainerId = 3)---------------|
        GroupLayer (id = 8)                                     |
            TextLayer (id = 9, sourceContainerId = 3)-----------|
            GeometryLayer (id = 10, featureContainerId = 2)-|   |
            TextLayer (id = 11, sourceContainerId = 2)------|   |
        GroupLayer (id = 14)                                    |
            GeometryLayer (id = 12, featureContainerId = 3)-----|
            GroupLayer (id = 13)
    */

    mapGui->open(map::createProgressStub());

    unsigned int childLayersCount = 0;

    // check that method throw exception
    //
    {
        // try call method for not exists layer
        //
        BOOST_CHECK_THROW(
            childLayersCount = mapGui->getChildLayersCount(20),
            base::Exception);
    }

    childLayersCount = mapGui->getChildLayersCount(0);

    BOOST_CHECK(childLayersCount == 3);

    childLayersCount = mapGui->getChildLayersCount(8);

    BOOST_CHECK(childLayersCount == 3);

    childLayersCount = mapGui->getChildLayersCount(14);

    BOOST_CHECK(childLayersCount == 2);

    childLayersCount = mapGui->getChildLayersCount(13);

    BOOST_CHECK(childLayersCount == 0);

    // try call method for text layer
    //
    childLayersCount = mapGui->getChildLayersCount(9);

    BOOST_CHECK(childLayersCount == 0);

    // try call method for geometry layer
    //
    childLayersCount = mapGui->getChildLayersCount(12);

    BOOST_CHECK(childLayersCount == 0);
}

bool moveLayerRowTestCheckBeginState(core::Map& map)
{
    const auto& lc = map.rootGroupLayer()->children();

    if (lc.size() != 3)
        return false;

    auto layerIt = lc.begin();

    if ((*layerIt)->id() != 7)
        return false;

    if (layerType(**layerIt) != core::ITypedLayer::DirectFileTextLayer)
        return false;

    ++layerIt;
    if ((*layerIt)->id() != 8)
        return false;

    if (layerType(**layerIt) != core::ITypedLayer::GroupLayer)
        return false;

    {
        const auto& lc2 = getChildren(**layerIt);

        if (lc2.size() != 3)
            return false;

        auto layerIt2 = lc2.begin();
        if ((*layerIt2)->id() != 9)
            return false;

        ++layerIt2;
        if ((*layerIt2)->id() != 10)
            return false;

        ++layerIt2;
        if ((*layerIt2)->id() != 11)
            return false;
    }

    ++layerIt;
    if ((*layerIt)->id() != 14)
            return false;
    if (layerType(**layerIt) != core::ITypedLayer::GroupLayer)
        return false;

    {
        const auto& lc2 = getChildren(**layerIt);

        if (lc2.size() != 2)
            return false;

        auto layerIt2 = lc2.begin();
        if ((*layerIt2)->id() != 12)
            return false;

        ++layerIt2;
        if ((*layerIt2)->id() != 13)
            return false;
    }

    return true;
}

bool moveLayerRowTestCheckEndState(core::Map& map)
{
/*
    Map
        GroupLayer (id = 14)
            GroupLayer (id = 8)
            GroupLayer (id = 13)
                TextLayer (id = 11)
                GeometryLayer (id = 10)
                TextLayer (id = 7)
                TextLayer (id = 9)
        GeometryLayer (id = 12)
*/


    const auto& lc = map.rootGroupLayer()->children();

    if (lc.size() != 2)
        return false;

    auto layerIt = lc.begin();

    if ((*layerIt)->id() != 14)
        return false;

    if (layerType(**layerIt) != core::ITypedLayer::GroupLayer)
        return false;

    {
        const auto& lc2 = getChildren(**layerIt);

        if (lc2.size() != 2)
            return false;

        auto layerIt2 = lc2.begin();
        if ((*layerIt2)->id() != 8)
            return false;

        if (layerType(**layerIt2) != core::ITypedLayer::GroupLayer)
            return false;

        {
            const auto& lc3 = getChildren(**layerIt2);

            if (lc3.size() != 0)
                return false;
        }

        ++layerIt2;
        if ((*layerIt2)->id() != 13)
            return false;

        if (layerType(**layerIt2) != core::ITypedLayer::GroupLayer)
            return false;

        {
            const auto& lc3 = getChildren(**layerIt2);

            if (lc3.size() != 4)
                return false;

            auto layerIt3 = lc3.begin();
            if ((*layerIt3)->id() != 11)
                return false;

            ++layerIt3;
            if ((*layerIt3)->id() != 10)
                return false;

            ++layerIt3;
            if ((*layerIt3)->id() != 7)
                return false;

            ++layerIt3;
            if ((*layerIt3)->id() != 9)
                return false;
        }
    }

    ++layerIt;
    if ((*layerIt)->id() != 12)
        return false;

    if (layerType(**layerIt) != core::ITypedLayer::DirectFileGeometryLayer)
        return false;

    return true;
}


void map::moveLayerRowTest()
{
    core::IMapGuiPtr mapGui = map::createTestMapGui();

    mapGui->loadFromXml(getChildLayersCountTestXmlFileName, true);

    /*
    map structure:

    Map
        TextLayer (id = 7)
        GroupLayer (id = 8)
            TextLayer (id = 9)
            GeometryLayer (id = 10)
            TextLayer (id = 11)
        GroupLayer (id = 14)
            GeometryLayer (id = 12)
            GroupLayer (id = 13)
    */

    mapGui->open(map::createProgressStub());

    // check that method not change map in special cases.
    //
    {
        BOOST_REQUIRE_NO_THROW(
            mapGui->moveLayerRow(10, 8, 1));

        BOOST_REQUIRE(moveLayerRowTestCheckBeginState(mapGui->map()));

        BOOST_REQUIRE_NO_THROW(
            mapGui->moveLayerRow(10, 8, 2));

        BOOST_REQUIRE(moveLayerRowTestCheckBeginState(mapGui->map()));

        BOOST_REQUIRE_NO_THROW(
            mapGui->moveLayerRow(11, 8, 3));

        BOOST_REQUIRE(moveLayerRowTestCheckBeginState(mapGui->map()));

        BOOST_REQUIRE_NO_THROW(
            mapGui->moveLayerRow(11, 8, 2));

        BOOST_REQUIRE(moveLayerRowTestCheckBeginState(mapGui->map()));
    }

    // check that method throw exception
    //
    {
        // try call method for not exists layer
        //
        BOOST_CHECK_THROW(
            mapGui->moveLayerRow(20, 0, 0),
            base::Exception);

        // try move layer into not exists group layer
        //
        BOOST_CHECK_THROW(
            mapGui->moveLayerRow(9, 20, 0),
            base::Exception);

        // try move layer into not group layer
        //
        BOOST_CHECK_THROW(
            mapGui->moveLayerRow(9, 12, 0),
            base::Exception);

        // try move layer into wrong position
        //
        BOOST_CHECK_THROW(
            mapGui->moveLayerRow(9, 0, 4),
            base::Exception);

        // try move layer into wrong position
        //
        BOOST_CHECK_THROW(
            mapGui->moveLayerRow(9, 0, -1),
            base::Exception);

        // try move group layer to itself
        //
        BOOST_CHECK_THROW(
            mapGui->moveLayerRow(14, 14, 1),
            base::Exception);
    }

    BOOST_REQUIRE_NO_THROW(
        mapGui->moveLayerRow(13, 14, 0));

    BOOST_REQUIRE_NO_THROW(
        mapGui->moveLayerRow(7, 13, 0));

    BOOST_REQUIRE_NO_THROW(
        mapGui->moveLayerRow(11, 13, 0));

    BOOST_REQUIRE_NO_THROW(
        mapGui->moveLayerRow(12, 0, 2));

    BOOST_REQUIRE_NO_THROW(
        mapGui->moveLayerRow(9, 13, 2));

    BOOST_REQUIRE_NO_THROW(
        mapGui->moveLayerRow(8, 14, 0));

    BOOST_REQUIRE_NO_THROW(
        mapGui->moveLayerRow(10, 13, 1));

/*
Map
    GroupLayer (id = 14)
        GroupLayer (id = 8)
        GroupLayer (id = 13)
            TextLayer (id = 11)
            GeometryLayer (id = 10)
            TextLayer (id = 7)
            TextLayer (id = 9)
    GeometryLayer (id = 12)
*/

    BOOST_REQUIRE(moveLayerRowTestCheckEndState(mapGui->map()));
}

// MAPSCORE-2828
void map::annotateLayerInOpenedMapTest()
{
    core::IMapGuiPtr mapGui = map::createTestMapGui();

    mapGui->open(map::createProgressStub());

    auto lc = mapGui->addLayerFromSource(
        map::createProgressStub(),
        io::path::absolute(sourceFileName),
        0);

    BOOST_REQUIRE(lc.size() == 1);

    auto geometryLayer = (*lc.begin());
    auto textLayer = mapGui->annotateGeometryLayer(0, 0, geometryLayer->id());

    BOOST_REQUIRE(layerType(*textLayer) == core::ITypedLayer::DirectFileTextLayer);

    BOOST_CHECK(textLayer->isOpened() == true);

    BOOST_CHECK_NO_THROW(mapGui->linkedWithLayers(*textLayer));

    mapGui->destroy();
}

void map::copyGroupLayerAndCheckOpenState()
{
    core::IMapGuiPtr mapGui = map::createTestMapGui();

    mapGui->open(map::createProgressStub());

    // prepare to reproduction bug:
    //
    // create not empty map
    // create new group layer
    // copy some layer to created group layer
    // make copy of created group layer
    //

    auto lc = mapGui->addLayerFromSource(
        map::createProgressStub(),
        io::path::absolute(sourceFileName),
        0);

    BOOST_REQUIRE(lc.size() == 1);

    auto geometryLayer = (*lc.begin());

    auto textLayer = mapGui->annotateGeometryLayer(0, 0, geometryLayer->id());

    auto groupLayer = mapGui->createGroupLayer();

    BOOST_REQUIRE_NO_THROW(
        mapGui->moveLayerRow(geometryLayer->id(), groupLayer->id(), 0));

    BOOST_REQUIRE_NO_THROW(
        mapGui->copyLayer(groupLayer, 0, textLayer->id()));

    unsigned int testGrLayerId =
        mapGui->copyLayer(core::ILayerPtr(), 0, groupLayer->id());

    auto testGroupLayer = mapGui->getLayerById(testGrLayerId);

    BOOST_REQUIRE(testGroupLayer);
    BOOST_REQUIRE(layerType(*testGroupLayer) == core::ITypedLayer::GroupLayer);

    // MAPSCORE-2846
    BOOST_REQUIRE(testGroupLayer->isOpened());

    lc = getChildren(*testGroupLayer);

    BOOST_REQUIRE(lc.size() == 2);

    // and bug: childs of last group layer are not opened (not accessible)
    //
    for (const auto& layer : lc) {
        BOOST_REQUIRE(layer->isOpened());
    }
    BOOST_REQUIRE_NO_THROW(
        lc = mapGui->linkedWithLayers(*geometryLayer));

    // and bug
    BOOST_REQUIRE_NO_THROW(
        lc = mapGui->linkedWithLayers(*textLayer));

    mapGui->destroy();
}

test_suite * map::initMapLayersAccessSuite()
{
    test_suite * suite = BOOST_TEST_SUITE("Map layers access test suite");

    suite->add(
        BOOST_TEST_CASE(&map::sortedLayersTest));
    suite->add(
        BOOST_TEST_CASE(&map::dependentLayersTest));
    suite->add(
        BOOST_TEST_CASE(&map::getParentLayerIdTest));
    suite->add(
        BOOST_TEST_CASE(&map::getChildLayerIdByRowTest));
    suite->add(
        BOOST_TEST_CASE(&map::getLayerRowInGroupTest));
    suite->add(
        BOOST_TEST_CASE(&map::getChildLayersCountTest));
    suite->add(
        BOOST_TEST_CASE(&map::moveLayerRowTest));
    suite->add(
        BOOST_TEST_CASE(&map::createGroupLayerTest));
    suite->add(
        BOOST_TEST_CASE(&map::copyLayerTest));
    suite->add(
        BOOST_TEST_CASE(&map::annotateLayerInOpenedMapTest));
    suite->add(
        BOOST_TEST_CASE(&map::copyGroupLayerAndCheckOpenState));

    ////suite->add(
    ////    BOOST_TEST_CASE(&map::linkedWithLayersTest));

    return suite;
}
