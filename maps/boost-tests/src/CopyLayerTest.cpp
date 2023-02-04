#include "../include/MapLayersAccessTest.h"
#include "tests/boost-tests/include/tools/map_tools.h"

using namespace boost::unit_test;
using namespace maps::renderer;
using namespace maps::renderer5;
using namespace maps::renderer5::test;

namespace
{
    const char* copyLayerTestXmlFileName = "tests/boost-tests/maps/MapSortedLayersTest.xml";

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

    bool checkLayerType(
        core::ILayer* layer,
        core::ITypedLayer::Type type)
    {
        if (layer)
            if (auto ptr = layer->cast<core::ITypedLayer>()) {
                return ptr->type() == type;
        }
        return false;
    }

    const core::LayersCollection& getChildren(core::ILayer& layer)
    {
        return layer.get<core::IGroupLayer>()->children();
    }
}

void copyLayerTestCheckEndState(core::Map& map)
{
    /*
    Map:
        Text Layer (id = 5)
        Group Layer (id = 6)
            Text Layer (id = 14)
            Text Layer (id = 8)
            Group Layer (id = 20)
                    Text Layer (id = 21)
                    Text Layer (id = 22)
                    Geometry Layer (id = 23)
            Geometry Layer (id = 13)
        New Group Layer (id = 15)
            Group Layer (id = 16)
                Text Layer (id = 17)
                Text Layer (id = 18)
                Geometry Layer (id = 19)
        Geometry Layer (id = 3)

    */

    const auto& lc = map.rootGroupLayer()->children();
    auto layerIt = lc.begin();

    BOOST_REQUIRE(lc.size() == 4);

    BOOST_REQUIRE(checkLayer(*layerIt, core::ITypedLayer::DirectFileTextLayer, 5));

    ++layerIt;

    BOOST_REQUIRE(checkLayer(*layerIt, core::ITypedLayer::GroupLayer, 6));

    {
        const auto& lc2 = getChildren(**layerIt);
        auto layerIt2 = lc2.begin();

        BOOST_REQUIRE(lc2.size() == 4);

        BOOST_REQUIRE(
            checkLayer(*layerIt2, core::ITypedLayer::DirectFileTextLayer, 14));

        ++layerIt2;

        BOOST_REQUIRE(
            checkLayer(*layerIt2, core::ITypedLayer::DirectFileTextLayer, 8));

        ++layerIt2;

        BOOST_REQUIRE(
            checkLayer(*layerIt2, core::ITypedLayer::GroupLayer, 20));

        {
            const auto& lc3 = getChildren(**layerIt2);
            auto layerIt3 = lc3.begin();

            BOOST_REQUIRE(lc3.size() == 3);

            BOOST_REQUIRE(
                checkLayer(*layerIt3, core::ITypedLayer::DirectFileTextLayer, 21));

            ++layerIt3;

            BOOST_REQUIRE(
                checkLayer(*layerIt3, core::ITypedLayer::DirectFileTextLayer, 22));

            ++layerIt3;

            BOOST_REQUIRE(
                checkLayer(*layerIt3, core::ITypedLayer::DirectFileGeometryLayer, 23));
        }

        ++layerIt2;

        BOOST_REQUIRE(
            checkLayer(*layerIt2, core::ITypedLayer::DirectFileGeometryLayer, 13));
    }

    ++layerIt;

    BOOST_REQUIRE(
        checkLayer(*layerIt, core::ITypedLayer::GroupLayer, 15));
    {
        const auto& lc2 = getChildren(**layerIt);
        auto layerIt2 = lc2.begin();

        BOOST_REQUIRE(lc2.size() == 1);

        BOOST_REQUIRE(
            checkLayer(*layerIt2, core::ITypedLayer::GroupLayer, 16));

        {
            const auto& lc3 = getChildren(**layerIt2);
            auto layerIt3 = lc3.begin();

            BOOST_REQUIRE(lc3.size() == 3);

            BOOST_REQUIRE(
                checkLayer(*layerIt3, core::ITypedLayer::DirectFileTextLayer, 17));

            ++layerIt3;

            BOOST_REQUIRE(
                checkLayer(*layerIt3, core::ITypedLayer::DirectFileTextLayer, 18));

            ++layerIt3;

            BOOST_REQUIRE(
                checkLayer(*layerIt3, core::ITypedLayer::DirectFileGeometryLayer, 19));
        }
    }

    ++layerIt;

    BOOST_REQUIRE(
        checkLayer(*layerIt, core::ITypedLayer::DirectFileGeometryLayer, 3));
}

void map::copyLayerTest()
{
    core::IMapGuiPtr mapGui = renderer5::test::map::createTestMapGui();
    mapGui->loadFromXml(copyLayerTestXmlFileName, true);

    /*
    Map:
        Text Layer (id = 5)
        Group Layer (id = 6)
            Text Layer (id = 8)
        Geometry Layer (id = 3)
    */

    // check that method throw exception in closed map
    //
    BOOST_CHECK_THROW(mapGui->copyLayer(core::ILayerPtr(), 0, 5), base::Exception);

    mapGui->open(map::createProgressStub());

    core::ILayerPtr layer;

    BOOST_REQUIRE_NO_THROW(layer = mapGui->getLayerById(0));

    BOOST_REQUIRE(checkLayerType(layer.get(), core::ITypedLayer::GroupLayer));

    core::ILayerPtr rootLayer = layer;

    BOOST_REQUIRE_NO_THROW(layer = mapGui->getLayerById(6));

    BOOST_REQUIRE(checkLayerType(layer.get(), core::ITypedLayer::GroupLayer));

    core::ILayerPtr groupLayer = layer;

    // check that method throw exception
    {
        // try copy not existent layer
        BOOST_CHECK_THROW(mapGui->copyLayer(groupLayer, 0, 2), base::Exception);

        // try create group layer by wrong position
        BOOST_CHECK_THROW(mapGui->copyLayer(groupLayer, -2, 3), base::Exception);

        BOOST_CHECK_THROW(mapGui->copyLayer(groupLayer, 2, 3), base::Exception);

        BOOST_CHECK_THROW(mapGui->copyLayer(rootLayer, -1, 5), base::Exception);

        BOOST_CHECK_THROW(mapGui->copyLayer(rootLayer, 4, 5), base::Exception);
    }

    BOOST_REQUIRE_NO_THROW(mapGui->copyLayer(groupLayer, 1, 3));
    BOOST_REQUIRE_NO_THROW(mapGui->copyLayer(groupLayer, 0, 8));

    core::ILayerPtr newGroupLayer;
    BOOST_REQUIRE_NO_THROW(newGroupLayer = mapGui->createGroupLayer(0, 2));

    BOOST_REQUIRE_NO_THROW(mapGui->copyLayer(newGroupLayer, 0, 6));

    BOOST_REQUIRE_NO_THROW(mapGui->copyLayer(groupLayer, 2, 6));

    copyLayerTestCheckEndState(mapGui->map());
}
