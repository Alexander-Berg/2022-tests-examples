#include "../include/MapLayersAccessTest.h"
#include "tests/boost-tests/include/tools/map_tools.h"

using namespace boost::unit_test;
using namespace maps::renderer;
using namespace maps::renderer5;
using namespace maps::renderer5::test;

namespace
{
    const char* createLayerTestXmlFileName = "tests/boost-tests/maps/MapSortedLayersTest.xml";

    bool checkLayer(
        core::ILayerPtr layer,
        core::ITypedLayer::Type expectedLayerType,
        unsigned int expectedLayerId)
    {
        if (layer == 0)
            return false;

        auto typedLayer = layer->get<core::ITypedLayer>();

        if (typedLayer->type() != expectedLayerType)
            return false;

        if (layer->id() != expectedLayerId)
            return false;

        if (layer->name().empty())
            return false;

        // MAPSCORE-2846: check that layer is opened.
        //
        if (!layer->isOpened())
            return false;

        return true;
    }

    const core::LayersCollection& getChildren(core::ILayer& layer)
    {
        return layer.get<core::IGroupLayer>()->children();
    }
}

void createGroupLayerTestCheckEndState(core::Map & map)
{
    /*
    Map:
        Group Layer (id = 12)
        Text Layer (id = 5)
        Group Layer (id = 13)
        Group Layer (id = 6)
            Group Layer (id = 15)
            Text Layer (id = 8)
            Group Layer (id = 14)
        Geometry Layer (id = 3)
    */

    const auto& lc = getChildren(*map.rootLayer());
    auto layerIt = lc.begin();

    BOOST_REQUIRE(lc.size() == 5);

    BOOST_REQUIRE(
        checkLayer(*layerIt, core::ITypedLayer::GroupLayer, 12));

    ++layerIt;

    BOOST_REQUIRE(
        checkLayer(*layerIt, core::ITypedLayer::DirectFileTextLayer, 5));

    ++layerIt;

    BOOST_REQUIRE(
        checkLayer(*layerIt, core::ITypedLayer::GroupLayer, 13));

    ++layerIt;

    BOOST_REQUIRE(
        checkLayer(*layerIt, core::ITypedLayer::GroupLayer, 6));

    {
        const auto& lc2 = getChildren(**layerIt);
        auto layerIt2 = lc2.begin();

        BOOST_REQUIRE(lc2.size() == 3);

        BOOST_REQUIRE(
            checkLayer(*layerIt2, core::ITypedLayer::GroupLayer, 15));

        ++layerIt2;

        BOOST_REQUIRE(
            checkLayer(*layerIt2, core::ITypedLayer::DirectFileTextLayer, 8));

        ++layerIt2;

        BOOST_REQUIRE(
            checkLayer(*layerIt2, core::ITypedLayer::GroupLayer, 14));
    }

    ++layerIt;

    BOOST_REQUIRE(
        checkLayer(*layerIt, core::ITypedLayer::DirectFileGeometryLayer, 3));
}

void map::createGroupLayerTest()
{
    core::IMapGuiPtr mapGui = renderer5::test::map::createTestMapGui();
    mapGui->loadFromXml(createLayerTestXmlFileName, true);

    /*
    Map:
        Text Layer (id = 5)
        Group Layer (id = 6)
            Text Layer (id = 8)
        Geometry Layer (id = 3)
    */

    // check that method throw exception in closed map
    //
    BOOST_CHECK_THROW(
        mapGui->createGroupLayer(0),
        base::Exception);

    mapGui->open(map::createProgressStub());

    core::ILayerPtr groupLayer;

    // check that method throw exception
    //
    {
        // try create group layer in not existent group layer
        //
        BOOST_CHECK_THROW(
            groupLayer = mapGui->createGroupLayer(1),
            base::Exception);

        // try create group layer into not group layer (into Text layer with id = 5)
        //
        BOOST_CHECK_THROW(
            groupLayer = mapGui->createGroupLayer(5),
            base::Exception);

        // try create group layer into not group layer (into Geometry layer with id = 3)
        //
        BOOST_CHECK_THROW(
            groupLayer = mapGui->createGroupLayer(3),
            base::Exception);

        // try create group layer by wrong position
        //
        BOOST_CHECK_THROW(
            groupLayer = mapGui->createGroupLayer(6, -2),
            base::Exception);

        BOOST_CHECK_THROW(
            groupLayer = mapGui->createGroupLayer(6, 2),
            base::Exception);

        BOOST_CHECK_THROW(
            groupLayer = mapGui->createGroupLayer(0, 4),
            base::Exception);
    }

    // create group layer in root group layer
    //
    BOOST_REQUIRE_NO_THROW(
        groupLayer = mapGui->createGroupLayer(0));

    // create group layer in root group layer
    //
    BOOST_REQUIRE_NO_THROW(
        groupLayer = mapGui->createGroupLayer(0, 2));

    // create group layer in other group layer
    //
    BOOST_REQUIRE_NO_THROW(
        groupLayer = mapGui->createGroupLayer(6, 1));

    // create group layer in other group layer
    //
    BOOST_REQUIRE_NO_THROW(
        groupLayer = mapGui->createGroupLayer(6, 0));

    createGroupLayerTestCheckEndState(mapGui->map());
}
