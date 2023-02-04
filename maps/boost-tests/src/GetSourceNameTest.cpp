#include "../include/GetSourceNameTest.h"
#include "tests/boost-tests/include/tools/map_tools.h"

#include "labeler/i_labelable_layer.h"

#include <yandex/maps/renderer5/core/ITypedLayer.h>
#include <yandex/maps/renderer5/core/Map.h>
#include <yandex/maps/renderer5/core/LayerProperties.h>

#include <boost/test/unit_test.hpp>

using namespace maps::renderer5;
using namespace maps::renderer5::core;
using namespace maps::renderer5::test;
using namespace boost::unit_test;

namespace {
const char* getFileSourceNameXmlFileName = "tests/boost-tests/maps/GetFileSourceNameTestMap.xml";
const char* fileSourceName = "../data/TwoPlineTest.mif";

bool checkLayerType(
    const core::ILayer* layer,
    core::ITypedLayer::Type type)
{
    if (layer) {
        if (auto ptr = layer->cast<core::ITypedLayer>()) {
            return ptr->type() == type;
        }
    }
    return false;
}

// throws core::NoCapabilityException
std::string getSourceFileName(
    core::ILayer& layer)
{
    if (auto ptr = layer.cast<labeler::ILabelableLayer>()) {
        return ptr->getSource().get<core::ILayerSourceFileName>()->sourceFileName();
    }
    return layer.get<core::ILayerSourceFileName>()->sourceFileName();
}

// throws core::NoCapabilityException
bool checkSourceFileName(
    core::ILayer& layer,
    const std::string& sourceFileName = std::string())
{
    return getSourceFileName(layer) == sourceFileName;
}

// throws core::NoCapabilityException
std::wstring getTableName(
    core::ILayer& layer)
{
    if (auto ptr = layer.cast<labeler::ILabelableLayer>()) {
        return ptr->getSource().get<core::ILayerTableName>()->tableName();
    }
    return layer.get<core::ILayerTableName>()->tableName();
}

// throws core::NoCapabilityException
bool checkTableName(
    core::ILayer& layer,
    const std::wstring& tableName = std::wstring())
{
    return getTableName(layer) == tableName;
}
}

void map::getFileSourceNameTest()
{
    {
        core::IMapGuiPtr mapGui = createTestMapGui();
        core::Map& map = mapGui->map();

        map.loadFromXml(getFileSourceNameXmlFileName, true);

        map.open();

        auto layer = map.rootGroupLayer()->getLayerById(20);

        BOOST_REQUIRE(
            checkLayerType(
                layer.get(),
                ITypedLayer::GroupLayer));

        BOOST_CHECK_THROW(
            checkSourceFileName(*layer),
            core::NoCapabilityException);

        BOOST_CHECK_THROW(
            checkTableName(*layer),
            core::NoCapabilityException);

        layer = map.rootGroupLayer()->getLayerById(21);

        BOOST_REQUIRE(
            checkLayerType(
                layer.get(),
                ITypedLayer::DirectFileGeometryLayer));

        BOOST_REQUIRE(
            checkSourceFileName(*layer, fileSourceName));

        layer = map.rootGroupLayer()->getLayerById(22);

        BOOST_REQUIRE(
            checkLayerType(layer.get(), ITypedLayer::DirectFileTextLayer));

        BOOST_REQUIRE(
            checkSourceFileName(*layer, fileSourceName));
    }

    deleteFilesFromDir(io::tempDirPath());
}
